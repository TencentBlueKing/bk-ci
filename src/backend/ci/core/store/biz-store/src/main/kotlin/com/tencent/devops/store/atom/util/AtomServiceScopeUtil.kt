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
import com.tencent.devops.store.atom.service.AtomLabelService
import com.tencent.devops.store.common.dao.ClassifyDao
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.JobTypeConfig
import com.tencent.devops.store.pojo.common.ServiceScopeDetail
import com.tencent.devops.store.pojo.common.enums.ServiceScopeEnum
import com.tencent.devops.store.pojo.common.label.Label
import com.tencent.devops.store.util.ServiceScopeUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 插件服务范围工具类，负责解析和构建 ServiceScopeConfig / ServiceScopeDetail。
 */
@Component
class AtomServiceScopeUtil @Autowired constructor(
    private val dslContext: DSLContext,
    private val classifyDao: ClassifyDao,
    private val atomLabelService: AtomLabelService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(AtomServiceScopeUtil::class.java)
    }
    
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

    private fun resolveJobTypes(scope: String, allJobTypes: Map<String, List<String>>): List<JobTypeEnum> {
        val names =
            allJobTypes[scope] ?: allJobTypes[ServiceScopeEnum.PIPELINE.name] ?: return listOf(JobTypeEnum.AGENT)
        val result = names.mapNotNull { name ->
            try {
                JobTypeEnum.valueOf(name)
            } catch (_: Exception) {
                null
            }
        }
        return result.ifEmpty { listOf(JobTypeEnum.AGENT) }
    }

    /**
     * 根据 CLASSIFY_ID_MAP 构建 scope → classifyId 映射。
     * 各 scope 优先从 MAP 中取；PIPELINE 若不在 MAP 中则使用 pipelineClassifyIdFallback（遗留字段，仅表示流水线）。
     */
    private fun buildScopeToClassifyId(
        serviceScopes: List<String>,
        classifyIdMapJson: String?,
        pipelineClassifyIdFallback: String?
    ): Map<String, String> {
        val normalizedMap = mutableMapOf<String, String>()
        if (!classifyIdMapJson.isNullOrEmpty()) {
            try {
                @Suppress("UNCHECKED_CAST")
                val raw = JsonUtil.toOrNull(classifyIdMapJson, Map::class.java) as? Map<String, Any>
                raw?.forEach { (key, value) ->
                    val scope = ServiceScopeUtil.normalize(key) ?: key
                    val id = value.toString().takeIf { it.isNotBlank() } ?: return@forEach
                    normalizedMap[scope] = id
                }
            } catch (e: Exception) {
                logger.warn("Failed to parse CLASSIFY_ID_MAP: $classifyIdMapJson", e)
            }
        }
        return serviceScopes.mapNotNull { scope ->
            val id = normalizedMap[scope]
                ?: (if (scope == ServiceScopeEnum.PIPELINE.name) pipelineClassifyIdFallback else null)
            if (id.isNullOrBlank()) null else scope to id
        }.toMap()
    }

    /**
     * 一站式构建 ServiceScopeDetail 列表（用于接口返回报文）。
     * 封装了「解析服务范围 → 构建 scopeToClassifyId → 批量查询分类信息 → 查标签 → 组装 Detail」的完整流程。
     *
     * @param atomId 插件ID
     * @param serviceScopeStr SERVICE_SCOPE 字段值（JSON 数组）
     * @param classifyIdMapJson CLASSIFY_ID_MAP 字段值（JSON 对象）
     * @param pipelineClassifyIdFallback PIPELINE 服务范围的兜底分类ID（来自 T_ATOM.CLASSIFY_ID 列）
     * @param jobTypeValue JOB_TYPE 字段值（PIPELINE 纯字符串）
     * @param jobTypeMapValue JOB_TYPE_MAP 字段值（完整多 scope JSON），优先级高于 jobTypeValue
     * @return ServiceScopeDetail 列表
     */
    fun buildServiceScopeDetails(
        atomId: String,
        serviceScopeStr: String?,
        classifyIdMapJson: String?,
        pipelineClassifyIdFallback: String?,
        jobTypeValue: String?,
        jobTypeMapValue: String? = null,
        osValue: String? = null,
        osMapValue: String? = null
    ): List<ServiceScopeDetail>? {
        val serviceScopes = getAllServiceScopes(serviceScopeStr, classifyIdMapJson)
        if (serviceScopes.isEmpty()) return null
        val scopeToClassifyId = buildScopeToClassifyId(
            serviceScopes = serviceScopes,
            classifyIdMapJson = classifyIdMapJson,
            pipelineClassifyIdFallback = pipelineClassifyIdFallback
        )
        val classifyIds = scopeToClassifyId.values.distinct()
        val classifyInfosById = classifyDao.getClassifyInfosByIds(dslContext, classifyIds)
        val allJobTypes = AtomJobTypeUtil.getAllJobTypes(jobTypeValue, jobTypeMapValue)
        val allOs = AtomOsMapUtil.getAllOs(osValue, osMapValue, jobTypeValue)
        val details = serviceScopes.mapNotNull { scope ->
            buildSingleServiceScopeDetail(
                scope = scope,
                allJobTypes = allJobTypes,
                allOs = allOs,
                getClassifyInfo = { serviceScopeEnum ->
                    scopeToClassifyId[serviceScopeEnum.name]?.let { classifyInfosById[it] }
                },
                getLabelList = { serviceScopeEnum ->
                    atomLabelService.getLabelsByAtomId(atomId, serviceScopeEnum)
                }
            )
        }
        return details.ifEmpty { null }
    }

    private fun buildSingleServiceScopeDetail(
        scope: String,
        allJobTypes: Map<String, List<String>>,
        allOs: Map<String, List<String>>,
        getClassifyInfo: (ServiceScopeEnum) -> Pair<String, String>?,
        getLabelList: (ServiceScopeEnum) -> List<Label>?
    ): ServiceScopeDetail? {
        return try {
            val serviceScopeEnum = ServiceScopeEnum.valueOf(scope)
            val (classifyCode, classifyName) = getClassifyInfo(serviceScopeEnum) ?: return null
            val jobTypes = resolveJobTypes(scope, allJobTypes)
            val labelList = getLabelList(serviceScopeEnum)
            val jobTypeConfigs = jobTypes.map { jt ->
                JobTypeConfig(
                    jobType = jt,
                    osList = if (jt.isBuildEnv()) allOs[jt.name] else null
                )
            }
            ServiceScopeDetail(
                serviceScope = serviceScopeEnum,
                classifyCode = classifyCode,
                classifyName = classifyName,
                jobTypeConfigs = jobTypeConfigs,
                labelList = labelList
            )
        } catch (e: Exception) {
            logger.warn("Failed to build ServiceScopeDetail for scope: $scope", e)
            null
        }
    }
}
