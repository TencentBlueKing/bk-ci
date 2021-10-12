package com.tencent.devops.process.notify.command

data class ExecutionVariables(
    val pipelineVersion: Int?,
    val buildNum: Int?,
    val trigger: String,
    val originTriggerType: String,
    val user: String,
    val isMobileStart: Boolean
)
