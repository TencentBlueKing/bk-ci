package com.tencent.devops.environment.pojo.job.resp

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("终止任务的结果")
data class TaskTerminateResult(
    @ApiModelProperty(value = "作业实例ID", required = true)
    val jobInstanceId: Long,
    @ApiModelProperty(value = "作业实例名称")
    val jobInstanceName: String?,
    @ApiModelProperty(value = "步骤实例ID")
    val stepInstanceId: Long?
)