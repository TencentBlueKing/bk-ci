package com.tencent.devops.turbo.dto

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.util.constants.QUEUE_TURBO_REPORT_UPDATE

@Event(destination = QUEUE_TURBO_REPORT_UPDATE)
data class TurboRecordUpdateDto(
    val engineCode: String,
    val tbsTurboRecordId: String?,
    val buildId: String?,
    val turboPlanId: String?
) : IEvent()
