package com.tencent.devops.dispatch.devcloud.pojo.performance

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "devcloud项目性能配置")
data class OPPerformanceConfigVO(
    @get:Schema(title = "蓝盾项目ID")
    val projectId: String,
    @get:Schema(title = "性能基础配置ID")
    val optionId: Long
)
