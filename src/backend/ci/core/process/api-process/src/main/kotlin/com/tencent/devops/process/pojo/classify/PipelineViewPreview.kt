package com.tencent.devops.process.pojo.classify

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Pipeline视图预览")
data class PipelineViewPreview(
    @ApiModelProperty("新增的流水线列表", required = true)
    val addedPipelines: List<PipelineInfo>,
    @ApiModelProperty("删除的流水线列表", required = true)
    val removedPipelines: List<PipelineInfo>
) {
    data class PipelineInfo(
        val pipelineId: String,
        val name: String
    )
}
