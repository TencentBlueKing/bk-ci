package com.tencent.devops.process.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线信息查询条件")
data class PipelineInfoQueryCondition(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线ID集合", required = false)
    val pipelineIds: Set<String>? = null,
    @get:Schema(title = "流水线名称(模糊匹配)", required = false)
    val pipelineName: String? = null,
    @get:Schema(title = "分页大小", required = false)
    val limit: Int? = null,
    @get:Schema(title = "分页偏移量", required = false)
    val offset: Int? = null
)
