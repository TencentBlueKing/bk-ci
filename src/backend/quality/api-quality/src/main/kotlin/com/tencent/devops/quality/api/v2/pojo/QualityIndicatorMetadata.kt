package com.tencent.devops.quality.api.v2.pojo

import com.tencent.devops.quality.api.v2.pojo.enums.QualityDataType
import io.swagger.annotations.ApiModel

@ApiModel("指标基础数据")
data class QualityIndicatorMetadata(
    val hashId: String,
    val dataId: String,
    val dataName: String,
    val elementType: String,
    val elementName: String,
    val elementDetail: String,
    val valueType: QualityDataType,
    val desc: String,
    val extra: String
)