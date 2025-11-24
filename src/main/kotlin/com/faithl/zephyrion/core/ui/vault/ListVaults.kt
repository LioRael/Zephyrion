package com.faithl.zephyrion.core.ui.vault

import com.faithl.zephyrion.Zephyrion
import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.core.models.Vault
import com.faithl.zephyrion.core.models.Workspace
import com.faithl.zephyrion.core.models.Workspaces
import com.faithl.zephyrion.core.ui.SearchUI
import com.faithl.zephyrion.core.ui.UI
import com.faithl.zephyrion.core.ui.search.Search
import com.faithl.zephyrion.core.ui.search.SearchItem
import com.faithl.zephyrion.core.ui.setLinkedMenuProperties
import com.faithl.zephyrion.core.ui.setRows6SplitBlock
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.jetbrains.exposed.sql.transactions.transaction
import taboolib.common.util.sync
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.buildMenu
import taboolib.module.ui.type.PageableChest
import taboolib.module.ui.type.impl.PageableChestImpl
import taboolib.platform.util.*

class ListVaults(override val opener: Player, val workspace: Workspace, val root: UI? = null) : SearchUI() {

    val vaults = mutableListOf<Vault>()
    val searchItems = mutableListOf<SearchItem>()
    override val params = mutableMapOf<String, String>()
    val searchUI = Search(opener, searchItems, this)

    init {
        addSearchItems("name")
        addSearchItems("desc")
    }

    override fun search() {
        vaults.clear()
        vaults.addAll(ZephyrionAPI.getVaults(workspace))
        if (params.isEmpty()) {
            return
        }
        params["name"]?.let {
            vaults.retainAll { vault ->
                vault.name.contains(it)
            }
        }
        params["desc"]?.let {
            vaults.retainAll { vault ->
                vault.desc?.contains(it) ?: false
            }
        }
    }

    fun addSearchItems(name: String) {
        searchItems += SearchItem(
            opener.asLangText("vaults-main-search-by-${name}-name"),
            opener.asLangText("vaults-main-search-by-${name}-desc")
        ) { player ->
            opener.closeInventory()
            opener.sendLang("vaults-main-search-by-${name}-input")
            opener.nextChat {
                params[name] = it
                sync {
                    searchUI.open()
                }
            }
        }
    }

    override fun build(): Inventory {
        search()
        return buildMenu<PageableChestImpl<Vault>>(title()) {
            setLinkedMenuProperties(this)
            setRows6SplitBlock(this)
            setElements(this)
            setCreateItem(this)
            setSearchItem(this)
            setPageTurnItems(this)
            setReturnItem(this)
        }
    }

    fun setElements(menu: PageableChest<Vault>) {
        menu.elements { vaults }
        menu.onGenerate { _, element, _, _ ->
            buildItem(XMaterial.CHEST) {
                name = opener.asLangText("vaults-main-item-name", element.name)
                transaction {
                    lore += if (element.workspace.owner == opener.uniqueId.toString() ||
                        ZephyrionAPI.isPluginAdmin(opener)) {
                        opener.asLangTextList(
                            "vaults-main-item-admin-desc",
                            element.id,
                            element.desc ?: "",
                            element.getCreatedAt(),
                            element.getUpdatedAt()
                        )
                    } else {
                        opener.asLangTextList(
                            "vaults-main-item-member-desc",
                            element.id,
                            element.desc ?: "",
                            element.getCreatedAt(),
                            element.getUpdatedAt()
                        )
                    }
                }
            }
        }
        menu.onClick { event, element ->
            if (event.clickEvent().isLeftClick) {
                VaultUI(event.clicker, element, this@ListVaults).open()
            } else if (event.clickEvent().isRightClick) {
                transaction {
                    if (element.workspace.owner == opener.uniqueId.toString() ||
                        ZephyrionAPI.isPluginAdmin(opener)) {
                        AdminVault(event.clicker, element, this@ListVaults).open()
                    }
                }
            }
        }
    }

    fun setCreateItem(menu: PageableChest<Vault>) {
        if (workspace.owner == opener.uniqueId.toString() ||
            ZephyrionAPI.isPluginAdmin(opener) ||
            (workspace.type == Workspaces.Type.INDEPENDENT && opener.hasPermission(
                Zephyrion.permissions.getString("create-independent-workspace")!!
            ))
        ) {
            menu.set(45) {
                buildItem(XMaterial.STICK) {
                    name = opener.asLangText("vaults-main-create")
                }
            }
            menu.onClick(45) { event ->
                if (event.currentItem != null) {
                    CreateVault(event.clicker, workspace, this).open()
                }
            }
        }
    }

    fun setPageTurnItems(menu: PageableChest<Vault>) {
        menu.setPreviousPage(48) { _, hasPreviousPage ->
            if (hasPreviousPage) {
                buildItem(XMaterial.ARROW) {
                    name = opener.asLangText("vaults-main-prev-page")
                }
            } else {
                buildItem(XMaterial.BARRIER) {
                    name = opener.asLangText("vaults-main-prev-page-disabled")
                }
            }
        }
        menu.setNextPage(50) { _, hasNextPage ->
            if (hasNextPage) {
                buildItem(XMaterial.ARROW) {
                    name = opener.asLangText("vaults-main-next-page")
                }
            } else {
                buildItem(XMaterial.BARRIER) {
                    name = opener.asLangText("vaults-main-next-page-disabled")
                }
            }
        }
    }

    fun setReturnItem(menu: PageableChest<Vault>) {
        menu.set(53) {
            buildItem(XMaterial.RED_STAINED_GLASS_PANE) {
                name = if (root != null) {
                    opener.asLangText("vaults-main-return")
                } else {
                    opener.asLangText("vaults-main-close")
                }
            }
        }
        menu.onClick(53) {
            it.clicker.closeInventory()
            root?.open()
        }
    }

    fun setSearchItem(menu: PageableChest<Vault>) {
        menu.set(49) {
            buildItem(XMaterial.COMPASS) {
                name = opener.asLangText("vaults-main-search")
            }
        }
        menu.onClick(49) {
            searchUI.open()
        }
    }

    override fun open() {
        if (!ZephyrionAPI.isPluginAdmin(opener) && !workspace.isMember(opener.uniqueId.toString())) {
            return
        }
        opener.openInventory(build())
    }

    override fun title(): String {
        return if (params.isNotEmpty()) {
            opener.asLangText("vaults-main-title-with-search", workspace.name)
        } else {
            opener.asLangText("vaults-main-title", workspace.name)
        }
    }

}