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
import taboolib.module.ui.type.Basic
import taboolib.platform.util.asLangText
import taboolib.platform.util.buildItem
import taboolib.platform.util.nextChat
import taboolib.platform.util.sendLang

class CreateVault(val owner: Player, val workspace: Workspace, val root: UI? = null) : UI() {

    var name: String? = null
    var description: String? = null

    override fun build(): Inventory {
        return buildMenu<Basic>(title()) {
            setProperties(this)
            setInfomationItem(this)
            setSplitBlock(this)
            setNameItem(this)
            setDescItem(this)
            setReturnItem(this)
            setConfirmItem(this)
        }
    }

    fun setProperties(menu: Basic) {
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

    fun setInfomationItem(menu: Basic) {
        menu.set('I') {
            buildItem(XMaterial.BOOK) {
                name = owner.asLangText("vaults-create-item-info-name")
                name?.let {
                    lore += owner.asLangText("vaults-create-item-info-lore-name", it)
                }
                name?.let {
                    lore += owner.asLangText("vaults-create-item-info-lore-desc", it)
                }
            }
        }
    }

    fun setNameItem(menu: Basic) {
        menu.set('N') {
            buildItem(XMaterial.PAPER) {
                name = if (name != null) {
                    owner.asLangText("vaults-create-reset-name")
                } else {
                    owner.asLangText("vaults-create-set-name")
                }
            }
        }
        menu.onClick('N') { event ->
            owner.closeInventory()
            owner.sendLang("vaults-create-input-name")
            owner.nextChat {
                sync {
                    name = it
                    open(event.clicker)
                }
            }
        }
    }

    fun setDescItem(menu: Basic) {
        menu.set('D') {
            buildItem(XMaterial.PAPER) {
                name = if (description != null) {
                    owner.asLangText("vaults-create-reset-desc")
                } else {
                    owner.asLangText("vaults-create-set-desc")
                }
            }
        }
        menu.onClick('D') { event ->
            owner.closeInventory()
            owner.sendLang("vaults-create-input-desc")
            owner.nextChat {
                sync {
                    description = it
                    open(event.clicker)
                }
            }
        }
    }

    fun setReturnItem(menu: Basic) {
        menu.set('R') {
            buildItem(XMaterial.BARRIER) {
                name = owner.asLangText("vaults-create-return")
            }
        }
        menu.onClick('R') {
            owner.closeInventory()
            root?.open(it.clicker)
        }
    }

    fun setConfirmItem(menu: Basic) {
        menu.set('C') {
            buildItem(XMaterial.GREEN_STAINED_GLASS_PANE) {
                name = owner.asLangText("vaults-create-confirm")
            }
        }
        menu.onClick('C') {
            val result = ZephyrionAPI.createVault(workspace, name, description)
            when (result.reason) {
                "vault_name_invalid" -> owner.sendLang("vaults-create-name-invalid")
                "vault_already_exists" -> owner.sendLang("vaults-create-name-existed")
                "vault_name_color" -> owner.sendLang("vaults-create-name-color")
                "vault_name_length" -> owner.sendLang("vaults-create-name-length")
                "vault_name_blacklist" -> owner.sendLang("vaults-create-name-blacklist")
                null -> {
                    owner.sendLang("vaults-create-succeed")
                    owner.closeInventory()
                    root?.open(it.clicker)
                }
            }
        }
    }

    override fun open(opener: Player) {
        opener.openInventory(build())
    }

    override fun title(): String {
        return owner.asLangText("vaults-create-title")
    }

}