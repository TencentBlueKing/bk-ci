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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.metrics.api.ServiceMetricsResource
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_UPDATE_FAILED
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
import com.tencent.devops.process.pojo.`var`.po.PublicVarPositionPO
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
    private val templatePipelineDao: TemplatePipelineDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarGroupReferInfoService::class.java)
        private const val PIPELINE_CONSOLE_URL_TEMPLATE = "/console/pipeline/%s/%s/history/pipeline"
        private const val TEMPLATE_CONSOLE_URL_TEMPLATE = "/console/pipeline/%s/template/%s/%s/pipeline"
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

            // 更新 T_PIPELINE_PUBLIC_VAR_GROUP 表的引用计数
            publicVarGroupDao.updateReferCount(
                dslContext = context,
                projectId = projectId,
                groupName = groupName,
                version = record.version,
                referCount = newReferCount
            )

            publicVarGroupDao.updateVarGroupNameReferCount(
                dslContext = context,
                projectId = projectId,
                groupName = groupName,
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
                positionInfo = referPO.positionInfo?.let { JsonUtil.toJson(it, false) },
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

        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)

            // 先获取所有要删除的引用记录，用于减少referCount
            val allReferInfosToDelete = collectReferInfosToDelete(
                context = context,
                projectId = projectId,
                referIds = referIds,
                referType = referType
            )

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

            // 为每个被删除的变量组引用减少referCount计数
            updateReferenceCountsForDeletedRefs(
                context = context,
                projectId = projectId,
                deletedReferInfos = allReferInfosToDelete
            )
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

    fun handleVarGroupReferBus(
        publicVarGroupReferDTO: PublicVarGroupReferDTO
    ) {
        logger.info("handleVarGroupBus publicVarGroupReferDTO:$publicVarGroupReferDTO")
        val model = publicVarGroupReferDTO.model
        val params = model.getTriggerContainer().params
        if (params.isEmpty()) {
            return
        }
        model.handlePublicVarInfo()
        // 提取非固定版本的变量组变量并记录位置信息
        data class GroupKey(val groupName: String, val version: Int?)

        val dynamicPublicVarWithPositions = params.withIndex()
            .filter { (_, element) ->
                !element.varGroupName.isNullOrBlank() && element.varGroupVersion == null
            }
            .groupBy { (_, element) ->
                GroupKey(element.varGroupName!!, element.varGroupVersion)
            }
            .mapValues { (key, group) ->
                group.map { (index, element) ->
                    PublicVarPositionPO(
                        groupName = key.groupName,
                        version = key.version,
                        varName = element.id,
                        index = index
                    )
                }
            }
        // 提取所有需要删除的索引，并按降序排序
        val indicesToRemove = dynamicPublicVarWithPositions.values
            .flatMap { it.map { po -> po.index } } // 提取所有索引
            .distinct() // 确保索引唯一
            .sortedDescending() // 降序排序

        // 按降序索引从 params 中移除元素
        indicesToRemove.forEach { index ->
            if (index < params.size) {
                params.removeAt(index)
            }
        }
        // 批量保存
        val currentTime = LocalDateTime.now()
        val segmentIds = client.get(ServiceAllocIdResource::class)
            .batchGenerateSegmentId("T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO", dynamicPublicVarWithPositions.size).data
        if (segmentIds.isNullOrEmpty()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_REST_EXCEPTION_COMMON_TIP)
        }
        var index = 0
        val pipelinePublicVarGroupReferPOs = dynamicPublicVarWithPositions.map { (groupKey, positionInfos) ->
            PipelinePublicVarGroupReferPO(
                id = segmentIds[index++] ?: 0,
                projectId = publicVarGroupReferDTO.projectId,
                groupName = groupKey.groupName,
                version = groupKey.version,
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
        publicVarGroupReferInfoDao.batchSave(dslContext, pipelinePublicVarGroupReferPOs)
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

    /**
     * 删除指定版本的变量组引用
     */
    fun delitePublicGroupRefer(
        userId: String,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referVersion: Int
    ) {
        
        try {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                
                // 先获取要删除的引用记录，用于减少referCount
                val referInfosToDelete = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferId(
                    dslContext = context,
                    projectId = projectId,
                    referId = referId,
                    referType = referType
                ).filter { it.referVersion == referVersion }
                
                if (referInfosToDelete.isEmpty()) {
                    logger.info("No reference found for referId: $referId with version: $referVersion, skip deletion")
                    return@transaction
                }
                
                // 删除指定版本的变量组引用记录
                referInfosToDelete.forEach { referInfo ->
                    publicVarGroupReferInfoDao.deleteByReferIds(
                        dslContext = context,
                        projectId = projectId,
                        referIds = listOf(referId),
                        referType = referType
                    )
                }
                
                // 为每个被删除的变量组引用减少referCount计数
                updateReferenceCountsForDeletedRefs(
                    context = context,
                    projectId = projectId,
                    deletedReferInfos = referInfosToDelete
                )
                
                logger.info("Successfully deleted ${referInfosToDelete.size} " +
                        "references for referId: $referId with version: $referVersion")
            }
        } catch (t: Throwable) {
            logger.warn("Failed to delete refer info for referId: $referId with version: $referVersion", t)
            throw ErrorCodeException(errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_UPDATE_FAILED)
        }
    }

}