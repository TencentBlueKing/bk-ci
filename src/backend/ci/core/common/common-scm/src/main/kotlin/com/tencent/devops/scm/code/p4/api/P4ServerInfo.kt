package com.tencent.devops.scm.code.p4.api

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "p4服务端信息")
data class P4ServerInfo(
    @get:Schema(title = "区别大小写")
    val caseSensitive: Boolean
)
