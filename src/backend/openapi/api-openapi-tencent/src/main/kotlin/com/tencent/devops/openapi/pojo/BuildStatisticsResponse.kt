package com.tencent.devops.openapi.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线构建统计数据响应消息体")
data class BuildStatisticsResponse(
    @ApiModelProperty("流水线执行次数")
    val buildCount: Long,
    @ApiModelProperty("流水线成功次数")
    val successCount: Long,
    @ApiModelProperty("流水线失败次数")
    val failCount: Long,
    @ApiModelProperty("流水线排队超时次数")
    val queueTimeoutCount: Long,
    @ApiModelProperty("流水线取消次数")
    val cancelCount: Long,
    @ApiModelProperty("流水线成功率")
    val successRate: Float,
    @ApiModelProperty("流水线失败率")
    val failRate: Float
)