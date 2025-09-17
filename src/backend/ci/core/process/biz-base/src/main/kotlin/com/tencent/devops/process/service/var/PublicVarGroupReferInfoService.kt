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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.metrics.api.ServiceMetricsResource
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_UPDATE_FAILED
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_VAR_NAME_DUPLICATE
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupReferInfoDao
import com.tencent.devops.process.dao.`var`.PublicVarReferInfoDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.pojo.`var`.`do`.PublicGroupVarRefDO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupInfoQueryReqDTO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupReferDTO
import com.tencent.devops.process.pojo.`var`.dto.ReferCountUpdateDTO
import com.tencent.devops.process.pojo.`var`.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PipelinePublicVarGroupReferPO
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PublicVarGroupReferInfoService @Autowired constructor(
    private val dslContext: DSLContext,
    private val publicVarGroupDao: PublicVarGroupDao,
    private val client: Client,
    private val templateDao: TemplateDao,
    private val publicVarReferInfoDao: PublicVarReferInfoDao,
    private val publicVarGroupReferInfoDao: PublicVarGroupReferInfoDao,
    private val publicVarReferInfoService: PublicVarReferInfoService,
    private val templatePipelineDao: TemplatePipelineDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarGroupReferInfoService::class.java)
        
        // URL模板常量
        private const val PIPELINE_CONSOLE_URL_TEMPLATE = "/console/pipeline/%s/%s/history/pipeline"
        private const val TEMPLATE_CONSOLE_URL_TEMPLATE = "/console/pipeline/%s/template/%s/%s/pipeline"
        
        // 数据库表名常量
        private const val VAR_GROUP_REFER_TABLE = "T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO"
        
        // 异步处理计数更新的线程池
        private val referCountUpdateExecutor = Executors.newFixedThreadPool(5)
    }

    /**
     * 更新公共变量组引用
     * 
     * 该方法采用事务分离的设计模式：
     * 1. 核心业务逻辑在事务中同步执行，确保数据一致性
     * 2. 引用计数更新异步执行，提升性能
     * 
     * @param userId 用户ID
     * @param projectId 项目ID
     * @param publicVarGroupReferInfo 变量组引用信息
     * @return 操作是否成功
     * @throws ErrorCodeException 当参数验证失败或数据库操作失败时抛出
     */
    fun updatePublicGroupRefer(
        userId: String,
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO
    ): Boolean {
        return try {
            // 预处理和验证
            preprocessAndValidate(publicVarGroupReferInfo)
            
            logger.info(
                "[updatePublicGroupRefer] Processing refer update for project: $projectId, " +
                "referId: ${publicVarGroupReferInfo.referId}, " +
                "groupCount: ${publicVarGroupReferInfo.publicVarGroupRefs.size}"
            )
            
            // 处理空变量组引用的情况
            if (publicVarGroupReferInfo.publicVarGroupRefs.isEmpty()) {
                return handleEmptyVarGroupRefs(userId, projectId, publicVarGroupReferInfo)
            }

            // 验证变量组存在性并构建数据对象
            val (newReferPOs, updateReferPOs) = buildDataObjects(
                userId = userId,
                projectId = projectId,
                publicVarGroupReferInfo = publicVarGroupReferInfo
            )

            // 执行核心事务操作
            val referCountUpdateInfo = executeReferenceUpdateTransaction(
                userId = userId,
                projectId = projectId,
                publicVarGroupReferInfo = publicVarGroupReferInfo,
                newReferPOs = newReferPOs,
                updateReferPOs = updateReferPOs
            )
            
            // 异步更新引用计数
            asyncUpdateReferenceCount(referCountUpdateInfo)
            
            logger.info(
                "[updatePublicGroupRefer] Successfully processed refer update for project: $projectId, " +
                "referId: ${publicVarGroupReferInfo.referId}"
            )
            
            true
        } catch (e: ErrorCodeException) {
            logger.warn(
                "[updatePublicGroupRefer] Failed to update refer for project: $projectId, " +
                "referId: ${publicVarGroupReferInfo.referId}", e
            )
            throw e
        } catch (t: Throwable) {
            logger.error(
                "[updatePublicGroupRefer] Unexpected error for project: $projectId, " +
                "referId: ${publicVarGroupReferInfo.referId}", t
            )
            throw ErrorCodeException(errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_UPDATE_FAILED)
        }
    }
    
    /**
     * 预处理和验证输入参数
     */
    private fun preprocessAndValidate(publicVarGroupReferInfo: PublicVarGroupReferDTO) {
        handlePublicVarInfo(publicVarGroupReferInfo)
        validateParameters(publicVarGroupReferInfo)
    }
    
    /**
     * 执行引用更新的核心事务操作（不包含计数更新）
     * 
     * 该方法确保所有引用信息的更新在同一个事务中完成，保证数据一致性
     * 
     * @return 返回计数更新所需的信息
     */
    private fun executeReferenceUpdateTransaction(
        userId: String,
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO,
        newReferPOs: List<PipelinePublicVarGroupReferPO>,
        updateReferPOs: List<PipelinePublicVarGroupReferPO>
    ): ReferCountUpdateDTO {
        var deletedReferInfos = emptyList<PipelinePublicVarGroupReferPO>()
        
        try {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                val newGroupNames = publicVarGroupReferInfo.publicVarGroupRefs.keys

                // 处理需要删除的旧引用
                if (newGroupNames.isNotEmpty()) {
                    deletedReferInfos = handleObsoleteReferencesWithoutCount(
                        context = context,
                        projectId = projectId,
                        publicVarGroupReferInfo = publicVarGroupReferInfo,
                        newGroupNames = newGroupNames
                    )
                }

                // 批量保存新的引用记录
                if (newReferPOs.isNotEmpty()) {
                    publicVarGroupReferInfoDao.batchSave(context, newReferPOs)
                }

                // 批量更新已存在的引用记录
                if (updateReferPOs.isNotEmpty()) {
                    updateExistingReferenceRecords(
                        context = context,
                        projectId = projectId,
                        publicVarGroupReferInfo = publicVarGroupReferInfo,
                        updateReferPOs = updateReferPOs,
                        userId = userId
                    )
                }
            }
            
            // 处理变量引用信息
            publicVarReferInfoService.addPublicVarRefer(
                userId = userId,
                projectId = projectId,
                publicVarGroupReferInfo = publicVarGroupReferInfo
            )
        } catch (t: Throwable) {
            logger.warn("Failed to add pipeline group refer for ${publicVarGroupReferInfo.referId}", t)
            throw ErrorCodeException(errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_UPDATE_FAILED)
        }
        
        return ReferCountUpdateDTO(
            projectId = projectId,
            newReferInfos = newReferPOs,
            deletedReferInfos = deletedReferInfos
        )
    }
    
    /**
     * 异步更新引用计数
     * 
     * 使用 CompletableFuture 提供更好的异步处理和错误处理能力
     */
    private fun asyncUpdateReferenceCount(referCountUpdateDTO: ReferCountUpdateDTO) {
        CompletableFuture.runAsync({
            try {
                val startTime = System.currentTimeMillis()
                logger.info(
                    "[asyncUpdateReferenceCount] Starting async update for project: ${referCountUpdateDTO.projectId}, " +
                    "newRefs: ${referCountUpdateDTO.newReferInfos.size}, " +
                    "deletedRefs: ${referCountUpdateDTO.deletedReferInfos.size}"
                )
                
                // 并行处理新增和删除的引用计数更新
                val futures = mutableListOf<CompletableFuture<Void>>()
                
                // 更新新增引用的计数
                if (referCountUpdateDTO.newReferInfos.isNotEmpty()) {
                    futures.add(CompletableFuture.runAsync({
                        updateReferenceCountsForNewRefs(
                            context = dslContext,
                            projectId = referCountUpdateDTO.projectId,
                            newReferPOs = referCountUpdateDTO.newReferInfos
                        )
                    }, referCountUpdateExecutor))
                }
                
                // 更新删除引用的计数
                if (referCountUpdateDTO.deletedReferInfos.isNotEmpty()) {
                    futures.add(CompletableFuture.runAsync({
                        updateReferenceCountsForDeletedRefs(
                            context = dslContext,
                            projectId = referCountUpdateDTO.projectId,
                            deletedReferInfos = referCountUpdateDTO.deletedReferInfos
                        )
                    }, referCountUpdateExecutor))
                }
                
                // 等待所有任务完成
                CompletableFuture.allOf(*futures.toTypedArray()).get()
                
                val duration = System.currentTimeMillis() - startTime
                logger.info(
                    "[asyncUpdateReferenceCount] Completed async update for project: ${referCountUpdateDTO.projectId}, " +
                    "duration: ${duration}ms"
                )
            } catch (t: Throwable) {
                logger.error(
                    "[asyncUpdateReferenceCount] Failed to update reference count for project: ${referCountUpdateDTO.projectId}", 
                    t
                )
            }
        }, referCountUpdateExecutor)
    }

    /**
     * 处理公共变量组信息
     * 
     * 从构建参数中提取变量组引用信息和位置信息
     */
    private fun handlePublicVarInfo(publicVarGroupReferInfo: PublicVarGroupReferDTO) {
        val params = publicVarGroupReferInfo.params
        if (params.isNullOrEmpty()) {
            return
        }
        
        // 从params获取varGroupName不为空的参数
        val varGroupParams = params.filter { !it.varGroupName.isNullOrBlank() }

        if (varGroupParams.isNotEmpty()) {
            // 构建变量组引用映射：变量组名 -> 版本号
            publicVarGroupReferInfo.publicVarGroupRefs = varGroupParams
                .associate { it.varGroupName!! to it.varGroupVersion }
            
            // 构建位置信息映射：变量ID -> 位置索引
            publicVarGroupReferInfo.positionInfo = buildPositionInfo(params)
        }
    }

    /**
     * 构建变量组位置信息
     * 
     * @param params 构建参数列表
     * @return 位置信息映射，key为变量ID，value为在参数列表中的位置索引
     */
    private fun buildPositionInfo(params: List<BuildFormProperty>): Map<String, Int> {
        return params.mapIndexedNotNull { index, property ->
            if (property.varGroupName != null) {
                property.id to index
            } else {
                null
            }
        }.toMap()
    }

    /**
     * 验证输入参数
     * 
     * @param publicVarGroupReferInfo 变量组引用信息
     * @throws ErrorCodeException 当存在重复的变量组名称时抛出
     */
    private fun validateParameters(publicVarGroupReferInfo: PublicVarGroupReferDTO) {
        // 检查变量组引用中是否存在重复的groupName
        val groupNames = publicVarGroupReferInfo.publicVarGroupRefs.keys
        if (groupNames.size != groupNames.toSet().size) {
            val duplicateGroupNames = groupNames
                .groupBy { it }
                .filter { it.value.size > 1 }
                .keys
            
            logger.warn(
                "[validateParameters] Duplicate group names found: ${duplicateGroupNames.joinToString(", ")}"
            )
            
            throw ErrorCodeException(
                errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_VAR_NAME_DUPLICATE,
                params = arrayOf(duplicateGroupNames.joinToString(", "))
            )
        }
    }

    /**
     * 处理空变量组引用的情况
     */
    private fun handleEmptyVarGroupRefs(
        userId: String,
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO
    ): Boolean {
        val countByReferId = publicVarGroupReferInfoDao.countByPublicVarGroupRef(
            dslContext = dslContext,
            projectId = projectId,
            referId = publicVarGroupReferInfo.referId,
            referType = publicVarGroupReferInfo.referType,
            referVersionName = publicVarGroupReferInfo.referVersionName
        )
        
        if (countByReferId > 0) {
            // 先获取要删除的引用记录，用于异步更新计数
            val referInfosToDelete = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferId(
                dslContext = dslContext,
                projectId = projectId,
                referId = publicVarGroupReferInfo.referId,
                referType = publicVarGroupReferInfo.referType,
                referVersionName = publicVarGroupReferInfo.referVersionName
            )
            
            // 执行删除操作（不包含计数更新）
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                deleteAllReferencesWithoutCount(context, projectId, publicVarGroupReferInfo)
            }
            
            // 异步更新引用计数
            val referCountUpdateDTO = ReferCountUpdateDTO(
                projectId = projectId,
                deletedReferInfos = referInfosToDelete
            )
            asyncUpdateReferenceCount(referCountUpdateDTO)
        }
        
        return true
    }

    /**
     * 验证变量组存在性并构建数据对象
     */
    private fun buildDataObjects(
        userId: String,
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO
    ): Pair<List<PipelinePublicVarGroupReferPO>, List<PipelinePublicVarGroupReferPO>> {
        val newReferPOs = mutableListOf<PipelinePublicVarGroupReferPO>()
        val updateReferPOs = mutableListOf<PipelinePublicVarGroupReferPO>()
        
        publicVarGroupReferInfo.publicVarGroupRefs.forEach { ref ->
            val groupName = ref.key
            val version = ref.value
            
            // 验证变量组是否存在
            validateVarGroupExists(projectId, groupName, version)
            
            // 检查是否已存在引用
            if (isVarGroupRefExists(projectId, publicVarGroupReferInfo, groupName)) {
                // 已存在，添加到更新列表
                updateReferPOs.add(
                    createVarGroupReferPO(
                        userId = userId,
                        projectId = projectId,
                        publicVarGroupReferInfo = publicVarGroupReferInfo,
                        groupName = groupName,
                        version = version
                    )
                )
            } else {
                // 不存在，添加到新增列表
                newReferPOs.add(
                    createVarGroupReferPO(
                        userId = userId,
                        projectId = projectId,
                        publicVarGroupReferInfo = publicVarGroupReferInfo,
                        groupName = groupName,
                        version = version
                    )
                )
            }
        }
        
        return Pair(newReferPOs, updateReferPOs)
    }

    /**
     * 验证变量组是否存在
     */
    private fun validateVarGroupExists(projectId: String, groupName: String, version: Int?) {
        val groupCount = publicVarGroupDao.countRecordByGroupName(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            version = version
        )
        if (groupCount == 0) {
            throw ErrorCodeException(
                errorCode = ERROR_INVALID_PARAM_,
                params = arrayOf(groupName)
            )
        }
    }

    /**
     * 检查变量组引用是否已存在
     */
    private fun isVarGroupRefExists(
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO,
        groupName: String
    ): Boolean {
        return publicVarGroupReferInfoDao.countByPublicVarGroupRef(
            dslContext = dslContext,
            projectId = projectId,
            referId = publicVarGroupReferInfo.referId,
            referType = publicVarGroupReferInfo.referType,
            groupName = groupName,
            referVersionName = publicVarGroupReferInfo.referVersionName
        ) > 0
    }

    /**
     * 创建变量组引用PO对象
     * 
     * @param userId 用户ID
     * @param projectId 项目ID
     * @param publicVarGroupReferInfo 变量组引用信息
     * @param groupName 变量组名称
     * @param version 变量组版本
     * @return 变量组引用PO对象
     */
    private fun createVarGroupReferPO(
        userId: String,
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO,
        groupName: String,
        version: Int?
    ): PipelinePublicVarGroupReferPO {
        val currentTime = LocalDateTime.now()
        val positionInfoJson = publicVarGroupReferInfo.positionInfo?.let { JsonUtil.toJson(it) }
        
        // 生成唯一ID
        val id = client.get(ServiceAllocIdResource::class)
            .generateSegmentId(VAR_GROUP_REFER_TABLE).data ?: 0L
        
        return PipelinePublicVarGroupReferPO(
            id = id,
            projectId = projectId,
            groupName = groupName,
            version = version,
            referId = publicVarGroupReferInfo.referId,
            referName = publicVarGroupReferInfo.referName,
            referVersionName = publicVarGroupReferInfo.referVersionName,
            referType = publicVarGroupReferInfo.referType,
            positionInfo = positionInfoJson,
            modifier = userId,
            updateTime = currentTime,
            creator = userId,
            createTime = currentTime
        )
    }
    

    /**
     * 处理需要删除的旧引用（不更新计数，返回删除的引用信息）
     */
    private fun handleObsoleteReferencesWithoutCount(
        context: DSLContext,
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO,
        newGroupNames: Set<String>
    ): List<PipelinePublicVarGroupReferPO> {
        // 获取现有的引用记录
        val existingReferInfos = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferId(
            dslContext = context,
            projectId = projectId,
            referId = publicVarGroupReferInfo.referId,
            referType = publicVarGroupReferInfo.referType,
            referVersionName = publicVarGroupReferInfo.referVersionName
        )
        
        // 找出需要删除的引用记录
        val toDeleteReferInfos = existingReferInfos.filter { it.groupName !in newGroupNames }
        
        if (toDeleteReferInfos.isNotEmpty()) {
            // 删除变量组引用记录
            deleteVarGroupReferences(
                context = context,
                projectId = projectId,
                publicVarGroupReferInfo = publicVarGroupReferInfo,
                excludedGroupNames = newGroupNames.toList()
            )
        }
        
        return toDeleteReferInfos
    }
    
    /**
     * 删除变量组引用记录的通用方法
     */
    private fun deleteVarGroupReferences(
        context: DSLContext,
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO,
        excludedGroupNames: List<String>? = null
    ) {
        if (excludedGroupNames != null) {
            publicVarGroupReferInfoDao.deleteByReferIdExcludingGroupNames(
                dslContext = context,
                projectId = projectId,
                referId = publicVarGroupReferInfo.referId,
                referType = publicVarGroupReferInfo.referType,
                referVersionName = publicVarGroupReferInfo.referVersionName,
                excludedGroupNames = excludedGroupNames
            )
        } else {
            publicVarGroupReferInfoDao.deleteByReferIdExcludingGroupNames(
                dslContext = context,
                projectId = projectId,
                referId = publicVarGroupReferInfo.referId,
                referType = publicVarGroupReferInfo.referType,
                referVersionName = publicVarGroupReferInfo.referVersionName
            )
        }
    }



    /**
     * 删除指定referId的所有引用记录（不更新计数）
     */
    private fun deleteAllReferencesWithoutCount(
        context: DSLContext,
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO
    ) {
        // 删除变量组引用记录
        deleteVarGroupReferences(
            context = context,
            projectId = projectId,
            publicVarGroupReferInfo = publicVarGroupReferInfo
        )
        
        // 删除变量引用记录
        publicVarReferInfoDao.deleteByReferIdExcludingGroupNames(
            dslContext = context,
            projectId = projectId,
            referId = publicVarGroupReferInfo.referId,
            referType = publicVarGroupReferInfo.referType,
            referVersionName = publicVarGroupReferInfo.referVersionName
        )
    }

    /**
     * 更新引用计数的通用方法
     * 
     * 该方法采用批量处理的方式，提高数据库操作效率
     * 
     * @param context 数据库上下文
     * @param projectId 项目ID
     * @param referInfos 引用信息列表
     * @param isIncrement true表示增加计数，false表示减少计数
     */
    private fun updateReferenceCounts(
        context: DSLContext,
        projectId: String,
        referInfos: List<PipelinePublicVarGroupReferPO>,
        isIncrement: Boolean
    ) {
        if (referInfos.isEmpty()) {
            logger.info("[updateReferenceCounts] No reference infos to update")
            return
        }
        
        logger.info(
            "[updateReferenceCounts] Updating ${referInfos.size} reference counts, " +
            "isIncrement: $isIncrement, projectId: $projectId"
        )
        
        // 按组名和版本分组统计需要更新的引用数量
        val referCountUpdates = referInfos
            .groupBy { Pair(it.groupName, it.version) }
            .mapValues { it.value.size }
        
        logger.info(
            "[updateReferenceCounts] Grouped updates: ${referCountUpdates.size} groups to update"
        )
        
        // 批量更新引用计数
        referCountUpdates.forEach { (groupInfo, countChange) ->
            val (groupName, version) = groupInfo
            val actualCountChange = if (isIncrement) countChange else -countChange
            
            try {
                updateSingleGroupReferCount(
                    context = context,
                    projectId = projectId,
                    groupName = groupName,
                    version = version,
                    countChange = actualCountChange
                )
            } catch (t: Throwable) {
                logger.error(
                    "[updateReferenceCounts] Failed to update count for group: $groupName, " +
                    "version: $version, countChange: $actualCountChange", t
                )
                // 继续处理其他组，不因单个失败而中断整个流程
            }
        }
    }
    
    /**
     * 更新单个变量组的引用计数
     * 
     * @param context 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名称
     * @param version 变量组版本
     * @param countChange 计数变化量（正数表示增加，负数表示减少）
     */
    private fun updateSingleGroupReferCount(
        context: DSLContext,
        projectId: String,
        groupName: String,
        version: Int?,
        countChange: Int
    ) {
        if (countChange == 0) {
            logger.info("[updateSingleGroupReferCount] Count change is 0, skipping update")
            return
        }
        
        val currentGroupRecord = publicVarGroupDao.getRecordByGroupName(
            dslContext = context,
            projectId = projectId,
            groupName = groupName,
            version = version
        )
        
        if (currentGroupRecord == null) {
            logger.warn(
                "[updateSingleGroupReferCount] Group record not found: " +
                "projectId=$projectId, groupName=$groupName, version=$version"
            )
            return
        }
        
        val oldReferCount = currentGroupRecord.referCount
        val newReferCount = maxOf(0, oldReferCount + countChange)
        
        logger.info(
            "[updateSingleGroupReferCount] Updating refer count: " +
            "groupName=$groupName, version=$version, " +
            "oldCount=$oldReferCount, countChange=$countChange, newCount=$newReferCount"
        )

        try {
            // 更新 T_PIPELINE_PUBLIC_VAR_GROUP 表的引用计数
            publicVarGroupDao.updateReferCount(
                dslContext = context,
                projectId = projectId,
                groupName = groupName,
                version = currentGroupRecord.version,
                referCount = newReferCount
            )

            // 更新变量组名称的引用计数
            publicVarGroupDao.updateVarGroupNameReferCount(
                dslContext = context,
                projectId = projectId,
                groupName = groupName,
                referCount = newReferCount
            )
            
            logger.info(
                "[updateSingleGroupReferCount] Successfully updated refer count for " +
                "groupName=$groupName, version=$version"
            )
        } catch (t: Throwable) {
            logger.error(
                "[updateSingleGroupReferCount] Failed to update refer count for " +
                "groupName=$groupName, version=$version", t
            )
            throw t
        }
    }

    /**
     * 更新被删除引用的计数
     */
    private fun updateReferenceCountsForDeletedRefs(
        context: DSLContext,
        projectId: String,
        deletedReferInfos: List<PipelinePublicVarGroupReferPO>
    ) {
        updateReferenceCounts(context, projectId, deletedReferInfos, false)
    }

    /**
     * 更新新增引用的计数
     */
    private fun updateReferenceCountsForNewRefs(
        context: DSLContext,
        projectId: String,
        newReferPOs: List<PipelinePublicVarGroupReferPO>
    ) {
        updateReferenceCounts(context, projectId, newReferPOs, true)
    }

    /**
     * 更新已存在的引用记录
     */
    private fun updateExistingReferenceRecords(
        context: DSLContext,
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO,
        updateReferPOs: List<PipelinePublicVarGroupReferPO>,
        userId: String
    ) {
        updateReferPOs.forEach { referPO ->
            val updateCount = publicVarGroupReferInfoDao.updateVarGroupReferInfo(
                dslContext = context,
                projectId = projectId,
                referId = publicVarGroupReferInfo.referId,
                referType = publicVarGroupReferInfo.referType,
                groupName = referPO.groupName,
                referVersionName = publicVarGroupReferInfo.referVersionName,
                version = referPO.version,
                positionInfo = referPO.positionInfo,
                modifier = userId,
                updateTime = LocalDateTime.now()
            )

            if (updateCount > 0) {
                logger.info(
                    "Successfully updated var group reference: projectId=$projectId, " +
                    "referId=${publicVarGroupReferInfo.referId}, groupName=${referPO.groupName}, " +
                    "version=${referPO.version}"
                )
            } else {
                logger.warn(
                    "Failed to update var group reference: projectId=$projectId, " +
                    "referId=${publicVarGroupReferInfo.referId}, groupName=${referPO.groupName}, " +
                    "version=${referPO.version}"
                )
            }
        }
    }

    /**
     * 根据引用ID删除变量组引用
     * @param projectId 项目ID
     * @param referId 引用ID
     * @param referType 引用类型
     */
    fun deletePublicVerGroupRefByReferId(
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum
    ) {
        
        try {
            // 先获取要删除的引用记录，用于异步更新计数
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
            
            // 执行删除操作（不包含计数更新）
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                
                // 删除变量组引用记录
                publicVarGroupReferInfoDao.deleteByReferIds(
                    dslContext = context,
                    projectId = projectId,
                    referIds = listOf(referId),
                    referType = referType
                )
                
                // 删除变量引用记录
                publicVarReferInfoDao.deleteByReferId(
                    dslContext = context,
                    projectId = projectId,
                    referId = referId,
                    referType = referType
                )
            }
            
            // 异步更新引用计数
            val referCountUpdateDTO = ReferCountUpdateDTO(
                projectId = projectId,
                deletedReferInfos = referInfosToDelete
            )
            asyncUpdateReferenceCount(referCountUpdateDTO)
            
            logger.info("Successfully deleted ${referInfosToDelete.size} references for referId: $referId")
        } catch (t: Throwable) {
            logger.warn("Failed to delete refer info for referId: $referId", t)
            throw ErrorCodeException(errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_UPDATE_FAILED)
        }
    }

    /**
     * 批量删除referId列表对应的变量组引用记录
     * @param projectId 项目ID
     * @param referIds referId列表
     * @param referType 引用类型
     * @return 操作是否成功
     */
    fun deletePublicVerGroupRefByReferIds(
        projectId: String,
        referIds: List<String>,
        referType: PublicVerGroupReferenceTypeEnum
    ): Boolean {
        
        if (referIds.isEmpty()) {
            return false
        }

        // 先获取所有要删除的引用记录，用于异步更新计数
        val allReferInfosToDelete = collectReferInfosToDelete(
            context = dslContext,
            projectId = projectId,
            referIds = referIds,
            referType = referType
        )

        // 执行删除操作（不包含计数更新）
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)

            // 批量删除变量组引用记录
            publicVarGroupReferInfoDao.deleteByReferIds(
                dslContext = context,
                projectId = projectId,
                referIds = referIds,
                referType = referType
            )

            // 批量删除对应的变量引用记录
            publicVarReferInfoDao.deleteByReferIdsWithoutVersion(
                dslContext = context,
                projectId = projectId,
                referIds = referIds,
                referType = referType
            )
        }
        
        // 异步更新引用计数
        if (allReferInfosToDelete.isNotEmpty()) {
            val referCountUpdateDTO = ReferCountUpdateDTO(
                projectId = projectId,
                deletedReferInfos = allReferInfosToDelete
            )
            asyncUpdateReferenceCount(referCountUpdateDTO)
        }
        
        return true
    }
    
    /**
     * 收集要删除的引用信息
     */
    private fun collectReferInfosToDelete(
        context: DSLContext,
        projectId: String,
        referIds: List<String>,
        referType: PublicVerGroupReferenceTypeEnum
    ): List<PipelinePublicVarGroupReferPO> {
        val allReferInfosToDelete = mutableListOf<PipelinePublicVarGroupReferPO>()
        
        referIds.forEach { referId ->
            val referInfos = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferId(
                dslContext = context,
                projectId = projectId,
                referId = referId,
                referType = referType
            )
            allReferInfosToDelete.addAll(referInfos)
        }
        
        return allReferInfosToDelete
    }

    /**
     * 查询变量引用信息
     * @param queryReq 查询请求
     * @return 分页结果
     */
    fun listVarReferInfo(queryReq: PublicVarGroupInfoQueryReqDTO): Page<PublicGroupVarRefDO> {
        val (totalCount, varGroupReferInfo) = queryVarGroupReferInfo(queryReq)
        if (totalCount == 0) {
            return Page(
                count = 0,
                page = queryReq.page,
                pageSize = queryReq.pageSize,
                records = emptyList()
            )
        }

        val referInfoByType = varGroupReferInfo.groupBy { it.referType }
        val records = mutableListOf<PublicGroupVarRefDO>()
        
        // 处理流水线类型的引用
        referInfoByType[PublicVerGroupReferenceTypeEnum.PIPELINE]?.let { pipelineReferInfos ->
            if (pipelineReferInfos.isNotEmpty()) {
                val pipelineIds = pipelineReferInfos.map { it.referId }
                
                // 批量查询流水线执行次数
                val pipelineExecCounts = client.get(ServiceMetricsResource::class)
                    .queryPipelineMonthlyExecCountByList(queryReq.projectId, pipelineIds)
                    .data ?: emptyMap()
                
                // 构建流水线引用记录
                pipelineReferInfos.forEach { referInfo ->
                    records.add(
                        PublicGroupVarRefDO(
                            referId = referInfo.referId,
                            referName = referInfo.referName,
                            referUrl = getVarGroupReferUrl(
                                projectId = queryReq.projectId,
                                referType = referInfo.referType,
                                referId = referInfo.referId,
                                version = null
                            ),
                            referType = referInfo.referType,
                            creator = referInfo.creator,
                            modifier = referInfo.modifier,
                            updateTime = referInfo.updateTime,
                            executeCount = pipelineExecCounts[referInfo.referId] ?: 0
                        )
                    )
                }
            }
        }
        
        // 处理模板类型的引用
        referInfoByType[PublicVerGroupReferenceTypeEnum.TEMPLATE]?.let { templateReferInfos ->
            if (templateReferInfos.isNotEmpty()) {
                // 构建模板引用记录
                templateReferInfos.forEach { referInfo ->
                    val template = templateDao.getTemplate(
                        dslContext = dslContext,
                        templateId = referInfo.referId,
                        version = referInfo.referVersionName?.toLong()
                    ) ?: return@forEach
                    
                    records.add(
                        PublicGroupVarRefDO(
                            referId = referInfo.referId,
                            referName = referInfo.referName,
                            referUrl = getVarGroupReferUrl(
                                projectId = queryReq.projectId,
                                referType = referInfo.referType,
                                referId = referInfo.referId,
                                version = template.version
                            ),
                            referType = referInfo.referType,
                            creator = template.creator,
                            modifier = template.creator,
                            updateTime = template.updateTime ?: LocalDateTime.now(),
                            instanceCount = countTemplateVersionInstances(
                                projectId = queryReq.projectId,
                                templateId = referInfo.referId,
                                version = template.version
                            )
                        )
                    )
                }
            }
        }

        return Page(
            count = totalCount.toLong(),
            page = queryReq.page,
            pageSize = queryReq.pageSize,
            records = records
        )
    }

    private fun queryVarGroupReferInfo(
        queryReq: PublicVarGroupInfoQueryReqDTO
    ): Pair<Int, List<PipelinePublicVarGroupReferPO>> {
        val projectId = queryReq.projectId
        val groupName = queryReq.groupName!!
        val version = queryReq.version

        // 检查变量组是否存在
        val pipelinePublicVarGroupCount = publicVarGroupDao.countRecordByGroupName(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            version = version
        )
        if (pipelinePublicVarGroupCount == 0) {
            return Pair(0, emptyList())
        }

        val totalCount = publicVarGroupReferInfoDao.countByGroupName(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            referType = queryReq.referType,
            version = version
        )
        val varGroupReferInfo = publicVarGroupReferInfoDao.listVarGroupReferInfo(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            referType = queryReq.referType,
            version = version,
            page = queryReq.page,
            pageSize = queryReq.pageSize
        )
        return Pair(totalCount, varGroupReferInfo)
    }

    /**
     * 统计模板版本实例数量
     * @param projectId 项目ID
     * @param templateId 模板ID
     * @param version 版本号
     * @return 实例数量
     */
    fun countTemplateVersionInstances(projectId: String, templateId: String, version: Long): Int {
        logger.info("[$projectId|$templateId|$version] Count template version instances")

        return templatePipelineDao.countByTemplateByVersion(
            dslContext = dslContext,
            projectId = projectId,
            instanceType = PipelineInstanceTypeEnum.CONSTRAINT.type,
            templateId = templateId,
            version = version
        )
    }

    /**
     * 获取变量组引用的URL
     * 
     * @param projectId 项目ID
     * @param referType 引用类型
     * @param referId 引用ID
     * @param version 版本号（仅模板类型需要）
     * @return 引用URL
     */
    private fun getVarGroupReferUrl(
        projectId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referId: String,
        version: Long? = null
    ): String {
        return when (referType) {
            PublicVerGroupReferenceTypeEnum.PIPELINE -> {
                String.format(PIPELINE_CONSOLE_URL_TEMPLATE, projectId, referId)
            }
            PublicVerGroupReferenceTypeEnum.TEMPLATE -> {
                requireNotNull(version) { "Template version is required for template URL" }
                String.format(TEMPLATE_CONSOLE_URL_TEMPLATE, projectId, referId, version)
            }
        }
    }
    
    /**
     * 关闭线程池资源
     * 
     * 该方法应在应用关闭时调用，确保线程池资源得到正确释放
     */
    fun shutdown() {
        try {
            logger.info("[shutdown] Shutting down reference count update executor")
            referCountUpdateExecutor.shutdown()
            
            if (!referCountUpdateExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("[shutdown] Executor did not terminate gracefully, forcing shutdown")
                referCountUpdateExecutor.shutdownNow()
            }
            
            logger.info("[shutdown] Reference count update executor shutdown completed")
        } catch (t: Throwable) {
            logger.error("[shutdown] Error during executor shutdown", t)
        }
    }
}