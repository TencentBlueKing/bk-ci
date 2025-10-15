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
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.metrics.api.ServiceMetricsResource
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_UPDATE_FAILED
import com.tencent.devops.process.dao.`var`.PublicVarDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupReferInfoDao
import com.tencent.devops.process.dao.`var`.PublicVarReferInfoDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.pojo.`var`.`do`.PublicGroupVarRefDO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupInfoQueryReqDTO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupReferDTO
import com.tencent.devops.process.pojo.`var`.enums.PublicVarTypeEnum
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
    private val templatePipelineDao: TemplatePipelineDao,
    private val publicVarService: PublicVarService,
    private val publicVarDao: PublicVarDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarGroupReferInfoService::class.java)
        private const val PIPELINE_CONSOLE_URL_TEMPLATE = "/console/pipeline/%s/%s/history/pipeline/%s"
        private const val TEMPLATE_CONSOLE_URL_TEMPLATE = "/console/pipeline/%s/template/%s/%s/pipeline"
        
        // 批量操作相关常量
        private const val DEFAULT_BATCH_SIZE = 100
        private const val MAX_RETRY_TIMES = 3
    }

    /**
     * 批量计算多个引用的实际变量数量（基于变量组VAR_COUNT总和）
     */
    private fun batchCountActualVarReferences(
        projectId: String,
        referInfos: List<PipelinePublicVarGroupReferPO>,
        referType: PublicVerGroupReferenceTypeEnum
    ): Map<String, Int> {
        if (referInfos.isEmpty()) {
            return emptyMap()
        }

        try {
            val referInfosList = referInfos.map { Pair(it.referId, it.referVersion) }.distinct()

            // 批量查询所有引用信息
            val allGroupInfos = publicVarGroupReferInfoDao.batchListVarGroupReferInfoByReferIds(
                dslContext = dslContext,
                projectId = projectId,
                referinfos = referInfosList,
                referType = referType
            )
            
            if (allGroupInfos.isEmpty()) {
                return emptyMap()
            }
            
            // 收集所有需要查询的变量组版本信息并去重
            val groupVersionsToQuery = allGroupInfos
                .map { Pair(it.groupName, it.version) }
            
            // 分离最新版本和指定版本的变量组
            val (latestGroups, specificGroups) = groupVersionsToQuery.partition { it.second == -1 }
            
            // 批量查询变量组的varCount
            val groupVarCounts = mutableMapOf<Pair<String, Int>, Int>()
            
            // 查询最新版本的变量组
            if (latestGroups.isNotEmpty()) {
                val latestGroupNames = latestGroups.map { it.first }.distinct()
                val latestVarCounts = publicVarGroupDao.batchGetLatestVarCountsByGroupNames(
                    dslContext = dslContext,
                    projectId = projectId,
                    groupNames = latestGroupNames
                )
                latestGroups.forEach { (groupName, _) ->
                    groupVarCounts[Pair(groupName, -1)] = latestVarCounts[groupName] ?: 0
                }
            }
            
            // 查询指定版本的变量组
            if (specificGroups.isNotEmpty()) {
                val specificGroupVersions = specificGroups
                    .filter { it.second != -1 }
                val specificVarCounts = publicVarGroupDao.batchGetSpecificVarCountsByGroupVersions(
                    dslContext = dslContext,
                    projectId = projectId,
                    groupVersions = specificGroupVersions
                )
                specificGroups.forEach { (groupName, version) ->
                    groupVarCounts[Pair(groupName, version)] = specificVarCounts[Pair(groupName, version)] ?: 0
                }
            }
            
            // 按referId分组并计算总变量数量
            return allGroupInfos.groupBy { it.referId }
                .mapValues { (_, groupInfos) ->
                    groupInfos.sumOf { groupInfo ->
                        groupVarCounts[Pair(groupInfo.groupName, groupInfo.version)] ?: 0
                    }
                }
        } catch (e: Throwable) {
            logger.warn("Failed to batch count actual var references for projectId: $projectId", e)
            return emptyMap()
        }
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
        if (referInfos.isEmpty()) {
            return
        }
        
        try {
            // 按组名和版本分组统计需要更新的引用数量，过滤掉版本为-1的记录
            val referCountUpdates = referInfos
                .filter { it.version != -1 }
                .groupBy { Pair(it.groupName, it.version) }
                .mapValues { it.value.size }
            
            if (referCountUpdates.isEmpty()) {
                return
            }
            
            logger.info("Updating reference counts for ${referCountUpdates.size} groups, isIncrement: $isIncrement")
            
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
        } catch (e: Throwable) {
            logger.warn("Failed to update reference counts for projectId: $projectId", e)
            throw e
        }
    }
    
    /**
     * 更新单个变量组的引用计数
     */
    private fun updateSingleGroupReferCount(
        context: DSLContext,
        projectId: String,
        groupName: String,
        version: Int,
        countChange: Int
    ) {
        
        try {
            val currentGroupRecord = publicVarGroupDao.getRecordByGroupName(
                dslContext = context,
                projectId = projectId,
                groupName = groupName,
                version = version
            )
            
            if (currentGroupRecord == null) {
                logger.warn("Group record not found: $groupName, version: $version, projectId: $projectId")
                return
            }

            val newReferCount = maxOf(0, currentGroupRecord.referCount + countChange)

            // 更新 T_PIPELINE_PUBLIC_VAR_GROUP 表的引用计数
            publicVarGroupDao.updateReferCount(
                dslContext = context,
                projectId = projectId,
                groupName = groupName,
                version = currentGroupRecord.version,
                referCount = newReferCount
            )

            publicVarGroupDao.updateVarGroupNameReferCount(
                dslContext = context,
                projectId = projectId,
                groupName = groupName,
                referCount = newReferCount
            )
            
            // 更新变量引用计数
            updateVarReferCounts(
                projectId = projectId,
                groupName = groupName,
                version = version,
                countChange = countChange
            )
         } catch (e: Throwable) {
            logger.warn("Failed to update single group refer count for group: $groupName, version: $version", e)
            throw e
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
            // 查询要删除的引用记录
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
            
            logger.info("Found ${referInfosToDelete.size} references to delete for referId: $referId")
            
            // 执行删除操作
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                
                // 删除变量组引用记录
                publicVarGroupReferInfoDao.deleteByReferId(
                    dslContext = context,
                    projectId = projectId,
                    referId = referId,
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
            
            // 更新引用计数
            updateReferenceCounts(
                context = dslContext,
                projectId = projectId,
                referInfos = referInfosToDelete,
                isIncrement = false
            )
            
            logger.info("Successfully deleted ${referInfosToDelete.size} references for referId: $referId")
        } catch (t: Throwable) {
            logger.warn("Failed to delete refer info for referId: $referId", t)
            throw ErrorCodeException(errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_UPDATE_FAILED)
        }
    }

    /**
     * 查询变量引用信息
     * @param queryReq 查询请求
     * @return 分页结果
     */
    fun listVarReferInfo(queryReq: PublicVarGroupInfoQueryReqDTO): Page<PublicGroupVarRefDO> {
        val (totalCount, varGroupReferInfo) = queryVarGroupReferInfo(queryReq)
        logger.info("listVarReferInfo queryReq:{$queryReq }totalCount: $totalCount ${varGroupReferInfo.size}")
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
        
        // 处理流水线和模板类型的引用
        val pipelineReferList = processPipelineReferences(queryReq, referInfoByType)
        val templateReferList = processTemplateReferences(queryReq, referInfoByType)
        
        records.addAll(pipelineReferList)
        records.addAll(templateReferList)

        return Page(
            count = totalCount.toLong(),
            page = queryReq.page,
            pageSize = queryReq.pageSize,
            records = records
        )
    }

    /**
     * 处理流水线类型的引用
     */
    private fun processPipelineReferences(
        queryReq: PublicVarGroupInfoQueryReqDTO,
        referInfoByType: Map<PublicVerGroupReferenceTypeEnum, List<PipelinePublicVarGroupReferPO>>
    ): List<PublicGroupVarRefDO> {
        return referInfoByType[PublicVerGroupReferenceTypeEnum.PIPELINE]?.let { pipelineReferInfos ->
            if (pipelineReferInfos.isEmpty()) {
                return@let emptyList()
            }
            
            try {
                val pipelineIds = pipelineReferInfos.map { it.referId }.distinct()

                // 批量查询流水线执行次数
                val pipelineExecCounts = client.get(ServiceMetricsResource::class)
                    .queryPipelineMonthlyExecCountByList(queryReq.projectId, pipelineIds)
                    .data ?: emptyMap()
                
                // 批量查询实际引用变量数
                val actualRefCounts = batchCountActualVarReferences(
                    projectId = queryReq.projectId,
                    referInfos = pipelineReferInfos,
                    referType = PublicVerGroupReferenceTypeEnum.PIPELINE
                )

                // 构建流水线引用记录
                pipelineReferInfos.map { referInfo ->
                    PublicGroupVarRefDO(
                        referId = referInfo.referId,
                        referName = referInfo.referName,
                        referUrl = getVarGroupReferUrl(
                            projectId = queryReq.projectId,
                            referType = referInfo.referType,
                            referId = referInfo.referId,
                            version = referInfo.referVersion.toLong(),
                        ),
                        referType = referInfo.referType,
                        creator = referInfo.creator,
                        modifier = referInfo.modifier,
                        updateTime = referInfo.updateTime,
                        actualRefCount = actualRefCounts[referInfo.referId] ?: 0,
                        executeCount = pipelineExecCounts[referInfo.referId] ?: 0
                    )
                }
            } catch (e: Throwable) {
                logger.warn("Failed to process pipeline references", e)
                emptyList()
            }
        } ?: emptyList()
    }

    /**
     * 处理模板类型的引用
     */
    private fun processTemplateReferences(
        queryReq: PublicVarGroupInfoQueryReqDTO,
        referInfoByType: Map<PublicVerGroupReferenceTypeEnum, List<PipelinePublicVarGroupReferPO>>
    ): List<PublicGroupVarRefDO> {
        return referInfoByType[PublicVerGroupReferenceTypeEnum.TEMPLATE]?.let { templateReferInfos ->
            if (templateReferInfos.isEmpty()) {
                return@let emptyList()
            }

            try {

                // 批量查询实际引用变量数
                val actualRefCounts = batchCountActualVarReferences(
                    projectId = queryReq.projectId,
                    referInfos = templateReferInfos,
                    referType = PublicVerGroupReferenceTypeEnum.TEMPLATE
                )

                // 批量查询模板信息
                val templateKeys = templateReferInfos
                    .map { Pair(it.referId, it.referVersion.toLong()) }
                    
                val templateMap = templateKeys.associateWith { (templateId, version) ->
                    templateDao.getTemplate(
                        dslContext = dslContext,
                        templateId = templateId,
                        version = version
                    )
                }.filterValues { it != null }.mapValues { it.value!! }

                // 构建模板引用记录
                templateReferInfos.mapNotNull { referInfo ->
                    val templateKey = Pair(referInfo.referId, referInfo.referVersion.toLong())
                    val template = templateMap[templateKey]
                    
                    if (template == null) {
                        logger.warn("Template not found: ${referInfo.referId}, version: ${referInfo.referVersion}")
                        return@mapNotNull null
                    }
                    
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
                        actualRefCount = actualRefCounts[referInfo.referId] ?: 0,
                        instanceCount = countTemplateVersionInstances(
                            projectId = queryReq.projectId,
                            templateId = referInfo.referId,
                            version = template.version
                        )
                    )
                }
            } catch (e: Throwable) {
                logger.warn("Failed to process template references", e)
                emptyList()
            }
        } ?: emptyList()
    }

    /**
     * 查询变量组引用信息
     */
    private fun queryVarGroupReferInfo(
        queryReq: PublicVarGroupInfoQueryReqDTO
    ): Pair<Int, List<PipelinePublicVarGroupReferPO>> {
        val projectId = queryReq.projectId
        val groupName = queryReq.groupName!!
        val version = queryReq.version

        try {
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
        } catch (e: Throwable) {
            logger.warn("Failed to query var group refer info for group: $groupName", e)
            return Pair(0, emptyList())
        }
    }

    /**
     * 统计模板版本实例数量
     * @param projectId 项目ID
     * @param templateId 模板ID
     * @param version 版本号
     * @return 实例数量
     */
    private fun countTemplateVersionInstances(projectId: String, templateId: String, version: Long): Int {
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

        // 检查参数ID是否存在重复名称参数
        val seenIds = HashSet<String>(params.size)
        val duplicateIds = mutableSetOf<String>()

        for (param in params) {
            if (!seenIds.add(param.id)) {
                duplicateIds.add(param.id)
            }
        }

        if (duplicateIds.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_CONFLICT,
                params = arrayOf(duplicateIds.joinToString(", "))
            )
        }

        // 查询历史引用记录
        val historicalReferInfos = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferId(
            dslContext = dslContext,
            projectId = publicVarGroupReferDTO.projectId,
            referId = publicVarGroupReferDTO.referId,
            referType = publicVarGroupReferDTO.referType,
            referVersion = publicVarGroupReferDTO.referVersion
        )

        model.handlePublicVarInfo()
        val publicVarGroups = model.publicVarGroups
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
                        index = index,
                        type = if (element.constant == true) PublicVarTypeEnum.CONSTANT else PublicVarTypeEnum.VARIABLE
                    )
                }
            }
        val pipelinePublicVarGroupReferPOs = mutableListOf<PipelinePublicVarGroupReferPO>()
        if (dynamicPublicVarWithPositions.isNotEmpty()) {
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
             dynamicPublicVarWithPositions.forEach() { (groupKey, positionInfos) ->
                 pipelinePublicVarGroupReferPOs.add(
                     PipelinePublicVarGroupReferPO(
                         id = segmentIds[index++] ?: 0,
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
                 )
            }
        }

        // 异步更新引用计数和批量保存
        asyncUpdateReferenceCountsAfterSave(
            projectId = publicVarGroupReferDTO.projectId,
            historicalReferInfos = historicalReferInfos,
            publicVarGroupNames = publicVarGroups?.map { it.groupName } ?: emptyList(),
            newReferInfos = pipelinePublicVarGroupReferPOs
        )
    }

    /**
     * 异步更新引用计数
     * @param projectId 项目ID
     * @param historicalReferInfos 历史引用信息
     * @param publicVarGroupNames 当前变量组名称列表
     * @param newReferInfos 新的引用信息列表，需要批量保存
     */
    private fun asyncUpdateReferenceCountsAfterSave(
        projectId: String,
        historicalReferInfos: List<PipelinePublicVarGroupReferPO>,
        publicVarGroupNames: List<String>,
        newReferInfos: List<PipelinePublicVarGroupReferPO> = emptyList()
    ) {
        if (historicalReferInfos.isEmpty() && publicVarGroupNames.isEmpty() && newReferInfos.isEmpty()) {
            return
        }

        java.util.concurrent.Executors.newFixedThreadPool(1).submit {
            try {
                // 首先批量保存新的引用信息
                if (newReferInfos.isNotEmpty()) {
                    publicVarGroupReferInfoDao.batchSave(dslContext, newReferInfos)
                }

                // 构建历史引用映射（按组名）
                val historicalGroupNames = historicalReferInfos.map { it.groupName }.toSet()
                val currentGroupNames = publicVarGroupNames.toSet()

                // 如果历史引用和当前引用完全相同，无需更新
                if (historicalGroupNames == currentGroupNames) {
                    return@submit
                }

                // 计算需要删除的引用（历史中有，当前中没有）
                val groupsToDelete = historicalReferInfos.filter { historical ->
                    !currentGroupNames.contains(historical.groupName)
                }

                // 计算需要新增的引用（当前中有，历史中没有）
                val groupsToAdd = currentGroupNames - historicalGroupNames

                // 处理删除的引用
                if (groupsToDelete.isNotEmpty()) {
                    // 更新引用计数（减少）
                    groupsToDelete.forEach { groupToDelete ->
                        if (groupToDelete.version != -1) {
                            updateSingleGroupReferCount(
                                context = dslContext,
                                projectId = projectId,
                                groupName = groupToDelete.groupName,
                                version = groupToDelete.version,
                                countChange = -1
                            )
                        }
                    }

                    // 删除引用记录
                    dslContext.transaction { configuration ->
                        val context = DSL.using(configuration)
                        groupsToDelete.forEach { groupToDelete ->
                            publicVarGroupReferInfoDao.deleteByReferIdAndGroup(
                                dslContext = context,
                                projectId = projectId,
                                referId = groupToDelete.referId,
                                referType = groupToDelete.referType,
                                groupName = groupToDelete.groupName,
                                referVersion = groupToDelete.referVersion
                            )
                            
                            publicVarReferInfoDao.deleteByReferIdAndGroup(
                                dslContext = context,
                                projectId = projectId,
                                referId = groupToDelete.referId,
                                referType = groupToDelete.referType,
                                groupName = groupToDelete.groupName,
                                referVersion = groupToDelete.referVersion
                            )
                        }
                    }
                }

                // 处理新增的引用（更新引用计数）
                groupsToAdd.forEach { groupName ->
                    // 从newReferInfos中找到对应的引用信息来获取版本
                    val newReferInfo = newReferInfos.find { it.groupName == groupName }
                    if (newReferInfo != null && newReferInfo.version != -1) {
                        updateSingleGroupReferCount(
                            context = dslContext,
                            projectId = projectId,
                            groupName = groupName,
                            version = newReferInfo.version,
                            countChange = 1
                        )
                    }
                }

                logger.info("Successfully updated reference counts for projectId: $projectId")

            } catch (e: Throwable) {
                logger.warn("Async update reference count failed for projectId: $projectId", e)
            }
        }
    }

    /**
     * 更新变量引用计数
     */
    private fun updateVarReferCounts(
        projectId: String,
        groupName: String,
        version: Int,
        countChange: Int
    ) {
        try {
            publicVarService.updateVarReferCounts(
                projectId = projectId,
                groupName = groupName,
                version = version,
                countChange = countChange
            )
        } catch (e: Throwable) {
            logger.warn("Failed to update variable refer counts for group: $groupName, version: $version", e)
        }
    }

    /**
     * 获取变量组引用的URL
     */
    private fun getVarGroupReferUrl(
        projectId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referId: String,
        version: Long
    ): String {
        val template = when (referType) {
            PublicVerGroupReferenceTypeEnum.PIPELINE -> PIPELINE_CONSOLE_URL_TEMPLATE
            PublicVerGroupReferenceTypeEnum.TEMPLATE -> TEMPLATE_CONSOLE_URL_TEMPLATE
        }
        return String.format(template, projectId, referId, version)
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
            // 查询要删除的引用记录
            val referInfosToDelete = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferId(
                dslContext = dslContext,
                projectId = projectId,
                referId = referId,
                referType = referType
            ).filter { it.referVersion == referVersion }

            if (referInfosToDelete.isEmpty()) {
                logger.info("No reference found for referId: $referId with version: $referVersion, skip deletion")
                return
            }
            
            logger.info("Found ${referInfosToDelete.size} references to delete for referId: $referId with version: $referVersion")

            // 执行删除操作
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                
                // 删除指定版本的变量组引用记录
                publicVarGroupReferInfoDao.deleteByReferId(
                    dslContext = context,
                    projectId = projectId,
                    referId = referId,
                    referType = referType,
                    referVersion = referVersion
                )
            }

            // 更新引用计数
            updateReferenceCounts(
                context = dslContext,
                projectId = projectId,
                referInfos = referInfosToDelete,
                isIncrement = false
            )

            logger.info("Successfully deleted ${referInfosToDelete.size} " +
                    "references for referId: $referId with version: $referVersion")
        } catch (t: Throwable) {
            logger.warn("Failed to delete refer info for referId: $referId with version: $referVersion", t)
            throw ErrorCodeException(errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_UPDATE_FAILED)
        }
    }

}