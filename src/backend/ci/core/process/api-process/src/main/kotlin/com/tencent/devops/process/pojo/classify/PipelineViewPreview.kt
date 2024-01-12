package com.tencent.devops.process.pojo.classify

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Pipeline视图预览")
data class PipelineViewPreview(
    @Schema(name = "新增的流水线ID列表", required = true)
    val addedPipelineInfos: List<PipelineInfo>,
    @Schema(name = "删除的流水线ID列表", required = true)
    val removedPipelineInfos: List<PipelineInfo>,
    @Schema(name = "保留的流水线ID列表", required = true)
    val reservePipelineInfos: List<PipelineInfo>
) {
    data class PipelineInfo(
        @Schema(name = "名称", required = true)
        val pipelineName: String,
        @Schema(name = "ID", required = true)
        val pipelineId: String,
        @Schema(name = "是否删除", required = true)
        val delete: Boolean
    )

    companion object {
        val EMPTY = PipelineViewPreview(emptyList(), emptyList(), emptyList())
    }
}
