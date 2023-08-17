package com.faithl.zephyrion.storage

import com.faithl.zephyrion.Zephyrion
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency

@RuntimeDependencies(
    RuntimeDependency(
        "!com.zaxxer:HikariCP:5.0.1",
        test = "!hikari501.HikariDataSource",
        relocate = ["!com.zaxxer.hikari", "!hikari501"]
    ),
)
object MySQL : Type() {

    override fun connect() {
        val host = Zephyrion.settings.getString("database.host")
        val port = Zephyrion.settings.getInt("database.port")
        val database = Zephyrion.settings.getString("database.database")
        val username = Zephyrion.settings.getString("database.username")
        val password = Zephyrion.settings.getString("database.password")
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://$host:$port/$database"
            driverClassName = "com.mysql.jdbc.Driver"
            this.username = username
            this.password = password
            maximumPoolSize = 10
        }
        Database.connect(HikariDataSource(config))
    }

}