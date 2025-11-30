package com.faithl.zephyrion.core.ui.workspace

import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.core.models.Workspace
import com.faithl.zephyrion.core.models.WorkspaceType
import com.faithl.zephyrion.core.ui.SearchUI
import com.faithl.zephyrion.core.ui.UI
import com.faithl.zephyrion.core.ui.search.SearchItem
import com.faithl.zephyrion.core.ui.setLinkedMenuProperties
import com.faithl.zephyrion.core.ui.setRows6SplitBlock
import com.faithl.zephyrion.core.ui.vault.ListVaults
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import taboolib.common.util.sync
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.buildMenu
import taboolib.module.ui.type.PageableChest
import taboolib.module.ui.type.impl.PageableChestImpl
import taboolib.platform.util.*

/**
 * @param opener 打开UI的玩家（查看者）
 * @param targetPlayer 目标玩家（被查看的玩家），为null时表示查看自己的工作空间
 */
class ListWorkspaces(override val opener: Player, val targetPlayer: Player? = null) : SearchUI() {

    override val root: UI? = null
    val dataOwner: Player = targetPlayer ?: opener
    val workspaces = mutableListOf<Workspace>()

    init {
        addSearchItems("name")
        addSearchItems("desc")
        addSearchItems("member")
    }

    override fun search() {
        workspaces.clear()
        workspaces.addAll(ZephyrionAPI.getJoinedWorkspaces(dataOwner))
        ZephyrionAPI.getIndependentWorkspace()?.let {
            workspaces.add(it)
        }
        if (params.isEmpty()) {
            return
        }
        params["name"]?.let {
            workspaces.retainAll { workspace ->
                workspace.name.contains(it)
            }
        }
        params["desc"]?.let {
            workspaces.retainAll { workspace ->
                workspace.desc?.contains(it) ?: false
            }
        }
        params["member"]?.let {
            workspaces.retainAll { workspace ->
                workspace.getMembersName().contains(it)
            }
        }
        sort()
    }

    fun sort() {
        workspaces.sortBy { it.owner == dataOwner.uniqueId.toString() }
    }

    override fun build(): Inventory {
        return buildMenu<PageableChestImpl<Workspace>>(title()) {
            setLinkedMenuProperties(this)
            setRows6SplitBlock(this)
            setElements(this)
            setCreateItem(this)
            setSearchItem(this)
            setReturnItem(this)
            setPageTurnItems(this)
        }
    }

    fun setElements(menu: PageableChest<Workspace>) {
        menu.elements { workspaces }
        menu.onGenerate { _, element, _, _ ->
            if (element.owner == dataOwner.uniqueId.toString()) {
                adminItem(element)
            } else {
                memberItem(element)
            }
        }
        menu.onClick { event, element ->
            if (event.clickEvent().isLeftClick) {
                ListVaults(event.clicker, element, this).open()
            } else if (event.clickEvent().isRightClick && element.owner == dataOwner.uniqueId.toString()) {
                AdminWorkspace(event.clicker, element, this).open()
            }
        }
    }

    fun memberItem(workspace: Workspace): ItemStack {
        if (workspace.type == WorkspaceType.INDEPENDENT) {
            return buildItem(XMaterial.BOOK) {
                name = opener.asLangText("workspace-main-item-name", opener.asLangText("independent-workspace"))
                lore += opener.asLangTextList(
                    "workspace-main-item-member-desc",
                    workspace.id,
                    opener.asLangText("independent-workspace-desc"),
                    opener.name,
                    workspace.getCreatedAt(),
                    workspace.getUpdatedAt(),
                    workspace.type.toString(),
                    "[${opener.name}]"
                )
            }
        }
        return buildItem(XMaterial.BOOK) {
            name = opener.asLangText("workspace-main-item-name", workspace.name)
            lore += opener.asLangTextList(
                "workspace-main-item-member-desc",
                workspace.id,
                workspace.desc ?: "",
                workspace.getOwner().name!!,
                workspace.getCreatedAt(),
                workspace.getUpdatedAt(),
                workspace.type.toString(),
                workspace.getMembersName()
            )
        }
    }

    fun adminItem(workspace: Workspace): ItemStack {
        return buildItem(XMaterial.ENCHANTED_BOOK) {
            name = opener.asLangText("workspace-main-item-name", workspace.name)
            lore += opener.asLangTextList(
                "workspace-main-item-admin-desc",
                workspace.id,
                workspace.desc ?: "",
                workspace.getOwner().name!!,
                workspace.getCreatedAt(),
                workspace.getUpdatedAt(),
                workspace.type.toString(),
                workspace.getMembersName()
            )
        }
    }

    fun setCreateItem(menu: PageableChest<Workspace>) {
        if (targetPlayer != null) {
            return
        }

        menu.set(45) {
            buildItem(XMaterial.STICK) {
                val user = ZephyrionAPI.getUserData(dataOwner.uniqueId.toString())
                name = opener.asLangText("workspace-main-create")
                lore += opener.asLangTextList("workspace-main-create-desc", user.workspaceUsed, user.workspaceQuotas)
            }
        }
        menu.onClick(45) {
            CreateWorkspace(it.clicker, this).open()
        }
    }

    fun setPageTurnItems(menu: PageableChest<Workspace>) {
        menu.setPreviousPage(48) { _, hasPreviousPage ->
            if (hasPreviousPage) {
                buildItem(XMaterial.ARROW) {
                    name = opener.asLangText("workspace-main-prev-page")
                }
            } else {
                buildItem(XMaterial.BARRIER) {
                    name = opener.asLangText("workspace-main-prev-page-disabled")
                }
            }
        }
        menu.setNextPage(50) { _, hasNextPage ->
            if (hasNextPage) {
                buildItem(XMaterial.ARROW) {
                    name = opener.asLangText("workspace-main-next-page")
                }
            } else {
                buildItem(XMaterial.BARRIER) {
                    name = opener.asLangText("workspace-main-next-page-disabled")
                }
            }
        }
    }

    fun addSearchItems(name: String) {
        searchItems += SearchItem(
            opener.asLangText("workspace-main-search-by-${name}-name"),
            opener.asLangText("workspace-main-search-by-${name}-desc")
        ) { player ->
            opener.closeInventory()
            opener.sendLang("workspace-main-search-by-${name}-input")
            opener.nextChat {
                params[name] = it
                sync { searchUI.open() }
            }
        }
    }

    override fun title(): String {
        return if (params.isNotEmpty()) {
            opener.asLangText("workspace-main-title-with-search")
        } else if (targetPlayer != null) {
            opener.asLangText("workspace-main-title") + " - ${dataOwner.name}"
        } else {
            opener.asLangText("workspace-main-title")
        }
    }

    override fun open() {
        if (targetPlayer != null && !ZephyrionAPI.isPluginAdmin(opener)) {
            opener.sendLang("no-permission")
            return
        }
        try {
            search()
            opener.openInventory(build())
        } catch (e: Exception) {
            e.printStackTrace()
            opener.sendLang("ui-load-error")
        }
    }

}