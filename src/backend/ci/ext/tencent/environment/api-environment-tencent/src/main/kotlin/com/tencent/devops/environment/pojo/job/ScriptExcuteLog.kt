package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("脚本执行任务日志")
data class ScriptExcuteLog(
    @ApiModelProperty(value = "主机信息", required = true)
    val host: Host,
    @ApiModelProperty(value = "脚本执行日志内容", required = true)
    val logContent: String
)