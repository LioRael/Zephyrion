package com.faithl.zephyrion.core.models

import com.faithl.zephyrion.Zephyrion
import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.core.cache.QuotaCache
import com.faithl.zephyrion.core.cache.VaultCache
import com.faithl.zephyrion.core.cache.WorkspaceCache
import com.faithl.zephyrion.storage.DatabaseConfig
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import taboolib.module.database.Table
import java.text.SimpleDateFormat
import java.util.*


enum class WorkspaceType {
    PUBLIC, PRIVATE, INDEPENDENT
}

data class Workspace(
    var id: Int,
    var name: String,
    var desc: String?,
    var type: WorkspaceType,
    var owner: String,
    var members: String,
    var createdAt: Long,
    var updatedAt: Long
) : java.io.Serializable {

    companion object {
        private const val serialVersionUID = 1L

        private val table: Table<*, *>
            get() = DatabaseConfig.workspacesTable

        private val dataSource
            get() = DatabaseConfig.dataSource

        fun initializeIndependentWorkspace() {
            if (!Zephyrion.settings.getBoolean("workspace.independent")) {
                return
            }

            val dataSource = DatabaseConfig.dataSource
            val table = DatabaseConfig.workspacesTable

            // 检查是否已存在
            val exists = table.select(dataSource) {
                where { "name" eq "Independent" }
            }.find()

            if (!exists) {
                table.insert(dataSource, "name", "description", "type", "owner", "members", "created_at", "updated_at") {
                    value(
                        "Independent",
                        "Independent workspace",
                        "INDEPENDENT",
                        "Server",
                        "",
                        System.currentTimeMillis(),
                        System.currentTimeMillis()
                    )
                }
            }
        }

        fun findById(id: Int): Workspace? {
            return WorkspaceCache.getById(id)
        }

        fun getIndependentWorkspace(): Workspace? {
            return WorkspaceCache.getIndependent()
        }

        fun getJoinedWorkspaces(player: Player): List<Workspace> {
            return WorkspaceCache.getJoinedWorkspaces(player)
        }

        fun getWorkspace(player: String, name: String): Workspace? {
            // 从数据库查询
            val workspace = table.select(dataSource) {
                where {
                    "members" like "%$player%"
                    and { "name" eq name }
                }
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

            // 更新缓存
            workspace?.let { WorkspaceCache.update(it) }
            return workspace
        }

        /**
         * 创建工作空间（数据库操作）
         */
        fun create(owner: String, name: String, type: WorkspaceType, desc: String?): Boolean {
            val quotasTable = DatabaseConfig.quotasTable
            val ownerData = Quota.getUser(owner)
            val newUsed = ownerData.workspaceUsed + 1

            if (newUsed > ownerData.workspaceQuotas) {
                return false
            }

            // 乐观锁更新配额
            val affected = quotasTable.update(dataSource) {
                set("workspace_used", newUsed)
                where {
                    "player" eq owner
                    and { "workspace_used" eq ownerData.workspaceUsed }
                }
            }

            if (affected == 0) {
                return false
            }

            // 插入工作空间
            table.insert(dataSource, "name", "description", "type", "owner", "members", "created_at", "updated_at") {
                value(
                    name,
                    desc,
                    type.name,
                    owner,
                    owner,
                    System.currentTimeMillis(),
                    System.currentTimeMillis()
                )
            }

            // 更新缓存
            ownerData.workspaceUsed = newUsed
            QuotaCache.update(owner, ownerData)
            WorkspaceCache.invalidatePlayerWorkspaces(owner)

            return true
        }

        fun addMember(workspace: Workspace, player: Player): Boolean {
            if (workspace.members.contains(player.uniqueId.toString())) {
                return false
            }

            workspace.members = workspace.members + "," + player.uniqueId.toString()
            workspace.updatedAt = System.currentTimeMillis()

            table.update(dataSource) {
                set("members", workspace.members)
                set("updated_at", workspace.updatedAt)
                where { "id" eq workspace.id }
            }
            WorkspaceCache.update(workspace)
            WorkspaceCache.invalidatePlayerWorkspaces(player.uniqueId.toString())
            return true
        }

        fun removeMember(workspace: Workspace, player: OfflinePlayer): Boolean {
            if (workspace.owner == player.uniqueId.toString()) {
                return false
            }

            if (!workspace.members.contains(player.uniqueId.toString())) {
                return false
            }

            workspace.members = workspace.members.replace("," + player.uniqueId.toString(), "")
            workspace.updatedAt = System.currentTimeMillis()

            table.update(dataSource) {
                set("members", workspace.members)
                set("updated_at", workspace.updatedAt)
                where { "id" eq workspace.id }
            }
            WorkspaceCache.update(workspace)
            WorkspaceCache.invalidatePlayerWorkspaces(player.uniqueId.toString())
            return true
        }
    }

    fun rename(newName: String): ZephyrionAPI.Result {
        val result = ZephyrionAPI.validateWorkspaceName(newName, getOwner().uniqueId.toString())
        if (!result.success) {
            return result
        }

        name = newName
        updatedAt = System.currentTimeMillis()

        table.update(dataSource) {
            set("name", name)
            set("updated_at", updatedAt)
            where { "id" eq id }
        }
        WorkspaceCache.update(this)
        return ZephyrionAPI.Result(true)
    }

    /**
     * 更新工作空间描述
     */
    fun updateDesc(newDesc: String?) {
        desc = newDesc
        updatedAt = System.currentTimeMillis()

        table.update(dataSource) {
            set("description", desc)
            set("updated_at", updatedAt)
            where { "id" eq id }
        }
        WorkspaceCache.update(this)
    }

    fun getCreatedAt(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(createdAt))
    }

    /**
     * 获取更新时间(格式化)
     */
    fun getUpdatedAt(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(updatedAt))
    }

    /**
     * 获取拥有者
     */
    fun getOwner(): OfflinePlayer {
        return Bukkit.getOfflinePlayer(UUID.fromString(owner))
    }

    /**
     * 获取所有成员
     */
    fun getMembers(): List<OfflinePlayer> {
        return members.split(",").filter { it.isNotBlank() }.map {
            Bukkit.getOfflinePlayer(UUID.fromString(it))
        }
    }

    /**
     * 获取所有成员名称
     */
    fun getMembersName(): List<String> {
        return members.split(",").filter { it.isNotBlank() }.mapNotNull {
            val offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(it))
            offlinePlayer.name
        }
    }

    /**
     * 删除工作空间
     */
    fun delete() {
        val user = ZephyrionAPI.getUserData(owner)
        val currentWorkspaceUsed = user.workspaceUsed
        val newWorkspaceUsed = currentWorkspaceUsed - 1

        if (newWorkspaceUsed < 0) {
            return
        }

        val quotasTable = DatabaseConfig.quotasTable

        val affected = quotasTable.update(dataSource) {
            set("workspace_used", newWorkspaceUsed)
            where {
                "player" eq owner
                and { "workspace_used" eq currentWorkspaceUsed }
            }
        }

        if (affected == 0) {
            return
        }

        table.delete(dataSource) {
            where { "id" eq id }
        }
        
        WorkspaceCache.invalidate(id)
        VaultCache.invalidateWorkspaceVaults(id)
        QuotaCache.invalidate(owner)
    }

    /**
     * 检查是否为成员
     */
    fun isMember(uuid: String): Boolean {
        return members.contains(uuid)
    }
}
