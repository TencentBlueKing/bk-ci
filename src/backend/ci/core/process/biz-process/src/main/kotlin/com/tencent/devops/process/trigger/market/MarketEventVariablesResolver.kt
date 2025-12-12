package com.tencent.devops.process.trigger.market

import com.tencent.devops.common.api.util.JsonPathUtil
import com.tencent.devops.store.pojo.trigger.EventFieldMappingItem
import com.tencent.devops.store.pojo.trigger.enums.MappingSource
import org.springframework.stereotype.Service

@Service
class MarketEventVariablesResolver {

    /**
     * 获取字段映射后的变量
     *
     * @param fieldMappings 字段映射列表
     * @param incomingHeaders 请求头
     * @param incomingQueryParamMap 请求参数
     * @param incomingBody 请求体
     */
    fun getEventVariables(
        fieldMappings: List<EventFieldMappingItem>,
        incomingHeaders: Map<String, String>?,
        incomingQueryParamMap: Map<String, String>?,
        incomingBody: String
    ): Map<String, Any> {
        val resolvedVariables = mutableMapOf<String, Any>()
        val document = JsonPathUtil.parse(incomingBody)
        fieldMappings.forEach { fieldMapping ->
            val targetField = fieldMapping.targetField
            when (fieldMapping.source) {
                MappingSource.QUERY -> {
                    incomingQueryParamMap?.get(fieldMapping.sourcePath)?.let {
                        resolvedVariables[targetField] = it
                    }
                }

                MappingSource.HEADER -> {
                    incomingHeaders?.get(fieldMapping.sourcePath)?.let {
                        resolvedVariables[targetField] = it
                    }
                }

                MappingSource.BODY -> {
                    JsonPathUtil.read<Any>(
                        document = document,
                        path = fieldMapping.sourcePath
                    )?.let {
                        resolvedVariables[targetField] = it
                    }
                }
            }
        }
        return resolvedVariables
    }
}
