package com.tencent.devops.store.pojo.container

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "容器信息")
data class ContainerType(
    @Schema(name = "流水线容器类型", required = true)
    var type: String,
    @Schema(name = "操作系统", required = true)
    var osInfos: List<ContainerOsInfo>
)
