package com.faithl.zephyrion.core.ui.workspace

import com.faithl.zephyrion.Zephyrion
import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.core.models.Workspaces
import com.faithl.zephyrion.core.ui.UI
import com.faithl.zephyrion.core.ui.setSplitBlock
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import taboolib.common.util.sync
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.buildMenu
import taboolib.module.ui.type.Basic
import taboolib.module.ui.type.Linked
import taboolib.platform.util.asLangText
import taboolib.platform.util.buildItem
import taboolib.platform.util.nextChat
import taboolib.platform.util.sendLang

class CreateWorkspace(val owner: Player, val root: UI) : UI() {

    var name: String? = null
    var description: String? = null
    var type: Workspaces.Type? = null

    override fun build(): Inventory {
        return buildMenu<Basic>(title()) {
            setProperties(this)
            setInfomationItem(this)
            setSplitBlock(this)
            setNameItem(this)
            setDescItem(this)
            setReturnItem(this)
            setConfirmItem(this)
            setTypeItem(this)
        }
    }

    class TypeUI(val opener: Player, val root: CreateWorkspace) : UI() {
        override fun build(): Inventory {
            return buildMenu<Linked<Workspaces.Type>>(title()) {
                rows(1)
                handLocked(true)
                slots(listOf(0, 1, 2))
                elements {
                    Workspaces.Type.entries
                }
                onGenerate { player, element, _, _ ->
                    when (element) {
                        Workspaces.Type.INDEPENDENT -> {
                            buildItem(XMaterial.PAPER) {
                                name = player.asLangText("independent-workspace")
                                lore += player.asLangText("independent-workspace-desc")
                            }
                        }

                        Workspaces.Type.PUBLIC -> {
                            buildItem(XMaterial.PAPER) {
                                name = player.asLangText("public-workspace")
                                lore += player.asLangText("public-workspace-desc")
                            }
                        }

                        Workspaces.Type.PRIVATE -> {
                            buildItem(XMaterial.PAPER) {
                                name = player.asLangText("private-workspace")
                                lore += player.asLangText("private-workspace-desc")
                            }
                        }
                    }
                }
                onClick { event, element ->
                    event.isCancelled = true
                    when (element) {
                        Workspaces.Type.INDEPENDENT -> {
                            if (opener.hasPermission(Zephyrion.permissions.getString("create-independent-workspace")!!)) {
                                root.type = element
                                root.open(opener)
                            } else {
                                opener.sendLang("workspace-create-type-no-permission")
                            }
                        }

                        Workspaces.Type.PUBLIC -> {
                            if (opener.hasPermission(Zephyrion.permissions.getString("create-public-workspace")!!)) {
                                root.type = element
                                root.open(opener)
                            } else {
                                opener.sendLang("workspace-create-type-no-permission")
                            }
                        }

                        Workspaces.Type.PRIVATE -> {
                            if (opener.hasPermission(Zephyrion.permissions.getString("create-private-workspace")!!)) {
                                root.type = element
                                root.open(opener)
                            } else {
                                opener.sendLang("workspace-create-type-no-permission")
                            }
                        }
                    }
                }
            }
        }

        override fun open(opener: Player) {
            val inv = build()
            opener.openInventory(inv)
        }

        override fun title(): String {
            return opener.asLangText("workspace-create-type-choose-title")
        }

    }

    fun setTypeItem(menu: Basic) {
        menu.set('T') {
            buildItem(XMaterial.PAPER) {
                name = owner.asLangText("workspace-create-type-name")
                lore += owner.asLangText("workspace-create-type-current", type.toString())
            }
        }
        menu.onClick('T') {
            TypeUI(owner, this).open(owner)
        }
    }

    fun setProperties(menu: Basic) {
        menu.rows(3)
        menu.handLocked(true)
        menu.map(
            "####I####",
            "NDT      ",
            "####C###R"
        )
        menu.onClick {
            it.isCancelled = true
        }
    }

    fun setInfomationItem(menu: Basic) {
        menu.set('I') {
            buildItem(XMaterial.BOOK) {
                name = owner.asLangText("workspace-create-info-name")
                this@CreateWorkspace.name?.let {
                    lore += owner.asLangText("workspace-create-info-lore-name", it)
                }
                description?.let {
                    lore += owner.asLangText("workspace-create-info-lore-desc", it)
                }
                lore += owner.asLangText("workspace-create-info-lore-member", owner.name)
            }
        }
    }

    fun setNameItem(menu: Basic) {
        menu.set('N') {
            buildItem(XMaterial.PAPER) {
                name = if (this@CreateWorkspace.name != null) {
                    owner.asLangText("workspace-create-reset-name")
                } else {
                    owner.asLangText("workspace-create-set-name")
                }
            }
        }
        menu.onClick('N') { event ->
            owner.closeInventory()
            owner.sendLang("workspace-create-input-name")
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
                    owner.asLangText("workspace-create-reset-desc")
                } else {
                    owner.asLangText("workspace-create-set-desc")
                }
            }
        }
        menu.onClick('D') { event ->
            owner.closeInventory()
            owner.sendLang("workspace-create-input-desc")
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
            buildItem(XMaterial.RED_STAINED_GLASS_PANE) {
                name = owner.asLangText("workspace-create-return")
            }
        }
        menu.onClick('R') {
            root.open(it.clicker)
        }
    }

    fun setConfirmItem(menu: Basic) {
        menu.set('C') {
            buildItem(XMaterial.GREEN_STAINED_GLASS_PANE) {
                name = owner.asLangText("workspace-create-confirm")
            }
        }
        menu.onClick('C') {
            val result = ZephyrionAPI.createWorkspace(owner.uniqueId.toString(), name, type, description)
            when (result.reason) {
                "workspace_quota_exceeded" -> owner.sendLang("workspace-create-quota-exceeded")

                "workspace_already_exists" -> owner.sendLang("workspace-create-name-existed")

                "workspace_name_invalid" -> owner.sendLang("workspace-create-name-invalid")

                "workspace_name_color" -> owner.sendLang("workspace-create-name-color")

                "workspace_name_length" -> owner.sendLang("workspace-create-name-length")

                "workspace_name_blacklist" -> owner.sendLang("workspace-create-name-blacklist")

                "workspace_type_invalid" -> owner.sendLang("workspace-create-type-invalid")

                null -> {
                    owner.sendLang("workspace-create-succeed")
                    root.open(it.clicker)
                }
            }
        }
    }

    override fun open(opener: Player) {
        opener.openInventory(build())
    }

    override fun title(): String {
        return owner.asLangText("workspace-create-title")
    }

}