package com.faithl.zephyrion.core.cache

import com.faithl.zephyrion.core.models.AutoPickup
import com.faithl.zephyrion.core.models.AutoPickupType
import com.faithl.zephyrion.core.models.Vault
import com.faithl.zephyrion.storage.DatabaseConfig
import com.faithl.zephyrion.storage.cache.CacheConfig
import com.faithl.zephyrion.storage.cache.get

object AutoPickupCache {

    private const val KEY_PREFIX = "autopickup"

    private val provider get() = CacheConfig.provider
    private val ttl get() = CacheConfig.defaultTtlMs

    fun get(vault: Vault): List<AutoPickup> {
        val key = "$KEY_PREFIX:${vault.id}"
        val cached = provider.get<List<AutoPickup>>(key)
        if (cached != null) {
            return cached
        }
        val rules = loadFromDb(vault)
        provider.set(key, rules, ttl)
        return rules
    }

    fun update(vaultId: Int, rules: List<AutoPickup>) {
        provider.set("$KEY_PREFIX:$vaultId", rules, ttl)
    }

    fun invalidate(vaultId: Int) {
        provider.delete("$KEY_PREFIX:$vaultId")
    }

    fun invalidateAll() {
        provider.deleteByPrefix(KEY_PREFIX)
    }

    /**
     * 批量加载多个保险库的自动拾取规则
     */
    fun batchLoad(vaults: List<Vault>): Map<Int, List<AutoPickup>> {
        if (vaults.isEmpty()) return emptyMap()

        val result = mutableMapOf<Int, List<AutoPickup>>()
        val uncachedVaultIds = mutableListOf<Int>()

        vaults.forEach { vault ->
            val cached = provider.get<List<AutoPickup>>("$KEY_PREFIX:${vault.id}")
            if (cached != null) {
                result[vault.id] = cached
            } else {
                uncachedVaultIds.add(vault.id)
            }
        }

        if (uncachedVaultIds.isNotEmpty()) {
            val table = DatabaseConfig.autoPickupsTable
            val dataSource = DatabaseConfig.dataSource

            val allRules = table.select(dataSource) {
                where { "vault_id" inside arrayOf(uncachedVaultIds) }
            }.map {
                AutoPickup(
                    id = getInt("id"),
                    type = AutoPickupType.valueOf(getString("type")),
                    value = getString("value"),
                    vaultId = getInt("vault_id"),
                    createdAt = getLong("created_at"),
                    updatedAt = getLong("updated_at")
                )
            }

            val groupedRules = allRules.groupBy { it.vaultId }

            uncachedVaultIds.forEach { vaultId ->
                val rules = groupedRules[vaultId] ?: emptyList()
                provider.set("$KEY_PREFIX:$vaultId", rules, ttl)
                result[vaultId] = rules
            }
        }

        return result
    }

    private fun loadFromDb(vault: Vault): List<AutoPickup> {
        val table = DatabaseConfig.autoPickupsTable
        val dataSource = DatabaseConfig.dataSource

        return table.select(dataSource) {
            where { "vault_id" eq vault.id }
        }.map {
            AutoPickup(
                id = getInt("id"),
                type = AutoPickupType.valueOf(getString("type")),
                value = getString("value"),
                vaultId = getInt("vault_id"),
                createdAt = getLong("created_at"),
                updatedAt = getLong("updated_at")
            )
        }
    }
}
