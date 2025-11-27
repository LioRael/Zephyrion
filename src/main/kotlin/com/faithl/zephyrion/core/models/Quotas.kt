package com.faithl.zephyrion.core.models

import com.faithl.zephyrion.Zephyrion
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
) {

    companion object {

        private val table: Table<*, *>
            get() = DatabaseConfig.quotasTable

        private val dataSource
            get() = DatabaseConfig.dataSource

        /**
         * 获取用户配额数据
         * 如果不存在则创建,并根据权限组设置配额
         */
        fun getUser(playerUniqueId: String): Quota {
            // 先查询数据库
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

            // 如果存在,更新配额(根据权限组)
            if (existing != null) {
                updateQuotasByPermission(existing, playerUniqueId)
                return existing
            }

            // 不存在则创建
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

            return getUser(playerUniqueId)
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
            return true
        }

        /**
         * 增加玩家的工作空间配额
         */
        fun addWorkspaceQuota(playerUniqueId: String, amount: Int): Boolean {
            if (amount <= 0) return false

            val userData = getUser(playerUniqueId)
            val newQuota = userData.workspaceQuotas + amount

            table.update(dataSource) {
                set("workspace_quotas", newQuota)
                where { "player" eq playerUniqueId }
            }
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

            table.update(dataSource) {
                set("workspace_quotas", userData.workspaceQuotas - amount)
                where { "player" eq playerUniqueId }
            }
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
            return true
        }

        /**
         * 增加玩家的容量配额
         */
        fun addSizeQuota(playerUniqueId: String, amount: Int): Boolean {
            if (amount <= 0) return false

            val userData = getUser(playerUniqueId)
            val newQuota = userData.sizeQuotas + amount

            table.update(dataSource) {
                set("size_quotas", newQuota)
                where { "player" eq playerUniqueId }
            }
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

            table.update(dataSource) {
                set("size_quotas", userData.sizeQuotas - amount)
                where { "player" eq playerUniqueId }
            }
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
            return true
        }
    }
}
