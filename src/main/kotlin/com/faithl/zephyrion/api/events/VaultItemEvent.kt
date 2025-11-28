package com.faithl.zephyrion.api.events

import com.faithl.zephyrion.core.models.Vault
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.platform.type.BukkitProxyEvent

/**
 * 物品添加到保险库事件
 */
class VaultAddItemEvent(
    val vault: Vault,
    val page: Int,
    val slot: Int,
    val itemStack: ItemStack,
    val operator: Player? = null
) : BukkitProxyEvent() {
    override val allowCancelled: Boolean get() = false
}

/**
 * 物品从保险库移除事件
 */
class VaultRemoveItemEvent(
    val vault: Vault,
    val page: Int,
    val slot: Int,
    val operator: Player? = null
) : BukkitProxyEvent() {
    override val allowCancelled: Boolean get() = false
}
