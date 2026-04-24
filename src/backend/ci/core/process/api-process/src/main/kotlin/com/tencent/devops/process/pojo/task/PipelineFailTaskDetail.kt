package com.tencent.devops.process.pojo.task

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线失败任务详情")
data class PipelineFailTaskDetail(
    @get:Schema(title = "step_id", required = true)
    @JsonProperty("step_id")
    val stepId: String,
    @JsonProperty("task_id")
    @get:Schema(title = "task_id", required = true)
    val taskId: String,
    @JsonProperty("task_name")
    @get:Schema(title = "task_name", required = false)
    val taskName: String? = null,
    @JsonProperty("job_id")
    @get:Schema(title = "job_id", required = true)
    val jobId: String,
    @JsonProperty("job_name")
    @get:Schema(title = "job_name", required = false)
    val jobName: String? = null,
    @JsonProperty("stage_name")
    @get:Schema(title = "stage_name", required = false)
    val stageName: String? = null,
    @JsonProperty("error_msg")
    @get:Schema(title = "error_msg", required = false)
    val errorMsg: String? = null
)
