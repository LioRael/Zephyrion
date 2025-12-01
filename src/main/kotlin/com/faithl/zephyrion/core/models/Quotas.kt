package com.faithl.zephyrion.core.models

import com.faithl.zephyrion.Zephyrion
import com.faithl.zephyrion.core.cache.QuotaCache
import com.faithl.zephyrion.storage.DatabaseConfig
import org.bukkit.Bukkit
import taboolib.module.database.HostSQL
import taboolib.module.database.HostSQLite
import taboolib.module.database.Table
import java.util.*

/**
 * Quota数据类
 */
data class Quota(
    var id: Int,
    var player: String,
    var workspaceQuotas: Int,
    var workspaceUsed: Int,
    var sizeQuotas: Int,
    var sizeUsed: Int,
    var unlimited: Boolean
) : java.io.Serializable {

    companion object {
        private const val serialVersionUID = 1L

        private val table: Table<*, *>
            get() = DatabaseConfig.quotasTable

        private val dataSource
            get() = DatabaseConfig.dataSource

        /**
         * 获取用户配额数据（使用缓存）
         * 如果不存在则创建,并根据权限组设置配额
         */
        fun getUser(playerUniqueId: String): Quota {
            val quota = QuotaCache.get(playerUniqueId)
            updateQuotasByPermission(quota, playerUniqueId)
            return quota
        }

        /**
         * 根据玩家权限组更新配额
         */
        private fun updateQuotasByPermission(quota: Quota, playerUniqueId: String) {
            val player = Bukkit.getPlayer(UUID.fromString(playerUniqueId)) ?: return

            var matchedGroup: String? = null
            Zephyrion.settings.getConfigurationSection("user")?.getKeys(false)?.forEach { groupKey ->
                if (groupKey == "default-quotas") return@forEach

                val permission = Zephyrion.permissions.getString("user.quotas.$groupKey")
                if (permission != null && player.hasPermission(permission)) {
                    matchedGroup = groupKey
                }
            }

            if (matchedGroup != null) {
                quota.workspaceQuotas = Zephyrion.settings.getInt("user.$matchedGroup.workspace")
                quota.sizeQuotas = Zephyrion.settings.getInt("user.$matchedGroup.size")
                quota.unlimited = Zephyrion.settings.getBoolean("user.$matchedGroup.unlimited")
            } else {
                quota.workspaceQuotas = Zephyrion.settings.getInt("user.default-quotas.workspace")
                quota.sizeQuotas = Zephyrion.settings.getInt("user.default-quotas.size")
                quota.unlimited = Zephyrion.settings.getBoolean("user.default-quotas.unlimited")
            }

            // 更新数据库
            table.update(dataSource) {
                set("workspace_quotas", quota.workspaceQuotas)
                set("size_quotas", quota.sizeQuotas)
                set("unlimited", if (DatabaseConfig.host is HostSQLite) (if (quota.unlimited) 1 else 0) else quota.unlimited)
                where { "player" eq playerUniqueId }
            }
        }

        /**
         * 设置玩家的工作空间配额
         */
        fun setWorkspaceQuota(playerUniqueId: String, quota: Int): Boolean {
            if (quota < 0) return false

            val userData = getUser(playerUniqueId)
            userData.workspaceQuotas = quota

            table.update(dataSource) {
                set("workspace_quotas", quota)
                where { "player" eq playerUniqueId }
            }
            QuotaCache.update(playerUniqueId, userData)
            return true
        }

        /**
         * 增加玩家的工作空间配额
         */
        fun addWorkspaceQuota(playerUniqueId: String, amount: Int): Boolean {
            if (amount <= 0) return false

            val userData = getUser(playerUniqueId)
            val newQuota = userData.workspaceQuotas + amount
            userData.workspaceQuotas = newQuota

            table.update(dataSource) {
                set("workspace_quotas", newQuota)
                where { "player" eq playerUniqueId }
            }
            QuotaCache.update(playerUniqueId, userData)
            return true
        }

        /**
         * 减少玩家的工作空间配额
         */
        fun removeWorkspaceQuota(playerUniqueId: String, amount: Int): Boolean {
            if (amount <= 0) return false

            val userData = getUser(playerUniqueId)
            if (userData.workspaceQuotas - amount < userData.workspaceUsed) {
                return false
            }
            userData.workspaceQuotas = userData.workspaceQuotas - amount

            table.update(dataSource) {
                set("workspace_quotas", userData.workspaceQuotas)
                where { "player" eq playerUniqueId }
            }
            QuotaCache.update(playerUniqueId, userData)
            return true
        }

        /**
         * 设置玩家的容量配额
         */
        fun setSizeQuota(playerUniqueId: String, quota: Int): Boolean {
            if (quota < 0) return false

            val userData = getUser(playerUniqueId)
            userData.sizeQuotas = quota

            table.update(dataSource) {
                set("size_quotas", quota)
                where { "player" eq playerUniqueId }
            }
            QuotaCache.update(playerUniqueId, userData)
            return true
        }

        /**
         * 增加玩家的容量配额
         */
        fun addSizeQuota(playerUniqueId: String, amount: Int): Boolean {
            if (amount <= 0) return false

            val userData = getUser(playerUniqueId)
            val newQuota = userData.sizeQuotas + amount
            userData.sizeQuotas = newQuota

            table.update(dataSource) {
                set("size_quotas", newQuota)
                where { "player" eq playerUniqueId }
            }
            QuotaCache.update(playerUniqueId, userData)
            return true
        }

        /**
         * 减少玩家的容量配额
         */
        fun removeSizeQuota(playerUniqueId: String, amount: Int): Boolean {
            if (amount <= 0) return false

            val userData = getUser(playerUniqueId)
            if (userData.sizeQuotas - amount < userData.sizeUsed) {
                return false
            }
            userData.sizeQuotas = userData.sizeQuotas - amount

            table.update(dataSource) {
                set("size_quotas", userData.sizeQuotas)
                where { "player" eq playerUniqueId }
            }
            QuotaCache.update(playerUniqueId, userData)
            return true
        }

        /**
         * 设置玩家的无限配额状态
         */
        fun setUnlimited(playerUniqueId: String, unlimited: Boolean): Boolean {
            val userData = getUser(playerUniqueId)
            userData.unlimited = unlimited

            table.update(dataSource) {
                set("unlimited", if (DatabaseConfig.host is HostSQLite) (if (unlimited) 1 else 0) else unlimited)
                where { "player" eq playerUniqueId }
            }
            QuotaCache.update(playerUniqueId, userData)
            return true
        }

        /**
         * 重置玩家的配额为默认值
         */
        fun resetQuota(playerUniqueId: String): Boolean {
            val defaultWorkspace = Zephyrion.settings.getInt("user.default-quotas.workspace")
            val defaultSize = Zephyrion.settings.getInt("user.default-quotas.size")
            val defaultUnlimited = Zephyrion.settings.getBoolean("user.default-quotas.unlimited")

            table.update(dataSource) {
                set("workspace_quotas", defaultWorkspace)
                set("size_quotas", defaultSize)
                set("unlimited", if (DatabaseConfig.host is HostSQLite) (if (defaultUnlimited) 1 else 0) else defaultUnlimited)
                where { "player" eq playerUniqueId }
            }
            QuotaCache.invalidate(playerUniqueId)
            return true
        }
    }
}
