package com.faithl.zephyrion.core.services

import com.faithl.zephyrion.Zephyrion
import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.core.models.*
import com.faithl.zephyrion.core.models.AutoPickup
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.module.nms.getName
import taboolib.platform.util.sendLang
import java.util.concurrent.ConcurrentHashMap

object AutoPickupService {

    private val playerVaultCache = ConcurrentHashMap<String, List<Vault>>()

    init {
        submit(period = 20 * 60 * 5, delay = 20 * 60 * 5, async = true) {
            invalidateAllCache()
        }
    }

    @SubscribeEvent
    fun onPlayerJoin(event: PlayerJoinEvent) {
        invalidateCache(event.player)
    }

    @SubscribeEvent
    fun onPlayerQuit(event: PlayerQuitEvent) {
        invalidateCache(event.player)
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityPickupItem(event: EntityPickupItemEvent) {
        if (event.entity !is Player) return
        val player = event.entity as Player
        val itemStack = event.item.itemStack

        val permission = Zephyrion.permissions.getString("auto-pick-items") ?: "zephyrion.auto-pick-items"
        if (!player.hasPermission(permission) && !ZephyrionAPI.isPluginAdmin(player)) {
            return
        }

        val vaultRules = getOrCacheVaultRules(player)
        if (vaultRules.isEmpty()) {
            return
        }

        for (vault in vaultRules) {
            val shouldPickup = com.faithl.zephyrion.core.models.AutoPickup.shouldAutoPickup(itemStack, vault)
            if (shouldPickup == true) {
                val result = storeItemInVault(vault, itemStack, player)
                if (result) {
                    event.isCancelled = true
                    event.item.remove()

                    val itemName = itemStack.getName()
                    player.sendLang("auto-pickup-item-picked", itemName, vault.name)
                    return
                }
            }
        }
    }

    private fun getOrCacheVaultRules(player: Player): List<Vault> {
        val playerId = player.uniqueId.toString()

        val cached = playerVaultCache[playerId]
        if (cached != null) {
            return cached
        }

        val vaults = transaction {
            val workspaces = ZephyrionAPI.getJoinedWorkspaces(player)
            val result = mutableListOf<Vault>()

            for (workspace in workspaces) {
                result.addAll(ZephyrionAPI.getVaults(workspace))
            }
            result.filter { vault ->
                AutoPickup.getAutoPickups(vault).isNotEmpty()
            }
        }

        playerVaultCache[playerId] = vaults
        return vaults
    }

    fun invalidateCache(player: Player) {
        playerVaultCache.remove(player.uniqueId.toString())
    }

    fun invalidateAllCache() {
        playerVaultCache.clear()
    }

    private fun storeItemInVault(vault: Vault, itemStack: ItemStack, player: Player): Boolean {
        return transaction {
            val vaultSize = vault.size
            if (vaultSize <= 0) {
                return@transaction false
            }

            val maxPage = vault.getMaxPage()
            val playerUuid = player.uniqueId.toString()
            val isIndependent = vault.workspace.type == Workspaces.Type.INDEPENDENT

            val allExistingItems = if (isIndependent) {
                Item.find {
                    (Items.vault eq vault.id) and (Items.owner eq playerUuid)
                }.toList()
            } else {
                Item.find { Items.vault eq vault.id }.toList()
            }

            val usedSlotsByPage = allExistingItems.groupBy { it.page }
                .mapValues { (_, items) -> items.map { it.slot }.toSet() }

            for (page in 1..maxPage) {
                val slotsInPage = minOf(page * 36, vaultSize) - (page - 1) * 36
                val usedSlots = usedSlotsByPage[page] ?: emptySet()

                for (slot in 0 until slotsInPage) {
                    if (slot !in usedSlots) {
                        Item.new {
                            this.vault = vault
                            this.page = page
                            this.slot = slot
                            this.itemStack = itemStack
                            if (isIndependent) {
                                this.owner = playerUuid
                            }
                        }
                        return@transaction true
                    }
                }
            }
            false
        }
    }

}
