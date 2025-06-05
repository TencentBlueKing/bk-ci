package com.tencent.devops.common.archive.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "制品质量元数据标签")
data class MetadataLabelDetailUpdate(
    val labelColorMap: Map<String, String>?,
    val enumType: Boolean?,
    val display: Boolean?,
    val category: String?,
    val system: Boolean?,
    val description: String?,
)
