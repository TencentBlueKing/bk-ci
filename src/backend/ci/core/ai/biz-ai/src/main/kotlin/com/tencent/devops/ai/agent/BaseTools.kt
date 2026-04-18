/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.ai.agent

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier
import kotlin.reflect.KClass

/**
 * 智能体工具集基类，提供所有 *Tools 子类共用的基础能力。
 * @param client 微服务客户端，用于调用其他微服务的 API
 * @param userIdSupplier 当前操作人 ID 供应器，运行时由框架注入
 */
abstract class BaseTools(
    protected val client: Client,
    private val userIdSupplier: Supplier<String>
) {

    protected abstract val logger: Logger

    /** 获取当前操作人 userId，身份不可用时抛出异常。 */
    protected fun getOperatorUserId(): String {
        return try {
            userIdSupplier.get()
        } catch (e: Exception) {
            throw IllegalStateException("操作人身份未知，无法执行操作", e)
        }
    }

    /** 快捷获取微服务客户端代理。 */
    protected fun <T : Any> service(clazz: KClass<T>): T = client.get(clazz)

    /** 解析逗号分隔的字符串为非空字符串列表。 */
    protected fun parseCommaSeparated(input: String): List<String> =
        input.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    /** 解析逗号分隔的字符串为 Int 列表。 */
    protected fun parseCommaSeparatedInts(input: String): List<Int> =
        parseCommaSeparated(input).map { it.toInt() }

    /**
     * 将对象序列化为带字段描述的 JSON。
     * 自动从 @Schema(title) 注解提取字段中文描述。
     *
     * 单个对象返回: {"_fields":{"role":"用户角色标识",...},"data":{...}}
     * 列表返回:     {"_fields":{"groupName":"用户组名称",...},"data":[...]}
     * 提取不到描述时返回裸 JSON。
     *
     * 超过 [MAX_TOOL_OUTPUT_CHARS] 时自动截断并附提示，
     * 防止单个工具输出撑爆 LLM 上下文。
     */
    protected fun toJson(obj: Any): String {
        val targetClass = resolveElementType(obj)
        val fields = extractSchemaFields(targetClass)
        val json = if (fields.isEmpty()) {
            if (obj is String) obj else JsonUtil.toJson(obj)
        } else {
            JsonUtil.toJson(mapOf("_fields" to fields, "data" to obj))
        }
        return truncatePlainText(json, MAX_TOOL_OUTPUT_CHARS)
    }

    private fun truncatePlainText(text: String, maxChars: Int): String {
        if (text.length <= maxChars) {
            return text
        }
        return text.take(maxChars - TRUNCATION_SUFFIX.length) + TRUNCATION_SUFFIX
    }

    private fun resolveElementType(obj: Any): Class<*>? {
        if (obj is Collection<*>) {
            return obj.firstOrNull()?.javaClass
        }
        if (obj is Map<*, *>) {
            val firstValue = obj.values.firstOrNull()
            if (firstValue is Collection<*>) return firstValue.firstOrNull()?.javaClass
            return firstValue?.javaClass
        }
        // SQLPage 等包含 records 列表的包装类型：提取 records 元素的类型
        try {
            val getter = obj.javaClass.methods.firstOrNull { it.name == "getRecords" }
            if (getter != null) {
                val records = getter.invoke(obj)
                if (records is Collection<*>) {
                    return records.firstOrNull()?.javaClass
                }
            }
        } catch (_: Exception) { }
        return obj.javaClass
    }

    private fun extractSchemaFields(clazz: Class<*>?): Map<String, String> {
        if (clazz == null) return emptyMap()
        return schemaCache.getOrPut(clazz) {
            val fields = mutableMapOf<String, String>()
            extractSchemaFieldsRecursive(clazz, "", fields, mutableSetOf(), 0)
            fields
        }
    }

    private fun extractSchemaFieldsRecursive(
        clazz: Class<*>,
        prefix: String,
        fields: MutableMap<String, String>,
        visited: MutableSet<Class<*>>,
        depth: Int
    ) {
        if (!visited.add(clazz)) return
        if (depth > MAX_SCHEMA_DEPTH) return
        if (clazz.packageName.startsWith("java.") || clazz.packageName.startsWith("kotlin.")) return
        if (clazz.isInterface) return

        clazz.methods.forEach { method ->
            val schema = method.getAnnotation(Schema::class.java) ?: return@forEach
            if (schema.title.isBlank()) return@forEach

            val fieldName = when {
                method.name.startsWith("get") -> method.name.removePrefix("get")
                    .replaceFirstChar { it.lowercase() }
                method.name.startsWith("is") -> method.name.removePrefix("is")
                    .replaceFirstChar { it.lowercase() }
                else -> return@forEach
            }

            // 优先使用 @JsonProperty 的名称作为 key（和 JSON 数据一致）
            val jsonKey = resolveJsonFieldName(clazz, fieldName, method)
            val fullKey = if (prefix.isEmpty()) jsonKey else "$prefix.$jsonKey"
            fields[fullKey] = schema.title

            val returnType = resolveNestedType(method.returnType, method.genericReturnType)
            if (returnType != null) {
                extractSchemaFieldsRecursive(returnType, fullKey, fields, visited, depth + 1)
            }
        }
        val nonSyntheticFields = clazz.declaredFields.filter { !it.isSynthetic }
        val ctorParamAnnotations = try {
            (clazz.constructors.firstOrNull() ?: clazz.declaredConstructors.firstOrNull())
                ?.parameterAnnotations
        } catch (_: Exception) { null }

        nonSyntheticFields.forEachIndexed { idx, field ->
            val schema = field.getAnnotation(Schema::class.java)
                ?: ctorParamAnnotations?.getOrNull(idx)
                    ?.filterIsInstance<Schema>()?.firstOrNull()
            val jsonName = field.getAnnotation(JsonProperty::class.java)?.value
                ?: ctorParamAnnotations?.getOrNull(idx)
                    ?.filterIsInstance<JsonProperty>()?.firstOrNull()?.value
                ?: field.name
            val key = if (prefix.isEmpty()) jsonName else "$prefix.$jsonName"
            if (schema != null && schema.title.isNotBlank() && !fields.containsKey(key)) {
                fields[key] = schema.title
            }
        }
    }

    private fun resolveJsonFieldName(clazz: Class<*>, fieldName: String, method: java.lang.reflect.Method): String {
        // 检查字段上的 @field:JsonProperty
        try {
            val field = clazz.getDeclaredField(fieldName)
            field.getAnnotation(JsonProperty::class.java)?.value
                ?.takeIf { it.isNotBlank() }?.let { return it }
        } catch (_: NoSuchFieldException) { }
        // 检查 getter 上的 @get:JsonProperty
        method.getAnnotation(JsonProperty::class.java)?.value
            ?.takeIf { it.isNotBlank() }?.let { return it }
        // 检查构造函数参数上的 @JsonProperty（Kotlin data class 默认目标）
        try {
            val ctor = clazz.constructors.firstOrNull() ?: clazz.declaredConstructors.firstOrNull()
            val nonSyntheticFields = clazz.declaredFields.filter { !it.isSynthetic }
            ctor?.parameterAnnotations?.forEachIndexed { idx, annotations ->
                if (idx < nonSyntheticFields.size && nonSyntheticFields[idx].name == fieldName) {
                    annotations.filterIsInstance<JsonProperty>().firstOrNull()?.value
                        ?.takeIf { it.isNotBlank() }?.let { return it }
                }
            }
        } catch (_: Exception) { }
        return fieldName
    }

    private fun resolveNestedType(rawType: Class<*>, genericType: java.lang.reflect.Type): Class<*>? {
        if (Collection::class.java.isAssignableFrom(rawType)) {
            return extractFirstTypeArg(genericType)
        }
        if (Map::class.java.isAssignableFrom(rawType)) {
            if (genericType is java.lang.reflect.ParameterizedType) {
                val valueType = genericType.actualTypeArguments.getOrNull(1)
                if (valueType is Class<*>) {
                    return valueType.takeIf { it.packageName.startsWith("com.tencent.devops") }
                }
                // Map<K, List<V>> → 取 V
                if (valueType is java.lang.reflect.ParameterizedType) {
                    return extractFirstTypeArg(valueType)
                }
            }
            return null
        }
        if (rawType.packageName.startsWith("com.tencent.devops")) return rawType
        return null
    }

    private fun extractFirstTypeArg(type: java.lang.reflect.Type): Class<*>? {
        if (type is java.lang.reflect.ParameterizedType) {
            val arg = type.actualTypeArguments.firstOrNull()
            if (arg is Class<*>) return arg
        }
        return null
    }

    // ── 安全执行包装 ──

    /**
     * 安全执行查询操作。
     * 查询操作日志轻量化：入口仅打方法名，异常时打堆栈。
     * 查询参数（分页、过滤条件等）通常不需要审计，故不记录。
     *
     * @param tag 日志标签，如 "AuthTool"
     * @param method 工具方法名，如 "listGroups"
     */
    protected inline fun safeQuery(
        tag: String,
        method: String,
        block: () -> String
    ): String {
        logger.info("[{}] {}", tag, method)
        return try {
            block()
        } catch (e: Exception) {
            logger.error("[{}] {} FAILED", tag, method, e)
            "查询失败: ${e.message}"
        }
    }

    /**
     * 安全执行写操作。
     * 写操作需要审计：入口打 INFO 日志（方法名 + 完整参数），异常打 ERROR 日志（方法名 + 参数 + 堆栈）。
     *
     * @param tag 日志标签，如 "AuthTool"
     * @param method 工具方法名，如 "addGroupMembers"
     * @param params LLM 传入的完整参数快照（用于审计）
     */
    protected inline fun safeOperate(
        tag: String,
        method: String,
        params: Map<String, Any?>,
        block: () -> String
    ): String {
        logger.info("[{}] {} | params: {}", tag, method, params)
        return try {
            block()
        } catch (e: Exception) {
            logger.error("[{}] {} FAILED | params: {}", tag, method, params, e)
            "操作失败: ${e.message}"
        }
    }

    companion object {
        private val schemaCache = ConcurrentHashMap<Class<*>, Map<String, String>>()
        private const val MAX_SCHEMA_DEPTH = 3
        private const val MAX_TOOL_OUTPUT_CHARS = 48_000
        private const val TRUNCATION_SUFFIX = "...(已截断)"
    }
}
