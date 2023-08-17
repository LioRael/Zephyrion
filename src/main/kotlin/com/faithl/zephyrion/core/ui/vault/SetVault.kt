package com.faithl.zephyrion.core.ui.vault

import com.faithl.zephyrion.core.models.Setting
import com.faithl.zephyrion.core.models.Settings
import com.faithl.zephyrion.core.models.Vault
import com.faithl.zephyrion.core.ui.UI
import com.faithl.zephyrion.core.ui.setLinkedMenuProperties
import com.faithl.zephyrion.core.ui.setRows6SplitBlock
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.inventory.Inventory
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.ui.buildMenu
import taboolib.module.ui.type.Linked

class SetVault(val vault: Vault, val owner: Player) : UI() {

    fun addAutoPickup() {
        // TODO
    }

    override fun build(): Inventory {
        return buildMenu<Linked<Setting>>(title()) {
            elements {
                Setting.find { Settings.vault eq vault.id }.toList()
            }
            setLinkedMenuProperties(this)
            setRows6SplitBlock(this)
        }
    }

    override fun open(opener: Player) {
        opener.openInventory(build())
    }

    override fun title(): String {
        return "Vault Settings"
    }

    companion object {

        @SubscribeEvent
        fun e(e: EntityPickupItemEvent) {
            // TODO
        }
    }

}