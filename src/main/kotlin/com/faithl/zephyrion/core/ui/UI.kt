package com.faithl.zephyrion.core.ui

import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

abstract class UI {
    abstract val opener:Player
    abstract fun build(): Inventory
    abstract fun open()
    abstract fun title(): String

}

abstract class SearchUI : UI() {

    abstract val params: MutableMap<String, String>
    abstract fun search()

}