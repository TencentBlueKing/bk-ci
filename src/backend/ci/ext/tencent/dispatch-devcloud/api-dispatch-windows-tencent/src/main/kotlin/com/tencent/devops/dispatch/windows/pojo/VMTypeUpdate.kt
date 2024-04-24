package com.tencent.devops.dispatch.windows.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "虚拟机类型信息")
data class VMTypeUpdate(
    var id: Int,
    var name: String,
    var systemVersion: String
)
