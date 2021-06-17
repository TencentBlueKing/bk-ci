package com.tencent.bk.codecc.task.pojo

import io.swagger.annotations.ApiModelProperty

data class TriggerPipelineRsp(
    @ApiModelProperty("项目id")
    val projectId: String,
    @ApiModelProperty("流水线id")
    val pipelineId: String,
    @ApiModelProperty("任务id")
    val taskId: Long,
    @ApiModelProperty("工具清单")
    val toolList: List<String>,
    @ApiModelProperty("是否首次触发")
    val firstTrigger: String,
    @ApiModelProperty("codecc构建id")
    val codeccBuildId: String
)