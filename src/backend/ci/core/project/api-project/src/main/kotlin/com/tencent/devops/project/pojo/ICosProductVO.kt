package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "Icos产品")
data class ICosProductVO(
    @get:Schema(title = "运营产品ID")
    @JsonProperty(value = "obs_product_id", required = true)
    val productId: Int,
    @get:Schema(title = "运营产品名称")
    @JsonProperty(value = "obs_product_name", required = true)
    val productName: String,
    @get:Schema(title = "财务Id")
    @JsonProperty(value = "icos_product_code", required = false)
    val iCosProductCode: String? = null,
    @get:Schema(title = "财务名称")
    @JsonProperty(value = "icos_product_name", required = false)
    val iCosProductName: String? = null
)
