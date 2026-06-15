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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.atom.util

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.ServiceScopeConfig

/**
 * 插件 OS 映射工具类 —— 处理 T_ATOM.OS 和 T_ATOM.OS_MAP 字段的读写。
 *
 * 双字段存储策略（与 JOB_TYPE / JOB_TYPE_MAP 对称）：
 * - OS：JSON 数组，仅存 PIPELINE scope 编译环境 jobType（AGENT）的 OS 列表，保持向后兼容
 * - OS_MAP：JSON 对象，以 jobType 为 key 存完整的 jobType → OS 列表映射
 *   如 {"AGENT":["WINDOWS","LINUX","MACOS"],"CREATIVE_STREAM":["WINDOWS"]}
 *   无编译环境 jobType（AGENT_LESS、CLOUD_TASK）的 OS 恒为空，不写入 OS_MAP
 *
 * 读取时优先从 OS_MAP 获取完整映射，OS 作为 PIPELINE scope AGENT jobType 的 fallback。
 */
object AtomOsMapUtil {

    /**
     * 从 ServiceScopeConfig 列表构建双字段写入值。
     *
     * @param serviceScopeConfigs 服务范围配置列表（包含每个 scope 的 jobType 及其 osMap）
     * @param defaultOs 默认 OS 列表（向后兼容：来自请求体的顶层 os 字段，用于 PIPELINE scope）
     * @return OsWriteResult 包含 pipelineOs（写入 OS）和 osMapJson（写入 OS_MAP）
     */
    fun buildOsFields(
        serviceScopeConfigs: List<ServiceScopeConfig>?,
        defaultOs: List<String>? = null
    ): OsWriteResult {
        if (serviceScopeConfigs.isNullOrEmpty()) {
            return OsWriteResult(
                pipelineOs = defaultOs ?: emptyList(),
                osMapJson = null
            )
        }
        // 遍历所有 config 的 jobTypeConfigs，仅对编译环境 jobType 构建 OS 映射：
        // 有 osList 则使用前端传的值，否则回退到 jobType 的默认 OS
        val osMap = serviceScopeConfigs
            .flatMap { it.jobTypeConfigs }
            .filter { it.jobType.isBuildEnv() }
            .mapNotNull { jtConfig ->
                val osList = jtConfig.osList?.takeIf { it.isNotEmpty() }
                    ?: jtConfig.jobType.getDefaultOs()
                osList?.let { jtConfig.jobType.name to it.sorted() }
            }
            .toMap()
        val pipelineOs = osMap[JobTypeEnum.AGENT.name] ?: defaultOs ?: emptyList()
        val osMapJson = osMap.takeIf { it.isNotEmpty() }
            ?.let { JsonUtil.toJson(it, formatted = false) }
        return OsWriteResult(pipelineOs = pipelineOs, osMapJson = osMapJson)
    }

    /**
     * 读取所有 jobType 的 OS 映射。
     * 优先从 OS_MAP（JSON 对象）获取完整映射，OS（JSON 数组）作为 AGENT jobType 的 fallback。
     *
     * @param osValue OS 字段值（JSON 数组，仅 PIPELINE scope AGENT jobType 的 OS）
     * @param osMapValue OS_MAP 字段值（JSON 对象），优先级高于 osValue
     * @param jobTypeValue JOB_TYPE 字段值（用于确定 OS 字段对应的 jobType，默认 AGENT）
     * @return jobType → OS 列表的映射
     */
    fun getAllOs(
        osValue: String?,
        osMapValue: String? = null,
        jobTypeValue: String? = null
    ): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()

        if (!osMapValue.isNullOrBlank()) {
            val parsed = parseOsMapJson(osMapValue)
            result.putAll(parsed)
        }

        if (!result.containsKey(JobTypeEnum.AGENT.name) && !osValue.isNullOrBlank()) {
            val osList = parseOsListJson(osValue)
            if (osList.isNotEmpty()) {
                val key = jobTypeValue?.takeIf { it.isNotBlank() } ?: JobTypeEnum.AGENT.name
                result[key] = osList
            }
        }

        return result
    }

    /**
     * 根据 jobType 获取其对应的 OS 列表。
     * 优先从 OS_MAP 获取，回退到 OS 字段。
     */
    fun getOsByJobType(
        jobType: String,
        osValue: String?,
        osMapValue: String? = null
    ): List<String> {
        val allOs = getAllOs(osValue, osMapValue)
        return allOs[jobType] ?: emptyList()
    }

    /**
     * 判断是否有任意编译环境 jobType（不管 OS 是否为空）。
     * 用于 classType 推导时，区分「有编译环境 jobType」和「全部无编译环境」。
     */
    fun hasAnyBuildEnvJobType(serviceScopeConfigs: List<ServiceScopeConfig>?): Boolean {
        if (serviceScopeConfigs.isNullOrEmpty()) return false
        return serviceScopeConfigs.any { config ->
            config.getEffectiveJobTypes().any { it.isBuildEnv() }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseOsMapJson(json: String): Map<String, List<String>> {
        return try {
            val raw = JsonUtil.toOrNull(json, Map::class.java) as? Map<String, Any> ?: return emptyMap()
            raw.mapNotNull { (key, value) ->
                val osList = when (value) {
                    is List<*> -> value.mapNotNull { (it as? String)?.takeIf(String::isNotBlank) }
                    is String -> if (value.isNotBlank()) listOf(value) else emptyList()
                    else -> emptyList()
                }
                if (osList.isEmpty()) null else key to osList
            }.toMap()
        } catch (ignored: Throwable) {
            emptyMap()
        }
    }

    private fun parseOsListJson(json: String): List<String> {
        return try {
            JsonUtil.to(json, object : TypeReference<List<String>>() {}).filter { it.isNotBlank() }
        } catch (ignored: Throwable) {
            emptyList()
        }
    }
}

/**
 * 双字段写入值对象，分别对应 T_ATOM.OS 和 T_ATOM.OS_MAP。
 */
data class OsWriteResult(
    /** 写入 OS 字段：PIPELINE scope AGENT jobType 的 OS 列表（JSON 数组），保持向后兼容 */
    val pipelineOs: List<String>,
    /** 写入 OS_MAP 字段：完整的 jobType → OS 映射 JSON，null 表示无需更新 */
    val osMapJson: String?
)
