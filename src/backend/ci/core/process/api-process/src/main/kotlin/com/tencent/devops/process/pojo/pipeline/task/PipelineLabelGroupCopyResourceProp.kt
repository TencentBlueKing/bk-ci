package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线标签组复制资源属性")
data class PipelineLabelGroupCopyResourceProp(
    @get:Schema(description = "标签组ID")
    val groupId: String,
    @get:Schema(description = "标签组名")
    val groupName: String
) : PipelineCopyResourceProp {
    companion object {
        const val classType = "pipelineLabelGroup"
    }
}
