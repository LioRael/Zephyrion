package com.faithl.zephyrion.storage

import com.faithl.zephyrion.Zephyrion
import taboolib.common.platform.function.console
import taboolib.module.database.Host
import taboolib.module.database.HostSQL
import taboolib.module.database.HostSQLite
import taboolib.module.database.Table
import taboolib.module.lang.sendErrorMessage
import java.io.File
import javax.sql.DataSource

object DatabaseConfig {

    lateinit var host: Host<*>
        private set

    val dataSource: DataSource by lazy {
        host.createDataSource()
    }

    lateinit var workspacesTable: Table<*, *>
    lateinit var vaultsTable: Table<*, *>
    lateinit var itemsTable: Table<*, *>
    lateinit var quotasTable: Table<*, *>
    lateinit var settingsTable: Table<*, *>
    lateinit var autoPickupsTable: Table<*, *>

    fun initialize() {
        connectToDatabase()
        createTables()
    }

    private fun connectToDatabase() {
        val type = Zephyrion.settings.getString("database.type")
            ?: run{
                console().sendErrorMessage("database type is not set")
                "sqlite"
            }

        host = when (type.lowercase()) {
            "sqlite" -> createSQLiteHost()
            "mysql" -> createMySQLHost()
            else ->  run {
                console().sendErrorMessage("unsupported database type: $type")
                createSQLiteHost()
            }
        }
    }

    private fun createSQLiteHost(): HostSQLite {
        val dataFolder = Zephyrion.plugin.dataFolder
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        val dbFile = File(dataFolder, "zephyrion.db")
        return HostSQLite(dbFile)
    }

    private fun createMySQLHost(): HostSQL {
        val host = Zephyrion.settings.getString("database.host") ?: "localhost"
        val port = Zephyrion.settings.getString("database.port") ?: "3306"
        val database = Zephyrion.settings.getString("database.database") ?: "zephyrion"
        val username = Zephyrion.settings.getString("database.username") ?: "root"
        val password = Zephyrion.settings.getString("database.password") ?: ""

        return HostSQL(
            host = host,
            port = port,
            user = username,
            password = password,
            database = database,
        )
    }

    private fun createTables() {
        // 创建 Quotas 表
        quotasTable = QuotasTable.createTable(host)
        quotasTable.createTable(dataSource)

        // 创建 Workspaces 表
        workspacesTable = WorkspacesTable.createTable(host)
        workspacesTable.createTable(dataSource)

        // 初始化公共工作空间
        com.faithl.zephyrion.core.models.Workspace.initializeIndependentWorkspace()

        // 创建 Vaults 表
        vaultsTable = VaultsTable.createTable(host)
        vaultsTable.createTable(dataSource)

        // 创建 Items 表
        itemsTable = ItemsTable.createTable(host)
        itemsTable.createTable(dataSource)

        // 创建 Settings 表
        settingsTable = SettingsTable.createTable(host)
        settingsTable.createTable(dataSource)

        // 创建 AutoPickups 表
        autoPickupsTable = AutoPickupsTable.createTable(host)
        autoPickupsTable.createTable(dataSource)

        // MySQL 设置表字符集为 utf8mb4
        if (host is HostSQL) {
            val tables = listOf("quotas", "workspaces", "vaults", "items", "settings", "auto_pickups")
            dataSource.connection.use { conn ->
                tables.forEach { name ->
                    runCatching {
                        conn.prepareStatement(
                            "ALTER TABLE `${getTableName(name)}` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
                        ).use { it.executeUpdate() }
                    }
                }
            }
        }
    }

    /**
     * 获取表名(支持前缀配置)
     */
    fun getTableName(name: String): String {
        val prefix = Zephyrion.settings.getString("database.table-prefix") ?: ""
        return prefix + name
    }
}
