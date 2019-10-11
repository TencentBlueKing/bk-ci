package com.tencent.devops.quality.api.v2.pojo.response

data class RangeExistElement(
    val name: String,
    val cnName: String,
    val count: Int,
    val params: Map<String, Any> = mapOf()
)