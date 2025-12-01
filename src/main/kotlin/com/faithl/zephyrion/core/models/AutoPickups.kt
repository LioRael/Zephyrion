package com.faithl.zephyrion.core.models

import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.core.cache.AutoPickupCache
import com.faithl.zephyrion.storage.DatabaseConfig
import org.bukkit.inventory.ItemStack
import taboolib.module.database.Table
import taboolib.module.nms.getName

/**
 * 自动拾取类型枚举
 */
enum class AutoPickupType {
    ITEM_PICKUP,
    ITEM_NOT_PICKUP,
}

/**
 * AutoPickup数据类
 */
data class AutoPickup(
    var id: Int,
    var type: AutoPickupType,
    var value: String,
    var vaultId: Int,
    var createdAt: Long,
    var updatedAt: Long
) : java.io.Serializable {

    companion object {
        private const val serialVersionUID = 1L

        private val table: Table<*, *>
            get() = DatabaseConfig.autoPickupsTable

        private val dataSource
            get() = DatabaseConfig.dataSource

        /**
         * 获取保险库的所有自动拾取规则（使用缓存）
         */
        fun getAutoPickups(vault: Vault): List<AutoPickup> {
            return AutoPickupCache.get(vault)
        }

        /**
         * 获取保险库的指定类型的自动拾取规则
         */
        fun getAutoPickupsByType(vault: Vault, type: AutoPickupType): List<AutoPickup> {
            return AutoPickupCache.get(vault).filter { it.type == type }
        }

        /**
         * 创建自动拾取规则
         */
        fun createAutoPickup(vault: Vault, type: AutoPickupType, value: String): ZephyrionAPI.Result {
            if (value.isBlank()) {
                return ZephyrionAPI.Result(false, "auto_pickup_value_empty")
            }

            val cachedRules = AutoPickupCache.get(vault)
            val existing = cachedRules.any { it.type == type && it.value == value }

            if (existing) {
                return ZephyrionAPI.Result(false, "auto_pickup_already_exists")
            }

            table.insert(dataSource, "type", "value", "vault_id", "created_at", "updated_at") {
                value(
                    type.name,
                    value,
                    vault.id,
                    System.currentTimeMillis(),
                    System.currentTimeMillis()
                )
            }
            AutoPickupCache.invalidate(vault.id)
            return ZephyrionAPI.Result(true)
        }

        /**
         * 删除保险库的所有自动拾取规则
         */
        fun clearAutoPickups(vault: Vault): Int {
            val rules = getAutoPickups(vault)
            val count = rules.size

            table.delete(dataSource) {
                where { "vault_id" eq vault.id }
            }

            AutoPickupCache.invalidate(vault.id)
            return count
        }

        /**
         * 检查物品是否匹配自动拾取规则
         */
        fun shouldAutoPickup(itemStack: ItemStack, vault: Vault): Boolean? {
            val rules = getAutoPickups(vault)
            if (rules.isEmpty()) return null

            val itemMaterial = itemStack.type.name
            val itemName = itemStack.getName()
            val itemLore = itemStack.itemMeta?.lore ?: emptyList()

            val notPickupRules = rules.filter { it.type == AutoPickupType.ITEM_NOT_PICKUP }
            for (rule in notPickupRules) {
                if (matchesRule(itemMaterial, itemName, itemLore, rule.value)) {
                    return false
                }
            }

            val pickupRules = rules.filter { it.type == AutoPickupType.ITEM_PICKUP }
            for (rule in pickupRules) {
                if (matchesRule(itemMaterial, itemName, itemLore, rule.value)) {
                    return true
                }
            }
            return null
        }

        /**
         * 检查物品是否匹配规则
         * 支持的规则格式：
         * - type:DIAMOND - 匹配材料类型
         * - name:钻石剑 - 匹配物品名称（包含）
         * - lore:稀有 - 匹配 lore 中包含指定文字
         * - regex:.*钻石.* - 正则表达式匹配名称
         * - regex-lore:.*传说.* - 正则表达式匹配 lore
         * - 直接文字 - 默认匹配物品名称（包含）
         */
        private fun matchesRule(
            itemMaterial: String,
            itemName: String,
            itemLore: List<String>,
            ruleValue: String
        ): Boolean {
            return when {
                ruleValue.startsWith("type:", ignoreCase = true) -> {
                    val material = ruleValue.substring(5)
                    itemMaterial.equals(material, ignoreCase = true)
                }

                ruleValue.startsWith("name:", ignoreCase = true) -> {
                    val name = ruleValue.substring(5)
                    itemName.contains(name, ignoreCase = true)
                }

                ruleValue.startsWith("lore:", ignoreCase = true) -> {
                    val loreText = ruleValue.substring(5)
                    itemLore.any { it.contains(loreText, ignoreCase = true) }
                }

                ruleValue.startsWith("regex:", ignoreCase = true) -> {
                    val pattern = ruleValue.substring(6)
                    try {
                        val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
                        regex.containsMatchIn(itemName)
                    } catch (e: Exception) {
                        false
                    }
                }

                ruleValue.startsWith("regex-lore:", ignoreCase = true) -> {
                    val pattern = ruleValue.substring(11)
                    try {
                        val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
                        itemLore.any { regex.containsMatchIn(it) }
                    } catch (e: Exception) {
                        false
                    }
                }

                else -> {
                    itemName.contains(ruleValue, ignoreCase = true)
                }
            }
        }
    }

    /**
     * 获取关联的保险库
     */
    val vault: Vault
        get() = Vault.findById(vaultId)
            ?: error("Vault not found for auto pickup $id")

    /**
     * 删除自动拾取规则
     */
    fun deleteRule(): Boolean {
        table.delete(dataSource) {
            where { "id" eq id }
        }
        AutoPickupCache.invalidate(vaultId)
        return true
    }

    /**
     * 更新自动拾取规则的值
     */
    fun updateValue(newValue: String): ZephyrionAPI.Result {
        if (newValue.isBlank()) {
            return ZephyrionAPI.Result(false, "auto_pickup_value_empty")
        }

        value = newValue
        updatedAt = System.currentTimeMillis()

        table.update(dataSource) {
            set("value", value)
            set("updated_at", updatedAt)
            where { "id" eq id }
        }

        AutoPickupCache.invalidate(vaultId)
        return ZephyrionAPI.Result(true)
    }
}
