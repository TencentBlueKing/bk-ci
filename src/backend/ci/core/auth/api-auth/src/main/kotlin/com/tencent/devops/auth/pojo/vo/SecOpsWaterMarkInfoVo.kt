package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "用户水印信息")
data class SecOpsWaterMarkInfoVo(
    @Schema(description = "类型")
    val type: String,
    @Schema(description = "水印信息")
    val data: String
)
