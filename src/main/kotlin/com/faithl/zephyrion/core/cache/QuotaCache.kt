package com.faithl.zephyrion.core.cache

import com.faithl.zephyrion.Zephyrion
import com.faithl.zephyrion.core.models.Quota
import com.faithl.zephyrion.storage.DatabaseConfig
import com.faithl.zephyrion.storage.cache.CacheConfig
import com.faithl.zephyrion.storage.cache.get
import taboolib.module.database.HostSQL
import taboolib.module.database.HostSQLite

object QuotaCache {

    private const val KEY_PREFIX = "quota"

    private val provider get() = CacheConfig.provider
    private val ttl get() = CacheConfig.defaultTtlMs

    fun get(playerUniqueId: String): Quota {
        val key = "$KEY_PREFIX:$playerUniqueId"
        val cached = provider.get<Quota>(key)
        if (cached != null) {
            return cached
        }
        val quota = loadFromDb(playerUniqueId)
        provider.set(key, quota, ttl)
        return quota
    }

    fun update(playerUniqueId: String, quota: Quota) {
        provider.set("$KEY_PREFIX:$playerUniqueId", quota, ttl)
    }

    fun invalidate(playerUniqueId: String) {
        provider.delete("$KEY_PREFIX:$playerUniqueId")
    }

    private fun loadFromDb(playerUniqueId: String): Quota {
        val table = DatabaseConfig.quotasTable
        val dataSource = DatabaseConfig.dataSource

        val existing = table.select(dataSource) {
            where { "player" eq playerUniqueId }
        }.firstOrNull {
            Quota(
                id = getInt("id"),
                player = getString("player"),
                workspaceQuotas = getInt("workspace_quotas"),
                workspaceUsed = getInt("workspace_used"),
                sizeQuotas = getInt("size_quotas"),
                sizeUsed = getInt("size_used"),
                unlimited = when (DatabaseConfig.host) {
                    is HostSQL -> getBoolean("unlimited")
                    is HostSQLite -> getInt("unlimited") != 0
                    else -> false
                }
            )
        }

        if (existing != null) {
            return existing
        }

        // 创建默认配额
        val defaultWorkspace = Zephyrion.settings.getInt("user.default-quotas.workspace")
        val defaultSize = Zephyrion.settings.getInt("user.default-quotas.size")
        val defaultUnlimited = Zephyrion.settings.getBoolean("user.default-quotas.unlimited")

        table.insert(dataSource, "player", "workspace_quotas", "workspace_used", "size_quotas", "size_used", "unlimited") {
            value(
                playerUniqueId,
                defaultWorkspace,
                0,
                defaultSize,
                0,
                if (DatabaseConfig.host is HostSQLite) (if (defaultUnlimited) 1 else 0) else defaultUnlimited
            )
        }

        return Quota(
            id = 0,
            player = playerUniqueId,
            workspaceQuotas = defaultWorkspace,
            workspaceUsed = 0,
            sizeQuotas = defaultSize,
            sizeUsed = 0,
            unlimited = defaultUnlimited
        )
    }
}
