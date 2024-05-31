package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "批量查询日志的结果")
data class JobCloudQueryJobInstanceLogsResult(
    @get:Schema(title = "作业实例ID", required = true)
    @JsonProperty("job_instance_id")
    val jobInstanceId: Long,
    @get:Schema(title = "步骤实例ID", required = true)
    @JsonProperty("step_instance_id")
    val stepInstanceId: Long,
    @get:Schema(title = "日志类型", description = "1-脚本执行任务日志，2-文件分发任务日志", required = true)
    @JsonProperty("log_type")
    val logType: Int,
    @get:Schema(title = "脚本执行任务日志")
    @JsonProperty("script_task_logs")
    val scriptTaskLogs: List<JobCloudScriptExcuteLog>? = null,
    @get:Schema(title = "文件分发任务日志")
    @JsonProperty("file_task_logs")
    val fileTaskLogs: List<JobCloudFileDistributeLog>? = null
) {
    constructor() : this(-1, -1, -1, null, null)
}