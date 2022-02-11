package org.laolittle.plugin

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.broadcast
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.event.events.MemberLeaveEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.info
import kotlin.random.Random

object IlliteracyAuth : KotlinPlugin(
    JvmPluginDescription(
        id = "org.laolittle.plugin.IlliteracyAuth",
        name = "IlliteracyAuth",
        version = "1.0",
    ) {
        author("LaoLittle")
    }
) {
    override fun onEnable() {
        AuthCommand.register()
        AuthPluginData.reload()
        AuthText.reload()

        logger.info { "Plugin loaded" }

        globalEventChannel().subscribeAlways<MemberJoinEvent>(
            priority = EventPriority.HIGH
        ) {
            if (groupId !in AuthPluginData.enabledGroups) return@subscribeAlways
            if (Bot.instances.all { it.id != member.id }) {
                val question =
                    AuthText.texts.random().split(Regex("[。.${if (Random.nextInt(100) > 50) "；;" else ""}！!？?”\"]+"))
                        .filter { it.isNotBlank() }.random()
                val answers = question.split(usefulRegex)
                group.sendMessage(At(member) + PlainText("欢迎来到${group.name}，为保障良好的聊天环境，请在180秒内为以下句子断句。"))
                delay(1000)
                group.sendMessage(question.replace(usefulRegex, ""))

                val codesChannel = Channel<String>()
                enabledUsers.add(member.id)
                val messageListener =
                    globalEventChannel().filterIsInstance<GroupMessageEvent>().filter { it.sender.id == member.id }
                        .subscribeGroupMessages {
                            always { code -> codesChannel.send(code) }
                        }

                val leaveListener =
                    globalEventChannel().filterIsInstance<MemberLeaveEvent>().filter { it.member.id == member.id }
                        .subscribeOnce<MemberLeaveEvent> {
                            QuitEvent.broadcast()
                        }

                val timeout = launch {
                    delay(180_000)
                    group.sendMessage("您已超时, 请重新加群")
                    delay(1_000)
                    member.kick("超时未验证")
                    QuitEvent.broadcast()
                }

                globalEventChannel().subscribeAlways<QuitEvent> {
                    messageListener.complete()
                    leaveListener.complete()
                    timeout.cancel()
                    codesChannel.close()
                }

                var times = 0
                for (code in codesChannel) {
                    var acc = 0.0
                    code.split(usefulRegex).forEachIndexed { index, str ->
                        if (answers[index] == str) acc++
                    }
                    times++
                    val foo = acc / answers.size
                    if (foo > 0.8) {
                        group.sendMessage("您已通过验证! ")
                        QuitEvent.broadcast()
                        break
                    } else {
                        if (times >= 5) {
                            group.sendMessage("您的分数为${foo * 100}, 未通过验证, 请重新加群")
                            member.kick("未通过验证")
                            QuitEvent.broadcast()
                            break
                        } else group.sendMessage("您的分数为${foo * 100}, 未通过验证, 还有${5 - times}次机会")
                    }
                }
            }
        }
    }
}
