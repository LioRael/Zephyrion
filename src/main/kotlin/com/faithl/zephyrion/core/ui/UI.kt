package com.faithl.zephyrion.core.ui

import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

abstract class UI {

    abstract fun build(): Inventory
    abstract fun open(opener: Player)
    abstract fun title(): String

}

abstract class SearchUI : UI() {

    abstract fun search()

}