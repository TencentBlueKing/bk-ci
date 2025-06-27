package com.tencent.devops.common.archive.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "制品质量元数据分析")
data class ArtifactQualityMetadataAnalytics(
    val labelKey: String,
    val value: String,
    val color: String? = "#C4C6CC",
    val count: Int
)
