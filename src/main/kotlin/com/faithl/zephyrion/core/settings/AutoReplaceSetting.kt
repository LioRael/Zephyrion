package com.faithl.zephyrion.core.settings

import com.faithl.zephyrion.Zephyrion
import com.faithl.zephyrion.core.models.Vault
import com.faithl.zephyrion.core.ui.UI
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.xseries.XMaterial
import taboolib.platform.util.asLangText
import taboolib.platform.util.asLangTextList
import taboolib.platform.util.buildItem

/**
 * 自动替换设置项
 */
class AutoReplaceSetting(
    opener: Player,
    vault: Vault,
    root: UI?
) : VaultSetting<Boolean>(opener, vault, root) {

    companion object {
        const val SETTING_KEY = "auto_replace"
    }

    override val permission: String? = Zephyrion.permissions.getString("vault.setting.auto-replace")
    override val settingKey = SETTING_KEY
    override val defaultValue = false
    override val name: String get() = opener.asLangText("vault-settings-auto-replace-name")
    override val description: List<String> get() = opener.asLangTextList("vault-settings-auto-replace-func-desc")

    override fun serialize(value: Boolean): String = value.toString()
    override fun deserialize(raw: String): Boolean = raw.toBoolean()

    override fun buildItem(): ItemStack {
        val enabled = getValueOrDefault()
        return buildItem(if (enabled) XMaterial.LIME_DYE else XMaterial.GRAY_DYE) {
            name = this@AutoReplaceSetting.name
            lore += description
            lore += ""
            lore += if (enabled) {
                opener.asLangTextList("vault-settings-auto-replace-enabled")
            } else {
                opener.asLangTextList("vault-settings-auto-replace-disabled")
            }
        }
    }

    override fun onClick() {
        val currentValue = getValueOrDefault()
        setValue(!currentValue)
        // 刷新界面
        root?.open()
    }
}
