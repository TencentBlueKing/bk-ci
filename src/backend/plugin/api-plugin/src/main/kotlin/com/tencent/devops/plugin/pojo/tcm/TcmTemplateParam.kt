package com.tencent.devops.plugin.pojo.tcm

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("tcm模板参数类型")
data class TcmTemplateParam(
    @ApiModelProperty("参数序号，顺序从1开始")
    val seq: String,
    @ApiModelProperty("模板参数名称")
    val paramName: String
)