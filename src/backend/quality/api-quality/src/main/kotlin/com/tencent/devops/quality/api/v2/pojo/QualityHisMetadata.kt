package com.tencent.devops.quality.api.v2.pojo

import com.tencent.devops.quality.api.v2.pojo.enums.QualityDataType

data class QualityHisMetadata(
    val enName: String,
    val cnName: String,
    val detail: String,
    val type: QualityDataType,
    val elementType: String,
    val msg: String,
    val value: String,
    val extra: String?
)