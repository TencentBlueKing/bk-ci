package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "查询任务状态的结果")
data class QueryJobInstanceStatusResult(
    @get:Schema(title = "作业是否结束", required = true)
    val finished: Boolean,
    @get:Schema(title = "作业实例基本信息")
    val jobInstance: JobInstance?,
    @get:Schema(title = "作业步骤列表")
    val stepInstanceList: List<JobStepInstance>?
)