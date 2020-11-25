package com.tencent.devops.common.pipeline.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.JsonUtil
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

    fun element2Str(element: Element, objectMapper: ObjectMapper): String? {
        val elementStr = objectMapper.writeValueAsString(element)
        if (elementStr.length > 65534) {
            return null
        }
        return elementStr
    }

    fun getElementInput(element: Element): Map<String, Any>? {
        val json = element.genTaskParams()["data"] ?: return null
        val inputData = JsonUtil.toMap(json!!)["input"] ?: return null
        return JsonUtil.toMap(inputData!!)
    }
}
