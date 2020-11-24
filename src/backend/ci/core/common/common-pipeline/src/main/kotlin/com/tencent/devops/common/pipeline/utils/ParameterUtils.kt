package com.tencent.devops.common.pipeline.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.Element

object ParameterUtils {
    fun getListValueByKey(list: List<BuildParameters>, key: String): String? {
        val valueMap = list.filter { it.key == key }.map { it.value }
        return if (valueMap.isNotEmpty()) {
            valueMap.first().toString()
        } else {
            null
        }
    }


    fun parameterSizeCheck(element: Element, objectMapper: ObjectMapper) : Boolean {
        val elementStr = objectMapper.writeValueAsString(element)
        if (elementStr.length > 65534) {
            return false
        }
        return true
    }
}
