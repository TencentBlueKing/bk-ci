/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.statistics.service.process.template

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeSvnElement
import com.tencent.devops.common.pipeline.pojo.element.agent.GithubElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.template.OptionalTemplate
import com.tencent.devops.process.pojo.template.OptionalTemplateList
import com.tencent.devops.process.pojo.template.TemplateListModel
import com.tencent.devops.process.pojo.template.TemplateModel
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.statistics.dao.process.PipelineSettingDao
import com.tencent.devops.statistics.dao.process.template.TemplateDao
import com.tencent.devops.statistics.dao.process.template.TemplatePipelineDao
import com.tencent.devops.statistics.service.process.permission.StatisticsPipelinePermissionService
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class TemplateService @Autowired constructor(
    private val dslContext: DSLContext,
    private val templateDao: TemplateDao,
    private val templatePipelineDao: TemplatePipelineDao,
    private val pipelineSettingDao: PipelineSettingDao,
    private val statisticsPipelinePermissionService: StatisticsPipelinePermissionService,
    private val client: Client,
    private val objectMapper: ObjectMapper
) {

    fun listTemplate(
        projectId: String,
        userId: String,
        templateType: TemplateType?,
        storeFlag: Boolean?,
        page: Int? = null,
        pageSize: Int? = null,
        keywords: String? = null
    ): TemplateListModel {
        logger.info("[$projectId|$userId|$templateType|$storeFlag|$page|$pageSize|$keywords] List template")
        val hasManagerPermission = hasManagerPermission(projectId, userId)
        val result = ArrayList<TemplateModel>()
        val count = templateDao.countTemplate(
            dslContext = dslContext,
            projectId = projectId,
            includePublicFlag = null,
            templateType = templateType,
            templateName = null,
            storeFlag = storeFlag
        )
        val templates = templateDao.listTemplate(
            dslContext = dslContext,
            projectId = projectId,
            includePublicFlag = null,
            templateType = templateType,
            templateIdList = null,
            storeFlag = storeFlag,
            page = page,
            pageSize = pageSize
        )
        logger.info("after get templates")
        fillResult(
            context = dslContext,
            templates = templates,
            hasManagerPermission = hasManagerPermission,
            userId = userId,
            templateType = templateType,
            storeFlag = storeFlag,
            page = page,
            pageSize = pageSize,
            keywords = keywords,
            result = result
        )
        return TemplateListModel(projectId, hasManagerPermission, result, count)
    }

    fun hasManagerPermission(projectId: String, userId: String): Boolean =
        statisticsPipelinePermissionService.isProjectUser(
            userId = userId,
            projectId = projectId,
            group = BkAuthGroup.MANAGER
        )

    fun listTemplateByProjectIds(
        projectIds: Set<String>,
        userId: String,
        templateType: TemplateType?,
        storeFlag: Boolean?,
        page: Int?,
        pageSize: Int?,
        keywords: String? = null
    ): Page<TemplateModel> {
        val projectIdsStr = projectIds.fold("") { s1, s2 -> "$s1:$s2" }
        logger.info(
            "listTemplateByProjectIds|$projectIdsStr,$userId,$templateType,$storeFlag,$page,$pageSize,$keywords"
        )
        var totalCount = 0
        val templates = ArrayList<TemplateModel>()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            totalCount = templateDao.countTemplateByProjectIds(
                dslContext = context,
                projectIds = projectIds,
                includePublicFlag = null,
                templateType = templateType,
                templateName = null,
                storeFlag = storeFlag
            )
            val templateRecords = templateDao.listTemplateByProjectIds(
                dslContext = context,
                projectIds = projectIds,
                includePublicFlag = null,
                templateType = templateType,
                templateIdList = null,
                storeFlag = storeFlag,
                page = page,
                pageSize = pageSize
            )
            // 接口用做统计，操作者是否有单个模板管理权限无意义，hasManagerPermission统一为false
            fillResult(
                context = context,
                templates = templateRecords,
                hasManagerPermission = false,
                userId = userId,
                templateType = templateType,
                storeFlag = storeFlag,
                page = page,
                pageSize = pageSize,
                keywords = keywords,
                result = templates
            )
        }
        return Page(
            page = PageUtil.getValidPage(page),
            pageSize = PageUtil.getValidPageSize(pageSize),
            count = totalCount.toLong(),
            records = templates
        )
    }

    /**
     * 从listTemplate与listTemplateByProjectIds中抽取出的公共方法
     * 填充基础模板的其他附加信息
     */
    @Suppress("NestedBlockDepth")
    fun fillResult(
        context: DSLContext,
        templates: Result<out Record>?,
        hasManagerPermission: Boolean,
        userId: String,
        templateType: TemplateType?,
        storeFlag: Boolean?,
        page: Int?,
        pageSize: Int?,
        keywords: String? = null,
        result: ArrayList<TemplateModel>
    ) {
        if (templates == null || templates.isEmpty()) {
            // 如果查询模板列表为空，则不再执行后续逻辑
            return
        }
        val templateIdList = mutableSetOf<String>()
        val srcTemplates = getConstrainedSrcTemplates(templates, templateIdList, context)

        val settings = pipelineSettingDao.getSettings(context, templateIdList).map { it.pipelineId to it }.toMap()
        templates.forEach { record ->
            val templateId = record["templateId"] as String
            val type = record["templateType"] as String

            val templateRecord = if (type == TemplateType.CONSTRAINT.name) {
                val srcTemplateId = record["srcTemplateId"] as String
                srcTemplates?.get(srcTemplateId)
            } else {
                record
            }

            if (templateRecord == null) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS,
                    defaultMessage = "模板不存在"
                )
            } else {
                val modelStr = templateRecord["template"] as String
                val version = templateRecord["version"] as Long
                val createdTime = templateRecord["createdTime"] as LocalDateTime
                val model: Model = objectMapper.readValue(modelStr)

                val setting = settings[templateId]
                val templateName = setting?.name ?: model.name

                // 根据keywords搜索过滤
                if (!keywords.isNullOrBlank() && !templateName.contains(keywords!!)) return@forEach

                val associateCodes = listAssociateCodes(record["projectId"] as String, model)
                val associatePipeline =
                    templatePipelineDao.listPipeline(
                        context,
                        PipelineInstanceTypeEnum.CONSTRAINT.type,
                        setOf(templateId)
                    )

                val pipelineIds = associatePipeline.map { PipelineId(it.pipelineId) }

                var hasInstances2Upgrade = false

                run lit@{
                    associatePipeline.forEach {
                        if (it.createdTime < createdTime) {
                            logger.info("The pipeline ${it.pipelineId} need to upgrade from ${it.version} to $version")
                            hasInstances2Upgrade = true
                            return@lit
                        }
                    }
                }

                val logoUrl = record["logoUrl"] as? String
                result.add(
                    TemplateModel(
                        name = templateName,
                        templateId = templateId,
                        version = version,
                        versionName = templateRecord["versionName"] as String,
                        templateType = type,
                        templateTypeDesc = TemplateType.getTemplateTypeDesc(type),
                        logoUrl = logoUrl ?: "",
                        storeFlag = record["storeFlag"] as Boolean,
                        associateCodes = associateCodes,
                        associatePipelines = pipelineIds,
                        hasInstance2Upgrade = hasInstances2Upgrade,
                        hasPermission = hasManagerPermission
                    )
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun listAllTemplate(
        projectId: String?,
        templateType: TemplateType?,
        templateIds: Collection<String>?,
        page: Int? = null,
        pageSize: Int? = null
    ): OptionalTemplateList {
        logger.info("[$projectId|$templateType|$page|$pageSize] List template")
        val result = mutableMapOf<String, OptionalTemplate>()
        val templateCount = templateDao.countTemplate(dslContext, projectId, true, templateType, null, null)
        val templates = templateDao.listTemplate(
            dslContext = dslContext,
            projectId = projectId,
            includePublicFlag = true,
            templateType = templateType,
            templateIdList = templateIds,
            storeFlag = null,
            page = page,
            pageSize = pageSize
        )
        if (templates == null || templates.isEmpty()) {
            // 如果查询模板列表为空，则不再执行后续逻辑
            return OptionalTemplateList(
                count = templateCount,
                page = page,
                pageSize = pageSize,
                templates = result
            )
        }
        val templateIdList = mutableSetOf<String>()
        val srcTemplates = getConstrainedSrcTemplates(templates, templateIdList, dslContext)

        val settings = pipelineSettingDao.getSettings(dslContext, templateIdList).map { it.pipelineId to it }.toMap()
        templates.forEach { record ->
            val templateId = record["templateId"] as String
            val type = record["templateType"] as String

            val templateRecord = if (type == TemplateType.CONSTRAINT.name) {
                val srcTemplateId = record["srcTemplateId"] as String
                srcTemplates?.get(srcTemplateId)
            } else {
                record
            }

            if (templateRecord != null) {
                val modelStr = templateRecord["template"] as String
                val version = templateRecord["version"] as Long

                val model: Model = objectMapper.readValue(modelStr)
                val setting = settings[templateId]
                val logoUrl = record["logoUrl"] as? String
                val categoryStr = record["category"] as? String
                val key = if (type == TemplateType.CONSTRAINT.name) record["srcTemplateId"] as String else templateId
                result[key] = OptionalTemplate(
                    name = setting?.name ?: model.name,
                    templateId = templateId,
                    projectId = templateRecord["projectId"] as String,
                    version = version,
                    versionName = templateRecord["versionName"] as String,
                    templateType = type,
                    templateTypeDesc = TemplateType.getTemplateTypeDesc(type),
                    logoUrl = logoUrl ?: "",
                    category = if (!categoryStr.isNullOrBlank()) JsonUtil.getObjectMapper()
                        .readValue(categoryStr, List::class.java) as List<String> else listOf(),
                    stages = model.stages
                )
            }
        }

        return OptionalTemplateList(
            count = templateCount,
            page = page,
            pageSize = pageSize,
            templates = result
        )
    }

    private fun getConstrainedSrcTemplates(
        templates: Result<out Record>?,
        templateIdList: MutableSet<String>,
        context: DSLContext
    ): Map<String, Record>? {
        val constrainedTemplateList = mutableListOf<String>()
        templates?.forEach { template ->
            if ((template["templateType"] as String) == TemplateType.CONSTRAINT.name) {
                constrainedTemplateList.add(template["srcTemplateId"] as String)
            }
            templateIdList.add(template["templateId"] as String)
        }
        val srcTemplateRecords = if (constrainedTemplateList.isNotEmpty()) templateDao.listTemplate(
            dslContext = context,
            projectId = null,
            includePublicFlag = null,
            templateType = null,
            templateIdList = constrainedTemplateList,
            storeFlag = null,
            page = null,
            pageSize = null
        ) else null
        return srcTemplateRecords?.associateBy { it["templateId"] as String }
    }

    /**
     * 列举这个模板关联的代码库
     */
    @Suppress("NestedBlockDepth", "ComplexMethod")
    private fun listAssociateCodes(projectId: String, model: Model): List<String> {
        val codes = ArrayList<String>()
        model.stages.forEach { stage ->
            stage.containers.forEach { container ->
                container.elements.forEach element@{ element ->
                    when (element) {
                        is CodeGitElement -> codes.add(
                            getCode(
                                projectId = projectId,
                                repositoryConfig = RepositoryConfigUtils.buildConfig(element)
                            ) ?: return@element
                        )
                        is GithubElement -> codes.add(
                            getCode(
                                projectId = projectId,
                                repositoryConfig = RepositoryConfigUtils.buildConfig(element)
                            ) ?: return@element
                        )
                        is CodeSvnElement -> codes.add(
                            getCode(projectId, RepositoryConfigUtils.buildConfig(element)) ?: return@element
                        )
                        is CodeGitWebHookTriggerElement -> codes.add(
                            getCode(projectId, RepositoryConfigUtils.buildConfig(element)) ?: return@element
                        )
                        is CodeGithubWebHookTriggerElement -> codes.add(
                            getCode(projectId, RepositoryConfigUtils.buildConfig(element)) ?: return@element
                        )
                        is CodeSVNWebHookTriggerElement -> codes.add(
                            getCode(projectId, RepositoryConfigUtils.buildConfig(element)) ?: return@element
                        )
                    }
                }
            }
        }
        return codes
    }

    private fun getCode(projectId: String, repositoryConfig: RepositoryConfig): String? {
        try {
            if (repositoryConfig.getRepositoryId().isBlank()) {
                return null
            }

            when (repositoryConfig.repositoryType) {
                RepositoryType.ID -> {
                    val repositoryId = repositoryConfig.getURLEncodeRepositoryId()
                    logger.info("Start to get the repository $repositoryId of project $projectId")
                    // use repository id to get the code
                    val result = client.get(ServiceRepositoryResource::class)
                        .get(projectId, repositoryId, repositoryConfig.repositoryType)
                    if (result.isNotOk()) {
                        logger.warn("Fail to get the repository|$repositoryId|$projectId|${result.message}")
                        return null
                    }
                    return result.data!!.url
                }
                RepositoryType.NAME -> {
                    // 仓库名是以变量来替换的
                    val repositoryName = repositoryConfig.getURLEncodeRepositoryId()
                    if (repositoryName.trim().startsWith("\${")) {
                        return repositoryName
                    }
                    val result = client.get(ServiceRepositoryResource::class)
                        .get(projectId, repositoryName, repositoryConfig.repositoryType)
                    if (result.isNotOk()) {
                        logger.warn("Fail to get the repository|$repositoryName|$projectId|${result.message}")
                        return null
                    }
                    return result.data!!.url
                }
            }
        } catch (t: Throwable) {
            logger.warn("Fail to get the code [$projectId|$repositoryConfig]", t)
        }

        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TemplateService::class.java)
    }
}
