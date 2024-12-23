package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户水印信息")
data class SecOpsWaterMarkInfoVo(
    @get:Schema(title = "类型")
    val type: String,
    @get:Schema(title = "水印信息")
    val data: String
)
