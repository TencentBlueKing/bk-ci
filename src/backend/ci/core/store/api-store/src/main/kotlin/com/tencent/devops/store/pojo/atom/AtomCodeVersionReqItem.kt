package com.tencent.devops.store.pojo.atom

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "插件代码版本请求对象")
data class AtomCodeVersionReqItem(
    @Schema(name = "插件代码", required = true)
    val atomCode: String,
    @Schema(name = "插件版本号", required = true)
    val version: String
)
