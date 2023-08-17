package com.faithl.zephyrion.core.models

import com.faithl.zephyrion.Zephyrion
import com.faithl.zephyrion.api.ZephyrionAPI
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object Workspaces : IntIdTable() {
    val name = varchar("name", 255)
    val desc = varchar("description", 255).nullable()
    val type = enumerationByName("type", 255, Type::class)
    val owner = varchar("owner", 255)
    val members = varchar("members", 255)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    enum class Type {
        PUBLIC, PRIVATE,
        INDEPENDENT
    }

    init {
        if (Zephyrion.settings.getBoolean("workspace.independent")) {
            val independentWorkspace = Workspace.find { name eq "Independent" }.firstOrNull()
            if (independentWorkspace == null) {
                transaction {
                    Workspace.new {
                        name = "Independent"
                        desc = "Independent workspace"
                        owner = "Server"
                        members = ""
                        type = Type.INDEPENDENT
                        createdAt = System.currentTimeMillis()
                        updatedAt = System.currentTimeMillis()
                    }
                }
            }
        }
    }
}

class Workspace(id: EntityID<Int>) : IntEntity(id) {

    companion object : IntEntityClass<Workspace>(Workspaces) {

        fun getIndependentWorkspace(): Workspace? {
            return transaction {
                find { Workspaces.name eq "independent" }.firstOrNull()
            }
        }

        fun getJoinedWorkspaces(player: Player): List<Workspace> {
            return transaction {
                find { Workspaces.members like "%${player.uniqueId}%" }.toList()
            }
        }

        fun getWorkspace(player: String, name: String): Workspace? {
            return transaction {
                find { (Workspaces.members like "%${player}%") and (Workspaces.name eq name) }.firstOrNull()
            }
        }

        fun addMember(workspace: Workspace, player: Player): Boolean {
            return transaction {
                if (workspace.members.contains(player.uniqueId.toString())) {
                    false
                } else {
                    workspace.members = workspace.members + "," + player.uniqueId.toString()
                    workspace.updatedAt = System.currentTimeMillis()
                    true
                }
            }
        }

        fun removeMember(workspace: Workspace, player: OfflinePlayer): Boolean {
            return transaction {
                if (workspace.owner == player.uniqueId.toString()) {
                    false
                } else if (workspace.members.contains(player.uniqueId.toString())) {
                    workspace.members = workspace.members.replace("," + player.uniqueId.toString(), "")
                    workspace.updatedAt = System.currentTimeMillis()
                    true
                } else {
                    false
                }
            }
        }

    }

    var name by Workspaces.name
    var desc by Workspaces.desc
    var owner by Workspaces.owner
    var type by Workspaces.type
    var members by Workspaces.members
    var createdAt by Workspaces.createdAt
    var updatedAt by Workspaces.updatedAt

    fun rename(newName: String): ZephyrionAPI.Result {
        val result = ZephyrionAPI.validateWorkspaceName(newName, getOwner().uniqueId.toString())
        if (!result.success) {
            return result
        }
        return transaction {
            name = newName
            updatedAt = System.currentTimeMillis()
            ZephyrionAPI.Result(true)
        }
    }

    fun getCreatedAt(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(createdAt))
    }

    fun getUpdatedAt(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(updatedAt))
    }

    fun getOwner(): OfflinePlayer {
        return Bukkit.getOfflinePlayer(UUID.fromString(owner))
    }

    fun getMembers(): List<OfflinePlayer> {
        return members.split(",").map {
            Bukkit.getOfflinePlayer(UUID.fromString(it))
        }
    }

    fun getMembersName(): List<String> {
        return members.split(",").mapNotNull {
            val offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(it))
            offlinePlayer.name
        }
    }

    override fun delete() {
        transaction {
            super.delete()
        }
        ZephyrionAPI.getUserData(owner).workspaceUsed -= 1
    }

    fun isMember(toString: String): Boolean {
        return members.contains(toString)
    }
}