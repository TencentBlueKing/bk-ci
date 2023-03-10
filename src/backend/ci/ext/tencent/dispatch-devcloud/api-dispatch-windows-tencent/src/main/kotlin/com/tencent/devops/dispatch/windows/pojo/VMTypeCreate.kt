package com.tencent.devops.dispatch.windows.pojo

import io.swagger.annotations.ApiModel

@ApiModel("虚拟机类型信息")
data class VMTypeCreate(
    var name: String,
    var systemVersion: String
)
