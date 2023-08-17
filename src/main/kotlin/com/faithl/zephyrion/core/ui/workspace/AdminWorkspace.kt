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
import taboolib.module.ui.type.Basic
import taboolib.platform.util.*

class AdminWorkspace(val owner: Player, val workspace: Workspace, val root: UI) : UI() {

    override fun build(): Inventory {
        return buildMenu<Basic>(title()) {
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

    fun setProperties(menu: Basic) {
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

    fun setInfomationItem(menu: Basic) {
        menu.set('I') {
            buildItem(XMaterial.BOOK) {
                name = owner.asLangText("workspace-admin-info-name")
                lore += owner.asLangTextList(
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

    fun setNameItem(menu: Basic) {
        menu.set('N') {
            buildItem(XMaterial.PAPER) {
                name = owner.asLangText("workspace-admin-reset-name")
            }
        }
        menu.onClick('N') { event ->
            owner.closeInventory()
            owner.sendLang("workspace-admin-input-name")
            owner.nextChat {
                sync {
                    val result = workspace.rename(it)
                    when (result.reason) {
                        "workspace_name_invalid" -> {
                            owner.sendLang("workspace-admin-reset-name-invalid")
                        }

                        "workspace_name_color" -> {
                            owner.sendLang("workspace-admin-reset-name-color")
                        }

                        "workspace_name_length" -> {
                            owner.sendLang("workspace-admin-reset-name-length")
                        }

                        "workspace_already_exists" -> {
                            owner.sendLang("workspace-admin-reset-name-existed")
                        }

                        null -> {
                            owner.sendLang("workspace-admin-reset-name-succeed")
                        }
                    }
                    open(event.clicker)
                }
            }
        }
    }

    fun setDescItem(menu: Basic) {
        menu.set('D') {
            buildItem(XMaterial.PAPER) {
                name = owner.asLangText("workspace-admin-reset-desc")
            }
        }
        menu.onClick('C') { event ->
            owner.closeInventory()
            owner.sendLang("workspace-admin-input-desc")
            owner.nextChat {
                sync {
                    transaction {
                        workspace.desc = it
                        workspace.updatedAt = System.currentTimeMillis()
                    }
                    owner.sendLang("workspace-admin-reset-desc-succeed")
                    open(event.clicker)
                }
            }
        }
    }

    fun setMembersItem(menu: Basic) {
        menu.set('M') {
            buildItem(XMaterial.PLAYER_HEAD) {
                name = owner.asLangText("workspace-admin-members")
            }
        }
        menu.onClick('M') {
            ListMembers(owner, workspace, this).open(it.clicker)
        }
    }

    fun setDeleteItem(menu: Basic) {
        menu.set('E') {
            buildItem(XMaterial.BARRIER) {
                name = owner.asLangText("workspace-admin-delete")
            }
        }
        menu.onClick('E') { event ->
            owner.closeInventory()
            owner.sendLang("workspace-admin-delete-tip")
            owner.nextChat {
                if (it == "Y") {
                    workspace.delete()
                    owner.sendLang("workspace-admin-delete-succeed")
                    sync {
                        root.open(event.clicker)
                    }
                } else {
                    owner.sendMessage("workspace-admin-delete-canceled")
                    sync {
                        root.open(event.clicker)
                    }
                }
            }
        }
    }

    fun setReturnItem(menu: Basic) {
        menu.set('R') {
            buildItem(XMaterial.RED_STAINED_GLASS_PANE) {
                name = owner.asLangText("workspace-admin-return")
            }
        }
        menu.onClick('R') {
            root.open(it.clicker)
        }
    }

    override fun open(opener: Player) {
        opener.openInventory(build())
    }

    override fun title(): String {
        return owner.asLangText("workspace-admin-title")
    }

}