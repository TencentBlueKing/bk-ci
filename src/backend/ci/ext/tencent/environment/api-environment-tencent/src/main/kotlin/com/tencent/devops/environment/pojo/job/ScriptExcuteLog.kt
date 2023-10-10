package com.tencent.devops.environment.pojo.job

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("脚本执行任务日志")
data class ScriptExcuteLog(
    @ApiModelProperty(value = "云区域ID")
    @JsonProperty("bk_cloud_id")
    val bkCloudId: Long?,
    @ApiModelProperty(value = "IP地址")
    val ip: String?,
    @ApiModelProperty(value = "主机ID")
    @JsonProperty("host_id")
    val bkHostId: Long?,
    @ApiModelProperty(value = "脚本执行日志内容", required = true)
    @JsonProperty("log_content")
    val logContent: String
)