package com.tencent.devops.store.pojo.atom

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "插件代码版本请求对象")
data class AtomCodeVersionReqItem(
    @get:Schema(title = "插件代码", required = true)
    val atomCode: String,
    @get:Schema(title = "插件版本号", required = true)
    val version: String
)
