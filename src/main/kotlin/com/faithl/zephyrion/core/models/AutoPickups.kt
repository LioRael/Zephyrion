package com.faithl.zephyrion.core.models

import com.faithl.zephyrion.api.ZephyrionAPI
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import taboolib.module.nms.getName

object AutoPickups : IntIdTable() {

    val type = enumerationByName("setting", 255, Type::class)
    val value = varchar("value", 255)
    val vault = reference("vault", Vaults)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    enum class Type {
        ITEM_PICKUP,
        ITEM_NOT_PICKUP,
    }

}

class AutoPickup(id: EntityID<Int>) : IntEntity(id) {

    companion object : IntEntityClass<AutoPickup>(AutoPickups) {

        /**
         * 获取保险库的所有自动拾取规则
         * @param vault 保险库
         * @return 自动拾取规则列表
         */
        fun getAutoPickups(vault: Vault): List<AutoPickup> {
            return transaction {
                find { AutoPickups.vault eq vault.id }.toList()
            }
        }

        /**
         * 获取保险库的指定类型的自动拾取规则
         * @param vault 保险库
         * @param type 规则类型
         * @return 自动拾取规则列表
         */
        fun getAutoPickupsByType(vault: Vault, type: AutoPickups.Type): List<AutoPickup> {
            return transaction {
                find {
                    (AutoPickups.vault eq vault.id) and (AutoPickups.type eq type)
                }.toList()
            }
        }

        /**
         * 创建自动拾取规则
         * @param vault 保险库
         * @param type 规则类型
         * @param value 匹配值（物品名称、类型等）
         * @return 创建结果
         */
        fun createAutoPickup(vault: Vault, type: AutoPickups.Type, value: String): ZephyrionAPI.Result {
            if (value.isBlank()) {
                return ZephyrionAPI.Result(false, "auto_pickup_value_empty")
            }

            val existing = transaction {
                find {
                    (AutoPickups.vault eq vault.id) and
                    (AutoPickups.type eq type) and
                    (AutoPickups.value eq value)
                }.firstOrNull()
            }

            if (existing != null) {
                return ZephyrionAPI.Result(false, "auto_pickup_already_exists")
            }

            transaction {
                new {
                    this.vault = vault
                    this.type = type
                    this.value = value
                    this.createdAt = System.currentTimeMillis()
                    this.updatedAt = System.currentTimeMillis()
                }
            }
            return ZephyrionAPI.Result(true)
        }

        /**
         * 删除保险库的所有自动拾取规则
         * @param vault 保险库
         * @return 删除的数量
         */
        fun clearAutoPickups(vault: Vault): Int {
            return transaction {
                val rules = find { AutoPickups.vault eq vault.id }.toList()
                val count = rules.size
                rules.forEach { it.delete() }
                count
            }
        }

        /**
         * 检查物品是否匹配自动拾取规则
         * @param itemStack 物品
         * @param vault 保险库
         * @return 是否应该自动拾取（true: 拾取, false: 不拾取, null: 无规则）
         */
        fun shouldAutoPickup(itemStack: ItemStack, vault: Vault): Boolean? {
            val rules = getAutoPickups(vault)
            if (rules.isEmpty()) return null

            val itemMaterial = itemStack.type.name
            val itemName = itemStack.getName()
            val itemLore = itemStack.itemMeta?.lore ?: emptyList()

            val notPickupRules = rules.filter { it.type == AutoPickups.Type.ITEM_NOT_PICKUP }
            for (rule in notPickupRules) {
                if (matchesRule(itemMaterial, itemName, itemLore, rule.value)) {
                    return false
                }
            }

            val pickupRules = rules.filter { it.type == AutoPickups.Type.ITEM_PICKUP }
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
         *
         * @param itemMaterial 物品材料类型
         * @param itemName 物品名称
         * @param itemLore 物品 lore 列表
         * @param ruleValue 规则值
         * @return 是否匹配
         */
        private fun matchesRule(itemMaterial: String, itemName: String, itemLore: List<String>, ruleValue: String): Boolean {
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

    var type by AutoPickups.type
    var value by AutoPickups.value
    var vault by Vault referencedOn AutoPickups.vault
    var createdAt by AutoPickups.createdAt
    var updatedAt by AutoPickups.updatedAt

    /**
     * 删除自动拾取规则
     * @return 是否成功
     */
    fun deleteRule(): Boolean {
        return transaction {
            this@AutoPickup.delete()
            true
        }
    }

    /**
     * 更新自动拾取规则的值
     * @param newValue 新的匹配值
     * @return 是否成功
     */
    fun updateValue(newValue: String): ZephyrionAPI.Result {
        if (newValue.isBlank()) {
            return ZephyrionAPI.Result(false, "auto_pickup_value_empty")
        }

        transaction {
            this@AutoPickup.value = newValue
            this@AutoPickup.updatedAt = System.currentTimeMillis()
        }
        return ZephyrionAPI.Result(true)
    }

}