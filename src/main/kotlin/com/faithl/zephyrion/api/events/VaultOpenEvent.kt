package com.faithl.zephyrion.api.events

import com.faithl.zephyrion.core.models.Vault
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import taboolib.platform.type.BukkitProxyEvent

class VaultOpenEvent(val vault: Vault, val page: Int, val inventory: Inventory, val opener: Player) :
    BukkitProxyEvent() {

    override val allowCancelled: Boolean
        get() = false

}