package com.tencent.devops.dispatch.docker.pojo.devcloud

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "devcloud项目性能配置")
data class PerformanceConfigVO(
    @get:Schema(title = "蓝盾项目ID")
    val projectId: String,
    @get:Schema(title = "CPU")
    val cpu: Int,
    @get:Schema(title = "内存")
    val memory: String,
    @get:Schema(title = "磁盘")
    val disk: String,
    @get:Schema(title = "描述")
    val description: String
)
