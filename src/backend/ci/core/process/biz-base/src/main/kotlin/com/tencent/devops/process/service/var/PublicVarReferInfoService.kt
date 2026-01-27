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
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.VarRefDetailDao
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
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 带锁的变量引用请求DTO
 * 用于封装 handleResourceVarReferencesWithLock 方法的参数
 */
data class VarReferenceRequestWithLock(
    val userId: String,
    val projectId: String,
    val resourceId: String,
    val resourceType: String,
    val resourceVersion: Int,
    val model: Model,
    val varRefDetails: List<VarRefDetail>
)

/**
 * 清理变量组引用请求DTO
 */
data class CleanupVarGroupReferenceRequest(
    val context: DSLContext,
    val projectId: String,
    val resourceId: String,
    val referType: PublicVerGroupReferenceTypeEnum,
    val resourceVersion: Int,
    val groupsToCleanup: Set<PublicGroupKey>? = null
)

/**
 * 资源引用查询参数DTO
 * 用于封装资源引用相关的查询参数
 */
data class ResourceReferenceQueryParams(
    val context: DSLContext,
    val projectId: String,
    val resourceId: String,
    val referType: PublicVerGroupReferenceTypeEnum,
    val resourceVersion: Int
)

@Service
class PublicVarReferInfoService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val publicVarGroupDao: PublicVarGroupDao,
    private val publicVarReferInfoDao: PublicVarReferInfoDao,
    private val publicVarReferCountService: PublicVarReferCountService,
    private val templateDao: TemplateDao,
    private val templatePipelineDao: TemplatePipelineDao,
    private val varRefDetailDao: VarRefDetailDao
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
     * @return 变量组所在的项目ID，如果无法确定则返回null（表示非跨项目引用）
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
     * 处理资源的变量引用关系（带锁保护的公共方法）
     * 使用资源级分布式锁保护整个操作流程，包括引用关系处理和引用计数更新
     * 
     * 线程安全说明：
     * - 使用资源级分布式锁：RESOURCE_VAR_REFER_LOCK:$projectId:$resourceType:$resourceId:$resourceVersion
     * - 锁保护范围：整个操作流程（事务处理 + 计数更新）
     * - 所有内部方法调用（包括 cleanupRemovedVarGroupReferences）都在此锁保护下执行
     * 
     * @param request 变量引用请求DTO（带锁版本）
     */
    fun handleResourceVarReferencesWithLock(request: VarReferenceRequestWithLock) {
        logger.info(
            "Start handling resource var references with lock: " +
                "resourceId=${request.resourceId}, resourceType=${request.resourceType}, " +
                "resourceVersion=${request.resourceVersion}"
        )

        // 使用资源级分布式锁，粒度：项目+资源类型+资源ID+版本
        // 锁粒度说明：
        // - 资源级别锁确保同一资源的引用操作串行化，避免并发修改导致的数据不一致
        // - 不同资源之间可以并发处理，提升整体性能
        // - 锁持有时间：事务处理 + 计数更新，已优化为批量处理，减少锁持有时间
        val lockKey = "RESOURCE_VAR_REFER_LOCK:${request.projectId}:${request.resourceType}:${request.resourceId}:${request.resourceVersion}"
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = lockKey,
            expiredTimeInSeconds = 10L
        )

        try {
            redisLock.lock()

            // 1. 在事务中处理资源维度的引用关系
            val referenceUpdateResult = dslContext.transactionResult { configuration ->
                val transactionContext = DSL.using(configuration)

                // 调用内部方法处理引用关系
                doHandleResourceVarReferences(
                    context = transactionContext,
                    userId = request.userId,
                    projectId = request.projectId,
                    resourceId = request.resourceId,
                    referType = PublicVerGroupReferenceTypeEnum.valueOf(request.resourceType),
                    resourceVersion = request.resourceVersion,
                    model = request.model,
                    varRefDetails = request.varRefDetails
                )
            }

            // 2. 在锁保护下更新变量维度的引用计数
            // 注意：外层已经提供了资源级锁保护，PublicVarReferCountService 内部不再使用锁，避免双重锁
            // 优化：使用批量处理，减少事务数量，提升性能
            publicVarReferCountService.updateVarReferCounts(
                referRecordsToAdd = referenceUpdateResult.referRecordsToAdd,
                varsNeedRecalculate = referenceUpdateResult.varsNeedRecalculate
            )
        } catch (e: Throwable) {
            logger.warn(
                "Failed to handle resource var references: " +
                    "resourceId=${request.resourceId}, resourceVersion=${request.resourceVersion}",
                e
            )
            throw e
        } finally {
            redisLock.unlock()
        }
    }

    /**
     * 实际处理资源的变量引用关系（内部方法）
     * 
     * 线程安全说明：
     * - 该方法在 handleResourceVarReferencesWithLock 的资源级锁保护下执行
     * - 该方法在事务中执行，调用 cleanupRemovedVarGroupReferences 等方法
     * - 所有对 cleanupRemovedVarGroupReferences 的调用都在同一把资源级锁保护下
     */
    private fun doHandleResourceVarReferences(
        context: DSLContext,
        userId: String,
        projectId: String,
        resourceId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        resourceVersion: Int,
        model: Model,
        varRefDetails: List<VarRefDetail>
    ): VarReferenceUpdateResult {
        
        // 删除历史引用记录
        varRefDetailDao.deleteByResourceId(
            dslContext = context,
            projectId = projectId,
            resourceId = resourceId,
            resourceType = referType.name,
            referVersion = resourceVersion
        )

        if (varRefDetails.isNotEmpty()) {
            varRefDetailDao.batchSave(
                dslContext = context,
                varRefDetails = varRefDetails
            )
        }

        // 获取Model中的变量组列表
        val modelVarGroups = model.publicVarGroups ?: emptyList()
        // 如果Model中没有变量组，清理所有已存在的引用记录
        if (modelVarGroups.isEmpty()) {
            val varsNeedRecalculate = cleanupRemovedVarGroupReferences(
                CleanupVarGroupReferenceRequest(
                    context = context,
                    projectId = projectId,
                    resourceId = resourceId,
                    referType = referType,
                    resourceVersion = resourceVersion,
                    groupsToCleanup = null
                )
            )
            return VarReferenceUpdateResult(
                referRecordsToAdd = emptyList(),
                varsNeedRecalculate = varsNeedRecalculate
            )
        }
        
        // 查询已存在的引用记录
        val existingGroupKeys = publicVarReferInfoDao.listVarGroupsByReferIdAndVersion(
            dslContext = context,
            projectId = projectId,
            referId = resourceId,
            referType = referType,
            referVersion = resourceVersion
        )

        // 获取流水线变量名集合(用于排除与公共变量重名的情况)
        val pipelineVarNames = model.getTriggerContainer().params
            .filter { it.varGroupName.isNullOrBlank() }
            .map { it.id }
            .toSet()
        
        // 构建公共变量映射和被引用变量集合
        val referencedVarNames = varRefDetails.map { it.varName }.toSet()
        val publicVarMap = buildPublicVarMap(model)

        // 构建查询参数DTO
        val queryParams = ResourceReferenceQueryParams(
            context = context,
            projectId = projectId,
            resourceId = resourceId,
            referType = referType,
            resourceVersion = resourceVersion
        )

        // 清理不在Model中的变量组引用，收集需要重算的变量
        val varsNeedRecalculateFromCleanup = cleanupObsoleteGroupReferences(
            queryParams = queryParams,
            modelVarGroups = modelVarGroups,
            existingGroupKeys = existingGroupKeys
        )

        // 批量查询最新版本号
        val latestVersionMap = queryLatestVersions(
            queryParams = queryParams,
            modelVarGroups = modelVarGroups
        )

        // 批量查询已存在的变量名
        val allExistingVarNames = queryExistingVarNames(queryParams = queryParams)

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
        val varsNeedRecalculateFromDelete = calculateAndExecuteDelete(context, varGroupContext)

        // 计算需要新增的变量引用记录
        val referRecordsToAdd = calculateVarsToAdd(varGroupContext)

        // 合并所有需要重算的变量（来自清理和删除操作）
        val allVarsNeedRecalculate = varsNeedRecalculateFromCleanup + varsNeedRecalculateFromDelete

        // 返回结果，由外部统一处理计数更新
        return VarReferenceUpdateResult(
            referRecordsToAdd = referRecordsToAdd,
            varsNeedRecalculate = allVarsNeedRecalculate
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
     * 
     * @param queryParams 资源引用查询参数
     * @param modelVarGroups Model中的变量组列表
     * @param existingGroupKeys 数据库中已存在的变量组键集合
     * @return 需要重新计算引用计数的变量信息集合
     */
    private fun cleanupObsoleteGroupReferences(
        queryParams: ResourceReferenceQueryParams,
        modelVarGroups: List<*>,
        existingGroupKeys: Set<PublicGroupKey>
    ): Set<VarCountUpdateInfo> {
        // 将Model中的变量组转换为PublicGroupKey集合
        val modelGroupKeys = modelVarGroups
            .map { it as com.tencent.devops.common.pipeline.pojo.PublicVarGroupRef }
            .map { PublicGroupKey(it.groupName, it.version) }
            .toSet()

        // 找出需要清理的变量组（存在于数据库但不在Model中）
        val groupsToCleanup = existingGroupKeys - modelGroupKeys

        return if (groupsToCleanup.isNotEmpty()) {
            cleanupRemovedVarGroupReferences(
                CleanupVarGroupReferenceRequest(
                    context = queryParams.context,
                    projectId = queryParams.projectId,
                    resourceId = queryParams.resourceId,
                    referType = queryParams.referType,
                    resourceVersion = queryParams.resourceVersion,
                    groupsToCleanup = groupsToCleanup
                )
            )
        } else {
            emptySet()
        }
    }

    /**
     * 批量查询最新版本号
     * 支持跨项目引用：如果变量组来自其他项目，使用 sourceProjectId 查询
     * 
     * @param queryParams 资源引用查询参数
     * @param modelVarGroups Model中的变量组列表
     * @return 变量组名称到最新版本号的映射
     */
    private fun queryLatestVersions(
        queryParams: ResourceReferenceQueryParams,
        modelVarGroups: List<PublicVarGroupRef>
    ): Map<String, Int> {
        val groupsNeedLatestVersion = modelVarGroups
            .filter { it.version == null }
            .map { it.groupName }

        if (groupsNeedLatestVersion.isEmpty()) {
            return emptyMap()
        }

        val result = mutableMapOf<String, Int>()
        
        groupsNeedLatestVersion.forEach { groupName ->
            // 获取变量组的源项目ID
            val sourceProjectId = getSourceProjectIdForVarGroup(
                projectId = queryParams.projectId,
                groupName = groupName,
                referId = queryParams.resourceId,
                referType = queryParams.referType
            )
            
            // 使用实际项目ID查询版本
            val actualProjectId = getActualProjectIdForQuery(queryParams.projectId, sourceProjectId)
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
     * 
     * @param queryParams 资源引用查询参数
     * @return Map<PublicGroupKey, Set<varName>>
     */
    private fun queryExistingVarNames(
        queryParams: ResourceReferenceQueryParams
    ): Map<PublicGroupKey, Set<String>> {
        return publicVarReferInfoDao.listVarReferInfoByReferIdAndVersion(
            dslContext = queryParams.context,
            projectId = queryParams.projectId,
            referId = queryParams.resourceId,
            referType = queryParams.referType,
            referVersion = queryParams.resourceVersion
        ).groupBy { PublicGroupKey(it.groupName, if (it.version == -1) null else it.version) }
            .mapValues { (_, records) -> records.map { it.varName }.toSet() }
    }

    /**
     * 计算并执行删除操作
     * 删除引用记录后，收集需要更新计数的变量信息
     * 
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
            val groupKey = PublicGroupKey(varGroup.groupName, varGroup.version)
            val groupName = groupKey.groupName

            // 获取该变量组的差异信息
            val diffResult = calculateVarGroupDiffForGroup(
                varGroupContext = varGroupContext,
                groupKey = groupKey,
                groupName = groupName
            ) ?: return@forEach

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

        return varsNeedUpdateCount
    }

    /**
     * 计算变量组的差异信息（公共方法）
     * 提取公共逻辑，避免代码重复
     * 
     * @param varGroupContext 变量组处理上下文
     * @param groupKey 变量组键
     * @param groupName 变量组名称
     * @return 变量组差异结果，如果变量组没有被引用的变量则返回null
     */
    private fun calculateVarGroupDiffForGroup(
        varGroupContext: VarGroupProcessContext,
        groupKey: PublicGroupKey,
        groupName: String
    ): com.tencent.devops.process.pojo.`var`.VarGroupDiffResult? {
        // 获取该变量组被引用的变量
        val referencedVarNameSet = getReferencedVarsForGroup(
            groupName = groupName,
            publicVarMap = varGroupContext.publicVarMap,
            referencedVarNames = varGroupContext.referencedVarNames,
            pipelineVarNames = varGroupContext.pipelineVarNames
        )
        
        if (referencedVarNameSet.isEmpty()) {
            return null
        }

        // 获取已存在的变量名
        val existingVarNames = varGroupContext.allExistingVarNames[groupKey] ?: emptySet()

        return calculateVarGroupDiff(
            referencedVars = referencedVarNameSet,
            existingVars = existingVarNames
        )
    }

    /**
     * 计算需要新增的变量引用记录
     * 
     * @param context 变量组处理上下文
     * @return 新增的变量引用记录列表
     */
    private fun calculateVarsToAdd(
        context: VarGroupProcessContext
    ): List<ResourcePublicVarReferPO> {
        val referRecordsToAdd = mutableListOf<ResourcePublicVarReferPO>()

        context.modelVarGroups.forEach { varGroup ->
            val groupKey = PublicGroupKey(varGroup.groupName, varGroup.version)
            val groupName = groupKey.groupName
            val versionForDb = groupKey.getVersionForDb()

            // 获取该变量组的差异信息
            val diffResult = calculateVarGroupDiffForGroup(
                varGroupContext = context,
                groupKey = groupKey,
                groupName = groupName
            ) ?: return@forEach

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
     * 删除引用记录后，收集需要重新计算引用计数的变量信息
     * 
     * 线程安全说明：
     * - 该方法在事务中执行，只负责删除引用记录和收集需要重算的变量信息
     * - 该方法不提供锁保护，必须由外层调用方提供锁保护
     * - 当前调用路径：handleResourceVarReferencesWithLock (资源级锁) -> doHandleResourceVarReferences (事务中) -> cleanupRemovedVarGroupReferences
     * - 因此该方法在资源级锁保护下执行，是线程安全的
     * - 注意：如果未来在其他地方调用此方法，必须确保外层提供适当的锁保护
     * 
     * @param request 清理请求DTO
     * @return 需要重新计算引用计数的变量信息集合
     */
    fun cleanupRemovedVarGroupReferences(request: CleanupVarGroupReferenceRequest): Set<VarCountUpdateInfo> {
        val context = request.context
        val projectId = request.projectId
        val resourceId = request.resourceId
        val referType = request.referType
        val resourceVersion = request.resourceVersion
        val groupsToCleanup = request.groupsToCleanup
        
        // 收集需要重新计算引用计数的变量信息
        val varsNeedRecalculate = mutableSetOf<VarCountUpdateInfo>()

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

            if (allReferRecords.isEmpty()) {
                return emptySet()
            }

            // 收集所有涉及的变量信息
            // 按项目+变量组+变量+版本去重，避免重复计算
            val uniqueVars = allReferRecords.map { record ->
                val actualProjectId = record.sourceProjectId ?: projectId
                VarCountUpdateInfo(
                    projectId = actualProjectId,
                    groupName = record.groupName,
                    varName = record.varName,
                    version = record.version ?: -1
                )
            }.distinctBy { 
                "${it.projectId}:${it.groupName}:${it.varName}:${it.version}"
            }
            
            varsNeedRecalculate.addAll(uniqueVars)

            // 删除所有引用记录（在当前事务中完成）
            publicVarReferInfoDao.deleteByReferIdAndVersion(
                dslContext = context,
                projectId = projectId,
                referId = resourceId,
                referType = referType,
                referVersion = resourceVersion
            )
            
        } else {
            // 按固定顺序处理，避免死锁
            val sortedGroups = groupsToCleanup.sortedWith(
                compareBy<PublicGroupKey> { it.groupName }
                    .thenBy { it.version ?: -1 }
            )

            sortedGroups.forEach { groupKey ->
                val groupName = groupKey.groupName
                val version = groupKey.getVersionForDb()
                
                // 查询需要删除的引用记录，用于收集需要重算的变量信息
                val records = publicVarReferInfoDao.listVarReferInfoByReferIdAndVersion(
                    dslContext = context,
                    projectId = projectId,
                    referId = resourceId,
                    referType = referType,
                    referVersion = resourceVersion,
                    groupName = groupName,
                    version = version
                )
                
                if (records.isNotEmpty()) {
                    // 收集需要重算的变量
                    // 注意：同一组内的记录应该有相同的 sourceProjectId，使用第一个记录的 sourceProjectId
                    val actualProjectId = records.first().sourceProjectId ?: projectId
                    
                    // 按变量去重，避免重复计算
                    val uniqueVars = records.map { record ->
                        VarCountUpdateInfo(
                            projectId = actualProjectId,
                            groupName = record.groupName,
                            varName = record.varName,
                            version = record.version ?: -1
                        )
                    }.distinctBy { 
                        "${it.projectId}:${it.groupName}:${it.varName}:${it.version}"
                    }
                    
                    varsNeedRecalculate.addAll(uniqueVars)
                    
                    // 删除引用记录（在事务中执行，确保原子性）
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
        }
        return varsNeedRecalculate
    }
}
