package com.faithl.zephyrion.core.ui.workspace

import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.core.models.Workspace
import com.faithl.zephyrion.core.models.Workspaces
import com.faithl.zephyrion.core.ui.SearchUI
import com.faithl.zephyrion.core.ui.search.Search
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
import taboolib.module.ui.type.Linked
import taboolib.platform.util.*

class ListWorkspaces(val owner: Player) : SearchUI() {

    val workspaces = mutableListOf<Workspace>()
    val searchItems = mutableListOf<SearchItem>()
    val params = mutableMapOf<String, String>()
    val searchUI = Search(owner, searchItems, this)

    init {
        addSearchItems("name")
        addSearchItems("desc")
        addSearchItems("member")
    }

    override fun search() {
        workspaces.clear()
        workspaces.addAll(ZephyrionAPI.getJoinedWorkspaces(owner))
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
        workspaces.sortBy { it.owner == owner.uniqueId.toString() }
    }

    override fun build(): Inventory {
        search()
        return buildMenu<Linked<Workspace>>(title()) {
            setLinkedMenuProperties(this)
            setRows6SplitBlock(this)
            setElements(this)
            setCreateItem(this)
            setSearchItem(this)
            setCloseItem(this)
            setPageTurnItems(this)
        }
    }

    fun setElements(menu: Linked<Workspace>) {
        menu.elements { workspaces }
        menu.onGenerate { _, element, _, _ ->
            if (element.owner == owner.uniqueId.toString()) {
                adminItem(element)
            } else {
                memberItem(element)
            }
        }
        menu.onClick { event, element ->
            if (event.clickEvent().isLeftClick) {
                ListVaults(owner, element, this).open(event.clicker)
            } else if (event.clickEvent().isRightClick && element.owner == owner.uniqueId.toString()) {
                AdminWorkspace(owner, element, this).open(event.clicker)
            }
        }
    }

    fun memberItem(workspace: Workspace): ItemStack {
        if (workspace.type == Workspaces.Type.INDEPENDENT) {
            return buildItem(XMaterial.BOOK) {
                name = owner.asLangText("workspace-main-item-name", owner.asLangText("independent-workspace"))
                lore += owner.asLangTextList(
                    "workspace-main-item-member-desc",
                    workspace.id,
                    owner.asLangText("independent-workspace-desc"),
                    owner.name,
                    workspace.getCreatedAt(),
                    workspace.getUpdatedAt(),
                    workspace.type.toString(),
                    "[${owner.name}]"
                )
            }
        }
        return buildItem(XMaterial.BOOK) {
            name = owner.asLangText("workspace-main-item-name", workspace.name)
            lore += owner.asLangTextList(
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
            name = owner.asLangText("workspace-main-item-name", workspace.name)
            lore += owner.asLangTextList(
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

    fun setCreateItem(menu: Linked<Workspace>) {
        menu.set(45) {
            buildItem(XMaterial.STICK) {
                val user = ZephyrionAPI.getUserData(owner.uniqueId.toString())
                name = owner.asLangText("workspace-main-create")
                lore += owner.asLangTextList("workspace-main-create-desc", user.workspaceUsed, user.workspaceQuotas)
            }
        }
        menu.onClick(45) {
            CreateWorkspace(owner, this).open(it.clicker)
        }
    }

    fun setCloseItem(menu: Linked<Workspace>) {
        menu.set(53) {
            buildItem(XMaterial.RED_STAINED_GLASS_PANE) {
                name = owner.asLangText("workspace-main-close")
            }
        }
        menu.onClick(53) {
            owner.closeInventory()
        }
    }

    fun setPageTurnItems(menu: Linked<Workspace>) {
        menu.setPreviousPage(48) { _, hasPreviousPage ->
            if (hasPreviousPage) {
                buildItem(XMaterial.ARROW) {
                    name = owner.asLangText("workspace-main-prev-page")
                }
            } else {
                buildItem(XMaterial.BARRIER) {
                    name = owner.asLangText("workspace-main-prev-page-disabled")
                }
            }
        }
        menu.setNextPage(50) { _, hasNextPage ->
            if (hasNextPage) {
                buildItem(XMaterial.ARROW) {
                    name = owner.asLangText("workspace-main-next-page")
                }
            } else {
                buildItem(XMaterial.BARRIER) {
                    name = owner.asLangText("workspace-main-next-page-disabled")
                }
            }
        }
    }

    fun setSearchItem(menu: Linked<Workspace>) {
        menu.set(49) {
            buildItem(XMaterial.COMPASS) {
                name = owner.asLangText("workspace-main-search")
            }
        }
        menu.onClick(49) {
            searchUI.open(it.clicker)
        }
    }

    fun addSearchItems(name: String) {
        searchItems += SearchItem(
            owner.asLangText("workspace-main-search-by-${name}-name"),
            owner.asLangText("workspace-main-search-by-${name}-desc")
        ) { player ->
            owner.closeInventory()
            owner.sendLang("workspace-main-search-by-${name}-input")
            owner.nextChat {
                params[name] = it
                sync {
                    searchUI.open(player)
                }
            }
        }
    }

    override fun title(): String {
        return if (params.isNotEmpty()) {
            owner.asLangText("workspace-main-title-with-search")
        } else {
            owner.asLangText("workspace-main-title")
        }
    }

    override fun open(opener: Player) {
        if (owner != opener && !ZephyrionAPI.isPluginAdmin(opener)) {
            return
        }
        opener.openInventory(build())
    }

}