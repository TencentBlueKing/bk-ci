package com.tencent.devops.environment.pojo.job.jobreq

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "终止任务的信息")
data class TaskTerminateReq(
    @get:Schema(title = "作业实例ID", required = true)
    val jobInstanceId: Long,
    @get:Schema(title = "操作类型", description = "1 - 终止作业(也是默认)")
    val operationCode: Int = 1
)