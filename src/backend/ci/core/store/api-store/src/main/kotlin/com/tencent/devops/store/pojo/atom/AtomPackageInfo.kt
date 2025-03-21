package com.tencent.devops.store.pojo.atom

import io.swagger.v3.oas.annotations.media.Schema


@Schema(title = "组件包信息")
data class AtomPackageInfo(
    @get:Schema(title = "操作系统名称")
    val osName: String,
    @get:Schema(title = "架构")
    val arch: String,
    @get:Schema(title = "包大小")
    val size: Long
)