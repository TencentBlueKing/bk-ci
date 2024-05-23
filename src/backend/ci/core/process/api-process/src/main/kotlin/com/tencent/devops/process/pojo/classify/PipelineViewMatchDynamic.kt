package com.tencent.devops.process.pojo.classify

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "命中动态组情况")
data class PipelineViewMatchDynamic(
    @get:Schema(title = "流水线名称")
    val pipelineName: String,
    @get:Schema(title = "标签列表")
    val labels: List<LabelInfo>
) {
    @Schema(title = "标签信息")
    data class LabelInfo(
        @get:Schema(title = "标签分组id", required = false)
        val groupId: String,
        @get:Schema(title = "标签id列表", required = false)
        val labelIds: List<String>
    )
}
