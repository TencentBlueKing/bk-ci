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

package com.tencent.devops.process.service.`var`

import com.tencent.devops.common.api.constant.CommonMessageCode.ERROR_INVALID_PARAM_
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.pojo.VarRefDetail
import com.tencent.devops.process.dao.`var`.PublicVarDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.dao.`var`.PublicVarReferInfoDao
import com.tencent.devops.process.pojo.`var`.PublicGroupKey
import com.tencent.devops.process.pojo.`var`.VarGroupProcessContext
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarReferPO
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PublicVarReferInfoService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val publicVarDao: PublicVarDao,
    private val publicVarGroupDao: PublicVarGroupDao,
    private val publicVarReferInfoDao: PublicVarReferInfoDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarReferInfoService::class.java)
    }

    /**
     * 处理公共变量组引用
     * 主要功能：
     * 1. 根据Model中的publicVarGroups和varRefDetails，同步T_RESOURCE_PUBLIC_VAR_REFER_INFO表的引用记录
     * 2. 更新T_RESOURCE_PUBLIC_VAR表的REFER_COUNT字段
     * 3. 处理变量组的新增、删除和更新操作
     *
     * @param userId 用户ID
     * @param projectId 项目ID
     * @param model 流水线模型
     * @param resourceId 资源ID（如流水线ID）
     * @param resourceType 资源类型
     * @param resourceVersion 资源版本
     * @param varRefDetails 变量引用详情列表
     */
    fun handlePublicVarGroupReferences(
        userId: String,
        model: Model,
        varRefDetails: List<VarRefDetail>
    ) {
        if (varRefDetails.isEmpty()) {
            return
        }
        
        val firstDetail = varRefDetails.first()
        val projectId = firstDetail.projectId
        val resourceId = firstDetail.resourceId
        val resourceType = firstDetail.resourceType
        val resourceVersion = firstDetail.referVersion
        
        logger.info("Start handling public var group references for resource: $resourceId|$resourceVersion")

        val referType = PublicVerGroupReferenceTypeEnum.valueOf(resourceType)

        // 查询已存在的引用记录
        val existingGroupKeys = publicVarReferInfoDao.listVarGroupsByReferIdAndVersion(
            dslContext = dslContext,
            projectId = projectId,
            referId = resourceId,
            referType = referType,
            referVersion = resourceVersion
        )

        // 获取Model中的变量组列表
        val modelVarGroups = model.publicVarGroups ?: emptyList()
        // 如果Model中没有变量组，清理所有已存在的引用记录
        if (modelVarGroups.isEmpty()) {
            cleanupRemovedVarGroupReferences(
                context = dslContext,
                projectId = projectId,
                resourceId = resourceId,
                referType = referType,
                resourceVersion = resourceVersion,
                groupsToCleanup = null
            )
            return
        }

        // 获取流水线变量名集合(用于排除与公共变量重名的情况)
        val pipelineVarNames = model.getTriggerContainer().params
            .filter { it.varGroupName.isNullOrBlank() }
            .map { it.id }
            .toSet()
        
        // 构建公共变量映射和被引用变量集合
        val referencedVarNames = varRefDetails.map { it.varName }.toSet()
        val publicVarMap = buildPublicVarMap(model)

        // 清理不在Model中的变量组引用
        cleanupObsoleteGroupReferences(
            projectId = projectId,
            resourceId = resourceId,
            referType = referType,
            resourceVersion = resourceVersion,
            modelVarGroups = modelVarGroups,
            existingGroupKeys = existingGroupKeys
        )

        // 批量查询最新版本号
        val latestVersionMap = queryLatestVersions(projectId, modelVarGroups)

        // 批量查询已存在的变量名
        val allExistingVarNames = queryExistingVarNames(
            projectId = projectId,
            resourceId = resourceId,
            referType = referType,
            resourceVersion = resourceVersion
        )

        // 构建处理上下文
        val context = VarGroupProcessContext(
            userId = userId,
            projectId = projectId,
            resourceId = resourceId,
            referType = referType,
            resourceVersion = resourceVersion,
            modelVarGroups = modelVarGroups,
            publicVarMap = publicVarMap,
            referencedVarNames = referencedVarNames,
            latestVersionMap = latestVersionMap,
            allExistingVarNames = allExistingVarNames,
            pipelineVarNames = pipelineVarNames
        )

        // 计算并执行删除操作
        calculateAndExecuteDelete(context)

        // 计算需要新增的变量引用记录
        val referRecordsToAdd = calculateVarsToAdd(context)

        // 执行批量插入（在事务内）
        if (referRecordsToAdd.isNotEmpty()) {
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                publicVarReferInfoDao.batchSave(dslContext = transactionContext, pipelinePublicVarReferPOs = referRecordsToAdd)
            }
        }

        // 计算需要更新引用计数的变量
        val varsNeedUpdateCount = calculateVarsNeedUpdateCount(context)

        // 批量更新引用计数
        batchUpdateReferCounts(projectId, varsNeedUpdateCount)
    }

    /**
     * 构建公共变量映射
     * 从流水线模型中提取所有公共变量，按变量组名分组
     * @param model 流水线模型
     * @return Map<varGroupName, Set<varName>> 变量组名到变量名集合的映射
     */
    private fun buildPublicVarMap(model: Model): Map<String, Set<String>> {
        val triggerContainer = model.getTriggerContainer()
        return triggerContainer.params
            .filter { !it.varGroupName.isNullOrBlank() }
            .groupBy { it.varGroupName!! }
            .mapValues { (_, vars) -> vars.map { it.id }.toSet() }
    }

    /**
     * 清理不在Model中的变量组引用
     * 对比数据库中的引用记录与Model中的变量组，删除已不存在的引用
     * @param projectId 项目ID
     * @param resourceId 资源ID
     * @param referType 引用类型
     * @param resourceVersion 资源版本
     * @param modelVarGroups Model中的变量组列表
     * @param existingGroupKeys 数据库中已存在的变量组键集合
     */
    private fun cleanupObsoleteGroupReferences(
        projectId: String,
        resourceId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        resourceVersion: Int,
        modelVarGroups: List<*>,
        existingGroupKeys: Set<PublicGroupKey>
    ) {
        // 将Model中的变量组转换为PublicGroupKey集合
        val modelGroupKeys = modelVarGroups
            .map { it as com.tencent.devops.common.pipeline.pojo.PublicVarGroupRef }
            .map { PublicGroupKey(it.groupName, it.version) }
            .toSet()

        // 找出需要清理的变量组（存在于数据库但不在Model中）
        val groupsToCleanup = existingGroupKeys - modelGroupKeys

        if (groupsToCleanup.isNotEmpty()) {
            cleanupRemovedVarGroupReferences(
                context = dslContext,
                projectId = projectId,
                resourceId = resourceId,
                referType = referType,
                resourceVersion = resourceVersion,
                groupsToCleanup = groupsToCleanup
            )
        }
    }

    /**
     * 批量查询最新版本号
     */
    private fun queryLatestVersions(
        projectId: String,
        modelVarGroups: List<*>
    ): Map<String, Int> {
        val groupsNeedLatestVersion = modelVarGroups
            .map { it as com.tencent.devops.common.pipeline.pojo.PublicVarGroupRef }
            .filter { it.version == null }
            .map { it.groupName }

        return if (groupsNeedLatestVersion.isNotEmpty()) {
            publicVarGroupDao.getLatestVersionsByGroupNames(
                dslContext = dslContext,
                projectId = projectId,
                groupNames = groupsNeedLatestVersion
            )
        } else {
            emptyMap()
        }
    }

    /**
     * 批量查询已存在的变量名
     * @return Map<PublicGroupKey, Set<varName>>
     */
    private fun queryExistingVarNames(
        projectId: String,
        resourceId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        resourceVersion: Int
    ): Map<PublicGroupKey, Set<String>> {
        return publicVarReferInfoDao.listVarReferInfoByReferIdAndVersion(
            dslContext = dslContext,
            projectId = projectId,
            referId = resourceId,
            referType = referType,
            referVersion = resourceVersion
        ).groupBy { PublicGroupKey(it.groupName, if (it.version == -1) null else it.version) }
            .mapValues { (_, records) -> records.map { it.varName }.toSet() }
    }

    /**
     * 计算并执行删除操作
     */
    private fun calculateAndExecuteDelete(
        context: VarGroupProcessContext
    ) {
        context.modelVarGroups.forEach { varGroup ->
            val group = varGroup
            val groupKey = PublicGroupKey(group.groupName, group.version)
            val groupName = groupKey.groupName

            // 获取该变量组被引用的变量
            val referencedVarNameSet = getReferencedVarsForGroup(
                groupName = groupName,
                publicVarMap = context.publicVarMap,
                referencedVarNames = context.referencedVarNames,
                pipelineVarNames = context.pipelineVarNames
            )
            
            if (referencedVarNameSet.isEmpty()) {
                return@forEach
            }

            // 获取已存在的变量名
            val existingVarNames = context.allExistingVarNames[groupKey] ?: emptySet()

            val diffResult = calculateVarGroupDiff(
                referencedVars = referencedVarNameSet,
                existingVars = existingVarNames
            )

            // 直接执行删除操作
            if (diffResult.varsToRemove.isNotEmpty()) {
                publicVarReferInfoDao.deleteByReferIdAndGroupAndVarNames(
                    dslContext = dslContext,
                    projectId = context.projectId,
                    referId = context.resourceId,
                    referType = context.referType,
                    groupName = groupName,
                    referVersion = context.resourceVersion,
                    varNames = diffResult.varsToRemove.toList()
                )
            }
        }
    }

    /**
     * 计算需要新增的变量引用记录
     * @param context 变量组处理上下文
     * @return 新增的变量引用记录列表
     */
    private fun calculateVarsToAdd(
        context: VarGroupProcessContext
    ): List<ResourcePublicVarReferPO> {
        val referRecordsToAdd = mutableListOf<ResourcePublicVarReferPO>()

        context.modelVarGroups.forEach { varGroup ->
            val group = varGroup
            val groupKey = PublicGroupKey(group.groupName, group.version)
            val groupName = groupKey.groupName
            val versionForDb = groupKey.getVersionForDb()

            // 获取该变量组被引用的变量
            val referencedVarNameSet = getReferencedVarsForGroup(
                groupName = groupName,
                publicVarMap = context.publicVarMap,
                referencedVarNames = context.referencedVarNames,
                pipelineVarNames = context.pipelineVarNames
            )
            
            if (referencedVarNameSet.isEmpty()) {
                return@forEach
            }

            // 获取已存在的变量名
            val existingVarNames = context.allExistingVarNames[groupKey] ?: emptySet()

            val diffResult = calculateVarGroupDiff(
                referencedVars = referencedVarNameSet,
                existingVars = existingVarNames
            )

            // 收集需要新增的变量
            if (diffResult.varsToAdd.isNotEmpty()) {
                // 批量生成ID
                val segmentIds = client.get(ServiceAllocIdResource::class)
                    .batchGenerateSegmentId("T_RESOURCE_PUBLIC_VAR_REFER_INFO", diffResult.varsToAdd.size).data
                if (segmentIds.isNullOrEmpty()) {
                    throw ErrorCodeException(
                        errorCode = ERROR_INVALID_PARAM_,
                        params = arrayOf("Failed to generate segment IDs for var refer info")
                    )
                }

                val currentTime = LocalDateTime.now()
                val newRecords = diffResult.varsToAdd.mapIndexed { index, varName ->
                    ResourcePublicVarReferPO(
                        id = segmentIds[index] ?: 0,
                        projectId = context.projectId,
                        groupName = groupName,
                        varName = varName,
                        version = versionForDb,
                        referId = context.resourceId,
                        referType = context.referType,
                        referVersion = context.resourceVersion,
                        referVersionName = "v${context.resourceVersion}",
                        creator = context.userId,
                        modifier = context.userId,
                        createTime = currentTime,
                        updateTime = currentTime
                    )
                }
                referRecordsToAdd.addAll(newRecords)
            }
        }

        return referRecordsToAdd
    }

    /**
     * 计算需要更新引用计数的变量
     * @param context 变量组处理上下文
     * @return 需要更新计数的变量映射 Map<PublicGroupKey, Set<varName>>
     */
    private fun calculateVarsNeedUpdateCount(
        context: VarGroupProcessContext
    ): Map<PublicGroupKey, MutableSet<String>> {
        val varsNeedUpdateCount = mutableMapOf<PublicGroupKey, MutableSet<String>>()

        context.modelVarGroups.forEach { varGroup ->
            val group = varGroup
            val groupKey = PublicGroupKey(group.groupName, group.version)
            val groupName = groupKey.groupName

            // 获取该变量组被引用的变量
            val referencedVarNameSet = getReferencedVarsForGroup(
                groupName = groupName,
                publicVarMap = context.publicVarMap,
                referencedVarNames = context.referencedVarNames,
                pipelineVarNames = context.pipelineVarNames
            )
            
            if (referencedVarNameSet.isEmpty()) {
                return@forEach
            }

            // 获取已存在的变量名
            val existingVarNames = context.allExistingVarNames[groupKey] ?: emptySet()

            val diffResult = calculateVarGroupDiff(
                referencedVars = referencedVarNameSet,
                existingVars = existingVarNames
            )

            // 收集需要更新计数的变量（包括删除和新增的）
            val varsChanged = diffResult.varsToRemove + diffResult.varsToAdd
            if (varsChanged.isNotEmpty()) {
                varsNeedUpdateCount.getOrPut(groupKey) { mutableSetOf() }.addAll(varsChanged)
            }
        }

        return varsNeedUpdateCount
    }

    /**
     * 计算变量组的差异
     * @param referencedVars 当前被引用的变量集合
     * @param existingVars 已存在的变量集合
     * @return VarGroupDiffResult 差异结果
     */
    private fun calculateVarGroupDiff(
        referencedVars: Set<String>,
        existingVars: Set<String>
    ): com.tencent.devops.process.pojo.`var`.VarGroupDiffResult {
        return com.tencent.devops.process.pojo.`var`.VarGroupDiffResult(
            varsToRemove = existingVars - referencedVars,  // 存在但不再被引用的变量
            varsToUpdate = existingVars.intersect(referencedVars),  // 继续被引用的变量（保持不变）
            varsToAdd = referencedVars - existingVars  // 新增被引用的变量
        )
    }

    /**
     * 获取变量组中被引用的变量
     */
    private fun getReferencedVarsForGroup(
        groupName: String,
        publicVarMap: Map<String, Set<String>>,
        referencedVarNames: Set<String>,
        pipelineVarNames: Set<String> = emptySet()
    ): Set<String> {
        val groupVarNames = publicVarMap[groupName] ?: emptySet()
        // 过滤出被引用的变量，并排除与流水线变量重名的公共变量
        return groupVarNames
            .filter { referencedVarNames.contains(it) }
            .filterNot { pipelineVarNames.contains(it) }
            .toSet()
    }





    /**
     * 批量更新引用计数
     */
    private fun batchUpdateReferCounts(
        projectId: String,
        varsNeedUpdateCount: Map<PublicGroupKey, MutableSet<String>>
    ) {
        if (varsNeedUpdateCount.isEmpty()) {
            return
        }

        varsNeedUpdateCount.forEach { (groupKey, varNames) ->
            // 如果版本为 null（动态版本），传递 null 以触发最新版本 + -1 版本的统计逻辑
            updateVarReferCount(
                projectId = projectId,
                groupName = groupKey.groupName,
                varNames = varNames.toList(),
                version = groupKey.version
            )
        }
    }

    /**
     * 更新指定变量组中指定变量的引用计数
     * 计数原则：referId + varName 的唯一组合计数为1，跨版本去重
     * @param projectId 项目ID
     * @param groupName 变量组名称
     * @param varNames 变量名列表
     * @param version 变量组版本号（可选，为null时更新最新版本）
     */
    fun updateVarReferCount(
        projectId: String,
        groupName: String,
        varNames: List<String>,
        version: Int? = null
    ) {
        if (varNames.isEmpty()) {
            logger.info("No variables to update for group: $groupName")
            return
        }

        // 确定实际版本号
        val actualVersion = version ?: publicVarGroupDao.getLatestVersionByGroupName(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName
        ) ?: run {
            logger.warn("Cannot find version for group: $groupName")
            return
        }

        logger.info("Updating refer count for group: $groupName, version: $actualVersion, vars: $varNames")

        // 对每个变量查询实际引用数并更新
        varNames.forEach { varName ->
            // 只统计明确指定该版本号的引用
            val actualReferCount = publicVarReferInfoDao.countDistinctReferIdsByVar(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName,
                version = actualVersion,
                varName = varName
            )

            // 直接更新为实际引用数
            publicVarDao.updateReferCountDirectly(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName,
                version = actualVersion,
                varName = varName,
                referCount = actualReferCount
            )

            logger.info("Updated refer count for var: $varName, count: $actualReferCount")
        }
    }

    /**
     * 清理已移除的变量组引用
     * 删除引用记录后，重新计算受影响变量的实际引用计数
     *
     * @param context DSL上下文
     * @param projectId 项目ID
     * @param resourceId 资源ID
     * @param referType 引用类型
     * @param resourceVersion 资源版本
     * @param groupsToCleanup 需要清理的变量组集合（PublicGroupKey类型），为null时清理该资源的所有变量引用
     */
    fun cleanupRemovedVarGroupReferences(
        context: DSLContext,
        projectId: String,
        resourceId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        resourceVersion: Int,
        groupsToCleanup: Set<PublicGroupKey>?
    ) {
        // 收集需要重新计算引用计数的变量组
        // 使用 PublicGroupKey 作为键，确保类型安全
        val groupsNeedUpdateCount = mutableMapOf<PublicGroupKey, List<String>>()

        if (groupsToCleanup == null) {
            // 清理该资源的所有变量引用
            logger.info("Cleaning up all var group references for resource: $resourceId, version: $resourceVersion")

            // 查询该资源的所有引用记录
            val allReferRecords = publicVarReferInfoDao.listVarReferInfoByReferIdAndVersion(
                dslContext = context,
                projectId = projectId,
                referId = resourceId,
                referType = referType,
                referVersion = resourceVersion
            )

            if (allReferRecords.isEmpty()) return

            // 按变量组名称分组（不区分版本），收集所有涉及的变量名
            // Map<groupName, Set<varName>>
            val groupVarsMap = mutableMapOf<String, MutableSet<String>>()
            allReferRecords.forEach { record ->
                groupVarsMap.getOrPut(record.groupName) { mutableSetOf() }.add(record.varName)
            }

            // 删除所有引用记录
            publicVarReferInfoDao.deleteByReferIdAndVersion(
                dslContext = context,
                projectId = projectId,
                referId = resourceId,
                referType = referType,
                referVersion = resourceVersion
            )

            // 收集需要更新引用计数的变量组（按groupName收集，不区分版本）
            groupVarsMap.forEach { (groupName, varNames) ->
                val groupKey = PublicGroupKey(groupName, null)
                groupsNeedUpdateCount[groupKey] = varNames.toList()
            }
            
        } else {
            groupsToCleanup.forEach { groupKey ->
                val groupName = groupKey.groupName
                val version = groupKey.getVersionForDb()

                // 查询该变量组版本的引用记录，收集变量名
                val records = publicVarReferInfoDao.listVarReferInfoByReferIdAndVersion(
                    dslContext = context,
                    projectId = projectId,
                    referId = resourceId,
                    referType = referType,
                    referVersion = resourceVersion
                ).filter { it.groupName == groupName && it.version == version }

                val varNames = records.map { it.varName }

                // 删除引用记录（按groupName和version删除）
                publicVarReferInfoDao.deleteByReferIdAndGroup(
                    dslContext = context,
                    projectId = projectId,
                    referId = resourceId,
                    referType = referType,
                    groupName = groupName,
                    referVersion = resourceVersion
                )

                // 收集需要更新的变量组和变量
                if (varNames.isNotEmpty()) {
                    val updateKey = PublicGroupKey(groupName, null)
                    groupsNeedUpdateCount[updateKey] = varNames
                }
            }
        }

        // 批量更新引用计数（会重新统计实际引用数）
        groupsNeedUpdateCount.forEach { (groupKey, varNames) ->
            updateVarReferCount(
                projectId = projectId,
                groupName = groupKey.groupName,
                varNames = varNames,
                version = null  // 传null以统计最新版本和-1版本的引用数
            )
        }
    }
}
