package com.faithl.zephyrion.core.cache

import com.faithl.zephyrion.storage.cache.CacheConfig
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submitAsync

/**
 * 缓存服务 - 负责事件监听、预加载和全局缓存操作
 */
object CacheService {

    private val provider get() = CacheConfig.provider

    /**
     * 预加载玩家相关的所有缓存数据
     */
    fun preloadPlayerData(player: Player) {
        submitAsync {
            val playerUUID = player.uniqueId.toString()

            // 预加载配额
            QuotaCache.get(playerUUID)

            // 预加载工作空间
            val workspaces = WorkspaceCache.getJoinedWorkspaces(player)

            // 预加载保险库和自动拾取规则
            workspaces.forEach { workspace ->
                val vaults = VaultCache.getByWorkspace(workspace)
                vaults.forEach { vault ->
                    AutoPickupCache.get(vault,player.uniqueId.toString())
                }
            }
        }
    }

    /**
     * 清理玩家相关的所有缓存
     */
    fun clearPlayerCache(playerUUID: String) {
        QuotaCache.invalidate(playerUUID)
        WorkspaceCache.invalidatePlayerWorkspaces(playerUUID)
    }

    /**
     * 清理所有缓存
     */
    fun clearAllCache() {
        provider.clear()
    }

    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): Map<String, Int> {
        return mapOf(
            "quota" to provider.keys("quota").size,
            "workspace" to provider.keys("workspace").size,
            "vault" to provider.keys("vault").size,
            "autopickup" to provider.keys("autopickup").size,
            "items" to provider.keys("items").size
        )
    }

    // ==================== 事件监听 ====================

    @SubscribeEvent
    fun onPlayerJoin(event: PlayerJoinEvent) {
        preloadPlayerData(event.player)
    }

    @SubscribeEvent
    fun onPlayerQuit(event: PlayerQuitEvent) {
        clearPlayerCache(event.player.uniqueId.toString())
    }
}
