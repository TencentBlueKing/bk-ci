package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("终止任务的信息")
data class TaskTerminateInfoReq(
    @ApiModelProperty(value = "作业实例ID", required = true)
    val jobInstanceId: Long,
    @ApiModelProperty(value = "操作类型", notes = "1 - 终止作业(也是默认)")
    val operationCode: Int = 1
)