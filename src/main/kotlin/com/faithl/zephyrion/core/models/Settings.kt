package com.faithl.zephyrion.core.models

import com.faithl.zephyrion.storage.DatabaseConfig
import taboolib.module.database.Table

/**
 * 设置类型枚举
 */
enum class SettingType {
    // 可在此处添加设置类型
}

/**
 * Setting数据类
 */
data class Setting(
    var id: Int,
    var setting: String,
    var value: String,
    var vaultId: Int,
    var createdAt: Long,
    var updatedAt: Long
) {

    companion object {

        private val table: Table<*, *>
            get() = DatabaseConfig.settingsTable

        private val dataSource
            get() = DatabaseConfig.dataSource

        /**
         * 创建设置
         */
        fun create(vault: Vault, setting: String, value: String) {
            table.insert(dataSource, "setting", "value", "vault_id", "created_at", "updated_at") {
                value(
                    setting,
                    value,
                    vault.id,
                    System.currentTimeMillis(),
                    System.currentTimeMillis()
                )
            }
        }
    }

    /**
     * 获取关联的保险库
     */
    val vault: Vault
        get() = Vault.findById(vaultId)
            ?: error("Vault not found for setting $id")
}
