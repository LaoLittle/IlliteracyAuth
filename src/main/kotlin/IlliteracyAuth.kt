package org.laolittle.plugin

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Contact.Companion.sendImage
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
import net.mamoe.mirai.utils.error
import net.mamoe.mirai.utils.info
import org.laolittle.plugin.joinorquit.AutoConfig
import org.laolittle.plugin.joinorquit.GroupList.enable
import org.laolittle.plugin.model.PatPatTool
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
            if (groupId !in AuthPluginData.enabledGroups) {
                if (!group.enable()) return@subscribeAlways
                delay(1000)
                group.sendMessage(AutoConfig.newMemberJoinMessage.random())
                delay(2547)
                if (AutoConfig.newMemberJoinPat) {
                    runCatching {
                        PatPatTool.getPat(member, 60)
                        group.sendImage(PatPat.dataFolder.resolve("tmp").resolve("${member.id}_pat.gif"))
                    }.onFailure {
                        if (it is ClassNotFoundException) logger.error { "需要前置插件：PatPat, 请前往下载https://mirai.mamoe.net/topic/740" }
                    }
                }
                return@subscribeAlways
            }
            if (Bot.instances.all { it.id != member.id }) {
                val question =
                    AuthText.texts.random()
                        .split(Regex("""[。.${if (Random.nextInt(100) > 49) "；;" else ""}！!？?“”"]+"""))
                        .filter { it.isNotBlank() }.random()
                val answers = question.split(usefulPattern)
                group.sendMessage(At(member) + PlainText("欢迎来到${group.name}，为保障良好的聊天环境，请在180秒内为以下句子断句。"))
                delay(1000)
                group.sendMessage(question.replace(usefulPattern.toRegex(), ""))

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
                        val regex = (if (index < lastIndex) "${answers[index]}$usefulPattern"
                        else "$usefulPattern${answers[index]}").toRegex()
                        if (auth.contains(regex)) {
                            auth = auth.replace(regex, "")
                            acc++
                        } else if (index == lastIndex && auth.replace(
                                Regex("$auth$usefulPattern"),
                                ""
                            ) == answers[index]
                        ) {
                            auth = ""
                            acc++
                        }
                    }
                    var num = 0
                    auth.forEach {
                        if (it in stopSymbol) {
                            if (num >= 1) acc -= 0.4
                            num++
                        }
                    }

                    times++
                    val foo = acc / (answers.size + 1)

                    if (foo > 0.8) {
                        group.sendMessage("您已通过验证! ")
                        run {
                            if (!group.enable()) return@subscribeAlways
                            delay(1000)
                            group.sendMessage(AutoConfig.newMemberJoinMessage.random())
                            delay(2547)
                            if (AutoConfig.newMemberJoinPat) {
                                runCatching {
                                    PatPatTool.getPat(member, 60)
                                    group.sendImage(PatPat.dataFolder.resolve("tmp").resolve("${member.id}_pat.gif"))
                                }.onFailure {
                                    if (it is ClassNotFoundException) logger.error { "需要前置插件：PatPat, 请前往下载https://mirai.mamoe.net/topic/740" }
                                }
                            }
                        }
                        QuitEvent(member).broadcast()
                        break
                    } else {
                        val result = String.format("%.2f", foo * 100)
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
