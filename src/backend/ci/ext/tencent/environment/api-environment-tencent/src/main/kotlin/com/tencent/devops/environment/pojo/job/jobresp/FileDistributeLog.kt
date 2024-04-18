package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "文件分发任务日志")
data class FileDistributeLog(
    @get:Schema(title = "云区域ID")
    val bkCloudId: Long?,
    @get:Schema(title = "IP地址")
    val ip: String?,
    @get:Schema(title = "主机ID")
    val bkHostId: Long?,
    @get:Schema(title = "文件分发日志内容", required = true)
    val fileLogList: List<FileLog>
)