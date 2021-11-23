package com.tencent.devops.stream.trigger.pojo

data class TriggerResult(
    val trigger: Boolean,
    val timeTrigger: Boolean,
    val deleteTrigger: Boolean
)
