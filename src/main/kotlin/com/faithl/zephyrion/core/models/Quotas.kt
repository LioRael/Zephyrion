package com.faithl.zephyrion.core.models

import com.faithl.zephyrion.Zephyrion
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Quotas : IntIdTable() {

    val player = varchar("player", 36)
    val workspaceQuotas = integer("workspace_quotas")
    val workspaceUsed = integer("workspace_used")
    val sizeQuotas = integer("size_quotas")
    val sizeUsed = integer("size_used")
    val unlimitedQuotas = integer("unlimited_quotas")
    val unlimitedUsed = integer("unlimited_used")

}

class Quota(id: EntityID<Int>) : IntEntity(id) {

    companion object : IntEntityClass<Quota>(Quotas) {
        fun getUser(playerUniqueId: String): Quota {
            return Quota.find { Quotas.player eq playerUniqueId }.firstOrNull() ?: Quota.new {
                this.player = playerUniqueId
                this.workspaceQuotas = Zephyrion.settings.getInt("user.default-quotas.workspace")
                this.workspaceUsed = 0
                this.sizeQuotas = Zephyrion.settings.getInt("user.default-quotas.size")
                this.sizeUsed = 0
                this.unlimitedQuotas = Zephyrion.settings.getInt("user.default-quotas.unlimited")
                this.unlimitedUsed = 0
            }
        }
    }

    var player by Quotas.player
    var workspaceQuotas by Quotas.workspaceQuotas
    var workspaceUsed by Quotas.workspaceUsed
    var sizeQuotas by Quotas.sizeQuotas
    var sizeUsed by Quotas.sizeUsed
    var unlimitedQuotas by Quotas.unlimitedQuotas
    var unlimitedUsed by Quotas.unlimitedUsed

}