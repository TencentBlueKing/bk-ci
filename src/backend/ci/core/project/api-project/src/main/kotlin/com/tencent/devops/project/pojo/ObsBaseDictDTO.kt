package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "OBS基礎字典数据")
data class ObsBaseDictDTO(
    @get:Schema(title = "jsonrpc")
    val jsonrpc: String,
    @get:Schema(title = "id")
    val id: String,
    @get:Schema(title = "method")
    val method: String,
    @get:Schema(title = "params")
    val params: Map<String, String>
)
