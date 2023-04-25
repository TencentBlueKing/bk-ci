package com.tencent.devops.dispatch.macos.pojo

import io.swagger.annotations.ApiModel

@ApiModel("虚拟机类型信息")
data class VMTypeUpdate(
    var id: Int,
    var name: String,
    var systemVersion: String,
    var version: String,
    var xcodeVersionList: List<String>
)
