package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "文件分发任务日志")
data class JobCloudFileDistributeLog(
    @get:Schema(title = "云区域ID")
    @JsonProperty("bk_cloud_id")
    val bkCloudId: Long?,
    @get:Schema(title = "IP地址")
    val ip: String?,
    @get:Schema(title = "主机ID")
    @JsonProperty("bk_host_id")
    val bkHostId: Long?,
    @get:Schema(title = "文件分发日志内容", required = true)
    @JsonProperty("file_logs")
    val jobCloudFileLogList: List<JobCloudFileLog>
)