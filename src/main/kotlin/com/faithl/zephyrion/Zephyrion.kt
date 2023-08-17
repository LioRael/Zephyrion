package com.faithl.zephyrion

import com.faithl.zephyrion.storage.DatabaseConfig
import taboolib.common.platform.Platform
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.console
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.lang.sendLang
import taboolib.module.metrics.Metrics
import taboolib.platform.BukkitPlugin

object Zephyrion : Plugin() {

    val plugin by lazy {
        BukkitPlugin.getInstance()
    }

    @Config("settings.yml", migrate = true, autoReload = true)
    lateinit var settings: Configuration
        private set

    @Config("permissions.yml", migrate = true, autoReload = true)
    lateinit var permissions: Configuration
        private set

    lateinit var metrics: Metrics
        private set

    override fun onEnable() {
        DatabaseConfig.connectToDatabase()
        console().sendLang("plugin-enabled")
    }


    override fun onActive() {
        metrics = Metrics(19130, plugin.description.version, Platform.BUKKIT)
    }

    override fun onDisable() {
        console().sendLang("plugin-disabled")
    }

}