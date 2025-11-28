package com.faithl.zephyrion.api.events

import com.faithl.zephyrion.core.models.Vault
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import taboolib.platform.type.BukkitProxyEvent

/**
 * 保险库搜索模式关闭事件
 */
class VaultSearchCloseEvent(
    val vault: Vault,
    val params: Map<String, String>,
    val inventory: Inventory,
    val closer: Player,
) : BukkitProxyEvent() {

    override val allowCancelled: Boolean
        get() = false

}
