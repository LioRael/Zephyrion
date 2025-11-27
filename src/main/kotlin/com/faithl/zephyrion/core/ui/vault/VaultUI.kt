package com.faithl.zephyrion.core.ui.vault

import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.api.events.VaultCloseEvent
import com.faithl.zephyrion.api.events.VaultOpenEvent
import com.faithl.zephyrion.core.models.Vault
import com.faithl.zephyrion.core.models.WorkspaceType
import com.faithl.zephyrion.core.ui.SearchUI
import com.faithl.zephyrion.core.ui.UI
import com.faithl.zephyrion.core.ui.search.Search
import com.faithl.zephyrion.core.ui.search.SearchItem
import com.faithl.zephyrion.core.ui.setRows6SplitBlock
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import taboolib.common.util.sync
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.InventoryViewProxy
import taboolib.module.ui.buildMenu
import taboolib.module.ui.type.Chest
import taboolib.module.ui.type.StorableChest
import taboolib.module.ui.type.impl.StorableChestImpl
import taboolib.platform.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * owner 打开的玩家
 */
class VaultUI(override val opener: Player, val vault: Vault, val root: UI? = null, var page: Int = 1) : SearchUI() {

    private val itemsCache = ConcurrentHashMap<Int, ItemStack?>()
    private var searchResults = listOf<com.faithl.zephyrion.core.models.Item>()

    val searchItems = mutableListOf<SearchItem>()
    override val params = mutableMapOf<String, String>()
    val searchUI = Search(opener, searchItems, this)

    init {
        addSearchItems("name")
        addSearchItems("lore")
    }

    override fun build(): Inventory {
        return buildMenu<StorableChestImpl>(title()) {
            setProperties(this)
            setRows6SplitBlock(this)

            val currentPage = page

            rule {

                checkSlot { inventory, itemStack, slot ->
                    if (slot !in 0..35) return@checkSlot false
                    if (params.isNotEmpty()) return@checkSlot false

                    val lockedSlots = getLockedSlots(vault, currentPage)
                    if (lockedSlots != null && slot in lockedSlots) {
                        return@checkSlot false
                    }
                    true
                }

                firstSlot { inventory, itemStack ->
                    if (params.isNotEmpty()) return@firstSlot -1

                    val lockedSlots = getLockedSlots(vault, currentPage)
                    (0..35).firstOrNull { slot ->
                        val item = inventory.getItem(slot)
                        val isEmpty = item == null || item.type.isAir
                        val isNotLocked = lockedSlots == null || slot !in lockedSlots
                        isEmpty && isNotLocked
                    } ?: -1
                }

                writeItem { inventory, itemStack, slot, clickType ->
                    if (params.isNotEmpty()) return@writeItem

                    if (slot in 0..35) {
                        inventory.setItem(slot, itemStack)
                        submitAsync {
                            try {
                                if (itemStack.isAir){
                                    ZephyrionAPI.removeItem(vault, currentPage, slot, opener)
                                }else{
                                    ZephyrionAPI.setItem(vault, currentPage, slot, itemStack, opener)
                                }
                            }catch (e:Exception){
                                e.printStackTrace()
                            }
                        }

                        if (vault.workspace.type == WorkspaceType.INDEPENDENT) return@writeItem
                        refresh(vault, currentPage, slot, itemStack)
                    }
                }

                readItem { inventory, slot ->
                    itemsCache[slot]
                }
            }

            onBuild { player, inventory ->
                itemsCache.forEach { (slot, itemStack) ->
                    itemStack?.let { inventory.setItem(slot, it) }
                }
                setElements(this, inventory)
            }

            onClose { event ->
                val player = event.player as Player
                VaultCloseEvent(vault, currentPage, event.inventory, player).call()
            }

            setPageTurnItems(this)
            setSearchItem(this)
            setReturnItem(this)
            setClickEvent(this)
        }
    }

    fun setProperties(menu: Chest) {
        menu.rows(6)
        menu.handLocked(false)
    }

    fun setElements(menu: StorableChest, inventory: Inventory) {
        if (params.isNotEmpty()) {
            return
        } else {
            // 普通浏览模式：显示锁定槽位
            if (page == vault.getMaxPage()) {
                val ownerData = ZephyrionAPI.getUserData(vault.workspace.owner)

                getLockedSlots(vault, page)?.let { range ->
                    for (i in range) {
                        inventory.setItem(i, buildItem(XMaterial.BLUE_STAINED_GLASS_PANE) {
                            name = opener.asLangText("vault-main-unlock")
                            // 工作空间所有者 或 插件管理员 显示管理员描述
                            lore += if (vault.workspace.owner == opener.uniqueId.toString() ||
                                ZephyrionAPI.isPluginAdmin(opener)) {
                                // 无限配额显示特殊描述
                                if (ownerData.unlimited) {
                                    opener.asLangTextList("vault-main-unlock-unlimited-desc")
                                } else {
                                    opener.asLangTextList(
                                        "vault-main-unlock-admin-desc",
                                        ownerData.sizeUsed,
                                        ownerData.sizeQuotas,
                                        ownerData.sizeQuotas - ownerData.sizeUsed
                                    )
                                }
                            } else {
                                opener.asLangTextList("vault-main-unlock-member-desc")
                            }
                        })

                        menu.onClick(i) { event ->
                            val clicker = event.clicker
                            if (vault.workspace.owner != clicker.uniqueId.toString() &&
                                !ZephyrionAPI.isPluginAdmin(clicker)) {
                                clicker.sendLang("vault-main-unlock-no-permission")
                                return@onClick
                            }

                            clicker.closeInventory()
                            clicker.sendLang("vault-main-unlock-tip")
                            clicker.nextChat { input ->
                                sync {
                                    if (input == "0") {
                                        clicker.sendLang("vault-main-unlock-canceled")
                                    } else {
                                        val currentOwnerData = ZephyrionAPI.getUserData(vault.workspace.owner)
                                        val result = vault.addSize(input.toInt())
                                        if (result) {
                                            clicker.sendLang("vault-main-unlock-succeed", input.toInt())
                                            refreshUI(vault,page)
                                        } else {
                                            clicker.sendLang(
                                                "vault-main-unlock-failed",
                                                currentOwnerData.sizeQuotas - currentOwnerData.sizeUsed,
                                                input
                                            )
                                        }
                                    }
                                    open()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun setPageTurnItems(menu: StorableChest) {
        val maxPage = if (params.isNotEmpty()) {
            // 搜索模式：根据搜索结果计算最大页数
            if (searchResults.isEmpty()) 1 else (searchResults.size + 35) / 36
        } else {
            // 普通模式：使用vault的最大页数
            vault.getMaxPage()
        }

        menu.set(48) {
            if (page == 1) {
                buildItem(XMaterial.BARRIER) {
                    name = opener.asLangText("vault-main-prev-page-disabled")
                }
            } else {
                buildItem(XMaterial.ARROW) {
                    name = opener.asLangText("vault-main-prev-page")
                }
            }
        }
        menu.set(50) {
            if (page == maxPage) {
                buildItem(XMaterial.BARRIER) {
                    name = opener.asLangText("vault-main-next-page-disabled")
                }
            } else {
                buildItem(XMaterial.ARROW) {
                    name = opener.asLangText("vault-main-next-page")
                }
            }
        }
        menu.onClick(48) { event ->
            if (page != 1) {
                VaultUI(event.clicker, vault, root, page - 1).apply {
                    params.putAll(this@VaultUI.params)
                }.open()
            }
        }
        menu.onClick(50) { event ->
            if (page != maxPage) {
                VaultUI(event.clicker, vault, root, page + 1).apply {
                    params.putAll(this@VaultUI.params)
                }.open()
            }
        }
    }

    fun setSearchItem(menu: StorableChest) {
        menu.set(49) {
            buildItem(XMaterial.COMPASS) {
                name = opener.asLangText("vault-main-search")
            }
        }
        menu.onClick(49) { event ->
            searchUI.open()
        }
    }

    fun addSearchItems(name: String) {
        searchItems += SearchItem(
            opener.asLangText("vault-main-search-by-${name}-name"),
            opener.asLangText("vault-main-search-by-${name}-desc")
        ) { player ->
            player.closeInventory()
            player.sendLang("vault-main-search-by-${name}-input")
            player.nextChat { input ->
                params[name] = input
                sync {
                    searchUI.open()
                }
            }
        }
    }

    fun setReturnItem(menu: StorableChest) {
        menu.set(53) {
            buildItem(XMaterial.RED_STAINED_GLASS_PANE) {
                name = if (root != null) {
                    opener.asLangText("vault-main-return")
                } else {
                    opener.asLangText("vault-main-close")
                }
            }
        }
        menu.onClick(53) { event ->
            if (root != null) {
                ListVaults(event.clicker, vault.workspace, (root as ListVaults).root).open()
            } else {
                event.clicker.closeInventory()
            }
        }
    }

    fun setClickEvent(menu: StorableChest) {
        menu.onClick {
            if (it.rawSlot in 36..53) {
                it.isCancelled = true
            }
        }
    }

    override fun open() {
        if (!ZephyrionAPI.isPluginAdmin(opener) &&
            !vault.workspace.isMember(opener.uniqueId.toString())) {
            return
        }

        submitAsync {
            try {
                if (params.isNotEmpty()) {
                    // 搜索模式
                    search()
                    sync {
                        val inv = build()
                        opener.openInventory(inv)
                        VaultOpenEvent(vault, page, inv, opener).call()
                    }
                } else {
                    // 普通浏览模式
                    val items = ZephyrionAPI.getItems(vault, page, opener)
                    itemsCache.clear()
                    items.forEach {
                        itemsCache[it.slot] = it.itemStack
                    }

                    sync {
                        val inv = build()
                        opener.openInventory(inv)
                        VaultOpenEvent(vault, page, inv, opener).call()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                sync {
                    opener.sendLang("ui-load-error")
                }
            }
        }
    }

    override fun title(): String {
        return if (params.isNotEmpty()) {
            opener.asLangText("vault-main-title-with-search", vault.name)
        } else {
            opener.asLangText("vault-main-title", vault.name)
        }
    }

    override fun search() {
        // 合并所有搜索条件的结果
        val allResults = mutableSetOf<com.faithl.zephyrion.core.models.Item>()

        params["name"]?.let { name ->
            allResults.addAll(ZephyrionAPI.searchItemsByName(vault, name))
        }

        params["lore"]?.let { lore ->
            allResults.addAll(ZephyrionAPI.searchItemsByLore(vault, lore))
        }

        searchResults = allResults.toList()

        // 根据当前页填充itemsCache
        itemsCache.clear()
        val startIndex = (page - 1) * 36
        val endIndex = minOf(startIndex + 36, searchResults.size)

        for (i in startIndex until endIndex) {
            val slot = i - startIndex
            itemsCache[slot] = searchResults[i].itemStack
        }
    }

    companion object {

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

        data class OpeningInv(val vaultId: Int, val page: Int, val players: MutableList<Player>)

        private val openViewers = mutableSetOf<OpeningInv>()

        @SubscribeEvent
        fun onOpen(e:VaultOpenEvent) {
            if (e.vault.workspace.type == WorkspaceType.INDEPENDENT) return
            val openingInv = openViewers.find { it.vaultId == e.vault.id && it.page == e.page } ?: run {
                val newOpeningInv = OpeningInv(e.vault.id, e.page, mutableListOf())
                openViewers.add(newOpeningInv)
                newOpeningInv
            }
            openingInv.players.add(e.opener)
        }

        @SubscribeEvent
        fun onClose(e:VaultCloseEvent) {
            if (e.vault.workspace.type == WorkspaceType.INDEPENDENT) return
            val openingInv = openViewers.find { it.vaultId == e.vault.id && it.page == e.page }!!
            openingInv.players.remove(e.closer)
        }

        private fun refresh(vault: Vault, page: Int, slot: Int, itemStack: ItemStack) {
            val openingInv = openViewers.find { it.vaultId == vault.id && it.page == page } ?: return
            openingInv.players.forEach {
                if (!it.isOnline) {
                    openingInv.players.remove(it)
                    return@forEach
                }
                submit {
                    val inv =InventoryViewProxy.getTopInventory(it.openInventory)
                    if (inv.holder is taboolib.module.ui.MenuHolder) {
                        inv.setItem(slot, itemStack)
                    }
                }
            }
        }

        //重新打开UI
        private fun refreshUI(vault: Vault, page: Int) {
            val openingInv = openViewers.find { it.vaultId == vault.id && it.page == page } ?: return
            openingInv.players.forEach {
                if (!it.isOnline) {
                    openingInv.players.remove(it)
                    return@forEach
                }
                it.closeInventory()
                VaultUI(it,vault,page = page).open()
            }
        }

    }
}
