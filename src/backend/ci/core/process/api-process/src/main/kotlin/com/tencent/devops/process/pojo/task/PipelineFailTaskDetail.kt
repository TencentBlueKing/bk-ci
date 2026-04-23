package com.tencent.devops.process.pojo.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线失败任务详情")
data class PipelineFailTaskDetail(
    @get:Schema(title = "step_id", required = true)
    val stepId: String,
    @get:Schema(title = "task_id", required = true)
    val taskId: String,
    @get:Schema(title = "task_name", required = false)
    val taskName: String? = null,
    @get:Schema(title = "job_id", required = true)
    val jobId: String,
    @get:Schema(title = "job_name", required = false)
    val jobName: String? = null,
    @get:Schema(title = "stage_name", required = false)
    val stageName: String? = null,
    @get:Schema(title = "error_msg", required = false)
    val errorMsg: String? = null
)
