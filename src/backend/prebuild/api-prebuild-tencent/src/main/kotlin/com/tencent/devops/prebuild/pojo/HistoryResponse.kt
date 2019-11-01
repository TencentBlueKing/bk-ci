package com.tencent.devops.prebuild.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("执行历史响应")
data class HistoryResponse(
    @ApiModelProperty("构建ID", required = true)
    val buildId: String,
    @ApiModelProperty("构建号", required = true)
    val buildNum: Int?,
    @ApiModelProperty("开始时间", required = true)
    val startTime: Long,
    @ApiModelProperty("结束时间", required = true)
    val endTime: Long?,
    @ApiModelProperty("状态", required = true)
    val status: String
)
