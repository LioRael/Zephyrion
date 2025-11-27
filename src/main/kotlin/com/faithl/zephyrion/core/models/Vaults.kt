package com.faithl.zephyrion.core.models

import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.storage.DatabaseConfig
import taboolib.module.database.Table
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

/**
 * Vault数据类
 */
data class Vault(
    var id: Int,
    var name: String,
    var desc: String?,
    var workspaceId: Int,
    var size: Int,
    var createdAt: Long,
    var updatedAt: Long
) {

    companion object {

        private val table: Table<*, *>
            get() = DatabaseConfig.vaultsTable

        private val dataSource
            get() = DatabaseConfig.dataSource

        /**
         * 根据ID获取保险库
         */
        fun findById(id: Int): Vault? {
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

        /**
         * 获取指定工作空间和名称的保险库
         */
        fun getVault(workspace: Workspace, name: String): Vault? {
            return table.select(dataSource) {
                where {
                    "name" eq name
                    and { "workspace_id" eq workspace.id }
                }
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

        /**
         * 获取指定工作空间的所有保险库
         */
        fun getVaults(workspace: Workspace): List<Vault> {
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

    /**
     * 获取关联的工作空间
     */
    val workspace: Workspace
        get() = Workspace.findById(workspaceId)
            ?: error("Workspace not found for vault $id")

    /**
     * 增加容量
     */
    fun addSize(add: Int): Boolean {
        val user = ZephyrionAPI.getUserData(workspace.owner)
        val currentSizeUsed = user.sizeUsed
        val newSizeUsed = currentSizeUsed + add

        if (!user.unlimited && newSizeUsed > user.sizeQuotas) {
            return false
        }

        val quotasTable = DatabaseConfig.quotasTable

        val affected = quotasTable.update(dataSource) {
            set("size_used", newSizeUsed)
            where {
                "player" eq workspace.owner
                and { "size_used" eq currentSizeUsed }
            }
        }

        if (affected == 0) {
            return false
        }

        size += add
        updatedAt = System.currentTimeMillis()

        table.update(dataSource) {
            set("size", size)
            set("updated_at", updatedAt)
            where { "id" eq id }
        }

        return true
    }

    /**
     * 减少容量
     */
    fun removeSize(remove: Int): Boolean {
        val user = ZephyrionAPI.getUserData(workspace.owner)
        val currentSizeUsed = user.sizeUsed
        val newSizeUsed = currentSizeUsed - remove

        if (newSizeUsed < 0) {
            return false
        }

        val quotasTable = DatabaseConfig.quotasTable

        val affected = quotasTable.update(dataSource) {
            set("size_used", newSizeUsed)
            where {
                "player" eq workspace.owner
                and { "size_used" eq currentSizeUsed }
            }
        }

        if (affected == 0) {
            return false
        }

        size -= remove
        updatedAt = System.currentTimeMillis()

        table.update(dataSource) {
            set("size", size)
            set("updated_at", updatedAt)
            where { "id" eq id }
        }

        return true
    }

    /**
     * 获取创建时间(格式化)
     */
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
     * 获取最大页数
     */
    fun getMaxPage(): Int {
        val maxPage = ceil(size.toDouble() / 36).toInt()
        return if (maxPage == 0) 1 else maxPage
    }

    /**
     * 重命名保险库
     */
    fun rename(newName: String): ZephyrionAPI.Result {
        val result = ZephyrionAPI.validateVaultName(newName, workspace)
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

        return ZephyrionAPI.Result(true)
    }
}
