package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("文件分发的结果")
data class FileDistributeResult (
    @ApiModelProperty(value = "作业实例ID", required = true)
    val jobInstanceId: Long,
    @ApiModelProperty(value = "作业实例名称", required = true)
    val jobInstanceName: Long,
    @ApiModelProperty(value = "步骤实例ID", required = true)
    val stepInstanceId: Long
)