package com.tencent.devops.common.archive.pojo

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "制品质量元数据标签")
data class MetadataLabelDetail(
    val labelKey: String,
    val labelColorMap: Map<String, String>,
    val enumType: Boolean,
    val display: Boolean,
    val category: String?,
    val system: Boolean,
    val description: String,
    val createdBy: String?,
    val createdDate: LocalDateTime?,
    val lastModifiedBy: String?,
    val lastModifiedDate: LocalDateTime?
)
