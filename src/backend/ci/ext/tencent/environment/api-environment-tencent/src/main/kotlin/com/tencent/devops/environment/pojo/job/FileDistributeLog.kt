package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("文件分发任务日志")
data class FileDistributeLog(
    @ApiModelProperty(value = "IP信息", required = true)
    val hostInfo: HostInfo,
    @ApiModelProperty(value = "文件分发日志内容", required = true)
    val fileLogList: List<FileLog>
)