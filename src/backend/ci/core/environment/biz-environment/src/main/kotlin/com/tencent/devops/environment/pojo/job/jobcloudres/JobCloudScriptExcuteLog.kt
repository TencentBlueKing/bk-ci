package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("脚本执行任务日志")
data class JobCloudScriptExcuteLog(
    @ApiModelProperty(value = "云区域ID")
    @JsonProperty("bk_cloud_id")
    val bkCloudId: Long?,
    @ApiModelProperty(value = "IP地址")
    val ip: String?,
    @ApiModelProperty(value = "主机ID")
    @JsonProperty("host_id")
    val bkHostId: Long?,
    @ApiModelProperty(value = "ipv6地址")
    val ipv6: String?,
    @ApiModelProperty(value = "脚本执行日志内容", required = true)
    @JsonProperty("log_content")
    val logContent: String
)