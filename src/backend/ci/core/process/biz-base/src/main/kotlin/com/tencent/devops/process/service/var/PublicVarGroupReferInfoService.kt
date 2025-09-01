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
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.metrics.api.ServiceMetricsResource
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupReferInfoDao
import com.tencent.devops.process.dao.`var`.PublicVarReferInfoDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarPipelineRefDO
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
    private val templatePipelineDao: TemplatePipelineDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarGroupReferInfoService::class.java)
    }

    fun updatePublicGroupRefer(
        userId: String,
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO
    ): Boolean {
        val referId = publicVarGroupReferInfo.referId
        val referType = publicVarGroupReferInfo.referType
        val publicVarGroupRefs = publicVarGroupReferInfo.publicVarGroupRefs
        val referVersionName = publicVarGroupReferInfo.referVersionName

        if (publicVarGroupRefs.isEmpty()) {
            val countByReferId = publicVarGroupReferInfoDao.countByPublicVarGroupRef(
                dslContext = dslContext,
                projectId = projectId,
                referId = referId,
                referType = referType,
                referVersionName = referVersionName
            )
            if (countByReferId > 0) {
                publicVarGroupReferInfoDao.deleteByReferIdExcludingGroupNames(
                    dslContext = dslContext,
                    projectId = projectId,
                    referId = referId,
                    referType = referType,
                    referVersionName = referVersionName
                )
                publicVarReferInfoDao.deleteByReferIdExcludingGroupNames(
                    dslContext = dslContext,
                    projectId = projectId,
                    referId = referId,
                    referType = referType,
                    referVersionName = referVersionName
                )
            }
            return true
        }

        val publicVarGroupReferPOs = mutableListOf<PipelinePublicVarGroupReferPO>()
        publicVarGroupRefs.forEach { ref ->
            // 检查变量组是否存在
            val groupName = ref.groupName
            // 从版本名称截取版本号，latest值默认设为空
            val version = ref.versionName?.substring(1)?.toIntOrNull()
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
            val countByPublicVarGroupRef = publicVarGroupReferInfoDao.countByPublicVarGroupRef(
                dslContext = dslContext,
                projectId = projectId,
                referId = referId,
                referType = referType,
                groupName = groupName,
                referVersionName = referVersionName
            )
            if (countByPublicVarGroupRef > 0) {
                return@forEach
            }
            publicVarGroupReferPOs.add(
                PipelinePublicVarGroupReferPO(
                    id = client.get(ServiceAllocIdResource::class)
                        .generateSegmentId("T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO").data ?: 0,
                    projectId = projectId,
                    groupName = groupName,
                    version = version,
                    referId = referId,
                    referName = publicVarGroupReferInfo.referName,
                    referVersionName= referVersionName,
                    referType = referType,
                    modifier = userId,
                    updateTime = LocalDateTime.now(),
                    creator = userId,
                    createTime = LocalDateTime.now()
                )
            )
        }

        try {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)

                // 直接删除referId、referVersionName相同但不在publicVarGroupRefs中的记录
                val newGroupNames = publicVarGroupRefs.map { it.groupName }.toSet()
                if (newGroupNames.isNotEmpty()) {
                    // 删除不在新变量组列表中的变量组引用记录
                    publicVarGroupReferInfoDao.deleteByReferIdExcludingGroupNames(
                        dslContext = context,
                        projectId = projectId,
                        referId = referId,
                        referType = referType,
                        referVersionName = referVersionName,
                        excludedGroupNames = newGroupNames.toList()
                    )
                }

                publicVarGroupReferInfoDao.batchSave(context, publicVarGroupReferPOs)
            }
            publicVarReferInfoService.addPublicVarRefer(
                userId = userId,
                projectId = projectId,
                publicVarGroupReferInfo = publicVarGroupReferInfo
            )
        } catch (t: Throwable) {
            logger.warn("Failed to add pipeline group refer for $referId", t)
            throw t
        }
        return true
    }

    fun deletePublicVerGroupRefByReferId(projectId: String, referId: String, referType: PublicVerGroupReferenceTypeEnum) {
        try {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                // 删除变量组引用记录
                publicVarGroupReferInfoDao.deleteByReferId(
                    dslContext = context,
                    projectId = projectId,
                    referId = referId,
                    referType = referType
                )
                // 删除对应的变量引用记录
                publicVarReferInfoDao.deleteByReferIdWithoutVersion(
                    dslContext = context,
                    projectId = projectId,
                    referId = referId,
                    referType = referType
                )
            }
        } catch (t: Throwable) {
            logger.warn("Failed to delete refer info for referId: $referId", t)
            throw t
        }
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
            referType = queryReq.referType!!,
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