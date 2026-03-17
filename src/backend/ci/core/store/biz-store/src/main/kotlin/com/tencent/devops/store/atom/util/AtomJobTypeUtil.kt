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
 * 插件 Job 类型工具类 —— 处理 T_ATOM.JOB_TYPE 和 T_ATOM.JOB_TYPE_MAP 字段的读写。
 *
 * 双字段存储策略（兼容多环境滚动发布）：
 * - JOB_TYPE：纯字符串，仅存 PIPELINE 范围的 Job 类型（如 "AGENT"），保持向后兼容
 * - JOB_TYPE_MAP：JSON 格式，存完整的多 scope → jobType 列表映射
 *   如 {"PIPELINE":["AGENT"],"CREATIVE_STREAM":["CREATIVE_STREAM","CLOUD_TASK"]}
 *
 * 读取时优先从 JOB_TYPE_MAP 获取完整映射，JOB_TYPE 作为 PIPELINE scope 的 fallback。
 */
object AtomJobTypeUtil {

    private val PIPELINE = ServiceScopeEnum.PIPELINE.name

    /**
     * 构建双字段写入值。
     *
     * @return JobTypeWriteResult 包含 pipelineJobType（写入 JOB_TYPE）和 jobTypeMapJson（写入 JOB_TYPE_MAP）
     */
    fun buildJobTypeFields(
        serviceScopeConfigs: List<ServiceScopeConfig>?,
        defaultJobType: String? = null
    ): JobTypeWriteResult {
        if (serviceScopeConfigs.isNullOrEmpty()) {
            return JobTypeWriteResult(
                pipelineJobType = extractPipelineJobType(defaultJobType),
                jobTypeMapJson = null
            )
        }

        val jobTypeMap = mutableMapOf<String, MutableSet<String>>()
        for (config in serviceScopeConfigs) {
            val types = config.getEffectiveJobTypes()
            if (types.isEmpty()) continue
            val scope = ServiceScopeUtil.normalize(config.serviceScope.name) ?: config.serviceScope.name
            val set = jobTypeMap.getOrPut(scope) { linkedSetOf() }
            types.forEach { set.add(it.name) }
        }

        if (jobTypeMap.isEmpty()) {
            return JobTypeWriteResult(
                pipelineJobType = extractPipelineJobType(defaultJobType),
                jobTypeMapJson = null
            )
        }

        val pipelineJobType = jobTypeMap[PIPELINE]?.firstOrNull()
            ?: extractPipelineJobType(defaultJobType)
        val jobTypeMapJson = JsonUtil.toJson(jobTypeMap.mapValues { it.value.toList() }, formatted = false)

        return JobTypeWriteResult(
            pipelineJobType = pipelineJobType,
            jobTypeMapJson = jobTypeMapJson
        )
    }

    /**
     * 获取所有 scope → jobType 列表的映射（用于构建 ServiceScopeConfig）。
     * 优先从 JOB_TYPE_MAP（JSON）获取完整映射，JOB_TYPE（纯字符串）作为 PIPELINE scope 的 fallback。
     *
     * @param jobTypeValue JOB_TYPE 字段值（纯字符串，仅 PIPELINE 范围）
     * @param jobTypeMapValue JOB_TYPE_MAP 字段值（JSON 格式），优先级高于 JOB_TYPE
     */
    fun getAllJobTypes(
        jobTypeValue: String?,
        jobTypeMapValue: String? = null
    ): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()

        if (!jobTypeMapValue.isNullOrBlank()) {
            val parsed = parseJobTypeMapJson(jobTypeMapValue)
            for ((scope, types) in parsed) {
                val list = result.getOrPut(scope) { mutableListOf() }
                types.forEach { list.addIfAbsent(it) }
            }
        }

        // JOB_TYPE 是纯字符串，直接作为 PIPELINE 的 fallback
        if (!jobTypeValue.isNullOrBlank() && !result.containsKey(PIPELINE)) {
            result.getOrPut(PIPELINE) { mutableListOf() }.addIfAbsent(jobTypeValue)
        }

        return result
    }

    private fun extractPipelineJobType(jobTypeValue: String?): String? {
        return jobTypeValue?.takeIf { it.isNotBlank() }
    }

    /** 向列表中添加元素（仅当不存在时） */
    private fun MutableList<String>.addIfAbsent(element: String) {
        if (element !in this) add(element)
    }

    /**
     * 解析 JOB_TYPE_MAP 字段的 JSON 值，返回 scope → jobType 列表映射。
     * 格式：{"PIPELINE":["AGENT"],"CREATIVE_STREAM":["CREATIVE_STREAM","CLOUD_TASK"]}
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseJobTypeMapJson(json: String): Map<String, List<String>> {
        val raw: Map<String, Any>?
        try {
            raw = JsonUtil.toOrNull(json, Map::class.java) as? Map<String, Any>
        } catch (ignored: Throwable) {
            return emptyMap()
        }
        if (raw == null) return emptyMap()
        return raw.mapNotNull { (key, value) ->
            val scope = ServiceScopeUtil.normalize(key) ?: key
            val types = when (value) {
                is String -> if (value.isNotBlank()) listOf(value) else emptyList()
                is List<*> -> value.mapNotNull { (it as? String)?.takeIf(String::isNotBlank) }
                else -> emptyList()
            }
            if (types.isEmpty()) null else scope to types
        }.toMap()
    }
}

/**
 * 双字段写入值对象，分别对应 T_ATOM.JOB_TYPE 和 T_ATOM.JOB_TYPE_MAP。
 */
data class JobTypeWriteResult(
    /** 写入 JOB_TYPE 字段：PIPELINE 范围的纯字符串 Job 类型（如 "AGENT"），保持向后兼容 */
    val pipelineJobType: String?,
    /** 写入 JOB_TYPE_MAP 字段：完整的多 scope JSON 映射，null 表示无需更新 */
    val jobTypeMapJson: String?
)
