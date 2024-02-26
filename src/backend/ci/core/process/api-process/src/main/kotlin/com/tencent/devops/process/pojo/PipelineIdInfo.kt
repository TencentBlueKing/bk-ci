package com.tencent.devops.process.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线id模型")
data class PipelineIdInfo(
    @get:Schema(title = "流水线id，全局唯一", required = false)
    val pipelineId: String,
    @get:Schema(title = "流水线自增ID，主要用于权限中心的资源ID，保证项目下唯一", required = false)
    val id: Long?
)
