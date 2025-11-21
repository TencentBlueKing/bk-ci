package com.tencent.devops.process.trigger.market

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.store.pojo.event.EventFieldMappingItem
import com.tencent.devops.store.pojo.event.enums.MappingSource
import org.springframework.stereotype.Service

@Service
class MarketEventVariablesResolver {

    private fun getEventVariables(
        fieldMappings: List<EventFieldMappingItem>,
        incomingHeaders: Map<String, String>?,
        incomingQueryParamMap: Map<String, String>?,
        incomingBody: String
    ): Map<String, String> {
        val resolvedVariables = mutableMapOf<String, String>()
        val jsonNode = JsonUtil.getObjectMapper().readTree(incomingBody)
        fieldMappings.forEach { fieldMapping ->
            when (fieldMapping.source) {
                MappingSource.QUERY -> {
                    incomingQueryParamMap?.get(fieldMapping.sourcePath)?.let {
                        resolvedVariables[fieldMapping.targetField] = it
                    }
                }

                MappingSource.HEADER -> {
                    incomingHeaders?.get(fieldMapping.sourcePath)?.let {
                        resolvedVariables[fieldMapping.targetField] = it
                    }
                }

                MappingSource.BODY -> {
                    val valueNode = jsonNode.path(fieldMapping.sourcePath)
                    if (!valueNode.isMissingNode && !valueNode.isNull) {
                        resolvedVariables[fieldMapping.targetField] = valueNode.asText()
                    }
                }
            }
        }
        return resolvedVariables
    }
}
