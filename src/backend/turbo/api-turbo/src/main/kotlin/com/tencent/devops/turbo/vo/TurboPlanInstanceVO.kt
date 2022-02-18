
package com.tencent.devops.turbo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("编译加速实例视图")
data class TurboPlanInstanceVO(
    @ApiModelProperty("流水线Id")
    var pipelineId: String? = null,

    @ApiModelProperty("流水线名称")
    var pipelineName: String? = null,

    @ApiModelProperty("构建机ip")
    var clientIp: String? = null,

    @ApiModelProperty("流水线元素id")
    var pipelineElementId: String? = null,

    @ApiModelProperty("加速次数")
    var executeCount: Int = 0,

    @ApiModelProperty("平均耗时")
    var averageExecuteTimeSecond: Long = 0L,

    @ApiModelProperty("平均耗时显示")
    var averageExecuteTimeValue: String? = "--",

    @ApiModelProperty("平均预估耗时")
    var averageEstimateTimeSecond: Long = 0L,

    @ApiModelProperty("节省率")
    var turboRatio: String = "--",

    @ApiModelProperty("最新开始时间")
    var latestStartTime: LocalDateTime? = null,

    @ApiModelProperty("最新状态")
    var latestStatus: String? = ""
)
