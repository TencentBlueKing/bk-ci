package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "运营产品")
data class OperationalProductVO(
    @Schema(name = "产品ID")
    @JsonProperty(value = "ProductId", required = true)
    val productId: Int,
    @Schema(name = "产品ID")
    @JsonProperty(value = "ProductName", required = true)
    val productName: String
)
