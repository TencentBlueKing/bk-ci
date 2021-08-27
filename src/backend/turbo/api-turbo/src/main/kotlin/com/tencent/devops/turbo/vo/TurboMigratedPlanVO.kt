package com.tencent.devops.turbo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("老迁移项目任务信息")
data class TurboMigratedPlanVO(
    @ApiModelProperty("任务ID", required = false)
    val taskId: String? = null,
    @ApiModelProperty("任务名称", required = false)
    val taskName: String? = null,
    @ApiModelProperty("项目语言code", required = false, allowableValues = "1, 2, 3")
    val projLang: String? = null,
    @ApiModelProperty("是否禁用distcc", required = false, allowableValues = "true, false")
    val banDistcc: String? = null,
    @ApiModelProperty("是否启用ccache", required = false, allowableValues = "true, false")
    val ccacheEnabled: String? = null,
    @ApiModelProperty("gcc版本", required = false)
    val gccVersion: String? = null,
    @ApiModelProperty("编译工具类型类型", required = false)
    val toolType: String? = null,
    @ApiModelProperty("禁用所有加速服务", required = false, allowableValues = "true, false")
    val banAllBooster: String? = null
)
