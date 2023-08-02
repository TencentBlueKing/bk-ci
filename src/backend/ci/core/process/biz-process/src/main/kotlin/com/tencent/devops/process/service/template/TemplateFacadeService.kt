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

package com.tencent.devops.process.service.template

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.KEY_UPDATED_TIME
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.constant.KEY_VERSION_NAME
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeSvnElement
import com.tencent.devops.common.pipeline.pojo.element.agent.GithubElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.process.tables.TTemplate
import com.tencent.devops.model.process.tables.records.TPipelineSettingRecord
import com.tencent.devops.model.process.tables.records.TTemplateInstanceItemRecord
import com.tencent.devops.model.process.tables.records.TTemplateRecord
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.engine.cfg.ModelContainerIdGenerator
import com.tencent.devops.process.engine.cfg.ModelTaskIdGenerator
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.compatibility.BuildPropertyCompatibilityTools
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineResDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.dao.template.TemplateInstanceBaseDao
import com.tencent.devops.process.engine.dao.template.TemplateInstanceItemDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.engine.service.PipelineInfoExtService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.utils.PipelineUtils
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.PipelineTemplateInfo
import com.tencent.devops.process.pojo.enums.TemplateSortTypeEnum
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.template.AddMarketTemplateRequest
import com.tencent.devops.process.pojo.template.CopyTemplateReq
import com.tencent.devops.process.pojo.template.OptionalTemplate
import com.tencent.devops.process.pojo.template.OptionalTemplateList
import com.tencent.devops.process.pojo.template.SaveAsTemplateReq
import com.tencent.devops.process.pojo.template.TemplateCompareModel
import com.tencent.devops.process.pojo.template.TemplateCompareModelResult
import com.tencent.devops.process.pojo.template.TemplateInstanceBaseStatus
import com.tencent.devops.process.pojo.template.TemplateInstanceCreate
import com.tencent.devops.process.pojo.template.TemplateInstanceItemStatus
import com.tencent.devops.process.pojo.template.TemplateInstancePage
import com.tencent.devops.process.pojo.template.TemplateInstanceParams
import com.tencent.devops.process.pojo.template.TemplateInstanceUpdate
import com.tencent.devops.process.pojo.template.TemplateListModel
import com.tencent.devops.process.pojo.template.TemplateModel
import com.tencent.devops.process.pojo.template.TemplateModelDetail
import com.tencent.devops.process.pojo.template.TemplateOperationMessage
import com.tencent.devops.process.pojo.template.TemplateOperationRet
import com.tencent.devops.process.pojo.template.TemplatePipeline
import com.tencent.devops.process.pojo.template.TemplatePipelineStatus
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.TemplateVersion
import com.tencent.devops.process.service.ParamFacadeService
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.process.service.PipelineRemoteAuthService
import com.tencent.devops.process.service.StageTagService
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.util.TempNotifyTemplateUtils
import com.tencent.devops.process.utils.KEY_PIPELINE_ID
import com.tencent.devops.process.utils.KEY_TEMPLATE_ID
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.store.api.common.ServiceStoreResource
import com.tencent.devops.store.api.template.ServiceTemplateResource
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.text.MessageFormat
import java.time.LocalDateTime
import javax.ws.rs.NotFoundException
import javax.ws.rs.core.Response
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

@Suppress("ALL")
@Service
@RefreshScope
class TemplateFacadeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val templateDao: TemplateDao,
    private val templatePipelineDao: TemplatePipelineDao,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineRemoteAuthService: PipelineRemoteAuthService,
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val stageTagService: StageTagService,
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val pipelineResDao: PipelineResDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val templateInstanceBaseDao: TemplateInstanceBaseDao,
    private val templateInstanceItemDao: TemplateInstanceItemDao,
    private val pipelineGroupService: PipelineGroupService,
    private val modelTaskIdGenerator: ModelTaskIdGenerator,
    private val modelContainerIdGenerator: ModelContainerIdGenerator,
    private val paramService: ParamFacadeService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val modelCheckPlugin: ModelCheckPlugin,
    private val pipelineInfoExtService: PipelineInfoExtService
) {

    @Value("\${template.maxSyncInstanceNum:10}")
    private val maxSyncInstanceNum: Int = 10

    @Value("\${template.maxSaveVersionNum:300}")
    private val maxSaveVersionNum: Int = 300

    @Value("\${template.maxUpdateInstanceNum:100}")
    private val maxUpdateInstanceNum: Int = 100

    @Value("\${template.maxSaveVersionRecordNum:2}")
    private val maxSaveVersionRecordNum: Int = 2

    @Value("\${template.instanceListUrl}")
    private val instanceListUrl: String = ""

    @Value("\${template.maxErrorReasonLength:200}")
    private val maxErrorReasonLength: Int = 200

    fun createTemplate(projectId: String, userId: String, template: Model): String {
        logger.info("Start to create the template ${template.name} by user $userId")
        checkPermission(projectId, userId)
        checkTemplate(template, projectId)
        val templateId = UUIDUtil.generate()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            checkTemplateName(context, template.name, projectId, templateId)
            updateModelParam(template)
            val version = templateDao.create(
                dslContext = context,
                projectId = projectId,
                templateId = templateId,
                templateName = template.name,
                versionName = INIT_TEMPLATE_NAME,
                userId = userId,
                template = JsonUtil.toJson(template, formatted = false),
                storeFlag = false,
                version = client.get(ServiceAllocIdResource::class).generateSegmentId(TEMPLATE_BIZ_TAG_NAME).data
            )

            insertTemplateSetting(
                context = context,
                projectId = projectId,
                templateId = templateId,
                pipelineName = template.name,
                isTemplate = true
            )
            logger.info("Get the template version $version")
        }

        return templateId
    }

    fun copyTemplate(
        userId: String,
        projectId: String,
        srcTemplateId: String,
        copyTemplateReq: CopyTemplateReq
    ): String {
        logger.info("Start to copy the template, $srcTemplateId | $userId | $copyTemplateReq")

        checkPermission(projectId, userId)

        var latestTemplate = templateDao.getLatestTemplate(dslContext, projectId, srcTemplateId)
        val template = latestTemplate
        if (latestTemplate.type == TemplateType.CONSTRAINT.name) {
            latestTemplate = templateDao.getLatestTemplate(dslContext, latestTemplate.srcTemplateId)
        }
        val newTemplateId = UUIDUtil.generate()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            checkTemplateName(context, copyTemplateReq.templateName, projectId, newTemplateId)
            val version = templateDao.createTemplate(
                dslContext = context,
                projectId = projectId,
                templateId = newTemplateId,
                templateName = copyTemplateReq.templateName,
                versionName = INIT_TEMPLATE_NAME,
                userId = userId,
                template = latestTemplate.template,
                type = TemplateType.CUSTOMIZE.name,
                category = template.category,
                logoUrl = template.logoUrl,
                srcTemplateId = srcTemplateId,
                storeFlag = false,
                weight = 0,
                version = client.get(ServiceAllocIdResource::class).generateSegmentId(TEMPLATE_BIZ_TAG_NAME).data
            )

            if (copyTemplateReq.isCopySetting) {
                val setting = copySetting(
                    getTemplateSetting(projectId, userId, srcTemplateId),
                    newTemplateId,
                    copyTemplateReq.templateName
                )
                saveTemplatePipelineSetting(userId, setting, true)
            } else {
                insertTemplateSetting(
                    context = context,
                    projectId = projectId,
                    templateId = newTemplateId,
                    isTemplate = true,
                    pipelineName = copyTemplateReq.templateName
                )
            }

            logger.info("Get the template version $version")
        }

        return newTemplateId
    }

    /**
     * 流水线另存为模版
     */
    fun saveAsTemplate(
        userId: String,
        projectId: String,
        saveAsTemplateReq: SaveAsTemplateReq
    ): String {
        logger.info("Start to saveAsTemplate, $userId | $projectId | $saveAsTemplateReq")

        checkPermission(projectId, userId)

        val template = pipelineResDao.getLatestVersionModelString(dslContext, projectId, saveAsTemplateReq.pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
            )

        val templateId = UUIDUtil.generate()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            checkTemplateName(context, saveAsTemplateReq.templateName, projectId, templateId)
            val version = templateDao.create(
                dslContext = context,
                projectId = projectId,
                templateId = templateId,
                templateName = saveAsTemplateReq.templateName,
                versionName = INIT_TEMPLATE_NAME,
                userId = userId,
                template = template,
                storeFlag = false,
                version = client.get(ServiceAllocIdResource::class).generateSegmentId(TEMPLATE_BIZ_TAG_NAME).data
            )

            if (saveAsTemplateReq.isCopySetting) {
                val setting = copySetting(
                    setting = getTemplateSetting(
                        projectId = projectId,
                        userId = userId,
                        templateId = saveAsTemplateReq.pipelineId
                    ),
                    pipelineId = templateId,
                    templateName = saveAsTemplateReq.templateName
                )
                saveTemplatePipelineSetting(userId, setting, true)
            } else {
                insertTemplateSetting(
                    context = context,
                    projectId = projectId,
                    templateId = templateId,
                    pipelineName = saveAsTemplateReq.templateName,
                    isTemplate = true
                )
            }

            logger.info("Get the template version $version")
        }

        return templateId
    }

    fun deleteTemplate(projectId: String, userId: String, templateId: String): Boolean {
        logger.info("Start to delete the template $templateId by user $userId")
        checkPermission(projectId, userId)
        val template = templateDao.getLatestTemplate(dslContext, templateId)
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val instanceSize = templatePipelineDao.countByVersionFeat(
                dslContext = context,
                projectId = projectId,
                templateId = templateId,
                instanceType = PipelineInstanceTypeEnum.CONSTRAINT.type
            )
            if (instanceSize > 0) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.TEMPLATE_CAN_NOT_DELETE_WHEN_HAVE_INSTANCE
                )
            }
            if (template.type == TemplateType.CUSTOMIZE.name && template.storeFlag == true) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.TEMPLATE_CAN_NOT_DELETE_WHEN_PUBLISH
                )
            }
            if (template.type == TemplateType.CUSTOMIZE.name &&
                templateDao.isExistInstalledTemplate(context, templateId)
            ) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.TEMPLATE_CAN_NOT_DELETE_WHEN_INSTALL
                )
            }
            templatePipelineDao.deleteByTemplateId(context, projectId, templateId)
            templateDao.delete(context, projectId, templateId)
            pipelineSettingDao.delete(context, projectId, templateId)
            if (template.type == TemplateType.CONSTRAINT.name) {
                client.get(ServiceStoreResource::class).uninstall(
                    storeCode = template.srcTemplateId,
                    storeType = StoreTypeEnum.TEMPLATE,
                    projectCode = template.projectId
                )
            }
        }
        return true
    }

    fun deleteTemplate(projectId: String, userId: String, templateId: String, version: Long): Boolean {
        logger.info("Start to delete the template [$projectId|$userId|$templateId|$version]")
        checkPermission(projectId, userId)
        return dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            val instanceSize =
                templatePipelineDao.countByVersionFeat(
                    dslContext = context,
                    projectId = projectId,
                    templateId = templateId,
                    instanceType = PipelineInstanceTypeEnum.CONSTRAINT.type,
                    version = version
                )
            if (instanceSize > 0) {
                logger.warn("There are $instanceSize pipeline attach to $templateId of version $version")
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.TEMPLATE_CAN_NOT_DELETE_WHEN_HAVE_INSTANCE
                )
            }
            templatePipelineDao.deleteByVersion(
                dslContext = dslContext,
                projectId = projectId,
                templateId = templateId,
                version = version
            )
            templateDao.delete(dslContext, projectId, templateId, setOf(version)) == 1
        }
    }

    fun deleteTemplate(projectId: String, userId: String, templateId: String, versionName: String): Boolean {
        logger.info("Start to delete the template [$projectId|$userId|$templateId|$versionName]")
        checkPermission(projectId, userId)
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val instanceSize =
                templatePipelineDao.countByVersionFeat(
                    dslContext = context,
                    projectId = projectId,
                    templateId = templateId,
                    instanceType = PipelineInstanceTypeEnum.CONSTRAINT.type,
                    versionName = versionName
                )
            if (instanceSize > 0) {
                logger.warn("There are $instanceSize pipeline attach to $templateId of versionName $versionName")
                throw ErrorCodeException(errorCode = ProcessMessageCode.TEMPLATE_CAN_NOT_DELETE_WHEN_HAVE_INSTANCE)
            }
            templatePipelineDao.deleteByVersionName(
                dslContext = dslContext,
                projectId = projectId,
                templateId = templateId,
                versionName = versionName
            )
            templateDao.delete(
                dslContext = dslContext,
                projectId = projectId,
                templateId = templateId,
                versionName = versionName
            )
        }
        return true
    }

    fun updateTemplate(
        projectId: String,
        userId: String,
        templateId: String,
        versionName: String,
        template: Model
    ): Long {
        logger.info("Start to update the template $templateId by user $userId - ($template)")
        checkPermission(projectId, userId)
        checkTemplate(template, projectId)
        val latestTemplate = templateDao.getLatestTemplate(dslContext, projectId, templateId)
        if (latestTemplate.type == TemplateType.CONSTRAINT.name && latestTemplate.storeFlag == true) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_NOT_UPDATE
            )
        }
        var version: Long = 0
        checkTemplateName(dslContext, template.name, projectId, templateId)
        updateModelParam(template)
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            pipelineSettingDao.updateSetting(
                dslContext = context,
                projectId = projectId,
                pipelineId = templateId,
                name = template.name,
                desc = template.desc ?: ""
            )
            val saveRecordVersions = templateDao.listSaveRecordVersions(
                dslContext = context,
                projectId = projectId,
                templateId = templateId,
                versionName = versionName,
                saveNum = maxSaveVersionRecordNum
            )
            if (saveRecordVersions?.isNotEmpty == true) {
                // 版本名称为versionName的版本只保存最近maxSaveVersionRecordNum条记录
                templateDao.deleteSpecVersion(
                    dslContext = context,
                    projectId = projectId,
                    templateId = templateId,
                    versionName = versionName,
                    saveVersions = saveRecordVersions.map { it.value1() }
                )
            }
            version = templateDao.createTemplate(
                dslContext = context,
                projectId = projectId,
                templateId = templateId,
                templateName = template.name,
                versionName = versionName,
                userId = userId,
                template = JsonUtil.toJson(template, formatted = false),
                type = latestTemplate.type,
                category = latestTemplate.category,
                logoUrl = latestTemplate.logoUrl,
                srcTemplateId = latestTemplate.srcTemplateId,
                storeFlag = latestTemplate.storeFlag,
                weight = latestTemplate.weight,
                version = client.get(ServiceAllocIdResource::class).generateSegmentId(TEMPLATE_BIZ_TAG_NAME).data
            )
            logger.info("Get the update template version $version")
        }

        return version
    }

    fun updateTemplateSetting(
        projectId: String,
        userId: String,
        templateId: String,
        setting: PipelineSetting
    ): Boolean {
        logger.info("Start to update the template setting - [$projectId|$userId|$templateId]")
        checkPermission(projectId, userId)
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            checkTemplateName(
                dslContext = context,
                name = setting.pipelineName,
                projectId = projectId,
                templateId = templateId
            )
            saveTemplatePipelineSetting(userId, setting, true)
        }
        return true
    }

    fun getTemplateSetting(projectId: String, userId: String, templateId: String): PipelineSetting {

        val setting = pipelineRepositoryService.getSetting(projectId, templateId)
        if (setting == null) {
            logger.warn("Fail to get the template setting - [$projectId|$userId|$templateId]")
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.PIPELINE_SETTING_NOT_EXISTS
            )
        }
        val hasPermission = hasManagerPermission(projectId, userId)
        val groups = pipelineGroupService.getGroups(userId, projectId, templateId)
        val labels = ArrayList<String>()
        groups.forEach {
            labels.addAll(it.labels)
        }
        setting.labels = labels
        setting.hasPermission = hasPermission
        return setting
    }

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
            pageSize = pageSize,
            queryModelFlag = true
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
            result = result,
            projectId = projectId
        )
        return TemplateListModel(projectId, hasManagerPermission, result, count)
    }

    fun getSrcTemplateCodes(projectId: String): com.tencent.devops.common.api.pojo.Result<List<String>> {
        return com.tencent.devops.common.api.pojo.Result(templateDao.getSrcTemplateCodes(dslContext, projectId))
    }

    /**
     * 从listTemplate与listTemplateByProjectIds中抽取出的公共方法
     * 填充基础模板的其他附加信息
     */
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
        result: ArrayList<TemplateModel>,
        projectId: String? = null
    ) {
        if (templates == null || templates.isEmpty()) {
            // 如果查询模板列表为空，则不再执行后续逻辑
            return
        }
        val templateIdList = mutableSetOf<String>()
        val srcTemplates = getConstrainedSrcTemplates(context, templates, templateIdList)

        val settings = pipelineSettingDao.getSettings(
            dslContext = context,
            pipelineIds = templateIdList,
            projectId = projectId
        ).map { it.pipelineId to it }.toMap()
        templates.forEach { record ->
            val tTemplate = TTemplate.T_TEMPLATE
            val templateId = record[tTemplate.ID]
            val type = record[tTemplate.TYPE]

            val templateRecord = if (type == TemplateType.CONSTRAINT.name) {
                val srcTemplateId = record[tTemplate.SRC_TEMPLATE_ID]
                srcTemplates?.get(srcTemplateId)
            } else {
                record
            }

            if (templateRecord == null) {
                throw ErrorCodeException(
                    errorCode = ERROR_TEMPLATE_NOT_EXISTS
                )
            } else {
                val modelStr = templateRecord[tTemplate.TEMPLATE] as String
                val version = templateRecord[tTemplate.VERSION] as Long
                val model: Model = objectMapper.readValue(modelStr)

                val setting = settings[templateId]
                val templateName = setting?.name ?: model.name

                // 根据keywords搜索过滤
                if (!keywords.isNullOrBlank() && !templateName.contains(keywords)) return@forEach
                val templateProjectId = record[tTemplate.PROJECT_ID] as String
                val associateCodes = listAssociateCodes(templateProjectId, model)
                val associatePipeline =
                    templatePipelineDao.listPipeline(
                        dslContext = context,
                        projectId = templateProjectId,
                        instanceType = PipelineInstanceTypeEnum.CONSTRAINT.type,
                        templateIds = setOf(templateId),
                        deleteFlag = false
                    )

                val pipelineIds = associatePipeline.map { PipelineId(it[KEY_PIPELINE_ID] as String) }

                var hasInstances2Upgrade = false

                run lit@{
                    associatePipeline.forEach {
                        val templatePipelineVersion = it[KEY_VERSION] as Long
                        val templatePipelineId = it[KEY_PIPELINE_ID] as String
                        if (templatePipelineVersion != version) {
                            logger.info(
                                "The pipeline $templatePipelineId need to upgrade " +
                                    "from $templatePipelineVersion to $version"
                            )
                            hasInstances2Upgrade = true
                            return@lit
                        }
                    }
                }

                val logoUrl = record[tTemplate.LOGO_URL]
                result.add(
                    TemplateModel(
                        name = templateName,
                        templateId = templateId,
                        version = version,
                        versionName = templateRecord[tTemplate.VERSION_NAME],
                        templateType = type,
                        templateTypeDesc = TemplateType.getTemplateTypeDesc(type),
                        logoUrl = logoUrl ?: "",
                        storeFlag = record[tTemplate.STORE_FLAG],
                        associateCodes = associateCodes,
                        associatePipelines = pipelineIds,
                        hasInstance2Upgrade = hasInstances2Upgrade,
                        hasPermission = hasManagerPermission
                    )
                )
            }
        }
    }

    private fun getConstrainedSrcTemplates(
        context: DSLContext,
        templates: Result<out Record>?,
        templateIdList: MutableSet<String>,
        queryModelFlag: Boolean = true
    ): Map<String, Record>? {
        val constrainedTemplateList = mutableListOf<String>()
        val tTemplate = TTemplate.T_TEMPLATE
        templates?.forEach { template ->
            if (template[tTemplate.TYPE] == TemplateType.CONSTRAINT.name) {
                constrainedTemplateList.add(template[tTemplate.SRC_TEMPLATE_ID])
            }
            templateIdList.add(template[tTemplate.ID])
        }
        val srcTemplateRecords = if (constrainedTemplateList.isNotEmpty()) templateDao.listTemplate(
            dslContext = context,
            projectId = null,
            includePublicFlag = null,
            templateType = null,
            templateIdList = constrainedTemplateList,
            storeFlag = null,
            page = null,
            pageSize = null,
            queryModelFlag = queryModelFlag
        ) else null
        return srcTemplateRecords?.associateBy { it[tTemplate.ID] }
    }

    /**
     * 列举这个模板关联的代码库
     */
    private fun listAssociateCodes(projectId: String, model: Model): List<String> {
        val codes = ArrayList<String>()
        model.stages.forEach { stage ->
            stage.containers.forEach { container ->
                container.elements.forEach element@{ element ->
                    when (element) {
                        is CodeGitElement -> codes.add(
                            getCode(projectId, repositoryConfig = RepositoryConfigUtils.buildConfig(element))
                                ?: return@element
                        )
                        is GithubElement -> codes.add(
                            getCode(projectId, repositoryConfig = RepositoryConfigUtils.buildConfig(element))
                                ?: return@element
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
                        logger.warn("getCodeRepositoryId|$repositoryId|$projectId|${result.message}")
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
                        logger.warn("getCodeRepositoryName|$repositoryName|$projectId|${result.message}")
                        return null
                    }
                    return result.data!!.url
                }
            }
        } catch (ignored: Throwable) {
            logger.warn("Fail to get the code [$projectId|$repositoryConfig]", ignored)
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    fun listAllTemplate(
        projectId: String?,
        templateType: TemplateType?,
        templateIds: Collection<String>?,
        page: Int? = null,
        pageSize: Int? = null
    ): OptionalTemplateList {
        logger.info("[$projectId|$templateType|$templateIds|$page|$pageSize] List template")
        val result = mutableMapOf<String, OptionalTemplate>()
        val templateCount = templateDao.countTemplate(
            dslContext = dslContext,
            projectId = projectId,
            includePublicFlag = true,
            templateType = templateType,
            templateName = null,
            storeFlag = null
        )
        val templates = templateDao.listTemplate(
            dslContext = dslContext,
            projectId = projectId,
            includePublicFlag = true,
            templateType = templateType,
            templateIdList = templateIds,
            storeFlag = null,
            page = page,
            pageSize = pageSize,
            queryModelFlag = true
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
        val srcTemplates = getConstrainedSrcTemplates(
            context = dslContext,
            templates = templates,
            templateIdList = templateIdList,
            queryModelFlag = true
        )

        val settings = pipelineSettingDao.getSettings(
            dslContext = dslContext,
            pipelineIds = templateIdList,
            projectId = projectId
        ).map { it.pipelineId to it }.toMap()
        val tTemplate = TTemplate.T_TEMPLATE
        templates.forEach { record ->
            val templateId = record[tTemplate.ID]
            val type = record[tTemplate.TYPE]
            val srcTemplateId = record[tTemplate.SRC_TEMPLATE_ID]
            val templateRecord = if (type == TemplateType.CONSTRAINT.name) {
                srcTemplates?.get(srcTemplateId)
            } else {
                record
            }

            if (templateRecord != null) {
                val modelStr = templateRecord[tTemplate.TEMPLATE]
                val version = templateRecord[tTemplate.VERSION] as Long

                val model: Model = objectMapper.readValue(modelStr)
                val setting = settings[templateId]
                val logoUrl = record[tTemplate.LOGO_URL]
                val categoryStr = record[tTemplate.CATEGORY]
                val key = if (type == TemplateType.CONSTRAINT.name) srcTemplateId else templateId
                result[key] = OptionalTemplate(
                    name = setting?.name ?: model.name,
                    templateId = templateId,
                    projectId = templateRecord[tTemplate.PROJECT_ID],
                    version = version,
                    versionName = templateRecord[tTemplate.VERSION_NAME],
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

    fun getTemplate(
        projectId: String,
        userId: String,
        templateId: String,
        version: Long?,
        versionName: String? = null
    ): TemplateModelDetail {
        var latestTemplate = templateDao.getLatestTemplate(dslContext, projectId, templateId)
        val isConstrainedFlag = latestTemplate.type == TemplateType.CONSTRAINT.name

        if (isConstrainedFlag) {
            try {
                latestTemplate = templateDao.getLatestTemplate(dslContext, latestTemplate.srcTemplateId)
            } catch (ignored: NotFoundException) {
                logger.warn("The src template ${latestTemplate.srcTemplateId} is not exist")
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_SOURCE_TEMPLATE_NOT_EXISTS
                )
            }
        }

        val setting = pipelineSettingDao.getSetting(dslContext, projectId, templateId)
        if (setting == null) {
            logger.warn("The template setting is not exist [$projectId|$userId|$templateId]")
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.PIPELINE_SETTING_NOT_EXISTS
            )
        }
        val template = if (version == null && versionName.isNullOrBlank()) {
            latestTemplate
        } else {
            if (version == null) {
                templateDao.getTemplate(
                    dslContext = dslContext,
                    templateId = templateId,
                    versionName = versionName
                )
            } else {
                templateDao.getTemplate(dslContext = dslContext, version = version)
            }
        }
        val currentVersion = TemplateVersion(
            template.version,
            template.versionName,
            template.updateTime.timestampmilli(),
            template.creator
        )
        val model: Model = objectMapper.readValue(template.template)
        model.name = setting.name
        model.desc = setting.desc
        val groups = pipelineGroupService.getGroups(userId, projectId, templateId)
        val labels = ArrayList<String>()
        groups.forEach {
            labels.addAll(it.labels)
        }
        model.labels = labels
        val templateResult = instanceParamModel(userId, projectId, model)
        if (!latestTemplate.storeFlag || latestTemplate.srcTemplateId.isNullOrBlank()) {
            try {
                checkTemplate(templateResult, projectId)
            } catch (ignored: ErrorCodeException) {
                // 兼容历史数据，模板内容有问题给出错误提示
                val message = MessageUtil.getMessageByLocale(
                    messageCode = ignored.errorCode,
                    params = ignored.params,
                    language = I18nUtil.getLanguage(userId)
                )
                templateResult.tips = message
            }
        }
        val latestVersion = TemplateVersion(
            version = latestTemplate.version,
            versionName = latestTemplate.versionName,
            updateTime = latestTemplate.updateTime.timestampmilli(),
            creator = latestTemplate.creator
        )
        val versions = listTemplateVersions(latestTemplate.projectId, latestTemplate.id)
        val triggerContainer = templateResult.stages[0].containers[0] as TriggerContainer
        val params = triggerContainer.params
        val templateParams = triggerContainer.templateParams
        return TemplateModelDetail(
            versions = versions,
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            templateName = setting.name,
            description = setting.desc ?: "",
            creator = if (isConstrainedFlag) latestTemplate.creator else template.creator,
            template = templateResult,
            templateType = if (isConstrainedFlag) latestTemplate.type else template.type,
            logoUrl = if (isConstrainedFlag) latestTemplate.logoUrl ?: "" else {
                if (template.logoUrl.isNullOrEmpty()) "" else template.logoUrl
            },
            hasPermission = hasManagerPermission(projectId, userId),
            params = params,
            templateParams = templateParams
        )
    }

    fun getLatestVersion(
        projectId: String,
        templateId: String
    ): TTemplateRecord {
        var latestVersion = templateDao.getLatestTemplate(dslContext, projectId, templateId)
        if (latestVersion.type == TemplateType.CONSTRAINT.name) {
            latestVersion = templateDao.getLatestTemplate(dslContext, latestVersion.srcTemplateId)
        }
        return latestVersion
    }

    private fun listTemplateVersions(projectId: String, templateId: String): List<TemplateVersion> {
        val versionInfos = templateDao.getTemplateVersionInfos(dslContext, projectId, templateId)
        val tTemplate = TTemplate.T_TEMPLATE
        val versions = mutableListOf<TemplateVersion>()
        val versionNames = mutableListOf<String>()
        versionInfos?.forEach { versionInfo ->
            val versionName = versionInfo[tTemplate.VERSION_NAME]
            // 取versionName分组的最新的一条记录
            if (!versionNames.contains(versionName)) {
                versions.add(
                    TemplateVersion(
                        version = versionInfo[tTemplate.VERSION],
                        versionName = versionName,
                        updateTime = versionInfo[tTemplate.UPDATE_TIME].timestampmilli(),
                        creator = versionInfo[tTemplate.CREATOR]
                    )
                )
            }
            versionNames.add(versionName)
        }
        return versions.sortedByDescending { templateVersion -> templateVersion.updateTime }
    }

    /**
     * 差异对比
     * 比较主要是对当前流水线对应的模板版本和选中的版本进行比较
     * 需要注意：
     * 1. 更新前的变量是流水线的变量
     * 2. 更新后的变量是流水线的变量和对应的模板的变量的一个对比
     */
    fun compareTemplateInstances(
        projectId: String,
        userId: String,
        templateId: String,
        pipelineId: String,
        version: Long
    ): TemplateCompareModelResult {
        logger.info("Compare the template instances - [$projectId|$userId|$templateId|$pipelineId|$version]")
        val templatePipelineRecord = templatePipelineDao.get(dslContext, projectId, pipelineId)
            ?: throw NotFoundException(
                I18nUtil.getCodeLanMessage(
                messageCode = ERROR_TEMPLATE_NOT_EXISTS,
                language = I18nUtil.getLanguage(userId)
            ))
        val template: Model = objectMapper.readValue(
            templateDao.getTemplate(dslContext = dslContext, version = templatePipelineRecord.version).template
        )
        val v1Model: Model = instanceCompareModel(
            objectMapper.readValue(
                content = pipelineResDao.getVersionModelString(dslContext, projectId, pipelineId, null)
                    ?: throw ErrorCodeException(
                        statusCode = Response.Status.NOT_FOUND.statusCode,
                        errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
                    )
            ),
            template
        )

        val srcTemplate = templateDao.getTemplate(dslContext = dslContext, version = version)
        val v2Model = getTemplateModel(srcTemplate.template)
        val v1Containers = getContainers(v1Model)
        val v2Containers = getContainers(v2Model)

        compareContainer(v1Containers, v2Containers)
        val versions = listTemplateVersions(srcTemplate.projectId, srcTemplate.id)
        return compareModel(versions, v1Model, v2Model)
    }

    fun compareModel(versions: List<TemplateVersion>, v1Model: Model, v2Model: Model): TemplateCompareModelResult {
        val v1TriggerContainer = v1Model.stages[0].containers[0] as TriggerContainer
        val v2TriggerContainer = v2Model.stages[0].containers[0] as TriggerContainer
        return TemplateCompareModelResult(
            versions,
            TemplateCompareModel(
                buildNo = v1TriggerContainer.buildNo,
                params = v1TriggerContainer.params,
                model = v1Model
            ),
            TemplateCompareModel(
                buildNo = v2TriggerContainer.buildNo,
                params = BuildPropertyCompatibilityTools.mergeProperties(
                    from = v1TriggerContainer.params, to = v2TriggerContainer.params
                ),
                model = v2Model
            )
        )
    }

    fun compareContainer(v1Containers: Map<String, Container>, v2Containers: Map<String, Container>) {

        v1Containers.forEach { (id, container) ->
            val v2Container = v2Containers[id]
            if (v2Container == null) {
                logger.warn("The container is not exist of $id")
                return@forEach
            }
            container.elements.forEach e@{ element ->
                v2Container.elements.forEach { v2Element ->
                    if (element.id == v2Element.id) {
                        val modify = elementModify(element, v2Element)
                        if (modify) {
                            element.templateModify = true
                            v2Element.templateModify = true
                        }
                        return@e
                    }
                }
            }
        }
    }

    private fun getTemplateModel(templateModelStr: String): Model {
        return objectMapper.readValue(templateModelStr)
    }

    fun elementModify(e1: Element, e2: Element): Boolean {
        if (e1::class != e2::class) {
            return true
        }

        val v1Properties = e1.javaClass.kotlin.declaredMemberProperties
        val v2Properties = e2.javaClass.kotlin.declaredMemberProperties
        if (v1Properties.size != v2Properties.size) {
            return true
        }

        val v1Map = v1Properties.associate {
            it.isAccessible = true
            it.name to it.get(e1)
        }

        val v2Map = v2Properties.associate {
            it.isAccessible = true
            it.name to it.get(e2)
        }

        if (v1Map.size != v2Map.size) {
            return true
        }

        for ((key, value) in v1Map) {
            if (!v2Map.containsKey(key)) {
                return true
            }
            if (v2Map[key] != value) {
                return true
            }
        }
        return false
    }

    private fun getContainers(model: Model): Map<String/*ID*/, Container> {
        val containers = HashMap<String, Container>()

        model.stages.forEach { stage ->
            stage.containers.forEach c@{
                if (it.containerId == null) {
                    logger.warn("The container id is null of $it")
                    return@c
                }
                containers[it.containerId!!] = it
            }
        }

        return containers
    }

    /**
     * 通过流水线ID获取流水线的参数，已经和模板的参数做了Merge操作
     */
    fun listTemplateInstancesParams(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        pipelineIds: Set<String>
    ): Map<String, TemplateInstanceParams> {
        try {
            val template = templateDao.getTemplate(dslContext = dslContext, version = version)
            val templateModel: Model = objectMapper.readValue(template.template)
            val templateTriggerContainer = templateModel.stages[0].containers[0] as TriggerContainer
            val latestInstances = listLatestModel(projectId, pipelineIds)
            val settings = pipelineSettingDao.getSettings(dslContext, pipelineIds, projectId)
            val buildNos = pipelineBuildSummaryDao.getSummaries(dslContext, projectId, pipelineIds).map {
                it.pipelineId to it.buildNo
            }.toMap()

            return latestInstances.map {
                val pipelineId = it.key
                val instanceModel: Model = objectMapper.readValue(it.value)
                val instanceTriggerContainer = instanceModel.stages[0].containers[0] as TriggerContainer
                val instanceParams = paramService.filterParams(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    params = removeProperties(templateTriggerContainer.params, instanceTriggerContainer.params)
                )
                logger.info("[$userId|$projectId|$templateId|$version] Get the param ($instanceParams)")

                val buildNo = templateTriggerContainer.buildNo
                var instanceBuildNoObj: BuildNo? = null
                // 模板中的buildNo存在才需要回显
                if (buildNo != null) {
                    instanceBuildNoObj = BuildNo(
                        buildNoType = buildNo.buildNoType,
                        required = buildNo.required ?: instanceTriggerContainer.buildNo?.required,
                        buildNo = buildNos[pipelineId] ?: buildNo.buildNo
                    )
                }

                pipelineId to TemplateInstanceParams(
                    pipelineId = pipelineId,
                    pipelineName = getPipelineName(settings, pipelineId) ?: templateModel.name,
                    buildNo = instanceBuildNoObj,
                    param = instanceParams
                )
            }.toMap()
        } catch (ignored: Throwable) {
            logger.warn("Fail to list pipeline params - [$projectId|$userId|$templateId|$version]", ignored)
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.FAIL_TO_LIST_TEMPLATE_PARAMS
            )
        }
    }

    fun createTemplateInstances(
        projectId: String,
        userId: String,
        templateId: String,
        version: Long,
        useTemplateSettings: Boolean,
        instances: List<TemplateInstanceCreate>
    ): TemplateOperationRet {
        logger.info("Create the new template instance [$projectId|$userId|$templateId|$version|$useTemplateSettings]")
        val template = templateDao.getTemplate(dslContext = dslContext, version = version)
        val successPipelines = ArrayList<String>()
        val failurePipelines = ArrayList<String>()
        val successPipelinesId = ArrayList<String>()
        val messages = HashMap<String, String>()

        instances.forEach { instance ->
            try {
                val pipelineName = instance.pipelineName
                val buildNo = instance.buildNo
                val param = instance.param
                val defaultStageTagId = stageTagService.getDefaultStageTag().data?.id
                val instanceModel =
                    PipelineUtils.instanceModel(
                        templateModel = objectMapper.readValue(template.template),
                        pipelineName = pipelineName,
                        buildNo = buildNo,
                        param = param,
                        instanceFromTemplate = true,
                        defaultStageTagId = defaultStageTagId
                    )
                instanceModel.templateId = templateId
                val pipelineId = pipelineInfoFacadeService.createPipeline(
                    userId = userId,
                    projectId = projectId,
                    model = instanceModel,
                    channelCode = ChannelCode.BS,
                    checkPermission = true,
                    fixPipelineId = null,
                    instanceType = PipelineInstanceTypeEnum.CONSTRAINT.type,
                    buildNo = buildNo,
                    param = param,
                    fixTemplateVersion = version
                )
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)
                    if (useTemplateSettings) {
                        val setting = copySetting(
                            setting = getTemplateSetting(
                                projectId = projectId,
                                userId = userId,
                                templateId = templateId
                            ),
                            pipelineId = pipelineId,
                            templateName = instance.pipelineName
                        )
                        saveTemplatePipelineSetting(userId, setting)
                    } else {
                        insertTemplateSetting(
                            context = context,
                            projectId = projectId,
                            templateId = pipelineId,
                            pipelineName = pipelineName,
                            isTemplate = false
                        )
                    }
                    addRemoteAuth(instanceModel, projectId, pipelineId, userId)
                    successPipelines.add(instance.pipelineName)
                    successPipelinesId.add(pipelineId)
                }
            } catch (ignored: DuplicateKeyException) {
                logger.warn("TemplateCreateInstanceDuplicate|$projectId|$instance|$userId|${ignored.message}")
                failurePipelines.add(instance.pipelineName)
                messages[instance.pipelineName] = "duplicate!"
            } catch (exception: ErrorCodeException) {
                logger.warn("TemplateCreateInstanceErrorCode|$projectId|$instance|$userId|${exception.message}")
                messages[instance.pipelineName] = I18nUtil.generateResponseDataObject(
                    messageCode = exception.errorCode,
                    params = exception.params,
                    data = null,
                    defaultMessage = exception.defaultMessage
                ).message ?: exception.defaultMessage ?: "unknown!"
                failurePipelines.add(instance.pipelineName)
            } catch (ignored: Throwable) {
                logger.warn("TemplateCreateInstanceThrowable|$projectId|$instance|$userId|${ignored.message}")
                failurePipelines.add(instance.pipelineName)
                messages[instance.pipelineName] = ignored.message ?: "create instance fail"
            }
        }

        return TemplateOperationRet(
            0,
            TemplateOperationMessage(
                successPipelines = successPipelines,
                failurePipelines = failurePipelines,
                failureMessages = messages,
                successPipelinesId = successPipelinesId
            ),
            ""
        )
    }

    /**
     * 批量更新模板实例
     */
    fun updateTemplateInstances(
        projectId: String,
        userId: String,
        templateId: String,
        version: Long? = null,
        versionName: String? = null,
        useTemplateSettings: Boolean,
        instances: List<TemplateInstanceUpdate>
    ): TemplateOperationRet {
        logger.info("UPDATE_TEMPLATE_INST[$projectId|$userId|$templateId|$version|$instances|$useTemplateSettings]")
        if (instances.size > maxUpdateInstanceNum) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.FAIL_TEMPLATE_UPDATE_NUM_TOO_BIG,
                params = arrayOf("${instances.size}", "$maxUpdateInstanceNum")
            )
        }
        val successPipelines = ArrayList<String>()
        val failurePipelines = ArrayList<String>()
        val messages = HashMap<String, String>()
        if (version == null && versionName.isNullOrBlank()) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_NEED_PARAM_,
                params = arrayOf("version or versionName")
            )
        }

        // 查询该模板的源模板ID(只查研发商店模板的源模板ID这种情况)
        val srcTemplateId = templateDao.getSrcTemplateId(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId,
            type = TemplateType.CONSTRAINT.name
        )

        val template = templateDao.getTemplate(
            dslContext = dslContext,
            templateId = srcTemplateId ?: templateId,
            versionName = versionName,
            version = version
        )
        instances.forEach {
            try {
                updateTemplateInstanceInfo(
                    userId = userId,
                    useTemplateSettings = useTemplateSettings,
                    projectId = projectId,
                    templateId = templateId,
                    templateVersion = template.version,
                    versionName = template.versionName,
                    templateContent = template.template,
                    templateInstanceUpdate = it,
                    srcTemplateId = srcTemplateId
                )
                successPipelines.add(it.pipelineName)
            } catch (ignored: DuplicateKeyException) {
                logger.warn("updateTemplateInstancesDuplicate|$projectId|$it|$userId|${ignored.message}")
                failurePipelines.add(it.pipelineName)
                messages[it.pipelineName] = " exist!"
            } catch (exception: ErrorCodeException) {
                logger.warn("updateTemplateInstancesErrorCode|$projectId|$it|$userId|${exception.message}")
                messages[it.pipelineName] = I18nUtil.generateResponseDataObject(
                    messageCode = exception.errorCode,
                    params = exception.params,
                    data = null,
                    defaultMessage = exception.defaultMessage
                ).message ?: exception.defaultMessage ?: "unknown!"
                failurePipelines.add(it.pipelineName)
            } catch (ignored: Throwable) {
                logger.warn("updateTemplateInstancesThrowable|$projectId|$it|$userId|${ignored.message}")
                failurePipelines.add(it.pipelineName)
                messages[it.pipelineName] = ignored.message ?: "update instance fail"
            }
        }
        return TemplateOperationRet(0, TemplateOperationMessage(successPipelines, failurePipelines, messages), "")
    }

    fun updateTemplateInstanceInfo(
        userId: String,
        useTemplateSettings: Boolean,
        projectId: String,
        templateId: String,
        templateVersion: Long,
        versionName: String,
        templateContent: String,
        templateInstanceUpdate: TemplateInstanceUpdate,
        srcTemplateId: String? = null
    ) {
        val templateSrcTemplateId = if (srcTemplateId.isNullOrBlank()) {
            templateDao.getSrcTemplateId(
                dslContext = dslContext,
                projectId = projectId,
                templateId = templateId,
                type = TemplateType.CONSTRAINT.name
            )
        } else {
            srcTemplateId
        }
        if (templateSrcTemplateId != null) {
            // 安装的研发商店模板需校验模板下组件可见范围
            val validateRet = client.get(ServiceTemplateResource::class)
                .validateUserTemplateComponentVisibleDept(
                    userId = userId,
                    templateCode = templateSrcTemplateId,
                    projectCode = projectId
                )
            if (validateRet.isNotOk()) {
                throw ErrorCodeException(
                    errorCode = validateRet.status.toString(),
                    defaultMessage = validateRet.message
                )
            }
        }

        // #4673,流水线更新应该单独一个事务,不然在发送完流水线更新MQ消息时,如果MQ的消费者查询流水线最新model,可能会查不到
        val templateModel: Model = objectMapper.readValue(templateContent)
        val labels = if (useTemplateSettings) {
            templateModel.labels
        } else {
            val tmpLabels = ArrayList<String>()
            pipelineGroupService.getGroups(
                userId = userId,
                projectId = projectId,
                pipelineId = templateInstanceUpdate.pipelineId
            ).forEach { group ->
                tmpLabels.addAll(group.labels)
            }
            tmpLabels
        }

        val instanceModel = PipelineUtils.instanceModel(
            templateModel = templateModel,
            pipelineName = templateInstanceUpdate.pipelineName,
            buildNo = templateInstanceUpdate.buildNo,
            param = templateInstanceUpdate.param,
            instanceFromTemplate = true,
            labels = labels,
            defaultStageTagId = stageTagService.getDefaultStageTag().data?.id
        )

        instanceModel.templateId = templateId
        pipelineInfoFacadeService.editPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = templateInstanceUpdate.pipelineId,
            model = instanceModel,
            channelCode = ChannelCode.BS,
            checkPermission = true,
            checkTemplate = false
        )
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            templatePipelineDao.update(
                dslContext = context,
                projectId = projectId,
                templateVersion = templateVersion,
                versionName = versionName,
                userId = userId,
                instance = templateInstanceUpdate
            )

            if (useTemplateSettings) {
                val setting = copySetting(
                    setting = getTemplateSetting(
                        projectId = projectId,
                        userId = userId,
                        templateId = templateId
                    ),
                    pipelineId = templateInstanceUpdate.pipelineId,
                    templateName = templateInstanceUpdate.pipelineName
                )
                saveTemplatePipelineSetting(userId, setting)
            }
        }
    }

    fun asyncUpdateTemplateInstances(
        projectId: String,
        userId: String,
        templateId: String,
        version: Long,
        useTemplateSettings: Boolean,
        instances: List<TemplateInstanceUpdate>
    ): Boolean {
        logger.info("asyncUpdateTemplateInstances [$projectId|$userId|$templateId|$version|$useTemplateSettings]")
        // 当更新的实例数量较小则走同步更新逻辑，较大走异步更新逻辑
        if (instances.size <= maxSyncInstanceNum) {
            val template = templateDao.getTemplate(dslContext = dslContext, version = version)
            val successPipelines = ArrayList<String>()
            val failurePipelines = ArrayList<String>()
            instances.forEach { templateInstanceUpdate ->
                try {
                    updateTemplateInstanceInfo(
                        userId = userId,
                        useTemplateSettings = useTemplateSettings,
                        projectId = projectId,
                        templateId = templateId,
                        templateVersion = template.version,
                        versionName = template.versionName,
                        templateContent = template.template,
                        templateInstanceUpdate = templateInstanceUpdate
                    )
                    successPipelines.add(templateInstanceUpdate.pipelineName)
                } catch (exception: ErrorCodeException) {
                    logger.info("asyncUpdateTemplate|$projectId|$templateInstanceUpdate|$userId|${exception.message}")
                    val message = I18nUtil.generateResponseDataObject(
                        messageCode = exception.errorCode,
                        params = exception.params,
                        data = null,
                        defaultMessage = exception.defaultMessage
                    ).message ?: exception.defaultMessage ?: "unknown!"
                    failurePipelines.add("【${templateInstanceUpdate.pipelineName}】reason：$message")
                } catch (t: Throwable) {
                    val message =
                        if (!t.message.isNullOrBlank() && t.message!!.length > maxErrorReasonLength)
                            t.message!!.substring(0, maxErrorReasonLength) + "......" else t.message
                    failurePipelines.add("【${templateInstanceUpdate.pipelineName}】reason：$message")
                    logger.warn("asyncUpdateTemplate|$projectId|$templateInstanceUpdate|$userId|$message")
                }
            }
            // 发送执行任务结果通知
            TempNotifyTemplateUtils.sendUpdateTemplateInstanceNotify(
                client = client,
                projectId = projectId,
                receivers = mutableSetOf(userId),
                instanceListUrl = MessageFormat(instanceListUrl).format(arrayOf(projectId, templateId)),
                successPipelines = successPipelines,
                failurePipelines = failurePipelines
            )
        } else {
            // 检查流水线是否处于更新中
            val pipelineIds = instances.map { it.pipelineId }.toSet()
            val templateInstanceItems =
                templateInstanceItemDao.getTemplateInstanceItemListByPipelineIds(dslContext, projectId, pipelineIds)
            if (templateInstanceItems != null && templateInstanceItems.isNotEmpty) {
                val pipelineNames = templateInstanceItems.map { it.pipelineName }
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_TEMPLATE_PIPELINE_IS_INSTANCING,
                    params = arrayOf(JsonUtil.toJson(pipelineNames))
                )
            }
            val baseId = UUIDUtil.generate()
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                templateInstanceBaseDao.createTemplateInstanceBase(
                    dslContext = context,
                    baseId = baseId,
                    templateId = templateId,
                    templateVersion = version.toString(),
                    useTemplateSettingsFlag = useTemplateSettings,
                    projectId = projectId,
                    totalItemNum = instances.size,
                    status = TemplateInstanceBaseStatus.INIT.name,
                    userId = userId
                )
                templateInstanceItemDao.createTemplateInstanceItem(
                    dslContext = context,
                    projectId = projectId,
                    baseId = baseId,
                    instances = instances,
                    status = TemplateInstanceItemStatus.INIT.name,
                    userId = userId
                )
            }
        }
        return true
    }

    fun copySetting(setting: PipelineSetting, pipelineId: String, templateName: String): PipelineSetting {
        with(setting) {
            return PipelineSetting(
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = templateName,
                desc = desc,
                runLockType = runLockType,
                successSubscription = successSubscription,
                failSubscription = failSubscription,
                labels = labels,
                waitQueueTimeMinute = waitQueueTimeMinute,
                maxQueueSize = maxQueueSize,
                concurrencyGroup = concurrencyGroup,
                hasPermission = hasPermission,
                maxPipelineResNum = maxPipelineResNum,
                maxConRunningQueueSize = maxConRunningQueueSize,
                pipelineAsCodeSettings = pipelineAsCodeSettings
            )
        }
    }

    /**
     * 删除流水线参数中的流水线常量
     */
    private fun instanceCompareModel(
        instance: Model,
        template: Model
    ): Model {
        val templateParams = (template.stages[0].containers[0] as TriggerContainer).templateParams
        if (templateParams.isNullOrEmpty()) {
            return instance
        }
        val triggerContainer = instance.stages[0].containers[0] as TriggerContainer
        val finalParams = ArrayList<BuildFormProperty>()
        val params = triggerContainer.params
        params.forEach { param ->
            var exist = false
            run lit@{
                templateParams.forEach { template ->
                    if (template.id == param.id) {
                        // 流水线参数中的这个参数是模板常量， 要剔除
                        exist = true
                        return@lit
                    }
                }
            }
            if (!exist) {
                finalParams.add(param)
            }
        }

        val finalTriggerContainer = with(triggerContainer) {
            TriggerContainer(
                id = id,
                name = name,
                elements = elements,
                params = finalParams,
                templateParams = templateParams,
                buildNo = buildNo,
                containerId = containerId,
                containerHashId = containerHashId
            )
        }

        return Model(
            name = instance.name,
            desc = instance.desc,
            stages = PipelineUtils.getFixedStages(instance, finalTriggerContainer, defaultStageTagId = null),
            labels = instance.labels,
            instanceFromTemplate = true
        )
    }

    private fun saveTemplatePipelineSetting(
        userId: String,
        setting: PipelineSetting,
        isTemplate: Boolean = false
    ): Int {
        pipelineGroupService.updatePipelineLabel(
            userId = userId,
            projectId = setting.projectId,
            pipelineId = setting.pipelineId,
            labelIds = setting.labels
        )
        pipelineInfoDao.update(
            dslContext = dslContext,
            projectId = setting.projectId,
            pipelineId = setting.pipelineId,
            userId = userId,
            updateVersion = false,
            pipelineName = setting.pipelineName,
            pipelineDesc = setting.desc
        )
        logger.info("Save the template pipeline setting - ($setting)")
        return pipelineSettingDao.saveSetting(dslContext, setting, isTemplate)
    }

    private fun instanceParamModel(userId: String, projectId: String, model: Model): Model {
        val triggerContainer = model.stages[0].containers[0] as TriggerContainer
        val params = paramService.filterParams(userId, projectId, null, triggerContainer.params)
        val templateParams =
            if (triggerContainer.templateParams == null || triggerContainer.templateParams!!.isEmpty()) {
                triggerContainer.templateParams
            } else {
                paramService.filterParams(userId, projectId, null, triggerContainer.templateParams!!)
            }
        val rewriteContainer = TriggerContainer(
            name = triggerContainer.name,
            elements = triggerContainer.elements,
            params = params, templateParams = templateParams,
            buildNo = triggerContainer.buildNo,
            containerId = triggerContainer.containerId,
            containerHashId = triggerContainer.containerHashId
        )
        val defaultStageTagId = stageTagService.getDefaultStageTag().data?.id
        return Model(
            name = model.name,
            desc = "",
            stages = PipelineUtils.getFixedStages(model, rewriteContainer, defaultStageTagId),
            labels = model.labels,
            instanceFromTemplate = false
        )
    }

    /**
     * 只有管理员权限才能对模板操作
     */
    private fun checkPermission(projectId: String, userId: String) {
        val isProjectUser = hasManagerPermission(projectId = projectId, userId = userId)
        if (!isProjectUser) {
            logger.warn("The manager users is empty of project $projectId")
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ONLY_MANAGE_CAN_OPERATE_TEMPLATE
            )
        }
    }

    fun hasManagerPermission(projectId: String, userId: String): Boolean =
        pipelinePermissionService.checkProjectManager(userId = userId, projectId = projectId)

    /**
     * 删除模板的参数， 如果模板中没有这个参数，那么流水线中应该删除掉
     */
    fun removeProperties(
        templateParams: List<BuildFormProperty>,
        pipelineParams: List<BuildFormProperty>
    ): List<BuildFormProperty> {
        if (templateParams.isEmpty()) {
            return emptyList()
        }

        val result = ArrayList<BuildFormProperty>()

        templateParams.forEach outside@{ template ->
            pipelineParams.forEach { pipeline ->
                if (pipeline.id == template.id) {
                    /**
                     * 1. 比较类型， 如果类型变了就直接用模板
                     * 2. 如果类型相同，下拉选项替换成模板的（要保存用户之前的默认值）
                     */
                    if (pipeline.type != template.type) {
                        result.add(template)
                    } else {
                        pipeline.options = template.options
                        pipeline.required = template.required
                        pipeline.desc = template.desc
                        result.add(pipeline)
                    }
                    return@outside
                }
            }
            result.add(template)
        }

        return result
    }

    fun listTemplateInstancesInPage(
        projectId: String,
        userId: String,
        templateId: String,
        page: Int,
        pageSize: Int,
        searchKey: String?,
        sortType: TemplateSortTypeEnum?,
        desc: Boolean?
    ): TemplateInstancePage {
        logger.info("LIST_TEMPLATE[$projectId|$userId|$templateId|$page|$pageSize]|$searchKey")

        val instancePage =
            templatePipelineDao.listPipelineInPage(
                dslContext = dslContext,
                projectId = projectId,
                templateId = templateId,
                instanceType = PipelineInstanceTypeEnum.CONSTRAINT.type,
                page = page,
                pageSize = pageSize,
                searchKey = searchKey,
                sortType = sortType,
                desc = desc
            )
        val associatePipelines = instancePage.records
        val pipelineIds = associatePipelines.map { it[KEY_PIPELINE_ID] as String }.toSet()
        logger.info("Get the pipelineIds - $associatePipelines")
        val pipelineSettings = pipelineSettingDao.getSettings(
            dslContext = dslContext,
            pipelineIds = pipelineIds,
            projectId = projectId
        ).groupBy { it.pipelineId }
        logger.info("Get the pipeline settings - $pipelineSettings")
        val hasPermissionList = pipelinePermissionService.getResourceByPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.EDIT
        )

        val latestVersion = getLatestVersion(projectId, templateId)
        val version = latestVersion.version
        val templateInstanceItems = templateInstanceItemDao.getTemplateInstanceItemListByPipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            pipelineIds = pipelineIds
        )
        val templatePipelines = associatePipelines.map {
            val pipelineId = it[KEY_PIPELINE_ID] as String
            val pipelineSetting = pipelineSettings[pipelineId]
            if (pipelineSetting.isNullOrEmpty()) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.PIPELINE_SETTING_NOT_EXISTS
                )
            }
            val templatePipelineVersion = it[KEY_VERSION] as Long
            val templatePipelineStatus = generateTemplatePipelineStatus(
                templateInstanceItems = templateInstanceItems,
                templatePipelineId = pipelineId,
                templatePipelineVersion = templatePipelineVersion,
                version = version
            )
            TemplatePipeline(
                templateId = it[KEY_TEMPLATE_ID] as String,
                versionName = it[KEY_VERSION_NAME] as String,
                version = templatePipelineVersion,
                pipelineId = pipelineId,
                pipelineName = pipelineSetting[0].name,
                updateTime = (it[KEY_UPDATED_TIME] as LocalDateTime).timestampmilli(),
                hasPermission = hasPermissionList.contains(pipelineId),
                status = templatePipelineStatus
            )
        }
        val sortTemplatePipelines = templatePipelines.sortedWith { a, b ->
            when (sortType) {
                TemplateSortTypeEnum.PIPELINE_NAME -> {
                    a.pipelineName.lowercase().compareTo(b.pipelineName.lowercase())
                }

                TemplateSortTypeEnum.STATUS -> {
                    b.status.name.compareTo(a.status.name)
                }

                else -> 0
            }
        }
        return TemplateInstancePage(
            projectId = projectId,
            templateId = templateId,
            instances = if (desc == true) sortTemplatePipelines.reversed() else sortTemplatePipelines,
            latestVersion = TemplateVersion(
                version = latestVersion.version,
                versionName = latestVersion.versionName,
                updateTime = latestVersion.updateTime.timestampmilli(),
                creator = latestVersion.creator
            ),
            count = instancePage.count.toInt(),
            page = page,
            pageSize = pageSize
        )
    }

    fun generateTemplatePipelineStatus(
        templateInstanceItems: Result<TTemplateInstanceItemRecord>?,
        templatePipelineId: String,
        templatePipelineVersion: Long,
        version: Long
    ): TemplatePipelineStatus {
        var templatePipelineStatus = TemplatePipelineStatus.UPDATED
        run lit@{
            templateInstanceItems?.forEach { templateInstanceItem ->
                if (templateInstanceItem.pipelineId == templatePipelineId) {
                    // 任务表中有记录说明模板实例处于更新中
                    templatePipelineStatus = TemplatePipelineStatus.UPDATING
                    return@lit
                }
            }
            if (templatePipelineVersion != version) {
                templatePipelineStatus = TemplatePipelineStatus.PENDING_UPDATE
            }
        }
        return templatePipelineStatus
    }

    fun serviceCountTemplateInstances(projectId: String, templateIds: Collection<String>): Int {
        logger.info("[$projectId|$templateIds] List the templates instances")
        if (templateIds.isEmpty()) return 0
        return templatePipelineDao.countByTemplates(
            dslContext = dslContext,
            projectId = projectId,
            instanceType = PipelineInstanceTypeEnum.CONSTRAINT.type,
            templateIds = templateIds
        )
    }

    fun serviceCountTemplateInstancesDetail(projectId: String, templateIds: Collection<String>): Map<String, Int> {
        logger.info("[$projectId|$templateIds] List the templates instances")
        if (templateIds.isEmpty()) {
            return mapOf()
        }
        return templatePipelineDao.listPipeline(
            dslContext = dslContext,
            projectId = projectId,
            instanceType = PipelineInstanceTypeEnum.CONSTRAINT.type,
            templateIds = templateIds
        ).groupBy { it[KEY_TEMPLATE_ID] as String }.map { it.key to it.value.size }.toMap()
    }

    /**
     * 检查模板是不是合法
     */
    private fun checkTemplate(template: Model, projectId: String? = null) {
        if (template.name.isBlank()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.TEMPLATE_NAME_CAN_NOT_NULL
            )
        }
        modelCheckPlugin.checkModelIntegrity(model = template, projectId = projectId)
        checkPipelineParam(template)
    }

    fun checkTemplate(templateId: String, projectId: String? = null): Boolean {
        val templateRecord = if (projectId.isNullOrEmpty()) {
            templateDao.getLatestTemplate(dslContext, templateId)
        } else {
            templateDao.getLatestTemplate(dslContext, projectId, templateId)
        }
        val modelStr = templateRecord.template
        if (modelStr != null) {
            val model = JsonUtil.to(modelStr, Model::class.java)
            checkTemplate(model, projectId)
        }
        return true
    }

    /**
     * 模板的流水线变量和模板常量不能相同
     */
    private fun checkPipelineParam(template: Model) {
        val triggerContainer = template.stages[0].containers[0] as TriggerContainer

        if (triggerContainer.params.isEmpty()) {
            return
        }

        if (triggerContainer.templateParams == null || triggerContainer.templateParams!!.isEmpty()) {
            return
        }

        triggerContainer.params.forEach { param ->
            triggerContainer.templateParams!!.forEach { template ->
                if (param.id == template.id) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.PIPELINE_PARAM_CONSTANTS_DUPLICATE
                    )
                }
            }
        }
    }

    private fun getPipelineName(records: Result<TPipelineSettingRecord>, pipelineId: String): String? {
        records.forEach {
            if (it.pipelineId == pipelineId) {
                return it.name
            }
        }
        return null
    }

    private fun updateModelParam(model: Model) {
        val defaultStageTagId = stageTagService.getDefaultStageTag().data?.id
        val defaultTagIds = defaultStageTagId?.let { listOf(it) }
        model.stages.forEachIndexed { index, stage ->
            stage.id = stage.id ?: VMUtils.genStageId(index + 1)
            if (stage.name.isNullOrBlank()) stage.name = stage.id
            if (stage.tag == null) stage.tag = defaultTagIds
            stage.containers.forEach { container ->
                if (container.containerId.isNullOrBlank()) {
                    container.containerId = container.id
                }
                if (container.containerHashId.isNullOrBlank()) {
                    container.containerHashId = modelContainerIdGenerator.getNextId()
                }
                container.elements.forEach { e ->
                    if (e.id.isNullOrBlank()) {
                        e.id = modelTaskIdGenerator.getNextId()
                    }
                }
            }
        }
    }

    private fun checkTemplateName(
        dslContext: DSLContext,
        name: String,
        projectId: String,
        templateId: String
    ) {
        val count = pipelineSettingDao.getSettingByName(
            dslContext = dslContext,
            name = name,
            projectId = projectId,
            pipelineId = templateId,
            isTemplate = true
        )?.value1() ?: 0
        if (count > 0) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_NAME_IS_EXISTS
            )
        }
        // 判断提交的模板数量是否超过系统规定的阈值
        val versionNameNum = templateDao.countTemplateVersionNum(dslContext, projectId, templateId)
        if (versionNameNum >= maxSaveVersionNum) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_VERSION_COUNT_EXCEEDS_LIMIT,
                params = arrayOf(maxSaveVersionNum.toString())
            )
        }
    }

    private fun insertTemplateSetting(
        context: DSLContext,
        projectId: String,
        templateId: String,
        pipelineName: String,
        isTemplate: Boolean
    ) {
        pipelineSettingDao.insertNewSetting(
            dslContext = context,
            projectId = projectId,
            pipelineId = templateId,
            pipelineName = pipelineName,
            isTemplate = isTemplate,
            failNotifyTypes = pipelineInfoExtService.failNotifyChannel(),
            pipelineAsCodeSettings = try {
                client.get(ServiceProjectResource::class).get(projectId).data
                    ?.properties?.pipelineAsCodeSettings
            } catch (ignore: Throwable) {
                logger.warn("[$projectId]|Failed to sync project|templateId=$templateId", ignore)
                null
            }
        )
    }

    fun listLatestModel(projectId: String, pipelineIds: Set<String>): Map<String/*Pipeline ID*/, String/*Model*/> {
        val modelResources = pipelineResDao.listLatestModelResource(dslContext, pipelineIds, projectId)
        return modelResources?.map { modelResource ->
            modelResource.value1() to modelResource.value3()
        }?.toMap() ?: mapOf()
    }

    fun addMarketTemplate(
        userId: String,
        addMarketTemplateRequest: AddMarketTemplateRequest
    ): com.tencent.devops.common.api.pojo.Result<Map<String, String>> {
        logger.info("the userId is:$userId,addMarketTemplateRequest is:$addMarketTemplateRequest")
        val templateCode = addMarketTemplateRequest.templateCode
        val publicFlag = addMarketTemplateRequest.publicFlag // 是否为公共模板
        val category = JsonUtil.toJson(addMarketTemplateRequest.categoryCodeList ?: listOf<String>(), false)
        val projectCodeList = addMarketTemplateRequest.projectCodeList
        // 校验安装的模板是否合法
        if (!publicFlag && redisOperation.get("checkInstallTemplateModelSwitch")?.toBoolean() != false) {
            val templateRecord = templateDao.getLatestTemplate(dslContext, templateCode)
            val modelStr = templateRecord.template
            if (modelStr != null) {
                val model = JsonUtil.to(modelStr, Model::class.java)
                projectCodeList.forEach {
                    checkTemplate(model, it)
                }
            }
        }
        val projectTemplateMap = mutableMapOf<String, String>()
        val versionName = if (publicFlag) {
            "init"
        } else {
            templateDao.getLatestTemplate(dslContext, templateCode).versionName
        }
        val templateName = addMarketTemplateRequest.templateName
        dslContext.transaction { t ->
            val context = DSL.using(t)
            projectCodeList.forEach {
                // 判断模板名称是否已经关联过
                val pipelineSettingRecord = pipelineSettingDao.getSetting(
                    dslContext = context,
                    projectId = it,
                    name = templateName,
                    isTemplate = true
                )
                if (pipelineSettingRecord.size > 0) {
                    return@forEach
                }
                val templateId = UUIDUtil.generate()
                templateDao.createTemplate(
                    dslContext = context,
                    projectId = it,
                    templateId = templateId,
                    templateName = templateName,
                    versionName = versionName,
                    userId = userId,
                    template = null,
                    type = TemplateType.CONSTRAINT.name,
                    category = category,
                    logoUrl = addMarketTemplateRequest.logoUrl,
                    srcTemplateId = templateCode,
                    storeFlag = true,
                    weight = 0,
                    version = client.get(ServiceAllocIdResource::class).generateSegmentId(TEMPLATE_BIZ_TAG_NAME).data
                )
                insertTemplateSetting(
                    context = context,
                    projectId = it,
                    templateId = templateId,
                    isTemplate = true,
                    pipelineName = templateName
                )
                projectTemplateMap[it] = templateId
            }
        }
        return com.tencent.devops.common.api.pojo.Result(projectTemplateMap)
    }

    fun updateMarketTemplateReference(
        userId: String,
        updateMarketTemplateRequest: AddMarketTemplateRequest
    ): com.tencent.devops.common.api.pojo.Result<Boolean> {
        logger.info("the userId is:$userId,updateMarketTemplateReference Request is:$updateMarketTemplateRequest")
        val templateCode = updateMarketTemplateRequest.templateCode
        val category = JsonUtil.toJson(updateMarketTemplateRequest.categoryCodeList ?: listOf<String>(), false)
        val referenceList = templateDao.listTemplateReferenceId(dslContext, templateCode).map { it["ID"] as String }
        if (referenceList.isNotEmpty()) {
            pipelineSettingDao.updateSettingName(dslContext, referenceList, updateMarketTemplateRequest.templateName)
            templateDao.updateTemplateReference(
                dslContext = dslContext,
                srcTemplateId = templateCode,
                name = updateMarketTemplateRequest.templateName,
                category = category,
                logoUrl = updateMarketTemplateRequest.logoUrl
            )
        }
        return com.tencent.devops.common.api.pojo.Result(true)
    }

    fun updateTemplateStoreFlag(
        userId: String,
        templateId: String,
        storeFlag: Boolean
    ): com.tencent.devops.common.api.pojo.Result<Boolean> {
        templateDao.updateStoreFlag(dslContext, userId, templateId, storeFlag)
        return com.tencent.devops.common.api.pojo.Result(true)
    }

    fun addRemoteAuth(model: Model, projectId: String, pipelineId: String, userId: String) {
        val elementList = model.stages[0].containers[0].elements
        var isAddRemoteAuth = false
        elementList.map {
            if (it is RemoteTriggerElement) {
                isAddRemoteAuth = true
            }
        }
        if (isAddRemoteAuth) {
            logger.info("template Model has RemoteTriggerElement project[$projectId] pipeline[$pipelineId]")
            pipelineRemoteAuthService.generateAuth(pipelineId, projectId, userId)
        }
    }

    fun getTemplateIdByTemplateCode(templateCode: String, projectIds: List<String>): List<PipelineTemplateInfo> {
        val templateInfos = templateDao.listTemplateReferenceByProjects(dslContext, templateCode, projectIds)
        val templateList = mutableListOf<PipelineTemplateInfo>()
        templateInfos.forEach {
            templateList.add(
                PipelineTemplateInfo(
                    projectId = it.projectId,
                    templateId = it.id,
                    templateName = it.templateName,
                    versionName = it.versionName,
                    srcTemplateId = it.srcTemplateId
                )
            )
        }
        return templateList
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TemplateFacadeService::class.java)
        private const val INIT_TEMPLATE_NAME = "init"
        private const val TEMPLATE_BIZ_TAG_NAME = "TEMPLATE"
    }
}
