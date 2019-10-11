package com.tencent.devops.quality.api.v2.pojo.op

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@JsonInclude(JsonInclude.Include.ALWAYS)
@ApiModel("质量红线-控制点修改模型")
data class ControlPointUpdate(
    @ApiModelProperty("原子的ClassType")
    val elementType: String?,
    @ApiModelProperty("控制点名称(原子名称)")
    val name: String?,
    @ApiModelProperty("研发阶段")
    val stage: String?,
    @ApiModelProperty("支持红线位置(准入-BEFORE, 准出-AFTER)")
    val availablePosition: String?,
    @ApiModelProperty("默认红线位置")
    val defaultPosition: String?,
    @ApiModelProperty("是否启用")
    val enable: Boolean?
)