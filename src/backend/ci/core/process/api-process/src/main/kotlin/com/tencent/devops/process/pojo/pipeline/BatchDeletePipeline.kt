package com.tencent.devops.process.pojo.pipeline

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("批量删除流水线")
data class BatchDeletePipeline(
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("流水线ID列表")
    val pipelineIds: List<String>
)
