package com.faithl.zephyrion.core.ui

import com.faithl.zephyrion.core.ui.search.Search
import com.faithl.zephyrion.core.ui.search.SearchItem
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.type.Chest
import taboolib.platform.util.asLangText
import taboolib.platform.util.buildItem

abstract class UI {
    abstract val opener:Player
    abstract val root: UI?
    abstract fun build(): Inventory
    abstract fun open()
    abstract fun title(): String

    open fun setReturnItem(menu: Chest) {
        menu.set(53) {
            buildItem(XMaterial.RED_STAINED_GLASS_PANE) {
                name = if (root != null) {
                    opener.asLangText("ui-item-name-return")
                } else {
                    opener.asLangText("ui-item-name-close")
                }
            }
        }
        menu.onClick(53) {
            root?.open() ?: it.clicker.closeInventory()
        }
    }
}

abstract class SearchUI : UI() {

    open var params = mutableMapOf<String, String>()

    open var searchItems = mutableListOf<SearchItem>()

    open val isSearching: Boolean
        get() = params.isNotEmpty()

    open val searchUI: Search by lazy { Search(opener, searchItems, this) }

    abstract fun search()

    open fun setSearchItem(menu: Chest) {
        menu.set(49) {
            buildItem(XMaterial.COMPASS) {
                name = opener.asLangText("ui-item-name-search")
            }
        }
        menu.onClick(49) {
            searchUI.open()
        }
    }
    override fun setReturnItem(menu: Chest) {
        menu.set(53) {
            buildItem(XMaterial.RED_STAINED_GLASS_PANE) {
                name = if (root != null || isSearching) {
                    opener.asLangText("ui-item-name-return")
                } else {
                    opener.asLangText("ui-item-name-close")
                }
            }
        }
        menu.onClick(53) {
            if (isSearching) {
                params.clear()
                open()
                return@onClick
            }
            root?.open() ?: it.clicker.closeInventory()
        }
    }

}