package com.faithl.zephyrion.core.cache

import com.faithl.zephyrion.core.models.Vault
import com.faithl.zephyrion.core.models.Workspace
import com.faithl.zephyrion.storage.DatabaseConfig
import com.faithl.zephyrion.storage.cache.CacheConfig
import com.faithl.zephyrion.storage.cache.get

object VaultCache {

    private const val KEY_PREFIX = "vault"
    private const val KEY_WORKSPACE_PREFIX = "vault:workspace"

    private val provider get() = CacheConfig.provider
    private val ttl get() = CacheConfig.defaultTtlMs

    fun getById(id: Int): Vault? {
        val key = "$KEY_PREFIX:$id"
        val cached = provider.get<Vault>(key)
        if (cached != null) {
            return cached
        }
        val vault = loadByIdFromDb(id) ?: return null
        provider.set(key, vault, ttl)
        return vault
    }

    fun getByWorkspace(workspace: Workspace): List<Vault> {
        val key = "$KEY_WORKSPACE_PREFIX:${workspace.id}"
        val cached = provider.get<List<Vault>>(key)
        if (cached != null) {
            return cached
        }
        val vaults = loadByWorkspaceFromDb(workspace)
        provider.set(key, vaults, ttl)
        vaults.forEach { provider.set("$KEY_PREFIX:${it.id}", it, ttl) }
        return vaults
    }

    fun update(vault: Vault) {
        provider.set("$KEY_PREFIX:${vault.id}", vault, ttl)
        provider.delete("$KEY_WORKSPACE_PREFIX:${vault.workspaceId}")
    }

    fun invalidate(vaultId: Int) {
        val vault = provider.get<Vault>("$KEY_PREFIX:$vaultId")
        provider.delete("$KEY_PREFIX:$vaultId")
        vault?.let { provider.delete("$KEY_WORKSPACE_PREFIX:${it.workspaceId}") }
    }

    fun invalidateWorkspaceVaults(workspaceId: Int) {
        provider.delete("$KEY_WORKSPACE_PREFIX:$workspaceId")
    }

    private fun loadByIdFromDb(id: Int): Vault? {
        val table = DatabaseConfig.vaultsTable
        val dataSource = DatabaseConfig.dataSource

        return table.select(dataSource) {
            where { "id" eq id }
        }.firstOrNull {
            Vault(
                id = getInt("id"),
                name = getString("name"),
                desc = getString("description"),
                workspaceId = getInt("workspace_id"),
                size = getInt("size"),
                createdAt = getLong("created_at"),
                updatedAt = getLong("updated_at")
            )
        }
    }

    private fun loadByWorkspaceFromDb(workspace: Workspace): List<Vault> {
        val table = DatabaseConfig.vaultsTable
        val dataSource = DatabaseConfig.dataSource

        return table.select(dataSource) {
            where { "workspace_id" eq workspace.id }
        }.map {
            Vault(
                id = getInt("id"),
                name = getString("name"),
                desc = getString("description"),
                workspaceId = getInt("workspace_id"),
                size = getInt("size"),
                createdAt = getLong("created_at"),
                updatedAt = getLong("updated_at")
            )
        }
    }
}
