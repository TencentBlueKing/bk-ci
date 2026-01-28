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
import com.tencent.devops.common.api.util.toLocalDateTime
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.metrics.api.ServiceMetricsResource
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_NOT_EXIST
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_QUERY_FAILED
import com.tencent.devops.process.dao.template.PipelineTemplateResourceDao
import com.tencent.devops.process.dao.`var`.PublicVarDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupReferInfoDao
import com.tencent.devops.process.dao.`var`.PublicVarReferInfoDao
import com.tencent.devops.process.dao.`var`.PublicVarVersionSummaryDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.pojo.`var`.VarGroupReferInfoQueryResult
import com.tencent.devops.process.pojo.`var`.`do`.PublicGroupVarRefDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarDO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupInfoQueryReqDTO
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarGroupReferPO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

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
    private val publicVarDao: PublicVarDao,
    private val publicVarVersionSummaryDao: PublicVarVersionSummaryDao,
    private val publicVarService: PublicVarService
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

        logger.info(
            "listVarReferInfo queryReq: $queryReq, totalCount: $totalCount, referInfosSize: ${varGroupReferInfo.size}"
        )
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
     * 查询资源实际引用的变量数量
     */
    private fun countActualVarReferences(
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referVersion: Int
    ): Int {
        return publicVarReferInfoDao.countActualVarReferencesByReferId(
            dslContext = dslContext,
            projectId = projectId,
            referId = referId,
            referType = referType,
            referVersion = referVersion
        )
    }

    /**
     * 由引用信息构建 PublicGroupVarRefDO 的入参封装
     */
    private data class PublicGroupVarRefBuildParams(
        val referInfo: ResourcePublicVarGroupReferPO,
        val projectId: String,
        val actualRefCount: Int,
        val creator: String,
        val modifier: String,
        val updateTime: LocalDateTime,
        val urlVersion: Long,
        val executeCount: Int? = null,
        val instanceCount: Int? = null
    )

    /**
     * 由引用信息构建 PublicGroupVarRefDO
     */
    private fun buildPublicGroupVarRefDO(params: PublicGroupVarRefBuildParams): PublicGroupVarRefDO {
        val referInfo = params.referInfo
        return PublicGroupVarRefDO(
            referId = referInfo.referId,
            referName = referInfo.referName,
            referUrl = getVarGroupReferUrl(
                projectId = params.projectId,
                referType = referInfo.referType,
                referId = referInfo.referId,
                version = params.urlVersion
            ),
            referType = referInfo.referType,
            creator = params.creator,
            modifier = params.modifier,
            updateTime = params.updateTime,
            actualRefCount = params.actualRefCount,
            executeCount = params.executeCount,
            instanceCount = params.instanceCount
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
            ?.takeIf { it.isNotEmpty() } ?: return emptyList()

        return runCatching {
            val pipelineExecCounts = client.get(ServiceMetricsResource::class)
                .queryPipelineMonthlyExecCountByList(
                    projectId = queryReq.projectId,
                    pipelineIdList = pipelineReferInfos.map { it.referId }.distinct()
                )
                .data ?: emptyMap()

            pipelineReferInfos.map { referInfo ->
                val actualRefCount = countActualVarReferences(
                    projectId = queryReq.projectId,
                    referId = referInfo.referId,
                    referType = PublicVerGroupReferenceTypeEnum.PIPELINE,
                    referVersion = referInfo.referVersion
                )
                buildPublicGroupVarRefDO(
                    PublicGroupVarRefBuildParams(
                        referInfo = referInfo,
                        projectId = queryReq.projectId,
                        actualRefCount = actualRefCount,
                        creator = referInfo.creator,
                        modifier = referInfo.modifier,
                        updateTime = referInfo.updateTime,
                        urlVersion = referInfo.referVersion.toLong(),
                        executeCount = pipelineExecCounts[referInfo.referId] ?: 0
                    )
                )
            }
        }.getOrElse { e ->
            logger.warn("Failed to process pipeline references for project: ${queryReq.projectId}", e)
            throw ErrorCodeException(errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_QUERY_FAILED)
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
            ?.takeIf { it.isNotEmpty() } ?: return emptyList()

        return runCatching {
            val templateKeys = templateReferInfos.map { Pair(it.referId, it.referVersion.toLong()) }.distinct()
            val templateMap = templateKeys.mapNotNull { (templateId, version) ->
                pipelineTemplateResourceDao.get(
                    dslContext = dslContext,
                    projectId = queryReq.projectId,
                    templateId = templateId,
                    version = version
                )?.let { Pair(templateId, version) to it }
            }.toMap()

            templateReferInfos.mapNotNull { referInfo ->
                val template = templateMap[Pair(referInfo.referId, referInfo.referVersion.toLong())]
                    ?: run {
                        logger.warn("Template not found: ${referInfo.referId}, version: ${referInfo.referVersion}")
                        return@mapNotNull null
                    }
                val actualRefCount = countActualVarReferences(
                    projectId = queryReq.projectId,
                    referId = referInfo.referId,
                    referType = PublicVerGroupReferenceTypeEnum.TEMPLATE,
                    referVersion = referInfo.referVersion
                )
                buildPublicGroupVarRefDO(
                    PublicGroupVarRefBuildParams(
                        referInfo = referInfo,
                        projectId = queryReq.projectId,
                        actualRefCount = actualRefCount,
                        creator = template.creator,
                        modifier = template.creator,
                        updateTime = template.updateTime?.toLocalDateTime() ?: LocalDateTime.now(),
                        urlVersion = template.version,
                        instanceCount = countTemplateVersionInstances(
                            projectId = queryReq.projectId,
                            templateId = referInfo.referId,
                            version = template.version
                        )
                    )
                )
            }
        }.getOrElse { e ->
            logger.warn("Failed to process template references for project: ${queryReq.projectId}", e)
            throw ErrorCodeException(errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_REFER_QUERY_FAILED)
        }
    }

    /**
     * 查询变量组引用信息（查询使用变量的最新版本，按 referId 去重）
     */
    fun queryVarGroupReferInfo(
        queryReq: PublicVarGroupInfoQueryReqDTO
    ): VarGroupReferInfoQueryResult {
        val projectId = queryReq.projectId
        val groupName = queryReq.groupName ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.ERROR_NEED_PARAM_,
            params = arrayOf("groupName")
        )
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

            // 统计使用变量的资源数量（按 referId 去重）
            val totalCount = publicVarGroupReferInfoDao.countLatestActiveVarGroupReferInfo(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName,
                referType = queryReq.referType
            )

            // 查询使用变量的最新版本记录
            val varGroupReferInfo = publicVarGroupReferInfoDao.listLatestActiveVarGroupReferInfo(
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
     * 根据变量名查询变量组变量引用信息（每个 referId 只返回最大版本）
     * 查询逻辑：
     * 1. 查询引用了该变量的所有 referId（使用 GROUP BY 获取每个 referId 的最大版本）
     * 2. 统计这些 referId 的最大版本数量
     * 3. 查询这些 referId 的最大版本详细信息
     * @param queryReq 查询请求
     * @param groupName 变量组名
     * @param varName 变量名
     * @return 变量组引用信息查询结果（每个 referId 只包含最大版本）
     */
    private fun queryVarGroupReferInfoByVarNameAllVersions(
        queryReq: PublicVarGroupInfoQueryReqDTO,
        groupName: String,
        varName: String
    ): VarGroupReferInfoQueryResult {
        val projectId = queryReq.projectId

        try {
            // Query referIds that reference this variable (max version for each referId)
            val referIds = publicVarReferInfoDao.listReferIdsByVarName(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName,
                varName = varName,
                referType = queryReq.referType
            )

            logger.info(
                "Query var refer info: project=$projectId, group=$groupName, var=$varName, " +
                "referType=${queryReq.referType?.name}, referIdCount=${referIds.size}"
            )

            if (referIds.isEmpty()) {
                return VarGroupReferInfoQueryResult(
                    totalCount = 0,
                    referInfos = emptyList()
                )
            }

            val totalCount = publicVarGroupReferInfoDao.countLatestActiveVarGroupReferInfoByReferIds(
                dslContext = dslContext,
                projectId = projectId,
                referIds = referIds,
                referType = queryReq.referType
            )

            // Query max version details for each referId
            val varGroupReferInfo = publicVarGroupReferInfoDao.listLatestActiveVarGroupReferInfoByReferIds(
                dslContext = dslContext,
                projectId = projectId,
                referIds = referIds,
                referType = queryReq.referType,
                page = queryReq.page,
                pageSize = queryReq.pageSize
            )

            logger.info(
                "Query result: totalCount=$totalCount, returnedCount=${varGroupReferInfo.size}"
            )

            return VarGroupReferInfoQueryResult(
                totalCount = totalCount,
                referInfos = varGroupReferInfo
            )
        } catch (e: Throwable) {
            logger.warn(
                "Failed to query var refer info: project=$projectId, group=$groupName, var=$varName",
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
     * 资源变量引用信息查询请求DTO
     */
    data class ResourceVarReferInfoQueryRequest(
        val projectId: String,
        val referId: String,
        val referType: PublicVerGroupReferenceTypeEnum,
        val referVersion: Int,
        val groupName: String,
        val version: Int?
    )

    /**
     * 获取资源关联的变量组变量引用信息
     * @param request 资源变量引用信息查询请求
     * @return 变量列表
     */
    fun listResourceVarReferInfo(request: ResourceVarReferInfoQueryRequest): List<PublicVarDO> {
        val projectId = request.projectId
        val referId = request.referId
        val referType = request.referType
        val referVersion = request.referVersion
        val groupName = request.groupName
        val version = request.version
        logger.info("listResourceVarReferInfo for referId: $referId, referType: $referType, " +
                "referVersion: $referVersion, groupName: $groupName, version: $version")

        try {
            // 查询引用信息
            val groupReferInfos = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferId(
                dslContext = dslContext,
                projectId = projectId,
                referId = referId,
                referType = referType,
                referVersion = referVersion,
                groupName = groupName
            )

            // 查询变量组信息，确认变量组存在
            val varGroupRecord = publicVarGroupDao.getRecordByGroupName(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName,
                version = version
            ) ?: throw ErrorCodeException(
                errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_NOT_EXIST,
                params = arrayOf(groupName)
            )

            // 先查询变量组中的所有变量详细信息
            val groupVars = publicVarDao.listVarByGroupName(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName,
                version = varGroupRecord.version
            )

            if (groupVars.isEmpty()) {
                return emptyList()
            }

            val referCountMap = publicVarVersionSummaryDao.batchGetTotalReferCount(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName,
                varNames = groupVars.map { it.varName }
            )
            return publicVarService.convertVarPOsToPublicVarDOs(
                varPOs = groupVars,
                referCountMap = referCountMap,
                varGroupVersion = version
            )
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
