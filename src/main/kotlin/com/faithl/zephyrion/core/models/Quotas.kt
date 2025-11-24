package com.faithl.zephyrion.core.models

import com.faithl.zephyrion.Zephyrion
import com.faithl.zephyrion.core.models.Quotas.player
import org.bukkit.Bukkit
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object Quotas : IntIdTable() {

    val player = varchar("player", 36)
    val workspaceQuotas = integer("workspace_quotas")
    val workspaceUsed = integer("workspace_used")
    val sizeQuotas = integer("size_quotas")
    val sizeUsed = integer("size_used")
    val unlimited = bool("unlimited")

}

class Quota(id: EntityID<Int>) : IntEntity(id) {

    companion object : IntEntityClass<Quota>(Quotas) {
        fun getUser(playerUniqueId: String): Quota {
            val quota = find { player eq playerUniqueId }.firstOrNull() ?: new {
                this.player = playerUniqueId
                this.workspaceQuotas = Zephyrion.settings.getInt("user.default-quotas.workspace")
                this.workspaceUsed = 0
                this.sizeQuotas = Zephyrion.settings.getInt("user.default-quotas.size")
                this.sizeUsed = 0
                this.unlimited = Zephyrion.settings.getBoolean("user.default-quotas.unlimited")
            }

            val player = Bukkit.getPlayer(UUID.fromString(playerUniqueId)) ?: return quota

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

            return quota
        }

        /**
         * 设置玩家的工作空间配额
         * @param playerUniqueId 玩家 UUID
         * @param quota 新配额值
         * @return 是否成功
         */
        fun setWorkspaceQuota(playerUniqueId: String, quota: Int): Boolean {
            if (quota < 0) return false
            return transaction {
                val userData = getUser(playerUniqueId)
                userData.workspaceQuotas = quota
                true
            }
        }

        /**
         * 增加玩家的工作空间配额
         * @param playerUniqueId 玩家 UUID
         * @param amount 增加的数量
         * @return 是否成功
         */
        fun addWorkspaceQuota(playerUniqueId: String, amount: Int): Boolean {
            if (amount <= 0) return false
            return transaction {
                val userData = getUser(playerUniqueId)
                userData.workspaceQuotas += amount
                true
            }
        }

        /**
         * 减少玩家的工作空间配额
         * @param playerUniqueId 玩家 UUID
         * @param amount 减少的数量
         * @return 是否成功
         */
        fun removeWorkspaceQuota(playerUniqueId: String, amount: Int): Boolean {
            if (amount <= 0) return false
            return transaction {
                val userData = getUser(playerUniqueId)
                if (userData.workspaceQuotas - amount < userData.workspaceUsed) {
                    false
                } else {
                    userData.workspaceQuotas -= amount
                    true
                }
            }
        }

        /**
         * 设置玩家的容量配额
         * @param playerUniqueId 玩家 UUID
         * @param quota 新配额值
         * @return 是否成功
         */
        fun setSizeQuota(playerUniqueId: String, quota: Int): Boolean {
            if (quota < 0) return false
            return transaction {
                val userData = getUser(playerUniqueId)
                userData.sizeQuotas = quota
                true
            }
        }

        /**
         * 增加玩家的容量配额
         * @param playerUniqueId 玩家 UUID
         * @param amount 增加的数量
         * @return 是否成功
         */
        fun addSizeQuota(playerUniqueId: String, amount: Int): Boolean {
            if (amount <= 0) return false
            return transaction {
                val userData = getUser(playerUniqueId)
                userData.sizeQuotas += amount
                true
            }
        }

        /**
         * 减少玩家的容量配额
         * @param playerUniqueId 玩家 UUID
         * @param amount 减少的数量
         * @return 是否成功
         */
        fun removeSizeQuota(playerUniqueId: String, amount: Int): Boolean {
            if (amount <= 0) return false
            return transaction {
                val userData = getUser(playerUniqueId)
                if (userData.sizeQuotas - amount < userData.sizeUsed) {
                    false
                } else {
                    userData.sizeQuotas -= amount
                    true
                }
            }
        }

        /**
         * 设置玩家的无限配额状态
         * @param playerUniqueId 玩家 UUID
         * @param unlimited 是否启用无限配额
         * @return 是否成功
         */
        fun setUnlimited(playerUniqueId: String, unlimited: Boolean): Boolean {
            return transaction {
                val userData = getUser(playerUniqueId)
                userData.unlimited = unlimited
                true
            }
        }

        /**
         * 重置玩家的配额为默认值
         * @param playerUniqueId 玩家 UUID
         * @return 是否成功
         */
        fun resetQuota(playerUniqueId: String): Boolean {
            return transaction {
                val userData = getUser(playerUniqueId)
                userData.workspaceQuotas = Zephyrion.settings.getInt("user.default-quotas.workspace")
                userData.sizeQuotas = Zephyrion.settings.getInt("user.default-quotas.size")
                userData.unlimited = Zephyrion.settings.getBoolean("user.default-quotas.unlimited")
                true
            }
        }
    }

    var player by Quotas.player
    var workspaceQuotas by Quotas.workspaceQuotas
    var workspaceUsed by Quotas.workspaceUsed
    var sizeQuotas by Quotas.sizeQuotas
    var sizeUsed by Quotas.sizeUsed
    var unlimited by Quotas.unlimited

}