package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "Cros产品")
data class CrosProductVO(
    @get:Schema(title = "kpiCode")
    @JsonProperty(value = "product_code", required = true)
    val kpiCode: String,
    @get:Schema(title = "kpi名称")
    @JsonProperty(value = "product_name", required = true)
    val kpiName: String,
    @get:Schema(title = "是否有效")
    @JsonProperty(value = "cros_check", required = true)
    val crosCheck: Int
)
