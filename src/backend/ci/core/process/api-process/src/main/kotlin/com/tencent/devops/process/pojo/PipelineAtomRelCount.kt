package com.tencent.devops.process.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "研发商店-组件引用次数统计信息")
data class PipelineAtomRelCount(
    @get:Schema(title = "项目层级引用数量", required = false)
    val projectCount: Long? = 0,
    @get:Schema(title = "流水线层级引用数量", required = false)
    val pipelineCount: Long? = 0,
)
