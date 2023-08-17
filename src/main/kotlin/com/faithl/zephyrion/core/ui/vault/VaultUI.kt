package com.faithl.zephyrion.core.ui.vault

import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.api.events.VaultCloseEvent
import com.faithl.zephyrion.api.events.VaultOpenEvent
import com.faithl.zephyrion.core.models.Item
import com.faithl.zephyrion.core.models.Vault
import com.faithl.zephyrion.core.ui.SearchUI
import com.faithl.zephyrion.core.ui.UI
import com.faithl.zephyrion.core.ui.search.Search
import com.faithl.zephyrion.core.ui.search.SearchItem
import com.faithl.zephyrion.core.ui.setRows6SplitBlock
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.jetbrains.exposed.sql.transactions.transaction
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.util.sync
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.buildMenu
import taboolib.module.ui.type.Basic
import taboolib.platform.util.*

class VaultUI(val owner: Player, val vault: Vault, val root: UI? = null) : SearchUI() {

    var page = 1
    val items = mutableListOf<Item>()
    val searchItems = mutableListOf<SearchItem>()
    val params = mutableMapOf<String, String>()
    val searchUI = Search(owner, searchItems, this)

    init {
        addSearchItems("name")
        addSearchItems("lore")
    }

    override fun search() {
        items.clear()
        items.addAll(ZephyrionAPI.getItems(vault, page, owner))
        if (params.isEmpty()) {
            return
        }
//        params["name"]?.let {
//            items.retainAll { item ->
//                item.getName().contains(it)
//            }
//        }
//        params["lore"]?.let {
//            items.retainAll { item ->
//                item.getLore().contains(it)
//            }
//        }
    }

    override fun build(): Inventory {
        search()
        return buildMenu<Basic>(title()) {
            setProperties(this)
            setRows6SplitBlock(this)
            setElements(this)
            setPageTurnItems(this)
            setSearchItem(this)
            setReturnItem(this)
            setClickEvent(this)
        }
    }

    fun setProperties(menu: Basic) {
        menu.rows(6)
        menu.handLocked(false)
    }

    fun setElements(menu: Basic) {
        if (params.isNotEmpty()) {
            // TODO
        } else {
            if (page == vault.getMaxPage()) {
                val ownerData = transaction {
                    ZephyrionAPI.getUserData(vault.workspace.owner)
                }
                getLockedSlots(vault, page)?.let {
                    for (i in it) {
                        menu.set(i) {
                            buildItem(XMaterial.BLUE_STAINED_GLASS_PANE) {
                                name = owner.asLangText("vault-main-unlock")
                                transaction {
                                    lore += if (vault.workspace.owner == owner.uniqueId.toString()) {
                                        owner.asLangTextList(
                                            "vault-main-unlock-admin-desc",
                                            ownerData.sizeUsed,
                                            ownerData.sizeQuotas,
                                            ownerData.sizeQuotas - ownerData.sizeUsed
                                        )
                                    } else {
                                        owner.asLangTextList("vault-main-unlock-member-desc")
                                    }
                                }
                            }
                        }
                        menu.onClick(i) { event ->
                            owner.closeInventory()
                            owner.sendLang("vault-main-unlock-tip")
                            owner.nextChat {
                                sync {
                                    if (it == "0") {
                                        owner.sendLang("vault-main-unlock-canceled")
                                    } else {
                                        val result = transaction {
                                            vault.addSize(it.toInt())
                                        }
                                        if (result) {
                                            owner.sendLang("vault-main-unlock-succeed", it.toInt())
                                        } else {
                                            owner.sendLang(
                                                "vault-main-unlock-failed",
                                                ownerData.workspaceQuotas - ownerData.workspaceUsed,
                                                it
                                            )
                                        }
                                    }
                                    open(event.clicker)
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    fun setPageTurnItems(menu: Basic) {
        menu.set(48) {
            if (page == 1) {
                buildItem(XMaterial.BARRIER) {
                    name = owner.asLangText("vault-main-prev-page-disabled")
                }
            } else {
                buildItem(XMaterial.ARROW) {
                    name = owner.asLangText("vault-main-prev-page")
                }
            }
        }
        menu.set(50) {
            if (page == vault.getMaxPage()) {
                buildItem(XMaterial.BARRIER) {
                    name = owner.asLangText("vault-main-next-page-disabled")
                }
            } else {
                buildItem(XMaterial.ARROW) {
                    name = owner.asLangText("vault-main-next-page")
                }
            }
        }
        menu.onClick(48) {
            if (page != 1) {
                page -= 1
                open(it.clicker)
            }
        }
        menu.onClick(50) {
            if (page != vault.getMaxPage()) {
                page += 1
                open(it.clicker)
            }
        }
    }

    fun setSearchItem(menu: Basic) {
        menu.set(49) {
            buildItem(XMaterial.COMPASS) {
                name = owner.asLangText("vault-main-search")
            }
        }
        menu.onClick(49) {
            val result = cache.filter { it.vaultId == vault.id.value }
            if (result.size > 1) {
                it.clicker.sendLang("vault-main-search-requirement")
                return@onClick
            } else {
                val value = result.first()
                if (value.players.size > 1) {
                    it.clicker.sendLang("vault-main-search-requirement")
                    return@onClick
                }
            }
            searchUI.open(it.clicker)
        }
    }

    fun addSearchItems(name: String) {
        searchItems += SearchItem(
            owner.asLangText("vault-main-search-by-${name}-name"), owner.asLangText("vault-main-search-by-${name}-desc")
        ) { player ->
            owner.closeInventory()
            owner.sendLang("vault-main-search-by-${name}-input")
            owner.nextChat {
                params[name] = it
                sync {
                    searchUI.open(player)
                }
            }
        }
    }

    fun setReturnItem(menu: Basic) {
        menu.set(53) {
            buildItem(XMaterial.RED_STAINED_GLASS_PANE) {
                name = if (root != null) {
                    owner.asLangText("vault-main-return")
                } else {
                    owner.asLangText("vault-main-close")
                }
            }
        }
        menu.onClick(53) {
            if (root != null) {
                transaction {
                    ListVaults(it.clicker, vault.workspace, (root as ListVaults).root).open(it.clicker)
                }
            } else {
                it.clicker.closeInventory()
            }
        }
    }

    fun setClickEvent(menu: Basic) {
        menu.onClick {
            if (it.rawSlot in 36..53) {
                it.isCancelled = true
            }
        }
    }

    override fun open(opener: Player) {
        if (owner != opener && !ZephyrionAPI.isPluginAdmin(opener) && !vault.workspace.isMember(opener.uniqueId.toString())) {
            return
        }
        if (params.isNotEmpty()) {
            return
        }
        val openingInv = cache.find { it.vaultId == vault.id.value && it.page == page }
        if (openingInv != null) {
            val inv = openingInv.inventory
            opener.openInventory(inv)
            openingInv.players.add(owner)
            VaultOpenEvent(vault, page, inv, opener).call()
        } else {
            val inv = build()
            items.forEach {
                inv.setItem(it.slot, it.itemStack)
            }
            opener.openInventory(inv)
            cache.add(OpeningInv(vault.id.value, page, mutableListOf(owner), inv))
            VaultOpenEvent(vault, page, inv, opener).call()
        }
    }

    override fun title(): String {
        return if (params.isNotEmpty()) {
            owner.asLangText("vault-main-title-with-search", vault.name)
        } else {
            owner.asLangText("vault-main-title", vault.name)
        }
    }

    companion object {

        @SubscribeEvent
        fun e(e: InventoryClickEvent) {
//            val result = cache.find { it.inventory == e.inventory }
//            if (result != null) {
//                val vaultId = result.vaultId
//                val vault = transaction {
//                    Vault[vaultId]
//                }
//                val page = result.page
//                val inv = result.inventory
//                save(vault, page, inv)
//            }
        }

        @SubscribeEvent
        fun e(e: InventoryCloseEvent) {
            val result = cache.find { it.inventory == e.inventory }
            if (result != null) {
                val vaultId = result.vaultId
                val vault = transaction {
                    Vault[vaultId]
                }
                val page = result.page
                val inv = result.inventory
                VaultCloseEvent(vault, page, inv, e.player as Player).call()
            }
        }

        @SubscribeEvent
        fun e(e: VaultCloseEvent) {
            val result = cache.find { it.vaultId == e.vault.id.value && it.page == e.page }
            if (result!!.players.size == 1) {
                save(e.vault, e.page, e.inventory, e.closer)
                cache.remove(result)
            } else {
                result.players.remove(e.closer)
            }
        }

        fun save(vault: Vault, page: Int, inv: Inventory, player: Player) {
            val range = getLockedSlots(vault, page)?.let { intRange ->
                (0..35).filter { it !in intRange }
            } ?: 0..35
            for (i in range) {
                val item = inv.getItem(i)
                if (item != null) {
                    ZephyrionAPI.setItem(vault, page, i, item, player)
                } else {
                    ZephyrionAPI.removeItem(vault, page, i, player)
                }
            }
        }

        fun refresh(vault: Vault, page: Int, inv: Inventory) {
            // TODO
        }

        fun getLockedSlots(vault: Vault, page: Int): IntRange? {
            return if (page == vault.getMaxPage()) {
                val lock = vault.size % 36
                if (vault.size == 0) {
                    0..<36
                } else if (lock == 0) {
                    null
                } else {
                    lock..<36
                }
            } else {
                null
            }
        }

        data class OpeningInv(
            val vaultId: Int, val page: Int, val players: MutableList<Player>, var inventory: Inventory
        )

        val cache = mutableListOf<OpeningInv>()

    }

}