package com.tencent.devops.stream.trigger.parsers.triggerMatch

data class TriggerResult(
    val trigger: Boolean,
    val timeTrigger: Boolean,
    val startParams: Map<String, String>,
    val deleteTrigger: Boolean,
    val repoHookName: List<String>? = null
)
