package com.faithl.zephyrion.core.ui.vault.autopickup

import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.core.models.AutoPickup
import com.faithl.zephyrion.core.models.AutoPickupType
import com.faithl.zephyrion.core.models.Vault
import com.faithl.zephyrion.core.ui.SearchUI
import com.faithl.zephyrion.core.ui.UI
import com.faithl.zephyrion.core.ui.search.SearchItem
import com.faithl.zephyrion.core.ui.setLinkedMenuProperties
import com.faithl.zephyrion.core.ui.setRows6SplitBlock
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import taboolib.common.util.sync
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.buildMenu
import taboolib.module.ui.type.PageableChest
import taboolib.module.ui.type.impl.PageableChestImpl
import taboolib.platform.util.*

class ListAutoPickups(override val opener: Player, val vault: Vault, override val root: UI? = null) : SearchUI() {

    val rules = mutableListOf<AutoPickup>()

    init {
        addSearchItems("value")
        addSearchItems("type")
    }

    override fun search() {
        rules.clear()
        rules.addAll(ZephyrionAPI.getAutoPickups(vault))
        if (params.isEmpty()) {
            return
        }
        params["value"]?.let { searchValue ->
            rules.retainAll { rule ->
                rule.value.contains(searchValue, ignoreCase = true)
            }
        }
        params["type"]?.let { searchType ->
            rules.retainAll { rule ->
                when (searchType.lowercase()) {
                    "pickup", "拾取" -> rule.type == AutoPickupType.ITEM_PICKUP
                    "not-pickup", "notpickup", "不拾取" -> rule.type == AutoPickupType.ITEM_NOT_PICKUP
                    else -> true
                }
            }
        }
    }

    fun addSearchItems(name: String) {
        searchItems += SearchItem(
            opener.asLangText("auto-pickup-search-by-${name}-name"),
            opener.asLangText("auto-pickup-search-by-${name}-desc")
        ) { player ->
            opener.closeInventory()
            opener.sendLang("auto-pickup-search-by-${name}-input")
            opener.nextChat {
                params[name] = it
                sync{ searchUI.open() }
            }
        }
    }

    override fun build(): Inventory {
        search()
        return buildMenu<PageableChestImpl<AutoPickup>>(title()) {
            setLinkedMenuProperties(this)
            setRows6SplitBlock(this)
            setElements(this)
            setAddRuleItem(this)
            setInfoItem(this)
            setClearItem(this)
            setPageTurnItems(this)
            setSearchItem(this)
            setReturnItem(this)
        }
    }

    fun setElements(menu: PageableChest<AutoPickup>) {
        menu.elements { rules }
        menu.onGenerate { _, element, _, _ ->
            if (element.type == AutoPickupType.ITEM_PICKUP) {
                buildItem(XMaterial.LIME_DYE) {
                    name = opener.asLangText("auto-pickup-rule-pickup-name", element.value)
                    lore += opener.asLangTextList("auto-pickup-rule-pickup-desc", element.value)
                }
            } else {
                buildItem(XMaterial.RED_DYE) {
                    name = opener.asLangText("auto-pickup-rule-not-pickup-name", element.value)
                    lore += opener.asLangTextList("auto-pickup-rule-not-pickup-desc", element.value)
                }
            }
        }
        menu.onClick { event, element ->
            if (event.clickEvent().isLeftClick) {
                opener.closeInventory()
                opener.sendLang("auto-pickup-input-rule")
                opener.sendLang("auto-pickup-input-rule-help")
                opener.nextChat {
                    if (it.equals("cancel", ignoreCase = true)) {
                        opener.sendLang("auto-pickup-input-canceled")
                        sync { open() }
                        return@nextChat
                    }
                    val result = ZephyrionAPI.updateAutoPickup(element, it)
                    if (result.success) {
                        opener.sendLang("auto-pickup-update-succeed")
                    } else {
                        when (result.reason) {
                            "auto_pickup_value_empty" -> opener.sendLang("auto-pickup-create-failed-empty")
                            else -> opener.sendLang("auto-pickup-update-failed")
                        }
                    }
                    sync { open() }
                }
            } else if (event.clickEvent().isRightClick) {
                if (ZephyrionAPI.deleteAutoPickup(element)) {
                    opener.sendLang("auto-pickup-delete-succeed")
                    open()
                } else {
                    opener.sendLang("auto-pickup-delete-failed")
                }
            }
        }
    }

    fun setAddRuleItem(menu: PageableChest<AutoPickup>) {
        // 槽位 45: 放置物品添加（推荐）
        menu.set(45) {
            buildItem(XMaterial.HOPPER) {
                name = opener.asLangText("auto-pickup-add-by-item")
                lore += opener.asLangTextList("auto-pickup-add-by-item-desc")
            }
        }
        menu.onClick(45) {
            AddAutoPickup(opener, vault, this).open()
        }

        // 槽位 46: 手动输入添加
        menu.set(46) {
            buildItem(XMaterial.NETHER_STAR) {
                name = opener.asLangText("auto-pickup-add-rule")
                lore += opener.asLangTextList("auto-pickup-add-rule-desc")
            }
        }
        menu.onClick(46) { event ->
            opener.closeInventory()
            opener.sendLang("auto-pickup-input-rule")
            opener.sendLang("auto-pickup-input-rule-help")
            opener.nextChat {
                if (it.equals("cancel", ignoreCase = true)) {
                    opener.sendLang("auto-pickup-input-canceled")
                    sync { open() }
                    return@nextChat
                }
                val ruleType = if (event.clickEvent().isLeftClick) {
                    AutoPickupType.ITEM_PICKUP
                } else {
                    AutoPickupType.ITEM_NOT_PICKUP
                }
                val result = ZephyrionAPI.createAutoPickup(vault, ruleType, it)
                if (result.success) {
                    opener.sendLang("auto-pickup-create-succeed")
                } else {
                    when (result.reason) {
                        "auto_pickup_value_empty" -> opener.sendLang("auto-pickup-create-failed-empty")
                        "auto_pickup_already_exists" -> opener.sendLang("auto-pickup-create-failed-exists")
                    }
                }
                sync { open() }
            }
        }
    }

    fun setInfoItem(menu: PageableChest<AutoPickup>) {
        menu.set(47) {
            val allRules = ZephyrionAPI.getAutoPickups(vault)
            val pickupCount = allRules.count { it.type == AutoPickupType.ITEM_PICKUP }
            val notPickupCount = allRules.count { it.type == AutoPickupType.ITEM_NOT_PICKUP }

            buildItem(XMaterial.BOOK) {
                name = opener.asLangText("auto-pickup-info-name")
                lore += opener.asLangTextList("auto-pickup-info-desc", pickupCount, notPickupCount)
            }
        }
    }

    fun setClearItem(menu: PageableChest<AutoPickup>) {
        menu.set(51) {
            buildItem(XMaterial.LAVA_BUCKET) {
                name = opener.asLangText("auto-pickup-clear")
                lore.add(opener.asLangText("auto-pickup-clear-desc"))
            }
        }
        menu.onClick(51) {
            opener.closeInventory()
            opener.sendLang("auto-pickup-clear-tip")
            opener.nextChat {
                if (it == "Y") {
                    val count = ZephyrionAPI.clearAutoPickups(vault)
                    opener.sendLang("auto-pickup-clear-succeed", count)
                } else {
                    opener.sendLang("auto-pickup-clear-canceled")
                }
                sync { open() }
            }
        }
    }

    fun setPageTurnItems(menu: PageableChest<AutoPickup>) {
        menu.setPreviousPage(48) { _, hasPreviousPage ->
            if (hasPreviousPage) {
                buildItem(XMaterial.ARROW) {
                    name = opener.asLangText("auto-pickup-prev-page")
                }
            } else {
                buildItem(XMaterial.BARRIER) {
                    name = opener.asLangText("auto-pickup-prev-page-disabled")
                }
            }
        }
        menu.setNextPage(50) { _, hasNextPage ->
            if (hasNextPage) {
                buildItem(XMaterial.ARROW) {
                    name = opener.asLangText("auto-pickup-next-page")
                }
            } else {
                buildItem(XMaterial.BARRIER) {
                    name = opener.asLangText("auto-pickup-next-page-disabled")
                }
            }
        }
    }


    override fun open() {
        opener.openInventory(build())
    }

    override fun title(): String {
        return if (params.isNotEmpty()) {
            opener.asLangText("auto-pickup-title-with-search")
        } else {
            opener.asLangText("auto-pickup-title")
        }
    }

}