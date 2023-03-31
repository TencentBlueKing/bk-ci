package com.tencent.devops.dispatch.macos.pojo

import io.swagger.annotations.ApiModel

@ApiModel("虚拟机类型信息")
data class MacOsVersionVO(
    val defaultVersion: String,
    val versionList: List<String>
)
