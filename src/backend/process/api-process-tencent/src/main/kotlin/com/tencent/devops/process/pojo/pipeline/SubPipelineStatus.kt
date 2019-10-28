package com.tencent.devops.process.pojo.pipeline

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("构建模型-ID")
data class SubPipelineStatus(
    @ApiModelProperty("子流水线状态", required = true)
    val status: String
)