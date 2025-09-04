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

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.constant.CommonMessageCode.ERROR_INVALID_PARAM_
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.metrics.api.ServiceMetricsResource
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupReferInfoDao
import com.tencent.devops.process.dao.`var`.PublicVarReferInfoDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.pojo.`var`.`do`.PublicGroupVarRefDO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupInfoQueryReqDTO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupReferDTO
import com.tencent.devops.process.pojo.`var`.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PipelinePublicVarGroupReferPO
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import java.time.LocalDateTime
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
    private val templatePipelineDao: TemplatePipelineDao,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarGroupReferInfoService::class.java)
        private const val PIPELINE_CONSOLE_URL_TEMPLATE = "/console/pipeline/%s/%s/history/pipeline"
        private const val TEMPLATE_CONSOLE_URL_TEMPLATE = "/console/pipeline/%s/template/%s/%s/pipeline"
    }

    /**
     * 更新公共变量组引用
     * @param userId 用户ID
     * @param projectId 项目ID
     * @param publicVarGroupReferInfo 变量组引用信息
     * @return 操作是否成功
     */
    fun updatePublicGroupRefer(
        userId: String,
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO
    ): Boolean {
        validateParameters(publicVarGroupReferInfo)
        
        // 处理空变量组引用的情况
        if (publicVarGroupReferInfo.publicVarGroupRefs.isEmpty()) {
            return handleEmptyVarGroupRefs(projectId, publicVarGroupReferInfo)
        }

        // 验证变量组存在性并构建数据对象
        val publicVarGroupReferPOs = buildDataObjects(
            userId = userId,
            projectId = projectId,
            publicVarGroupReferInfo = publicVarGroupReferInfo
        )

        // 执行数据库事务操作
        return executeTransaction(
            userId = userId,
            projectId = projectId,
            publicVarGroupReferInfo = publicVarGroupReferInfo,
            publicVarGroupReferPOs = publicVarGroupReferPOs
        )
    }
    
    /**
     * 验证输入参数
     */
    private fun validateParameters(
        publicVarGroupReferInfo: PublicVarGroupReferDTO
    ) {
        // 检查变量组引用中是否存在重复的groupName
        val groupNames = publicVarGroupReferInfo.publicVarGroupRefs.map { it.groupName }
        val duplicateGroupNames = groupNames.groupBy { it }.filter { it.value.size > 1 }.keys
        
        if (duplicateGroupNames.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ERROR_INVALID_PARAM_,
                params = arrayOf("变量组引用中存在重复的组名: ${duplicateGroupNames.joinToString(", ")}")
            )
        }
    }

    /**
     * 处理空变量组引用的情况
     */
    private fun handleEmptyVarGroupRefs(
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
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                deleteAllReferencesByReferId(context, projectId, publicVarGroupReferInfo)
            }
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
    ): List<PipelinePublicVarGroupReferPO> {
        val publicVarGroupReferPOs = mutableListOf<PipelinePublicVarGroupReferPO>()
        
        publicVarGroupReferInfo.publicVarGroupRefs.forEach { ref ->
            val groupName = ref.groupName
            val version = ref.versionName?.substring(1)?.toIntOrNull()
            
            // 验证变量组是否存在
            validateVarGroupExists(projectId, groupName, version)
            
            // 检查是否已存在引用
            if (isVarGroupRefExists(projectId, publicVarGroupReferInfo, groupName)) {
                return@forEach
            }
            
            // 构建引用对象
            publicVarGroupReferPOs.add(
                createVarGroupReferPO(
                    userId = userId,
                    projectId = projectId,
                    publicVarGroupReferInfo = publicVarGroupReferInfo,
                    groupName = groupName,
                    version = version
                )
            )
        }
        
        return publicVarGroupReferPOs
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
     */
    private fun createVarGroupReferPO(
        userId: String,
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO,
        groupName: String,
        version: Int?
    ): PipelinePublicVarGroupReferPO {

        val positionInfoJson = publicVarGroupReferInfo.positionInfo?.let { JsonUtil.toJson(it) }
        
        return PipelinePublicVarGroupReferPO(
            id = client.get(ServiceAllocIdResource::class)
                .generateSegmentId("T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO").data ?: 0,
            projectId = projectId,
            groupName = groupName,
            version = version,
            referId = publicVarGroupReferInfo.referId,
            referName = publicVarGroupReferInfo.referName,
            referVersionName = publicVarGroupReferInfo.referVersionName,
            referType = publicVarGroupReferInfo.referType,
            positionInfo = positionInfoJson,
            modifier = userId,
            updateTime = LocalDateTime.now(),
            creator = userId,
            createTime = LocalDateTime.now()
        )
    }



    /**
     * 执行数据库事务操作
     */
    private fun executeTransaction(
        userId: String,
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO,
        publicVarGroupReferPOs: List<PipelinePublicVarGroupReferPO>
    ): Boolean {
        try {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                val newGroupNames = publicVarGroupReferInfo.publicVarGroupRefs.map { it.groupName }.toSet()

                // 处理需要删除的旧引用
                if (newGroupNames.isNotEmpty()) {
                    handleObsoleteReferences(
                        context = context,
                        projectId = projectId,
                        publicVarGroupReferInfo = publicVarGroupReferInfo,
                        newGroupNames = newGroupNames
                    )
                }

                // 批量保存新的引用记录
                if (publicVarGroupReferPOs.isNotEmpty()) {
                    publicVarGroupReferInfoDao.batchSave(context, publicVarGroupReferPOs)
                }
                
                // 更新新增引用的计数
                updateReferenceCountsForNewRefs(
                    context = context,
                    projectId = projectId,
                    newReferPOs = publicVarGroupReferPOs
                )
            }
            
            // 处理变量引用信息
            publicVarReferInfoService.addPublicVarRefer(
                userId = userId,
                projectId = projectId,
                publicVarGroupReferInfo = publicVarGroupReferInfo
            )
        } catch (t: Throwable) {
            logger.error("Failed to add pipeline group refer for ${publicVarGroupReferInfo.referId}", t)
            throw RuntimeException("更新变量组引用失败: ${t.message}", t)
        }
        return true
    }

    /**
     * 处理需要删除的旧引用
     */
    private fun handleObsoleteReferences(
        context: DSLContext,
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO,
        newGroupNames: Set<String>
    ) {
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
            
            // 更新引用计数
            updateReferenceCountsForDeletedRefs(
                context = context,
                projectId = projectId,
                deletedReferInfos = toDeleteReferInfos
            )
        }
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
     * 删除指定referId的所有引用记录
     */
    private fun deleteAllReferencesByReferId(
        context: DSLContext,
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO
    ) {
        // 获取要删除的引用记录
        val existingReferInfos = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferId(
            dslContext = context,
            projectId = projectId,
            referId = publicVarGroupReferInfo.referId,
            referType = publicVarGroupReferInfo.referType,
            referVersionName = publicVarGroupReferInfo.referVersionName
        )
        
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
        

        
        // 更新引用计数
        updateReferenceCountsForDeletedRefs(
            context = context,
            projectId = projectId,
            deletedReferInfos = existingReferInfos
        )
    }

    /**
     * 更新引用计数的通用方法
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
        if (referInfos.isEmpty()) return
        
        // 按组名和版本分组统计需要更新的引用数量
        val referCountUpdates = referInfos
            .groupBy { Pair(it.groupName, it.version) }
            .mapValues { it.value.size }
        
        // 批量更新引用计数
        referCountUpdates.forEach { (groupInfo, countChange) ->
            val (groupName, version) = groupInfo
            updateSingleGroupReferCount(
                context = context,
                projectId = projectId,
                groupName = groupName,
                version = version,
                countChange = if (isIncrement) countChange else -countChange
            )
        }
    }
    
    /**
     * 更新单个变量组的引用计数
     */
    private fun updateSingleGroupReferCount(
        context: DSLContext,
        projectId: String,
        groupName: String,
        version: Int?,
        countChange: Int
    ) {
        val currentGroupRecord = publicVarGroupDao.getRecordByGroupName(
            dslContext = context,
            projectId = projectId,
            groupName = groupName,
            version = version
        )
        
        currentGroupRecord?.let { record ->
            val newReferCount = if (countChange > 0) {
                record.referCount + countChange
            } else {
                maxOf(0, record.referCount + countChange) // countChange为负数时
            }
            
            publicVarGroupDao.updateReferCount(
                dslContext = context,
                projectId = projectId,
                groupName = groupName,
                version = record.version,
                referCount = newReferCount
            )
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
        require(projectId.isNotBlank()) { "项目ID不能为空" }
        require(referId.isNotBlank()) { "引用ID不能为空" }
        
        try {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                
                // 先获取要删除的引用记录，用于减少referCount
                val referInfosToDelete = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferId(
                    dslContext = context,
                    projectId = projectId,
                    referId = referId,
                    referType = referType
                )
                
                if (referInfosToDelete.isEmpty()) {
                    logger.info("No reference found for referId: $referId, skip deletion")
                    return@transaction
                }
                
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
                
                // 为每个被删除的变量组引用减少referCount计数
                updateReferenceCountsForDeletedRefs(
                    context = context,
                    projectId = projectId,
                    deletedReferInfos = referInfosToDelete
                )
                
                logger.info("Successfully deleted ${referInfosToDelete.size} references for referId: $referId")
            }
        } catch (t: Throwable) {
            logger.error("Failed to delete refer info for referId: $referId", t)
            throw RuntimeException("删除变量组引用失败: ${t.message}", t)
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
        require(projectId.isNotBlank()) { "项目ID不能为空" }
        
        if (referIds.isEmpty()) {
            logger.info("ReferIds list is empty, skip deletion")
            return false
        }
        
        val validReferIds = referIds.filter { it.isNotBlank() }
        if (validReferIds.isEmpty()) {
            logger.warn("All referIds are blank, skip deletion")
            return false
        }
        
        try {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                
                // 先获取所有要删除的引用记录，用于减少referCount
                val allReferInfosToDelete = collectReferInfosToDelete(
                    context = context,
                    projectId = projectId,
                    referIds = validReferIds,
                    referType = referType
                )
                
                if (allReferInfosToDelete.isEmpty()) {
                    logger.info("No references found for referIds: $validReferIds, skip deletion")
                    return@transaction
                }
                
                // 批量删除变量组引用记录
                publicVarGroupReferInfoDao.deleteByReferIds(
                    dslContext = context,
                    projectId = projectId,
                    referIds = validReferIds,
                    referType = referType
                )
                
                // 批量删除对应的变量引用记录
                publicVarReferInfoDao.deleteByReferIdsWithoutVersion(
                    dslContext = context,
                    projectId = projectId,
                    referIds = validReferIds,
                    referType = referType
                )
                
                // 为每个被删除的变量组引用减少referCount计数
                updateReferenceCountsForDeletedRefs(
                    context = context,
                    projectId = projectId,
                    deletedReferInfos = allReferInfosToDelete
                )
                
                logger.info("Successfully deleted ${allReferInfosToDelete.size} references for ${validReferIds.size} referIds")
            }
        } catch (t: Throwable) {
            logger.error("Failed to delete refer info for referIds: $validReferIds", t)
            throw RuntimeException("批量删除变量组引用失败: ${t.message}", t)
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
        return getReferInfoByType(queryReq)
    }

    private fun getReferInfoByType(queryReq: PublicVarGroupInfoQueryReqDTO): Page<PublicGroupVarRefDO> {
        val (totalCount, varGroupReferInfo) = queryVarGroupReferInfo(queryReq)
        if (totalCount == 0) {
            return Page(
                count = 0,
                page = queryReq.page,
                pageSize = queryReq.pageSize,
                records = emptyList()
            )
        }

        val records = varGroupReferInfo.map {
            when (queryReq.referType) {
                PublicVerGroupReferenceTypeEnum.PIPELINE -> {
                    PublicGroupVarRefDO(
                        referId = it.referId,
                        referName = it.referName,
                        referUrl = getVarGroupReferUrl(
                            projectId = queryReq.projectId,
                            referType = it.referType,
                            referId = it.referId,
                            version = it.referVersionName?.toLong()
                        ),
                        referType = it.referType,
                        creator = it.creator,
                        modifier = it.modifier,
                        updateTime = it.updateTime,
                        executeCount = client.get(ServiceMetricsResource::class).queryPipelineMonthlyExecCount(
                            projectId = queryReq.projectId,
                            pipelineId = it.referId
                        ).data ?: 0
                    )
                }
                PublicVerGroupReferenceTypeEnum.TEMPLATE -> {
                    val template = templateDao.getTemplate(
                        dslContext = dslContext,
                        templateId = it.referId,
                        version = it.referVersionName?.toLong()
                    )!!
                    PublicGroupVarRefDO(
                        referId = it.referId,
                        referName = it.referName,
                        referUrl = getVarGroupReferUrl(
                            projectId = queryReq.projectId,
                            referType = it.referType,
                            referId = it.referId,
                            version = it.referVersionName?.toLong()
                        ),
                        referType = it.referType,
                        creator = template.creator,
                        modifier = template.creator,
                        updateTime = template.updateTime ?: LocalDateTime.now(),
                        instanceCount = countTemplateVersionInstances(
                            projectId = queryReq.projectId,
                            templateId = it.referId,
                            version = it.referVersionName?.toLong()!!
                        )
                    )
                }
                else -> throw IllegalArgumentException("Unsupported refer type")
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
     */
    private fun getVarGroupReferUrl(
        projectId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referId: String,
        version: Long? = null
    ): String {
        return when (referType) {
            PublicVerGroupReferenceTypeEnum.PIPELINE -> 
                String.format(PIPELINE_CONSOLE_URL_TEMPLATE, projectId, referId)
            PublicVerGroupReferenceTypeEnum.TEMPLATE -> {
                String.format(TEMPLATE_CONSOLE_URL_TEMPLATE, projectId, referId, version!!)
            }
        }
    }

}