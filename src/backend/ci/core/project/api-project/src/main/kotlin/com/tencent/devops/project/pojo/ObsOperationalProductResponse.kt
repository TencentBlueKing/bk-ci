package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "OBS运营产品")
data class ObsOperationalProductResponse(
    @get:Schema(title = "jsonrpc")
    val jsonrpc: String,
    @get:Schema(title = "id")
    val id: String,
    @get:Schema(title = "运营产品")
    val result: ObsOperationalProductResult
)
