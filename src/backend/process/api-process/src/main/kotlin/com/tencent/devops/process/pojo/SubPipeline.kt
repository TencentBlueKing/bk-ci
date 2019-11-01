package com.tencent.devops.process.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * @ Author     ：Royal Huang
 * @ Date       ：Created in 11:14 2019-07-31
 */

@ApiModel("子流水线基本信息")
data class SubPipeline(
    @ApiModelProperty("流水线名称", required = true)
    val pipelineName: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String
)
