package com.faithl.zephyrion.core.ui

import taboolib.library.xseries.XMaterial
import taboolib.module.ui.type.Basic
import taboolib.module.ui.type.Linked
import taboolib.platform.util.buildItem

val slots = mutableListOf(
    0, 1, 2, 3, 4, 5, 6, 7, 8,
    9, 10, 11, 12, 13, 14, 15, 16, 17,
    18, 19, 20, 21, 22, 23, 24, 25, 26,
    27, 28, 29, 30, 31, 32, 33, 34, 35
)

fun <T> setLinkedMenuProperties(menu: Linked<T>) {
    menu.rows(6)
    menu.slots(slots)
    menu.handLocked(true)
}

fun setRows6SplitBlock(menu: Basic) {
    for (i in 36..44) {
        menu.set(i) {
            buildItem(XMaterial.BLACK_STAINED_GLASS_PANE) {
                name = "§r"
            }
        }
    }
}

fun setSplitBlock(menu: Basic) {
    menu.set('#') {
        buildItem(XMaterial.BLACK_STAINED_GLASS_PANE) {
            name = "§r"
        }
    }
}