package com.tencent.devops.process.pojo.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线失败任务详情")
data class PipelineFailTaskDetail(
    @get:Schema(title = "step_id", required = true)
    val stepId: String,
    @get:Schema(title = "task_id", required = true)
    val taskId: String,
    @get:Schema(title = "task名称", required = false)
    val taskName: String? = null,
    @get:Schema(title = "job_id", required = true)
    val jobId: String,
    @get:Schema(title = "job名称", required = false)
    val jobName: String? = null,
    @get:Schema(title = "stage名称", required = false)
    val stageName: String? = null,
    @get:Schema(title = "失败信息", required = false)
    val errorMsg: String? = null,
    @get:Schema(title = "是否为矩阵插件", required = false)
    val matrixFlag: Boolean? = false
)
