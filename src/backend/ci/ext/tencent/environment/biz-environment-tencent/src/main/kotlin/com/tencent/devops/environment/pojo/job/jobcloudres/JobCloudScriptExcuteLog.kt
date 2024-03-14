package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "脚本执行任务日志")
data class JobCloudScriptExcuteLog(
    @get:Schema(title = "云区域ID")
    @JsonProperty("bk_cloud_id")
    val bkCloudId: Long?,
    @get:Schema(title = "IP地址")
    val ip: String?,
    @get:Schema(title = "主机ID")
    @JsonProperty("host_id")
    val bkHostId: Long?,
    @get:Schema(title = "ipv6地址")
    val ipv6: String?,
    @get:Schema(title = "脚本执行日志内容", required = true)
    @JsonProperty("log_content")
    val logContent: String
)