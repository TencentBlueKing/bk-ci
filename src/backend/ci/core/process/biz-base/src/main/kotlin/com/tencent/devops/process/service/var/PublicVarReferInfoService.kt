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
import com.tencent.devops.common.pipeline.enums.PublicVarGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.pojo.PublicVarGroupRef
import com.tencent.devops.common.pipeline.pojo.VarRefDetail
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode.DYNAMIC_VERSION
import com.tencent.devops.process.dao.VarRefDetailDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.dao.`var`.PublicVarReferInfoDao
import com.tencent.devops.process.pojo.`var`.CleanupVarGroupReferenceRequest
import com.tencent.devops.process.pojo.`var`.PublicGroupKey
import com.tencent.devops.process.pojo.`var`.ResourceReferenceQueryParams
import com.tencent.devops.process.pojo.`var`.VarGroupProcessContext
import com.tencent.devops.process.pojo.`var`.VarReferenceRequestWithLock
import com.tencent.devops.process.pojo.`var`.VarReferenceUpdateResult
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
    private val redisOperation: RedisOperation,
    private val publicVarGroupDao: PublicVarGroupDao,
    private val publicVarReferInfoDao: PublicVarReferInfoDao,
    private val publicVarReferWriteService: PublicVarReferWriteService,
    private val varRefDetailDao: VarRefDetailDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarReferInfoService::class.java)
        private const val RESOURCE_VAR_REFER_LOCK_TIMEOUT_SECONDS = 10L
    }

    /**
     * 处理资源的变量引用关系（带锁保护的公共方法）
     * 线程安全说明：
     * - 使用资源级分布式锁：RESOURCE_VAR_REFER_LOCK:$projectId:$resourceType:$resourceId:$resourceVersion
     * - 所有内部方法调用（包括 cleanupRemovedVarGroupReferences）都在此锁保护下执行
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
        // - 锁持有时间：事务处理 + 计数更新，均在同一事务中完成
        val lockKey = "RESOURCE_VAR_REFER_LOCK:${request.projectId}:${request.resourceType}" +
                ":${request.resourceId}:${request.resourceVersion}"
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = lockKey,
            expiredTimeInSeconds = RESOURCE_VAR_REFER_LOCK_TIMEOUT_SECONDS
        )

        try {
            redisLock.lock()

            // 在同一个事务中完成引用关系处理 + 引用记录写入，保证原子性
            // 注：referCount 已改为实时 JOIN 聚合，无需再维护 Summary.REFER_COUNT 缓存
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)

                // 1. 处理资源维度的引用关系
                val referenceUpdateResult = doHandleResourceVarReferences(
                    context = transactionContext,
                    userId = request.userId,
                    projectId = request.projectId,
                    resourceId = request.resourceId,
                    referType = PublicVarGroupReferenceTypeEnum.valueOf(request.resourceType),
                    resourceVersion = request.resourceVersion,
                    model = request.model,
                    varRefDetails = request.varRefDetails
                )

                // 2. 在同一事务中批量写入新增的引用记录
                publicVarReferWriteService.batchAddReferInTransaction(
                    context = transactionContext,
                    referInfos = referenceUpdateResult.referRecordsToAdd
                )
            }
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
        referType: PublicVarGroupReferenceTypeEnum,
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
            // 批量生成分布式ID
            val segmentIds = client.get(ServiceAllocIdResource::class)
                .batchGenerateSegmentId("T_VAR_REF_DETAIL", varRefDetails.size).data
            if (segmentIds.isNullOrEmpty() || segmentIds.size != varRefDetails.size) {
                throw ErrorCodeException(
                    errorCode = ERROR_INVALID_PARAM_,
                    params = arrayOf("Failed to generate segment IDs for var ref detail")
                )
            }
            varRefDetails.forEachIndexed { index, detail ->
                detail.id = segmentIds[index] ?: 0
            }
            varRefDetailDao.batchSave(
                dslContext = context,
                varRefDetails = varRefDetails
            )
        }

        // 获取Model中的变量组列表
        val modelVarGroups = model.publicVarGroups ?: emptyList()
        // 如果Model中没有变量组，清理所有已存在的引用记录
        if (modelVarGroups.isEmpty()) {
            cleanupRemovedVarGroupReferences(
                context = context,
                request = CleanupVarGroupReferenceRequest(
                    projectId = projectId,
                    resourceId = resourceId,
                    referType = referType,
                    resourceVersion = resourceVersion,
                    groupsToCleanup = null
                )
            )
            return VarReferenceUpdateResult(referRecordsToAdd = emptyList())
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

        // 清理不在Model中的变量组引用
        cleanupObsoleteGroupReferences(
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

        // 计算并执行删除操作
        calculateAndExecuteDelete(context, varGroupContext)

        // 计算需要新增的变量引用记录
        val referRecordsToAdd = calculateVarsToAdd(varGroupContext)

        // 返回结果，由外部统一写入引用记录
        return VarReferenceUpdateResult(referRecordsToAdd = referRecordsToAdd)
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
     * @param queryParams 资源引用查询参数
     * @param modelVarGroups Model中的变量组列表
     * @param existingGroupKeys 数据库中已存在的变量组键集合
     */
    private fun cleanupObsoleteGroupReferences(
        queryParams: ResourceReferenceQueryParams,
        modelVarGroups: List<PublicVarGroupRef>,
        existingGroupKeys: Set<PublicGroupKey>
    ) {
        // 将Model中的变量组转换为PublicGroupKey集合
        val modelGroupKeys = modelVarGroups
            .map { PublicGroupKey(it.groupName, it.version) }
            .toSet()

        // 找出需要清理的变量组（存在于数据库但不在Model中）
        val groupsToCleanup = existingGroupKeys - modelGroupKeys

        if (groupsToCleanup.isNotEmpty()) {
            cleanupRemovedVarGroupReferences(
                context = queryParams.context,
                request = CleanupVarGroupReferenceRequest(
                    projectId = queryParams.projectId,
                    resourceId = queryParams.resourceId,
                    referType = queryParams.referType,
                    resourceVersion = queryParams.resourceVersion,
                    groupsToCleanup = groupsToCleanup
                )
            )
        }
    }

    /**
     * 批量查询最新版本号
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

        return publicVarGroupDao.getLatestVersionsByGroupNames(
            dslContext = queryParams.context,
            projectId = queryParams.projectId,
            groupNames = groupsNeedLatestVersion
        )
    }

    /**
     * 批量查询已存在的变量名
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
        ).groupBy { PublicGroupKey(it.groupName, if (it.version == DYNAMIC_VERSION ) null else it.version) }
            .mapValues { (_, records) -> records.map { it.varName }.toSet() }
    }

    /**
     * 计算并执行删除操作
     * 从 ReferInfo 表中删除当前 Model 不再引用的变量记录
     * @param context 数据库上下文
     * @param varGroupContext 变量组处理上下文
     */
    private fun calculateAndExecuteDelete(
        context: DSLContext,
        varGroupContext: VarGroupProcessContext
    ) {
        varGroupContext.modelVarGroups.forEach { varGroup ->
            val groupKey = PublicGroupKey(varGroup.groupName, varGroup.version)
            val groupName = groupKey.groupName

            // 获取该变量组的差异信息
            val diffResult = calculateVarGroupDiffForGroup(
                varGroupContext = varGroupContext,
                groupKey = groupKey,
                groupName = groupName
            ) ?: return@forEach

            // 执行删除操作
            if (diffResult.varsToRemove.isNotEmpty()) {
                publicVarReferInfoDao.deleteByReferIdAndGroupAndVarNames(
                    dslContext = context,
                    projectId = varGroupContext.projectId,
                    referId = varGroupContext.resourceId,
                    referType = varGroupContext.referType,
                    groupName = groupName,
                    referVersion = varGroupContext.resourceVersion,
                    varNames = diffResult.varsToRemove.toList()
                )
            }
        }
    }

    /**
     * 计算变量组的差异信息（公共方法）
     * 提取公共逻辑，避免代码重复
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

        // 获取已存在的变量名
        val existingVarNames = varGroupContext.allExistingVarNames[groupKey] ?: emptySet()

        // 只有当前没有任何引用，且 DB 中也没有历史记录时，才真正无事可做；
        // 否则需要走 diff 计算（特别是 referenced 为空但 existing 非空的场景，
        // 表示用户去掉了所有引用表达式，应清理 DB 中残留的引用记录）
        if (referencedVarNameSet.isEmpty() && existingVarNames.isEmpty()) {
            return null
        }

        return calculateVarGroupDiff(
            referencedVars = referencedVarNameSet,
            existingVars = existingVarNames
        )
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
            varsToRemove = existingVars - referencedVars, // 存在但不再被引用的变量
            varsToUpdate = existingVars.intersect(referencedVars), // 继续被引用的变量（保持不变）
            varsToAdd = referencedVars - existingVars // 新增被引用的变量
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
     * 从 ReferInfo 表中删除对应的变量引用记录。
     * 线程安全说明：
     * - 该方法在事务中执行，只负责删除引用记录
     * - 该方法不提供锁保护，必须由外层调用方提供锁保护
     * - 当前调用路径：
     * handleResourceVarReferencesWithLock (资源级锁) ->
     * doHandleResourceVarReferences (事务中) ->
     * cleanupRemovedVarGroupReferences
     * - 因此该方法在资源级锁保护下执行，是线程安全的
     * - 注意：如果未来在其他地方调用此方法，必须确保外层提供适当的锁保护
     * @param context 数据库上下文
     * @param request 清理请求DTO
     */
    private fun cleanupRemovedVarGroupReferences(
        context: DSLContext,
        request: CleanupVarGroupReferenceRequest
    ) {
        val projectId = request.projectId
        val resourceId = request.resourceId
        val referType = request.referType
        val resourceVersion = request.resourceVersion
        val groupsToCleanup = request.groupsToCleanup

        if (groupsToCleanup == null) {
            // 清理该资源的所有变量引用
            logger.info("Cleaning up all var group references for resource: $resourceId, version: $resourceVersion")
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
                    .thenBy { it.version ?: DYNAMIC_VERSION }
            )

            sortedGroups.forEach { groupKey ->
                val groupName = groupKey.groupName
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
}
