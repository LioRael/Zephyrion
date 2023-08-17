package com.faithl.zephyrion.core.models

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import taboolib.module.nms.getI18nName
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object Items : IntIdTable() {
    val vault = reference("vault", Vaults)
    val page = integer("page")
    val owner = varchar("owner", 36).nullable()
    val slot = integer("slot")
    val itemStackSerialized = text("item_stack")
}

class Item(id: EntityID<Int>) : IntEntity(id) {

    var owner by Items.owner
    var vault by Vault referencedOn Items.vault
    var page by Items.page
    var slot by Items.slot
    var itemStackSerialized by Items.itemStackSerialized
    var itemStack: ItemStack
        get() = fromByteArray(itemStackSerialized)
        set(value) {
            itemStackSerialized = toBase64(value)
        }

    fun getName(): String {
        return itemStack.itemMeta?.displayName ?: itemStack.getI18nName()
    }

    fun getLore(): List<String> {
        return itemStack.itemMeta?.lore ?: listOf()
    }

    companion object : IntEntityClass<Item>(Items) {

        fun searchItemsByName(vault: Vault, name: String): List<Item> {
            return transaction {
                find { Items.vault eq vault.id }.filter {
                    it.getName().contains(name, true)
                }.toList()
            }
        }

        fun searchItemsByLore(vault: Vault, lore: String): List<Item> {
            return transaction {
                find { Items.vault eq vault.id }.filter {
                    it.getLore().contains(lore)
                }.toList()
            }
        }

        fun getItems(vault: Vault, page: Int, player: Player): List<Item> {
            return transaction {
                if (vault.workspace.type == Workspaces.Type.INDEPENDENT) {
                    find { Items.vault eq vault.id and (Items.page eq page) and (Items.owner eq player.uniqueId.toString()) }.toList()
                } else {
                    find { Items.vault eq vault.id and (Items.page eq page) }.toList()
                }
            }
        }

        fun toBase64(itemStack: ItemStack): String {
            return itemStack.toBase64()
        }

        fun fromByteArray(base64: String): ItemStack {
            return base64.base64ToItemStack()
        }

        fun setItem(vault: Vault, page: Int, slot: Int, itemStack: ItemStack, player: Player? = null) {
            transaction {
                val item =
                    find { (Items.vault eq vault.id) and (Items.page eq page) and (Items.slot eq slot) }.firstOrNull()
                if (item == null) {
                    new {
                        if (vault.workspace.type == Workspaces.Type.INDEPENDENT) {
                            this.owner = player!!.uniqueId.toString()
                        }
                        this.vault = vault
                        this.page = page
                        this.slot = slot
                        this.itemStack = itemStack
                    }
                } else {
                    item.itemStack = itemStack
                }
            }
        }

        fun removeItem(vault: Vault, page: Int, slot: Int, player: Player? = null) {
            if (vault.workspace.type == Workspaces.Type.INDEPENDENT) {
                transaction {
                    val item =
                        find { (Items.vault eq vault.id) and (Items.page eq page) and (Items.slot eq slot) and (Items.owner eq player!!.uniqueId.toString()) }.firstOrNull()
                    item?.delete()
                }
                return
            }
            transaction {
                val item =
                    find { (Items.vault eq vault.id) and (Items.page eq page) and (Items.slot eq slot) }.firstOrNull()
                item?.delete()
            }
        }

    }

}

@OptIn(ExperimentalEncodingApi::class)
fun ItemStack.toBase64(): String {
    ByteArrayOutputStream().use { byteArrayOutputStream ->
        BukkitObjectOutputStream(byteArrayOutputStream).use { bukkitObjectOutputStream ->
            bukkitObjectOutputStream.writeObject(this)
            return Base64.encode(byteArrayOutputStream.toByteArray())
        }
    }
}

@OptIn(ExperimentalEncodingApi::class)
fun String.base64ToItemStack(): ItemStack {
    ByteArrayInputStream(Base64.decode(this)).use { byteArrayInputStream ->
        BukkitObjectInputStream(byteArrayInputStream).use { bukkitObjectInputStream ->
            return bukkitObjectInputStream.readObject() as ItemStack
        }
    }
}