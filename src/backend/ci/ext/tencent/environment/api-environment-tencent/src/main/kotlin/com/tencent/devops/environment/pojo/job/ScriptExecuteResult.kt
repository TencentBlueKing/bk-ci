package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("执行脚本的结果")
data class ScriptExecuteResult (
    @ApiModelProperty("任务名称", required = true)
    val name: String,
    @ApiModelProperty("操作人", required = true)
    val operator: String,
    @ApiModelProperty("结果状态", required = true)
    val status: Int,
    @ApiModelProperty("执行开始时间", required = false)
    val startTime: String?,
    @ApiModelProperty("执行结束时间", required = false)
    val endTime: String?,
    @ApiModelProperty("执行用时", required = false)
    val totalTime: Int?,
    @ApiModelProperty("任务创建时间", required = false)
    val createTime: String?,
    @ApiModelProperty("任务ID", required = true)
    val jobInstanceId: Long
)