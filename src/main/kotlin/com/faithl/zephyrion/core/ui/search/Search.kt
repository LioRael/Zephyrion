package com.faithl.zephyrion.core.ui.search

import com.faithl.zephyrion.core.ui.SearchUI
import com.faithl.zephyrion.core.ui.UI
import com.faithl.zephyrion.core.ui.vault.VaultUI
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.buildMenu
import taboolib.module.ui.type.impl.PageableChestImpl
import taboolib.platform.util.asLangText
import taboolib.platform.util.buildItem

data class SearchItem(val name: String, val description: String, val action: (clicker: Player) -> Unit)

class Search(override val opener: Player, val elements: List<SearchItem>, override val root: SearchUI) : UI() {

    override fun title(): String {
        return opener.asLangText("search-title")
    }

    override fun build(): Inventory {
        return buildMenu<PageableChestImpl<SearchItem>>(title()) {
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
            set(27) {
                if (root.params.isEmpty()) {
                    buildItem(XMaterial.PAPER) {
                        name = opener.asLangText("search-condition-empty")
                    }
                } else {
                    buildItem(XMaterial.WRITABLE_BOOK) {
                        name = opener.asLangText("search-condition-title")
                        lore.clear()
                        root.params.forEach { (key, value) ->
                            lore += opener.asLangText("search-condition-item", key, value)
                        }
                    }
                }
            }
            set(29) {
                buildItem(XMaterial.BARRIER) {
                    name = opener.asLangText("search-clear")
                    if (root.params.isEmpty()) {
                        lore += opener.asLangText("search-clear-disabled-desc")
                    } else {
                        lore += opener.asLangText("search-clear-desc")
                    }
                }
            }
            onClick(29) {
                if (root.params.isNotEmpty()) {
                    root.params.clear()
                    open()
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
                        name = opener.asLangText("search-prev-page")
                    }
                } else {
                    buildItem(XMaterial.BARRIER) {
                        name = opener.asLangText("search-prev-page-disabled")
                    }
                }
            }
            setNextPage(23) { _, hasNextPage ->
                if (hasNextPage) {
                    buildItem(XMaterial.ARROW) {
                        name = opener.asLangText("search-next-page")
                    }
                } else {
                    buildItem(XMaterial.BARRIER) {
                        name = opener.asLangText("search-next-page-disabled")
                    }
                }
            }
            set(31) {
                buildItem(XMaterial.COMPASS) {
                    name = opener.asLangText("search-confirm")
                }
            }
            onClick(31) {
                // 确认搜索：调用 search() 方法执行搜索
                val vaultUI = root as? VaultUI
                if (vaultUI != null) {
                    vaultUI.search()
                    vaultUI.open()
                } else {
                    root.search()
                    root.open()
                }
            }
            set(35) {
                buildItem(XMaterial.RED_STAINED_GLASS_PANE) {
                    name = opener.asLangText("search-return")
                }
            }
            onClick(35) {
                root.open()
            }
            onClick { event, element ->
                element.action(event.clicker)
            }
        }
    }

    override fun open() {
        opener.openInventory(build())
    }
}
