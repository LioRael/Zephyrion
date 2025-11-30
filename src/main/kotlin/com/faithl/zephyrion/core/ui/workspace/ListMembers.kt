package com.faithl.zephyrion.core.ui.workspace

import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.core.models.Workspace
import com.faithl.zephyrion.core.ui.SearchUI
import com.faithl.zephyrion.core.ui.UI
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
import taboolib.module.ui.type.PageableChest
import taboolib.module.ui.type.impl.PageableChestImpl
import taboolib.platform.util.asLangText
import taboolib.platform.util.buildItem
import taboolib.platform.util.nextChat
import taboolib.platform.util.sendLang

class ListMembers(override val opener: Player, val workspace: Workspace, override val root: UI) : SearchUI() {

    val members = mutableListOf<OfflinePlayer>()

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
        members.sortBy { it.uniqueId.toString() == opener.uniqueId.toString() }
        members.sortBy { it.isOnline }
    }

    override fun build(): Inventory {
        return buildMenu<PageableChestImpl<OfflinePlayer>>(title()) {
            setLinkedMenuProperties(this)
            setRows6SplitBlock(this)
            setElements(this)
            setAddItem(this)
            setReturnItem(this)
            setSearchItem(this)
            setPageTurnItems(this)
        }
    }

    fun setElements(menu: PageableChest<OfflinePlayer>) {
        menu.elements { members }
        menu.onGenerate { _, element, _, _ ->
            buildItem(XMaterial.PLAYER_HEAD) {
                name = opener.asLangText("workspace-members-item-name", element.name!!)
                lore += if (element.isOnline) {
                    opener.asLangText("workspace-members-item-lore-online")
                } else {
                    opener.asLangText("workspace-members-item-lore-offline")
                }
                lore += opener.asLangText("workspace-members-item-lore-remove")
            }
        }
        menu.onClick { event, element ->
            if (event.clickEvent().isLeftClick) {
                if (workspace.owner == element.uniqueId.toString()) {
                    opener.sendLang("workspace-members-remove-opener")
                    return@onClick
                }
                val result = ZephyrionAPI.removeMember(workspace, element)
                if (result) {
                    opener.sendLang("workspace-members-remove-succeed")
                } else {
                    opener.sendLang("workspace-members-remove-not-member")
                }
                open()
            }
        }
    }

    fun setPageTurnItems(menu: PageableChest<OfflinePlayer>) {
        menu.setPreviousPage(48) { _, hasPreviousPage ->
            if (hasPreviousPage) {
                buildItem(XMaterial.ARROW) {
                    name = opener.asLangText("workspace-members-prev-page")
                }
            } else {
                buildItem(XMaterial.BARRIER) {
                    name = opener.asLangText("workspace-members-prev-page-disabled")
                }
            }
        }
        menu.setNextPage(50) { _, hasNextPage ->
            if (hasNextPage) {
                buildItem(XMaterial.ARROW) {
                    name = opener.asLangText("workspace-members-next-page")
                }
            } else {
                buildItem(XMaterial.BARRIER) {
                    name = opener.asLangText("workspace-members-next-page-disabled")
                }
            }
        }
    }

    fun setAddItem(menu: PageableChest<OfflinePlayer>) {
        menu.set(45) {
            buildItem(XMaterial.STICK) {
                name = opener.asLangText("workspace-members-add")
            }
        }
        menu.onClick(45) { event ->
            opener.closeInventory()
            opener.sendLang("workspace-members-add-input")
            opener.nextChat {
                val target = Bukkit.getPlayer(it)
                if (target == null) {
                    opener.sendLang("workspace-members-add-offline")
                    sync { open() }
                    return@nextChat
                }
                val result = ZephyrionAPI.addMember(workspace, target)
                if (result) {
                    opener.sendLang("workspace-members-add-succeed")
                } else {
                    opener.sendLang("workspace-members-add-existed")
                }
                sync { open() }
            }
        }
    }

    override fun open() {
        try {
            search()
            opener.openInventory(build())
        } catch (e: Exception) {
            e.printStackTrace()
            opener.sendLang("ui-load-error")
        }
    }

    override fun title(): String {
        return if (params.isNotEmpty()) {
            opener.asLangText("workspace-members-title-with-search")
        } else {
            opener.asLangText("workspace-members-title")
        }
    }

    fun addSearchItems(name: String) {
        searchItems += SearchItem(
            opener.asLangText("workspace-members-search-by-${name}-name"),
            opener.asLangText("workspace-members-search-by-${name}-desc")
        ) { player ->
            opener.closeInventory()
            opener.sendLang("workspace-members-search-by-${name}-input")
            opener.nextChat {
                params[name] = it
                sync { searchUI.open() }
            }
        }
    }
}