package com.tencent.devops.store.pojo.container

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "操作系统信息")
data class ContainerOsInfo(
    @Schema(name = "OS", required = true)
    val os: String,
    @Schema(name = "NAME", required = true)
    val name: String
)
