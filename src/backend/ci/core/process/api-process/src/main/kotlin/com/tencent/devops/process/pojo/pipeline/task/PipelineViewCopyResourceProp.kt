package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线视图复制资源属性")
data class PipelineViewCopyResourceProp(
    @get:Schema(title = "流水线组类型,1--动态,2--静态")
    val viewType: Int
) : PipelineCopyResourceProp {
    companion object {
        const val classType = "pipelineView"
    }
}
