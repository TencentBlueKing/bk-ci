package com.tencent.devops.process.pojo.template

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线模板信息")
class TemplatePipelineInfo(
    @ApiModelProperty("模板id", required = false)
    val templateId: String? = null,
    @ApiModelProperty("版本名称", required = false)
    val versionName: String? = null,
    @ApiModelProperty("版本", required = false)
    val version: Long? = null,
    @ApiModelProperty("流水线id", required = false)
    val pipelineId: String
)
