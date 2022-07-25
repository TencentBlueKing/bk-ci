package com.tencent.devops.process.pojo.classify

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Pipeline视图预览")
data class PipelineViewPreview(
    @ApiModelProperty("新增的流水线ID列表", required = true)
    val addedPipelineIds: List<String>,
    @ApiModelProperty("删除的流水线ID列表", required = true)
    val removedPipelineIds: List<String>
) {
    companion object {
        val EMPTY = PipelineViewPreview(emptyList(), emptyList())
    }
}
