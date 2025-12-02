package com.faithl.zephyrion.core.models

import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.core.cache.AutoPickupCache
import com.faithl.zephyrion.core.cache.SettingCache
import com.faithl.zephyrion.core.cache.ItemCache
import com.faithl.zephyrion.core.cache.QuotaCache
import com.faithl.zephyrion.core.cache.VaultCache
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
) : java.io.Serializable {

    companion object {
        private const val serialVersionUID = 1L

        private val table: Table<*, *>
            get() = DatabaseConfig.vaultsTable

        private val dataSource
            get() = DatabaseConfig.dataSource

        /**
         * 根据ID获取保险库（使用缓存）
         */
        fun findById(id: Int): Vault? {
            return VaultCache.getById(id)
        }

        /**
         * 获取指定工作空间和名称的保险库
         */
        fun getVault(workspace: Workspace, name: String): Vault? {
            return VaultCache.getByWorkspace(workspace).find { it.name == name }
        }

        /**
         * 获取指定工作空间的所有保险库（使用缓存）
         */
        fun getVaults(workspace: Workspace): List<Vault> {
            return VaultCache.getByWorkspace(workspace)
        }

        /**
         * 创建保险库（数据库操作）
         */
        fun create(workspace: Workspace, name: String, desc: String?): Boolean {
            table.insert(dataSource, "name", "description", "workspace_id", "size", "created_at", "updated_at") {
                value(
                    name,
                    desc,
                    workspace.id,
                    0,
                    System.currentTimeMillis(),
                    System.currentTimeMillis()
                )
            }

            // 更新缓存
            VaultCache.invalidateWorkspaceVaults(workspace.id)

            return true
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

        user.sizeUsed = newSizeUsed
        QuotaCache.update(workspace.owner, user)
        VaultCache.update(this)
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

        user.sizeUsed = newSizeUsed
        QuotaCache.update(workspace.owner, user)
        VaultCache.update(this)
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

        VaultCache.update(this)
        return ZephyrionAPI.Result(true)
    }

    /**
     * 更新保险库描述
     */
    fun updateDesc(newDesc: String?) {
        desc = newDesc
        updatedAt = System.currentTimeMillis()

        table.update(dataSource) {
            set("description", desc)
            set("updated_at", updatedAt)
            where { "id" eq id }
        }

        VaultCache.update(this)
    }

    /**
     * 删除保险库及其所有数据
     */
    fun delete(): Boolean {
        val user = ZephyrionAPI.getUserData(workspace.owner)
        val currentSizeUsed = user.sizeUsed
        val newSizeUsed = currentSizeUsed - size

        if (newSizeUsed < 0) {
            return false
        }

        val quotasTable = DatabaseConfig.quotasTable
        val itemsTable = DatabaseConfig.itemsTable
        val settingsTable = DatabaseConfig.settingsTable
        val autoPickupsTable = DatabaseConfig.autoPickupsTable

        // 删除物品
        itemsTable.delete(dataSource) {
            where { "vault_id" eq id }
        }

        // 删除设置
        settingsTable.delete(dataSource) {
            where { "vault_id" eq id }
        }

        // 删除自动拾取规则
        autoPickupsTable.delete(dataSource) {
            where { "vault_id" eq id }
        }

        // 删除保险库
        table.delete(dataSource) {
            where { "id" eq id }
        }

        // 更新配额
        quotasTable.update(dataSource) {
            set("size_used", newSizeUsed)
            where { "player" eq workspace.owner }
        }

        // 更新缓存
        user.sizeUsed = newSizeUsed
        QuotaCache.update(workspace.owner, user)
        VaultCache.invalidate(id)
        AutoPickupCache.invalidateByVault(id)
        SettingCache.invalidateByVault(id)
        ItemCache.invalidateAll(id)

        return true
    }
}
