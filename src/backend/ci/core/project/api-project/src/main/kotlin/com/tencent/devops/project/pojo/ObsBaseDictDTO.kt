package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "OBS基礎字典数据")
data class ObsBaseDictDTO(
    @Schema(name = "jsonrpc")
    val jsonrpc: String,
    @Schema(name = "id")
    val id: String,
    @Schema(name = "method")
    val method: String,
    @Schema(name = "params")
    val params: Map<String, String>
)
