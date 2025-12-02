package com.faithl.zephyrion.core.settings

import com.faithl.zephyrion.Zephyrion
import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.core.models.AutoPickupType
import com.faithl.zephyrion.core.models.Vault
import com.faithl.zephyrion.core.ui.UI
import com.faithl.zephyrion.core.ui.vault.autopickup.ListAutoPickups
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.xseries.XMaterial
import taboolib.platform.util.asLangText
import taboolib.platform.util.asLangTextList
import taboolib.platform.util.buildItem

/**
 * 自动拾取设置项（跳转类，不需要存储）
 */
class AutoPickupSetting(
    opener: Player,
    vault: Vault,
    root: UI?
) : VaultSetting<Unit>(opener, vault, root) {

    override val permission: String? = Zephyrion.permissions.getString("vault.setting.auto-pick")
    override val defaultValue: Unit = Unit
    override val name: String get() = opener.asLangText("vault-settings-auto-pickup-name")
    override val description: List<String> get() = opener.asLangTextList("vault-settings-auto-pickup-func-desc")

    override fun serialize(value: Unit): String = ""
    override fun deserialize(raw: String): Unit = Unit

    override fun buildItem(): ItemStack {
        val rules = ZephyrionAPI.getAutoPickups(vault, opener.uniqueId.toString())
        val pickupCount = rules.count { it.type == AutoPickupType.ITEM_PICKUP }
        val notPickupCount = rules.count { it.type == AutoPickupType.ITEM_NOT_PICKUP }

        return buildItem(XMaterial.HOPPER) {
            name = this@AutoPickupSetting.name
            lore += description
            lore += ""
            lore += opener.asLangTextList("vault-settings-auto-pickup-status", pickupCount, notPickupCount)
        }
    }

    override fun onClick() {
        ListAutoPickups(opener, vault, root).open()
    }
}
