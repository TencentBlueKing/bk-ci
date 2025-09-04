package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "执行脚本的结果")
data class ScriptExecuteResult(
    @get:Schema(title = "作业实例ID", required = true)
    val jobInstanceId: Long,
    @get:Schema(title = "作业实例名称", required = true)
    val jobInstanceName: String,
    @get:Schema(title = "步骤实例ID", required = true)
    val stepInstanceId: Long
)