package com.tencent.devops.process.trigger.market

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ReflectUtil
import com.tencent.devops.store.pojo.trigger.EventFieldMappingItem
import com.tencent.devops.store.pojo.trigger.enums.MappingSource
import org.springframework.stereotype.Service

@Service
class MarketEventVariablesResolver {

    fun getEventVariables(
        fieldMappings: List<EventFieldMappingItem>,
        incomingHeaders: Map<String, String>?,
        incomingQueryParamMap: Map<String, String>?,
        incomingBody: String
    ): Map<String, String> {
        val resolvedVariables = mutableMapOf<String, String>()
        val jsonNode = flattenToMap(JsonUtil.toMap(incomingBody))
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
                    jsonNode[fieldMapping.sourcePath]?.let {
                        resolvedVariables[targetField] = it as String
                    }
                }
            }
        }
        return resolvedVariables
    }

    /**
     * 将对象扁平化为深度为1的Map
     * @param separator 属性分隔符
     */
    fun flattenToMap(obj: Any, separator: String = "."): Map<String, Any?> {
        return flattenObject(obj, "", separator).toMap()
    }

    /**
     * 将对象扁平化为深度为1的可变Map
     * @param separator 属性分隔符
     */
    fun flattenToMutableMap(obj: Any, separator: String = "."): MutableMap<String, Any?> {
        return flattenObject(obj, "", separator)
    }

    /**
     * 递归扁平化对象
     *
     * @param obj 当前要处理的对象
     * @param prefix 当前属性路径前缀
     * @return 扁平化的可变Map
     */
    private fun flattenObject(
        obj: Any?,
        prefix: String,
        separator: String = "."
    ): MutableMap<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        when {
            obj == null -> {
                if (prefix.isNotEmpty()) {
                    result[prefix] = null
                }
            }
            // 基础类型
            ReflectUtil.isNativeType(obj) || obj is String -> {
                if (prefix.isNotEmpty()) {
                    result[prefix] = obj
                }
            }
            // map
            obj is Map<*, *> -> {
                obj.forEach { (key, value) ->
                    val newPrefix = if (prefix.isEmpty()) key.toString() else "$prefix$separator$key"
                    result.putAll(flattenObject(value, newPrefix, separator))
                }
            }
            // 集合
            obj is Collection<*> || obj.javaClass.isArray -> {
                val collection = if (obj.javaClass.isArray) {
                    (obj as Array<*>).toList()
                } else {
                    obj as Collection<*>
                }
                collection.forEachIndexed { index, item ->
                    result.putAll(flattenObject(item, "$prefix[$index]", separator))
                }
            }
            // 对象
            else -> {
                val map = JsonUtil.toMutableMap(obj)
                map.forEach { (key, value) ->
                    val newPrefix = if (prefix.isEmpty()) key else "$prefix$separator$key"
                    result.putAll(flattenObject(value, newPrefix, separator))
                }
            }
        }

        return result
    }
}
