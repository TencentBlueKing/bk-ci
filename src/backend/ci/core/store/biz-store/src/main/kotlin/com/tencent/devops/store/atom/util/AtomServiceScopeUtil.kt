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
 * 
 * since: 2024
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
        
        // 从 SERVICE_SCOPE 中获取
        if (!serviceScopeStr.isNullOrEmpty()) {
            try {
                val serviceScopeList = JsonUtil.toOrNull(serviceScopeStr, List::class.java)
                serviceScopeList?.forEach { scope ->
                    val scopeName = when (scope) {
                        is String -> scope
                        else -> scope.toString()
                    }
                    if (scopeName.isNotEmpty()) {
                        val normalized = ServiceScopeUtil.normalize(scopeName) ?: scopeName.uppercase()
                        serviceScopes.add(normalized)
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to parse SERVICE_SCOPE: $serviceScopeStr", e)
            }
        }
        
        // 从 CLASSIFY_ID_MAP 中获取
        if (!classifyIdMapJson.isNullOrEmpty()) {
            try {
                val classifyIdMap = JsonUtil.toOrNull(classifyIdMapJson, Map::class.java)
                classifyIdMap?.keys?.forEach { key ->
                    val scopeName = key.toString()
                    if (scopeName.isNotEmpty()) {
                        val normalized = ServiceScopeUtil.normalize(scopeName) ?: scopeName.uppercase()
                        serviceScopes.add(normalized)
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to parse CLASSIFY_ID_MAP: $classifyIdMapJson", e)
            }
        }
        
        // 如果没有找到任何服务范围，默认返回 PIPELINE
        if (serviceScopes.isEmpty()) {
            serviceScopes.add(ServiceScopeEnum.PIPELINE.name)
        }
        
        return serviceScopes.toList()
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
        if (serviceScopes.isEmpty()) {
            return null
        }
        
        // 获取所有服务范围的 JobType 映射
        val allJobTypes = AtomJobTypeUtil.getAllJobTypes(jobTypeValue, null)
        
        // 为每个服务范围构建 ServiceScopeConfig
        val serviceScopeConfigs = mutableListOf<ServiceScopeConfig>()
        for (scope in serviceScopes) {
            try {
                val serviceScopeEnum = ServiceScopeEnum.valueOf(scope)
                
                // 获取该服务范围的分类代码
                val classifyCode = getClassifyCode(serviceScopeEnum)
                
                // 获取该服务范围的 JobType
                val jobTypeName = allJobTypes[scope] ?: allJobTypes["PIPELINE"]
                val jobType = if (!jobTypeName.isNullOrEmpty()) {
                    try {
                        JobTypeEnum.valueOf(jobTypeName)
                    } catch (e: Exception) {
                        JobTypeEnum.AGENT // 默认值
                    }
                } else {
                    JobTypeEnum.AGENT // 默认值
                }
                
                // 获取该服务范围的标签ID列表
                val labelIdList = getLabelIdList(serviceScopeEnum)
                
                // 构建 ServiceScopeConfig
                if (classifyCode != null) {
                    serviceScopeConfigs.add(
                        ServiceScopeConfig(
                            serviceScope = serviceScopeEnum,
                            classifyCode = classifyCode,
                            jobType = jobType,
                            labelIdList = labelIdList
                        )
                    )
                }
            } catch (e: Exception) {
                logger.warn("Failed to build ServiceScopeConfig for scope: $scope", e)
            }
        }
        
        return serviceScopeConfigs.ifEmpty { null }
    }
}

