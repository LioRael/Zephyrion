package com.faithl.zephyrion.storage

import taboolib.module.database.*

/**
 * 配额表定义
 */
object QuotasTable {

    fun createTable(host: Host<*>): Table<*, *> {
        val tableName = DatabaseConfig.getTableName("quotas")

        return when (host) {
            is HostSQL -> {
                Table(tableName, host) {
                    add("id") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(
                                ColumnOptionSQL.PRIMARY_KEY,
                                ColumnOptionSQL.AUTO_INCREMENT,
                                ColumnOptionSQL.UNSIGNED
                            )
                        }
                    }
                    add("player") {
                        type(ColumnTypeSQL.VARCHAR, 36) {
                            options(ColumnOptionSQL.UNIQUE_KEY, ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("workspace_quotas") {
                        type(ColumnTypeSQL.INT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("workspace_used") {
                        type(ColumnTypeSQL.INT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("size_quotas") {
                        type(ColumnTypeSQL.INT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("size_used") {
                        type(ColumnTypeSQL.INT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("unlimited") {
                        type(ColumnTypeSQL.BOOLEAN) {
                            options(ColumnOptionSQL.NOTNULL)
                            def(false)
                        }
                    }
                }
            }
            is HostSQLite -> {
                Table(tableName, host) {
                    add("id") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(
                                ColumnOptionSQLite.PRIMARY_KEY,
                                ColumnOptionSQLite.AUTOINCREMENT
                            )
                        }
                    }
                    add("player") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("workspace_quotas") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("workspace_used") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("size_quotas") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("size_used") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("unlimited") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                            def(0)
                        }
                    }
                }.also { table ->
                    // SQLite 使用索引实现唯一约束
                    table.index("unique_player", listOf("player"), unique = true)
                }
            }
            else -> error("unknown database type")
        }
    }
}

/**
 * 工作空间表定义
 */
object WorkspacesTable {

    fun createTable(host: Host<*>): Table<*, *> {
        val tableName = DatabaseConfig.getTableName("workspaces")

        return when (host) {
            is HostSQL -> {
                Table(tableName, host) {
                    add("id") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(
                                ColumnOptionSQL.PRIMARY_KEY,
                                ColumnOptionSQL.AUTO_INCREMENT,
                                ColumnOptionSQL.UNSIGNED
                            )
                        }
                    }
                    add("name") {
                        type(ColumnTypeSQL.VARCHAR, 255) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("description") {
                        type(ColumnTypeSQL.VARCHAR, 255)
                    }
                    add("type") {
                        type(ColumnTypeSQL.VARCHAR, 50) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("owner") {
                        type(ColumnTypeSQL.VARCHAR, 36) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("members") {
                        type(ColumnTypeSQL.TEXT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("created_at") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("updated_at") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                }
            }
            is HostSQLite -> {
                Table(tableName, host) {
                    add("id") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(
                                ColumnOptionSQLite.PRIMARY_KEY,
                                ColumnOptionSQLite.AUTOINCREMENT
                            )
                        }
                    }
                    add("name") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("description") {
                        type(ColumnTypeSQLite.TEXT)
                    }
                    add("type") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("owner") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("members") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("created_at") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("updated_at") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                }
            }
            else -> error("unknown database type")
        }
    }
}

/**
 * 保险库表定义
 */
object VaultsTable {

    fun createTable(host: Host<*>): Table<*, *> {
        val tableName = DatabaseConfig.getTableName("vaults")

        return when (host) {
            is HostSQL -> {
                Table(tableName, host) {
                    add("id") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(
                                ColumnOptionSQL.PRIMARY_KEY,
                                ColumnOptionSQL.AUTO_INCREMENT,
                                ColumnOptionSQL.UNSIGNED
                            )
                        }
                    }
                    add("name") {
                        type(ColumnTypeSQL.VARCHAR, 255) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("description") {
                        type(ColumnTypeSQL.VARCHAR, 255)
                    }
                    add("workspace_id") {
                        type(ColumnTypeSQL.INT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("size") {
                        type(ColumnTypeSQL.INT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("created_at") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("updated_at") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                }
            }
            is HostSQLite -> {
                Table(tableName, host) {
                    add("id") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(
                                ColumnOptionSQLite.PRIMARY_KEY,
                                ColumnOptionSQLite.AUTOINCREMENT
                            )
                        }
                    }
                    add("name") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("description") {
                        type(ColumnTypeSQLite.TEXT)
                    }
                    add("workspace_id") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("size") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("created_at") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("updated_at") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                }
            }
            else -> error("unknown database type")
        }
    }
}

/**
 * 物品表定义
 */
object ItemsTable {

    fun createTable(host: Host<*>): Table<*, *> {
        val tableName = DatabaseConfig.getTableName("items")

        return when (host) {
            is HostSQL -> {
                Table(tableName, host) {
                    add("id") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(
                                ColumnOptionSQL.PRIMARY_KEY,
                                ColumnOptionSQL.AUTO_INCREMENT,
                                ColumnOptionSQL.UNSIGNED
                            )
                        }
                    }
                    add("vault_id") {
                        type(ColumnTypeSQL.INT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("page") {
                        type(ColumnTypeSQL.INT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("owner") {
                        type(ColumnTypeSQL.VARCHAR, 36)
                    }
                    add("slot") {
                        type(ColumnTypeSQL.INT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("item_stack") {
                        type(ColumnTypeSQL.TEXT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                }
            }
            is HostSQLite -> {
                Table(tableName, host) {
                    add("id") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(
                                ColumnOptionSQLite.PRIMARY_KEY,
                                ColumnOptionSQLite.AUTOINCREMENT
                            )
                        }
                    }
                    add("vault_id") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("page") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("owner") {
                        type(ColumnTypeSQLite.TEXT)
                    }
                    add("slot") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("item_stack") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                }
            }
            else -> error("unknown database type")
        }
    }
}

/**
 * 设置表定义
 */
object SettingsTable {

    enum class SettingType {
        AUTO_PICKUP
    }

    fun createTable(host: Host<*>): Table<*, *> {
        val tableName = DatabaseConfig.getTableName("settings")

        return when (host) {
            is HostSQL -> {
                Table(tableName, host) {
                    add("id") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(
                                ColumnOptionSQL.PRIMARY_KEY,
                                ColumnOptionSQL.AUTO_INCREMENT,
                                ColumnOptionSQL.UNSIGNED
                            )
                        }
                    }
                    add("setting") {
                        type(ColumnTypeSQL.VARCHAR, 255) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("value") {
                        type(ColumnTypeSQL.VARCHAR, 255) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("vault_id") {
                        type(ColumnTypeSQL.INT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("created_at") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("updated_at") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                }
            }
            is HostSQLite -> {
                Table(tableName, host) {
                    add("id") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(
                                ColumnOptionSQLite.PRIMARY_KEY,
                                ColumnOptionSQLite.AUTOINCREMENT
                            )
                        }
                    }
                    add("setting") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("value") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("vault_id") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("created_at") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("updated_at") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                }
            }
            else -> error("unknown database type")
        }
    }
}

/**
 * 自动拾取表定义
 */
object AutoPickupsTable {

    enum class Type {
        ITEM_PICKUP,
        ITEM_NOT_PICKUP
    }

    fun createTable(host: Host<*>): Table<*, *> {
        val tableName = DatabaseConfig.getTableName("auto_pickups")

        return when (host) {
            is HostSQL -> {
                Table(tableName, host) {
                    add("id") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(
                                ColumnOptionSQL.PRIMARY_KEY,
                                ColumnOptionSQL.AUTO_INCREMENT,
                                ColumnOptionSQL.UNSIGNED
                            )
                        }
                    }
                    add("vault_id") {
                        type(ColumnTypeSQL.INT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("type") {
                        type(ColumnTypeSQL.VARCHAR, 50) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("value") {
                        type(ColumnTypeSQL.VARCHAR, 255) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("created_at") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                }
            }
            is HostSQLite -> {
                Table(tableName, host) {
                    add("id") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(
                                ColumnOptionSQLite.PRIMARY_KEY,
                                ColumnOptionSQLite.AUTOINCREMENT
                            )
                        }
                    }
                    add("vault_id") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("type") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("value") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("created_at") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                }
            }
            else -> error("unknown database type")
        }
    }
}
