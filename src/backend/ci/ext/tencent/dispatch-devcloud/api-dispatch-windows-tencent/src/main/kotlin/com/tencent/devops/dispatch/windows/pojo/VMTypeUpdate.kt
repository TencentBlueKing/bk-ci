package com.tencent.devops.dispatch.windows.pojo

import io.swagger.annotations.ApiModel

@ApiModel("虚拟机类型信息")
data class VMTypeUpdate(
    var id: Int,
    var name: String,
    var systemVersion: String
)
