package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class JobCloudFileStepInfo(
    @get:Schema(title = "源文件列表")
    @JsonProperty("file_source_list")
    val jobCloudFileSourceList: List<JobCloudFileSource>,
    @get:Schema(title = "目标信息")
    @JsonProperty("file_destination")
    val fileDestination: JobCloudFileDestination,
    @get:Schema(title = "超时时间", description = "单位为秒")
    val timeout: Int,
    @get:Schema(title = "上传文件限速", description = "单位为MB/s，没有值表示不限速")
    @JsonProperty("source_speed_limit")
    val sourceSpeedLimit: Int?,
    @get:Schema(title = "下载文件限速", description = "单位为MB/s，没有值表示不限速")
    @JsonProperty("destination_speed_limit")
    val destinationSpeedLimit: Int?,
    @get:Schema(title = "传输模式", description = "1 - 严谨模式, 2 - 强制模式, 3 - 安全模式")
    @JsonProperty("transfer_mode")
    val transferMode: Int,
    @get:Schema(title = "是否忽略错误", description = "0-不忽略，1-忽略")
    @JsonProperty("is_ignore_error")
    val isIgnoreError: Int
)