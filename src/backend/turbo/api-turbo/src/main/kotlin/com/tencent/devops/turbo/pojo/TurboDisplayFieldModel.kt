package com.tencent.devops.turbo.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("编译加速模式显示字段配置")
data class TurboDisplayFieldModel(
    @ApiModelProperty("字段key值")
    val fieldKey: String,
    @ApiModelProperty("字段名")
    val fieldName: String,
    @ApiModelProperty("是否有链接")
    val link: Boolean?,
    @ApiModelProperty("链接模板")
    val linkTemplate: String?,
    @ApiModelProperty("链接变量")
    val linkVariable: Set<String>?
)
