package com.tencent.devops.store.pojo.common

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "商城组件组件包信息")
data class StorePackageInfoReq(
    @get:Schema(title = "操作系统名称")
    val osName: String?="",
    @get:Schema(title = "架构")
    val arch: String?="",
    @get:Schema(title = "包大小")
    val size: Long
)