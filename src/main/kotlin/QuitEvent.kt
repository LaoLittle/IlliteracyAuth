package org.laolittle.plugin

import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.AbstractEvent

data class QuitEvent(
    val member: Member
) : AbstractEvent()