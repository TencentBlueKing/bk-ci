package com.tencent.devops.environment.pojo.job.resp

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("脚本执行任务日志")
data class ScriptExcuteLog(
    @ApiModelProperty(value = "云区域ID")
    val bkCloudId: Long?,
    @ApiModelProperty(value = "IP地址")
    val ip: String?,
    @ApiModelProperty(value = "主机ID")
    val bkHostId: Long?,
    @ApiModelProperty(value = "ipv6地址")
    val ipv6: String?,
    @ApiModelProperty(value = "脚本执行日志内容", required = true)
    val logContent: String
)