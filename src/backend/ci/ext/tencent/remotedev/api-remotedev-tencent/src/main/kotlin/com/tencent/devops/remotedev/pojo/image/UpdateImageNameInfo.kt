package com.tencent.devops.remotedev.pojo.image

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "修改镜像名称信息")
data class UpdateImageNameInfo(
    @get:Schema(title = "id")
    val id: Long,
    @get:Schema(title = "镜像名称")
    val imageName: String
)
