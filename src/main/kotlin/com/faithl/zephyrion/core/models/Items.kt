package com.faithl.zephyrion.core.models

import com.faithl.zephyrion.api.events.VaultAddItemEvent
import com.faithl.zephyrion.api.events.VaultRemoveItemEvent
import com.faithl.zephyrion.storage.DatabaseConfig
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import taboolib.common.util.sync
import taboolib.module.database.Table
import taboolib.module.nms.getI18nName
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
) {

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
         * 根据名称搜索物品
         */
        fun searchItemsByName(vault: Vault, name: String): List<Item> {
            return table.select(dataSource) {
                where { "vault_id" eq vault.id }
            }.map {
                Item(
                    id = getInt("id"),
                    vaultId = getInt("vault_id"),
                    page = getInt("page"),
                    owner = getString("owner"),
                    slot = getInt("slot"),
                    itemStackSerialized = getString("item_stack")
                )
            }.filter { it.getName().contains(name,true) }
        }

        /**
         * 根据Lore搜索物品
         */
        fun searchItemsByLore(vault: Vault, lore: String): List<Item> {
            return table.select(dataSource) {
                where { "vault_id" eq vault.id }
            }.map {
                Item(
                    id = getInt("id"),
                    vaultId = getInt("vault_id"),
                    page = getInt("page"),
                    owner = getString("owner"),
                    slot = getInt("slot"),
                    itemStackSerialized = getString("item_stack")
                )
            }.filter { it.getLore().any { it.contains(lore,true) } }
        }

        /**
         * 组合搜索物品（支持名称和Lore的组合搜索）
         */
        fun searchItems(vault: Vault, params: Map<String, String>, player: Player? = null): List<Item> {
            val allItems = if (vault.workspace.type == WorkspaceType.INDEPENDENT && player != null) {
                table.select(dataSource) {
                    where {
                        "vault_id" eq vault.id
                        and { "owner" eq player.uniqueId.toString() }
                    }
                }.map {
                    Item(
                        id = getInt("id"),
                        vaultId = getInt("vault_id"),
                        page = getInt("page"),
                        owner = getString("owner"),
                        slot = getInt("slot"),
                        itemStackSerialized = getString("item_stack")
                    )
                }
            } else {
                table.select(dataSource) {
                    where { "vault_id" eq vault.id }
                }.map {
                    Item(
                        id = getInt("id"),
                        vaultId = getInt("vault_id"),
                        page = getInt("page"),
                        owner = getString("owner"),
                        slot = getInt("slot"),
                        itemStackSerialized = getString("item_stack")
                    )
                }
            }

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
         * 获取指定页的物品
         */
        fun getItems(vault: Vault, page: Int, player: Player): List<Item> {
            return if (vault.workspace.type == WorkspaceType.INDEPENDENT) {
                table.select(dataSource) {
                    where {
                        "vault_id" eq vault.id
                        and { "page" eq page }
                        and { "owner" eq player.uniqueId.toString() }
                    }
                }.map {
                    Item(
                        id = getInt("id"),
                        vaultId = getInt("vault_id"),
                        page = getInt("page"),
                        owner = getString("owner"),
                        slot = getInt("slot"),
                        itemStackSerialized = getString("item_stack")
                    )
                }
            } else {
                table.select(dataSource) {
                    where {
                        "vault_id" eq vault.id
                        and { "page" eq page }
                    }
                }.map {
                    Item(
                        id = getInt("id"),
                        vaultId = getInt("vault_id"),
                        page = getInt("page"),
                        owner = getString("owner"),
                        slot = getInt("slot"),
                        itemStackSerialized = getString("item_stack")
                    )
                }
            }
        }

        /**
         * 设置物品
         */
        fun setItem(vault: Vault, page: Int, slot: Int, itemStack: ItemStack, player: Player? = null) {
            submitAsync {
                val existing = if (vault.workspace.type == WorkspaceType.INDEPENDENT) {
                    table.select(dataSource) {
                        where {
                            "vault_id" eq vault.id
                            and { "page" eq page }
                            and { "slot" eq slot }
                            and { "owner" eq player!!.uniqueId.toString() }
                        }
                    }.find()
                } else {
                    table.select(dataSource) {
                        where {
                            "vault_id" eq vault.id
                            and { "page" eq page }
                            and { "slot" eq slot }
                        }
                    }.find()
                }

                val itemBase64 = itemStack.toBase64()

                if (!existing) {
                    // 插入新记录
                    table.insert(dataSource, "vault_id", "page", "owner", "slot", "item_stack") {
                        value(
                            vault.id,
                            page,
                            if (vault.workspace.type == WorkspaceType.INDEPENDENT) player!!.uniqueId.toString() else null,
                            slot,
                            itemBase64
                        )
                    }
                } else {
                    // 更新现有记录
                    table.update(dataSource) {
                        set("item_stack", itemBase64)
                        where {
                            "vault_id" eq vault.id
                            "page" eq page
                            "slot" eq slot
                            if (vault.workspace.type == WorkspaceType.INDEPENDENT) {
                                and { "owner" eq player!!.uniqueId.toString() }
                            }
                        }
                    }
                }
                sync {
                    VaultAddItemEvent(vault, page, slot, itemStack, player).call()
                }
            }
        }

        /**
         * 删除物品
         */
        fun removeItem(vault: Vault, page: Int, slot: Int, player: Player? = null) {
            submitAsync {
                if (vault.workspace.type == WorkspaceType.INDEPENDENT) {
                    table.delete(dataSource) {
                        where {
                            "vault_id" eq vault.id
                            and { "page" eq page }
                            and { "slot" eq slot }
                            and { "owner" eq player!!.uniqueId.toString() }
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
                sync {
                    VaultRemoveItemEvent(vault, page, slot, player).call()
                }
            }
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
