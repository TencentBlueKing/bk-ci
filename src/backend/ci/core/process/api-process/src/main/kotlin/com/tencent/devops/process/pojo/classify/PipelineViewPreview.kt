package com.tencent.devops.process.pojo.classify

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Pipeline视图预览")
data class PipelineViewPreview(
    @ApiModelProperty("新增的流水线列表", required = true)
    private val addedPipelines: List<PipelineInfo>,
    @ApiModelProperty("删除的流水线列表", required = true)
    private val removedPipelines: List<PipelineInfo>
) {
    data class PipelineInfo(
        private val pipelineId: String,
        private val name: String
    )
}
