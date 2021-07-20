package com.tencent.bk.codecc.task.pojo

import io.swagger.annotations.ApiModelProperty

data class TriggerPipelineOldRsp(
    @ApiModelProperty("显示页面路径")
    val displayAddress: String,
    @ApiModelProperty("构建id")
    val buildId: String,
    @ApiModelProperty("任务id")
    val taskId: Long,
    @ApiModelProperty("工具清单")
    val toolList: List<String>
)