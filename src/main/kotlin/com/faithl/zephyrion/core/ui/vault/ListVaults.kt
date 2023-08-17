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
import taboolib.module.ui.type.Linked
import taboolib.platform.util.*

class ListVaults(val owner: Player, val workspace: Workspace, val root: UI? = null) : SearchUI() {

    val vaults = mutableListOf<Vault>()
    val searchItems = mutableListOf<SearchItem>()
    val params = mutableMapOf<String, String>()
    val searchUI = Search(owner, searchItems, this)

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
            owner.asLangText("vaults-main-search-by-${name}-name"),
            owner.asLangText("vaults-main-search-by-${name}-desc")
        ) { player ->
            owner.closeInventory()
            owner.sendLang("vaults-main-search-by-${name}-input")
            owner.nextChat {
                params[name] = it
                sync {
                    searchUI.open(player)
                }
            }
        }
    }

    override fun build(): Inventory {
        search()
        return buildMenu<Linked<Vault>>(title()) {
            setLinkedMenuProperties(this)
            setRows6SplitBlock(this)
            setElements(this)
            setCreateItem(this)
            setSearchItem(this)
            setPageTurnItems(this)
            setReturnItem(this)
        }
    }

    fun setElements(menu: Linked<Vault>) {
        menu.elements { vaults }
        menu.onGenerate { _, element, _, _ ->
            buildItem(XMaterial.CHEST) {
                name = owner.asLangText("vaults-main-item-name", element.name)
                transaction {
                    lore += if (element.workspace.owner == owner.uniqueId.toString()) {
                        owner.asLangTextList(
                            "vaults-main-item-admin-desc",
                            element.id,
                            element.desc ?: "",
                            element.getCreatedAt(),
                            element.getUpdatedAt()
                        )
                    } else {
                        owner.asLangTextList(
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
                VaultUI(owner, element, this@ListVaults).open(event.clicker)
            } else if (event.clickEvent().isRightClick) {
                transaction {
                    if (element.workspace.owner == owner.uniqueId.toString()) {
                        AdminVault(owner, element, this@ListVaults).open(event.clicker)
                    }
                }
            }
        }
    }

    fun setCreateItem(menu: Linked<Vault>) {
        if (workspace.owner == owner.uniqueId.toString() || (workspace.type == Workspaces.Type.INDEPENDENT && owner.hasPermission(
                Zephyrion.permissions.getString("create-independent-workspace")!!
            ))
        ) {
            menu.set(45) {
                buildItem(XMaterial.STICK) {
                    name = owner.asLangText("vaults-main-create")
                }
            }
            menu.onClick(45) { event ->
                if (event.currentItem != null) {
                    CreateVault(owner, workspace, this).open(event.clicker)
                }
            }
        }
    }

    fun setPageTurnItems(menu: Linked<Vault>) {
        menu.setPreviousPage(48) { _, hasPreviousPage ->
            if (hasPreviousPage) {
                buildItem(XMaterial.ARROW) {
                    name = owner.asLangText("vaults-main-prev-page")
                }
            } else {
                buildItem(XMaterial.BARRIER) {
                    name = owner.asLangText("vaults-main-prev-page-disabled")
                }
            }
        }
        menu.setNextPage(50) { _, hasNextPage ->
            if (hasNextPage) {
                buildItem(XMaterial.ARROW) {
                    name = owner.asLangText("vaults-main-next-page")
                }
            } else {
                buildItem(XMaterial.BARRIER) {
                    name = owner.asLangText("vaults-main-next-page-disabled")
                }
            }
        }
    }

    fun setReturnItem(menu: Linked<Vault>) {
        menu.set(53) {
            buildItem(XMaterial.RED_STAINED_GLASS_PANE) {
                name = if (root != null) {
                    owner.asLangText("vaults-main-return")
                } else {
                    owner.asLangText("vaults-main-close")
                }
            }
        }
        menu.onClick(53) {
            owner.closeInventory()
            root?.open(it.clicker)
        }
    }

    fun setSearchItem(menu: Linked<Vault>) {
        menu.set(49) {
            buildItem(XMaterial.COMPASS) {
                name = owner.asLangText("vaults-main-search")
            }
        }
        menu.onClick(49) {
            searchUI.open(it.clicker)
        }
    }

    override fun open(opener: Player) {
        if (owner != opener && !ZephyrionAPI.isPluginAdmin(opener) && !workspace.isMember(opener.uniqueId.toString())) {
            return
        }
        opener.openInventory(build())
    }

    override fun title(): String {
        return if (params.isNotEmpty()) {
            owner.asLangText("vaults-main-title-with-search", workspace.name)
        } else {
            owner.asLangText("vaults-main-title", workspace.name)
        }
    }

}