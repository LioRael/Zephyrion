package com.faithl.zephyrion.storage

import com.faithl.zephyrion.Zephyrion
import com.faithl.zephyrion.core.models.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency

@RuntimeDependencies(
    RuntimeDependency("org.jetbrains.exposed:exposed-core:0.41.1", isolated = true),
    RuntimeDependency("org.jetbrains.exposed:exposed-dao:0.41.1", isolated = true),
    RuntimeDependency("org.jetbrains.exposed:exposed-jdbc:0.41.1", isolated = true),
)
object DatabaseConfig {

    fun connectToDatabase() {
        val type = Zephyrion.settings.getString("database.type")
        if (type != null) {
            when (type.lowercase()) {
                "sqlite" -> {
                    SQLite.connect()
                }

                "mysql" -> {
                    MySQL.connect()
                }

                else -> {
                    error("unsupported database type: $type")
                }
            }
            transaction {
                SchemaUtils.create(Workspaces, Vaults, Items, Quotas, Settings, AutoPickups)
            }
        } else {
            error("database type is not set")
        }
    }

}