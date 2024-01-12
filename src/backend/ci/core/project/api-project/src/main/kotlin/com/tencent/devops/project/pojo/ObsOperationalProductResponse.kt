package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "OBS运营产品")
data class ObsOperationalProductResponse(
    @Schema(description = "jsonrpc")
    val jsonrpc: String,
    @Schema(description = "id")
    val id: String,
    @Schema(description = "运营产品")
    val result: ObsOperationalProductResult
)
