package com.tencent.devops.process.pojo.classify

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线组--详细数目")
data class PipelineViewPipelineCount(
    @get:Schema(title = "可查看流水线数目")
    val normalCount: Int,
    @get:Schema(title = "已删除流水线数目")
    val deleteCount: Int
) {
    companion object {
        val DEFAULT = PipelineViewPipelineCount(0, 0)
    }
}
