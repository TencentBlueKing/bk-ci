package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("批量查询日志的结果")
data class QueryJobInstanceLogsResult(
    @ApiModelProperty(value = "IP信息", required = true)
    val hostInfo: HostInfo,
    @ApiModelProperty(value = "日志类型", notes = "1-脚本执行任务日志，2-文件分发任务日志", required = true)
    val logType: Int,
    @ApiModelProperty(value = "脚本执行任务日志", allowEmptyValue = true)
    val scriptTaskLogs: List<ScriptExcuteLog>? = null,
    @ApiModelProperty(value = "文件分发任务日志", allowEmptyValue = true)
    val fileTaskLogs: List<FileDistributeLog>? = null
)