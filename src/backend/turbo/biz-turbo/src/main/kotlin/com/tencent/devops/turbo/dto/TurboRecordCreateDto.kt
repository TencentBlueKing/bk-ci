package com.tencent.devops.turbo.dto

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.util.constants.QUEUE_TURBO_REPORT_CREATE

@Event(destination = QUEUE_TURBO_REPORT_CREATE)
data class TurboRecordCreateDto(
    val engineCode: String,
    val dataMap: Map<String, Any?>
): IEvent()
