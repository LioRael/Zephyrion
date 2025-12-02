package com.faithl.zephyrion.core.settings

import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.core.models.Setting
import com.faithl.zephyrion.core.models.Vault
import com.faithl.zephyrion.core.ui.UI
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * 保险库设置项抽象类
 * @param T 设置值的类型
 */
abstract class VaultSetting<T>(
    val opener: Player,
    val vault: Vault,
    val root: UI?
) {
    /**
     * 设置项的唯一标识 key，用于存储到数据库
     * 如果为 null 则表示该设置不需要存储（如跳转类设置）
     */
    open val settingKey: String? = null

    /**
     * 使用此设置项所需的权限
     * 如果为 null 则表示不需要权限
     */
    open val permission: String? = null

    /**
     * 默认值
     */
    abstract val defaultValue: T

    /**
     * 设置项名称
     */
    abstract val name: String

    /**
     * 功能描述（常驻显示）
     */
    abstract val description: List<String>

    /**
     * 检查玩家是否有权限使用此设置
     */
    fun hasPermission(): Boolean {
        val perm = permission ?: return true
        return opener.hasPermission(perm) || ZephyrionAPI.isPluginAdmin(opener)
    }

    /**
     * 将值序列化为字符串存储
     */
    abstract fun serialize(value: T): String

    /**
     * 将字符串反序列化为值
     */
    abstract fun deserialize(raw: String): T

    /**
     * 获取当前设置值
     */
    fun getValue(): T? {
        val key = settingKey ?: return null
        val raw = Setting.get(vault, key, opener.uniqueId.toString()) ?: return null
        return deserialize(raw)
    }

    /**
     * 获取当前设置值，如果不存在则返回默认值
     */
    fun getValueOrDefault(): T {
        return getValue() ?: defaultValue
    }

    /**
     * 设置值
     */
    fun setValue(value: T) {
        val key = settingKey ?: return
        Setting.set(vault, key, serialize(value), opener.uniqueId.toString())
    }

    /**
     * 构建在 UI 中显示的物品
     */
    abstract fun buildItem(): ItemStack

    /**
     * 点击时执行的操作
     */
    abstract fun onClick()
}
