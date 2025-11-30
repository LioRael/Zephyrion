package com.faithl.zephyrion.core.ui.vault

import com.faithl.zephyrion.core.models.Vault
import com.faithl.zephyrion.core.ui.UI
import com.faithl.zephyrion.core.ui.setSplitBlock
import com.faithl.zephyrion.core.ui.vault.autopickup.ListAutoPickups
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import taboolib.common.util.sync
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.buildMenu
import taboolib.module.ui.type.Chest
import taboolib.module.ui.type.impl.ChestImpl
import taboolib.platform.util.*

class AdminVault(override val opener: Player, val vault: Vault, override val root: UI? = null) : UI() {

    override fun build(): Inventory {
        return buildMenu<ChestImpl>(title()) {
            setProperties(this)
            setInfomationItem(this)
            setSplitBlock(this)
            setNameItem(this)
            setDescItem(this)
            setAutoPickupItem(this)
            setReturnItem(this)
            setDeleteItem(this)
        }
    }

    fun setProperties(menu: Chest) {
        menu.rows(3)
        menu.handLocked(true)
        menu.map(
            "#########",
            "NDA     E",
            "####I###R"
        )
        menu.onClick {
            it.isCancelled = true
        }
    }

    fun setInfomationItem(menu: Chest) {
        menu.set('I') {
            buildItem(XMaterial.BOOK) {
                name = opener.asLangText("vaults-admin-info-name")
                lore += opener.asLangTextList(
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

    fun setNameItem(menu: Chest) {
        menu.set('N') {
            buildItem(XMaterial.PAPER) {
                name = opener.asLangText("vaults-admin-reset-name")
            }
        }
        menu.onClick('N') { event ->
            opener.closeInventory()
            opener.sendLang("vaults-admin-input-name")
            opener.nextChat {
                val result = vault.rename(it)
                when (result.reason) {
                    "vault_name_invalid" -> opener.sendLang("vaults-admin-reset-name-invalid")
                    "vault_already_exists" -> opener.sendLang("vaults-admin-reset-name-existed")
                    "vault_name_color" -> opener.sendLang("vaults-admin-reset-name-color")
                    "vault_name_length" -> opener.sendLang("vaults-admin-reset-name-length")
                    null -> {
                        opener.sendLang("vaults-admin-reset-name-succeed")
                        sync {
                            opener.closeInventory()
                            root?.open()
                        }
                    }
                }
            }
        }
    }

    fun setDescItem(menu: Chest) {
        menu.set('D') {
            buildItem(XMaterial.PAPER) {
                name = opener.asLangText("vaults-admin-reset-desc")
            }
        }
        menu.onClick('D') { event ->
            opener.closeInventory()
            opener.sendLang("vaults-admin-input-desc")
            opener.nextChat {
                vault.desc = it
                vault.updatedAt = System.currentTimeMillis()

                val table = com.faithl.zephyrion.storage.DatabaseConfig.vaultsTable
                val dataSource = com.faithl.zephyrion.storage.DatabaseConfig.dataSource
                table.update(dataSource) {
                    set("description", vault.desc)
                    set("updated_at", vault.updatedAt)
                    where { "id" eq vault.id }
                }

                opener.sendLang("vaults-admin-reset-desc-succeed")
                sync {
                 opener.closeInventory()
                 root?.open()
                }
            }
        }
    }

    fun setAutoPickupItem(menu: Chest) {
        menu.set('A') {
            buildItem(XMaterial.HOPPER) {
                name = opener.asLangText("vaults-admin-auto-pickup")
                lore += opener.asLangTextList("vaults-admin-auto-pickup-desc")
            }
        }
        menu.onClick('A') {
            ListAutoPickups(opener, vault, this).open()
        }
    }

    fun setDeleteItem(menu: Chest) {
        menu.set('E') {
            buildItem(XMaterial.BARRIER) {
                name = opener.asLangText("vaults-admin-delete")
            }
        }
        menu.onClick('E') { event ->
            opener.closeInventory()
            opener.sendLang("vaults-admin-delete-tip")
            opener.nextChat {
                if (it == "Y") {
                    val itemsTable = com.faithl.zephyrion.storage.DatabaseConfig.itemsTable
                    val settingsTable = com.faithl.zephyrion.storage.DatabaseConfig.settingsTable
                    val autoPickupsTable = com.faithl.zephyrion.storage.DatabaseConfig.autoPickupsTable
                    val vaultsTable = com.faithl.zephyrion.storage.DatabaseConfig.vaultsTable
                    val quotasTable = com.faithl.zephyrion.storage.DatabaseConfig.quotasTable
                    val dataSource = com.faithl.zephyrion.storage.DatabaseConfig.dataSource

                    val user = com.faithl.zephyrion.api.ZephyrionAPI.getUserData(vault.workspace.owner)
                    user.sizeUsed -= vault.size

                    itemsTable.delete(dataSource) {
                        where { "vault_id" eq vault.id }
                    }

                    settingsTable.delete(dataSource) {
                        where { "vault_id" eq vault.id }
                    }

                    autoPickupsTable.delete(dataSource) {
                        where { "vault_id" eq vault.id }
                    }

                    vaultsTable.delete(dataSource) {
                        where { "id" eq vault.id }
                    }

                    quotasTable.update(dataSource) {
                        set("size_used", user.sizeUsed)
                        where { "player" eq vault.workspace.owner }
                    }

                    opener.sendLang("vaults-admin-delete-succeed")
                } else {
                    opener.sendLang("vaults-admin-delete-canceled")
                }
                sync { root?.open() }
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

    override fun open() {
        opener.openInventory(build())
    }

    override fun title(): String {
        return opener.asLangText("vaults-admin-title")
    }

}