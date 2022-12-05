package com.tencent.devops.turbo.pojo

import com.tencent.devops.turbo.validate.TurboRecordGroup
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDate
import javax.validation.constraints.NotBlank

@ApiModel("编译加速历史请求数据模型")
data class TurboRecordModel(
    @ApiModelProperty("项目id")
    @get:NotBlank(
        message = "项目id不能为空",
        groups = [
            TurboRecordGroup.OpenApi::class
        ]
    )
    val projectId: String?,
    @ApiModelProperty("加速方案id")
    val turboPlanId: List<String>?,
    @ApiModelProperty("流水线id")
    val pipelineId: List<String>?,
    @ApiModelProperty("客户端ip")
    val clientIp: List<String>?,
    @ApiModelProperty("状态")
    val status: List<String>?,
    @ApiModelProperty("开始时间")
    val startTime: LocalDate?,
    @ApiModelProperty("结束时间")
    val endTime: LocalDate?
)
