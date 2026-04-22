package com.tencent.devops.remotedev.pojo.cvd

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "CVD资源池实例数量")
@JsonIgnoreProperties(ignoreUnknown = true)
data class CvdCountItem(
    @get:Schema(description = "总数量")
    val total: Int? = null,
    @get:Schema(description = "可用数量")
    val available: Int? = null
)
