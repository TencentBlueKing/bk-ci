package com.tencent.devops.quality.api.v2.pojo.request

import com.tencent.devops.quality.api.v2.pojo.enums.QualityDataType

data class MetadataCallback(
    val elementType: String,
    val data: List<CallbackHisMetadata>
) {

    data class CallbackHisMetadata(
        val enName: String,
        val cnName: String,
        val detail: String,
        val type: QualityDataType,
        val msg: String,
        val value: String,
        val extra: String?
    )
}
