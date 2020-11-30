package com.tencent.devops.dispatch.docker.pojo

data class FormatLog (
    val logType: String,
    val logMessageMap: Map<String, String>
)
