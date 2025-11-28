package com.faithl.zephyrion.core.services

import com.faithl.zephyrion.api.events.VaultAddItemEvent
import com.faithl.zephyrion.api.events.VaultCloseEvent
import com.faithl.zephyrion.api.events.VaultOpenEvent
import com.faithl.zephyrion.api.events.VaultRemoveItemEvent
import com.faithl.zephyrion.api.events.VaultSearchCloseEvent
import com.faithl.zephyrion.api.events.VaultSearchOpenEvent
import com.faithl.zephyrion.core.models.Vault
import com.faithl.zephyrion.core.models.WorkspaceType
import com.faithl.zephyrion.core.ui.vault.VaultUI
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.module.ui.InventoryViewProxy
import taboolib.module.ui.MenuHolder


object VaultSyncService {

    data class ViewerGroup(
        val vaultId: Int,
        val page: Int,
        val viewers: MutableList<Player> = mutableListOf()
    )

    data class SearchViewerGroup(
        val vaultId: Int,
        val params: Map<String, String>,
        val viewers: MutableList<Player> = mutableListOf()
    )

    private val groups = mutableSetOf<ViewerGroup>()
    private val searchGroups = mutableSetOf<SearchViewerGroup>()

    private fun register(vault: Vault, page: Int, player: Player) {
        if (vault.workspace.type == WorkspaceType.INDEPENDENT) return

        val group = groups.find { it.vaultId == vault.id && it.page == page }
            ?: ViewerGroup(vault.id, page).also { groups.add(it) }

        if (!group.viewers.contains(player)) {
            group.viewers.add(player)
        }
    }

    private fun unregister(vault: Vault, page: Int, player: Player) {
        if (vault.workspace.type == WorkspaceType.INDEPENDENT) return

        val group = groups.find { it.vaultId == vault.id && it.page == page } ?: return
        group.viewers.remove(player)

        if (group.viewers.isEmpty()) {
            groups.remove(group)
        }
    }

    private fun registerSearch(vault: Vault, params: Map<String, String>, player: Player) {
        if (vault.workspace.type == WorkspaceType.INDEPENDENT) return

        val group = searchGroups.find { it.vaultId == vault.id && it.params == params }
            ?: SearchViewerGroup(vault.id, params).also { searchGroups.add(it) }

        if (!group.viewers.contains(player)) {
            group.viewers.add(player)
        }
    }

    private fun unregisterSearch(vault: Vault, player: Player) {
        if (vault.workspace.type == WorkspaceType.INDEPENDENT) return

        val affectedGroups = searchGroups.filter { it.vaultId == vault.id }.toList()
        affectedGroups.forEach { group ->
            group.viewers.remove(player)
            if (group.viewers.isEmpty()) {
                searchGroups.remove(group)
            }
        }
    }

    private fun syncItemChange(vault: Vault, page: Int, slot: Int, itemStack: ItemStack?, operator: Player?) {
        if (slot !in 0..35) return

        if (vault.workspace.type == WorkspaceType.INDEPENDENT) {
            if (operator != null) {
                val group = groups.find { it.vaultId == vault.id && it.page == page }
                if (group != null && group.viewers.contains(operator)) {
                    submit {
                        val inv = InventoryViewProxy.getTopInventory(operator.openInventory)
                        if (inv.holder is MenuHolder) {
                            inv.setItem(slot, itemStack)
                        }
                    }
                }
            }
            return
        }

        val group = groups.find { it.vaultId == vault.id && it.page == page }
        if (group != null) {
            group.viewers.removeIf { !it.isOnline }
            group.viewers.forEach { player ->
                submit {
                    val inv = InventoryViewProxy.getTopInventory(player.openInventory)
                    if (inv.holder is MenuHolder) {
                        inv.setItem(slot, itemStack)
                    }
                }
            }
        }

        refreshSearchViewers(vault, operator)
    }

    private fun refreshSearchViewers(vault: Vault, operator: Player?) {
        val affectedGroups = searchGroups.filter { it.vaultId == vault.id }.toList()

        affectedGroups.forEach { group ->
            group.viewers.removeIf { !it.isOnline }

            val viewersToRefresh = if (vault.workspace.type == WorkspaceType.INDEPENDENT) {
                if (operator != null && group.viewers.contains(operator)) {
                    listOf(operator)
                } else {
                    emptyList()
                }
            } else {
                group.viewers.toList()
            }

            viewersToRefresh.forEach { player ->
                submit {
                    val newUI = VaultUI(player, vault)
                    newUI.params.putAll(group.params)
                    newUI.refreshSearchResults()
                }
            }
        }
    }

    fun refreshAllViewers(vault: Vault, page: Int) {
        if (vault.workspace.type == WorkspaceType.INDEPENDENT) return

        val group = groups.find { it.vaultId == vault.id && it.page == page } ?: return

        group.viewers.removeIf { !it.isOnline }

        val players = group.viewers.toList()
        groups.remove(group)

        players.forEach { player ->
            player.closeInventory()
            VaultUI(player, vault, page = page).open()
        }
    }

    @SubscribeEvent
    fun onVaultOpen(e: VaultOpenEvent) {
        register(e.vault, e.page, e.opener)
    }

    @SubscribeEvent
    fun onVaultClose(e: VaultCloseEvent) {
        unregister(e.vault, e.page, e.closer)
    }

    @SubscribeEvent
    fun onVaultSearchOpen(e: VaultSearchOpenEvent) {
        registerSearch(e.vault, e.params, e.opener)
    }

    @SubscribeEvent
    fun onVaultSearchClose(e: VaultSearchCloseEvent) {
        unregisterSearch(e.vault, e.closer)
    }

    @SubscribeEvent
    fun onVaultAddItem(e: VaultAddItemEvent) {
        syncItemChange(e.vault, e.page, e.slot, e.itemStack, e.operator)
    }

    @SubscribeEvent
    fun onVaultRemoveItem(e: VaultRemoveItemEvent) {
        syncItemChange(e.vault, e.page, e.slot, null, e.operator)
    }
}
