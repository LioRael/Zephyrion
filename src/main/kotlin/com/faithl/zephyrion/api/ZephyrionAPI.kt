package com.faithl.zephyrion.api

import com.faithl.zephyrion.Zephyrion
import com.faithl.zephyrion.core.models.*
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object ZephyrionAPI {

    class Result(val success: Boolean, val reason: String? = null)

    fun getUserData(playerUniqueId: String): Quota {
        return Quota.getUser(playerUniqueId)
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
    fun createWorkspace(owner: String, name: String?, type: WorkspaceType?, desc: String?): Result {
        val result = validateWorkspaceName(name, owner)
        if (!result.success) {
            return result
        }
        if (type == null) {
            return Result(false, "workspace_type_invalid")
        }

        val ownerData = getUserData(owner)
        if (ownerData.workspaceUsed + 1 > ownerData.workspaceQuotas) {
            return Result(false, "workspace_quota_exceeded")
        }

        val success = Workspace.create(owner, name!!, type, desc)
        return if (success) {
            Result(true)
        } else {
            Result(false, "workspace_quota_exceeded")
        }
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

        Vault.create(workspace, name!!, desc)
        return Result(true)
    }

    fun searchItemsByName(vault: Vault, name: String): List<Item> {
        return Item.searchItemsByName(vault, name)
    }

    fun searchItemsByLore(vault: Vault, lore: String): List<Item> {
        return Item.searchItemsByLore(vault, lore)
    }

    fun searchItems(vault: Vault, params: Map<String, String>, player: Player? = null): List<Item> {
        return Item.searchItems(vault, params, player)
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

    fun newSetting(vault: Vault, setting: String, value: String, owner: String) {
        Setting.create(vault, setting, value, owner)
    }

    /**
     * 更新保险库描述
     */
    fun updateVaultDesc(vault: Vault, desc: String?) {
        vault.updateDesc(desc)
    }

    /**
     * 删除保险库
     */
    fun deleteVault(vault: Vault): Boolean {
        return vault.delete()
    }

    /**
     * 更新工作空间描述
     */
    fun updateWorkspaceDesc(workspace: Workspace, desc: String?) {
        workspace.updateDesc(desc)
    }

    /**
     * 删除工作空间
     */
    fun deleteWorkspace(workspace: Workspace) {
        workspace.delete()
    }

    fun isPluginAdmin(opener: Player): Boolean {
        return opener.hasPermission(Zephyrion.permissions.getString("admin")!!)
    }

    // ==================== 配额管理 API ====================

    /**
     * 设置玩家的工作空间配额
     */
    fun setWorkspaceQuota(playerUniqueId: String, quota: Int): Boolean {
        return Quota.setWorkspaceQuota(playerUniqueId, quota)
    }

    /**
     * 增加玩家的工作空间配额
     */
    fun addWorkspaceQuota(playerUniqueId: String, amount: Int): Boolean {
        return Quota.addWorkspaceQuota(playerUniqueId, amount)
    }

    /**
     * 减少玩家的工作空间配额
     */
    fun removeWorkspaceQuota(playerUniqueId: String, amount: Int): Boolean {
        return Quota.removeWorkspaceQuota(playerUniqueId, amount)
    }

    /**
     * 设置玩家的容量配额
     */
    fun setSizeQuota(playerUniqueId: String, quota: Int): Boolean {
        return Quota.setSizeQuota(playerUniqueId, quota)
    }

    /**
     * 增加玩家的容量配额
     */
    fun addSizeQuota(playerUniqueId: String, amount: Int): Boolean {
        return Quota.addSizeQuota(playerUniqueId, amount)
    }

    /**
     * 减少玩家的容量配额
     */
    fun removeSizeQuota(playerUniqueId: String, amount: Int): Boolean {
        return Quota.removeSizeQuota(playerUniqueId, amount)
    }

    /**
     * 设置玩家的无限配额状态
     */
    fun setUnlimited(playerUniqueId: String, unlimited: Boolean): Boolean {
        return Quota.setUnlimited(playerUniqueId, unlimited)
    }

    /**
     * 重置玩家的配额为默认值
     */
    fun resetQuota(playerUniqueId: String): Boolean {
        return Quota.resetQuota(playerUniqueId)
    }

    // ==================== 自动拾取管理 API ====================

    /**
     * 获取保险库的所有自动拾取规则（按 owner 过滤）
     */
    fun getAutoPickups(vault: Vault, owner: String): List<AutoPickup> {
        return AutoPickup.getAutoPickups(vault, owner)
    }

    /**
     * 获取保险库的指定类型的自动拾取规则
     */
    fun getAutoPickupsByType(vault: Vault, type: AutoPickupType, owner: String): List<AutoPickup> {
        return AutoPickup.getAutoPickupsByType(vault, type, owner)
    }

    /**
     * 创建自动拾取规则
     */
    fun createAutoPickup(vault: Vault, type: AutoPickupType, value: String, owner: String): Result {
        val result = AutoPickup.createAutoPickup(vault, type, value, owner)
        if (result.success) {
            com.faithl.zephyrion.core.services.AutoPickupService.invalidateAllCache()
        }
        return result
    }

    /**
     * 删除自动拾取规则
     */
    fun deleteAutoPickup(autoPickup: AutoPickup): Boolean {
        val success = autoPickup.deleteRule()
        if (success) {
            com.faithl.zephyrion.core.services.AutoPickupService.invalidateAllCache()
        }
        return success
    }

    /**
     * 删除保险库指定 owner 的所有自动拾取规则
     */
    fun clearAutoPickups(vault: Vault, owner: String): Int {
        val count = AutoPickup.clearAutoPickups(vault, owner)
        if (count > 0) {
            com.faithl.zephyrion.core.services.AutoPickupService.invalidateAllCache()
        }
        return count
    }

    /**
     * 更新自动拾取规则的值
     */
    fun updateAutoPickup(autoPickup: AutoPickup, newValue: String): Result {
        val result = autoPickup.updateValue(newValue)
        if (result.success) {
            com.faithl.zephyrion.core.services.AutoPickupService.invalidateAllCache()
        }
        return result
    }

    /**
     * 检查物品是否匹配自动拾取规则
     */
    fun shouldAutoPickup(itemStack: ItemStack, vault: Vault, owner: String): Boolean? {
        return AutoPickup.shouldAutoPickup(itemStack, vault, owner)
    }
}
