package com.faithl.zephyrion.core.ui.workspace

import com.faithl.zephyrion.Zephyrion
import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.core.models.Workspace
import com.faithl.zephyrion.core.ui.SearchUI
import com.faithl.zephyrion.core.ui.UI
import com.faithl.zephyrion.core.ui.search.Search
import com.faithl.zephyrion.core.ui.search.SearchItem
import com.faithl.zephyrion.core.ui.setLinkedMenuProperties
import com.faithl.zephyrion.core.ui.setRows6SplitBlock
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import taboolib.common.util.sync
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.buildMenu
import taboolib.module.ui.type.Linked
import taboolib.platform.util.asLangText
import taboolib.platform.util.buildItem
import taboolib.platform.util.nextChat
import taboolib.platform.util.sendLang

class ListMembers(val owner: Player, val workspace: Workspace, val root: UI) : SearchUI() {

    val members = mutableListOf<OfflinePlayer>()
    val searchItems = mutableListOf<SearchItem>()
    val params = mutableMapOf<String, String>()
    val searchUI = Search(owner, searchItems, this)

    init {
        addSearchItems("name")
    }

    override fun search() {
        members.clear()
        members.addAll(workspace.getMembers())
        if (params.isEmpty()) {
            return
        }
        params["name"]?.let {
            members.retainAll { offlinePlayer ->
                offlinePlayer.name != null && offlinePlayer.name!!.contains(it)
            }
        }
        sort()
    }

    fun sort() {
        members.sortBy { it.uniqueId.toString() == owner.uniqueId.toString() }
        members.sortBy { it.isOnline }
    }

    override fun build(): Inventory {
        search()
        return buildMenu<Linked<OfflinePlayer>>(title()) {
            setLinkedMenuProperties(this)
            setRows6SplitBlock(this)
            setElements(this)
            setAddItem(this)
            setReturnItem(this)
            setSearchItem(this)
            setPageTurnItems(this)
        }
    }

    fun setElements(menu: Linked<OfflinePlayer>) {
        menu.elements { members }
        menu.onGenerate { _, element, _, _ ->
            buildItem(XMaterial.PLAYER_HEAD) {
                name = owner.asLangText("workspace-members-item-name", element.name!!)
                lore += if (element.isOnline) {
                    owner.asLangText("workspace-members-item-lore-online")
                } else {
                    owner.asLangText("workspace-members-item-lore-offline")
                }
                lore += owner.asLangText("workspace-members-item-lore-remove")
            }
        }
        menu.onClick { event, element ->
            if (event.clickEvent().isLeftClick) {
                if (workspace.owner == element.uniqueId.toString()) {
                    owner.sendLang("workspace-members-remove-owner")
                    return@onClick
                }
                val result = ZephyrionAPI.removeMember(workspace, element)
                if (result) {
                    owner.sendLang("workspace-members-remove-succeed")
                } else {
                    owner.sendLang("workspace-members-remove-not-member")
                }
                sync {
                    open(event.clicker)
                }
            }
        }
    }

    fun setPageTurnItems(menu: Linked<OfflinePlayer>) {
        menu.setPreviousPage(48) { _, hasPreviousPage ->
            if (hasPreviousPage) {
                buildItem(XMaterial.ARROW) {
                    name = owner.asLangText("workspace-members-prev-page")
                }
            } else {
                buildItem(XMaterial.BARRIER) {
                    name = owner.asLangText("workspace-members-prev-page-disabled")
                }
            }
        }
        menu.setNextPage(50) { _, hasNextPage ->
            if (hasNextPage) {
                buildItem(XMaterial.ARROW) {
                    name = owner.asLangText("workspace-members-next-page")
                }
            } else {
                buildItem(XMaterial.BARRIER) {
                    name = owner.asLangText("workspace-members-next-page-disabled")
                }
            }
        }
    }

    fun setAddItem(menu: Linked<OfflinePlayer>) {
        menu.set(45) {
            buildItem(XMaterial.STICK) {
                name = owner.asLangText("workspace-members-add")
            }
        }
        menu.onClick(45) { event ->
            owner.closeInventory()
            owner.sendLang("workspace-members-add-input")
            owner.nextChat {
                val target = Bukkit.getPlayer(it)
                if (target == null) {
                    owner.sendLang("workspace-members-add-offline")
                    sync {
                        open(event.clicker)
                    }
                    return@nextChat
                }
                val result = ZephyrionAPI.addMember(workspace, target)
                if (result) {
                    owner.sendLang("workspace-members-add-succeed")
                } else {
                    owner.sendLang("workspace-members-add-existed")
                }
                sync {
                    open(event.clicker)
                }
            }
        }
    }

    fun setReturnItem(menu: Linked<OfflinePlayer>) {
        menu.set(53) {
            buildItem(XMaterial.RED_STAINED_GLASS_PANE) {
                name = owner.asLangText("workspace-members-return")
            }
        }
        menu.onClick(53) {
            root.open(it.clicker)
        }
    }

    fun setSearchItem(menu: Linked<OfflinePlayer>) {
        menu.set(49) {
            buildItem(XMaterial.COMPASS) {
                name = owner.asLangText("workspace-members-search")
            }
        }
        menu.onClick(49) {
            val permission = Zephyrion.settings.getString("permissions.add-member")
            if (permission != null && !owner.hasPermission(permission)) {
                owner.sendLang("workspace-members-add-no-perm")
                return@onClick
            }
            searchUI.open(it.clicker)
        }
    }

    override fun open(opener: Player) {
        opener.openInventory(build())
    }

    override fun title(): String {
        return if (params.isNotEmpty()) {
            owner.asLangText("workspace-members-title-with-search")
        } else {
            owner.asLangText("workspace-members-title")
        }
    }

    fun addSearchItems(name: String) {
        searchItems += SearchItem(
            owner.asLangText("workspace-members-search-by-${name}-name"),
            owner.asLangText("workspace-members-search-by-${name}-desc")
        ) { player ->
            owner.closeInventory()
            owner.sendLang("workspace-members-search-by-${name}-input")
            owner.nextChat {
                params[name] = it
                sync {
                    searchUI.open(player)
                }
            }
        }
    }
}