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
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.content
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
            priority = EventPriority.LOW
        ) {
            if (groupId !in AuthPluginData.enabledGroups) return@subscribeAlways

            if (Bot.instances.all { it.id != member.id }) {
                val bar = Regex("""[。.${if (Random.nextInt(100) > 49) "；;" else ""}！!？?“”"]+""")
                val question =
                    AuthText.texts.random()
                        .split(bar)
                        .filter { it.contains(usefulRegex) }.random()
                val answers = question.split(usefulRegex)
                group.sendMessage(At(member) + PlainText("欢迎来到${group.name}，为保障良好的聊天环境，请在180秒内为以下句子断句。"))
                delay(1000)
                group.sendMessage(question.replace(usefulRegex, ""))

                val messageChannel = Channel<MessageChain>()
                val messageListener =
                    globalEventChannel().filterIsInstance<GroupMessageEvent>().filter { it.sender.id == member.id }
                        .subscribeGroupMessages {
                            always { messageChannel.send(message) }
                        }

                val leaveListener =
                    globalEventChannel().filterIsInstance<MemberLeaveEvent>().filter { it.member.id == member.id }
                        .subscribeOnce<MemberLeaveEvent> {
                            QuitEvent(member).broadcast()
                        }

                val timeout = launch {
                    delay(180_000)
                    group.sendMessage("您已超时, 请重新加群")
                    delay(1_000)
                    member.kick("超时未验证")
                    QuitEvent(member).broadcast()
                }

                globalEventChannel().subscribeAlways<QuitEvent> Quit@{
                    if (this@Quit.member == this@subscribeAlways.member) {
                        messageListener.complete()
                        leaveListener.complete()
                        timeout.cancel()
                        messageChannel.close()
                    }
                }

                var times = 0
                for (msg in messageChannel) {
                    var acc = 0.0
                    var auth = msg.content
                    val lastIndex = answers.size - 1

                    for (index in 0..lastIndex) {
                        val regex = (if (index < lastIndex) "${answers[index]}$usefulRegex"
                        else "$usefulRegex${answers[index]}").toRegex()
                        kotlin.runCatching {
                            if (auth.contains(regex)) {
                                auth = auth.replace(regex, "")
                                acc++
                            } else if (index == lastIndex && auth.substring(
                                    0,
                                    auth.indexOf(answers[index].last()) + 1
                                ) == answers[index]
                            ) {
                                auth = ""
                                acc++
                            }
                            Unit
                        }.onFailure { acc -= 0.8 }
                    }
                    var num = 0

                    auth.forEach {
                        if (usefulRegex.containsMatchIn(it.toString())) {
                            num++
                            if (num >= if (answers.size <= 4) answers.size - 2 else 3) acc -= 0.4
                        }
                    }

                    times++
                    val foo = acc / (answers.size - 1)

                    val result = String.format("%.2f", if (foo < 1) foo * 100 else 100.0)
                    if (foo > 0.8) {
                        group.sendMessage("您的分数为${result}, 已通过验证! ")
                        QuitEvent(member).broadcast()
                        break
                    } else {
                        if (times >= 5) {
                            group.sendMessage(PlainText("您的分数为$result, 未通过验证, 请重新加群") + msg.quote())
                            member.kick("未通过验证")
                            QuitEvent(member).broadcast()
                            break
                        } else group.sendMessage(PlainText("您的分数为$result, 未通过验证, 还有${5 - times}次机会") + msg.quote())
                    }
                }
            }
        }
    }
}
