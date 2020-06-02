package com.tencent.bk.codecc.quartz.pojo

data class JobInfo(
    val className: String,
    val jobName: String,
    val triggerName: String,
    val cronExpression: String,
    val jobParam: MutableMap<String, Any>
)