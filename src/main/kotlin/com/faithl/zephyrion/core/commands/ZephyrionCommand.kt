package com.faithl.zephyrion.core.commands

import com.faithl.zephyrion.Zephyrion
import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.core.ui.vault.ListVaults
import com.faithl.zephyrion.core.ui.vault.VaultUI
import com.faithl.zephyrion.core.ui.workspace.ListWorkspaces
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.*
import taboolib.common.platform.function.onlinePlayers
import taboolib.expansion.createHelper
import taboolib.module.lang.sendLang
import taboolib.platform.util.asLangText
import taboolib.platform.util.sendLang

@CommandHeader(name = "zephyrion", aliases = ["ze"], permission = "zephyrion.command", permissionDefault = PermissionDefault.TRUE)
object ZephyrionCommand {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }


    @CommandBody
    val help = subCommand {
        createHelper()
    }

    @CommandBody(permission = "zephyrion.command.reload")
    val reload = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendLang("plugin-reload")
        }
    }

    @CommandBody(permission = "zephyrion.command.open", permissionDefault = PermissionDefault.TRUE)
    val open = subCommand {
        execute<Player> { sender, _, _ ->
            ListWorkspaces(sender).open()
        }
        dynamic(comment = "player", permission = Zephyrion.permissions.getString("command.open-other","zephyrion.command.open.other")!!) {
            suggestion<CommandSender> { _, _ ->
                onlinePlayers().map { it.name }
            }
            execute<Player> { sender, _, argument ->
                val targetPlayer = Bukkit.getPlayer(argument)
                if (targetPlayer != null) {
                    ListWorkspaces(sender, targetPlayer).open()
                } else {
                    sender.sendLang("command-player-offline")
                }
            }
            dynamic(comment = "workspace", optional = true) {
                suggestion<CommandSender> { _, ctx ->
                    val targetPlayer = Bukkit.getPlayer(ctx["player"])
                    if (targetPlayer != null) {
                        ZephyrionAPI.getJoinedWorkspaces(targetPlayer).map { it.name }
                    } else {
                        listOf()
                    }
                }
                execute<Player> { sender, ctx, argument ->
                    val targetPlayer = Bukkit.getPlayer(ctx["player"])
                    if (targetPlayer != null) {
                        val workspace = ZephyrionAPI.getWorkspace(targetPlayer.uniqueId.toString(), argument)
                        if (workspace != null) {
                            ListVaults(sender, workspace).open()
                        } else {
                            sender.sendLang("command-workspace-not-exist")
                        }
                    } else {
                        sender.sendLang("command-player-offline")
                    }
                }
                dynamic(comment = "vault", optional = true) {
                    suggestion<CommandSender> { _, ctx ->
                        val targetPlayer = Bukkit.getPlayer(ctx["player"])
                        if (targetPlayer != null) {
                            ZephyrionAPI.getWorkspace(targetPlayer.uniqueId.toString(), ctx["workspace"])?.let {
                                ZephyrionAPI.getVaults(it).map { it.name }
                            } ?: listOf()
                        } else {
                            listOf()
                        }
                    }
                    execute<Player> { sender, ctx, argument ->
                        val targetPlayer = Bukkit.getPlayer(ctx["player"])
                        if (targetPlayer != null) {
                            val workspace = ZephyrionAPI.getWorkspace(targetPlayer.uniqueId.toString(), ctx["workspace"])
                            if (workspace != null) {
                                val vault = ZephyrionAPI.getVaults(workspace).find { it.name == argument }
                                if (vault != null) {
                                    VaultUI(sender, vault).open()
                                } else {
                                    sender.sendLang("command-vault-not-exist")
                                }
                            } else {
                                sender.sendLang("command-workspace-not-exist")
                            }
                        } else {
                            sender.sendLang("command-player-offline")
                        }
                    }
                }
            }
        }
    }

    @CommandBody(permission = "zephyrion.command.quota", permissionDefault = PermissionDefault.TRUE)
    val quota = subCommand {
        execute<Player> { sender, _, _ ->
            val quota = ZephyrionAPI.getUserData(sender.uniqueId.toString())
            sender.sendLang("quota-header-self")
            sender.sendLang("quota-workspace", quota.workspaceUsed, quota.workspaceQuotas)
            sender.sendLang("quota-size", quota.sizeUsed, quota.sizeQuotas)
            val unlimitedText = sender.asLangText(if (quota.unlimited) "quota-unlimited-yes" else "quota-unlimited-no")
            sender.sendLang("quota-unlimited", unlimitedText)
        }

        dynamic(comment = "player", permission = Zephyrion.permissions.getString("command.quota-other","zephyrion.command.quota.other")!!) {
            suggestion<CommandSender> { _, _ ->
                onlinePlayers().map { it.name }
            }
            execute<CommandSender> { sender, ctx, argument ->
                val target = Bukkit.getPlayer(ctx["player"]) ?: return@execute
                val quota = ZephyrionAPI.getUserData(target.uniqueId.toString())
                sender.sendLang("quota-header-other", target.name)
                sender.sendLang("quota-workspace", quota.workspaceUsed, quota.workspaceQuotas)
                sender.sendLang("quota-size", quota.sizeUsed, quota.sizeQuotas)
                val unlimitedText = (sender as? Player)?.asLangText(if (quota.unlimited) "quota-unlimited-yes" else "quota-unlimited-no")
                    ?: if (quota.unlimited) "Yes" else "No"
                sender.sendLang("quota-unlimited", unlimitedText)
            }

            literal("set", permission = Zephyrion.permissions.getString("command.quota-edit","zephyrion.command.quota.edit")!!) {
                dynamic(comment = "type") {
                    suggestion<CommandSender> { _, _ ->
                        listOf("workspace", "size", "unlimited")
                    }
                    dynamic(comment = "value") {
                        suggestion<CommandSender> { _, ctx ->
                            if (ctx["type"] == "unlimited") {
                                listOf("true", "false")
                            } else {
                                listOf()
                            }
                        }
                        execute<CommandSender> { sender, context, argument ->
                            val target = Bukkit.getPlayer(context["player"]) ?: return@execute
                            val type = context["type"]

                            val success = when (type) {
                                "workspace" -> {
                                    val amount = argument.toIntOrNull()
                                    if (amount == null || amount < 0) {
                                        sender.sendLang("quota-set-invalid-amount")
                                        return@execute
                                    }
                                    ZephyrionAPI.setWorkspaceQuota(target.uniqueId.toString(), amount)
                                }
                                "size" -> {
                                    val amount = argument.toIntOrNull()
                                    if (amount == null || amount < 0) {
                                        sender.sendLang("quota-set-invalid-amount")
                                        return@execute
                                    }
                                    ZephyrionAPI.setSizeQuota(target.uniqueId.toString(), amount)
                                }
                                "unlimited" -> {
                                    val value = argument.lowercase() == "true"
                                    ZephyrionAPI.setUnlimited(target.uniqueId.toString(), value)
                                }
                                else -> {
                                    sender.sendLang("quota-set-invalid-type")
                                    return@execute
                                }
                            }

                            if (success) {
                                val displayValue = if (type == "unlimited") {
                                    if (argument.lowercase() == "true") "开启" else "关闭"
                                } else argument
                                sender.sendLang("quota-set-succeed", target.name, type, displayValue)
                            } else {
                                sender.sendLang("quota-set-failed")
                            }
                        }
                    }
                }
            }

            literal("add", permission = Zephyrion.permissions.getString("command.quota-edit","zephyrion.command.quota.edit")!!) {
                dynamic(comment = "type") {
                    suggestion<CommandSender> { _, _ ->
                        listOf("workspace", "size")
                    }
                    dynamic(comment = "amount") {
                        execute<CommandSender> { sender, context, argument ->
                            val target = Bukkit.getPlayer(context["player"]) ?: return@execute
                            val type = context["type"]
                            val amount = argument.toIntOrNull()

                            if (amount == null || amount <= 0) {
                                sender.sendLang("quota-add-invalid-amount")
                                return@execute
                            }

                            val success = when (type) {
                                "workspace" -> ZephyrionAPI.addWorkspaceQuota(target.uniqueId.toString(), amount)
                                "size" -> ZephyrionAPI.addSizeQuota(target.uniqueId.toString(), amount)
                                else -> {
                                    sender.sendLang("quota-add-invalid-type")
                                    return@execute
                                }
                            }

                            if (success) {
                                sender.sendLang("quota-add-succeed", target.name, amount, type)
                            } else {
                                sender.sendLang("quota-add-failed")
                            }
                        }
                    }
                }
            }

            literal("remove", permission = Zephyrion.permissions.getString("command.quota-edit","zephyrion.command.quota.edit")!!) {
                dynamic(comment = "type") {
                    suggestion<CommandSender> { _, _ ->
                        listOf("workspace", "size")
                    }
                    dynamic(comment = "amount") {
                        execute<CommandSender> { sender, context, argument ->
                            val target = Bukkit.getPlayer(context["player"]) ?: return@execute
                            val type = context["type"]
                            val amount = argument.toIntOrNull()

                            if (amount == null || amount <= 0) {
                                sender.sendLang("quota-remove-invalid-amount")
                                return@execute
                            }


                            val success = when (type) {
                                "workspace" -> ZephyrionAPI.removeWorkspaceQuota(target.uniqueId.toString(), amount)
                                "size" -> ZephyrionAPI.removeSizeQuota(target.uniqueId.toString(), amount)
                                else -> {
                                    sender.sendLang("quota-remove-invalid-type")
                                    return@execute
                                }
                            }

                            if (success) {
                                sender.sendLang("quota-remove-succeed", target.name, amount, type)
                            } else {
                                sender.sendLang("quota-remove-failed")
                            }
                        }
                    }
                }
            }

            literal("reset", permission = Zephyrion.permissions.getString("command.quota-edit","zephyrion.command.quota.edit")!!) {
                execute<CommandSender> { sender, context, _ ->
                    val target = Bukkit.getPlayer(context["player"]) ?: return@execute
                    val success = ZephyrionAPI.resetQuota(target.uniqueId.toString())

                    if (success) {
                        sender.sendLang("quota-reset-succeed", target.name)
                    } else {
                        sender.sendLang("quota-reset-failed")
                    }
                }
            }
        }
    }

    @CommandBody
    val bind = subCommand {

    }

    @CommandBody
    val unbind = subCommand {

    }

}