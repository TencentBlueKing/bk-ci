package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "OBS运营产品")
data class ObsOperationalProductResponse(
    @Schema(name = "jsonrpc")
    val jsonrpc: String,
    @Schema(name = "id")
    val id: String,
    @Schema(name = "运营产品")
    val result: ObsOperationalProductResult
)
