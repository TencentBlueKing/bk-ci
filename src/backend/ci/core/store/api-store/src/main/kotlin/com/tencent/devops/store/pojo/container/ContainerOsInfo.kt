package com.tencent.devops.store.pojo.container

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "操作系统信息")
data class ContainerOsInfo(
    @Schema(description = "OS", required = true)
    val os: String,
    @Schema(description = "NAME", required = true)
    val name: String
)
