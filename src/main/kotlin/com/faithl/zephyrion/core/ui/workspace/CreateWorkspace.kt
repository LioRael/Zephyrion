package com.faithl.zephyrion.core.ui.workspace

import com.faithl.zephyrion.Zephyrion
import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.core.models.WorkspaceType
import com.faithl.zephyrion.core.ui.UI
import com.faithl.zephyrion.core.ui.setSplitBlock
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import taboolib.common.platform.function.submit
import taboolib.common.util.sync
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.buildMenu
import taboolib.module.ui.type.Chest
import taboolib.module.ui.type.impl.ChestImpl
import taboolib.module.ui.type.impl.PageableChestImpl
import taboolib.platform.util.asLangText
import taboolib.platform.util.buildItem
import taboolib.platform.util.nextChat
import taboolib.platform.util.sendLang

/**
 * @param opener 打开UI的玩家
 */
class CreateWorkspace(override val opener: Player,override val root: UI) : UI() {

    var name: String? = null
    var description: String? = null
    var type: WorkspaceType? = null

    override fun build(): Inventory {
        return buildMenu<ChestImpl>(title()) {
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

    class TypeUI(override val opener: Player,override val root: CreateWorkspace) : UI() {
        override fun build(): Inventory {
            return buildMenu<PageableChestImpl<WorkspaceType>>(title()) {
                rows(1)
                handLocked(true)
                slots(listOf(0, 1, 2))
                elements {
                    WorkspaceType.entries
                }
                onGenerate { player, element, _, _ ->
                    when (element) {
                        WorkspaceType.INDEPENDENT -> {
                            buildItem(XMaterial.PAPER) {
                                name = player.asLangText("independent-workspace")
                                lore += player.asLangText("independent-workspace-desc")
                            }
                        }

                        WorkspaceType.PUBLIC -> {
                            buildItem(XMaterial.PAPER) {
                                name = player.asLangText("public-workspace")
                                lore += player.asLangText("public-workspace-desc")
                            }
                        }

                        WorkspaceType.PRIVATE -> {
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
                        WorkspaceType.INDEPENDENT -> {
                            if (opener.hasPermission(Zephyrion.permissions.getString("create-independent-workspace")!!)) {
                                root.type = element
                                root.open()
                            } else {
                                opener.sendLang("workspace-create-type-no-permission")
                            }
                        }

                        WorkspaceType.PUBLIC -> {
                            if (opener.hasPermission(Zephyrion.permissions.getString("create-public-workspace")!!)) {
                                root.type = element
                                root.open()
                            } else {
                                opener.sendLang("workspace-create-type-no-permission")
                            }
                        }

                        WorkspaceType.PRIVATE -> {
                            if (opener.hasPermission(Zephyrion.permissions.getString("create-private-workspace")!!)) {
                                root.type = element
                                root.open()
                            } else {
                                opener.sendLang("workspace-create-type-no-permission")
                            }
                        }
                    }
                }
            }
        }

        override fun open() {
            val inv = build()
            opener.openInventory(inv)
        }

        override fun title(): String {
            return opener.asLangText("workspace-create-type-choose-title")
        }

    }

    fun setTypeItem(menu: Chest) {
        menu.set('T') {
            buildItem(XMaterial.PAPER) {
                name = opener.asLangText("workspace-create-type-name")
                lore += opener.asLangText("workspace-create-type-current", type.toString())
            }
        }
        menu.onClick('T') {
            TypeUI(opener, this).open()
        }
    }

    fun setProperties(menu: Chest) {
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

    fun setInfomationItem(menu: Chest) {
        menu.set('I') {
            buildItem(XMaterial.BOOK) {
                name = opener.asLangText("workspace-create-info-name")
                this@CreateWorkspace.name?.let {
                    lore += opener.asLangText("workspace-create-info-lore-name", it)
                }
                description?.let {
                    lore += opener.asLangText("workspace-create-info-lore-desc", it)
                }
                lore += opener.asLangText("workspace-create-info-lore-member", opener.name)
            }
        }
    }

    fun setNameItem(menu: Chest) {
        menu.set('N') {
            buildItem(XMaterial.PAPER) {
                name = if (this@CreateWorkspace.name != null) {
                    opener.asLangText("workspace-create-reset-name")
                } else {
                    opener.asLangText("workspace-create-set-name")
                }
            }
        }
        menu.onClick('N') { event ->
            opener.closeInventory()
            opener.sendLang("workspace-create-input-name")
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
                    opener.asLangText("workspace-create-reset-desc")
                } else {
                    opener.asLangText("workspace-create-set-desc")
                }
            }
        }
        menu.onClick('D') { event ->
            opener.closeInventory()
            opener.sendLang("workspace-create-input-desc")
            opener.nextChat {
                description = it
                sync { open() }
            }
        }
    }

    fun setConfirmItem(menu: Chest) {
        menu.set('C') {
            buildItem(XMaterial.GREEN_STAINED_GLASS_PANE) {
                name = opener.asLangText("workspace-create-confirm")
            }
        }
        menu.onClick('C') {
            val result = ZephyrionAPI.createWorkspace(opener.uniqueId.toString(), name, type, description)
            when (result.reason) {
                "workspace_quota_exceeded" -> opener.sendLang("workspace-create-quota-exceeded")

                "workspace_already_exists" -> opener.sendLang("workspace-create-name-existed")

                "workspace_name_invalid" -> opener.sendLang("workspace-create-name-invalid")

                "workspace_name_color" -> opener.sendLang("workspace-create-name-color")

                "workspace_name_length" -> opener.sendLang("workspace-create-name-length")

                "workspace_name_blacklist" -> opener.sendLang("workspace-create-name-blacklist")

                "workspace_type_invalid" -> opener.sendLang("workspace-create-type-invalid")

                null -> {
                    opener.sendLang("workspace-create-succeed")
                    submit(delay = 1) {
                        root.open()
                    }
                }
            }
        }
    }

    override fun setReturnItem(menu: Chest) {
        menu.set('R') {
            buildItem(XMaterial.RED_STAINED_GLASS_PANE) {
                name = opener.asLangText("ui-item-name-return")
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
        return opener.asLangText("workspace-create-title")
    }

}