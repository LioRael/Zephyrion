package com.faithl.zephyrion.core.cache

import com.faithl.zephyrion.core.models.Vault
import com.faithl.zephyrion.storage.DatabaseConfig
import com.faithl.zephyrion.storage.cache.CacheConfig
import com.faithl.zephyrion.storage.cache.get

object SettingCache {

    private const val KEY_PREFIX = "setting"

    private val provider get() = CacheConfig.provider
    private val ttl get() = CacheConfig.defaultTtlMs

    /**
     * 获取设置值（使用缓存）
     */
    fun get(vaultId: Int, setting: String, owner: String): String? {
        val key = buildKey(vaultId, setting, owner)
        val cached = provider.get<String>(key)
        if (cached != null) {
            return cached
        }
        val value = loadFromDb(vaultId, setting, owner) ?: return null
        provider.set(key, value, ttl)
        return value
    }

    /**
     * 设置缓存值
     */
    fun set(vaultId: Int, setting: String, owner: String, value: String) {
        provider.set(buildKey(vaultId, setting, owner), value, ttl)
    }

    /**
     * 使单个设置缓存失效
     */
    fun invalidate(vaultId: Int, setting: String, owner: String) {
        provider.delete(buildKey(vaultId, setting, owner))
    }

    /**
     * 使指定仓库和用户的所有设置缓存失效
     */
    fun invalidateByOwner(vaultId: Int, owner: String) {
        provider.deleteByPrefix("$KEY_PREFIX:$vaultId:$owner:")
    }

    /**
     * 使指定仓库的所有设置缓存失效
     */
    fun invalidateByVault(vaultId: Int) {
        provider.deleteByPrefix("$KEY_PREFIX:$vaultId:")
    }

    /**
     * 使所有设置缓存失效
     */
    fun invalidateAll() {
        provider.deleteByPrefix(KEY_PREFIX)
    }

    private fun buildKey(vaultId: Int, setting: String, owner: String): String {
        return "$KEY_PREFIX:$vaultId:$owner:$setting"
    }

    private fun loadFromDb(vaultId: Int, setting: String, owner: String): String? {
        val table = DatabaseConfig.settingsTable
        val dataSource = DatabaseConfig.dataSource

        return table.select(dataSource) {
            where {
                "vault_id" eq vaultId
                and { "setting" eq setting }
                and { "owner" eq owner }
            }
        }.firstOrNull {
            getString("value")
        }
    }
}
