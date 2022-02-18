package com.tencent.devops.stream.pojo.v2

/**
 * 创建红线时需要校验的红线指标列表
 */
data class QualityElementInfo(
    val elementName: String,
    val atomCode: String
)
