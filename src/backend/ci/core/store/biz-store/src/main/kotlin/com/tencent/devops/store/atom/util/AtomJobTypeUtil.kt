/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.atom.util

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.store.pojo.common.ServiceScopeConfig
import com.tencent.devops.store.pojo.common.enums.ServiceScopeEnum
import com.tencent.devops.store.util.ServiceScopeUtil

/**
 * 插件 Job 类型工具类 —— 处理 T_ATOM.JOB_TYPE 字段的读写。
 *
 * 存储格式演进：
 * 1. 老数据：纯字符串，如 "AGENT"（隐含 PIPELINE 范围）
 * 2. V1 JSON：scope → 单个 jobType，如 {"PIPELINE":"AGENT","CREATIVE_STREAM":"CREATIVE_STREAM"}
 * 3. V2 JSON：scope → jobType 列表，如 {"PIPELINE":["AGENT"],"CREATIVE_STREAM":["CREATIVE_STREAM","CLOUD_TASK"]}
 *
 * 读取时三种格式均兼容；写入时统一使用 V2 格式（仅 PIPELINE + 单 jobType 退化为纯字符串以兼容老代码路径）。
 */
object AtomJobTypeUtil {

    private val PIPELINE = ServiceScopeEnum.PIPELINE.name

    /**
     * 从 serviceScopeConfigs 构建 JOB_TYPE 字段值。
     *
     * - 仅 PIPELINE + 单 jobType → 返回纯字符串（向后兼容）
     * - 其他情况 → 返回 V2 JSON（scope → list<jobType>）
     */
    fun buildJobTypeMap(
        serviceScopeConfigs: List<ServiceScopeConfig>?,
        defaultJobType: String? = null
    ): String? {
        if (serviceScopeConfigs.isNullOrEmpty()) return defaultJobType

        val jobTypeMap = mutableMapOf<String, MutableSet<String>>()
        for (config in serviceScopeConfigs) {
            val types = config.getEffectiveJobTypes()
            if (types.isEmpty()) continue
            val scope = ServiceScopeUtil.normalize(config.serviceScope.name) ?: config.serviceScope.name
            val set = jobTypeMap.getOrPut(scope) { linkedSetOf() }
            types.forEach { set.add(it.name) }
        }

        if (jobTypeMap.isEmpty()) return defaultJobType

        // 向后兼容：仅 PIPELINE 且只有一个 jobType → 纯字符串
        val pipelineTypes = jobTypeMap[PIPELINE]
        if (jobTypeMap.size == 1 && pipelineTypes != null && pipelineTypes.size == 1) {
            return pipelineTypes.first()
        }

        return JsonUtil.toJson(jobTypeMap.mapValues { it.value.toList() }, formatted = false)
    }

    /**
     * 获取所有 scope → jobType 列表的映射（用于构建 ServiceScopeConfig）。
     */
    fun getAllJobTypes(
        jobTypeValue: String?,
        defaultJobType: String? = null
    ): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()
        if (!defaultJobType.isNullOrBlank()) {
            result[PIPELINE] = mutableListOf(defaultJobType)
        }
        if (jobTypeValue.isNullOrBlank()) return result

        val parsed = parseAll(jobTypeValue)
        if (parsed.isEmpty()) {
            // 纯字符串 → 视为 PIPELINE
            result.getOrPut(PIPELINE) { mutableListOf() }.addIfAbsent(jobTypeValue)
        } else {
            for ((scope, types) in parsed) {
                val list = result.getOrPut(scope) { mutableListOf() }
                types.forEach { list.addIfAbsent(it) }
            }
        }
        return result
    }

    /** 向列表中添加元素（仅当不存在时） */
    private fun MutableList<String>.addIfAbsent(element: String) {
        if (element !in this) add(element)
    }

    /**
     * 解析 JOB_TYPE 值，返回指定 scope 下的 jobType 列表。
     * 兼容 V1 (scope→string) 和 V2 (scope→list) 两种 JSON 格式，以及纯字符串。
     */
    private fun parseForScope(jobTypeValue: String, scope: String): List<String> {
        val raw: Any?
        try {
            raw = JsonUtil.toOrNull(jobTypeValue, Map::class.java)
        } catch (e: Exception) {
            // 不是有效 JSON → 纯字符串格式
            return if (scope == PIPELINE) listOf(jobTypeValue) else emptyList()
        }
        if (raw == null) {
            return if (scope == PIPELINE) listOf(jobTypeValue) else emptyList()
        }
        @Suppress("UNCHECKED_CAST")
        val map = raw as Map<String, Any>
        val normalizedKey = map.keys.firstOrNull { (ServiceScopeUtil.normalize(it) ?: it) == scope }
            ?: return emptyList()
        return toStringList(map[normalizedKey])
    }

    /**
     * 解析 JOB_TYPE 值，返回全量 scope → jobType 列表映射。
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseAll(jobTypeValue: String): Map<String, List<String>> {
        val raw: Map<String, Any>?
        try {
            raw = JsonUtil.toOrNull(jobTypeValue, Map::class.java) as? Map<String, Any>
        } catch (e: Exception) {
            return emptyMap()
        }
        if (raw == null) return emptyMap()
        return raw.mapNotNull { (key, value) ->
            val scope = ServiceScopeUtil.normalize(key) ?: key
            val types = toStringList(value)
            if (types.isEmpty()) null else scope to types
        }.toMap()
    }

    /** 将 V1(String) 或 V2(List) 格式的值统一转为 List<String>。 */
    private fun toStringList(value: Any?): List<String> = when (value) {
        is String -> if (value.isNotBlank()) listOf(value) else emptyList()
        is List<*> -> value.mapNotNull { (it as? String)?.takeIf(String::isNotBlank) }
        else -> emptyList()
    }
}
