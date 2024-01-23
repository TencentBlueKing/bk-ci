package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("批量查询日志的结果")
@JsonIgnoreProperties(ignoreUnknown = true)
data class JobCloudQueryJobInstanceLogsResult(
    @ApiModelProperty(value = "作业实例ID", required = true)
    @JsonProperty("job_instance_id")
    val jobInstanceId: Long,
    @ApiModelProperty(value = "步骤实例ID", required = true)
    @JsonProperty("step_instance_id")
    val stepInstanceId: Long,
    @ApiModelProperty(value = "日志类型", notes = "1-脚本执行任务日志，2-文件分发任务日志", required = true)
    @JsonProperty("log_type")
    val logType: Int,
    @ApiModelProperty(value = "脚本执行任务日志", allowEmptyValue = true)
    @JsonProperty("script_task_logs")
    val scriptTaskLogs: List<JobCloudScriptExcuteLog>? = null,
    @ApiModelProperty(value = "文件分发任务日志", allowEmptyValue = true)
    @JsonProperty("file_task_logs")
    val fileTaskLogs: List<JobCloudFileDistributeLog>? = null
) {
    constructor() : this(-1, -1, -1, null, null)
}