package com.faithl.zephyrion.core.cache

import com.faithl.zephyrion.core.models.Workspace
import com.faithl.zephyrion.core.models.WorkspaceType
import com.faithl.zephyrion.storage.DatabaseConfig
import com.faithl.zephyrion.storage.cache.CacheConfig
import com.faithl.zephyrion.storage.cache.get
import org.bukkit.entity.Player

object WorkspaceCache{

    private const val KEY_PREFIX = "workspace"
    private const val KEY_INDEPENDENT = "workspace:independent"
    private const val KEY_PLAYER_PREFIX = "workspace:player"

    private val provider get() = CacheConfig.provider
    private val ttl get() = CacheConfig.defaultTtlMs

    fun getById(id: Int): Workspace? {
        val key = "$KEY_PREFIX:$id"
        val cached = provider.get<Workspace>(key)
        if (cached != null) {
            return cached
        }
        val workspace = loadByIdFromDb(id) ?: return null
        provider.set(key, workspace, ttl)
        return workspace
    }

    fun getJoinedWorkspaces(player: Player): List<Workspace> {
        val playerUUID = player.uniqueId.toString()
        val key = "$KEY_PLAYER_PREFIX:$playerUUID"
        val cached = provider.get<List<Workspace>>(key)
        if (cached != null) {
            return cached
        }
        val workspaces = loadJoinedFromDb(player)
        provider.set(key, workspaces, ttl)
        workspaces.forEach { provider.set("$KEY_PREFIX:${it.id}", it, ttl) }
        return workspaces
    }

    fun getIndependent(): Workspace? {
        val cached = provider.get<Workspace>(KEY_INDEPENDENT)
        if (cached != null) {
            return cached
        }
        val workspace = loadIndependentFromDb()
        if (workspace != null) {
            provider.set(KEY_INDEPENDENT, workspace, ttl)
            provider.set("$KEY_PREFIX:${workspace.id}", workspace, ttl)
        }
        return workspace
    }

    fun update(workspace: Workspace) {
        provider.set("$KEY_PREFIX:${workspace.id}", workspace, ttl)
        workspace.members.split(",").filter { it.isNotBlank() }.forEach { uuid ->
            provider.delete("$KEY_PLAYER_PREFIX:$uuid")
        }
    }

    fun invalidate(workspaceId: Int) {
        provider.delete("$KEY_PREFIX:$workspaceId")
        provider.deleteByPrefix(KEY_PLAYER_PREFIX)
    }

    fun invalidatePlayerWorkspaces(playerUUID: String) {
        provider.delete("$KEY_PLAYER_PREFIX:$playerUUID")
    }

    private fun loadByIdFromDb(id: Int): Workspace? {
        val table = DatabaseConfig.workspacesTable
        val dataSource = DatabaseConfig.dataSource

        return table.select(dataSource) {
            where { "id" eq id }
        }.firstOrNull {
            Workspace(
                id = getInt("id"),
                name = getString("name"),
                desc = getString("description"),
                type = WorkspaceType.valueOf(getString("type")),
                owner = getString("owner"),
                members = getString("members"),
                createdAt = getLong("created_at"),
                updatedAt = getLong("updated_at")
            )
        }
    }

    private fun loadJoinedFromDb(player: Player): List<Workspace> {
        val table = DatabaseConfig.workspacesTable
        val dataSource = DatabaseConfig.dataSource
        val playerUUID = player.uniqueId.toString()

        return table.select(dataSource) {
            where { "members" like "%$playerUUID%" }
        }.map {
            Workspace(
                id = getInt("id"),
                name = getString("name"),
                desc = getString("description"),
                type = WorkspaceType.valueOf(getString("type")),
                owner = getString("owner"),
                members = getString("members"),
                createdAt = getLong("created_at"),
                updatedAt = getLong("updated_at")
            )
        }
    }

    private fun loadIndependentFromDb(): Workspace? {
        val table = DatabaseConfig.workspacesTable
        val dataSource = DatabaseConfig.dataSource

        return table.select(dataSource) {
            where { "name" eq "Independent" }
        }.firstOrNull {
            Workspace(
                id = getInt("id"),
                name = getString("name"),
                desc = getString("description"),
                type = WorkspaceType.valueOf(getString("type")),
                owner = getString("owner"),
                members = getString("members"),
                createdAt = getLong("created_at"),
                updatedAt = getLong("updated_at")
            )
        }
    }
}
