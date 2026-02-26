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
import org.slf4j.LoggerFactory

/**
 * 插件Job类型工具类
 * 用于处理多服务范围下的Job类型映射
 * 
 * T_ATOM 表的 JOB_TYPE 字段目前只针对流水线服务范围，
 * 现在需要支持不同服务范围对应不同的Job类型。
 * 
 * 解决方案：
 * - JOB_TYPE 字段保留（向后兼容，默认为 PIPELINE 的Job类型）
 * - 使用 serviceScopeConfigs 构建 jobTypeMap，存储在 JOB_TYPE 字段中（JSON格式）
 * - 格式：{"PIPELINE":"AGENT","CREATIVE_STREAM":"AGENT_LESS"}
 * - 如果只有一个服务范围，直接存储字符串（兼容老数据）
 */
object AtomJobTypeUtil {
    
    private val logger = LoggerFactory.getLogger(AtomJobTypeUtil::class.java)
    
    /**
     * 从 serviceScopeConfigs 构建 jobTypeMap JSON 字符串
     * 
     * @param serviceScopeConfigs 服务范围配置列表
     * @param defaultJobType 默认Job类型（JOB_TYPE字段值，用于兼容老数据）
     * @return jobTypeMap JSON 字符串，如果只有一个服务范围且是PIPELINE，返回字符串；否则返回JSON对象
     */
    fun buildJobTypeMap(
        serviceScopeConfigs: List<ServiceScopeConfig>?,
        defaultJobType: String? = null
    ): String? {
        if (serviceScopeConfigs.isNullOrEmpty()) {
            // 如果没有配置，返回默认值（兼容老数据）
            return defaultJobType
        }
        
        // 构建 jobTypeMap
        val jobTypeMap = mutableMapOf<String, String>()
        serviceScopeConfigs.forEach { config ->
            config.jobType?.let { jobType ->
                val normalizedScope = ServiceScopeUtil.normalize(config.serviceScope.name) ?: config.serviceScope.name
                jobTypeMap[normalizedScope] = jobType.name
            }
        }
        
        // 如果只有一个服务范围且是PIPELINE，返回字符串（兼容老数据）
        val pipelineScope = ServiceScopeEnum.PIPELINE.name
        if (jobTypeMap.size == 1 && jobTypeMap.containsKey(pipelineScope)) {
            return jobTypeMap[pipelineScope]
        }
        
        // 多个服务范围，返回JSON格式
        return JsonUtil.toJson(jobTypeMap, formatted = false)
    }
    
    /**
     * 获取指定服务范围的Job类型
     * 
     * @param jobTypeValue JOB_TYPE字段值（可能是字符串或JSON对象）
     * @param defaultJobType 默认Job类型（用于兼容老数据）
     * @param serviceScope 服务范围，如 "PIPELINE"、"CREATIVE_STREAM"，如果为null则返回默认Job类型
     * @return Job类型名称，如果未找到则返回默认值（PIPELINE）或null
     */
    @Suppress("UNCHECKED_CAST")
    fun getJobType(
        jobTypeValue: String?,
        defaultJobType: String? = null,
        serviceScope: String? = null
    ): String? {
        // 如果没有指定服务范围，返回默认Job类型（兼容老逻辑）
        if (serviceScope.isNullOrBlank()) {
            return defaultJobType ?: jobTypeValue
        }
        
        // 标准化服务范围（统一转换为大写）
        val normalizedScope = ServiceScopeUtil.normalize(serviceScope) ?: serviceScope
        
        // 如果 JOB_TYPE 为空，使用默认Job类型（兼容老数据）
        if (jobTypeValue.isNullOrBlank()) {
            // 如果是 PIPELINE，返回默认Job类型；否则返回 null
            val pipelineScope = ServiceScopeEnum.PIPELINE.name
            return if (normalizedScope == pipelineScope) defaultJobType else null
        }
        
        // 尝试解析为JSON对象（多服务范围）
        val pipelineScope = ServiceScopeEnum.PIPELINE.name
        try {
            val jobTypeMap = JsonUtil.toOrNull(jobTypeValue, Map::class.java) as? Map<String, String>
            if (jobTypeMap != null) {
                // 是JSON对象，从映射中获取
                val jobType = jobTypeMap[normalizedScope]
                if (!jobType.isNullOrBlank()) {
                    return jobType
                }
                // 如果未找到，且是 PIPELINE，返回默认Job类型
                return if (normalizedScope == pipelineScope) defaultJobType else null
            }
        } catch (e: Exception) {
            // 不是JSON对象，可能是字符串（老数据格式）
            logger.debug("JOB_TYPE is not JSON format, treating as string: $jobTypeValue")
        }
        
        // 是字符串格式（老数据），只有PIPELINE服务范围才返回
        if (normalizedScope == pipelineScope) {
            return jobTypeValue
        }
        
        // 其他服务范围，返回null
        return null
    }
    
    /**
     * 获取所有服务范围的Job类型映射
     * 
     * @param jobTypeValue JOB_TYPE字段值（可能是字符串或JSON对象）
     * @param defaultJobType 默认Job类型（用于兼容老数据）
     * @return 所有服务范围的Job类型映射，key为大写格式的服务范围，value为Job类型名称
     */
    fun getAllJobTypes(
        jobTypeValue: String?,
        defaultJobType: String? = null
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val pipelineScope = ServiceScopeEnum.PIPELINE.name

        // 设置默认值
        defaultJobType?.takeIf { it.isNotBlank() }?.let {
            result[pipelineScope] = it
        }

        return when {
            jobTypeValue.isNullOrBlank() -> result
            else -> parseJobTypeValue(jobTypeValue, result)
        }
    }

    private fun parseJobTypeValue(
        jobTypeValue: String,
        result: MutableMap<String, String>
    ): Map<String, String> {
        return try {
            val parsedMap = parseJobTypeJsonOrString(jobTypeValue)
            result.apply {
                if (parsedMap.isNotEmpty()) {
                    putAll(parsedMap)
                } else {
                    // 解析为空时，将原始值作为 PIPELINE
                    putIfValueNotBlank(ServiceScopeEnum.PIPELINE.name, jobTypeValue)
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse jobTypeValue: $jobTypeValue", e)
            result // 解析失败，返回已有结果（可能包含默认值）
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseJobTypeJsonOrString(value: String): Map<String, String> {
        val rawMap = JsonUtil.toOrNull(value, Map::class.java) as? Map<String, String>
            ?: return emptyMap()
        return rawMap
            .filterValues { it.isNotBlank() }
            .mapKeys { (scope, _) -> ServiceScopeUtil.normalize(scope) ?: scope }
    }

    private fun MutableMap<String, String>.putIfValueNotBlank(key: String, value: String?) {
        value?.takeIf { it.isNotBlank() }?.let {
            this[key] = it
        }
    }
}
