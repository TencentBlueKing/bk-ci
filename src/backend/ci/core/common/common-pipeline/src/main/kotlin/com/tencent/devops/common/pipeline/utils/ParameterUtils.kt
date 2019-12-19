package com.tencent.devops.common.pipeline.utils

import com.tencent.devops.common.pipeline.pojo.BuildParameters

object ParameterUtils {
    fun getListValueByKey(list: List<BuildParameters>, key: String): String? {
        val valueMap = list.filter { it.key == key }.map { it.value }
        return if (valueMap.isNotEmpty()) {
            valueMap.first().toString()
        } else {
            null
        }
    }
}