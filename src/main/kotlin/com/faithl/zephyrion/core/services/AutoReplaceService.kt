package com.faithl.zephyrion.core.services

import com.faithl.zephyrion.Zephyrion
import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.core.models.Item
import com.faithl.zephyrion.core.models.Vault
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerItemBreakEvent
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.module.nms.getName
import taboolib.platform.util.sendLang

object AutoReplaceService {

    // 工具分组，按优先级排序（高到低）
    private val toolGroups = mapOf(
        "pickaxe" to listOf(
            Material.NETHERITE_PICKAXE, Material.DIAMOND_PICKAXE, Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE, Material.STONE_PICKAXE, Material.WOODEN_PICKAXE
        ),
        "axe" to listOf(
            Material.NETHERITE_AXE, Material.DIAMOND_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.STONE_AXE, Material.WOODEN_AXE
        ),
        "shovel" to listOf(
            Material.NETHERITE_SHOVEL, Material.DIAMOND_SHOVEL, Material.IRON_SHOVEL,
            Material.GOLDEN_SHOVEL, Material.STONE_SHOVEL, Material.WOODEN_SHOVEL
        ),
        "hoe" to listOf(
            Material.NETHERITE_HOE, Material.DIAMOND_HOE, Material.IRON_HOE,
            Material.GOLDEN_HOE, Material.STONE_HOE, Material.WOODEN_HOE
        ),
        "sword" to listOf(
            Material.NETHERITE_SWORD, Material.DIAMOND_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.STONE_SWORD, Material.WOODEN_SWORD
        ),
        "helmet" to listOf(
            Material.NETHERITE_HELMET, Material.DIAMOND_HELMET, Material.IRON_HELMET,
            Material.GOLDEN_HELMET, Material.CHAINMAIL_HELMET, Material.LEATHER_HELMET, Material.TURTLE_HELMET
        ),
        "chestplate" to listOf(
            Material.NETHERITE_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.IRON_CHESTPLATE,
            Material.GOLDEN_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.LEATHER_CHESTPLATE, Material.ELYTRA
        ),
        "leggings" to listOf(
            Material.NETHERITE_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.IRON_LEGGINGS,
            Material.GOLDEN_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.LEATHER_LEGGINGS
        ),
        "boots" to listOf(
            Material.NETHERITE_BOOTS, Material.DIAMOND_BOOTS, Material.IRON_BOOTS,
            Material.GOLDEN_BOOTS, Material.CHAINMAIL_BOOTS, Material.LEATHER_BOOTS
        ),
        "bow" to listOf(Material.BOW),
        "crossbow" to listOf(Material.CROSSBOW),
        "trident" to listOf(Material.TRIDENT),
        "shield" to listOf(Material.SHIELD),
        "fishing_rod" to listOf(Material.FISHING_ROD),
        "shears" to listOf(Material.SHEARS),
        "flint_and_steel" to listOf(Material.FLINT_AND_STEEL)
    )

    // 材料到分组的反向映射
    private val materialToGroup: Map<Material, List<Material>> = toolGroups.values
        .flatMap { group -> group.map { it to group } }
        .toMap()

    private val toolMaterials = toolGroups.values.flatten().toSet()

    @SubscribeEvent(priority = EventPriority.MONITOR)
    fun onPlayerItemBreak(event: PlayerItemBreakEvent) {
        val player = event.player
        val brokenItem = event.brokenItem
        val brokenMaterial = brokenItem.type

        val permission = Zephyrion.permissions.getString("auto-replace-tool") ?: "zephyrion.auto-replace-tool"
        if (!player.hasPermission(permission) && !ZephyrionAPI.isPluginAdmin(player)) {
            return
        }

        if (!toolMaterials.contains(brokenMaterial)) {
            return
        }

        submit(delay = 1L) {
            tryReplaceItem(player, brokenMaterial)
        }
    }

    private fun tryReplaceItem(player: Player, brokenMaterial: Material) {
        val workspaces = ZephyrionAPI.getJoinedWorkspaces(player)
        if (workspaces.isEmpty()) {
            return
        }

        // 获取同类型工具的材料集合
        val toolGroup = materialToGroup[brokenMaterial]?.toSet() ?: setOf(brokenMaterial)

        // 收集所有仓库中匹配的物品
        val allItems = mutableListOf<Pair<Item, Vault>>()
        for (workspace in workspaces) {
            val vaults = ZephyrionAPI.getVaults(workspace)
            for (vault in vaults) {
                val items = Item.getItemsByMaterials(vault, toolGroup, player)
                items.forEach { allItems.add(it to vault) }
            }
        }

        if (allItems.isEmpty()) {
            player.sendLang("auto-replace-tool-no-replacement", brokenMaterial.name)
            return
        }

        // 已按slot位置排序，取第一个
        val (replacementItem, vault) = allItems.first()
        val itemStack = replacementItem.itemStack

        ZephyrionAPI.removeItem(vault, replacementItem.page, replacementItem.slot, player)
        player.inventory.setItemInMainHand(itemStack)
        player.sendLang("auto-replace-tool-replaced", itemStack.getName(), vault.name)
    }
}
