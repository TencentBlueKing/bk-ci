package com.tencent.devops.common.pipeline.pojo.atom.form

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 分组信息
 */
@Schema(title = "插件分组信息")
data class AtomFromOutputItem(
    @get:Schema(title = "类型")
    val type: String,
    @get:Schema(title = "标识")
    val description: String,
    @JsonProperty("isSensitive")
    @get:Schema(title = "是否敏感信息")
    val sensitive: Boolean? = null
)
