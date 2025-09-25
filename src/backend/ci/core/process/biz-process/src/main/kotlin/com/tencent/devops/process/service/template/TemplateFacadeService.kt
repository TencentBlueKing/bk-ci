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

package com.tencent.devops.process.service.template

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.KEY_INSTANCE_ERROR_INFO
import com.tencent.devops.common.api.constant.KEY_UPDATED_TIME
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.constant.KEY_VERSION_NAME
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeSvnElement
import com.tencent.devops.common.pipeline.pojo.element.agent.GithubElement
import com.tencent.devops.common.pipeline.pojo.element.atom.PipelineCheckFailedErrors
import com.tencent.devops.common.pipeline.pojo.element.atom.PipelineCheckFailedMsg
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.process.tables.TTemplate
import com.tencent.devops.model.process.tables.records.TTemplateInstanceItemRecord
import com.tencent.devops.model.process.tables.records.TTemplateRecord
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_ELEMENT_CHECK_FAILED
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.dao.label.PipelineLabelPipelineDao
import com.tencent.devops.process.engine.atom.AtomUtils
import com.tencent.devops.process.engine.cfg.ModelContainerIdGenerator
import com.tencent.devops.process.engine.cfg.ModelTaskIdGenerator
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.compatibility.BuildPropertyCompatibilityTools
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.dao.template.TemplateInstanceBaseDao
import com.tencent.devops.process.engine.dao.template.TemplateInstanceItemDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.engine.pojo.event.PipelineTemplateMigrateEvent
import com.tencent.devops.process.engine.utils.PipelineUtils
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.permission.template.PipelineTemplatePermissionService
import com.tencent.devops.process.pojo.PTemplateOrderByType
import com.tencent.devops.process.pojo.PTemplateSortType
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.PipelineTemplateInfo
import com.tencent.devops.process.pojo.enums.TemplateSortTypeEnum
import com.tencent.devops.process.pojo.template.CloneTemplateSettingExist
import com.tencent.devops.process.pojo.template.CopyTemplateReq
import com.tencent.devops.process.pojo.template.MarketTemplateRequest
import com.tencent.devops.process.pojo.template.OptionalTemplate
import com.tencent.devops.process.pojo.template.OptionalTemplateList
import com.tencent.devops.process.pojo.template.SaveAsTemplateReq
import com.tencent.devops.process.pojo.template.TemplateCompareModel
import com.tencent.devops.process.pojo.template.TemplateCompareModelResult
import com.tencent.devops.process.pojo.template.TemplateInstanceCreate
import com.tencent.devops.process.pojo.template.TemplateInstanceItemStatus
import com.tencent.devops.process.pojo.template.TemplateInstancePage
import com.tencent.devops.process.pojo.template.TemplateInstanceParams
import com.tencent.devops.process.pojo.template.TemplateInstanceStatus
import com.tencent.devops.process.pojo.template.TemplateInstanceUpdate
import com.tencent.devops.process.pojo.template.TemplateListModel
import com.tencent.devops.process.pojo.template.TemplateModel
import com.tencent.devops.process.pojo.template.TemplateModelDetail
import com.tencent.devops.process.pojo.template.TemplateOperationMessage
import com.tencent.devops.process.pojo.template.TemplateOperationRet
import com.tencent.devops.process.pojo.template.TemplatePipeline
import com.tencent.devops.process.pojo.template.TemplatePipelineStatus
import com.tencent.devops.common.pipeline.enums.TemplateRefType
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.TemplateVersion
import com.tencent.devops.process.pojo.template.TemplateWithPermission
import com.tencent.devops.process.pojo.template.v2.PTemplatePipelineVersion
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCompatibilityCreateReq
import com.tencent.devops.process.service.ParamFacadeService
import com.tencent.devops.process.service.PipelineAsCodeService
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.process.service.PipelineRemoteAuthService
import com.tencent.devops.process.service.StageTagService
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import com.tencent.devops.process.service.template.v2.PipelineTemplateMarketFacadeService
import com.tencent.devops.process.service.template.v2.PipelineTemplatePipelineVersionService
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionManager
import com.tencent.devops.process.util.TempNotifyTemplateUtils
import com.tencent.devops.process.utils.BK_EMPTY_PIPELINE
import com.tencent.devops.process.utils.KEY_PIPELINE_ID
import com.tencent.devops.process.utils.KEY_TEMPLATE_ID
import com.tencent.devops.process.utils.PipelineVersionUtils.differ
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.store.api.template.ServiceTemplateResource
import com.tencent.devops.store.pojo.atom.AtomCodeVersionReqItem
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.Response
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
import java.text.MessageFormat
import java.time.LocalDateTime

@Suppress("ALL")
@Service
@RefreshScope
class TemplateFacadeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val templateDao: TemplateDao,
    private val templatePipelineDao: TemplatePipelineDao,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineTemplatePermissionService: PipelineTemplatePermissionService,
    private val pipelineRemoteAuthService: PipelineRemoteAuthService,
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val stageTagService: StageTagService,
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val pipelineResourceDao: PipelineResourceDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val templateInstanceBaseDao: TemplateInstanceBaseDao,
    private val templateInstanceItemDao: TemplateInstanceItemDao,
    private val pipelineLabelPipelineDao: PipelineLabelPipelineDao,
    private val pipelineGroupService: PipelineGroupService,
    private val modelTaskIdGenerator: ModelTaskIdGenerator,
    private val modelContainerIdGenerator: ModelContainerIdGenerator,
    private val paramService: ParamFacadeService,
    private val modelCheckPlugin: ModelCheckPlugin,
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val templateCommonService: TemplateCommonService,
    private val templateSettingService: TemplateSettingService,
    private val pipelineAsCodeService: PipelineAsCodeService,
    private val pipelineTemplateVersionManager: PipelineTemplateVersionManager,
    private val pipelineTemplateMarketFacadeService: PipelineTemplateMarketFacadeService,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val templatePipelineVersionService: PipelineTemplatePipelineVersionService
) {

    @Value("\${template.maxSyncInstanceNum:10}")
    private val maxSyncInstanceNum: Int = 10

    @Value("\${template.maxUpdateInstanceNum:100}")
    private val maxUpdateInstanceNum: Int = 100

    @Value("\${template.maxSaveVersionRecordNum:2}")
    private val maxSaveVersionRecordNum: Int = 2

    @Value("\${template.instanceListUrl}")
    private val instanceListUrl: String = ""

    @Value("\${template.maxErrorReasonLength:200}")
    private val maxErrorReasonLength: Int = 200

    @Value("\${template.emptyPipelineTemplateId:#{null}}")
    private val emptyPipelineTemplateId: String? = null

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_CREATE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE_TEMPLATE
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_CREATE_CONTENT
    )
    fun createTemplate(projectId: String, userId: String, template: Model): String {
        logger.info("Start to create the template ${template.name} by user $userId")
        pipelineTemplatePermissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.CREATE
        )
        checkTemplate(template, projectId, userId)
        val templateId = UUIDUtil.generate()
        templateCommonService.checkTemplateName(
            dslContext = dslContext,
            name = template.name,
            projectId = projectId,
            templateId = templateId
        )
        updateModelParam(template)
        val setting = templateCommonService.getDefaultSetting(
            projectId = projectId,
            templateId = templateId,
            templateName = template.name,
            creator = userId
        )
        val request = PipelineTemplateCompatibilityCreateReq(
            model = template,
            setting = setting
        )
        val result = pipelineTemplateVersionManager.deployTemplate(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            request = request
        )
        logger.info("Get the template version ${result.version}")
        ActionAuditContext.current()
            .setInstanceId(templateId)
            .setInstanceName(template.name)
        return templateId
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE_TEMPLATE,
            instanceIds = "#srcTemplateId",
            instanceNames = "#copyTemplateReq?.templateName"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_EDIT_COPY_CONTENT
    )
    fun copyTemplate(
        userId: String,
        projectId: String,
        srcTemplateId: String,
        copyTemplateReq: CopyTemplateReq
    ): String {
        logger.info("Start to copy the template, $srcTemplateId | $userId | $copyTemplateReq")
        pipelineTemplatePermissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            templateId = srcTemplateId,
            permission = AuthPermission.EDIT
        )

        var latestTemplate = templateDao.getLatestTemplate(dslContext, projectId, srcTemplateId)
        if (latestTemplate.type == TemplateType.CONSTRAINT.name) {
            latestTemplate = templateDao.getLatestTemplate(dslContext, latestTemplate.srcTemplateId)
        }
        val templateId = UUIDUtil.generate()
        templateCommonService.checkTemplateName(
            dslContext = dslContext,
            name = copyTemplateReq.templateName,
            projectId = projectId,
            templateId = templateId
        )
        val setting = if (copyTemplateReq.isCopySetting) {
            templateSettingService.copySetting(
                templateSettingService.getTemplateSetting(
                    projectId = projectId,
                    userId = userId,
                    templateId = srcTemplateId
                ),
                templateId,
                copyTemplateReq.templateName,
                userId
            )
        } else {
            templateCommonService.getDefaultSetting(
                projectId = projectId,
                templateId = templateId,
                templateName = copyTemplateReq.templateName,
                creator = userId
            )
        }
        val model = JsonUtil.to(latestTemplate.template, Model::class.java)
        model.name = copyTemplateReq.templateName
        val request = PipelineTemplateCompatibilityCreateReq(
            model = model,
            setting = setting,
            category = latestTemplate.category,
            logoUrl = latestTemplate.logoUrl
        )
        val result = pipelineTemplateVersionManager.deployTemplate(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            request = request
        )
        logger.info("Get the template version ${result.version}")
        return templateId
    }

    /**
     * 流水线另存为模版
     */
    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE_TEMPLATE
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_EDIT_SAVE_AS_CONTENT
    )
    fun saveAsTemplate(
        userId: String,
        projectId: String,
        saveAsTemplateReq: SaveAsTemplateReq
    ): String {
        logger.info("Start to saveAsTemplate, $userId | $projectId | $saveAsTemplateReq")

        pipelineTemplatePermissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.CREATE
        )
        val template = pipelineResourceDao.getLatestVersionModelString(
            dslContext, projectId, saveAsTemplateReq.pipelineId
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
        )
        val templateModel: Model = PipelineUtils.fixedTemplateParam(objectMapper.readValue(template))
        templateModel.name = saveAsTemplateReq.templateName
        checkTemplateAtomsForExplicitVersion(templateModel, userId)
        val templateId = UUIDUtil.generate()
        templateCommonService.checkTemplateName(
            dslContext = dslContext,
            name = templateModel.name,
            projectId = projectId,
            templateId = templateId
        )
        val setting = if (saveAsTemplateReq.isCopySetting) {
            templateSettingService.copySetting(
                setting = templateSettingService.getTemplateSetting(
                    projectId = projectId,
                    userId = userId,
                    templateId = saveAsTemplateReq.pipelineId
                ),
                pipelineId = templateId,
                templateName = saveAsTemplateReq.templateName,
                creator = userId
            )
        } else {
            templateCommonService.getDefaultSetting(
                projectId = projectId,
                templateId = templateId,
                templateName = saveAsTemplateReq.templateName,
                creator = userId
            )
        }
        val request = PipelineTemplateCompatibilityCreateReq(
            model = templateModel,
            setting = setting
        )
        pipelineTemplateVersionManager.deployTemplate(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            request = request
        )

        return templateId
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE_TEMPLATE
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_DELETE_CONTENT
    )
    fun deleteTemplate(projectId: String, userId: String, templateId: String): Boolean {
        logger.info("Start to delete the template $templateId by user $userId")
        pipelineTemplatePermissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            permission = AuthPermission.DELETE
        )
        val template = templateDao.getLatestTemplate(dslContext, templateId)
        ActionAuditContext.current()
            .setInstanceId(templateId)
            .setInstanceName(template.templateName)
        pipelineTemplateVersionManager.deleteAllVersions(
            userId = userId,
            projectId = projectId,
            templateId = templateId
        )
        return true
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE_TEMPLATE
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_DELETE_CONTENT
    )
    fun deleteTemplate(projectId: String, userId: String, templateId: String, version: Long): Boolean {
        logger.info("Start to delete the template [$projectId|$userId|$templateId|$version]")
        pipelineTemplatePermissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            permission = AuthPermission.DELETE
        )
        ActionAuditContext.current()
            .setInstanceId(templateId)
            .setInstanceName(templateId)
        pipelineTemplateVersionManager.deleteVersion(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = version
        )
        return true
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE_TEMPLATE
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_DELETE_CONTENT
    )
    fun deleteTemplate(
        projectId: String,
        userId: String,
        templateId: String,
        versionName: String
    ): Boolean {
        logger.info("Start to delete the template [$projectId|$userId|$templateId|$versionName]")
        pipelineTemplatePermissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            permission = AuthPermission.DELETE
        )
        ActionAuditContext.current()
            .setInstanceId(templateId)
            .setInstanceName(templateId)
        pipelineTemplateVersionManager.deleteVersion(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = null,
            versionName = versionName
        )
        return true
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE_TEMPLATE
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_EDIT_CONTENT
    )

    /**
     * 更新流水线模板
     *
     * @param projectId 项目ID
     * @param userId 用户ID
     * @param templateId 模板ID
     * @param versionName 新版本名
     * @param template 模板模型数据
     * @param checkPermissionFlag 是否检查权限，默认为true
     * @return 新生成的模板版本号
     */
    fun updateTemplate(
        projectId: String,
        userId: String,
        templateId: String,
        versionName: String,
        template: Model,
        checkPermissionFlag: Boolean = true
    ): Long {
        logger.info("Start to update the template $templateId by user $userId - ($template)")

        // 1. 前置校验与准备
        if (checkPermissionFlag) {
            pipelineTemplatePermissionService.checkPipelineTemplatePermissionWithMessage(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                permission = AuthPermission.EDIT
            )
        }
        checkTemplate(template, projectId, userId)
        checkTemplateAtomsForExplicitVersion(template, userId)
        templateCommonService.checkTemplateName(dslContext, template.name, projectId, templateId)

        val v1LatestTemplate = templateDao.getLatestTemplate(dslContext, projectId, templateId)
        if (v1LatestTemplate.type == TemplateType.CONSTRAINT.name && v1LatestTemplate.storeFlag == true) {
            throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_TEMPLATE_NOT_UPDATE)
        }

        ActionAuditContext.current()
            .setInstanceId(templateId)
            .setInstanceName(template.name)

        // 注意：此方法会直接修改入参template对象
        updateModelParam(template)

        // 2. 决策：根据数据同步状态，选择更新路径
        val v2LatestTemplateResource = pipelineTemplateResourceService.getLatestReleasedResource(projectId, templateId)
        val needsMigration = v2LatestTemplateResource == null ||
            v2LatestTemplateResource.version != v1LatestTemplate.version

        val version = if (needsMigration) {
            // 路径1: V1数据已过时或V2数据不存在，需更新V1并触发迁移
            logger.info("Template $templateId is out of sync or new, using V1 compatibility update path.")
            updateV1AndTriggerMigration(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                versionName = versionName,
                template = template,
                v1LatestTemplate = v1LatestTemplate,
                checkPermissionFlag = checkPermissionFlag
            )
        } else {
            // 路径2: 数据同步，直接走V2标准更新流程
            logger.info("Template $templateId is in sync, using V2 standard update path.")
            updateV2Standard(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                versionName = versionName,
                template = template,
                v1LatestTemplate = v1LatestTemplate
            )
        }
        logger.info("Get the update template version $version")
        return version
    }

    /**
     * 兼容模式：更新V1表，并触发到V2的异步迁移
     */
    private fun updateV1AndTriggerMigration(
        userId: String,
        projectId: String,
        templateId: String,
        versionName: String,
        template: Model,
        v1LatestTemplate: TTemplateRecord,
        checkPermissionFlag: Boolean
    ): Long {
        var newVersion = 0L
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            pipelineSettingDao.updateSetting(
                dslContext = context,
                projectId = projectId,
                pipelineId = templateId,
                name = template.name,
                desc = template.desc ?: ""
            )

            // 清理超出数量限制的旧版本记录
            // 新版本不清理同个版本名称的记录
//            val saveRecordVersions = templateDao.listSaveRecordVersions(
//                context, projectId, templateId, versionName, maxSaveVersionRecordNum
//            )
//            if (saveRecordVersions?.isNotEmpty == true) {
//                templateDao.deleteSpecVersion(
//                    dslContext = context,
//                    projectId = projectId,
//                    templateId = templateId,
//                    versionName = versionName,
//                    saveVersions = saveRecordVersions.map { it.value1() }
//                )
//            }

            // 在v1表中创建新版本
            newVersion = templateDao.createTemplate(
                dslContext = context,
                projectId = projectId,
                templateId = templateId,
                templateName = template.name,
                versionName = versionName,
                userId = userId,
                template = JsonUtil.toJson(template, formatted = false),
                type = v1LatestTemplate.type,
                category = v1LatestTemplate.category,
                logoUrl = v1LatestTemplate.logoUrl,
                srcTemplateId = v1LatestTemplate.srcTemplateId,
                storeFlag = v1LatestTemplate.storeFlag,
                weight = v1LatestTemplate.weight,
                version = client.get(ServiceAllocIdResource::class).generateSegmentId(TEMPLATE_BIZ_TAG_NAME).data,
                desc = template.desc
            )

            if (checkPermissionFlag) {
                pipelineTemplatePermissionService.modifyResource(userId, projectId, templateId, template.name)
            }
        }

        // 触发异步迁移
        pipelineEventDispatcher.dispatch(
            PipelineTemplateMigrateEvent(
                projectId = projectId,
                source = "PIPELINE_TEMPLATE_MIGRATE",
                pipelineId = "",
                userId = userId,
                templateId = templateId,
            )
        )
        return newVersion
    }

    /**
     * 标准模式：直接使用V2管理器更新模板
     */
    private fun updateV2Standard(
        userId: String,
        projectId: String,
        templateId: String,
        versionName: String,
        template: Model,
        v1LatestTemplate: TTemplateRecord
    ): Long {
        val request = PipelineTemplateCompatibilityCreateReq(
            model = template,
            setting = PipelineSetting(
                projectId = projectId,
                pipelineId = templateId,
                pipelineName = v1LatestTemplate.templateName,
                desc = template.desc ?: "",
                pipelineAsCodeSettings = null,
                creator = userId,
                updater = userId
            ),
            v1VersionName = versionName,
            category = v1LatestTemplate.category,
            logoUrl = v1LatestTemplate.logoUrl
        )
        pipelineTemplateVersionManager.deployTemplate(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            request = request
        )
        // 部署后，重新获取最新的版本信息
        return pipelineTemplateResourceService.getLatestReleasedResource(projectId, templateId)!!.version
    }

    fun listTemplate(
        projectId: String,
        userId: String,
        templateType: TemplateType?,
        storeFlag: Boolean?,
        orderBy: PTemplateOrderByType? = null,
        sort: PTemplateSortType? = null,
        page: Int? = null,
        pageSize: Int? = null,
        keywords: String? = null
    ): TemplateListModel {
        logger.info("[$projectId|$userId|$templateType|$storeFlag|$page|$pageSize|$keywords] List template")
        val hasManagerPermission = templateCommonService.hasManagerPermission(projectId, userId)
        val result = ArrayList<TemplateModel>()
        val templateWithPermission = getTemplateListAndCount(
            userId = userId,
            projectId = projectId,
            templateType = templateType,
            storeFlag = storeFlag,
            hasManagerPermission = hasManagerPermission,
            orderBy = orderBy,
            sort = sort,
            page = page,
            pageSize = pageSize,
            templateIds = null
        )
        logger.info("get templates and count :$templateWithPermission")
        fillResult(
            context = dslContext,
            templates = templateWithPermission.templatesWithListPermRecords,
            hasManagerPermission = hasManagerPermission,
            userId = userId,
            templateType = templateType,
            storeFlag = storeFlag,
            page = page,
            pageSize = pageSize,
            keywords = keywords,
            result = result,
            templateWithPermission = templateWithPermission,
            projectId = projectId
        )

        val hasCreatePermission = pipelineTemplatePermissionService.checkPipelineTemplatePermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.CREATE
        )
        val enableTemplatePermissionManage = enableTemplatePermissionManage(projectId = projectId)
        return TemplateListModel(
            projectId = projectId,
            hasPermission = hasManagerPermission,
            models = result,
            count = templateWithPermission.count,
            hasCreatePermission = hasCreatePermission,
            enableTemplatePermissionManage = enableTemplatePermissionManage
        )
    }

    private fun getTemplateListAndCount(
        userId: String?,
        projectId: String?,
        templateType: TemplateType?,
        storeFlag: Boolean?,
        hasManagerPermission: Boolean?,
        checkPermission: Boolean = true,
        orderBy: PTemplateOrderByType? = null,
        sort: PTemplateSortType? = null,
        page: Int?,
        pageSize: Int?,
        includePublicFlag: Boolean? = null,
        templateIds: Collection<String>?
    ): TemplateWithPermission {
        val offset = if (null != page && null != pageSize) (page - 1) * pageSize else null
        // 若不开启模板权限或者不检查列表权限的，直接返回列表数据。
        return if (!checkPermission || !enableTemplatePermissionManage(projectId!!)) {
            logger.info("get templates without permission :$projectId|$userId|$checkPermission")
            getTemplateListAndCountWithoutPermission(
                projectId = projectId,
                includePublicFlag = includePublicFlag,
                templateType = templateType,
                storeFlag = storeFlag,
                orderBy = orderBy,
                sort = sort,
                offset = offset,
                pageSize = pageSize,
                hasManagerPermission = hasManagerPermission,
                templateIds = templateIds
            )
        } else {
            logger.info("get templates with permission :$projectId|$userId|$checkPermission")
            val templatesByPermissionMap = pipelineTemplatePermissionService.getResourcesByPermission(
                userId = userId!!,
                projectId = projectId,
                permissions = setOf(
                    AuthPermission.VIEW,
                    AuthPermission.LIST,
                    AuthPermission.DELETE,
                    AuthPermission.EDIT
                )
            )
            getTemplateListAndCountWithPermission(
                projectId = projectId,
                includePublicFlag = includePublicFlag,
                templateType = templateType,
                storeFlag = storeFlag,
                orderBy = orderBy,
                sort = sort,
                offset = offset,
                pageSize = pageSize,
                templatesByPermissionMap = templatesByPermissionMap
            )
        }
    }

    private fun getTemplateListAndCountWithoutPermission(
        projectId: String?,
        includePublicFlag: Boolean?,
        templateType: TemplateType?,
        storeFlag: Boolean?,
        orderBy: PTemplateOrderByType?,
        sort: PTemplateSortType?,
        offset: Int?,
        pageSize: Int?,
        hasManagerPermission: Boolean?,
        templateIds: Collection<String>?
    ): TemplateWithPermission {
        val tTemplate = TTemplate.T_TEMPLATE
        val templateRecord = templateDao.listTemplate(
            dslContext = dslContext,
            projectId = projectId,
            includePublicFlag = includePublicFlag,
            templateType = templateType,
            templateIdList = templateIds,
            storeFlag = storeFlag,
            orderBy = orderBy,
            sort = sort,
            offset = offset,
            limit = pageSize,
            queryModelFlag = true
        )
        val count = templateDao.countTemplate(
            dslContext = dslContext,
            projectId = projectId,
            includePublicFlag = includePublicFlag,
            templateType = templateType,
            templateName = null,
            storeFlag = storeFlag
        )
        val templatesWithPermIds = if (hasManagerPermission == true) {
            templateRecord?.map { it[tTemplate.ID] }
        } else {
            emptyList()
        }
        return TemplateWithPermission(
            templatesWithListPermRecords = templateRecord,
            templatesWithViewPermIds = templatesWithPermIds,
            templatesWithEditPermIds = templatesWithPermIds,
            templatesWithDeletePermIds = templatesWithPermIds,
            count = count
        )
    }

    private fun getTemplateListAndCountWithPermission(
        projectId: String,
        includePublicFlag: Boolean?,
        templateType: TemplateType?,
        storeFlag: Boolean?,
        orderBy: PTemplateOrderByType?,
        sort: PTemplateSortType?,
        offset: Int?,
        pageSize: Int?,
        templatesByPermissionMap: Map<AuthPermission, List<String>>
    ): TemplateWithPermission {
        val templatesWithDeletePermIds = templatesByPermissionMap[AuthPermission.DELETE]
        val templatesWithEditPermIds = templatesByPermissionMap[AuthPermission.EDIT]
        val templatesWithViewPermIds = templatesByPermissionMap[AuthPermission.VIEW]
        // 公共模板如 流水线空白模板，不需要权限校验，直接返回
        val templatesWithListPermIds = if (includePublicFlag == true) {
            templateDao.getPublicTemplate(dslContext = dslContext)
        } else {
            emptyList()
        }.toMutableList().apply { addAll(templatesByPermissionMap[AuthPermission.LIST] ?: emptyList()) }

        if (templatesWithListPermIds.isEmpty()) {
            return TemplateWithPermission(
                templatesWithListPermRecords = null,
                templatesWithViewPermIds = null,
                templatesWithEditPermIds = null,
                templatesWithDeletePermIds = null,
                count = 0
            )
        }

        val templatesWithListPermRecords = templateDao.listTemplate(
            dslContext = dslContext,
            projectId = projectId,
            includePublicFlag = includePublicFlag,
            templateType = templateType,
            templateIdList = templatesWithListPermIds,
            storeFlag = storeFlag,
            orderBy = orderBy,
            sort = sort,
            offset = offset,
            limit = pageSize,
            queryModelFlag = true
        )
        return TemplateWithPermission(
            templatesWithListPermRecords = templatesWithListPermRecords,
            templatesWithViewPermIds = templatesWithViewPermIds,
            templatesWithEditPermIds = templatesWithEditPermIds,
            templatesWithDeletePermIds = templatesWithDeletePermIds,
            count = templatesWithListPermIds.size
        )
    }

    fun getSrcTemplateCodes(projectId: String): List<String> {
        return templateDao.getSrcTemplateCodes(dslContext, projectId)
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
        templateWithPermission: TemplateWithPermission,
        projectId: String? = null
    ) {
        if (templates == null || templates.isEmpty()) {
            // 如果查询模板列表为空，则不再执行后续逻辑
            return
        }
        val tTemplate = TTemplate.T_TEMPLATE
        val templateIdList = mutableSetOf<String>()
        val srcTemplates = getConstrainedSrcTemplates(context, templates, templateIdList)

        val settings = pipelineSettingDao.getSettings(
            dslContext = context,
            pipelineIds = templateIdList,
            projectId = projectId
        ).associateBy { it.pipelineId }
        templates.forEach { record ->
            val templateId = record[tTemplate.ID]
            val type = record[tTemplate.TYPE]

            val templateRecord = if (type == TemplateType.CONSTRAINT.name) {
                val srcTemplateId = record[tTemplate.SRC_TEMPLATE_ID]
                srcTemplates?.get(srcTemplateId)
            } else {
                record
            }
            if (templateRecord == null) {
                logger.warn("src store template not exist:$templateId|$type|$record")
                throw ErrorCodeException(
                    errorCode = ERROR_TEMPLATE_NOT_EXISTS,
                    defaultMessage = "$templateId of src store template not exist:"
                )
            } else {
                val modelStr = templateRecord[tTemplate.TEMPLATE] as String
                val version = templateRecord[tTemplate.VERSION] as Long
                val model: Model = objectMapper.readValue(modelStr)

                val setting = settings[templateId]
                val templateName = setting?.pipelineName ?: model.name

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
                        hasPermission = hasManagerPermission,
                        canView = templateWithPermission.templatesWithViewPermIds?.contains(templateId),
                        canDelete = templateWithPermission.templatesWithDeletePermIds?.contains(templateId),
                        canEdit = templateWithPermission.templatesWithEditPermIds?.contains(templateId),
                        creator = record[tTemplate.CREATOR],
                        updateTime = record[tTemplate.UPDATE_TIME].timestampmilli()
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
            try {
                if (template[tTemplate.TYPE] == TemplateType.CONSTRAINT.name) {
                    constrainedTemplateList.add(template[tTemplate.SRC_TEMPLATE_ID])
                }
            } catch (ex: Exception) {
                logger.warn("get constrained src templates failed {}", template[tTemplate.ID])
            }
            // 非研发商店流水线模板ID集合
            templateIdList.add(template[tTemplate.ID])
        }
        val srcTemplateRecords = if (constrainedTemplateList.isNotEmpty()) templateDao.listTemplate(
            dslContext = context,
            projectId = null,
            includePublicFlag = null,
            templateType = null,
            templateIdList = constrainedTemplateList,
            storeFlag = null,
            offset = null,
            limit = null,
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
        userId: String? = null,
        projectId: String?,
        templateType: TemplateType?,
        templateIds: Collection<String>?,
        page: Int? = null,
        pageSize: Int? = null,
        checkPermission: Boolean = true
    ): OptionalTemplateList {
        logger.info("[$projectId|$templateType|$templateIds|$page|$pageSize] List template")
        val result = mutableMapOf<String, OptionalTemplate>()
        val templateWithPermission = getTemplateListAndCount(
            userId = userId,
            projectId = projectId,
            templateType = templateType,
            storeFlag = null,
            hasManagerPermission = null,
            checkPermission = checkPermission,
            page = page,
            pageSize = pageSize,
            includePublicFlag = true,
            templateIds = templateIds
        )
        val templates = templateWithPermission.templatesWithListPermRecords
        val templateCount = templateWithPermission.count
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
        ).associateBy { it.pipelineId }
        val pipelinesWithLabels = projectId?.let {
            pipelineLabelPipelineDao.exitsLabelPipelines(dslContext, projectId, templateIdList)
        }
        val tTemplate = TTemplate.T_TEMPLATE
        templates.forEach { record ->
            val type = record[tTemplate.TYPE]
            val srcTemplateId = record[tTemplate.SRC_TEMPLATE_ID]
            val templateId = record[tTemplate.ID]
            val setting = settings[templateId]
            val key = if (type == TemplateType.CONSTRAINT.name) srcTemplateId else templateId
            val optionalTemplateInfo = generateOptionalTemplate(
                setting = setting,
                record = record,
                pipelinesWithLabels = pipelinesWithLabels,
                srcTemplates = srcTemplates
            ) ?: return@forEach
            result[key] = optionalTemplateInfo
        }
        return OptionalTemplateList(
            count = templateCount,
            page = page,
            pageSize = pageSize,
            templates = result
        )
    }

    private fun generateOptionalTemplate(
        setting: PipelineSetting?,
        record: Record,
        pipelinesWithLabels: Set<String>?,
        srcTemplates: Map<String, Record>?
    ): OptionalTemplate? {
        val tTemplate = TTemplate.T_TEMPLATE
        val templateId = record[tTemplate.ID]
        val type = record[tTemplate.TYPE]
        val srcTemplateId = record[tTemplate.SRC_TEMPLATE_ID]
        val templateRecord = if (type == TemplateType.CONSTRAINT.name) {
            srcTemplates?.get(srcTemplateId)
        } else {
            record
        }
        return if (templateRecord != null) {
            val modelStr = templateRecord[tTemplate.TEMPLATE]
            val version = templateRecord[tTemplate.VERSION] as Long
            val model: Model = objectMapper.readValue(modelStr)
            val logoUrl = record[tTemplate.LOGO_URL]
            val categoryStr = record[tTemplate.CATEGORY]
            OptionalTemplate(
                name = generateI18nTemplateName(templateId = templateId, templateType = type) ?: setting?.pipelineName
                ?: model.name,
                templateId = templateId,
                srcTemplateId = srcTemplateId,
                projectId = templateRecord[tTemplate.PROJECT_ID],
                version = version,
                versionName = templateRecord[tTemplate.VERSION_NAME],
                templateType = type,
                templateTypeDesc = TemplateType.getTemplateTypeDesc(type),
                logoUrl = logoUrl ?: "",
                category = try {
                    if (!categoryStr.isNullOrBlank()) JsonUtil.getObjectMapper()
                        .readValue(categoryStr, List::class.java) as List<String> else listOf()
                } catch (ex: Exception) {
                    emptyList()
                },
                stages = model.stages,
                cloneTemplateSettingExist = CloneTemplateSettingExist.fromSetting(
                    setting, pipelinesWithLabels
                ),
                desc = record[tTemplate.DESC]
            )
        } else {
            null
        }
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
            } ?: throw ErrorCodeException(
                errorCode = ERROR_TEMPLATE_NOT_EXISTS
            )
        }
        val currentVersion = TemplateVersion(
            template.version,
            template.versionName,
            template.createdTime.timestampmilli(),
            template.updateTime.timestampmilli(),
            template.creator
        )
        val model: Model = objectMapper.readValue(template.template)
        model.name = setting.pipelineName
        model.desc = setting.desc
        val groups = pipelineGroupService.getGroups(userId, projectId, templateId)
        val labels = ArrayList<String>()
        groups.forEach {
            labels.addAll(it.labels)
        }
        model.labels = labels
        val templateResult = instanceParamModel(userId, projectId, model)
        val latestVersion = TemplateVersion(
            version = latestTemplate.version,
            versionName = latestTemplate.versionName,
            createTime = latestTemplate.createdTime.timestampmilli(),
            updateTime = latestTemplate.updateTime.timestampmilli(),
            creator = latestTemplate.creator
        )
        val versions = listTemplateVersions(latestTemplate.projectId, latestTemplate.id)
        val triggerContainer = templateResult.getTriggerContainer()
        val params = triggerContainer.params
        val templateParams = triggerContainer.templateParams
        return TemplateModelDetail(
            versions = versions,
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            templateName = setting.pipelineName,
            description = setting.desc,
            creator = if (isConstrainedFlag) latestTemplate.creator else template.creator,
            template = templateResult,
            templateType = if (isConstrainedFlag) TemplateType.CONSTRAINT.name else template.type,
            logoUrl = if (isConstrainedFlag) latestTemplate.logoUrl ?: "" else {
                if (template.logoUrl.isNullOrEmpty()) "" else template.logoUrl
            },
            hasPermission = pipelineTemplatePermissionService.checkPipelineTemplatePermission(
                userId = userId,
                projectId = projectId,
                permission = AuthPermission.EDIT,
                templateId = templateId
            ),
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

    fun listTemplateVersions(
        projectId: String,
        templateId: String,
        ascSort: Boolean = false
    ): List<TemplateVersion> {
        val versionInfos = templateDao.getTemplateVersionInfos(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId,
            ascSort = ascSort
        )
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
                        createTime = versionInfo[tTemplate.CREATED_TIME].timestampmilli(),
                        updateTime = versionInfo[tTemplate.UPDATE_TIME].timestampmilli(),
                        creator = versionInfo[tTemplate.CREATOR]
                    )
                )
            }
            versionNames.add(versionName)
        }
        return if (ascSort) {
            versions.sortedBy { templateVersion -> templateVersion.updateTime }
        } else {
            versions.sortedByDescending { templateVersion -> templateVersion.updateTime }
        }
    }

    /**
     * 获取模板所有的版本
     *
     * @param projectId 项目ID
     * @param templateId 模板ID
     * @param ascSort 是否按更新时间升序排序
     * @return 模板版本列表，重复的 versionName 会被添加后缀（如: "-1", "-2"）
     */
    fun listTemplateAllVersions(
        projectId: String,
        templateId: String,
        ascSort: Boolean = false
    ): List<TemplateVersion> {
        val versionInfos = templateDao.getTemplateVersionInfos(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId,
            ascSort = ascSort
        ) ?: return emptyList()

        val tTemplate = TTemplate.T_TEMPLATE

        // --- 步骤 1: 准备数据并排序 ---
        // 先将所有记录转换为 TemplateVersion 对象，并确保按 updateTime 排序。
        // 排序是必须的，以保证后缀（-1, -2）是按时间顺序应用的。
        val sortedVersions = versionInfos.map { versionInfo ->
            TemplateVersion(
                version = versionInfo[tTemplate.VERSION],
                versionName = versionInfo[tTemplate.VERSION_NAME],
                createTime = versionInfo[tTemplate.CREATED_TIME].timestampmilli(),
                updateTime = versionInfo[tTemplate.UPDATE_TIME].timestampmilli(),
                creator = versionInfo[tTemplate.CREATOR]
            )
        }.let { versions ->
            // 使用 let 块来应用排序，代码更清晰
            if (ascSort) {
                versions.sortedBy { it.updateTime }
            } else {
                versions.sortedByDescending { it.updateTime }
            }
        }

        // --- 步骤 2: 预计算每个 versionName 的总数 ---
        // 使用 groupingBy().eachCount() 是最高效、最Kotlin风格的方式。
        val totalNameCounts = sortedVersions.groupingBy { it.versionName }.eachCount()

        // --- 步骤 3: 最终转换，根据预计算结果进行重命名 ---
        // 用于为重复项生成递增后缀（如：-1, -2, ...）
        val runningDuplicateCounters = mutableMapOf<String, Int>()

        return sortedVersions.map { version ->
            val originalName = version.versionName
            val totalCount = totalNameCounts[originalName] ?: 1

            // 如果总数大于1，说明它是重复项，需要重命名
            if (totalCount > 1) {
                // 获取当前名称的后缀计数，从 1 开始
                val suffixIndex = runningDuplicateCounters.getOrDefault(originalName, 0) + 1
                // 更新计数器，为下一个同名项做准备
                runningDuplicateCounters[originalName] = suffixIndex

                // 使用 copy 创建一个带有新名称的新实例
                version.copy(versionName = "$originalName-$suffixIndex")
            } else {
                // 如果总数不大于1，说明它是唯一的，保持原样
                version
            }
        }
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
                )
            )
        val templateRecord = templateDao.getTemplate(dslContext = dslContext, version = templatePipelineRecord.version)
            ?: throw ErrorCodeException(
                errorCode = ERROR_TEMPLATE_NOT_EXISTS
            )
        val template: Model = objectMapper.readValue(
            templateRecord.template
        )
        val v1Model: Model = instanceCompareModel(
            objectMapper.readValue(
                content = pipelineResourceDao.getVersionModelString(dslContext, projectId, pipelineId, null)
                    ?: throw ErrorCodeException(
                        statusCode = Response.Status.NOT_FOUND.statusCode,
                        errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
                    )
            ),
            template
        )

        val srcTemplate = templateDao.getTemplate(dslContext = dslContext, version = version)
            ?: throw ErrorCodeException(
                errorCode = ERROR_TEMPLATE_NOT_EXISTS
            )
        val v2Model = getTemplateModel(srcTemplate.template)
        val v1Containers = getContainers(v1Model)
        val v2Containers = getContainers(v2Model)

        compareContainer(v1Containers, v2Containers)
        val versions = listTemplateVersions(srcTemplate.projectId, srcTemplate.id)
        return compareModel(versions, v1Model, v2Model)
    }

    fun compareModel(versions: List<TemplateVersion>, v1Model: Model, v2Model: Model): TemplateCompareModelResult {
        val v1TriggerContainer = v1Model.getTriggerContainer()
        val v2TriggerContainer = v2Model.getTriggerContainer()
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
                        if (element.differ(v2Element)) {
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
                ?: throw ErrorCodeException(
                    errorCode = ERROR_TEMPLATE_NOT_EXISTS
                )
            val templateModel: Model = objectMapper.readValue(template.template)
            val templateTriggerContainer = templateModel.getTriggerContainer()
            val latestInstances = listLatestModel(projectId, pipelineIds)
            val settings = pipelineSettingDao.getSettings(dslContext, pipelineIds, projectId)
            val buildNos = pipelineBuildSummaryDao.getSummaries(dslContext, projectId, pipelineIds).map {
                it.pipelineId to it.buildNo
            }.toMap()

            return latestInstances.map {
                val pipelineId = it.key
                val instanceModel: Model = objectMapper.readValue(it.value)
                val instanceTriggerContainer = instanceModel.getTriggerContainer()
                val instanceParams = paramService.filterParams(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    params = removeProperties(templateTriggerContainer.params, instanceTriggerContainer.params)
                )
                logger.info("[$userId|$projectId|$templateId|$version] Get the param ($instanceParams)")

                // 模板中的buildNo存在才需要回显
                // 将实例自己维护的当前值一起返回
                val instanceBuildNoObj = templateTriggerContainer.buildNo?.let { no ->
                    BuildNo(
                        buildNoType = no.buildNoType,
                        required = no.required ?: instanceTriggerContainer.buildNo?.required,
                        buildNo = no.buildNo,
                        currentBuildNo = buildNos[pipelineId]
                    )
                }

                pipelineId to TemplateInstanceParams(
                    pipelineId = pipelineId,
                    pipelineName = getPipelineName(settings, pipelineId) ?: templateModel.name,
                    buildNo = instanceBuildNoObj,
                    param = instanceParams,
                    updateBuildNo = instanceTriggerContainer.buildNo?.let { ino ->
                        ino.buildNo != templateModel.getTriggerContainer().buildNo?.buildNo
                    }
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
            ?: throw ErrorCodeException(
                errorCode = ERROR_TEMPLATE_NOT_EXISTS
            )
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
                // TODO #9145 创建流水线实例时的yaml覆盖逻辑
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
                ).pipelineId
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)
                    if (useTemplateSettings) {
                        val setting = templateSettingService.copySetting(
                            setting = templateSettingService.getTemplateSetting(
                                projectId = projectId,
                                userId = userId,
                                templateId = templateId
                            ),
                            pipelineId = pipelineId,
                            templateName = instance.pipelineName
                        )
                        pipelineSettingFacadeService.saveSetting(
                            context = context,
                            userId = userId,
                            projectId = setting.projectId,
                            pipelineId = setting.pipelineId,
                            setting = setting
                        )
                    } else {
                        val defaultSetting = templateCommonService.getDefaultSetting(
                            projectId = projectId,
                            templateId = pipelineId,
                            templateName = pipelineName
                        )
                        pipelineSettingFacadeService.saveSetting(
                            context = context,
                            userId = userId,
                            projectId = defaultSetting.projectId,
                            pipelineId = defaultSetting.pipelineId,
                            setting = defaultSetting
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
        ) ?: throw ErrorCodeException(
            errorCode = ERROR_TEMPLATE_NOT_EXISTS
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
                val setting = templateSettingService.copySetting(
                    setting = templateSettingService.getTemplateSetting(
                        projectId = projectId,
                        userId = userId,
                        templateId = templateId
                    ),
                    pipelineId = templateInstanceUpdate.pipelineId,
                    templateName = templateInstanceUpdate.pipelineName
                )
                pipelineSettingFacadeService.saveSetting(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = templateInstanceUpdate.pipelineId,
                    setting = setting,
                    checkPermission = true,
                    dispatchPipelineUpdateEvent = false
                )
            } else {
                // 不应用模板设置但是修改了流水线名称,需要重命名流水线
                val setting = pipelineSettingDao.getSetting(
                    dslContext = context,
                    projectId = projectId,
                    pipelineId = templateInstanceUpdate.pipelineId
                ) ?: throw ErrorCodeException(
                    errorCode = ProcessMessageCode.PIPELINE_SETTING_NOT_EXISTS
                )
                if (setting.pipelineName != templateInstanceUpdate.pipelineName) {
                    setting.pipelineName = templateInstanceUpdate.pipelineName
                    pipelineSettingFacadeService.saveSetting(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = templateInstanceUpdate.pipelineId,
                        setting = setting,
                        checkPermission = true,
                        dispatchPipelineUpdateEvent = false
                    )
                }
            }
            val result = pipelineInfoFacadeService.editPipeline(
                userId = userId,
                projectId = projectId,
                pipelineId = templateInstanceUpdate.pipelineId,
                model = instanceModel,
                // TODO #9145 修改流水线实例时的yaml覆盖逻辑
                yaml = null,
                channelCode = ChannelCode.BS,
                checkPermission = true,
                checkTemplate = false
            )

            templatePipelineVersionService.createOrUpdate(
                record = PTemplatePipelineVersion(
                    projectId = projectId,
                    pipelineId = result.pipelineId,
                    pipelineVersion = result.version,
                    pipelineVersionName = result.versionName ?: "",
                    instanceType = PipelineInstanceTypeEnum.CONSTRAINT,
                    buildNo = templateInstanceUpdate.buildNo,
                    params = templateInstanceUpdate.param,
                    refType = TemplateRefType.ID,
                    inputTemplateId = templateId,
                    inputTemplateVersionName = versionName,
                    templateId = templateId,
                    templateVersion = templateVersion,
                    templateVersionName = versionName,
                    creator = userId,
                    updater = userId
                )
            )
            templateInstanceUpdate.buildNo?.let {
                if (templateInstanceUpdate.resetBuildNo == true) {
                    pipelineInfoFacadeService.updateBuildNo(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = templateInstanceUpdate.pipelineId,
                        targetBuildNo = it.buildNo
                    )
                }
            }
        }
    }

    fun updateInstanceErrorInfo(
        projectId: String,
        pipelineId: String,
        errorInfo: String?
    ) {
        if (errorInfo == null) return
        try {
            templatePipelineDao.updateInstanceErrorInfo(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                errorInfo = errorInfo
            )
        } catch (ignored: Exception) {
            logger.warn("Failed to update instance error info|$projectId|$pipelineId$errorInfo")
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
        val template = templateDao.getTemplate(dslContext = dslContext, version = version)
            ?: throw ErrorCodeException(
                errorCode = ERROR_TEMPLATE_NOT_EXISTS
            )
        val templateModel: Model = objectMapper.readValue(template.template)
        checkTemplateAtomsForExplicitVersion(templateModel, userId)
        // 当更新的实例数量较小则走同步更新逻辑，较大走异步更新逻辑
        if (instances.size <= maxSyncInstanceNum) {
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
                    // ERROR_PIPELINE_ELEMENT_CHECK_FAILED输出的是一个json,需要格式化输出
                    val reason = if (exception.errorCode == ERROR_PIPELINE_ELEMENT_CHECK_FAILED) {
                        JsonUtil.to(message, PipelineCheckFailedErrors::class.java)
                    } else {
                        PipelineCheckFailedMsg(message)
                    }
                    updateInstanceErrorInfo(
                        projectId = projectId,
                        pipelineId = templateInstanceUpdate.pipelineId,
                        errorInfo = JsonUtil.toJson(reason, false)
                    )
                    failurePipelines.add("【${templateInstanceUpdate.pipelineName}】reason：${reason.message}")
                } catch (t: Throwable) {
                    val message =
                        if (!t.message.isNullOrBlank() && t.message!!.length > maxErrorReasonLength)
                            t.message!!.substring(0, maxErrorReasonLength) + "......" else t.message
                    message?.let {
                        updateInstanceErrorInfo(
                            projectId = projectId,
                            pipelineId = templateInstanceUpdate.pipelineId,
                            errorInfo = JsonUtil.toJson(PipelineCheckFailedMsg(it), false)
                        )
                    }
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
                    status = TemplateInstanceStatus.INIT.name,
                    userId = userId,
                    gray = false
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

    /**
     * 删除流水线参数中的流水线常量
     */
    private fun instanceCompareModel(
        instance: Model,
        template: Model
    ): Model {
        val templateParams = (template.getTriggerContainer()).templateParams
        if (templateParams.isNullOrEmpty()) {
            return instance
        }
        val triggerContainer = instance.getTriggerContainer()
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

    private fun instanceParamModel(userId: String, projectId: String, model: Model): Model {
        val triggerContainer = model.getTriggerContainer()
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

    fun hasPipelineTemplatePermission(
        userId: String,
        projectId: String,
        templateId: String?,
        permission: AuthPermission
    ): Boolean {
        if (!enableTemplatePermissionManage(projectId)) {
            return true
        }
        return pipelineTemplatePermissionService.checkPipelineTemplatePermission(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            permission = permission
        )
    }

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
                     * 3. 如果模版由常量改成变量,则流水线常量也应该改成变量
                     */
                    if (pipeline.type != template.type) {
                        result.add(template)
                    } else {
                        pipeline.options = template.options
                        pipeline.required = template.required
                        pipeline.desc = template.desc
                        pipeline.constant = template.constant
                        pipeline.displayCondition = template.displayCondition
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
            val instanceErrorInfo = it[KEY_INSTANCE_ERROR_INFO] as String?
            val templatePipelineStatus = generateTemplatePipelineStatus(
                templateInstanceItems = templateInstanceItems,
                templatePipelineId = pipelineId,
                templatePipelineVersion = templatePipelineVersion,
                version = version,
                instanceErrorInfo = instanceErrorInfo
            )
            TemplatePipeline(
                templateId = it[KEY_TEMPLATE_ID] as String,
                versionName = it[KEY_VERSION_NAME] as String,
                version = templatePipelineVersion,
                pipelineId = pipelineId,
                pipelineName = pipelineSetting[0].pipelineName,
                updateTime = (it[KEY_UPDATED_TIME] as LocalDateTime).timestampmilli(),
                hasPermission = hasPermissionList.contains(pipelineId),
                status = templatePipelineStatus,
                instanceErrorInfo = instanceErrorInfo
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
        val hasCreateTemplateInstancePerm = pipelineTemplatePermissionService.hasCreateTemplateInstancePermission(
            userId = userId,
            projectId = projectId
        )

        return TemplateInstancePage(
            projectId = projectId,
            templateId = templateId,
            instances = if (desc == true) sortTemplatePipelines.reversed() else sortTemplatePipelines,
            latestVersion = TemplateVersion(
                version = latestVersion.version,
                versionName = latestVersion.versionName,
                createTime = latestVersion.createdTime.timestampmilli(),
                updateTime = latestVersion.updateTime.timestampmilli(),
                creator = latestVersion.creator
            ),
            count = instancePage.count.toInt(),
            page = page,
            pageSize = pageSize,
            hasCreateTemplateInstancePerm = hasCreateTemplateInstancePerm
        )
    }

    fun generateTemplatePipelineStatus(
        templateInstanceItems: Result<TTemplateInstanceItemRecord>?,
        templatePipelineId: String,
        templatePipelineVersion: Long,
        version: Long,
        instanceErrorInfo: String?
    ): TemplatePipelineStatus {
        return when {
            templateInstanceItems?.any { it.pipelineId == templatePipelineId } ?: false ->
                TemplatePipelineStatus.UPDATING

            !instanceErrorInfo.isNullOrBlank() ->
                TemplatePipelineStatus.FAILED

            templatePipelineVersion != version ->
                TemplatePipelineStatus.PENDING_UPDATE

            else ->
                TemplatePipelineStatus.UPDATED
        }
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
    private fun checkTemplate(template: Model, projectId: String? = null, userId: String) {
        if (template.name.isBlank()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.TEMPLATE_NAME_CAN_NOT_NULL
            )
        }
        // 模版先都统一使用项目配置
        val projectDialect = projectId?.let { pipelineAsCodeService.getProjectDialect(projectId = it) }
        modelCheckPlugin.checkModelIntegrity(
            model = template,
            projectId = projectId,
            userId = userId,
            isTemplate = true,
            pipelineDialect = projectDialect
        )
        checkPipelineParam(template)
    }

    /**
     * 检查模板中是否存在已下架、测试中插件(明确版本号)
     */
    fun checkTemplateAtomsForExplicitVersion(template: Model, userId: String) {
        val codeVersions = mutableSetOf<AtomCodeVersionReqItem>()
        template.stages.forEach { stage ->
            stage.containers.forEach { container ->
                container.elements.forEach nextElement@{ element ->
                    val atomCode = element.getAtomCode()
                    val version = element.version
                    if (version.contains("*")) {
                        return@nextElement
                    }
                    codeVersions.add(AtomCodeVersionReqItem(atomCode, version))
                }
            }
        }
        if (codeVersions.isNotEmpty()) {
            AtomUtils.checkTemplateRealVersionAtoms(
                codeVersions = codeVersions,
                userId = userId,
                client = client
            )
        }
    }

    fun checkTemplate(templateId: String, projectId: String? = null, userId: String): Boolean {
        val templateRecord = if (projectId.isNullOrEmpty()) {
            templateDao.getLatestTemplate(dslContext, templateId)
        } else {
            templateDao.getLatestTemplate(dslContext, projectId, templateId)
        }
        val modelStr = templateRecord.template
        if (modelStr != null) {
            val model = JsonUtil.to(modelStr, Model::class.java)
            checkTemplate(template = model, projectId = projectId, userId = userId)
        }
        return true
    }

    /**
     * 模板的流水线变量和模板常量不能相同
     */
    private fun checkPipelineParam(template: Model) {
        val triggerContainer = template.getTriggerContainer()

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

    private fun getPipelineName(records: List<PipelineSetting>, pipelineId: String): String? {
        records.forEach {
            if (it.pipelineId == pipelineId) {
                return it.pipelineName
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
                if (container is TriggerContainer) {
                    container.params = PipelineUtils.cleanOptions(params = container.params)
                    container.templateParams = container.templateParams?.let { PipelineUtils.cleanOptions(params = it) }
                }
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

    fun listLatestModel(projectId: String, pipelineIds: Set<String>): Map<String/*Pipeline ID*/, String/*Model*/> {
        val modelResources = pipelineResourceDao.listLatestModelResource(dslContext, pipelineIds, projectId)
        return modelResources?.map { modelResource ->
            modelResource.value1() to modelResource.value3()
        }?.toMap() ?: mapOf()
    }

    fun addMarketTemplate(
        userId: String,
        projectId: String,
        addMarketTemplateRequest: MarketTemplateRequest
    ): Map<String, String> {
        logger.info("the userId is:$userId,addMarketTemplateRequest is:$addMarketTemplateRequest")
        val templateCode = addMarketTemplateRequest.templateCode
        val publicFlag = addMarketTemplateRequest.publicFlag // 是否为公共模板
        val category = JsonUtil.toJson(addMarketTemplateRequest.categoryCodeList ?: listOf<String>(), false)
        // 校验安装的模板是否合法
        if (!publicFlag && redisOperation.get("checkInstallTemplateModelSwitch")?.toBoolean() != false) {
            val templateRecord = templateDao.getLatestTemplate(dslContext, templateCode)
            val modelStr = templateRecord.template
            if (modelStr != null) {
                val model = JsonUtil.to(modelStr, Model::class.java)
                checkTemplate(model, projectId, userId)
            }
        }
        val projectTemplateMap = mutableMapOf<String, String>()
        val versionName = if (publicFlag) {
            "init"
        } else {
            templateDao.getLatestTemplate(dslContext, templateCode).versionName
        }
        val templateName = addMarketTemplateRequest.templateName
        val templateId = UUIDUtil.generate()
        dslContext.transaction { t ->
            val context = DSL.using(t)
            // 判断模板名称是否已经关联过
            val pipelineSettingRecord = pipelineSettingDao.getSetting(
                dslContext = context,
                projectId = projectId,
                name = templateName,
                isTemplate = true
            )
            if (pipelineSettingRecord.isNotEmpty()) {
                return@transaction
            }
            templateDao.createTemplate(
                dslContext = context,
                projectId = projectId,
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
                version = client.get(ServiceAllocIdResource::class).generateSegmentId(TEMPLATE_BIZ_TAG_NAME).data,
                desc = null
            )
            pipelineTemplatePermissionService.createResource(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                templateName = templateName
            )
            templateSettingService.saveDefaultTemplateSetting(
                context = context,
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                templateName = templateName
            )
            projectTemplateMap[projectId] = templateId
        }
        // 安装研发商店的模板，需后台进行迁移数据。
        // 同步数据
        pipelineEventDispatcher.dispatch(
            PipelineTemplateMigrateEvent(
                projectId = projectId,
                source = "PIPELINE_TEMPLATE_MIGRATE",
                pipelineId = "",
                userId = userId,
                templateId = templateId,
            )
        )
        return projectTemplateMap
    }

    fun updateMarketTemplateReference(
        userId: String,
        projectId: String,
        updateMarketTemplateRequest: MarketTemplateRequest
    ): Boolean {
        pipelineTemplateMarketFacadeService.propagateTemplateUpdateToDependents(
            userId = userId,
            projectId = projectId,
            updateMarketTemplateRequest = updateMarketTemplateRequest
        )
        // 同步数据
        pipelineEventDispatcher.dispatch(
            PipelineTemplateMigrateEvent(
                projectId = projectId,
                source = "PIPELINE_TEMPLATE_MIGRATE",
                pipelineId = "",
                userId = userId,
                templateId = updateMarketTemplateRequest.templateCode,
            )
        )
        return true
    }

    fun updateTemplateStoreFlag(
        userId: String,
        projectId: String,
        templateId: String,
        storeFlag: Boolean
    ): Boolean {
        logger.info("update v1 template store flag :$projectId|$userId|$templateId|$storeFlag")
        templateDao.updateStoreFlag(
            dslContext = dslContext,
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            storeFlag = storeFlag
        )
        return true
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

    fun getTemplateIdByTemplateCode(
        templateCode: String,
        projectIds: List<String>
    ): List<PipelineTemplateInfo> {
        val templateList = mutableListOf<PipelineTemplateInfo>()
        val templateIdList = mutableSetOf<String>()
        val templateInfos = templateDao.listTemplateReferenceByProjects(
            dslContext = dslContext,
            templateId = templateCode,
            projectIds = projectIds
        )
        val srcTemplates = getConstrainedSrcTemplates(
            context = dslContext,
            templates = templateInfos,
            templateIdList = templateIdList,
            queryModelFlag = true
        )

        templateInfos.forEach {
            val tTemplate = TTemplate.T_TEMPLATE
            val type = it[tTemplate.TYPE]
            val optionalTemplateInfo = generateOptionalTemplate(
                setting = null,
                record = it,
                pipelinesWithLabels = null,
                srcTemplates = srcTemplates
            ) ?: return@forEach
            templateList.add(
                PipelineTemplateInfo(
                    name = it.templateName,
                    templateId = it.id,
                    projectId = it.projectId,
                    version = it.version,
                    srcTemplateVersion = optionalTemplateInfo.version,
                    versionName = it.versionName,
                    templateType = type,
                    templateTypeDesc = optionalTemplateInfo.templateTypeDesc,
                    category = optionalTemplateInfo.category,
                    logoUrl = optionalTemplateInfo.logoUrl,
                    stages = optionalTemplateInfo.stages,
                    templateName = it.templateName,
                    srcTemplateId = it.srcTemplateId
                )
            )
        }
        return templateList
    }

    fun enableTemplatePermissionManage(projectId: String): Boolean {
        return pipelineTemplatePermissionService.enableTemplatePermissionManage(projectId)
    }

    private fun generateI18nTemplateName(templateId: String, templateType: String): String? {
        if (templateType == TemplateType.PUBLIC.name && !emptyPipelineTemplateId
                .isNullOrBlank() && templateId == emptyPipelineTemplateId
        ) {
            return I18nUtil.getCodeLanMessage(messageCode = BK_EMPTY_PIPELINE)
        }
        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TemplateFacadeService::class.java)
        private const val TEMPLATE_BIZ_TAG_NAME = "TEMPLATE"
    }
}
