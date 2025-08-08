package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "Cros产品")
data class CrosProductVO(
    @get:Schema(title = "财务Id")
    @JsonProperty(value = "product_code", required = true)
    val iCosProductCode: String,
    @get:Schema(title = "是否有效")
    @JsonProperty(value = "cros_check", required = true)
    val crosCheck: Int
)
