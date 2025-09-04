package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "批量查询日志的结果")
data class QueryJobInstanceLogsResult(
    @get:Schema(title = "作业实例ID", required = true)
    val jobInstanceId: Long,
    @get:Schema(title = "步骤实例ID", required = true)
    val stepInstanceId: Long,
    @get:Schema(title = "日志类型", description = "1-脚本执行任务日志，2-文件分发任务日志", required = true)
    val logType: Int,
    @get:Schema(title = "脚本执行任务日志")
    val scriptTaskLogs: List<ScriptExcuteLog>? = null,
    @get:Schema(title = "文件分发任务日志")
    val fileTaskLogs: List<FileDistributeLog>? = null
)