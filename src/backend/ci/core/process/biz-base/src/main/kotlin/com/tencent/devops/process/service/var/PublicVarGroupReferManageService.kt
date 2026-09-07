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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.PublicVarGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.PublicVarGroupRef
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessConstants.DYNAMIC_VERSION
import com.tencent.devops.process.constant.ProcessConstants.PUBLIC_VAR_GROUP_LOCK_EXPIRED_TIME_IN_SECONDS
import com.tencent.devops.process.constant.ProcessConstants.PUBLIC_VAR_GROUP_REFER_LOCK_KEY_PREFIX
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_CONFLICT
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_NOT_EXIST
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_UPDATE_FAILED
import com.tencent.devops.process.dao.VarRefDetailDao
import com.tencent.devops.process.dao.`var`.PublicVarDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupReferInfoDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupVersionSummaryDao
import com.tencent.devops.process.dao.`var`.PublicVarVersionSummaryDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.mq.ModelVarReferenceEvent
import com.tencent.devops.process.pojo.`var`.PublicGroupKey
import com.tencent.devops.process.pojo.`var`.VarGroupVersionChangeInfo
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupReferDTO
import com.tencent.devops.process.pojo.`var`.enums.PublicVarTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PublicVarGroupVersionSummaryPO
import com.tencent.devops.process.pojo.`var`.po.PublicVarPositionPO
import com.tencent.devops.process.pojo.`var`.po.PublicVarVersionSummaryPO
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarGroupReferPO
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PublicVarGroupReferManageService @Autowired constructor(
    private val dslContext: DSLContext,
    private val publicVarGroupDao: PublicVarGroupDao,
    private val client: Client,
    private val publicVarGroupReferInfoDao: PublicVarGroupReferInfoDao,
    private val publicVarGroupVersionSummaryDao: PublicVarGroupVersionSummaryDao,
    private val publicVarVersionSummaryDao: PublicVarVersionSummaryDao,
    private val templatePipelineDao: TemplatePipelineDao,
    private val templateDao: TemplateDao,
    private val publicVarDao: PublicVarDao,
    private val sampleEventDispatcher: SampleEventDispatcher,
    private val publicVarGroupReferCountService: PublicVarGroupReferCountService,
    private val varRefDetailDao: VarRefDetailDao,
    private val redisOperation: RedisOperation
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarGroupReferManageService::class.java)
    }

    /**
     * 创建引用级别的分布式锁
     * 锁粒度：
     * - 如果提供referVersion：项目ID + 引用ID + 版本号（版本级别），适用于单版本操作，提升并发性能
     * - 如果不提供referVersion：项目ID + 引用ID（引用级别），适用于跨版本操作
     *
     * @param projectId 项目ID
     * @param referId 引用ID
     * @param referType 引用类型
     * @param referVersion 引用版本号（可选，提供时使用版本级别锁，不提供时使用引用级别锁）
     * @return RedisLock实例
     */
    private fun createReferLock(
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum,
        referVersion: Int? = null
    ): RedisLock {
        val lockKey = if (referVersion != null) {
            "$PUBLIC_VAR_GROUP_REFER_LOCK_KEY_PREFIX:$projectId:$referType:$referId:$referVersion"
        } else {
            "$PUBLIC_VAR_GROUP_REFER_LOCK_KEY_PREFIX:$projectId:$referType:$referId"
        }
        return RedisLock(
            redisOperation = redisOperation,
            lockKey = lockKey,
            expiredTimeInSeconds = PUBLIC_VAR_GROUP_LOCK_EXPIRED_TIME_IN_SECONDS
        )
    }

    /**
     * 变量组级别的 summary 串行化锁（跨 referId）。
     * summary 行按 (项目, 组名) 聚合多个 referId 的引用，故并发重算需按"变量组"串行，
     * 而不是按 referId——否则两条不同流水线同时发布同一变量组时会出现读改写竞态。
     */
    private fun createGroupSummaryLock(projectId: String, groupName: String): RedisLock {
        return RedisLock(
            redisOperation = redisOperation,
            lockKey = "$PUBLIC_VAR_GROUP_REFER_LOCK_KEY_PREFIX:summary:$projectId:$groupName",
            expiredTimeInSeconds = PUBLIC_VAR_GROUP_LOCK_EXPIRED_TIME_IN_SECONDS
        )
    }

    /**
     * 按变量组串行执行 action（通常内部再开启 DB 事务）。
     * 关键点：锁必须在事务开启前获取、事务提交后释放——这样等待方的事务读视图建立于对方提交之后，
     * 避免 MySQL 快照隔离下"读到旧快照"的问题（DB 行级 FOR UPDATE 在保存链路早已建立读视图，无法保证这一点）。
     * 多组按 groupName 排序加锁，避免 AB-BA 死锁。
     * 说明：对外暴露，供变量明细异步写入链路（PublicVarReferInfoService）复用同一把组级锁，
     * 保证变量级 summary 重算与组级操作对同一变量组串行化。
     */
    fun <T> executeWithGroupSummaryLocks(
        projectId: String,
        groupNames: Collection<String>,
        action: () -> T
    ): T {
        val locks = groupNames.toSortedSet().map { createGroupSummaryLock(projectId, it) }
        locks.forEach { it.lock() }
        try {
            return action()
        } finally {
            locks.asReversed().forEach { lock -> runCatching { lock.unlock() } }
        }
    }

    /**
     * 根据引用ID删除变量组引用（自行管理事务）
     * @see deletePublicVerGroupRefByReferId(transactionContext, projectId, referId, referType)
     */
    fun deletePublicVerGroupRefByReferId(
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum
    ) {
        deletePublicVerGroupRefByReferId(
            transactionContext = null,
            projectId = projectId,
            referId = referId,
            referType = referType
        )
    }

    /**
     * 根据引用ID删除变量组引用（可复用外部事务，B-2）
     * @param transactionContext 外部事务 DSLContext，null 表示自行管理事务
     */
    fun deletePublicVerGroupRefByReferId(
        transactionContext: DSLContext?,
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum
    ) {
        val lock = createReferLock(
            projectId = projectId,
            referId = referId,
            referType = referType
        )
        lock.lock()
        try {
            val queryContext = transactionContext ?: dslContext
            val referInfosToDelete = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferId(
                dslContext = queryContext,
                projectId = projectId,
                referId = referId,
                referType = referType
            )

            if (referInfosToDelete.isEmpty()) {
                logger.info("No reference found for referId: $referId, skip deletion")
                return
            }
            val affectedGroups = referInfosToDelete.map { it.groupName }.toSet()
            // 引用删除 + 明细清理 + summary 重算需同事务，避免"明细已删、summary 未更"的漂移窗口。
            // 按组串行化，避免与并发保存/删除对同一组 summary 的读改写竞态。
            if (transactionContext != null) {
                // 复用外部事务：无法把锁持有到外部提交，此处按组加锁为尽力而为（配合绝对重算可自愈）。
                executeWithGroupSummaryLocks(projectId, affectedGroups) {
                    removeReferAndRefreshSummary(
                        context = transactionContext,
                        projectId = projectId,
                        referId = referId,
                        referType = referType,
                        referInfosToDelete = referInfosToDelete,
                        affectedGroups = affectedGroups,
                        referVersion = null
                    )
                }
            } else {
                executeWithGroupSummaryLocks(projectId, affectedGroups) {
                    dslContext.transaction { configuration ->
                        removeReferAndRefreshSummary(
                            context = DSL.using(configuration),
                            projectId = projectId,
                            referId = referId,
                            referType = referType,
                            referInfosToDelete = referInfosToDelete,
                            affectedGroups = affectedGroups,
                            referVersion = null
                        )
                    }
                }
            }
        } catch (t: Throwable) {
            logger.warn("Failed to delete refer info for referId: $referId", t)
            throw ErrorCodeException(errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_UPDATE_FAILED)
        } finally {
            lock.unlock()
        }
    }

    /**
     * 在给定事务内删除引用记录 + 变量引用明细，并对受影响变量组重算 summary。
     * 供各删除入口复用，保证删除与计数更新原子一致。
     */
    private fun removeReferAndRefreshSummary(
        context: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum,
        referInfosToDelete: List<ResourcePublicVarGroupReferPO>,
        affectedGroups: Set<String>,
        referVersion: Int?
    ) {
        publicVarGroupReferCountService.batchRemoveReferInfo(
            transactionContext = context,
            projectId = projectId,
            referId = referId,
            referType = referType,
            referInfosToDelete = referInfosToDelete,
            referVersion = referVersion
        )
        varRefDetailDao.deleteByResourceId(
            dslContext = context,
            projectId = projectId,
            resourceId = referId,
            resourceType = referType.name,
            referVersion = referVersion
        )
        // 删除链路在本事务内同步删除了变量组引用明细与变量引用明细两张表，故组级/变量级 summary 可一并重算。
        refreshGroupLevelSummary(
            context = context,
            projectId = projectId,
            userId = "system",
            groupNames = affectedGroups
        )
        refreshVarLevelSummary(
            context = context,
            projectId = projectId,
            userId = "system",
            groupNames = affectedGroups
        )
    }

    /**
     * 获取源头项目ID
     * 当referHasSource为true时，根据referType检查referId是否存在源模板，并获取源模板所属的项目ID
     * @param projectId 当前项目ID
     * @param referId 引用资源ID（模板ID或流水线ID）
     * @param referType 引用类型（TEMPLATE或PIPELINE）
     * @param referHasSource 是否存在源模板
     * @return 源头项目ID，如果不存在源头则返回当前projectId
     */
    private fun getSourceProjectId(
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum,
        referHasSource: Boolean
    ): String {
        // 如果referHasSource为false，直接返回当前projectId
        if (!referHasSource) {
            return projectId
        }

        return try {
            when (referType) {
                PublicVarGroupReferenceTypeEnum.TEMPLATE -> {
                    // 模板类型：直接查询T_TEMPLATE.SRC_TEMPLATE_ID
                    getSourceProjectIdForTemplate(projectId, referId)
                }
                PublicVarGroupReferenceTypeEnum.PIPELINE -> {
                    // 流水线类型：先查询T_TEMPLATE_PIPELINE判断是否为模板实例
                    getSourceProjectIdForPipeline(projectId, referId)
                }
            }
        } catch (e: Throwable) {
            logger.warn("Failed to get source project id for referId: $referId, referType: $referType", e)
            projectId
        }
    }

    /**
     * 获取模板类型的源头项目ID
     * 查询T_TEMPLATE.SRC_TEMPLATE_ID，如果存在则根据源模板ID查询其所属项目
     */
    private fun getSourceProjectIdForTemplate(projectId: String, templateId: String): String {
        // 查询模板的源模板ID
        val srcTemplateId = templateDao.getSrcTemplateId(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId
        )

        if (srcTemplateId.isNullOrBlank()) {
            return projectId
        }

        // 根据源模板ID查询其所属项目ID
        val sourceProjectId = templateDao.getProjectIdByTemplateId(
            dslContext = dslContext,
            templateId = srcTemplateId
        )

        return sourceProjectId ?: projectId
    }

    /**
     * 获取流水线类型的源头项目ID
     * 先查询T_TEMPLATE_PIPELINE判断流水线是否为模板实例，
     * 如果是则获取TEMPLATE_ID，再查询T_TEMPLATE.SRC_TEMPLATE_ID获取源模板所属项目
     */
    private fun getSourceProjectIdForPipeline(projectId: String, pipelineId: String): String {
        val templateId = templatePipelineDao.get(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )?.templateId

        if (templateId.isNullOrBlank()) {
            return projectId
        }

        // 查询模板的源模板ID
        val srcTemplateId = templateDao.getSrcTemplateId(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId
        )

        if (srcTemplateId.isNullOrBlank()) {
            return projectId
        }

        // 根据源模板ID查询其所属项目ID
        val sourceProjectId = templateDao.getProjectIdByTemplateId(
            dslContext = dslContext,
            templateId = srcTemplateId
        )

        return sourceProjectId ?: projectId
    }

    /**
     * 校验变量组引用合法性，供调用方在事务前调用（B-1）
     * 调用方事务前调本方法，事务后调 [handleVarGroupReferBus] 只做纯写入。
     * handlePublicVarInfo 幂等，前置调用后 handleVarGroupReferBus 内不会重复处理。
     */
    fun validateVarGroupReferences(
        model: Model,
        projectId: String
    ) {
        val params = model.getTriggerParams()
        if (params.isNotEmpty()) {
            validateParamIds(params)
        }
        model.handlePublicVarInfo()
        val publicVarGroups = model.publicVarGroups
        publicVarGroups?.let {
            validatePublicVarGroupsExist(projectId, it)
            validateVarNameConflict(projectId, it)
        }
    }

    /**
     * 检测引用的多个变量组之间是否存在同名变量冲突（B-5）
     */
    private fun validateVarNameConflict(
        projectId: String,
        publicVarGroups: List<PublicVarGroupRef>
    ) {
        if (publicVarGroups.size <= 1) return

        val groupNameVersionPairs = publicVarGroups.map { it.groupName to it.version }
        val groupRecords = publicVarGroupDao.batchGetRecordsByGroupNameAndVersion(
            dslContext = dslContext,
            projectId = projectId,
            groupNameVersionPairs = groupNameVersionPairs
        )
        if (groupRecords.isEmpty()) return

        val allVarPOs = publicVarDao.batchListVarsByGroupNameAndVersion(
            dslContext = dslContext,
            projectId = projectId,
            groupNameVersionList = groupRecords.map { it.groupName to it.version }
        )

        // 跨组同名冲突检测：varName -> 首次出现的 groupName
        val varsByGroup = allVarPOs.groupBy { it.groupName }
        val seenVarNames = mutableMapOf<String, String>()
        publicVarGroups.forEach { ref ->
            val varNames = varsByGroup[ref.groupName] ?: return@forEach
            varNames.forEach { varPO ->
                val firstGroup = seenVarNames[varPO.varName]
                if (firstGroup != null && firstGroup != ref.groupName) {
                    throw ErrorCodeException(
                        errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_CONFLICT,
                        params = arrayOf("$firstGroup & ${ref.groupName}", varPO.varName)
                    )
                }
                seenVarNames[varPO.varName] = ref.groupName
            }
        }
    }

    /**
     * 处理变量组引用写入（引用记录 + LATEST_FLAG）
     * 计数体系已下线（展示计数改为实时 JOIN 聚合），本方法只维护引用关联记录与 LATEST_FLAG。
     * 引用记录写入与 LATEST_FLAG 同步在同一 DB 事务内完成，保证原子性。
     * 校验逻辑已提取到 [validateVarGroupReferences]，调用方应事务前校验、事务后调本方法。
     * 未前置校验的调用方由 needFallbackValidation 兜底。
     */
    fun handleVarGroupReferBus(
        publicVarGroupReferDTO: PublicVarGroupReferDTO
    ) {
        val model = publicVarGroupReferDTO.model
        val params = model.getTriggerParams()
        logger.info(
            "handleVarGroupReferBus: projectId=${publicVarGroupReferDTO.projectId}, " +
                "referId=${publicVarGroupReferDTO.referId}, referType=${publicVarGroupReferDTO.referType}, " +
                "referVersion=${publicVarGroupReferDTO.referVersion}, " +
                "activeVersion=${publicVarGroupReferDTO.activeVersion}"
        )

        // 兜底校验：未前置调 validateVarGroupReferences 的调用方走这里
        val needFallbackValidation = model.publicVarGroups == null
        if (needFallbackValidation && params.isNotEmpty()) {
            validateParamIds(params)
        }

        val lock = createReferLock(
            projectId = publicVarGroupReferDTO.projectId,
            referId = publicVarGroupReferDTO.referId,
            referType = publicVarGroupReferDTO.referType,
            referVersion = publicVarGroupReferDTO.referVersion
        )
        lock.lock()
        try {
            val historicalReferInfos = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferId(
                dslContext = dslContext,
                projectId = publicVarGroupReferDTO.projectId,
                referId = publicVarGroupReferDTO.referId,
                referType = publicVarGroupReferDTO.referType,
                referVersion = publicVarGroupReferDTO.referVersion
            )
            // 无引用且无历史引用：仅同步 LATEST_FLAG 后直接返回
            // 注意：YAML template 场景下 params 可能为空但 model.publicVarGroups 非空，
            // 需调用 handlePublicVarInfo 后再判断
            if (handleEmptyReferEarlyReturn(publicVarGroupReferDTO, model, params, historicalReferInfos)) {
                return
            }
            model.handlePublicVarInfo()
            val publicVarGroups = model.publicVarGroups
            if (needFallbackValidation) {
                publicVarGroups?.let { validatePublicVarGroupsExist(publicVarGroupReferDTO.projectId, it) }
            }
            val pipelinePublicVarGroupReferPOs = processDynamicVarGroups(
                publicVarGroupReferDTO = publicVarGroupReferDTO,
                params = params
            )
            // YAML template 场景：publicVarGroups 中的组在 params 中没有对应项，补充 PO 使关联能正常更新。
            // 说明：取号（远程调用）在 processDynamicVarGroups / buildAdditionalReferPOs 内完成，均位于下方 DB 事务之外。
            val allReferPOs = pipelinePublicVarGroupReferPOs + buildAdditionalReferPOs(
                publicVarGroupReferDTO = publicVarGroupReferDTO,
                publicVarGroups = publicVarGroups,
                pipelinePublicVarGroupReferPOs = pipelinePublicVarGroupReferPOs
            )
            val currentGroupNames = publicVarGroups?.map { it.groupName }?.toSet() ?: emptySet()
            val historicalGroupNames = historicalReferInfos.map { it.groupName }.toSet()
            val projectId = publicVarGroupReferDTO.projectId
            val publicVarGroupNames = publicVarGroups?.map { it.groupName } ?: emptyList()
            if (publicVarGroupReferDTO.activeVersion) {
                // 生效版本：需同步 LATEST_FLAG 并重算 summary。受影响组 = 当前 ∪ 历史 ∪ 该 referId 当前已置位的组。
                // 先按组加串行化锁再开事务，保证跨 referId 的 summary 重算不发生读改写竞态。
                val flaggedGroups = publicVarGroupReferInfoDao.listLatestFlagGroupNamesByReferId(
                    dslContext = dslContext,
                    projectId = projectId,
                    referId = publicVarGroupReferDTO.referId,
                    referType = publicVarGroupReferDTO.referType
                )
                val affectedGroups = currentGroupNames + historicalGroupNames + flaggedGroups
                executeWithGroupSummaryLocks(projectId, affectedGroups) {
                    // 引用记录写入 + LATEST_FLAG 同步 + summary 重算放入同一事务，保证原子性
                    dslContext.transaction { configuration ->
                        val transactionContext = DSL.using(configuration)
                        updateReferenceCountsAfterSave(
                            context = transactionContext,
                            projectId = projectId,
                            historicalReferInfos = historicalReferInfos,
                            publicVarGroupNames = publicVarGroupNames,
                            resourcePublicVarGroupReferPOS = allReferPOs
                        )
                        val syncedGroups = syncLatestFlagForAllGroups(
                            context = transactionContext,
                            publicVarGroupReferDTO = publicVarGroupReferDTO,
                            currentGroupNames = currentGroupNames,
                            involvedGroupNames = affectedGroups
                        )
                        // 组级 summary：变量组引用明细在本事务内同步更新，即时重算。
                        refreshGroupLevelSummary(
                            context = transactionContext,
                            projectId = projectId,
                            userId = publicVarGroupReferDTO.userId,
                            groupNames = syncedGroups
                        )
                        // 变量级 summary：
                        // - 被移除的组（不再被当前版本引用）在本事务内 LATEST_FLAG 已清零，变量级计数即时清算为准；
                        //   且不会有后续异步变量明细事件覆盖它们，故必须在此清算。
                        // - 仍被当前版本引用的组，其新增变量明细经 MQ 异步写入，变量级 summary 由
                        //   PublicVarReferInfoService 在写入明细的同一事务内重算，不在此处处理（避免读到未写入的旧明细）。
                        val removedGroups = syncedGroups - currentGroupNames
                        refreshVarLevelSummary(
                            context = transactionContext,
                            projectId = projectId,
                            userId = publicVarGroupReferDTO.userId,
                            groupNames = removedGroups
                        )
                    }
                }
            } else {
                // 草稿保存（COMMITTING）：只写引用明细、不改动 LATEST_FLAG，也不影响 summary（引用计数只算生效版本）。
                // 仍需按组加串行化锁：删除保护会把草稿引用明细算进去（存在任意引用即禁止删组），
                // 若草稿写入与 deleteGroup 不串行，会出现"删组时校验无引用→草稿刚写入引用明细"的竞态残留孤儿引用。
                val draftAffectedGroups = currentGroupNames + historicalGroupNames
                executeWithGroupSummaryLocks(projectId, draftAffectedGroups) {
                    dslContext.transaction { configuration ->
                        updateReferenceCountsAfterSave(
                            context = DSL.using(configuration),
                            projectId = projectId,
                            historicalReferInfos = historicalReferInfos,
                            publicVarGroupNames = publicVarGroupNames,
                            resourcePublicVarGroupReferPOS = allReferPOs
                        )
                    }
                }
            }
        } finally {
            lock.unlock()
        }
        sampleEventDispatcher.dispatch(
            ModelVarReferenceEvent(
                userId = publicVarGroupReferDTO.userId,
                projectId = publicVarGroupReferDTO.projectId,
                resourceId = publicVarGroupReferDTO.referId,
                resourceType = publicVarGroupReferDTO.referType.name,
                resourceVersion = publicVarGroupReferDTO.referVersion,
                resourceVersionName = publicVarGroupReferDTO.referVersionName
            )
        )
    }

    /**
     * params 和历史引用均为空时的早期返回处理：
     * 调用 handlePublicVarInfo 后若 publicVarGroups 为空，则在独立事务内同步 LATEST_FLAG 后直接返回。
     * @return true 表示已处理并应 return，false 表示需继续后续流程
     */
    private fun handleEmptyReferEarlyReturn(
        publicVarGroupReferDTO: PublicVarGroupReferDTO,
        model: Model,
        params: MutableList<BuildFormProperty>,
        historicalReferInfos: List<ResourcePublicVarGroupReferPO>
    ): Boolean {
        if (params.isNotEmpty() || historicalReferInfos.isNotEmpty()) {
            return false
        }
        model.handlePublicVarInfo()
        if (model.publicVarGroups.isNullOrEmpty()) {
            // 仅生效版本（RELEASED/BRANCH）在"无任何引用"时才清空 LATEST_FLAG；
            // 草稿保存（COMMITTING）不改动 LATEST_FLAG，避免误伤已发布版本的引用计数。
            if (publicVarGroupReferDTO.activeVersion) {
                val projectId = publicVarGroupReferDTO.projectId
                // 受影响组 = 该 referId 当前已置位的组（现在要全部清空）
                val flaggedGroups = publicVarGroupReferInfoDao.listLatestFlagGroupNamesByReferId(
                    dslContext = dslContext,
                    projectId = projectId,
                    referId = publicVarGroupReferDTO.referId,
                    referType = publicVarGroupReferDTO.referType
                )
                if (flaggedGroups.isNotEmpty()) {
                    executeWithGroupSummaryLocks(projectId, flaggedGroups) {
                        dslContext.transaction { configuration ->
                            val transactionContext = DSL.using(configuration)
                            val syncedGroups = syncLatestFlagForAllGroups(
                                context = transactionContext,
                                publicVarGroupReferDTO = publicVarGroupReferDTO,
                                currentGroupNames = emptySet(),
                                involvedGroupNames = flaggedGroups
                            )
                            // 当前已无任何引用：syncedGroups 全部为被移除的组，其 LATEST_FLAG 在本事务内清零，
                            // 组级与变量级 summary 均可即时清算（不会有后续异步变量明细事件覆盖它们）。
                            refreshGroupLevelSummary(
                                context = transactionContext,
                                projectId = projectId,
                                userId = publicVarGroupReferDTO.userId,
                                groupNames = syncedGroups
                            )
                            refreshVarLevelSummary(
                                context = transactionContext,
                                projectId = projectId,
                                userId = publicVarGroupReferDTO.userId,
                                groupNames = syncedGroups
                            )
                        }
                    }
                }
            }
            return true
        }
        return false
    }

    /**
     * YAML template 场景：publicVarGroups 中的组在 params 中没有对应项，
     * 构建补充 PO 使关联和计数能正常更新。
     */
    private fun buildAdditionalReferPOs(
        publicVarGroupReferDTO: PublicVarGroupReferDTO,
        publicVarGroups: List<PublicVarGroupRef>?,
        pipelinePublicVarGroupReferPOs: List<ResourcePublicVarGroupReferPO>
    ): List<ResourcePublicVarGroupReferPO> {
        if (publicVarGroups.isNullOrEmpty()) {
            return emptyList()
        }
        val coveredGroupNames = pipelinePublicVarGroupReferPOs.map { it.groupName }.toSet()
        val missingGroups = publicVarGroups.filter { it.groupName !in coveredGroupNames }
        if (missingGroups.isEmpty()) {
            return emptyList()
        }
        val bizTag = "T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO"
        val segmentIds = client.get(ServiceAllocIdResource::class)
            .batchGenerateSegmentId(bizTag, missingGroups.size).data
        if (segmentIds.isNullOrEmpty() || segmentIds.size != missingGroups.size || segmentIds.any { it == null }) {
            logger.warn(
                "Failed to generate segment IDs: bizTag=$bizTag, " +
                    "requested=${missingGroups.size}, got=${segmentIds?.size}, " +
                    "hasNull=${segmentIds?.any { it == null }}"
            )
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_ADD_FAILED,
                params = arrayOf("ID allocation failed for $bizTag")
            )
        }
        val now = LocalDateTime.now()
        return missingGroups.mapIndexed { index, ref ->
            ResourcePublicVarGroupReferPO(
                id = segmentIds[index]!!,
                projectId = publicVarGroupReferDTO.projectId,
                groupName = ref.groupName,
                version = ref.version ?: DYNAMIC_VERSION,
                referId = publicVarGroupReferDTO.referId,
                referType = publicVarGroupReferDTO.referType,
                referName = publicVarGroupReferDTO.referName,
                referVersion = publicVarGroupReferDTO.referVersion,
                referVersionName = publicVarGroupReferDTO.referVersionName,
                positionInfo = emptyList(),
                creator = publicVarGroupReferDTO.userId,
                modifier = publicVarGroupReferDTO.userId,
                createTime = now,
                updateTime = now,
                // 仅生效版本直接置 true；草稿保存置 false，由后续发布时的 syncLatestFlag 统一维护，
                // 与 createReferRecords（默认 false）保持一致，避免草稿绕过门控污染引用计数。
                latestFlag = publicVarGroupReferDTO.activeVersion
            )
        }
    }

    /**
     * 同步 LATEST_FLAG：保证每个 (projectId, referId, referType, groupName) 下最多一条 LATEST_FLAG=true，
     * 且该行对应 currentReferVersion（如果当前保存版本仍在引用该 groupName）。
     * 处理逻辑：
     * 1. 汇总需要同步的 groupName 集合（涉及 = 当前版本引用的 ∪ 历史曾引用的 ∪ DB 中当前 LATEST_FLAG=true 的）
     * 2. 对每个 groupName：
     *    - 先将该 (referId, groupName) 下所有 LATEST_FLAG=true 的行置为 false
     *    - 若 currentGroupNames 包含该 groupName，则将当前 referVersion 对应行置为 true
     * 外层已提供锁保护，此方法无需加锁。
     */
    private fun syncLatestFlagForAllGroups(
        context: DSLContext,
        publicVarGroupReferDTO: PublicVarGroupReferDTO,
        currentGroupNames: Set<String>,
        involvedGroupNames: Set<String>? = null
    ): Set<String> {
        val projectId = publicVarGroupReferDTO.projectId
        val referId = publicVarGroupReferDTO.referId
        val referType = publicVarGroupReferDTO.referType
        val referVersion = publicVarGroupReferDTO.referVersion

        // 汇总需要同步的 groupName：调用方传入的 + DB 中当前 LATEST_FLAG=true 的（覆盖完全卸载场景）
        val groupNamesWithLatestFlag = publicVarGroupReferInfoDao.listLatestFlagGroupNamesByReferId(
            dslContext = context,
            projectId = projectId,
            referId = referId,
            referType = referType
        )
        val groupsToSync = (involvedGroupNames ?: currentGroupNames) + groupNamesWithLatestFlag

        if (groupsToSync.isEmpty()) return emptySet()

        groupsToSync.forEach { groupName ->
            // 将该 (referId, groupName) 下所有行的 LATEST_FLAG 置为 false
            publicVarGroupReferInfoDao.clearLatestFlag(
                dslContext = context,
                projectId = projectId,
                referId = referId,
                referType = referType,
                groupName = groupName
            )
            // 若当前版本仍引用该变量组，把当前 referVersion 行置为 true
            if (groupName in currentGroupNames) {
                val updated = publicVarGroupReferInfoDao.setLatestFlag(
                    dslContext = context,
                    projectId = projectId,
                    referId = referId,
                    referType = referType,
                    groupName = groupName,
                    referVersion = referVersion
                )
                if (updated == 0) {
                    logger.warn(
                        "syncLatestFlag: no row updated to true, " +
                            "referId=$referId, groupName=$groupName, referVersion=$referVersion"
                    )
                }
            }
        }
        return groupsToSync
    }

    /**
     * 重算并覆盖写入【组级】引用数概要（T_RESOURCE_PUBLIC_VAR_GROUP_VERSION_SUMMARY）。
     *
     * 语义：对每个受影响的 groupName，从【变量组引用明细表】聚合当前 LATEST_FLAG=true 的去重引用数（权威口径），
     * 用"绝对值覆盖"写入 summary，并删除已归零的行——不做增量增减，因此不会累积误差、可自愈。
     * 必须与触发它的引用变更（LATEST_FLAG 同步 / 引用删除）在同一事务、同一按组串行化锁内调用，
     * 保证不产生"明细已变、summary 未更"的漂移窗口，也不发生跨 referId 的读改写竞态。
     *
     * 注意：组级概要的权威源是【变量组引用明细表 T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO】，
     * 它在保存链路的本事务内同步写入，故可在此重算。变量级概要依赖【变量引用明细表
     * T_RESOURCE_PUBLIC_VAR_REFER_INFO】，后者在保存链路是经 MQ 异步写入，
     * 因此变量级重算不放在此处，见 [refreshVarLevelSummary]。
     *
     * 性能：仅在需要新建概要行（UPDATE 命中 0 行）时才批量取号（远程调用）；常见的重复保存命中已有行走纯 UPDATE。
     */
    private fun refreshGroupLevelSummary(
        context: DSLContext,
        projectId: String,
        userId: String,
        groupNames: Collection<String>
    ) {
        if (groupNames.isEmpty()) return
        val now = LocalDateTime.now()
        // 需要新建的概要行（UPDATE 命中 0 行）
        val newGroupRows = mutableListOf<Triple<String, Int, Int>>() // group, version, count
        groupNames.toSet().forEach { groupName ->
            refreshGroupLevelForOneGroup(context, projectId, userId, groupName, newGroupRows)
        }
        insertNewGroupSummaryRows(context, projectId, userId, now, newGroupRows)
    }

    /**
     * 重算并覆盖写入【变量级】引用数概要（T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY）。
     *
     * 语义与组级一致（绝对值覆盖、可自愈），但权威源是【变量引用明细表 T_RESOURCE_PUBLIC_VAR_REFER_INFO】
     * JOIN 变量组引用明细表 LATEST_FLAG=true。由于变量引用明细在保存链路经 MQ 异步写入，
     * 本方法必须在【写入变量引用明细的同一事务内】调用（见 PublicVarReferInfoService 异步链路），
     * 才能读到一致的明细；删除链路因在同一事务内同步删除两张明细表，也在删除事务内直接调用本方法。
     * 调用方需在事务开启前用 [executeWithGroupSummaryLocks] 对受影响组加锁，避免跨 referId 读改写竞态。
     */
    fun refreshVarLevelSummary(
        context: DSLContext,
        projectId: String,
        userId: String,
        groupNames: Collection<String>
    ) {
        if (groupNames.isEmpty()) return
        val now = LocalDateTime.now()
        val newVarRows = mutableListOf<VarSummaryRow>()
        groupNames.toSet().forEach { groupName ->
            refreshVarLevelForOneGroup(context, projectId, userId, groupName, newVarRows)
        }
        insertNewVarSummaryRows(context, projectId, userId, now, newVarRows)
    }

    private data class VarSummaryRow(val groupName: String, val varName: String, val version: Int, val count: Int)

    private fun refreshGroupLevelForOneGroup(
        context: DSLContext,
        projectId: String,
        userId: String,
        groupName: String,
        newGroupRows: MutableList<Triple<String, Int, Int>>
    ) {
        val versionCounts = publicVarGroupReferInfoDao.getLatestReferCountByVersion(
            dslContext = context,
            projectId = projectId,
            groupName = groupName
        )
        versionCounts.forEach { (version, count) ->
            val updated = publicVarGroupVersionSummaryDao.updateReferCount(
                dslContext = context,
                projectId = projectId,
                groupName = groupName,
                version = version,
                referCount = count,
                modifier = userId
            )
            if (updated == 0) newGroupRows.add(Triple(groupName, version, count))
        }
        publicVarGroupVersionSummaryDao.deleteByGroupNameExceptVersions(
            dslContext = context,
            projectId = projectId,
            groupName = groupName,
            keepVersions = versionCounts.keys
        )
    }

    private fun refreshVarLevelForOneGroup(
        context: DSLContext,
        projectId: String,
        userId: String,
        groupName: String,
        newVarRows: MutableList<VarSummaryRow>
    ) {
        val varVersionCounts = publicVarVersionSummaryDao.getLatestReferCountByVarAndVersion(
            dslContext = context,
            projectId = projectId,
            groupName = groupName
        )
        varVersionCounts.forEach { (key, count) ->
            val (varName, version) = key
            val updated = publicVarVersionSummaryDao.updateReferCount(
                dslContext = context,
                projectId = projectId,
                groupName = groupName,
                varName = varName,
                version = version,
                referCount = count,
                modifier = userId
            )
            if (updated == 0) newVarRows.add(VarSummaryRow(groupName, varName, version, count))
        }
        publicVarVersionSummaryDao.deleteByGroupNameExceptVarVersions(
            dslContext = context,
            projectId = projectId,
            groupName = groupName,
            keep = varVersionCounts.keys
        )
    }

    private fun insertNewGroupSummaryRows(
        context: DSLContext,
        projectId: String,
        userId: String,
        now: LocalDateTime,
        newRows: List<Triple<String, Int, Int>>
    ) {
        if (newRows.isEmpty()) return
        val bizTag = "T_RESOURCE_PUBLIC_VAR_GROUP_VERSION_SUMMARY"
        val ids = client.get(ServiceAllocIdResource::class)
            .batchGenerateSegmentId(bizTag, newRows.size).data
        if (ids.isNullOrEmpty() || ids.size != newRows.size || ids.any { it == null }) {
            logger.warn(
                "Failed to generate segment IDs: bizTag=$bizTag, " +
                    "requested=${newRows.size}, got=${ids?.size}, hasNull=${ids?.any { it == null }}"
            )
            throw ErrorCodeException(errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_UPDATE_FAILED)
        }
        newRows.forEachIndexed { index, (groupName, version, count) ->
            val id = ids[index]!!
            publicVarGroupVersionSummaryDao.save(
                dslContext = context,
                po = PublicVarGroupVersionSummaryPO(
                    id = id,
                    projectId = projectId,
                    groupName = groupName,
                    version = version,
                    referCount = count,
                    creator = userId,
                    modifier = userId,
                    createTime = now,
                    updateTime = now
                )
            )
        }
    }

    private fun insertNewVarSummaryRows(
        context: DSLContext,
        projectId: String,
        userId: String,
        now: LocalDateTime,
        newRows: List<VarSummaryRow>
    ) {
        if (newRows.isEmpty()) return
        val bizTag = "T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY"
        val ids = client.get(ServiceAllocIdResource::class)
            .batchGenerateSegmentId(bizTag, newRows.size).data
        if (ids.isNullOrEmpty() || ids.size != newRows.size || ids.any { it == null }) {
            logger.warn(
                "Failed to generate segment IDs: bizTag=$bizTag, " +
                    "requested=${newRows.size}, got=${ids?.size}, hasNull=${ids?.any { it == null }}"
            )
            throw ErrorCodeException(errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_UPDATE_FAILED)
        }
        newRows.forEachIndexed { index, row ->
            val id = ids[index]!!
            publicVarVersionSummaryDao.save(
                dslContext = context,
                po = PublicVarVersionSummaryPO(
                    id = id,
                    projectId = projectId,
                    groupName = row.groupName,
                    varName = row.varName,
                    version = row.version,
                    referCount = row.count,
                    creator = userId,
                    modifier = userId,
                    createTime = now,
                    updateTime = now
                )
            )
        }
    }

    /**
     * 处理跨项目变量组引用，更新model中的参数
     * 展开变量组中的变量到params，清除引用信息，清除publicVarGroups
     */
    fun handleCrossProjectVarGroup(
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum,
        referVersion: Int,
        model: Model
    ) {
        val params = model.getTriggerParams()
        if (params.isEmpty()) {
            return
        }

        // 获取源头项目ID
        val sourceProjectId = getSourceProjectId(
            projectId = projectId,
            referId = referId,
            referType = referType,
            referHasSource = true
        )

        // 使用分布式锁保护操作
        val lock = createReferLock(
            projectId = projectId,
            referId = referId,
            referType = referType,
            referVersion = referVersion
        )
        lock.lock()
        try {
            model.handlePublicVarInfo()
            val publicVarGroups = model.publicVarGroups

            // 验证变量组在源项目中是否存在
            publicVarGroups?.let { validatePublicVarGroupsExist(sourceProjectId, it) }

            // 展开跨项目变量组（跨项目不落引用记录）
            expandCrossProjectVarGroupsInternal(
                params = params,
                model = model,
                sourceProjectId = sourceProjectId
            )
            logger.info(
                "Cross-project var groups expanded: projectId=$projectId, referId=$referId, " +
                    "sourceProjectId=$sourceProjectId, groupCount=${publicVarGroups?.size ?: 0}"
            )
        } finally {
            lock.unlock()
        }
    }

    /**
     * 展开跨项目变量组（内部方法）
     * 将公共变量组的变量展开为普通变量
     */
    private fun expandCrossProjectVarGroupsInternal(
        params: MutableList<BuildFormProperty>,
        model: Model,
        sourceProjectId: String
    ) {
        val publicVarGroups = model.publicVarGroups

        if (publicVarGroups.isNullOrEmpty()) {
            return
        }

        // 预先收集所有需要处理的varGroupName集合，避免多次遍历params
        val groupNameSet = publicVarGroups.map { it.groupName }.toSet()

        // 一次遍历params，收集所有需要移除的占位符索引
        val indicesToRemove = mutableListOf<Int>()
        params.forEachIndexed { index, param ->
            if (param.varGroupName != null && groupNameSet.contains(param.varGroupName)) {
                indicesToRemove.add(index)
            }
        }

        // 收集所有需要添加的变量
        val varsToAdd = mutableListOf<BuildFormProperty>()

        // 遍历每个变量组，查询并收集变量
        publicVarGroups.forEach { publicVarGroup ->
            val groupName = publicVarGroup.groupName
            val versionName = publicVarGroup.versionName
            val version = publicVarGroup.version

            // 使用源项目ID查询变量组信息
            val varGroupRecord = publicVarGroupDao.getRecordByGroupName(
                dslContext = dslContext,
                projectId = sourceProjectId,
                groupName = groupName,
                version = version,
                versionName = versionName
            ) ?: throw ErrorCodeException(
                errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_NOT_EXIST,
                params = arrayOf(groupName)
            )

            // 使用源项目ID查询变量组中的变量
            val groupVars = publicVarDao.listVarByGroupName(
                dslContext = dslContext,
                projectId = sourceProjectId,
                groupName = groupName,
                version = varGroupRecord.version
            )

            // 将变量组的变量转换为BuildFormProperty并添加到待添加列表
            // 在添加前清除公共变量组引用信息，因为跨项目场景不保存引用记录
            groupVars.forEach { varPO ->
                val buildFormProperty = JsonUtil.to(varPO.buildFormProperty, BuildFormProperty::class.java)
                // 清除公共变量组引用信息
                buildFormProperty.varGroupName = null
                buildFormProperty.varGroupVersion = null
                varsToAdd.add(buildFormProperty)
            }
        }

        // 按降序移除占位符（避免索引偏移问题）
        indicesToRemove.distinct().sortedDescending().forEach { index ->
            if (index < params.size) {
                params.removeAt(index)
            }
        }

        // 将展开的变量添加到params列表
        params.addAll(varsToAdd)

        // 清除model中的公共变量组信息
        model.publicVarGroups = null
    }

    /**
     * 更新公共变量组引用计数
     * 使用优化后的数据结构，以 groupName 为维度管理变化信息
     *
     * @param context 外部事务上下文（引用记录写入与 LATEST_FLAG 同步共用同一事务）
     * @param projectId 项目ID
     * @param historicalReferInfos 历史引用信息列表
     * @param publicVarGroupNames 当前变量组名称列表
     * @param resourcePublicVarGroupReferPOS 新的引用信息列表，需要批量保存
     */
    private fun updateReferenceCountsAfterSave(
        context: DSLContext,
        projectId: String,
        historicalReferInfos: List<ResourcePublicVarGroupReferPO>,
        publicVarGroupNames: List<String>,
        resourcePublicVarGroupReferPOS: List<ResourcePublicVarGroupReferPO> = emptyList()
    ) {
        if (publicVarGroupNames.isEmpty() &&
            resourcePublicVarGroupReferPOS.isEmpty() && historicalReferInfos.isEmpty()) {
            return
        }
        try {
            // 步骤1：构建变化信息映射，Key为groupName
            val changeInfoMap = mutableMapOf<String, VarGroupVersionChangeInfo>()

            // 构建当前变量组名称集合，用于快速查找
            val currentGroupNames = publicVarGroupNames.toSet()

            // 构建历史记录的快速查找映射，Key为groupName
            val historicalGroupMap: Map<String, ResourcePublicVarGroupReferPO> =
                historicalReferInfos.associateBy { it.groupName }

            // 步骤2：处理历史引用记录 - 识别需要删除的引用
            historicalReferInfos.forEach { historical ->
                // 场景1：变量组在当前列表中不存在 -> 删除操作
                if (!currentGroupNames.contains(historical.groupName)) {
                    val changeInfo = changeInfoMap.getOrPut(historical.groupName) {
                        VarGroupVersionChangeInfo(
                            groupName = historical.groupName,
                            version = historical.version,
                            referId = historical.referId,
                            referType = historical.referType,
                            referVersion = historical.referVersion
                        )
                    }
                    changeInfo.referInfoToDelete = historical
                }
            }

            // 步骤3：处理当前引用记录 - 识别需要新增或版本切换的引用
            resourcePublicVarGroupReferPOS.forEach { current ->
                // 使用groupName查找历史记录
                val historical = historicalGroupMap[current.groupName]

                when {
                    // 场景2：变量组在历史列表中不存在 -> 新增操作
                    historical == null -> {
                        val changeInfo = changeInfoMap.getOrPut(current.groupName) {
                            VarGroupVersionChangeInfo(
                                groupName = current.groupName,
                                version = current.version,
                                referId = current.referId,
                                referType = current.referType,
                                referVersion = current.referVersion
                            )
                        }
                        changeInfo.referInfoToAdd = current
                    }
                    // 场景3：变量组在历史列表中存在且版本不同 -> 版本切换操作
                    historical.version != current.version -> {
                        val changeInfo = changeInfoMap.getOrPut(current.groupName) {
                            VarGroupVersionChangeInfo(
                                groupName = current.groupName,
                                version = current.version,
                                referId = current.referId,
                                referType = current.referType,
                                referVersion = current.referVersion
                            )
                        }
                        changeInfo.referInfoToDelete = historical
                        changeInfo.referInfoToAdd = current
                    }
                }
            }

            // 步骤4：过滤出有实际变化的记录
            val changeInfos = changeInfoMap.values.filter { it.hasChanges() }

            // 步骤5：批量写入引用记录（计数体系已下线，仅维护引用关联本身）
            if (changeInfos.isNotEmpty()) {
                publicVarGroupReferCountService.batchUpdateReferInfo(
                    context = context,
                    projectId = projectId,
                    changeInfos = changeInfos
                )
            }
        } catch (e: Throwable) {
            logger.warn("Failed to update reference count for projectId: $projectId", e)
            throw e
        }
    }

    /**
     * 验证公共变量参数ID是否重复（只检查varGroupName不为空的参数）
     */
    private fun validateParamIds(params: List<BuildFormProperty>) {
        // 只检查公共变量参数，统计ID出现次数并过滤出重复的
        val duplicateIds = params
            .filter { !it.varGroupName.isNullOrBlank() }
            .groupingBy { it.id }
            .eachCount()
            .filterValues { it > 1 }
            .keys

        if (duplicateIds.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_ID_DUPLICATE,
                params = arrayOf(duplicateIds.joinToString(", "))
            )
        }
    }

    /**
     * 验证变量组是否存在（含固定版本存在性校验，M-4）
     */
    private fun validatePublicVarGroupsExist(
        projectId: String,
        publicVarGroups: List<PublicVarGroupRef>
    ) {
        val groupNames = publicVarGroups.map { it.groupName }.distinct()

        // 批量查询变量组是否存在
        val existingGroups = publicVarGroupDao.listGroupsNameByProjectId(
            dslContext = dslContext,
            projectId = projectId
        ).toSet()

        // 找出不存在的变量组
        val notExistGroups = groupNames.filter { !existingGroups.contains(it) }

        if (notExistGroups.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_NOT_EXIST,
                params = arrayOf(notExistGroups.joinToString(", "))
            )
        }

        // 校验固定版本引用的版本是否存在（M-4）
        val fixedVersionRefs = publicVarGroups.filter { it.version != null }
        if (fixedVersionRefs.isNotEmpty()) {
            val groupNameVersionPairs = fixedVersionRefs.map { it.groupName to it.version }
            val existingRecords = publicVarGroupDao.batchGetRecordsByGroupNameAndVersion(
                dslContext = dslContext,
                projectId = projectId,
                groupNameVersionPairs = groupNameVersionPairs
            ).map { it.groupName to it.version }.toSet()

            val notExistVersions = fixedVersionRefs.filter { ref ->
                (ref.groupName to ref.version) !in existingRecords
            }
            if (notExistVersions.isNotEmpty()) {
                throw ErrorCodeException(
                    errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_NOT_EXIST,
                    params = arrayOf(notExistVersions.joinToString(", ") { "${it.groupName}@v${it.version}" })
                )
            }
        }
    }

    /**
     * 处理动态变量组
     */
    private fun processDynamicVarGroups(
        publicVarGroupReferDTO: PublicVarGroupReferDTO,
        params: MutableList<BuildFormProperty>
    ): List<ResourcePublicVarGroupReferPO> {

        // 提取非固定版本的变量组变量并记录位置信息
        val dynamicPublicVarWithPositions = params.withIndex()
            .filter { (_, element) ->
                !element.varGroupName.isNullOrBlank() && element.varGroupVersion == null
            }
            .groupBy { (_, element) ->
                PublicGroupKey(element.varGroupName!!, element.varGroupVersion)
            }
            .mapValues { (key, group) ->
                group.map { (index, element) ->
                    PublicVarPositionPO(
                        groupName = key.groupName,
                        version = key.version,
                        varName = element.id,
                        index = index,
                        type = if (element.constant == true) {
                            PublicVarTypeEnum.CONSTANT
                        } else {
                            PublicVarTypeEnum.VARIABLE
                        },
                        required = element.required
                    )
                }
            }

        // 提取固定版本的变量组（不记录位置信息，只创建引用记录）
        val fixedPublicVarGroups = params
            .filter { !it.varGroupName.isNullOrBlank() && it.varGroupVersion != null }
            .groupBy { PublicGroupKey(it.varGroupName!!, it.varGroupVersion) }
            .keys

        val resourcePublicVarGroupReferPOS = mutableListOf<ResourcePublicVarGroupReferPO>()

        if (dynamicPublicVarWithPositions.isNotEmpty()) {
            // 提取所有需要删除的索引，并按降序排序
            val indicesToRemove = dynamicPublicVarWithPositions.values
                .flatMap { it.map { po -> po.index } }
                .distinct()
                .sortedDescending()

            // 按降序索引从 params 中移除元素
            indicesToRemove.forEach { index ->
                if (index < params.size) {
                    params.removeAt(index)
                }
            }

            // 批量生成ID并保存动态版本引用记录
            resourcePublicVarGroupReferPOS.addAll(
                createReferRecords(
                    publicVarGroupReferDTO = publicVarGroupReferDTO,
                    dynamicPublicVarWithPositions = dynamicPublicVarWithPositions
                )
            )
        }

        // 为固定版本变量组创建引用记录（不包含位置信息）
        if (fixedPublicVarGroups.isNotEmpty()) {
            val fixedVarGroupMap = fixedPublicVarGroups.associateWith { emptyList<PublicVarPositionPO>() }
            resourcePublicVarGroupReferPOS.addAll(
                createReferRecords(
                    publicVarGroupReferDTO = publicVarGroupReferDTO,
                    dynamicPublicVarWithPositions = fixedVarGroupMap
                )
            )
        }

        return resourcePublicVarGroupReferPOS
    }

    /**
     * 创建引用记录
     */
    private fun createReferRecords(
        publicVarGroupReferDTO: PublicVarGroupReferDTO,
        dynamicPublicVarWithPositions: Map<PublicGroupKey, List<PublicVarPositionPO>>
    ): List<ResourcePublicVarGroupReferPO> {

        val currentTime = LocalDateTime.now()
        val bizTag = "T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO"
        val segmentIds = client.get(ServiceAllocIdResource::class).batchGenerateSegmentId(
            bizTag = bizTag,
            number = dynamicPublicVarWithPositions.size
        ).data

        if (segmentIds.isNullOrEmpty() ||
            segmentIds.size != dynamicPublicVarWithPositions.size ||
            segmentIds.any { it == null }
        ) {
            logger.warn(
                "Failed to generate segment IDs: bizTag=$bizTag, " +
                    "requested=${dynamicPublicVarWithPositions.size}, got=${segmentIds?.size}, " +
                    "hasNull=${segmentIds?.any { it == null }}"
            )
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_REST_EXCEPTION_COMMON_TIP
            )
        }

        return dynamicPublicVarWithPositions.entries.mapIndexed { index, (groupKey, positionInfos) ->
            ResourcePublicVarGroupReferPO(
                id = segmentIds[index]!!,
                projectId = publicVarGroupReferDTO.projectId,
                groupName = groupKey.groupName,
                version = groupKey.version ?: -1,
                referId = publicVarGroupReferDTO.referId,
                referName = publicVarGroupReferDTO.referName,
                referType = publicVarGroupReferDTO.referType,
                referVersion = publicVarGroupReferDTO.referVersion,
                referVersionName = publicVarGroupReferDTO.referVersionName,
                positionInfo = positionInfos,
                creator = publicVarGroupReferDTO.userId,
                modifier = publicVarGroupReferDTO.userId,
                createTime = currentTime,
                updateTime = currentTime
            )
        }
    }

    /**
     * 删除指定版本的变量组引用与变量引用记录
     * 使用版本级别的分布式锁保护，确保删除操作的原子性，避免并发修改导致的引用计数错误
     * 优化：使用版本级别的锁而不是引用级别的锁，提升并发性能
     */
    fun deletePublicGroupRefer(
        userId: String,
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum,
        referVersion: Int
    ) {
        // 使用版本级别的分布式锁保护整个删除流程
        val lock = createReferLock(
            projectId = projectId,
            referId = referId,
            referType = referType,
            referVersion = referVersion
        )
        lock.lock()
        try {
            // 查询要删除的引用记录（在锁保护下查询，确保数据一致性）
            val referInfosToDelete = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferId(
                dslContext = dslContext,
                projectId = projectId,
                referId = referId,
                referType = referType,
                referVersion = referVersion
            )
            if (referInfosToDelete.isEmpty()) {
                logger.info("No reference found for referId: $referId, referVersion: $referVersion, skip deletion")
                return
            }
            val affectedGroups = referInfosToDelete.map { it.groupName }.toSet()
            // 删除引用记录 + 明细清理 + summary 重算放入同一事务，保证原子一致；按组串行化避免并发竞态。
            executeWithGroupSummaryLocks(projectId, affectedGroups) {
                dslContext.transaction { configuration ->
                    removeReferAndRefreshSummary(
                        context = DSL.using(configuration),
                        projectId = projectId,
                        referId = referId,
                        referType = referType,
                        referInfosToDelete = referInfosToDelete,
                        affectedGroups = affectedGroups,
                        referVersion = referVersion
                    )
                }
            }
        } catch (t: Throwable) {
            logger.warn("Failed to delete refer info for referId: $referId with version: $referVersion", t)
            throw ErrorCodeException(errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_UPDATE_FAILED)
        } finally {
            lock.unlock()
        }
    }

    /**
     * 仅将公共变量组成员解析写回 TriggerContainer.params，不记录引用关系。
     * 适用于模板 YAML 转 Model 等无有效 referId 的场景。
     * 引用关系记录需在模板保存（持有 templateId）时单独处理。
     */
    fun addPublicVarGroupsToParams(
        model: Model,
        projectId: String,
        userId: String
    ) {
        val publicVarGroups = model.publicVarGroups
        if (publicVarGroups.isNullOrEmpty()) {
            return
        }

        val params = model.getTriggerParams()
        val referencedGroupNames = publicVarGroups.map { it.groupName }.toSet()
        // 清理 params 中已不再被引用的变量组变量（换组后残留的旧组）
        params.removeAll { param ->
            val groupName = param.varGroupName
            groupName != null && groupName !in referencedGroupNames
        }
        // 查出params中已存在的变量组名称
        val existingGroupNames = params
            .mapNotNull { it.varGroupName }
            .toSet()

        // 对比publicVarGroups，找出params中不存在的变量组
        val groupsToAdd = publicVarGroups.filter { publicVarGroup ->
            !existingGroupNames.contains(publicVarGroup.groupName)
        }
        if (groupsToAdd.isEmpty()) {
            return
        }
        logger.info(
            "addPublicVarGroupsToParams: projectId=$projectId, userId=$userId, " +
                "groups=${groupsToAdd.map { it.groupName }}"
        )
        groupsToAdd.forEach { publicVarGroup ->
            addVarGroupToParams(
                projectId = projectId,
                publicVarGroup = publicVarGroup,
                params = params
            )
        }
    }

    /**
     * 添加变量组到参数列表
     */
    private fun addVarGroupToParams(
        projectId: String,
        publicVarGroup: PublicVarGroupRef,
        params: MutableList<BuildFormProperty>
    ): MutableList<BuildFormProperty> {
        val groupName = publicVarGroup.groupName
        val versionName = publicVarGroup.versionName
        val version = publicVarGroup.version
        // 使用当前项目ID查询变量组信息
        val varGroupRecord = publicVarGroupDao.getRecordByGroupName(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            version = version,
            versionName = versionName
        ) ?: throw ErrorCodeException(
            errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_NOT_EXIST,
            params = arrayOf(groupName)
        )

        // 使用当前项目ID查询变量组中的变量
        val groupVars = publicVarDao.listVarByGroupName(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            version = varGroupRecord.version
        )
        // 将变量添加到params
        groupVars.forEach { varPO ->
            val buildFormProperty = JsonUtil.to(varPO.buildFormProperty, BuildFormProperty::class.java)
            buildFormProperty.varGroupName = groupName
            buildFormProperty.varGroupVersion = version
            params.add(buildFormProperty)
        }
        return params
    }
}