package com.tencent.devops.plugin.pojo.tcm

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("tcm模板类型")
data class TcmTemplate(
    @ApiModelProperty("模板所在目录")
    val templateCategory: String,
    @ApiModelProperty("模板名称")
    val templateName: String,
    @ApiModelProperty("模板id")
    val templateId: String
)