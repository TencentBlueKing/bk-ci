package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "OBS基礎字典数据")
data class ObsBaseDictDTO(
    @Schema(description = "jsonrpc")
    val jsonrpc: String,
    @Schema(description = "id")
    val id: String,
    @Schema(description = "method")
    val method: String,
    @Schema(description = "params")
    val params: Map<String, String>
)
