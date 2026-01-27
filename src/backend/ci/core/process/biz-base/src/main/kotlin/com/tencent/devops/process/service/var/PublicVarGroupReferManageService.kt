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
import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.PublicVarGroupRef
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_NOT_EXIST
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_UPDATE_FAILED
import com.tencent.devops.process.constant.ProcessMessageCode.PUBLIC_VAR_GROUP_LOCK_EXPIRED_TIME_IN_SECONDS
import com.tencent.devops.process.constant.ProcessMessageCode.PUBLIC_VAR_GROUP_REFER_LOCK_KEY_PREFIX
import com.tencent.devops.process.dao.`var`.PublicVarDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupReferInfoDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.mq.ModelVarReferenceEvent
import com.tencent.devops.process.pojo.`var`.PublicGroupKey
import com.tencent.devops.process.pojo.`var`.VarGroupVersionChangeInfo
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupReferDTO
import com.tencent.devops.process.pojo.`var`.enums.PublicVarTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PublicVarPositionPO
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarGroupReferPO
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PublicVarGroupReferManageService @Autowired constructor(
    private val dslContext: DSLContext,
    private val publicVarGroupDao: PublicVarGroupDao,
    private val client: Client,
    private val publicVarGroupReferInfoDao: PublicVarGroupReferInfoDao,
    private val templatePipelineDao: TemplatePipelineDao,
    private val templateDao: TemplateDao,
    private val publicVarDao: PublicVarDao,
    private val sampleEventDispatcher: SampleEventDispatcher,
    private val publicVarGroupReferCountService: PublicVarGroupReferCountService,
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
        referType: PublicVerGroupReferenceTypeEnum,
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
     * 根据引用ID删除变量组引用
     * 同时删除变量组引用记录和变量引用记录
     * 使用分布式锁保护，确保删除操作的原子性，避免并发修改导致的引用计数错误
     */
    fun deletePublicVerGroupRefByReferId(
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum
    ) {
        // 使用分布式锁保护整个删除流程
        val lock = createReferLock(
            projectId = projectId,
            referId = referId,
            referType = referType
        )
        lock.lock()
        try {
            // 查询要删除的引用记录（在锁保护下查询，确保数据一致性）
            val referInfosToDelete = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferId(
                dslContext = dslContext,
                projectId = projectId,
                referId = referId,
                referType = referType
            )

            if (referInfosToDelete.isEmpty()) {
                logger.info("No reference found for referId: $referId, skip deletion")
                return
            }

            // 删除变量组引用和变量引用记录（在锁保护下执行，确保原子性）
            publicVarGroupReferCountService.batchRemoveReferInfo(
                projectId = projectId,
                referId = referId,
                referType = referType,
                referInfosToDelete = referInfosToDelete
            )
        } catch (t: Throwable) {
            logger.warn("Failed to delete refer info for referId: $referId", t)
            throw ErrorCodeException(errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_UPDATE_FAILED)
        } finally {
            lock.unlock()
        }
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
        referType: PublicVerGroupReferenceTypeEnum,
        referHasSource: Boolean
    ): String {
        // 如果referHasSource为false，直接返回当前projectId
        if (!referHasSource) {
            return projectId
        }

        return try {
            when (referType) {
                PublicVerGroupReferenceTypeEnum.TEMPLATE -> {
                    // 模板类型：直接查询T_TEMPLATE.SRC_TEMPLATE_ID
                    getSourceProjectIdForTemplate(projectId, referId)
                }
                PublicVerGroupReferenceTypeEnum.PIPELINE -> {
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
     * 处理变量组引用业务逻辑
     * 使用分布式锁保护，确保同一引用的操作串行化，避免并发修改导致的引用计数错误
     */
    fun handleVarGroupReferBus(
        publicVarGroupReferDTO: PublicVarGroupReferDTO
    ) {
        val model = publicVarGroupReferDTO.model
        val params = model.getTriggerContainer().params
        if (params.isEmpty()) {
            return
        }
        // 检查参数ID是否存在重复
        validateParamIds(params)
        // 源头检查：当referHasSource为true时，获取源头项目ID
        val sourceProjectId = getSourceProjectId(
            projectId = publicVarGroupReferDTO.projectId,
            referId = publicVarGroupReferDTO.referId,
            referType = publicVarGroupReferDTO.referType,
            referHasSource = publicVarGroupReferDTO.referHasSource
        )
        // 使用版本级别的分布式锁保护整个操作流程，避免并发修改导致的引用计数错误
        val lock = createReferLock(
            projectId = publicVarGroupReferDTO.projectId,
            referId = publicVarGroupReferDTO.referId,
            referType = publicVarGroupReferDTO.referType,
            referVersion = publicVarGroupReferDTO.referVersion
        )
        lock.lock()
        try {
            // 查询历史引用记录（在锁保护下查询，确保数据一致性）
            val historicalReferInfos = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferId(
                dslContext = dslContext,
                projectId = publicVarGroupReferDTO.projectId,
                referId = publicVarGroupReferDTO.referId,
                referType = publicVarGroupReferDTO.referType,
                referVersion = publicVarGroupReferDTO.referVersion
            )
            model.handlePublicVarInfo()
            val publicVarGroups = model.publicVarGroups
            logger.info("handleVarGroupReferBus publicVarGroups: $publicVarGroups")
            publicVarGroups?.let { validatePublicVarGroupsExist(sourceProjectId, it) }
            // 提取并处理动态变量组
            val pipelinePublicVarGroupReferPOs = processDynamicVarGroups(
                publicVarGroupReferDTO = publicVarGroupReferDTO,
                params = params,
                sourceProjectId = sourceProjectId
            )
            // 更新引用计数和批量保存（在锁保护下执行，确保原子性）
            updateReferenceCountsAfterSave(
                projectId = publicVarGroupReferDTO.projectId,
                historicalReferInfos = historicalReferInfos,
                publicVarGroupNames = publicVarGroups?.map { it.groupName } ?: emptyList(),
                resourcePublicVarGroupReferPOS = pipelinePublicVarGroupReferPOs
            )
            logger.info("updateReferenceCountsAfterSave Success")
        } finally {
            lock.unlock()
        }
        // 发送事件（在锁外执行，避免阻塞其他操作）
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
     * 更新公共变量组引用计数
     * 使用优化后的数据结构，以(sourceProjectId, groupName)为维度管理变化信息
     *
     * @param projectId 项目ID
     * @param historicalReferInfos 历史引用信息列表
     * @param publicVarGroupNames 当前变量组名称列表
     * @param resourcePublicVarGroupReferPOS 新的引用信息列表，需要批量保存
     */
    private fun updateReferenceCountsAfterSave(
        projectId: String,
        historicalReferInfos: List<ResourcePublicVarGroupReferPO>,
        publicVarGroupNames: List<String>,
        resourcePublicVarGroupReferPOS: List<ResourcePublicVarGroupReferPO> = emptyList()
    ) {
        // 记录输入参数的关键信息
        logger.info(
            "updateReferenceCountsAfterSave - historicalReferInfos size: ${historicalReferInfos.size}, " +
                    "publicVarGroupNames: $publicVarGroupNames, " +
                    "resourcePublicVarGroupReferPOS size: ${resourcePublicVarGroupReferPOS.size}"
        )
        if (publicVarGroupNames.isEmpty() && resourcePublicVarGroupReferPOS.isEmpty()) {
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
                            sourceProjectId = historical.sourceProjectId ?: projectId,
                            referId = historical.referId,
                            referType = historical.referType,
                            referVersion = historical.referVersion
                        )
                    }
                    changeInfo.setDeleteOperation(historical)
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
                                sourceProjectId = current.sourceProjectId ?: projectId,
                                referId = current.referId,
                                referType = current.referType,
                                referVersion = current.referVersion
                            )
                        }
                        changeInfo.setAddOperation(current)
                    }
                    // 场景3：变量组在历史列表中存在且版本不同 -> 版本切换操作
                    historical.version != current.version -> {
                        val changeInfo = changeInfoMap.getOrPut(current.groupName) {
                            VarGroupVersionChangeInfo(
                                groupName = current.groupName,
                                version = current.version,
                                sourceProjectId = current.sourceProjectId ?: projectId,
                                referId = current.referId,
                                referType = current.referType,
                                referVersion = current.referVersion
                            )
                        }
                        changeInfo.setDeleteOperation(historical)
                        changeInfo.setAddOperation(current)
                    }
                }
            }

            // 步骤4：过滤出有实际变化的记录
            val changeInfos = changeInfoMap.values.filter { it.hasChanges() }

            // 步骤5：批量更新引用计数
            // 注意：外层已经提供了锁保护，PublicVarGroupReferCountService 内部不再使用锁，避免双重锁
            if (changeInfos.isNotEmpty()) {
                publicVarGroupReferCountService.batchUpdateReferWithCount(
                    projectId = projectId,
                    changeInfos = changeInfos
                )
            }
        } catch (e: Throwable) {
            logger.warn("Failed to update reference count for projectId: $projectId", e)
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
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_CONFLICT,
                params = arrayOf(duplicateIds.joinToString(", "))
            )
        }
    }

    /**
     * 验证变量组是否存在
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
    }

    /**
     * 处理动态变量组
     */
    private fun processDynamicVarGroups(
        publicVarGroupReferDTO: PublicVarGroupReferDTO,
        params: MutableList<BuildFormProperty>,
        sourceProjectId: String
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

            // 批量生成ID并保存
            resourcePublicVarGroupReferPOS.addAll(
                createReferRecords(
                    publicVarGroupReferDTO = publicVarGroupReferDTO,
                    dynamicPublicVarWithPositions = dynamicPublicVarWithPositions,
                    sourceProjectId = sourceProjectId
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
        dynamicPublicVarWithPositions: Map<PublicGroupKey, List<PublicVarPositionPO>>,
        sourceProjectId: String
    ): List<ResourcePublicVarGroupReferPO> {

        val currentTime = LocalDateTime.now()
        val segmentIds = client.get(ServiceAllocIdResource::class).batchGenerateSegmentId(
            bizTag = "T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO",
            number = dynamicPublicVarWithPositions.size
        ).data

        if (segmentIds.isNullOrEmpty() || segmentIds.size != dynamicPublicVarWithPositions.size) {
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
                sourceProjectId = if (sourceProjectId != publicVarGroupReferDTO.projectId) {
                    sourceProjectId
                } else {
                    null
                },
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
        referType: PublicVerGroupReferenceTypeEnum,
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
            // 删除变量组引用和变量引用记录（在锁保护下执行，确保原子性）
            publicVarGroupReferCountService.batchRemoveReferInfo(
                projectId = projectId,
                referId = referId,
                referType = referType,
                referInfosToDelete = referInfosToDelete,
                referVersion = referVersion
            )
        } catch (t: Throwable) {
            logger.warn("Failed to delete refer info for referId: $referId with version: $referVersion", t)
            throw ErrorCodeException(errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_UPDATE_FAILED)
        } finally {
            lock.unlock()
        }
    }

    /**
     * 根据版本名称处理变量组引用
     */
    fun handleVarGroupReferByVersionName(
        publicVarGroupReferDTO: PublicVarGroupReferDTO
    ) {
        val model = publicVarGroupReferDTO.model
        val publicVarGroups = model.publicVarGroups

        logger.info("handleVarGroupReferByVersionName publicVarGroups: $publicVarGroups")

        if (publicVarGroups.isNullOrEmpty()) {
            logger.info("No public var groups found, skip handling")
            return
        }

        val params = model.getTriggerContainer().params

        // 查出params中已存在的变量组名称
        val existingGroupNames = params
            .mapNotNull { it.varGroupName }
            .toSet()

        // 对比publicVarGroups，找出params中不存在的变量组
        val groupsToAdd = publicVarGroups.filter { publicVarGroup ->
            !existingGroupNames.contains(publicVarGroup.groupName)
        }

        if (groupsToAdd.isEmpty()) {
            handleVarGroupReferBus(publicVarGroupReferDTO)
            return
        }

        // 查询这些变量组的变量并添加到params末尾
        groupsToAdd.forEach { publicVarGroup ->
            addVarGroupToParams(
                publicVarGroupReferDTO = publicVarGroupReferDTO,
                publicVarGroup = publicVarGroup,
                params = params
            )
        }

        handleVarGroupReferBus(publicVarGroupReferDTO)
    }

    /**
     * 添加变量组到参数列表
     */
    private fun addVarGroupToParams(
        publicVarGroupReferDTO: PublicVarGroupReferDTO,
        publicVarGroup: PublicVarGroupRef,
        params: MutableList<BuildFormProperty>
    ) {
        val groupName = publicVarGroup.groupName
        val versionName = publicVarGroup.versionName
        val version = publicVarGroup.version

        // 检查引用是否存在源模板项目，获取源头项目ID
        val sourceProjectId = getSourceProjectId(
            projectId = publicVarGroupReferDTO.projectId,
            referId = publicVarGroupReferDTO.referId,
            referType = publicVarGroupReferDTO.referType,
            referHasSource = publicVarGroupReferDTO.referHasSource
        )

        // 使用源头项目ID查询变量组信息
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

        // 使用源头项目ID查询变量组中的变量
        val groupVars = publicVarDao.listVarByGroupName(
            dslContext = dslContext,
            projectId = sourceProjectId,
            groupName = groupName,
            version = varGroupRecord.version
        )

        // 将变量添加到params
        groupVars.forEach { varPO ->
            val buildFormProperty = JsonUtil.to(varPO.buildFormProperty, BuildFormProperty::class.java)
            buildFormProperty.varGroupName = groupName
            buildFormProperty.varGroupVersion = if (varPO.version != -1) version else null
            params.add(buildFormProperty)
        }
    }
}
