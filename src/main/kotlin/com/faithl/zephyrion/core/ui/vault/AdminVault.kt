package com.faithl.zephyrion.core.ui.vault

import com.faithl.zephyrion.core.models.Vault
import com.faithl.zephyrion.core.ui.UI
import com.faithl.zephyrion.core.ui.setSplitBlock
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.jetbrains.exposed.sql.transactions.transaction
import taboolib.common.util.sync
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.buildMenu
import taboolib.module.ui.type.Basic
import taboolib.platform.util.*

class AdminVault(val owner: Player, val vault: Vault, val root: UI? = null) : UI() {

    override fun build(): Inventory {
        return buildMenu<Basic>(title()) {
            setProperties(this)
            setInfomationItem(this)
            setSplitBlock(this)
            setNameItem(this)
            setDescItem(this)
            setReturnItem(this)
            setDeleteItem(this)
        }
    }

    fun setProperties(menu: Basic) {
        menu.rows(3)
        menu.handLocked(true)
        menu.map(
            "#########",
            "ND      E",
            "####I###R"
        )
        menu.onClick {
            it.isCancelled = true
        }
    }

    fun setInfomationItem(menu: Basic) {
        menu.set('I') {
            buildItem(XMaterial.BOOK) {
                name = owner.asLangText("vaults-admin-info-name")
                lore += owner.asLangTextList(
                    "vaults-admin-info-desc",
                    vault.id,
                    vault.name,
                    vault.desc ?: "",
                    vault.getCreatedAt(),
                    vault.getUpdatedAt()
                )
            }
        }
    }

    fun setNameItem(menu: Basic) {
        menu.set('N') {
            buildItem(XMaterial.PAPER) {
                name = owner.asLangText("vaults-admin-reset-name")
            }
        }
        menu.onClick('N') { event ->
            owner.closeInventory()
            owner.sendLang("vaults-admin-input-name")
            owner.nextChat {
                sync {
                    val result = vault.rename(it)
                    when (result.reason) {
                        "vault_name_invalid" -> owner.sendLang("vaults-admin-reset-name-invalid")
                        "vault_already_exists" -> owner.sendLang("vaults-admin-reset-name-existed")
                        "vault_name_color" -> owner.sendLang("vaults-admin-reset-name-color")
                        "vault_name_length" -> owner.sendLang("vaults-admin-reset-name-length")
                        null -> {
                            owner.sendLang("vaults-admin-reset-name-succeed")
                            owner.closeInventory()
                            root?.open(event.clicker)
                        }
                    }
                }
            }
        }
    }

    fun setDescItem(menu: Basic) {
        menu.set('D') {
            buildItem(XMaterial.PAPER) {
                name = owner.asLangText("vaults-admin-reset-desc")
            }
        }
        menu.onClick('D') { event ->
            owner.closeInventory()
            owner.sendLang("vaults-admin-input-desc")
            owner.nextChat {
                sync {
                    transaction {
                        vault.desc = it
                        vault.updatedAt = System.currentTimeMillis()
                    }
                    owner.sendLang("vaults-admin-reset-desc-succeed")
                    owner.closeInventory()
                    root?.open(event.clicker)
                }
            }
        }
    }

    fun setReturnItem(menu: Basic) {
        menu.set('R') {
            buildItem(XMaterial.RED_STAINED_GLASS_PANE) {
                name = owner.asLangText("vaults-admin-return")
            }
        }
        menu.onClick('R') {
            owner.closeInventory()
            root?.open(it.clicker)
        }
    }

    fun setDeleteItem(menu: Basic) {
        menu.set('E') {
            buildItem(XMaterial.BARRIER) {
                name = owner.asLangText("vaults-admin-delete")
            }
        }
        menu.onClick('E') { event ->
            owner.closeInventory()
            owner.sendLang("vaults-admin-delete-tip")
            owner.nextChat {
                if (it == "Y") {
                    transaction {
                        vault.delete()
                    }
                    owner.sendLang("vaults-admin-delete-succeed")
                } else {
                    owner.sendLang("vaults-admin-delete-canceled")
                }
                sync {
                    root?.open(event.clicker)
                }
            }
        }
    }

    override fun open(opener: Player) {
        opener.openInventory(build())
    }

    override fun title(): String {
        return owner.asLangText("vaults-admin-title")
    }

}