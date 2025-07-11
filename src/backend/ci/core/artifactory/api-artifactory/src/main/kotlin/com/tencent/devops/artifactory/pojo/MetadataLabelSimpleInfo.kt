package com.tencent.devops.artifactory.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "元数据标签-简化")
data class MetadataLabelSimpleInfo(
    val key: String,
    val values: List<String>
)
