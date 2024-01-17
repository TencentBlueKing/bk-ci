package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "OBS基礎字典数据")
data class ObsBaseDictDTO(
    @Schema(title = "jsonrpc")
    val jsonrpc: String,
    @Schema(title = "id")
    val id: String,
    @Schema(title = "method")
    val method: String,
    @Schema(title = "params")
    val params: Map<String, String>
)
