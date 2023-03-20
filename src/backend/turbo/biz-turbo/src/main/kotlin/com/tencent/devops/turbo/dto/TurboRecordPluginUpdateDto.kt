package com.tencent.devops.turbo.dto

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.util.constants.QUEUE_TURBO_PLUGIN_DATA

@Event(destination = QUEUE_TURBO_PLUGIN_DATA)
data class TurboRecordPluginUpdateDto(
    val buildId: String,
    val user: String,
    override var delayMills: Int
): IEvent(delayMills)
