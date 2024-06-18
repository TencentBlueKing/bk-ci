package com.tencent.devops.metrics.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "JOB执行最大并发")
data class MaxJobConcurrencyVO(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "VM最大并发")
    val dockerVm: Int,
    @get:Schema(title = "DevCloud-Linux最大并发")
    val dockerDevcloud: Int,
    @get:Schema(title = "DevCloud-macOS最大并发")
    val macosDevcloud: Int,
    @get:Schema(title = "DevCloud-Windows最大并发")
    val windowsDevcloud: Int,
    @get:Schema(title = "无编译环境最大并发")
    val buildLess: Int,
    @get:Schema(title = "第三方构建机最大并发")
    val other: Int
)
