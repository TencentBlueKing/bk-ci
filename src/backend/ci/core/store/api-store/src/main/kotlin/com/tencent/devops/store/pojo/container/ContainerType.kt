package com.tencent.devops.store.pojo.container

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "容器信息")
data class ContainerType(
    @get:Schema(title = "流水线容器类型", required = true)
    var type: String,
    @get:Schema(title = "操作系统", required = true)
    var osInfos: List<ContainerOsInfo>
)
