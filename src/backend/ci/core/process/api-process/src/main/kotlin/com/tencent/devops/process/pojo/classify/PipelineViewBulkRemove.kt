package com.tencent.devops.process.pojo.classify

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线组批量移除")
data class PipelineViewBulkRemove(
    @ApiModelProperty("流水线ID列表")
    val pipelineIds: List<String>,
    @ApiModelProperty("视图ID")
    val viewId: String
)
