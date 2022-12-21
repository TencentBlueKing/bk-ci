package com.tencent.devops.process.pojo.classify

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Pipeline视图预览")
data class PipelineViewPreview(
    @ApiModelProperty("新增的流水线ID列表", required = true)
    val addedPipelineInfos: List<PipelineInfo>,
    @ApiModelProperty("删除的流水线ID列表", required = true)
    val removedPipelineInfos: List<PipelineInfo>,
    @ApiModelProperty("保留的流水线ID列表", required = true)
    val reservePipelineInfos: List<PipelineInfo>
) {
    data class PipelineInfo(
        @ApiModelProperty("名称", required = true)
        val pipelineName: String,
        @ApiModelProperty("ID", required = true)
        val pipelineId: String,
        @ApiModelProperty("是否删除", required = true)
        val delete: Boolean
    )

    companion object {
        val EMPTY = PipelineViewPreview(emptyList(), emptyList(), emptyList())
    }
}
