package com.faithl.zephyrion.core.ui.vault.autopickup

import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.core.models.AutoPickupType
import com.faithl.zephyrion.core.models.Vault
import com.faithl.zephyrion.core.services.AutoPickupService
import com.faithl.zephyrion.core.ui.UI
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import taboolib.library.xseries.XMaterial
import taboolib.module.nms.getName
import taboolib.module.ui.buildMenu
import taboolib.module.ui.returnItems
import taboolib.module.ui.type.Chest
import taboolib.module.ui.type.StorableChest
import taboolib.module.ui.type.impl.StorableChestImpl
import taboolib.platform.util.*

/**
 * 添加自动拾取规则的 UI
 * 支持放入物品一键添加规则
 */
class AddAutoPickup(
    override val opener: Player,
    val vault: Vault,
    override val root: UI? = null
) : UI() {

    // 当前选择的规则类型
    var selectedType: AutoPickupType = AutoPickupType.ITEM_PICKUP

    override fun build(): Inventory {
        return buildMenu<StorableChestImpl>(title()) {
            rows(4)
            handLocked(false)
            map(
                "####I####",
                "#P#####N#",
                "#T#L#M#R#",
                "####B####"
            )

            rule {
                checkSlot { _, _, slot -> slot == getFirstSlot('I') }
                firstSlot { _, _ -> getFirstSlot('I') }
                writeItem { inventory, itemStack, slot, _ ->
                    if (slot == getFirstSlot('I')) {
                        inventory.setItem(slot, itemStack)
                        updateRuleButtons(this@buildMenu, inventory, if (itemStack.isAir) null else itemStack)
                    }
                }
                readItem { inventory, slot ->
                    if (slot == getFirstSlot('I')) inventory.getItem(slot) else null
                }
            }

            onBuild { _, inventory ->
                updateRuleButtons(this@buildMenu, inventory, null)
            }

            onClose {
                it.returnItems(getSlots('I'))
            }

            setBackground(this)
            setTypeSelector(this)
            setRuleButtons(this)
            setReturnItem(this)
        }
    }

    private fun setBackground(menu: StorableChest) {
        menu.set('#') {
            buildItem(XMaterial.BLACK_STAINED_GLASS_PANE) { name = "§r" }
        }
    }

    private fun setTypeSelector(menu: StorableChest) {
        menu.set('P') {
            buildTypeSelectorItem(AutoPickupType.ITEM_PICKUP)
        }
        menu.onClick('P') {
            if (selectedType != AutoPickupType.ITEM_PICKUP) {
                selectedType = AutoPickupType.ITEM_PICKUP
                it.inventory.setItem(menu.getFirstSlot('P'), buildTypeSelectorItem(AutoPickupType.ITEM_PICKUP))
                it.inventory.setItem(menu.getFirstSlot('N'), buildTypeSelectorItem(AutoPickupType.ITEM_NOT_PICKUP))
            }
        }

        menu.set('N') {
            buildTypeSelectorItem(AutoPickupType.ITEM_NOT_PICKUP)
        }
        menu.onClick('N') {
            if (selectedType != AutoPickupType.ITEM_NOT_PICKUP) {
                selectedType = AutoPickupType.ITEM_NOT_PICKUP
                it.inventory.setItem(menu.getFirstSlot('P'), buildTypeSelectorItem(AutoPickupType.ITEM_PICKUP))
                it.inventory.setItem(menu.getFirstSlot('N'), buildTypeSelectorItem(AutoPickupType.ITEM_NOT_PICKUP))
            }
        }
    }

    private fun buildTypeSelectorItem(type: AutoPickupType): ItemStack {
        val isSelected = selectedType == type
        return if (type == AutoPickupType.ITEM_PICKUP) {
            buildItem(if (isSelected) XMaterial.LIME_DYE else XMaterial.GRAY_DYE) {
                name = opener.asLangText("auto-pickup-add-type-pickup")
                lore += opener.asLangTextList(
                    if (isSelected) "auto-pickup-add-type-pickup-selected" else "auto-pickup-add-type-pickup-unselected"
                )
            }
        } else {
            buildItem(if (isSelected) XMaterial.RED_DYE else XMaterial.GRAY_DYE) {
                name = opener.asLangText("auto-pickup-add-type-not-pickup")
                lore += opener.asLangTextList(
                    if (isSelected) "auto-pickup-add-type-not-pickup-selected" else "auto-pickup-add-type-not-pickup-unselected"
                )
            }
        }
    }

    private fun setRuleButtons(menu: StorableChest) {
        menu.onClick('T') { event ->
            val item = event.inventory.getItem(menu.getFirstSlot('I'))
            if (item != null && !item.isAir) {
                addRule("type:${item.type.name}", event.inventory)
            } else {
                opener.sendLang("auto-pickup-add-no-item")
            }
        }

        menu.onClick('L') { event ->
            val item = event.inventory.getItem(menu.getFirstSlot('I'))
            if (item != null && !item.isAir) {
                val loreText = item.itemMeta?.lore?.firstOrNull()
                if (loreText != null) {
                    addRule("lore:$loreText", event.inventory)
                } else {
                    opener.sendLang("auto-pickup-add-no-lore")
                }
            } else {
                opener.sendLang("auto-pickup-add-no-item")
            }
        }

        menu.onClick('M') { event ->
            val item = event.inventory.getItem(menu.getFirstSlot('I'))
            if (item != null && !item.isAir) {
                addRule("name:${item.getName()}", event.inventory)
            } else {
                opener.sendLang("auto-pickup-add-no-item")
            }
        }

        menu.onClick('R') { event ->
            val item = event.inventory.getItem(menu.getFirstSlot('I'))
            if (item != null && !item.isAir) {
                addRule("regex:^${Regex.escape(item.getName())}$", event.inventory)
            } else {
                opener.sendLang("auto-pickup-add-no-item")
            }
        }
    }

    private fun updateRuleButtons(menu: StorableChest, inventory: Inventory, item: ItemStack?) {
        if (item != null && !item.isAir) {
            inventory.setItem(menu.getFirstSlot('T'), buildItem(XMaterial.IRON_INGOT) {
                name = opener.asLangText("auto-pickup-add-by-type")
                lore += opener.asLangTextList("auto-pickup-add-by-type-desc", item.type.name)
            })

            val loreText = item.itemMeta?.lore?.firstOrNull()
            inventory.setItem(menu.getFirstSlot('L'), if (loreText != null) {
                buildItem(XMaterial.PAPER) {
                    name = opener.asLangText("auto-pickup-add-by-lore")
                    lore += opener.asLangTextList("auto-pickup-add-by-lore-desc", loreText.take(20))
                }
            } else {
                buildItem(XMaterial.BARRIER) {
                    name = opener.asLangText("auto-pickup-add-no-lore")
                }
            })

            val displayName = item.getName()
            inventory.setItem(menu.getFirstSlot('M'), buildItem(XMaterial.NAME_TAG) {
                name = opener.asLangText("auto-pickup-add-by-name")
                lore += opener.asLangTextList("auto-pickup-add-by-name-desc", displayName)
            })

            inventory.setItem(menu.getFirstSlot('R'), buildItem(XMaterial.GOLDEN_APPLE) {
                name = opener.asLangText("auto-pickup-add-by-exact-name")
                lore += opener.asLangTextList("auto-pickup-add-by-exact-name-desc", displayName)
            })
        } else {
            val noItemIcon = buildItem(XMaterial.BARRIER) {
                name = opener.asLangText("auto-pickup-add-no-item")
            }
            inventory.setItem(menu.getFirstSlot('T'), noItemIcon.clone())
            inventory.setItem(menu.getFirstSlot('L'), noItemIcon.clone())
            inventory.setItem(menu.getFirstSlot('M'), noItemIcon.clone())
            inventory.setItem(menu.getFirstSlot('R'), noItemIcon.clone())
        }
    }

    override fun setReturnItem(menu: Chest) {
        menu.set('B') {
            buildItem(XMaterial.RED_STAINED_GLASS_PANE) {
                name = opener.asLangText("ui-item-name-return")
            }
        }
        menu.onClick('B') {
            root?.open() ?: opener.closeInventory()
        }
    }

    private fun addRule(ruleValue: String, inventory: Inventory) {
        val result = ZephyrionAPI.createAutoPickup(vault, selectedType, ruleValue)
        if (result.success) {
            opener.sendLang("auto-pickup-create-succeed")
            AutoPickupService.invalidateAllCache()
            root?.open() ?: opener.closeInventory()
        } else {
            when (result.reason) {
                "auto_pickup_value_empty" -> opener.sendLang("auto-pickup-create-failed-empty")
                "auto_pickup_already_exists" -> opener.sendLang("auto-pickup-create-failed-exists")
                else -> opener.sendLang("auto-pickup-create-failed")
            }
        }
    }

    override fun open() {
        opener.openInventory(build())
    }

    override fun title(): String {
        return opener.asLangText("auto-pickup-add-title")
    }
}