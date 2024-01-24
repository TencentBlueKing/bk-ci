package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class JobCloudFileStepInfo(
    @ApiModelProperty(value = "源文件列表")
    @JsonProperty("file_source_list")
    val jobCloudFileSourceList: List<JobCloudFileSource>,
    @ApiModelProperty(value = "目标信息")
    @JsonProperty("file_destination")
    val fileDestination: JobCloudFileDestination,
    @ApiModelProperty(value = "超时时间", notes = "单位为秒")
    val timeout: Int,
    @ApiModelProperty(value = "上传文件限速", notes = "单位为MB/s，没有值表示不限速")
    @JsonProperty("source_speed_limit")
    val sourceSpeedLimit: Int?,
    @ApiModelProperty(value = "下载文件限速", notes = "单位为MB/s，没有值表示不限速")
    @JsonProperty("destination_speed_limit")
    val destinationSpeedLimit: Int?,
    @ApiModelProperty(value = "传输模式", notes = "1 - 严谨模式, 2 - 强制模式, 3 - 安全模式")
    @JsonProperty("transfer_mode")
    val transferMode: Int,
    @ApiModelProperty(value = "是否忽略错误", notes = "0-不忽略，1-忽略")
    @JsonProperty("is_ignore_error")
    val isIgnoreError: Int
)