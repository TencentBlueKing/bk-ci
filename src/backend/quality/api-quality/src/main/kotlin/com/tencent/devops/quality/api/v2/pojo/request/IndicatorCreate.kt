package com.tencent.devops.quality.api.v2.pojo.request

import com.tencent.devops.quality.api.v2.pojo.enums.QualityDataType
import io.swagger.annotations.ApiModel
import com.tencent.devops.quality.api.v2.pojo.enums.QualityOperation

@ApiModel("创建指标请求")
data class IndicatorCreate(
    val name: String,
    val cnName: String,
    val desc: String,
    val dataType: QualityDataType,
    val operation: List<QualityOperation>,
    val threshold: String,
    val elementType: String
)