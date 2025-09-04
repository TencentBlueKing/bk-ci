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
import com.tencent.devops.metrics.api.ServiceMetricsResource
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupPipelineConfigDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupReferInfoDao
import com.tencent.devops.process.dao.`var`.PublicVarReferInfoDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.pojo.`var`.`do`.PublicGroupVarRefDO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupInfoQueryReqDTO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupReferDTO
import com.tencent.devops.process.pojo.`var`.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PipelinePublicVarGroupReferPO
import com.tencent.devops.process.pojo.`var`.po.PublicVarGroupPipelineConfigPO
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
    private val publicVarGroupPipelineConfigDao: PublicVarGroupPipelineConfigDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarGroupReferInfoService::class.java)
    }

    fun updatePublicGroupRefer(
        userId: String,
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO
    ): Boolean {
        // 处理空变量组引用的情况
        if (publicVarGroupReferInfo.publicVarGroupRefs.isEmpty()) {
            return handleEmptyVarGroupRefs(projectId, publicVarGroupReferInfo)
        }

        // 验证变量组存在性并构建数据对象
        val (publicVarGroupReferPOs, publicVarGroupPipelineConfigPOs) = buildDataObjects(
            userId, projectId, publicVarGroupReferInfo
        )

        // 执行数据库事务操作
        return executeTransaction(
            userId, projectId, publicVarGroupReferInfo, publicVarGroupReferPOs, publicVarGroupPipelineConfigPOs
        )
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
    ): Pair<List<PipelinePublicVarGroupReferPO>, List<PublicVarGroupPipelineConfigPO>> {
        val publicVarGroupReferPOs = mutableListOf<PipelinePublicVarGroupReferPO>()
        val publicVarGroupPipelineConfigPOs = mutableListOf<PublicVarGroupPipelineConfigPO>()
        
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
                createVarGroupReferPO(userId, projectId, publicVarGroupReferInfo, groupName, version)
            )
            
            // 构建配置对象
            publicVarGroupPipelineConfigPOs.add(
                createPipelineConfigPO(userId, projectId, publicVarGroupReferInfo, groupName, version, ref.positionInfoMap)
            )
        }
        
        return Pair(publicVarGroupReferPOs, publicVarGroupPipelineConfigPOs)
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
            modifier = userId,
            updateTime = LocalDateTime.now(),
            creator = userId,
            createTime = LocalDateTime.now()
        )
    }

    /**
     * 创建流水线配置PO对象
     */
    private fun createPipelineConfigPO(
        userId: String,
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO,
        groupName: String,
        version: Int?,
        positionInfoMap: Map<String, Int>
    ): PublicVarGroupPipelineConfigPO {
        val positionInfoJson = if (positionInfoMap.isNotEmpty()) {
            JsonUtil.toJson(positionInfoMap)
        } else {
            null
        }
        
        return PublicVarGroupPipelineConfigPO(
            id = client.get(ServiceAllocIdResource::class)
                .generateSegmentId("T_PIPELINE_PUBLIC_VAR_GROUP_PIPELINE_CONFIG").data ?: 0,
            projectId = projectId,
            referId = publicVarGroupReferInfo.referId,
            referVersionName = publicVarGroupReferInfo.referVersionName,
            groupName = groupName,
            groupVersion = version ?: 0,
            positionInfo = positionInfoJson,
            referType = publicVarGroupReferInfo.referType.name,
            creator = userId,
            modifier = userId,
            createTime = LocalDateTime.now(),
            updateTime = LocalDateTime.now()
        )
    }

    /**
     * 执行数据库事务操作
     */
    private fun executeTransaction(
        userId: String,
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO,
        publicVarGroupReferPOs: List<PipelinePublicVarGroupReferPO>,
        publicVarGroupPipelineConfigPOs: List<PublicVarGroupPipelineConfigPO>
    ): Boolean {
        try {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                val newGroupNames = publicVarGroupReferInfo.publicVarGroupRefs.map { it.groupName }.toSet()

                // 处理需要删除的旧引用
                if (newGroupNames.isNotEmpty()) {
                    handleObsoleteReferences(context, projectId, publicVarGroupReferInfo, newGroupNames)
                }

                // 批量保存新的引用记录
                if (publicVarGroupReferPOs.isNotEmpty()) {
                    publicVarGroupReferInfoDao.batchSave(context, publicVarGroupReferPOs)
                }
                
                // 批量保存流水线配置记录
                if (publicVarGroupPipelineConfigPOs.isNotEmpty()) {
                    publicVarGroupPipelineConfigDao.batchSave(context, publicVarGroupPipelineConfigPOs)
                }
                
                // 更新新增引用的计数
                updateReferenceCountsForNewRefs(context, projectId, publicVarGroupReferPOs)
            }
            
            // 处理变量引用信息
            publicVarReferInfoService.addPublicVarRefer(
                userId = userId,
                projectId = projectId,
                publicVarGroupReferInfo = publicVarGroupReferInfo
            )
        } catch (t: Throwable) {
            logger.warn("Failed to add pipeline group refer for ${publicVarGroupReferInfo.referId}", t)
            throw t
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
            publicVarGroupReferInfoDao.deleteByReferIdExcludingGroupNames(
                dslContext = context,
                projectId = projectId,
                referId = publicVarGroupReferInfo.referId,
                referType = publicVarGroupReferInfo.referType,
                referVersionName = publicVarGroupReferInfo.referVersionName,
                excludedGroupNames = newGroupNames.toList()
            )
            
            // 删除流水线配置记录
            deleteObsoletePipelineConfigs(context, projectId, publicVarGroupReferInfo, newGroupNames)
            
            // 更新引用计数
            updateReferenceCountsForDeletedRefs(context, projectId, toDeleteReferInfos)
        }
    }

    /**
     * 删除过时的流水线配置记录
     */
    private fun deleteObsoletePipelineConfigs(
        context: DSLContext,
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO,
        newGroupNames: Set<String>
    ) {
        val existingConfigs = publicVarGroupPipelineConfigDao.getByReferIdAndVersion(
            dslContext = context,
            projectId = projectId,
            referId = publicVarGroupReferInfo.referId,
            referVersionName = publicVarGroupReferInfo.referVersionName
        )
        
        existingConfigs.forEach { config ->
            if (config.groupName !in newGroupNames) {
                publicVarGroupPipelineConfigDao.delete(
                    dslContext = context,
                    projectId = projectId,
                    referId = publicVarGroupReferInfo.referId,
                    referVersionName = publicVarGroupReferInfo.referVersionName,
                    groupName = config.groupName
                )
            }
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
        publicVarGroupReferInfoDao.deleteByReferIdExcludingGroupNames(
            dslContext = context,
            projectId = projectId,
            referId = publicVarGroupReferInfo.referId,
            referType = publicVarGroupReferInfo.referType,
            referVersionName = publicVarGroupReferInfo.referVersionName
        )
        
        // 删除变量引用记录
        publicVarReferInfoDao.deleteByReferIdExcludingGroupNames(
            dslContext = context,
            projectId = projectId,
            referId = publicVarGroupReferInfo.referId,
            referType = publicVarGroupReferInfo.referType,
            referVersionName = publicVarGroupReferInfo.referVersionName
        )
        
        // 删除流水线配置记录
        val existingConfigs = publicVarGroupPipelineConfigDao.getByReferIdAndVersion(
            dslContext = context,
            projectId = projectId,
            referId = publicVarGroupReferInfo.referId,
            referVersionName = publicVarGroupReferInfo.referVersionName
        )
        
        existingConfigs.forEach { config ->
            publicVarGroupPipelineConfigDao.delete(
                dslContext = context,
                projectId = projectId,
                referId = publicVarGroupReferInfo.referId,
                referVersionName = publicVarGroupReferInfo.referVersionName,
                groupName = config.groupName
            )
        }
        
        // 更新引用计数
        updateReferenceCountsForDeletedRefs(context, projectId, existingReferInfos)
    }

    /**
     * 更新被删除引用的计数
     */
    private fun updateReferenceCountsForDeletedRefs(
        context: DSLContext,
        projectId: String,
        deletedReferInfos: List<PipelinePublicVarGroupReferPO>
    ) {
        deletedReferInfos.forEach { referInfo ->
            val currentGroupRecord = publicVarGroupDao.getRecordByGroupName(
                dslContext = context,
                projectId = projectId,
                groupName = referInfo.groupName,
                version = referInfo.version
            )
            currentGroupRecord?.let { record ->
                val newReferCount = maxOf(0, record.referCount - 1)
                publicVarGroupDao.updateReferCount(
                    dslContext = context,
                    projectId = projectId,
                    groupName = referInfo.groupName,
                    version = record.version,
                    referCount = newReferCount
                )
            }
        }
    }

    /**
     * 更新新增引用的计数
     */
    private fun updateReferenceCountsForNewRefs(
        context: DSLContext,
        projectId: String,
        newReferPOs: List<PipelinePublicVarGroupReferPO>
    ) {
        newReferPOs.forEach { referPO ->
            val currentGroupRecord = publicVarGroupDao.getRecordByGroupName(
                dslContext = context,
                projectId = projectId,
                groupName = referPO.groupName,
                version = referPO.version
            )
            currentGroupRecord?.let { record ->
                publicVarGroupDao.updateReferCount(
                    dslContext = context,
                    projectId = projectId,
                    groupName = referPO.groupName,
                    version = record.version,
                    referCount = record.referCount + 1
                )
            }
        }
    }

    fun deletePublicVerGroupRefByReferId(projectId: String, referId: String, referType: PublicVerGroupReferenceTypeEnum) {
        deletePublicVerGroupRefByReferIds(projectId, listOf(referId), referType)
    }

    /**
     * 批量删除referId列表对应的变量组引用记录
     * @param projectId 项目ID
     * @param referIds referId列表
     * @param referType 引用类型
     */
    fun deletePublicVerGroupRefByReferIds(
        projectId: String,
        referIds: List<String>,
        referType: PublicVerGroupReferenceTypeEnum
    ): Boolean {
        if (referIds.isEmpty()) {
            return false
        }
        
        try {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                
                // 先获取所有要删除的引用记录，用于减少referCount
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
                
                // 删除对应的流水线公共变量组配置记录
                referIds.forEach { referId ->
                    publicVarGroupPipelineConfigDao.deleteByReferId(
                        dslContext = context,
                        projectId = projectId,
                        referId = referId
                    )
                }
                
                // 为每个被删除的变量组引用减少referCount计数
                updateReferenceCountsForDeletedRefs(context, projectId, allReferInfosToDelete)
            }
        } catch (t: Throwable) {
            logger.warn("Failed to delete refer info for referIds: $referIds", t)
            throw t
        }
        return true
    }

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

    fun countTemplateVersionInstances(projectId: String, templateId: String, version: Long): Int {
        logger.info("[$projectId|$templateId|$version] List the templates version instances")

        return templatePipelineDao.countByTemplateByVersion(
            dslContext = dslContext,
            projectId = projectId,
            instanceType = PipelineInstanceTypeEnum.CONSTRAINT.type,
            templateId = templateId,
            version = version
        )
    }

    private fun getVarGroupReferUrl(
        projectId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referId: String,
        version: Long? = null
    ): String {
        return when (referType) {
            PublicVerGroupReferenceTypeEnum.PIPELINE -> "/console/pipeline/$projectId/$referId/history/pipeline"
            PublicVerGroupReferenceTypeEnum.TEMPLATE -> {
                "/console/pipeline/$projectId/template/$referId/$version/pipeline"
            }
        }
    }

}