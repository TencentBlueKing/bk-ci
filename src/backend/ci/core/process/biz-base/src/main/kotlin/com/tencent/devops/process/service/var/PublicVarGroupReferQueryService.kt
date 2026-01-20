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
import com.tencent.devops.common.api.util.toLocalDateTime
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.metrics.api.ServiceMetricsResource
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_NOT_EXIST
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_QUERY_FAILED
import com.tencent.devops.process.dao.template.PipelineTemplateResourceDao
import com.tencent.devops.process.dao.`var`.PublicVarDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupReferInfoDao
import com.tencent.devops.process.dao.`var`.PublicVarReferInfoDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.pojo.`var`.VarGroupReferInfoQueryResult
import com.tencent.devops.process.pojo.`var`.`do`.PublicGroupVarRefDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarDO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupInfoQueryReqDTO
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarGroupReferPO
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 公共变量组引用查询服务
 * 负责处理所有与变量组引用相关的查询操作
 */
@Service
class PublicVarGroupReferQueryService @Autowired constructor(
    private val dslContext: DSLContext,
    private val publicVarGroupDao: PublicVarGroupDao,
    private val client: Client,
    private val pipelineTemplateResourceDao: PipelineTemplateResourceDao,
    private val publicVarReferInfoDao: PublicVarReferInfoDao,
    private val publicVarGroupReferInfoDao: PublicVarGroupReferInfoDao,
    private val templatePipelineDao: TemplatePipelineDao,
    private val templateDao: TemplateDao,
    private val publicVarDao: PublicVarDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarGroupReferQueryService::class.java)
    }

    @Value("\${publicVarGroup.urlTemplates.base:#{null}}")
    val basePath: String = ""

    @Value("\${publicVarGroup.urlTemplates.pipeline:#{null}}")
    val pipelinePath: String = ""

    @Value("\${publicVarGroup.urlTemplates.template:#{null}}")
    val templatePath: String = ""

    /**
     * 查询变量引用信息
     * @param queryReq 查询请求
     * @return 分页结果
     */
    fun listVarReferInfo(queryReq: PublicVarGroupInfoQueryReqDTO): Page<PublicGroupVarRefDO> {

        val queryResult = queryVarGroupReferInfo(queryReq)
        val totalCount = queryResult.totalCount
        val varGroupReferInfo = queryResult.referInfos

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
        val pipelineReferList = processPipelineReferences(
            queryReq = queryReq,
            referInfoByType = referInfoByType
        )
        val templateReferList = processTemplateReferences(
            queryReq = queryReq,
            referInfoByType = referInfoByType
        )

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
        referInfoByType: Map<PublicVerGroupReferenceTypeEnum, List<ResourcePublicVarGroupReferPO>>
    ): List<PublicGroupVarRefDO> {
        val pipelineReferInfos = referInfoByType[PublicVerGroupReferenceTypeEnum.PIPELINE]
            ?: return emptyList()

        if (pipelineReferInfos.isEmpty()) {
            return emptyList()
        }

        try {
            val pipelineIds = pipelineReferInfos.map { it.referId }.distinct()

            // 批量查询流水线执行次数
            val pipelineExecCounts = client.get(ServiceMetricsResource::class)
                .queryPipelineMonthlyExecCountByList(queryReq.projectId, pipelineIds)
                .data ?: emptyMap()

            // 构建流水线引用记录
            return pipelineReferInfos.map { referInfo ->
                // 查询该流水线实际引用的变量数量
                val actualRefCount = publicVarReferInfoDao.countActualVarReferencesByReferId(
                    dslContext = dslContext,
                    projectId = queryReq.projectId,
                    referId = referInfo.referId,
                    referType = PublicVerGroupReferenceTypeEnum.PIPELINE,
                    referVersion = referInfo.referVersion
                )

                PublicGroupVarRefDO(
                    referId = referInfo.referId,
                    referName = referInfo.referName,
                    referUrl = getVarGroupReferUrl(
                        projectId = queryReq.projectId,
                        referType = referInfo.referType,
                        referId = referInfo.referId,
                        version = referInfo.referVersion.toLong()
                    ),
                    referType = referInfo.referType,
                    creator = referInfo.creator,
                    modifier = referInfo.modifier,
                    updateTime = referInfo.updateTime,
                    actualRefCount = actualRefCount,
                    executeCount = pipelineExecCounts[referInfo.referId] ?: 0
                )
            }
        } catch (e: Throwable) {
            logger.warn("Failed to process pipeline references for project: ${queryReq.projectId}", e)
            throw ErrorCodeException(
                errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_QUERY_FAILED
            )
        }
    }

    /**
     * 处理模板类型的引用
     */
    private fun processTemplateReferences(
        queryReq: PublicVarGroupInfoQueryReqDTO,
        referInfoByType: Map<PublicVerGroupReferenceTypeEnum, List<ResourcePublicVarGroupReferPO>>
    ): List<PublicGroupVarRefDO> {
        val templateReferInfos = referInfoByType[PublicVerGroupReferenceTypeEnum.TEMPLATE]
            ?: return emptyList()

        if (templateReferInfos.isEmpty()) {
            return emptyList()
        }

        try {
            // 批量查询模板信息
            val templateKeys = templateReferInfos
                .map { Pair(it.referId, it.referVersion.toLong()) }
                .distinct()

            val templateMap = templateKeys.mapNotNull { (templateId, version) ->
                pipelineTemplateResourceDao.get(
                    dslContext = dslContext,
                    projectId = queryReq.projectId,
                    templateId = templateId,
                    version = version
                )?.let { Pair(templateId, version) to it }
            }.toMap()

            // 构建模板引用记录
            return templateReferInfos.mapNotNull { referInfo ->
                val templateKey = Pair(referInfo.referId, referInfo.referVersion.toLong())
                val template = templateMap[templateKey]

                if (template == null) {
                    logger.warn("Template not found: ${referInfo.referId}, version: ${referInfo.referVersion}")
                    return@mapNotNull null
                }

                // 查询该模板实际引用的变量数量
                val actualRefCount = publicVarReferInfoDao.countActualVarReferencesByReferId(
                    dslContext = dslContext,
                    projectId = queryReq.projectId,
                    referId = referInfo.referId,
                    referType = PublicVerGroupReferenceTypeEnum.TEMPLATE,
                    referVersion = referInfo.referVersion
                )

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
                    updateTime = template.updateTime?.toLocalDateTime() ?: LocalDateTime.now(),
                    actualRefCount = actualRefCount,
                    instanceCount = countTemplateVersionInstances(
                        projectId = queryReq.projectId,
                        templateId = referInfo.referId,
                        version = template.version
                    )
                )
            }
        } catch (e: Throwable) {
            logger.warn("Failed to process template references for project: ${queryReq.projectId}", e)
            throw ErrorCodeException(
                errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_QUERY_FAILED
            )
        }
    }

    /**
     * 查询变量组引用信息（查询所有版本的引用）
     */
    fun queryVarGroupReferInfo(
        queryReq: PublicVarGroupInfoQueryReqDTO
    ): VarGroupReferInfoQueryResult {
        val projectId = queryReq.projectId
        val groupName = queryReq.groupName!!
        val varName = queryReq.varName

        try {
            // 检查变量组是否存在（查询任意版本以确认变量组存在）
            val varGroupRecord = publicVarGroupDao.getRecordByGroupName(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName
            )

            if (varGroupRecord == null) {
                return VarGroupReferInfoQueryResult(
                    totalCount = 0,
                    referInfos = emptyList()
                )
            }

            // 如果指定了 varName，则查询引用了该变量的资源列表
            if (!varName.isNullOrBlank()) {
                return queryVarGroupReferInfoByVarNameAllVersions(
                    queryReq = queryReq,
                    groupName = groupName,
                    varName = varName
                )
            }

            // 查询所有版本的引用
            // 统计总数
            val totalCount = publicVarGroupReferInfoDao.countByGroupName(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName,
                referType = queryReq.referType
            )

            // 查询引用信息
            val varGroupReferInfo = publicVarGroupReferInfoDao.listVarGroupReferInfo(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName,
                referType = queryReq.referType,
                page = queryReq.page,
                pageSize = queryReq.pageSize
            )

            return VarGroupReferInfoQueryResult(
                totalCount = totalCount,
                referInfos = varGroupReferInfo
            )
        } catch (e: Throwable) {
            logger.warn("Failed to query var group refer info for group: $groupName in project: $projectId", e)
            throw ErrorCodeException(
                errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_QUERY_FAILED
            )
        }
    }

    /**
     * 根据变量名查询变量组引用信息（所有版本）
     */
    private fun queryVarGroupReferInfoByVarNameAllVersions(
        queryReq: PublicVarGroupInfoQueryReqDTO,
        groupName: String,
        varName: String
    ): VarGroupReferInfoQueryResult {
        val projectId = queryReq.projectId

        try {
            // 查询引用了该变量的 referId 列表（所有版本）
            val referIds = publicVarReferInfoDao.listReferIdsByVarName(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName,
                varName = varName,
                referType = queryReq.referType
            )

            if (referIds.isEmpty()) {
                return VarGroupReferInfoQueryResult(
                    totalCount = 0,
                    referInfos = emptyList()
                )
            }

            // 统计总数
            val totalCount = publicVarGroupReferInfoDao.countByReferIds(
                dslContext = dslContext,
                projectId = projectId,
                referIds = referIds,
                referType = queryReq.referType
            )

            // 查询详细信息
            val varGroupReferInfo = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferIds(
                dslContext = dslContext,
                projectId = projectId,
                referIds = referIds,
                referType = queryReq.referType,
                page = queryReq.page,
                pageSize = queryReq.pageSize
            )

            return VarGroupReferInfoQueryResult(
                totalCount = totalCount,
                referInfos = varGroupReferInfo
            )
        } catch (e: Throwable) {
            logger.warn(
                "Failed to query var group refer info by varName: $varName (all versions) in project: $projectId",
                e
            )
            throw ErrorCodeException(
                errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_QUERY_FAILED
            )
        }
    }

    /**
     * 统计模板版本实例数量
     * @param projectId 项目ID
     * @param templateId 模板ID
     * @param version 版本号
     * @return 实例数量
     */
    fun countTemplateVersionInstances(projectId: String, templateId: String, version: Long): Int {
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
        version: Long
    ): String {
        return when (referType) {
            PublicVerGroupReferenceTypeEnum.PIPELINE -> {
                String.format("$basePath/$projectId/${referId}$pipelinePath", version)
            }
            PublicVerGroupReferenceTypeEnum.TEMPLATE -> {
                String.format("$basePath/${projectId}$templatePath", referId, version)
            }
        }
    }

    /**
     * 获取资源关联的变量组变量引用信息
     * @param projectId 项目ID
     * @param referId 引用资源ID
     * @param referType 引用资源类型
     * @param referVersion 引用版本号
     * @param groupName 变量组名称
     * @param version 变量组版本号
     * @return 变量列表
     */
    fun listResourceVarReferInfo(
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referVersion: Int,
        groupName: String,
        version: Int?
    ): List<PublicVarDO> {
        logger.info("listResourceVarReferInfo for referId: $referId, referType: $referType, " +
                "referVersion: $referVersion, groupName: $groupName, version: $version")

        try {
            // 先查询引用信息，获取sourceProjectId
            val groupReferInfos = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferId(
                dslContext = dslContext,
                projectId = projectId,
                referId = referId,
                referType = referType,
                referVersion = referVersion,
                groupName = groupName
            )

            // 获取sourceProjectId，如果不为空则使用它来查询变量组信息
            val sourceProjectId = groupReferInfos.firstOrNull()?.sourceProjectId ?: projectId
            logger.info("listResourceVarReferInfo projectId:$projectId|sourceProjectId: $sourceProjectId")
            // 查询变量组信息，确认变量组存在
            val varGroupRecord = publicVarGroupDao.getRecordByGroupName(
                dslContext = dslContext,
                projectId = sourceProjectId,
                groupName = groupName,
                version = version
            ) ?: throw ErrorCodeException(
                errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_NOT_EXIST,
                params = arrayOf(groupName)
            )

            // 先查询变量组中的所有变量详细信息
            val groupVars = publicVarDao.listVarByGroupName(
                dslContext = dslContext,
                projectId = sourceProjectId,
                groupName = groupName,
                version = varGroupRecord.version
            )

            if (groupVars.isEmpty()) {
                return emptyList()
            }

            // 查询该资源在该变量组中引用的变量信息
            val varReferInfos = publicVarReferInfoDao.listVarReferInfoByReferIdAndGroup(
                dslContext = dslContext,
                projectId = projectId,
                referId = referId,
                referType = referType,
                groupName = groupName,
                referVersion = referVersion
            )

            // 统计每个变量的引用次数
            val varReferCountMap = varReferInfos.groupingBy { it.varName }.eachCount()

            // 返回所有变量，有关联就设置referCount
            return groupVars.map { varPO ->
                val buildFormProperty = JsonUtil.to(varPO.buildFormProperty, BuildFormProperty::class.java)
                buildFormProperty.varGroupVersion = version
                PublicVarDO(
                    varName = varPO.varName,
                    alias = varPO.alias,
                    type = varPO.type,
                    valueType = varPO.valueType,
                    defaultValue = varPO.defaultValue,
                    desc = varPO.desc,
                    referCount = varReferCountMap[varPO.varName] ?: 0,
                    buildFormProperty = buildFormProperty
                )
            }
        } catch (e: ErrorCodeException) {
            throw e
        } catch (e: Throwable) {
            logger.warn("Failed to list resource var refer info for referId: $referId, groupName: $groupName", e)
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_REST_EXCEPTION_COMMON_TIP,
                params = arrayOf(e.message ?: "Unknown error")
            )
        }
    }
}
