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
import com.tencent.devops.common.pipeline.pojo.PublicVarGroupRef
import com.tencent.devops.common.pipeline.pojo.VarRefDetail
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.dao.`var`.PublicVarReferInfoDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.pojo.`var`.PublicGroupKey
import com.tencent.devops.process.pojo.`var`.VarCountUpdateInfo
import com.tencent.devops.process.pojo.`var`.VarGroupProcessContext
import com.tencent.devops.process.pojo.`var`.VarReferenceUpdateResult
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarReferPO
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PublicVarReferInfoService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val publicVarGroupDao: PublicVarGroupDao,
    private val publicVarReferInfoDao: PublicVarReferInfoDao,
    private val publicVarReferCountService: PublicVarReferCountService,
    private val templateDao: TemplateDao,
    private val templatePipelineDao: TemplatePipelineDao,
    private val varRefDetailDao: com.tencent.devops.process.dao.VarRefDetailDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarReferInfoService::class.java)
    }

    /**
     * 获取变量组的源项目ID
     * 用于跨项目引用场景，返回变量组实际所在的项目ID
     * 
     * @param projectId 当前项目ID
     * @param groupName 变量组名称
     * @param referId 引用资源ID（流水线ID或模板ID）
     * @param referType 引用类型
     * @return 变量组所在的项目ID，如果无法确定则返回null
     */
    private fun getSourceProjectIdForVarGroup(
        projectId: String,
        groupName: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum
    ): String? {
        // 查询变量组信息，检查是否在当前项目存在
        val varGroup = publicVarGroupDao.getRecordByGroupName(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName
        )

        // 如果变量组在当前项目存在，则不是跨项目引用
        if (varGroup != null) {
            return null
        }

        // 变量组在当前项目不存在，可能是跨项目引用
        // 尝试从引用资源获取源项目ID
        return try {
            when (referType) {
                PublicVerGroupReferenceTypeEnum.TEMPLATE -> {
                    getSourceProjectIdForTemplate(projectId, referId)
                }
                PublicVerGroupReferenceTypeEnum.PIPELINE -> {
                    getSourceProjectIdForPipeline(projectId, referId)
                }
            }
        } catch (e: Throwable) {
            logger.warn(
                "Failed to get source project id for varGroup: $groupName, " +
                "referId: $referId, referType: $referType",
                e
            )
            null
        }
    }

    /**
     * 获取模板的源项目ID
     */
    private fun getSourceProjectIdForTemplate(projectId: String, templateId: String): String? {
        val srcTemplateId = templateDao.getSrcTemplateId(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId
        )

        if (srcTemplateId.isNullOrBlank()) {
            return null
        }

        return templateDao.getProjectIdByTemplateId(
            dslContext = dslContext,
            templateId = srcTemplateId
        )
    }

    /**
     * 获取流水线的源项目ID
     */
    private fun getSourceProjectIdForPipeline(projectId: String, pipelineId: String): String? {
        val templateId = templatePipelineDao.get(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )?.templateId

        if (templateId.isNullOrBlank()) {
            return null
        }

        val srcTemplateId = templateDao.getSrcTemplateId(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId
        )

        if (srcTemplateId.isNullOrBlank()) {
            return null
        }

        return templateDao.getProjectIdByTemplateId(
            dslContext = dslContext,
            templateId = srcTemplateId
        )
    }

    /**
     * 获取用于查询的实际项目ID
     * 如果 sourceProjectId 不为空，返回 sourceProjectId（跨项目场景）
     * 否则返回当前 projectId（非跨项目场景）
     * 
     * @param projectId 当前项目ID
     * @param sourceProjectId 源项目ID（可选）
     * @return 用于查询的项目ID
     */
    private fun getActualProjectIdForQuery(projectId: String, sourceProjectId: String?): String {
        return sourceProjectId ?: projectId
    }

    /**
     * 处理资源的变量引用关系
     */
    fun handleResourceVarReferences(
        context: DSLContext,
        userId: String,
        projectId: String,
        resourceId: String,
        resourceType: String,
        resourceVersion: Int,
        model: Model,
        varRefDetails: List<VarRefDetail>
    ): VarReferenceUpdateResult {
        logger.info("Start handling resource var references for resource: $resourceId|$resourceVersion")

        // 1. 处理资源维度的引用记录
        // 删除旧的引用记录
        varRefDetailDao.deleteByResourceId(
            dslContext = context,
            projectId = projectId,
            resourceId = resourceId,
            resourceType = resourceType,
            referVersion = resourceVersion
        )

        if (varRefDetails.isNotEmpty()) {
            varRefDetailDao.batchSave(
                dslContext = context,
                varRefDetails = varRefDetails
            )
        }

        // 2. 处理变量组引用关系
        val referType = PublicVerGroupReferenceTypeEnum.valueOf(resourceType)

        // 查询已存在的引用记录
        val existingGroupKeys = publicVarReferInfoDao.listVarGroupsByReferIdAndVersion(
            dslContext = context,
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
                context = context,
                projectId = projectId,
                resourceId = resourceId,
                referType = referType,
                resourceVersion = resourceVersion,
                groupsToCleanup = null
            )
            return VarReferenceUpdateResult(
                referRecordsToAdd = emptyList(),
                varsNeedRecalculate = emptySet()
            )
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
            context = context,
            projectId = projectId,
            resourceId = resourceId,
            referType = referType,
            resourceVersion = resourceVersion,
            modelVarGroups = modelVarGroups,
            existingGroupKeys = existingGroupKeys
        )

        // 批量查询最新版本号
        val latestVersionMap = queryLatestVersions(
            projectId = projectId,
            resourceId = resourceId,
            referType = referType,
            modelVarGroups = modelVarGroups
        )

        // 批量查询已存在的变量名
        val allExistingVarNames = queryExistingVarNames(
            context = context,
            projectId = projectId,
            resourceId = resourceId,
            referType = referType,
            resourceVersion = resourceVersion
        )

        // 构建处理上下文
        val varGroupContext = VarGroupProcessContext(
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

        // 计算并执行删除操作，收集需要重算计数的变量
        val varsNeedRecalculate = calculateAndExecuteDelete(context, varGroupContext)

        // 计算需要新增的变量引用记录
        val referRecordsToAdd = calculateVarsToAdd(varGroupContext)

        // 返回结果，由外部统一处理计数更新
        return VarReferenceUpdateResult(
            referRecordsToAdd = referRecordsToAdd,
            varsNeedRecalculate = varsNeedRecalculate
        )
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
     * @param context 数据库上下文
     * @param projectId 项目ID
     * @param resourceId 资源ID
     * @param referType 引用类型
     * @param resourceVersion 资源版本
     * @param modelVarGroups Model中的变量组列表
     * @param existingGroupKeys 数据库中已存在的变量组键集合
     */
    private fun cleanupObsoleteGroupReferences(
        context: DSLContext,
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
                context = context,
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
     * 支持跨项目引用：如果变量组来自其他项目，使用 sourceProjectId 查询
     */
    private fun queryLatestVersions(
        projectId: String,
        resourceId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        modelVarGroups: List<PublicVarGroupRef>
    ): Map<String, Int> {
        val groupsNeedLatestVersion = modelVarGroups
            .map { it }
            .filter { it.version == null }
            .map { it.groupName }

        if (groupsNeedLatestVersion.isEmpty()) {
            return emptyMap()
        }

        val result = mutableMapOf<String, Int>()
        
        groupsNeedLatestVersion.forEach { groupName ->
            // 获取变量组的源项目ID
            val sourceProjectId = getSourceProjectIdForVarGroup(
                projectId = projectId,
                groupName = groupName,
                referId = resourceId,
                referType = referType
            )
            
            // 使用实际项目ID查询版本
            val actualProjectId = getActualProjectIdForQuery(projectId, sourceProjectId)
            val version = publicVarGroupDao.getLatestVersionByGroupName(
                dslContext = dslContext,
                projectId = actualProjectId,
                groupName = groupName
            )
            
            if (version != null) {
                result[groupName] = version
            }
        }

        return result
    }

    /**
     * 批量查询已存在的变量名
     * @param context 数据库上下文
     * @return Map<PublicGroupKey, Set<varName>>
     */
    private fun queryExistingVarNames(
        context: DSLContext,
        projectId: String,
        resourceId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        resourceVersion: Int
    ): Map<PublicGroupKey, Set<String>> {
        return publicVarReferInfoDao.listVarReferInfoByReferIdAndVersion(
            dslContext = context,
            projectId = projectId,
            referId = resourceId,
            referType = referType,
            referVersion = resourceVersion
        ).groupBy { PublicGroupKey(it.groupName, if (it.version == -1) null else it.version) }
            .mapValues { (_, records) -> records.map { it.varName }.toSet() }
    }

    /**
     * 计算并执行删除操作
     * 删除引用记录后，收集需要更新计数的变量信息
     * @param context 数据库上下文
     * @param varGroupContext 变量组处理上下文
     * @return 需要重新计算计数的变量信息集合
     */
    private fun calculateAndExecuteDelete(
        context: DSLContext,
        varGroupContext: VarGroupProcessContext
    ): Set<VarCountUpdateInfo> {
        // 收集需要更新引用计数的变量信息
        val varsNeedUpdateCount = mutableSetOf<VarCountUpdateInfo>()

        varGroupContext.modelVarGroups.forEach { varGroup ->
            val group = varGroup
            val groupKey = PublicGroupKey(group.groupName, group.version)
            val groupName = groupKey.groupName

            // 获取该变量组被引用的变量
            val referencedVarNameSet = getReferencedVarsForGroup(
                groupName = groupName,
                publicVarMap = varGroupContext.publicVarMap,
                referencedVarNames = varGroupContext.referencedVarNames,
                pipelineVarNames = varGroupContext.pipelineVarNames
            )
            
            if (referencedVarNameSet.isEmpty()) {
                return@forEach
            }

            // 获取已存在的变量名
            val existingVarNames = varGroupContext.allExistingVarNames[groupKey] ?: emptySet()

            val diffResult = calculateVarGroupDiff(
                referencedVars = referencedVarNameSet,
                existingVars = existingVarNames
            )

            // 执行删除操作并收集需要更新计数的变量
            if (diffResult.varsToRemove.isNotEmpty()) {
                // 获取源项目ID
                val sourceProjectId = getSourceProjectIdForVarGroup(
                    projectId = varGroupContext.projectId,
                    groupName = groupName,
                    referId = varGroupContext.resourceId,
                    referType = varGroupContext.referType
                ) ?: varGroupContext.projectId

                // 删除引用记录
                publicVarReferInfoDao.deleteByReferIdAndGroupAndVarNames(
                    dslContext = context,
                    projectId = varGroupContext.projectId,
                    referId = varGroupContext.resourceId,
                    referType = varGroupContext.referType,
                    groupName = groupName,
                    referVersion = varGroupContext.resourceVersion,
                    varNames = diffResult.varsToRemove.toList()
                )

                // 收集需要更新计数的变量
                val version = groupKey.getVersionForDb()
                diffResult.varsToRemove.forEach { varName ->
                    varsNeedUpdateCount.add(
                        VarCountUpdateInfo(
                            projectId = sourceProjectId,
                            groupName = groupName,
                            varName = varName,
                            version = version
                        )
                    )
                }
            }
        }

        // 返回需要重算计数的变量信息，不在此处执行计数更新
        return varsNeedUpdateCount
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
                // 获取变量组的源项目ID（用于跨项目引用）
                val sourceProjectId = getSourceProjectIdForVarGroup(
                    projectId = context.projectId,
                    groupName = groupName,
                    referId = context.resourceId,
                    referType = context.referType
                )

                // 批量生成ID
                val segmentIds = client.get(ServiceAllocIdResource::class)
                    .batchGenerateSegmentId("T_RESOURCE_PUBLIC_VAR_REFER_INFO", diffResult.varsToAdd.size).data
                if (segmentIds.isNullOrEmpty() || segmentIds.size != diffResult.varsToAdd.size) {
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
                        sourceProjectId = sourceProjectId,  // 设置源项目ID
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
     * 清理已移除的变量组引用
     * 删除引用记录后，在事务外重新计算受影响变量的实际引用计数
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
        // 收集需要重新计算引用计数的变量信息
        // Map<(sourceProjectId, groupName), Set<varName>>
        val varsNeedRecalculate = mutableMapOf<Pair<String, String>, MutableSet<String>>()

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

            // 按 (sourceProjectId, groupName) 分组，收集所有涉及的变量名
            allReferRecords.forEach { record ->
                val actualProjectId = record.sourceProjectId ?: projectId
                val key = Pair(actualProjectId, record.groupName)
                varsNeedRecalculate.getOrPut(key) { mutableSetOf() }.add(record.varName)
            }

            // 删除所有引用记录（在当前事务中完成）
            publicVarReferInfoDao.deleteByReferIdAndVersion(
                dslContext = context,
                projectId = projectId,
                referId = resourceId,
                referType = referType,
                referVersion = resourceVersion
            )
            
        } else {
            groupsToCleanup.forEach { groupKey ->
                val groupName = groupKey.groupName
                val version = groupKey.getVersionForDb()

                // 查询该变量组版本的引用记录，收集变量名和 sourceProjectId
                val records = publicVarReferInfoDao.listVarReferInfoByReferIdAndVersion(
                    dslContext = context,
                    projectId = projectId,
                    referId = resourceId,
                    referType = referType,
                    referVersion = resourceVersion
                ).filter { it.groupName == groupName && it.version == version }

                if (records.isNotEmpty()) {
                    // 收集需要重算的变量
                    val actualProjectId = records.first().sourceProjectId ?: projectId
                    val key = Pair(actualProjectId, groupName)
                    varsNeedRecalculate.getOrPut(key) { mutableSetOf() }
                        .addAll(records.map { it.varName })
                }

                // 删除引用记录（在当前事务中完成）
                publicVarReferInfoDao.deleteByReferIdAndGroup(
                    dslContext = context,
                    projectId = projectId,
                    referId = resourceId,
                    referType = referType,
                    groupName = groupName,
                    referVersion = resourceVersion
                )
            }
        }
        
        if (varsNeedRecalculate.isNotEmpty()) {
            batchRecalculateReferCount(
                varsNeedRecalculate = varsNeedRecalculate
            )
        }
    }

    /**
     * 批量重新计算引用计数
     * 为每个变量独立获取锁和事务，避免长时间持锁
     * 
     * @param varsNeedRecalculate Map<(sourceProjectId, groupName), Set<varName>>
     */
    private fun batchRecalculateReferCount(
        varsNeedRecalculate: Map<Pair<String, String>, Set<String>>
    ) {
        varsNeedRecalculate.forEach { (key, varNames) ->
            val (sourceProjectId, groupName) = key
            
            // 确定实际版本号
            val actualVersion = try {
                publicVarGroupDao.getLatestVersionByGroupName(
                    dslContext = dslContext,
                    projectId = sourceProjectId,
                    groupName = groupName
                )
            } catch (e: Throwable) {
                logger.warn(
                    "Failed to get version for group: $groupName in project: $sourceProjectId",
                    e
                )
                null
            }

            if (actualVersion == null) {
                logger.warn("Cannot find version for group: $groupName in project: $sourceProjectId")
                return@forEach
            }

            // 为每个变量独立重算计数
            varNames.forEach { varName ->
                try {
                    publicVarReferCountService.recalculateReferCount(
                        projectId = sourceProjectId,
                        groupName = groupName,
                        varName = varName,
                        version = actualVersion
                    )
                } catch (e: Throwable) {
                    // 单个变量重算失败不影响其他变量
                    logger.warn(
                        "Failed to recalculate refer count for var: $varName, " +
                        "group: $groupName, project: $sourceProjectId",
                        e
                    )
                }
            }
        }
    }
}
