package org.laolittle.plugin

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.getGroupOrNull

object AuthCommand : CompositeCommand(
    IlliteracyAuth, "auth",
    description = "入群验证管理"
) {

    @SubCommand("add")
    suspend fun CommandSender.add(group: Long? = getGroupOrNull()?.id) {
        when (group?.let { AuthPluginData.enabledGroups.add(it) }) {
            true -> sendMessage("添加成功")
            false -> sendMessage("目标群已开启验证, 无需重复开启")
            null -> sendMessage("请指定群号")
        }
    }

    @SubCommand("remove", "rm")
    suspend fun CommandSender.remove(group: Long? = getGroupOrNull()?.id) {
        when (group?.let { AuthPluginData.enabledGroups.remove(it) }) {
            true -> sendMessage("删除成功")
            false -> sendMessage("目标群未开启验证")
            null -> sendMessage("请指定群号")
        }
    }
}