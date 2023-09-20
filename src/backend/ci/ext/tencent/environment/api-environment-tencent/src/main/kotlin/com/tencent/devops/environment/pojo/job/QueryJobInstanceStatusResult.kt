package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("查询任务状态的结果")
data class QueryJobInstanceStatusResult(
    @ApiModelProperty(value = "作业是否结束", required = true)
    val finished: Boolean,
    @ApiModelProperty(value = "作业实例基本信息", required = true)
    val jobInstance: JobInstance,
    @ApiModelProperty(value = "作业步骤列表", required = true)
    val stepInstanceList: List<JobStepInstance>?
)