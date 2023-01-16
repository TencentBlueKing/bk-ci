package com.tencent.devops.process.pojo.classify

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线视图表单")
data class PipelineViewTopForm(
    @ApiModelProperty("是否生效", required = true)
    val enabled: Boolean
)
