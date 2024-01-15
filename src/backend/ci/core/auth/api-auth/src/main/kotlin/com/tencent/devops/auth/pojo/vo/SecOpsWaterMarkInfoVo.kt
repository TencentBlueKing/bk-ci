package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "用户水印信息")
data class SecOpsWaterMarkInfoVo(
    @Schema(name = "类型")
    val type: String,
    @Schema(name = "水印信息")
    val data: String
)
