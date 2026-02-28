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
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.atom.util

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.ServiceScopeConfig
import com.tencent.devops.store.pojo.common.enums.ServiceScopeEnum
import com.tencent.devops.store.util.ServiceScopeUtil
import org.slf4j.LoggerFactory

/**
 * 插件服务范围工具类
 */
object AtomServiceScopeUtil {
    
    private val logger = LoggerFactory.getLogger(AtomServiceScopeUtil::class.java)
    
    /**
     * 从 SERVICE_SCOPE 和 CLASSIFY_ID_MAP 中获取所有服务范围
     *
     * @param serviceScopeStr SERVICE_SCOPE 字段值（JSON 数组）
     * @param classifyIdMapJson CLASSIFY_ID_MAP 字段值（JSON 对象）
     * @return 服务范围名称列表（大写格式）
     */
    fun getAllServiceScopes(
        serviceScopeStr: String?,
        classifyIdMapJson: String?
    ): List<String> {
        val serviceScopes = mutableSetOf<String>()
        serviceScopes.addAll(parseServiceScopesFromJson(serviceScopeStr))
        serviceScopes.addAll(parseServiceScopesFromClassifyIdMap(classifyIdMapJson))
        if (serviceScopes.isEmpty()) {
            serviceScopes.add(ServiceScopeEnum.PIPELINE.name)
        }
        return serviceScopes.toList()
    }

    private fun parseServiceScopesFromJson(serviceScopeStr: String?): Set<String> {
        if (serviceScopeStr.isNullOrEmpty()) return emptySet()
        return try {
            val list = JsonUtil.toOrNull(serviceScopeStr, List::class.java) ?: return emptySet()
            list.mapNotNull { scope ->
                val scopeName = (scope as? String) ?: scope.toString()
                if (scopeName.isEmpty()) null else ServiceScopeUtil.normalize(scopeName) ?: scopeName.uppercase()
            }.toSet()
        } catch (e: Exception) {
            logger.warn("Failed to parse SERVICE_SCOPE: $serviceScopeStr", e)
            emptySet()
        }
    }

    private fun parseServiceScopesFromClassifyIdMap(classifyIdMapJson: String?): Set<String> {
        if (classifyIdMapJson.isNullOrEmpty()) return emptySet()
        return try {
            val map = JsonUtil.toOrNull(classifyIdMapJson, Map::class.java) ?: return emptySet()
            map.keys.mapNotNull { key ->
                val scopeName = key.toString()
                if (scopeName.isEmpty()) null else ServiceScopeUtil.normalize(scopeName) ?: scopeName.uppercase()
            }.toSet()
        } catch (e: Exception) {
            logger.warn("Failed to parse CLASSIFY_ID_MAP: $classifyIdMapJson", e)
            emptySet()
        }
    }
    
    /**
     * 构建 ServiceScopeConfig 的核心逻辑
     *
     * @param serviceScopes 服务范围列表
     * @param jobTypeValue JOB_TYPE 字段值
     * @param getClassifyCode 获取分类代码的函数，参数为服务范围枚举，返回分类代码
     * @param getLabelIdList 获取标签ID列表的函数，参数为服务范围枚举，返回标签ID列表
     * @return ServiceScopeConfig 列表
     */
    fun buildServiceScopeConfigs(
        serviceScopes: List<String>,
        jobTypeValue: String?,
        getClassifyCode: (ServiceScopeEnum) -> String?,
        getLabelIdList: (ServiceScopeEnum) -> List<String>?
    ): List<ServiceScopeConfig>? {
        if (serviceScopes.isEmpty()) return null
        val allJobTypes = AtomJobTypeUtil.getAllJobTypes(jobTypeValue, null)
        val configs = serviceScopes.mapNotNull { scope ->
            buildSingleServiceScopeConfig(
                scope = scope,
                allJobTypes = allJobTypes,
                getClassifyCode = getClassifyCode,
                getLabelIdList = getLabelIdList
            )
        }
        return configs.ifEmpty { null }
    }

    private fun resolveJobTypes(scope: String, allJobTypes: Map<String, List<String>>): List<JobTypeEnum> {
        val names = allJobTypes[scope] ?: allJobTypes[ServiceScopeEnum.PIPELINE.name] ?: return listOf(JobTypeEnum.AGENT)
        val result = names.mapNotNull { name ->
            try { JobTypeEnum.valueOf(name) } catch (_: Exception) { null }
        }
        return result.ifEmpty { listOf(JobTypeEnum.AGENT) }
    }

    private fun buildSingleServiceScopeConfig(
        scope: String,
        allJobTypes: Map<String, List<String>>,
        getClassifyCode: (ServiceScopeEnum) -> String?,
        getLabelIdList: (ServiceScopeEnum) -> List<String>?
    ): ServiceScopeConfig? {
        return try {
            val serviceScopeEnum = ServiceScopeEnum.valueOf(scope)
            val classifyCode = getClassifyCode(serviceScopeEnum) ?: return null
            val jobTypes = resolveJobTypes(scope, allJobTypes)
            val labelIdList = getLabelIdList(serviceScopeEnum)
            ServiceScopeConfig(
                serviceScope = serviceScopeEnum,
                classifyCode = classifyCode,
                jobTypes = jobTypes,
                labelIdList = labelIdList
            )
        } catch (e: Exception) {
            logger.warn("Failed to build ServiceScopeConfig for scope: $scope", e)
            null
        }
    }
}
