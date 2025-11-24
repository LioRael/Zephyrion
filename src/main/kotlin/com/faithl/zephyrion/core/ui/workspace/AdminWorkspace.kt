package com.faithl.zephyrion.core.ui.workspace

import com.faithl.zephyrion.core.models.Workspace
import com.faithl.zephyrion.core.ui.UI
import com.faithl.zephyrion.core.ui.setSplitBlock
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.jetbrains.exposed.sql.transactions.transaction
import taboolib.common.util.sync
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.buildMenu
import taboolib.module.ui.type.Chest
import taboolib.module.ui.type.impl.ChestImpl
import taboolib.platform.util.*

class AdminWorkspace(override val opener: Player, val workspace: Workspace, val root: UI) : UI() {

    override fun build(): Inventory {
        return buildMenu<ChestImpl>(title()) {
            setProperties(this)
            setInfomationItem(this)
            setSplitBlock(this)
            setNameItem(this)
            setDescItem(this)
            setMembersItem(this)
            setDeleteItem(this)
            setReturnItem(this)
        }
    }

    fun setProperties(menu: Chest) {
        menu.rows(3)
        menu.handLocked(true)
        menu.map(
            "#########",
            "NDM     E",
            "####I###R"
        )
        menu.onClick {
            it.isCancelled = true
        }
    }

    fun setInfomationItem(menu: Chest) {
        menu.set('I') {
            buildItem(XMaterial.BOOK) {
                name = opener.asLangText("workspace-admin-info-name")
                lore += opener.asLangTextList(
                    "workspace-admin-info-desc",
                    workspace.id,
                    workspace.name,
                    workspace.desc ?: "",
                    workspace.getOwner().name!!,
                    workspace.getCreatedAt(),
                    workspace.getUpdatedAt(),
                    workspace.getMembersName()
                )
            }
        }
    }

    fun setNameItem(menu: Chest) {
        menu.set('N') {
            buildItem(XMaterial.PAPER) {
                name = opener.asLangText("workspace-admin-reset-name")
            }
        }
        menu.onClick('N') { event ->
            opener.closeInventory()
            opener.sendLang("workspace-admin-input-name")
            opener.nextChat {
                sync {
                    val result = workspace.rename(it)
                    when (result.reason) {
                        "workspace_name_invalid" -> {
                            opener.sendLang("workspace-admin-reset-name-invalid")
                        }

                        "workspace_name_color" -> {
                            opener.sendLang("workspace-admin-reset-name-color")
                        }

                        "workspace_name_length" -> {
                            opener.sendLang("workspace-admin-reset-name-length")
                        }

                        "workspace_already_exists" -> {
                            opener.sendLang("workspace-admin-reset-name-existed")
                        }

                        null -> {
                            opener.sendLang("workspace-admin-reset-name-succeed")
                        }
                    }
                    open()
                }
            }
        }
    }

    fun setDescItem(menu: Chest) {
        menu.set('D') {
            buildItem(XMaterial.PAPER) {
                name = opener.asLangText("workspace-admin-reset-desc")
            }
        }
        menu.onClick('C') { event ->
            opener.closeInventory()
            opener.sendLang("workspace-admin-input-desc")
            opener.nextChat {
                sync {
                    transaction {
                        workspace.desc = it
                        workspace.updatedAt = System.currentTimeMillis()
                    }
                    opener.sendLang("workspace-admin-reset-desc-succeed")
                    open()
                }
            }
        }
    }

    fun setMembersItem(menu: Chest) {
        menu.set('M') {
            buildItem(XMaterial.PLAYER_HEAD) {
                name = opener.asLangText("workspace-admin-members")
            }
        }
        menu.onClick('M') {
            ListMembers(opener, workspace, this).open()
        }
    }

    fun setDeleteItem(menu: Chest) {
        menu.set('E') {
            buildItem(XMaterial.BARRIER) {
                name = opener.asLangText("workspace-admin-delete")
            }
        }
        menu.onClick('E') { event ->
            opener.closeInventory()
            opener.sendLang("workspace-admin-delete-tip")
            opener.nextChat {
                if (it == "Y") {
                    workspace.delete()
                    opener.sendLang("workspace-admin-delete-succeed")
                    sync {
                        root.open()
                    }
                } else {
                    opener.sendLang("workspace-admin-delete-canceled")
                    sync {
                        root.open()
                    }
                }
            }
        }
    }

    fun setReturnItem(menu: Chest) {
        menu.set('R') {
            buildItem(XMaterial.RED_STAINED_GLASS_PANE) {
                name = opener.asLangText("workspace-admin-return")
            }
        }
        menu.onClick('R') {
            root.open()
        }
    }

    override fun open() {
        opener.openInventory(build())
    }

    override fun title(): String {
        return opener.asLangText("workspace-admin-title")
    }

}