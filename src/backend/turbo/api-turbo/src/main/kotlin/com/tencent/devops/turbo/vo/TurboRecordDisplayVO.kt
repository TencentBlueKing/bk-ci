package com.tencent.devops.turbo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("编译加速记录显示视图")
data class TurboRecordDisplayVO(
    @ApiModelProperty("编译开始时间")
    val startTime: LocalDateTime,
    @ApiModelProperty("状态")
    val status: String?,
    @ApiModelProperty("执行次数")
    var executeCount: Int = 0,
    @ApiModelProperty("编译花费时间")
    val elapsedTime: String?,
    @ApiModelProperty("流水线id")
    val pipelineId: String?,
    @ApiModelProperty("流水线名称")
    val pipelineName: String?,
    @ApiModelProperty("客户端ip")
    val clientIp: String?,
    @ApiModelProperty("显示字段")
    val displayFields: List<TurboDisplayFieldVO>?,
    @ApiModelProperty("编译加速临时数据显示连接")
    val recordViewUrl: String?
)
