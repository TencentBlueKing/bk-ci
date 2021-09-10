
package com.tencent.devops.turbo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("编译加速历史列表视图")
data class TurboRecordHistoryVO(
    @ApiModelProperty("编译加速记录主键")
    var id: String = "",

    @ApiModelProperty("流水线Id")
    var pipelineId: String? = null,

    @ApiModelProperty("流水线名称")
    var pipelineName: String? = null,

    @ApiModelProperty("客户端ip")
    var clientIp: String? = null,

    @ApiModelProperty("编译加速记录id")
    var buildId: String? = null,

    @ApiModelProperty("流水线构建id")
    var devopsBuildId: String? = null,

    @ApiModelProperty("未加速耗时")
    var estimateTimeSecond: Long = 0L,

    @ApiModelProperty("未加速耗时显示")
    var estimateTimeValue: String? = "--",

    @ApiModelProperty("实际耗时")
    var executeTimeSecond: Long = 0L,

    @ApiModelProperty("实际耗时显示")
    var executeTimeValue: String? = "--",

    @ApiModelProperty("节省率")
    var turboRatio: String? = "--",

    @ApiModelProperty("开始时间")
    var startTime: LocalDateTime? = null,

    @ApiModelProperty("状态")
    var status: String = "",

    @ApiModelProperty("失败原因")
    var message: String? = null,

    @ApiModelProperty("执行编号")
    var executeNum: Int? = null
)
