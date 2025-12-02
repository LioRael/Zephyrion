package com.faithl.zephyrion.core.models

import com.faithl.zephyrion.core.cache.SettingCache
import com.faithl.zephyrion.storage.DatabaseConfig
import taboolib.module.database.Table

/**
 * Setting数据类
 */
data class Setting(
    var id: Int,
    var setting: String,
    var value: String,
    var vaultId: Int,
    var owner: String,
    var createdAt: Long,
    var updatedAt: Long
) {

    companion object {

        private val table: Table<*, *>
            get() = DatabaseConfig.settingsTable

        private val dataSource
            get() = DatabaseConfig.dataSource

        /**
         * 获取设置值（使用缓存）
         */
        fun get(vault: Vault, setting: String, owner: String): String? {
            return SettingCache.get(vault.id, setting, owner)
        }

        /**
         * 获取设置值，如果不存在则返回默认值
         */
        fun getOrDefault(vault: Vault, setting: String, owner: String, default: String): String {
            return get(vault, setting, owner) ?: default
        }

        /**
         * 设置值（创建或更新）
         */
        fun set(vault: Vault, setting: String, value: String, owner: String) {
            val existing = table.select(dataSource) {
                where {
                    "vault_id" eq vault.id
                    and { "setting" eq setting }
                    and { "owner" eq owner }
                }
            }.firstOrNull { getInt("id") }

            if (existing != null) {
                table.update(dataSource) {
                    set("value", value)
                    set("updated_at", System.currentTimeMillis())
                    where { "id" eq existing }
                }
            } else {
                create(vault, setting, value, owner)
            }
            // 更新缓存
            SettingCache.set(vault.id, setting, owner, value)
        }

        /**
         * 创建设置
         */
        fun create(vault: Vault, setting: String, value: String, owner: String) {
            table.insert(dataSource, "setting", "value", "vault_id", "owner", "created_at", "updated_at") {
                value(
                    setting,
                    value,
                    vault.id,
                    owner,
                    System.currentTimeMillis(),
                    System.currentTimeMillis()
                )
            }
            // 更新缓存
            SettingCache.set(vault.id, setting, owner, value)
        }

        /**
         * 删除设置
         */
        fun delete(vault: Vault, setting: String, owner: String) {
            table.delete(dataSource) {
                where {
                    "vault_id" eq vault.id
                    and { "setting" eq setting }
                    and { "owner" eq owner }
                }
            }
            // 清除缓存
            SettingCache.invalidate(vault.id, setting, owner)
        }
    }

    /**
     * 获取关联的保险库
     */
    val vault: Vault
        get() = Vault.findById(vaultId)
            ?: error("Vault not found for setting $id")

    /**
     * 更新设置值
     */
    fun updateValue(newValue: String) {
        value = newValue
        updatedAt = System.currentTimeMillis()
        table.update(dataSource) {
            set("value", value)
            set("updated_at", updatedAt)
            where { "id" eq id }
        }
    }
}
