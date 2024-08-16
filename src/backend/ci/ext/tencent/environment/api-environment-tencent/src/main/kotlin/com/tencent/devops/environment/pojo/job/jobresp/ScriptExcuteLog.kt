package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "脚本执行任务日志")
data class ScriptExcuteLog(
    @get:Schema(title = "云区域ID")
    val bkCloudId: Long?,
    @get:Schema(title = "IP地址")
    val ip: String?,
    @get:Schema(title = "主机ID")
    val bkHostId: Long?,
    @get:Schema(title = "ipv6地址")
    val ipv6: String?,
    @get:Schema(title = "脚本执行日志内容", required = true)
    val logContent: String
)