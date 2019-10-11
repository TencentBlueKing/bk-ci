package com.tencent.devops.quality.api.v2.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线-控制点v2")
data class QualityControlPoint(
    @ApiModelProperty("控制点HashId", required = true)
    val hashId: String,
    @ApiModelProperty("原子的ClassType", required = true)
    val type: String,
    @ApiModelProperty("控制点名称", required = true)
    val name: String,
    @ApiModelProperty("研发阶段", required = true)
    val stage: String,
    @ApiModelProperty("支持红线位置(准入-BEFORE, 准出-AFTER)", required = true)
    val availablePos: List<ControlPointPosition>,
    @ApiModelProperty("默认红线位置", required = true)
    val defaultPos: ControlPointPosition,
    @ApiModelProperty("是否启用", required = true)
    val enable: Boolean,
    @ApiModelProperty("对应有质量红线输出的版本", required = true)
    val atomVersion: String,
    @ApiModelProperty("对应有质量红线测试项目", required = true)
    val testProject: String = ""
)