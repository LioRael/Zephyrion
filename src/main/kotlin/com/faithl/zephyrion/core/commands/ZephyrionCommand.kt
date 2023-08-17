package com.faithl.zephyrion.core.commands

import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.core.ui.vault.ListVaults
import com.faithl.zephyrion.core.ui.vault.VaultUI
import com.faithl.zephyrion.core.ui.workspace.ListWorkspaces
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.onlinePlayers
import taboolib.expansion.createHelper
import taboolib.module.lang.sendLang

@CommandHeader(name = "zephyrion", aliases = ["ze"], permission = "zephyrion.command")
object ZephyrionCommand {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }


    @CommandBody
    val help = subCommand {
        createHelper()
    }

    @CommandBody
    val reload = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendLang("plugin-reload")
        }
    }

    @CommandBody
    val open = subCommand {
        execute<Player> { sender, _, _ ->
            ListWorkspaces(sender).open(sender)
        }
        dynamic(comment = "owner") {
            suggestion<CommandSender> { _, _ ->
                onlinePlayers().map { it.name }
            }
            execute<Player> { sender, _, argument ->
                val owner = Bukkit.getPlayer(argument)
                if (owner != null) {
                    ListWorkspaces(owner).open(sender)
                } else {
                    sender.sendMessage("§c玩家不在线")
                }
            }
            dynamic(comment = "workspace", optional = true) {
                suggestion<CommandSender> { _, ctx ->
                    val owner = Bukkit.getPlayer(ctx["owner"])
                    if (owner != null) {
                        ZephyrionAPI.getJoinedWorkspaces(owner).map { it.name }
                    } else {
                        listOf()
                    }
                }
                execute<Player> { sender, ctx, argument ->
                    val owner = Bukkit.getPlayer(ctx["owner"])
                    if (owner != null) {
                        val workspace = ZephyrionAPI.getWorkspace(owner.uniqueId.toString(), argument)
                        if (workspace != null) {
                            ListVaults(owner, workspace).open(sender)
                        } else {
                            sender.sendMessage("§c工作空间不存在")
                        }
                    } else {
                        sender.sendMessage("§c玩家不在线")
                    }
                }
                dynamic(comment = "vault", optional = true) {
                    suggestion<CommandSender> { _, ctx ->
                        val owner = Bukkit.getPlayer(ctx["owner"])
                        if (owner != null) {
                            ZephyrionAPI.getWorkspace(owner.uniqueId.toString(), ctx["workspace"])?.let {
                                ZephyrionAPI.getVaults(it).map { it.name }
                            } ?: listOf()
                        } else {
                            listOf()
                        }
                    }
                    execute<Player> { sender, ctx, argument ->
                        val owner = Bukkit.getPlayer(ctx["owner"])
                        if (owner != null) {
                            val workspace = ZephyrionAPI.getWorkspace(owner.uniqueId.toString(), ctx["workspace"])
                            if (workspace != null) {
                                val vault = ZephyrionAPI.getVaults(workspace).find { it.name == argument }
                                if (vault != null) {
                                    VaultUI(owner, vault).open(sender)
                                } else {
                                    sender.sendMessage("§c仓库不存在")
                                }
                            } else {
                                sender.sendMessage("§c工作空间不存在")
                            }
                        } else {
                            sender.sendMessage("§c玩家不在线")
                        }
                    }
                }
            }
        }
    }

    @CommandBody
    val quota = subCommand {
//        execute<Player> { sender, _, _ ->
//            sender.sendMessage("§c你的配额: ${ZephyrionAPI.getQuota(sender)}")
//        }
//        dynamic(comment = "owner") {
//            suggestion<CommandSender> { _, _ ->
//                onlinePlayers().map { it.name }
//            }
//            execute<Player> { sender, _, argument ->
//                val owner = Bukkit.getPlayer(argument)
//                if (owner != null) {
//                    sender.sendMessage("§c${owner.name}的配额: ${ZephyrionAPI.getQuota(owner)}")
//                } else {
//                    sender.sendMessage("§c玩家不在线")
//                }
//            }
//        }
    }

    @CommandBody
    val bind = subCommand {

    }

    @CommandBody
    val unbind = subCommand {

    }

}