package com.faithl.zephyrion.core.ui.vault.settings

import com.faithl.zephyrion.core.models.Vault
import com.faithl.zephyrion.core.settings.AutoPickupSetting
import com.faithl.zephyrion.core.settings.AutoReplaceSetting
import com.faithl.zephyrion.core.settings.VaultSetting
import com.faithl.zephyrion.core.ui.UI
import com.faithl.zephyrion.core.ui.setLinkedMenuProperties
import com.faithl.zephyrion.core.ui.setRows6SplitBlock
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.buildMenu
import taboolib.module.ui.type.Chest
import taboolib.module.ui.type.PageableChest
import taboolib.module.ui.type.impl.PageableChestImpl
import taboolib.platform.util.asLangText
import taboolib.platform.util.buildItem

/**
 * 保险库设置界面
 */
class VaultSettingsUI(
    override val opener: Player,
    val vault: Vault,
    override val root: UI?
) : UI() {

    private val settings: List<VaultSetting<*>> by lazy {
        listOf(
            AutoPickupSetting(opener, vault, this),
            AutoReplaceSetting(opener, vault, this)
        ).filter { it.hasPermission() }
    }

    override fun build(): Inventory {
        return buildMenu<PageableChestImpl<VaultSetting<*>>>(title()) {
            setLinkedMenuProperties(this)
            setRows6SplitBlock(this)
            setElements(this)
            setReturnItem(this)
        }
    }

    private fun setElements(menu: PageableChest<VaultSetting<*>>) {
        menu.elements { settings }
        menu.onGenerate { _, element, _, _ ->
            element.buildItem()
        }
        menu.onClick { _, element ->
            element.onClick()
        }
    }

    override fun setReturnItem(menu: Chest) {
        menu.set(53) {
            buildItem(XMaterial.RED_STAINED_GLASS_PANE) {
                name = opener.asLangText("ui-item-name-return")
            }
        }
        menu.onClick(53) {
            root?.open() ?: it.clicker.closeInventory()
        }
    }

    override fun open() {
        opener.openInventory(build())
    }

    override fun title(): String {
        return opener.asLangText("vault-settings-title", vault.name)
    }
}
