package com.tencent.devops.dispatch.devcloud.pojo.performance

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "devcloud性能基础选项配置")
data class PerformanceOptionsVO(
    @get:Schema(title = "CPU")
    val cpu: Int,
    @get:Schema(title = "内存")
    val memory: Int,
    @get:Schema(title = "磁盘")
    val disk: Int,
    @get:Schema(title = "描述")
    val description: String
)
