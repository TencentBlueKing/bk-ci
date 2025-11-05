package com.tencent.devops.common.pipeline.pojo.atom.form

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 分组信息
 */
@Schema(title = "插件分组信息")
data class AtomFromInputGroups(
    @get:Schema(title = "名称")
    val name: String,
    @get:Schema(title = "标识")
    val label: String,
    @get:Schema(title = "是否展开")
    @JsonProperty("isExpanded")
    val expanded: Boolean
)
