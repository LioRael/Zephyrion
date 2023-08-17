package com.faithl.zephyrion.api

import com.faithl.zephyrion.Zephyrion
import com.faithl.zephyrion.core.models.*
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.transactions.transaction

object ZephyrionAPI {

    class Result(val success: Boolean, val reason: String? = null)

    fun getUserData(playerUniqueId: String): Quota {
        return transaction {
            Quota.getUser(playerUniqueId)
        }
    }

    fun addSize(vault: Vault, size: Int): Boolean {
        return vault.addSize(size)
    }

    fun removeSize(vault: Vault, size: Int): Boolean {
        return vault.removeSize(size)
    }

    fun getJoinedWorkspaces(player: Player): List<Workspace> {
        return Workspace.getJoinedWorkspaces(player)
    }

    fun getIndependentWorkspace(): Workspace? {
        if (Zephyrion.settings.getBoolean("workspace.independent")) {
            return Workspace.getIndependentWorkspace()
        }
        return null
    }

    fun getWorkspace(player: String, name: String): Workspace? {
        return Workspace.getWorkspace(player, name)
    }

    fun addMember(workspace: Workspace, player: Player): Boolean {
        return Workspace.addMember(workspace, player)
    }

    fun removeMember(workspace: Workspace, player: OfflinePlayer): Boolean {
        return Workspace.removeMember(workspace, player)
    }

    fun validateWorkspaceName(name: String?, owner: String): Result {
        if (name == null) {
            return Result(false, "workspace_name_invalid")
        } else if (name.contains(" ")) {
            return Result(false, "workspace_name_invalid")
        } else if (Zephyrion.settings.getBoolean("workspace.name.allow-color") && (name.contains("&") || name.contains("§"))) {
            return Result(false, "workspace_name_color")
        } else if (Zephyrion.settings.getStringList("workspace.name.blacklist").contains(name)) {
            return Result(false, "workspace_name_blacklist")
        } else if (name.length > Zephyrion.settings.getInt("workspace.name.max-length") || name.length < Zephyrion.settings.getInt(
                "workspace.name.min-length"
            )
        ) {
            return Result(false, "workspace_name_length")
        }
        val workspace = getWorkspace(owner, name)
        return if (workspace != null) {
            Result(false, "workspace_already_exists")
        } else {
            Result(true)
        }
    }

    // 创建工作空间
    fun createWorkspace(owner: String, name: String?, type: Workspaces.Type?, desc: String?): Result {
        val ownerData = getUserData(owner)
        if (ownerData.workspaceUsed + 1 > ownerData.workspaceQuotas) {
            return Result(false, "workspace_quota_exceeded")
        }
        val result = validateWorkspaceName(name, owner)
        if (!result.success) {
            return result
        }
        if (type == null) {
            return Result(false, "workspace_type_invalid")
        }
        transaction {
            Workspace.new {
                this.name = name!!
                this.desc = desc
                this.type = type
                this.owner = owner
                this.members = owner
                this.createdAt = System.currentTimeMillis()
                this.updatedAt = System.currentTimeMillis()
            }
            getUserData(owner).workspaceUsed += 1
        }
        return Result(true)
    }

    fun getVaults(workspace: Workspace): List<Vault> {
        return Vault.getVaults(workspace)
    }

    fun getVault(workspace: Workspace, name: String): Vault? {
        return Vault.getVault(workspace, name)
    }

    fun validateVaultName(name: String?, workspace: Workspace): Result {
        if (name == null) {
            return Result(false, "vault_name_invalid")
        } else if (name.contains(" ")) {
            return Result(false, "vault_name_invalid")
        } else if (Zephyrion.settings.getBoolean("vault.name.allow-color") && (name.contains("&") || name.contains("§"))) {
            return Result(false, "vault_name_color")
        } else if (Zephyrion.settings.getStringList("vault.name.blacklist").contains(name)) {
            return Result(false, "vault_name_blacklist")
        } else if (name.length > Zephyrion.settings.getInt("vault.name.max-length") || name.length < Zephyrion.settings.getInt(
                "vault.name.min-length"
            )
        ) {
            return Result(false, "vault_name_length")
        }
        val vault = getVault(workspace, name)
        return if (vault != null) {
            Result(false, "vault_already_exists")
        } else {
            Result(true)
        }
    }

    fun createVault(workspace: Workspace, name: String?, desc: String? = null): Result {
        val result = validateVaultName(name, workspace)
        if (!result.success) {
            return result
        }
        transaction {
            Vault.new {
                this.name = name!!
                this.desc = desc
                this.workspace = workspace
                this.size = 0
                this.createdAt = System.currentTimeMillis()
                this.updatedAt = System.currentTimeMillis()
            }
        }
        return Result(true)
    }

    fun searchItemsByName(vault: Vault, name: String): List<Item> {
        return Item.searchItemsByName(vault, name)
    }

    fun searchItemsByLore(vault: Vault, lore: String): List<Item> {
        return Item.searchItemsByLore(vault, lore)
    }

    fun getItems(vault: Vault, page: Int, player: Player): List<Item> {
        return Item.getItems(vault, page, player)
    }

    fun setItem(vault: Vault, page: Int, slot: Int, itemStack: ItemStack, player: Player? = null) {
        return Item.setItem(vault, page, slot, itemStack, player)
    }

    fun removeItem(vault: Vault, page: Int, slot: Int, player: Player? = null) {
        return Item.removeItem(vault, page, slot, player)
    }

    fun newSetting(vault: Vault, setting: String, value: String) {
        return transaction {
            Setting.new {
                this.setting = Settings.SettingType.valueOf(setting)
                this.value = value
                this.vault = vault
                this.createdAt = System.currentTimeMillis()
                this.updatedAt = System.currentTimeMillis()
            }
        }
    }

    fun isPluginAdmin(opener: Player): Boolean {
        return opener.hasPermission(Zephyrion.settings.getString("permissions.admin")!!)
    }
}