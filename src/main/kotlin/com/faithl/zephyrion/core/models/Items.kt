package com.faithl.zephyrion.core.models

import com.faithl.zephyrion.api.events.VaultAddItemEvent
import com.faithl.zephyrion.api.events.VaultRemoveItemEvent
import com.faithl.zephyrion.core.cache.ItemCache
import com.faithl.zephyrion.storage.DatabaseConfig
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import taboolib.module.database.Table
import taboolib.module.nms.getName
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Item数据类
 */
data class Item(
    var id: Int,
    var vaultId: Int,
    var page: Int,
    var owner: String?,
    var slot: Int,
    var itemStackSerialized: String
) : java.io.Serializable {

    var itemStack: ItemStack
        get() = fromByteArray(itemStackSerialized)
        set(value) {
            itemStackSerialized = toBase64(value)
        }

    companion object {

        private val table: Table<*, *>
            get() = DatabaseConfig.itemsTable

        private val dataSource
            get() = DatabaseConfig.dataSource

        /**
         * 获取保险库所有物品（使用缓存）
         */
        private fun getAllItems(vault: Vault, player: Player? = null): List<Item> {
            val isIndependent = vault.workspace.type == WorkspaceType.INDEPENDENT
            val ownerUUID = player?.uniqueId?.toString()

            // 尝试从缓存获取
            val cached = ItemCache.getAllItems(vault.id, ownerUUID, isIndependent)
            if (cached != null) {
                return cached
            }

            // 从数据库加载
            val items = if (isIndependent && ownerUUID != null) {
                table.select(dataSource) {
                    where {
                        "vault_id" eq vault.id
                        and { "owner" eq ownerUUID }
                    }
                }.map { mapRowToItem() }
            } else {
                table.select(dataSource) {
                    where { "vault_id" eq vault.id }
                }.map { mapRowToItem() }
            }

            // 更新缓存
            ItemCache.updateAllItems(vault.id, ownerUUID, isIndependent, items)
            return items
        }

        /**
         * 行映射到 Item 对象
         */
        private fun java.sql.ResultSet.mapRowToItem(): Item {
            return Item(
                id = getInt("id"),
                vaultId = getInt("vault_id"),
                page = getInt("page"),
                owner = getString("owner"),
                slot = getInt("slot"),
                itemStackSerialized = getString("item_stack")
            )
        }

        /**
         * 根据名称搜索物品（使用缓存）
         */
        fun searchItemsByName(vault: Vault, name: String): List<Item> {
            return getAllItems(vault).filter { it.getName().contains(name, true) }
        }

        /**
         * 根据Lore搜索物品（使用缓存）
         */
        fun searchItemsByLore(vault: Vault, lore: String): List<Item> {
            return getAllItems(vault).filter { it.getLore().any { line -> line.contains(lore, true) } }
        }

        /**
         * 组合搜索物品（支持名称和Lore的组合搜索，使用缓存）
         */
        fun searchItems(vault: Vault, params: Map<String, String>, player: Player? = null): List<Item> {
            val allItems = getAllItems(vault, player)

            return allItems.filter { item ->
                val nameMatch = params["name"]?.let { name ->
                    item.getName().contains(name, true)
                } ?: true

                val loreMatch = params["lore"]?.let { lore ->
                    item.getLore().any { it.contains(lore, true) }
                } ?: true

                nameMatch && loreMatch
            }
        }

        /**
         * 获取指定页的物品（使用缓存）
         */
        fun getItems(vault: Vault, page: Int, player: Player): List<Item> {
            val isIndependent = vault.workspace.type == WorkspaceType.INDEPENDENT
            val ownerUUID = player.uniqueId.toString()

            // 尝试从缓存获取
            val cached = ItemCache.getPageItems(vault.id, page, ownerUUID, isIndependent)
            if (cached != null) {
                return cached
            }

            // 从数据库加载
            val items = if (isIndependent) {
                table.select(dataSource) {
                    where {
                        "vault_id" eq vault.id
                        and { "page" eq page }
                        and { "owner" eq ownerUUID }
                    }
                }.map { mapRowToItem() }
            } else {
                table.select(dataSource) {
                    where {
                        "vault_id" eq vault.id
                        and { "page" eq page }
                    }
                }.map { mapRowToItem() }
            }

            // 更新缓存
            ItemCache.updatePageItems(vault.id, page, ownerUUID, isIndependent, items)
            return items
        }

        /**
         * 设置物品（优化：使用 DELETE + INSERT 替代 SELECT + UPDATE/INSERT）
         */
        fun setItem(vault: Vault, page: Int, slot: Int, itemStack: ItemStack, player: Player? = null) {
            val isIndependent = vault.workspace.type == WorkspaceType.INDEPENDENT
            val ownerUUID = if (isIndependent) player!!.uniqueId.toString() else null
            val itemBase64 = itemStack.toBase64()

            // 先删除旧记录（如果存在）
            if (isIndependent) {
                table.delete(dataSource) {
                    where {
                        "vault_id" eq vault.id
                        and { "page" eq page }
                        and { "slot" eq slot }
                        and { "owner" eq ownerUUID!! }
                    }
                }
            } else {
                table.delete(dataSource) {
                    where {
                        "vault_id" eq vault.id
                        and { "page" eq page }
                        and { "slot" eq slot }
                    }
                }
            }

            // 插入新记录
            table.insert(dataSource, "vault_id", "page", "owner", "slot", "item_stack") {
                value(vault.id, page, ownerUUID, slot, itemBase64)
            }

            // 写时更新缓存（而非失效）
            val newItem = Item(
                id = 0,
                vaultId = vault.id,
                page = page,
                owner = ownerUUID,
                slot = slot,
                itemStackSerialized = itemBase64
            )
            ItemCache.addOrUpdate(vault.id, page, slot, ownerUUID, isIndependent, newItem)

            VaultAddItemEvent(vault, page, slot, itemStack, player).call()
        }

        /**
         * 删除物品
         */
        fun removeItem(vault: Vault, page: Int, slot: Int, player: Player? = null) {
            val isIndependent = vault.workspace.type == WorkspaceType.INDEPENDENT
            val ownerUUID = if (isIndependent) player!!.uniqueId.toString() else null

            if (isIndependent) {
                table.delete(dataSource) {
                    where {
                        "vault_id" eq vault.id
                        and { "page" eq page }
                        and { "slot" eq slot }
                        and { "owner" eq ownerUUID!! }
                    }
                }
            } else {
                table.delete(dataSource) {
                    where {
                        "vault_id" eq vault.id
                        and { "page" eq page }
                        and { "slot" eq slot }
                    }
                }
            }

            // 写时更新缓存（而非失效）
            ItemCache.remove(vault.id, page, slot, ownerUUID, isIndependent)

            VaultRemoveItemEvent(vault, page, slot, player).call()
        }

        /**
         * 获取保险库中匹配指定材料类型的物品（使用缓存）
         */
        fun getItemsByMaterials(vault: Vault, materials: Set<Material>, player: Player? = null): List<Item> {
            return getAllItems(vault, player)
                .filter { materials.contains(it.itemStack.type) }
                .sortedWith(compareBy({ it.page }, { it.slot }))
        }

        private fun toBase64(itemStack: ItemStack): String {
            return itemStack.toBase64()
        }

        private fun fromByteArray(base64: String): ItemStack {
            return base64.base64ToItemStack()
        }
    }

    /**
     * 获取关联的保险库
     */
    val vault: Vault
        get() = Vault.findById(vaultId)
            ?: error("Vault not found for item $id")

    /**
     * 获取物品名称
     */
    fun getName(): String {
        return itemStack.getName()
    }

    /**
     * 获取物品Lore
     */
    fun getLore(): List<String> {
        return itemStack.itemMeta?.lore ?: listOf()
    }

    /**
     * 移除Minecraft颜色代码
     */
    private fun stripColorCodes(text: String): String {
        return text.replace("§[0-9a-fk-or]".toRegex(), "")
    }
}

/**
 * ItemStack序列化扩展函数
 */
@OptIn(ExperimentalEncodingApi::class)
fun ItemStack.toBase64(): String {
    ByteArrayOutputStream().use { byteArrayOutputStream ->
        BukkitObjectOutputStream(byteArrayOutputStream).use { bukkitObjectOutputStream ->
            bukkitObjectOutputStream.writeObject(this)
            return Base64.encode(byteArrayOutputStream.toByteArray())
        }
    }
}

/**
 * Base64反序列化为ItemStack
 */
@OptIn(ExperimentalEncodingApi::class)
fun String.base64ToItemStack(): ItemStack {
    ByteArrayInputStream(Base64.decode(this)).use { byteArrayInputStream ->
        BukkitObjectInputStream(byteArrayInputStream).use { bukkitObjectInputStream ->
            return bukkitObjectInputStream.readObject() as ItemStack
        }
    }
}
