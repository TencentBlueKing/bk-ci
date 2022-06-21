package com.tencent.devops.turbo.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import javax.validation.constraints.NotBlank

@ApiModel("编译加速模式优先级请求模型")
data class TurboEngineConfigPriorityModel(
    @ApiModelProperty("引擎代码")
    @get:NotBlank(
        message = "请先选择加速模式！"
    )
    val engineCode: String,
    @ApiModelProperty("优先级序号")
    val priorityNum: Int?
)
