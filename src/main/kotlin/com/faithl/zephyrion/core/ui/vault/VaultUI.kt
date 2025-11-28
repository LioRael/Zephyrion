package com.faithl.zephyrion.core.ui.vault

import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.api.events.VaultCloseEvent
import com.faithl.zephyrion.api.events.VaultOpenEvent
import com.faithl.zephyrion.api.events.VaultSearchCloseEvent
import com.faithl.zephyrion.api.events.VaultSearchOpenEvent
import com.faithl.zephyrion.core.models.Vault
import com.faithl.zephyrion.core.models.Item
import com.faithl.zephyrion.core.services.VaultSyncService
import com.faithl.zephyrion.core.ui.SearchUI
import com.faithl.zephyrion.core.ui.UI
import com.faithl.zephyrion.core.ui.search.Search
import com.faithl.zephyrion.core.ui.search.SearchItem
import com.faithl.zephyrion.core.ui.setRows6SplitBlock
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import taboolib.common.platform.function.submitAsync
import taboolib.common.util.sync
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.buildMenu
import taboolib.module.ui.type.Chest
import taboolib.module.ui.type.StorableChest
import taboolib.module.ui.type.impl.StorableChestImpl
import taboolib.platform.util.*

/**
 * owner 打开的玩家
 */
class VaultUI(override val opener: Player, val vault: Vault, val root: UI? = null, var page: Int = 1) : SearchUI() {


    val searchItems = mutableListOf<SearchItem>()
    override val params = mutableMapOf<String, String>()
    val searchUI = Search(opener, searchItems, this)

    // 搜索模式相关字段
    var searchResults: List<Item> = emptyList()
    var searchPage: Int = 1

    init {
        addSearchItems("name")
        addSearchItems("lore")
    }

    /**
     * 判断是否处于搜索模式
     */
    fun isSearchMode(): Boolean = params.isNotEmpty()

    /**
     * 获取搜索结果的最大页数
     */
    fun getSearchMaxPage(): Int {
        if (searchResults.isEmpty()) return 1
        return (searchResults.size + 35) / 36
    }

    /**
     * 获取当前搜索页的物品（虚拟槽位 0-35）
     */
    fun getSearchPageItems(): List<Item> {
        val startIndex = (searchPage - 1) * 36
        val endIndex = minOf(startIndex + 36, searchResults.size)
        return if (startIndex < searchResults.size) {
            searchResults.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }

    /**
     * 根据虚拟槽位获取实际的 Item
     */
    fun getItemByVirtualSlot(slot: Int): Item? {
        val index = (searchPage - 1) * 36 + slot
        return searchResults.getOrNull(index)
    }

    /**
     * 找到保险库中第一个可用的槽位
     * @return Pair<page, slot> 或 null 如果没有可用槽位
     */
    fun findFirstAvailableSlot(): Pair<Int, Int>? {
        val maxPage = vault.getMaxPage()
        for (p in 1..maxPage) {
            val items = ZephyrionAPI.getItems(vault, p, opener)
            val occupiedSlots = items.map { it.slot }.toSet()
            val lockedSlots = getLockedSlots(vault, p)

            for (slot in 0..35) {
                if (slot !in occupiedSlots) {
                    if (lockedSlots == null || slot !in lockedSlots) {
                        return Pair(p, slot)
                    }
                }
            }
        }
        return null
    }

    override fun build(): Inventory {
        return if (isSearchMode()) {
            buildSearchMode()
        } else {
            buildNormalMode()
        }
    }

    /**
     * 构建正常模式的界面
     */
    private fun buildNormalMode(): Inventory {
        return buildMenu<StorableChestImpl>(title()) {
            setProperties(this)
            setRows6SplitBlock(this)

            val currentPage = page

            rule {

                checkSlot { inventory, itemStack,slot ->
                    if (slot !in 0..35) return@checkSlot false
                    val lockedSlots = getLockedSlots(vault, currentPage)
                    if (lockedSlots != null && slot in lockedSlots) {
                        return@checkSlot false
                    }
                    true
                }

                firstSlot { inventory, itemStack ->
                    val lockedSlots = getLockedSlots(vault, currentPage)
                    (0..35).firstOrNull { slot ->
                        val item = inventory.getItem(slot)
                        val isEmpty = item == null || item.type.isAir
                        val isNotLocked = lockedSlots == null || slot !in lockedSlots
                        isEmpty && isNotLocked
                    } ?: -1
                }

                writeItem { inventory, itemStack, slot, clickType ->
                    if (slot in 0..35) {
                        inventory.setItem(slot, itemStack)
                        if (itemStack.isAir){
                            ZephyrionAPI.removeItem(vault, currentPage, slot, opener)
                        }else{
                            ZephyrionAPI.setItem(vault, currentPage, slot, itemStack, opener)
                        }
                    }
                }

                readItem { inventory, slot ->
                    val items = ZephyrionAPI.getItems(vault, currentPage,opener)
                    items.find { it.slot == slot }?.itemStack
                }
            }

            onBuild { player, inventory ->
                val items = ZephyrionAPI.getItems(vault, currentPage,player)
                items.forEach {
                    inventory.setItem(it.slot,it.itemStack)
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

    /**
     * 构建搜索模式的界面
     */
    private fun buildSearchMode(): Inventory {
        val ui = this
        return buildMenu<StorableChestImpl>(title()) {
            setProperties(this)
            setRows6SplitBlock(this)

            val currentSearchPage = searchPage
            val pageItems = getSearchPageItems()

            rule {
                checkSlot { inventory, itemStack, slot ->
                    if (slot !in 0..35) return@checkSlot false
                    true
                }

                firstSlot { inventory, itemStack ->
                    // 搜索模式下，放入物品需要找到实际的可用槽位
                    val available = findFirstAvailableSlot()
                    if (available != null) {
                        // 返回一个有效槽位，但实际写入会映射到实际位置
                        (0..35).firstOrNull { slot ->
                            val item = inventory.getItem(slot)
                            item == null || item.type.isAir
                        } ?: -1
                    } else {
                        -1
                    }
                }

                writeItem { inventory, itemStack, slot, clickType ->
                    if (slot in 0..35) {
                        if (itemStack.isAir) {
                            // 取出物品：从实际位置删除
                            val item = ui.getItemByVirtualSlot(slot)
                            if (item != null) {
                                inventory.setItem(slot, null)
                                ZephyrionAPI.removeItem(vault, item.page, item.slot, opener)
                                // 延迟刷新，等待数据库操作完成
                                submitAsync(delay = 2L) {
                                    sync { ui.refreshSearchResults() }
                                }
                            }
                        } else {
                            // 放入物品：找到实际的可用槽位
                            val available = ui.findFirstAvailableSlot()
                            if (available != null) {
                                val (targetPage, targetSlot) = available
                                ZephyrionAPI.setItem(vault, targetPage, targetSlot, itemStack, opener)
                                // 延迟刷新，等待数据库操作完成
                                submitAsync(delay = 2L) {
                                    sync { ui.refreshSearchResults() }
                                }
                            } else {
                                // 没有可用槽位，返回物品给玩家
                                opener.inventory.addItem(itemStack)
                            }
                        }
                    }
                }

                readItem { inventory, slot ->
                    val index = (currentSearchPage - 1) * 36 + slot
                    searchResults.getOrNull(index)?.itemStack
                }
            }

            onBuild { player, inventory ->
                // 显示搜索结果
                pageItems.forEachIndexed { index, item ->
                    inventory.setItem(index, item.itemStack)
                }
                setSearchModeElements(this, inventory)
            }

            onClose { event ->
                val player = event.player as Player
                VaultSearchCloseEvent(vault, params.toMap(), event.inventory, player).call()
            }

            setSearchPageTurnItems(this)
            setSearchInfoItem(this)
            setSearchReturnItem(this)
            setClickEvent(this)
        }
    }

    /**
     * 刷新搜索结果并重新打开界面
     */
    fun refreshSearchResults() {
        searchResults = ZephyrionAPI.searchItems(vault, params, opener)
        // 如果当前页超出范围，回到第一页
        if (searchPage > getSearchMaxPage()) {
            searchPage = 1
        }
        open()
    }

    /**
     * 设置搜索模式的翻页按钮
     */
    fun setSearchPageTurnItems(menu: StorableChest) {
        menu.set(48) {
            if (searchPage == 1) {
                buildItem(XMaterial.BARRIER) {
                    name = opener.asLangText("vault-search-prev-page-disabled")
                }
            } else {
                buildItem(XMaterial.ARROW) {
                    name = opener.asLangText("vault-search-prev-page")
                }
            }
        }
        menu.set(50) {
            if (searchPage >= getSearchMaxPage()) {
                buildItem(XMaterial.BARRIER) {
                    name = opener.asLangText("vault-search-next-page-disabled")
                }
            } else {
                buildItem(XMaterial.ARROW) {
                    name = opener.asLangText("vault-search-next-page")
                }
            }
        }
        menu.onClick(48) { event ->
            if (searchPage > 1) {
                searchPage--
                open()
            }
        }
        menu.onClick(50) { event ->
            if (searchPage < getSearchMaxPage()) {
                searchPage++
                open()
            }
        }
    }

    /**
     * 设置搜索信息按钮（替代普通模式的搜索按钮）
     */
    fun setSearchInfoItem(menu: StorableChest) {
        menu.set(49) {
            buildItem(XMaterial.WRITABLE_BOOK) {
                name = opener.asLangText("vault-search-info-title")
                lore.clear()
                lore += opener.asLangText("vault-search-info-count", searchResults.size)
                lore += opener.asLangText("vault-search-info-page", searchPage, getSearchMaxPage())
                lore += ""
                params.forEach { (key, value) ->
                    lore += opener.asLangText("vault-search-info-condition", key, value)
                }
                lore += ""
                lore += opener.asLangText("vault-search-info-click-to-modify")
            }
        }
        menu.onClick(49) { event ->
            searchUI.open()
        }
    }

    /**
     * 设置搜索模式的返回按钮
     */
    fun setSearchReturnItem(menu: StorableChest) {
        menu.set(53) {
            buildItem(XMaterial.RED_STAINED_GLASS_PANE) {
                name = opener.asLangText("vault-search-exit")
            }
        }
        menu.onClick(53) { event ->
            // 退出搜索模式前，先发送关闭事件以正确注销
            VaultSearchCloseEvent(vault, params.toMap(), event.inventory, opener).call()
            // 退出搜索模式，返回正常模式
            params.clear()
            searchResults = emptyList()
            searchPage = 1
            page = 1
            open()
        }
    }

    /**
     * 设置搜索模式的元素
     */
    fun setSearchModeElements(menu: StorableChest, inventory: Inventory) {
        // 搜索模式下不显示解锁按钮
        // 如果没有搜索结果，显示提示
        if (searchResults.isEmpty()) {
            inventory.setItem(13, buildItem(XMaterial.BARRIER) {
                name = opener.asLangText("vault-search-no-results")
            })
        }
    }

    fun setProperties(menu: Chest) {
        menu.rows(6)
        menu.handLocked(false)
    }

    fun setElements(menu: StorableChest, inventory: Inventory) {
        // 仅在正常模式下使用，搜索模式使用 setSearchModeElements
        if (page == vault.getMaxPage()) {
            val ownerData =  ZephyrionAPI.getUserData(vault.workspace.owner)
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
                        if (vault.workspace.owner != clicker.uniqueId.toString()&&
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
                                            VaultSyncService.refreshAllViewers(vault, page)
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

    fun setPageTurnItems(menu: StorableChest) {
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
            if (page == vault.getMaxPage()) {
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
                VaultUI(event.clicker, vault, root, page - 1).open()
            }
        }
        menu.onClick(50) { event ->
            if (page != vault.getMaxPage()) {
                VaultUI(event.clicker, vault, root, page + 1).open()
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
        // 权限检查
        if (!ZephyrionAPI.isPluginAdmin(opener) &&
            !vault.workspace.isMember(opener.uniqueId.toString())) {
            return
        }

        val inv = build()
        opener.openInventory(inv)

        if (isSearchMode()) {
            VaultSearchOpenEvent(vault, params.toMap(), inv, opener).call()
        } else {
            VaultOpenEvent(vault, page, inv, opener).call()
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
        if (params.isEmpty()) return
        searchResults = ZephyrionAPI.searchItems(vault, params, opener)
        searchPage = 1
        open()
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
    }
}

