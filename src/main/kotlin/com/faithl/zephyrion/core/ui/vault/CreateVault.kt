package com.faithl.zephyrion.core.ui.vault

import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.core.models.Workspace
import com.faithl.zephyrion.core.ui.UI
import com.faithl.zephyrion.core.ui.setSplitBlock
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import taboolib.common.util.sync
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.buildMenu
import taboolib.module.ui.type.Chest
import taboolib.module.ui.type.impl.ChestImpl
import taboolib.platform.util.asLangText
import taboolib.platform.util.buildItem
import taboolib.platform.util.nextChat
import taboolib.platform.util.sendLang

class CreateVault(override val opener: Player, val workspace: Workspace,override val root: UI? = null) : UI() {

    var name: String? = null
    var description: String? = null

    override fun build(): Inventory {
        return buildMenu<ChestImpl>(title()) {
            setProperties(this)
            setInfomationItem(this)
            setSplitBlock(this)
            setNameItem(this)
            setDescItem(this)
            setReturnItem(this)
            setConfirmItem(this)
        }
    }

    fun setProperties(menu: Chest) {
        menu.rows(3)
        menu.handLocked(true)
        menu.map(
            "####I####",
            "ND       ",
            "####C###R"
        )
        menu.onClick {
            it.isCancelled = true
        }
    }

    fun setInfomationItem(menu: Chest) {
        menu.set('I') {
            buildItem(XMaterial.BOOK) {
                name = opener.asLangText("vaults-create-item-info-name")
                this@CreateVault.name?.let {
                    lore += opener.asLangText("vaults-create-item-info-lore-name", it)
                }
                this@CreateVault.description?.let {
                    lore += opener.asLangText("vaults-create-item-info-lore-desc", it)
                }
            }
        }
    }

    fun setNameItem(menu: Chest) {
        menu.set('N') {
            buildItem(XMaterial.PAPER) {
                name = if (name != null) {
                    opener.asLangText("vaults-create-reset-name")
                } else {
                    opener.asLangText("vaults-create-set-name")
                }
            }
        }
        menu.onClick('N') { event ->
            opener.closeInventory()
            opener.sendLang("vaults-create-input-name")
            opener.nextChat {
                name = it
                sync { open() }
            }
        }
    }

    fun setDescItem(menu: Chest) {
        menu.set('D') {
            buildItem(XMaterial.PAPER) {
                name = if (description != null) {
                    opener.asLangText("vaults-create-reset-desc")
                } else {
                    opener.asLangText("vaults-create-set-desc")
                }
            }
        }
        menu.onClick('D') { event ->
            opener.closeInventory()
            opener.sendLang("vaults-create-input-desc")
            opener.nextChat {
                description = it
                sync { open() }
            }
        }
    }

    override fun setReturnItem(menu: Chest) {
        menu.set('R') {
            buildItem(XMaterial.BARRIER) {
                name = opener.asLangText("ui-item-name-return")
            }
        }
        menu.onClick('R') {
            it.clicker.closeInventory()
            root?.open()
        }
    }

    fun setConfirmItem(menu: Chest) {
        menu.set('C') {
            buildItem(XMaterial.GREEN_STAINED_GLASS_PANE) {
                name = opener.asLangText("vaults-create-confirm")
            }
        }
        menu.onClick('C') {
            val result = ZephyrionAPI.createVault(workspace, name, description)
            when (result.reason) {
                "vault_name_invalid" -> opener.sendLang("vaults-create-name-invalid")
                "vault_already_exists" -> opener.sendLang("vaults-create-name-existed")
                "vault_name_color" -> opener.sendLang("vaults-create-name-color")
                "vault_name_length" -> opener.sendLang("vaults-create-name-length")
                "vault_name_blacklist" -> opener.sendLang("vaults-create-name-blacklist")
                null -> {
                    opener.sendLang("vaults-create-succeed")
                    opener.closeInventory()
                    root?.open()
                }
            }
        }
    }

    override fun open() {
        opener.openInventory(build())
    }

    override fun title(): String {
        return opener.asLangText("vaults-create-title")
    }

}