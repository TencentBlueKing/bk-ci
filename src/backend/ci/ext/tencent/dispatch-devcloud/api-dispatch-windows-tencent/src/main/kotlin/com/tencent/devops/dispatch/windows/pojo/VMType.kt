package com.tencent.devops.dispatch.windows.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "Windows类型信息")
data class VMType(
    var id: Int,
    var name: String,
    var systemVersion: String,
    var createTime: Long?,
    var updateTime: Long?
)
