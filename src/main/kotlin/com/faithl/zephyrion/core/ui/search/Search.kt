package com.faithl.zephyrion.core.ui.search

import com.faithl.zephyrion.core.ui.SearchUI
import com.faithl.zephyrion.core.ui.UI
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.buildMenu
import taboolib.module.ui.type.Linked
import taboolib.platform.util.asLangText
import taboolib.platform.util.buildItem

data class SearchItem(val name: String, val description: String, val action: (clicker: Player) -> Unit)

class Search(val owner: Player, val elements: List<SearchItem>, val root: SearchUI) : UI() {

    override fun title(): String {
        return owner.asLangText("search-title")
    }

    override fun build(): Inventory {
        return buildMenu<Linked<SearchItem>>(title()) {
            rows(4)
            elements {
                elements
            }
            slots(
                mutableListOf(
                    9, 10, 11, 12, 13, 14, 15, 16, 17,
                )
            )
            for (i in 0..8) {
                set(i) {
                    buildItem(XMaterial.BLACK_STAINED_GLASS_PANE) {
                        name = "§f"
                    }
                }
            }
            for (i in 18..26) {
                set(i) {
                    buildItem(XMaterial.BLACK_STAINED_GLASS_PANE) {
                        name = "§f"
                    }
                }
            }
            onGenerate { _, element, _, _ ->
                buildItem(XMaterial.PAPER) {
                    name = "§f${element.name}"
                    lore += "§7${element.description}"
                }
            }
            setPreviousPage(21) { _, hasPreviousPage ->
                if (hasPreviousPage) {
                    buildItem(XMaterial.ARROW) {
                        name = owner.asLangText("search-prev-page")
                    }
                } else {
                    buildItem(XMaterial.BARRIER) {
                        name = owner.asLangText("search-prev-page-disabled")
                    }
                }
            }
            setNextPage(23) { _, hasNextPage ->
                if (hasNextPage) {
                    buildItem(XMaterial.ARROW) {
                        name = owner.asLangText("search-next-page")
                    }
                } else {
                    buildItem(XMaterial.BARRIER) {
                        name = owner.asLangText("search-next-page-disabled")
                    }
                }
            }
            set(31) {
                buildItem(XMaterial.COMPASS) {
                    name = owner.asLangText("search-confirm")
                }
            }
            onClick(31) {
                root.search()
                root.open(it.clicker)
            }
            set(35) {
                buildItem(XMaterial.RED_STAINED_GLASS_PANE) {
                    name = owner.asLangText("search-return")
                }
            }
            onClick(35) {
                root.open(it.clicker)
            }
            onClick { event, element ->
                element.action(event.clicker)
            }
        }
    }

    override fun open(opener: Player) {
        opener.openInventory(build())
    }
}
