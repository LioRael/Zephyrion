package com.faithl.zephyrion.core.models

import com.faithl.zephyrion.api.ZephyrionAPI
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.math.ceil

object Vaults : IntIdTable() {
    val name = varchar("name", 255)
    val desc = varchar("description", 255).nullable()
    val workspace = reference("workspace", Workspaces)
    val size = integer("size")
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
}

class Vault(id: EntityID<Int>) : IntEntity(id) {

    companion object : IntEntityClass<Vault>(Vaults) {

        fun getVault(workspace: Workspace, name: String): Vault? {
            return transaction { find { (Vaults.name eq name) and (Vaults.workspace eq workspace.id) }.firstOrNull() }
        }

        fun getVaults(workspace: Workspace): List<Vault> {
            return transaction { find { Vaults.workspace eq workspace.id }.toList() }
        }

    }

    fun addSize(add: Int): Boolean {
        val user = ZephyrionAPI.getUserData(workspace.owner)
        return transaction {
            if (user.sizeUsed + add > user.sizeQuotas) {
                false
            } else {
                user.sizeUsed += add
                size += add
                updatedAt = System.currentTimeMillis()
                true
            }
        }
    }

    fun removeSize(remove: Int): Boolean {
        val user = ZephyrionAPI.getUserData(workspace.owner)
        return transaction {
            if (user.sizeUsed - remove < 0) {
                false
            } else {
                user.sizeUsed -= remove
                size -= remove
                updatedAt = System.currentTimeMillis()
                true
            }
        }
    }

    fun getCreatedAt(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(createdAt))
    }

    fun getUpdatedAt(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(updatedAt))
    }

    fun getMaxPage(): Int {
        val maxPage = ceil(size.toDouble() / 36).toInt()
        return if (maxPage == 0) {
            1
        } else {
            maxPage
        }
    }

    fun rename(newName: String): ZephyrionAPI.Result {
        val result = ZephyrionAPI.validateVaultName(newName, workspace)
        if (!result.success) {
            return result
        }
        return transaction {
            name = newName
            updatedAt = System.currentTimeMillis()
            ZephyrionAPI.Result(true)
        }
    }

    var name by Vaults.name
    var desc by Vaults.desc
    var workspace by Workspace referencedOn Vaults.workspace
    var size by Vaults.size
    var createdAt by Vaults.createdAt
    var updatedAt by Vaults.updatedAt
}