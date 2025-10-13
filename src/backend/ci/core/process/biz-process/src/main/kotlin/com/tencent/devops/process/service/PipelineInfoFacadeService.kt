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

package com.tencent.devops.process.service

import com.fasterxml.jackson.core.JsonParseException
import com.google.common.cache.CacheBuilder
import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.exception.PipelineAlreadyExistException
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationDTO
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.db.pojo.ARCHIVE_SHARDING_DSL_CONTEXT
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.ModelUpdate
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType
import com.tencent.devops.common.pipeline.pojo.transfer.TransferBody
import com.tencent.devops.common.pipeline.pojo.transfer.YamlWithVersion
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.audit.service.AuditService
import com.tencent.devops.process.constant.PipelineViewType
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_MAX_PIPELINE_COUNT_PER_PROJECT
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_NO_PERMISSION_PLUGIN_IN_TEMPLATE
import com.tencent.devops.process.constant.ProcessMessageCode.ILLEGAL_PIPELINE_MODEL_JSON
import com.tencent.devops.process.constant.ProcessMessageCode.USER_NEED_PIPELINE_X_PERMISSION
import com.tencent.devops.process.engine.compatibility.BuildPropertyCompatibilityTools
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.utils.PipelineUtils
import com.tencent.devops.process.enums.OperationLogType
import com.tencent.devops.process.jmx.api.ProcessJmxApi
import com.tencent.devops.process.jmx.pipeline.PipelineBean
import com.tencent.devops.process.permission.PipelineAuthorizationService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.PipelineCopy
import com.tencent.devops.process.pojo.audit.Audit
import com.tencent.devops.process.pojo.classify.PipelineViewBulkAdd
import com.tencent.devops.process.pojo.pipeline.DeletePipelineResult
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVo
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import com.tencent.devops.process.service.pipeline.PipelineTransferYamlService
import com.tencent.devops.process.service.view.PipelineViewGroupService
import com.tencent.devops.process.strategy.context.UserPipelinePermissionCheckContext
import com.tencent.devops.process.strategy.factory.UserPipelinePermissionCheckStrategyFactory
import com.tencent.devops.process.template.service.TemplateService
import com.tencent.devops.process.yaml.PipelineYamlFacadeService
import com.tencent.devops.process.yaml.transfer.aspect.IPipelineTransferAspect
import com.tencent.devops.store.api.template.ServiceTemplateResource
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.StreamingOutput
import java.io.File
import java.io.FileInputStream
import java.net.URLEncoder
import java.time.LocalDateTime
import java.util.LinkedList
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineInfoFacadeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val templateDao: TemplateDao,
    private val projectCacheService: ProjectCacheService,
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineViewGroupService: PipelineViewGroupService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val stageTagService: StageTagService,
    private val templateService: TemplateService,
    private val modelCheckPlugin: ModelCheckPlugin,
    private val pipelineBean: PipelineBean,
    private val processJmxApi: ProcessJmxApi,
    private val client: Client,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val transferService: PipelineTransferYamlService,
    private val yamlFacadeService: PipelineYamlFacadeService,
    private val operationLogService: PipelineOperationLogService,
    private val pipelineAuthorizationService: PipelineAuthorizationService,
    private val auditService: AuditService
) {

    @Value("\${process.deletedPipelineStoreDays:30}")
    private val deletedPipelineStoreDays: Int = 30

    // pipeline对应的channel为静态数据, 基本不会变
    private val pipelineChannelCache = CacheBuilder.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<String/*pipelineId*/, ChannelCode>()

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE,
            instanceIds = "#pipelineId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_EDIT_EXPORT_PIPELINE_CONTENT
    )
    fun exportPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int? = null,
        storageType: PipelineStorageType? = PipelineStorageType.MODEL,
        archiveFlag: Boolean? = false
    ): Response {
        val language = I18nUtil.getLanguage(userId)
        val permission = AuthPermission.EDIT
        val userPipelinePermissionCheckStrategy =
            UserPipelinePermissionCheckStrategyFactory.createUserPipelinePermissionCheckStrategy(archiveFlag)
        UserPipelinePermissionCheckContext(userPipelinePermissionCheckStrategy).checkUserPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = permission,
            message = MessageUtil.getMessageByLocale(
                USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                language,
                arrayOf(
                    userId,
                    projectId,
                    permission.getI18n(I18nUtil.getLanguage(userId)),
                    pipelineId
                )
            )
        )

        val targetVersion = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId, pipelineId = pipelineId, version = version, archiveFlag = archiveFlag
        ) ?: throw OperationException(
            I18nUtil.getCodeLanMessage(ILLEGAL_PIPELINE_MODEL_JSON, language = I18nUtil.getLanguage(userId))
        )
        val settingInfo = pipelineSettingFacadeService.userGetSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = targetVersion.settingVersion ?: 1,
            archiveFlag = archiveFlag
        )
        val model = targetVersion.model
        // 适配兼容老数据
        model.stages.forEach {
            it.transformCompatibility()
        }
        val modelAndSetting = PipelineModelAndSetting(model = model, setting = settingInfo)

        // 审计
        ActionAuditContext.current().setInstanceName(model.name)

        logger.info("exportPipeline |$pipelineId | $projectId| $userId")
        return if (storageType == PipelineStorageType.YAML) {
            val suffix = PipelineStorageType.YAML.fileSuffix
            exportStringToFile(targetVersion.yaml ?: "", "${settingInfo.pipelineName}$suffix")
        } else {
            val suffix = PipelineStorageType.MODEL.fileSuffix
            exportStringToFile(JsonUtil.toSortJson(modelAndSetting), "${settingInfo.pipelineName}$suffix")
        }
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE,
            instanceIds = "#pipelineId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_EDIT_EXPORT_PIPELINE_CONTENT
    )
    fun exportPipelineAll(
        userId: String,
        projectId: String,
        storageType: PipelineStorageType = PipelineStorageType.YAML,
        page: Int = 1,
        pageSize: Int = 50
    ): Response {

        val watcher = Watcher(id = "exportPipelineAll|$projectId|$userId")
        watcher.start("permission_check")
        val authPipelines = pipelinePermissionService.getResourceByPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.LIST
        )

        val editPermissionPipelines = pipelinePermissionService.filterPipelines(
            userId = userId,
            projectId = projectId,
            authPermissions = setOf(
                AuthPermission.EDIT
            ),
            pipelineIds = authPipelines
        ).getOrDefault(AuthPermission.EDIT, emptyList()).sorted()
        watcher.stop()
        watcher.start("db_select")
        val fileList = mutableListOf<File>()
        val dir = MDC.get(TraceTag.BIZID) ?: TraceTag.buildBiz()
        // 根据page参数获取需要导出的editPermissionPipelines
        val exportPipelines = editPermissionPipelines.chunked(pageSize).getOrNull(page - 1) ?: emptyList()
        exportPipelines.chunked(10).forEach { chunk ->
            chunk.parallelStream().forEach pipeline@{ pipeline ->
                val targetVersion = pipelineRepositoryService.getPipelineResourceVersion(
                    projectId = projectId, pipelineId = pipeline, version = null, archiveFlag = false
                ) ?: return@pipeline
                val settingInfo = pipelineSettingFacadeService.userGetSetting(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipeline,
                    version = targetVersion.settingVersion ?: 1,
                    archiveFlag = false
                )
                val model = targetVersion.model
                // 适配兼容老数据
                model.stages.forEach {
                    it.transformCompatibility()
                }
                // 审计
                ActionAuditContext.current().setInstanceName(model.name)
                val exportFile = if (storageType == PipelineStorageType.YAML) {
                    exportStringToTempFile(
                        content = targetVersion.yaml ?: kotlin.runCatching {
                            transferService.transfer(
                                userId = userId,
                                projectId = projectId,
                                pipelineId = pipeline,
                                actionType = TransferActionType.FULL_MODEL2YAML,
                                data = TransferBody(
                                    modelAndSetting = PipelineModelAndSetting(
                                        model = model,
                                        setting = settingInfo
                                    )
                                )
                            ).yamlWithVersion?.yamlStr
                        }.onFailure {
                            logger.warn("TRANSFER_YAML|$projectId|$userId|${it.message}")
                        }.getOrNull() ?: "unsupported code model",
                        dir = dir,
                        fileName = settingInfo.pipelineName,
                        suffix = PipelineStorageType.YAML.fileSuffix
                    )
                } else {
                    exportStringToTempFile(
                        content = JsonUtil.toSortJson(
                            PipelineModelAndSetting(
                                model = model,
                                setting = settingInfo
                            )
                        ),
                        dir = dir,
                        fileName = settingInfo.pipelineName,
                        suffix = PipelineStorageType.MODEL.fileSuffix
                    )
                }
                fileList.add(exportFile)
            }
        }
        watcher.stop()

        logger.info("exportPipelineAll completed |$projectId|$userId|exported ${exportPipelines.size} pipelines")

        watcher.start("zip_pipelines")
        val (zipFileName, fileStream) = createZipFile(fileList, "pipelines_export_${projectId}_$pageSize.zip")

        watcher.stop()
        LogUtils.printCostTimeWE(watcher)
        val encodedFileName = URLEncoder.encode(zipFileName, "UTF-8")
        return Response
            .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("content-disposition", "attachment; filename=$encodedFileName")
            .header("Cache-Control", "no-cache")
            .build()
    }

    private fun createZipFile(fileList: MutableList<File>, zipFileName: String): Pair<String, StreamingOutput> {
        val fileStream = StreamingOutput { output ->
            ZipOutputStream(output).use { zipOut ->
                fileList.forEach { file ->
                    try {
                        if (file.exists()) {
                            val zipEntry = ZipEntry(file.name)
                            zipOut.putNextEntry(zipEntry)

                            FileInputStream(file).use { fileInput ->
                                fileInput.copyTo(zipOut)
                            }
                            zipOut.closeEntry()
                        }
                    } catch (e: Exception) {
                        logger.warn("Failed to add file ${file.name} to zip", e)
                    }
                }
            }
            output.flush()

            // 清理临时文件
            fileList.forEach { file ->
                try {
                    if (file.exists()) {
                        file.delete()
                        logger.debug("Deleted temp file: ${file.absolutePath}")
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to delete temp file: ${file.absolutePath}", e)
                }
            }
        }
        return Pair(zipFileName, fileStream)
    }

    private fun exportStringToTempFile(content: String, dir: String, fileName: String, suffix: String): File {
        // 获取系统临时目录
        val systemTempDir = System.getProperty("java.io.tmpdir")
        val tmpdir = File(systemTempDir, dir)
        if (!tmpdir.exists()) {
            tmpdir.mkdirs()
        }
        val file = File(tmpdir, fileName + suffix)

        try {
            file.createNewFile()
            // 将内容写入文件
            file.writeText(content, Charsets.UTF_8)
            logger.info("Successfully exported content to temp file: ${file.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to write content to temp file: ${file.absolutePath}", e)
            // 如果写入失败，删除已创建的文件
            if (file.exists()) {
                file.delete()
            }
        }
        return file
    }

    fun uploadPipeline(userId: String, projectId: String, pipelineModelAndSetting: PipelineModelAndSetting): String {
        val permissionCheck = pipelinePermissionService.checkPipelinePermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.CREATE
        )
        if (!permissionCheck) {
            logger.warn("$userId|$projectId uploadPipeline permission check fail")
            throw PermissionForbiddenException(
                MessageUtil.getMessageByLocale(
                    messageCode = USER_NEED_PIPELINE_X_PERMISSION,
                    params = arrayOf(AuthPermission.CREATE.getI18n(I18nUtil.getLanguage(userId))),
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        val model = pipelineModelAndSetting.model
        modelCheckPlugin.clearUpModel(model)
        if (model.srcTemplateId.isNullOrBlank()) {
            val validateRet = client.get(ServiceTemplateResource::class)
                .validateModelComponentVisibleDept(
                    userId = userId,
                    model = model,
                    projectCode = projectId
                )
            if (validateRet.isNotOk()) {
                throw ErrorCodeException(
                    errorCode = validateRet.status.toString(),
                    defaultMessage = validateRet.message
                )
            }
        }
        return createPipeline(
            userId = userId,
            projectId = projectId,
            model = model,
            setting = pipelineModelAndSetting.setting,
            channelCode = ChannelCode.BS,
            checkPermission = true
        ).pipelineId
    }

    private fun exportStringToFile(content: String, fileName: String): Response {
        // 流式下载
        val fileStream = StreamingOutput { output ->
            val sb = StringBuilder()
            sb.append(content)
            output.write(sb.toString().toByteArray())
            output.flush()
        }
        val encodeName = URLEncoder.encode(fileName, "UTF-8")
        return Response
            .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("content-disposition", "attachment; filename = $encodeName")
            .header("Cache-Control", "no-cache")
            .build()
    }

    fun getPipelineNameVersion(projectId: String, pipelineId: String): Pair<String, Int> {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
        return Pair(pipelineInfo?.pipelineName ?: "", pipelineInfo?.version ?: 0)
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_CREATE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_CREATE_CONTENT
    )
    fun createPipeline(
        userId: String,
        projectId: String,
        model: Model,
        channelCode: ChannelCode,
        setting: PipelineSetting? = null,
        yaml: YamlWithVersion? = null,
        checkPermission: Boolean = true,
        fixPipelineId: String? = null,
        instanceType: String? = PipelineInstanceTypeEnum.FREEDOM.type,
        buildNo: BuildNo? = null,
        param: List<BuildFormProperty>? = null,
        fixTemplateVersion: Long? = null,
        versionStatus: VersionStatus? = VersionStatus.RELEASED,
        branchName: String? = null,
        useSubscriptionSettings: Boolean? = false,
        useConcurrencyGroup: Boolean? = false,
        description: String? = null,
        yamlInfo: PipelineYamlVo? = null,
        pipelineDisable: Boolean? = null
    ): DeployPipelineResult {
        val watcher =
            Watcher(id = "createPipeline|$projectId|$userId|$channelCode|$checkPermission|$instanceType|$fixPipelineId")
        var success = false
        try {

            if (checkPermission) {
                watcher.start("perm_v_perm")
                val language = I18nUtil.getLanguage(userId)
                val permission = AuthPermission.CREATE
                pipelinePermissionService.validPipelinePermission(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = "*",
                    permission = permission,
                    message = MessageUtil.getMessageByLocale(
                        USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                        language,
                        arrayOf(
                            userId,
                            projectId,
                            permission.getI18n(I18nUtil.getLanguage(userId)),
                            "*"
                        )
                    )
                )
                watcher.stop()
            }

            if (isPipelineExist(
                    projectId = projectId,
                    pipelineId = fixPipelineId,
                    name = model.name,
                    channelCode = channelCode
                )
            ) {
                logger.warn("The pipeline(${model.name}) is exist")
                throw ErrorCodeException(
                    statusCode = Response.Status.CONFLICT.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_NAME_EXISTS
                )
            }

            val templateId = model.templateId
            if (templateId != null) {
                // 如果是根据模板创建的流水线需为model设置srcTemplateId
                model.srcTemplateId = templateDao.getSrcTemplateId(
                    dslContext = dslContext,
                    projectId = projectId,
                    templateId = templateId,
                    type = TemplateType.CONSTRAINT.name
                )
            }

            // 如果为分支版本的报错，必须指定分支名称
            if (versionStatus == VersionStatus.BRANCH && branchName.isNullOrBlank()) {
                throw ErrorCodeException(
                    statusCode = Response.Status.BAD_REQUEST.statusCode,
                    errorCode = CommonMessageCode.ERROR_NEED_PARAM_,
                    params = arrayOf("branchName")
                )
            }

            // 检查用户是否有插件的使用权限
            if (model.srcTemplateId != null) {
                watcher.start("store_template_perm")
                val validateRet = client.get(ServiceTemplateResource::class)
                    .validateUserTemplateComponentVisibleDept(
                        userId = userId,
                        templateCode = model.srcTemplateId as String,
                        projectCode = projectId
                    )
                if (validateRet.isNotOk()) {
                    throw OperationException(
                        validateRet.message ?: MessageUtil.getMessageByLocale(
                            ERROR_NO_PERMISSION_PLUGIN_IN_TEMPLATE,
                            I18nUtil.getLanguage(userId)
                        )
                    )
                }
                watcher.stop()
            }

            watcher.start("project_v_pipeline")
            // 检查用户流水线是否达到上限
            val projectVO = projectCacheService.getProject(projectId)
            if (projectVO?.pipelineLimit != null) {
                val preCount = pipelineRepositoryService.countByProjectIds(setOf(projectId), ChannelCode.BS)
                if (preCount >= projectVO.pipelineLimit!!) {
                    throw OperationException(
                        MessageUtil.getMessageByLocale(
                            ERROR_MAX_PIPELINE_COUNT_PER_PROJECT,
                            I18nUtil.getLanguage(userId),
                            arrayOf("${projectVO.pipelineLimit}")
                        )
                    )
                }
            }
            watcher.stop()

            var pipelineId: String? = null
            try {
                val instance = if (instanceType == PipelineInstanceTypeEnum.FREEDOM.type) {
                    // 将模版常量变更实例化为流水线变量
                    val triggerContainer = model.getTriggerContainer()
                    PipelineUtils.instanceModel(
                        templateModel = model,
                        pipelineName = model.name,
                        buildNo = triggerContainer.buildNo,
                        param = triggerContainer.params,
                        instanceFromTemplate = false,
                        labels = model.labels,
                        defaultStageTagId = stageTagService.getDefaultStageTag().data?.id
                    )
                } else {
                    model
                }

                watcher.start("deployPipeline")
                val result = pipelineRepositoryService.deployPipeline(
                    model = instance,
                    setting = setting,
                    projectId = projectId,
                    signPipelineId = fixPipelineId,
                    userId = userId,
                    channelCode = channelCode,
                    create = true,
                    useSubscriptionSettings = useSubscriptionSettings,
                    useConcurrencyGroup = useConcurrencyGroup,
                    versionStatus = versionStatus,
                    branchName = branchName,
                    templateId = templateId,
                    description = description,
                    yaml = yaml,
                    baseVersion = null,
                    yamlInfo = yamlInfo,
                    pipelineDisable = pipelineDisable
                )
                pipelineId = result.pipelineId
                watcher.stop()

                // 先进行模板关联操作
                if (templateId != null) {
                    watcher.start("createTemplate")
                    templateService.createRelationBtwTemplate(
                        userId = userId,
                        projectId = projectId,
                        templateId = templateId,
                        pipelineId = pipelineId,
                        instanceType = instanceType!!,
                        buildNo = buildNo,
                        param = param,
                        fixTemplateVersion = fixTemplateVersion
                    )
                    watcher.stop()
                }

                // 模板关联操作成功后再创建流水线相关资源
                if (checkPermission) {
                    watcher.start("perm_c_perm")
                    try {
                        pipelinePermissionService.createResource(
                            userId = userId,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            pipelineName = model.name
                        )
                        pipelineAuthorizationService.addResourceAuthorization(
                            projectId = projectId,
                            resourceAuthorizationList = listOf(
                                ResourceAuthorizationDTO(
                                    projectCode = projectId,
                                    resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                                    resourceCode = pipelineId,
                                    resourceName = model.name,
                                    handoverFrom = userId,
                                    handoverTime = LocalDateTime.now().timestampmilli()
                                )
                            )
                        )
                    } catch (ignored: Throwable) {
                        if (fixPipelineId != pipelineId) {
                            throw ignored
                        }
                    }
                    watcher.stop()
                }

                // 添加标签
                if (versionStatus == VersionStatus.RELEASED) pipelineGroupService.addPipelineLabel(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    labelIds = model.labels
                )

                // 添加到静态分组
                val bulkAdd = PipelineViewBulkAdd(pipelineIds = listOf(pipelineId), viewIds = model.staticViews)
                pipelineViewGroupService.bulkAdd(userId, projectId, bulkAdd)

                // 添加到动态分组
                pipelineViewGroupService.updateGroupAfterPipelineCreate(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId
                )
                ActionAuditContext.current()
                    .addInstanceInfo(pipelineId, model.name, null, null)
                success = true
                return result
            } catch (duplicateKeyException: DuplicateKeyException) {
                logger.info("duplicateKeyException: ${duplicateKeyException.message}")
                if (pipelineId != null) {
                    pipelineRepositoryService.deletePipeline(projectId, pipelineId, userId, channelCode, true)
                }
                throw ErrorCodeException(
                    statusCode = Response.Status.CONFLICT.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_IS_EXISTS
                )
            } catch (ignored: Throwable) {
                if (pipelineId != null) {
                    pipelineRepositoryService.deletePipeline(projectId, pipelineId, userId, channelCode, true)
                }
                throw ignored
            } finally {
                if (!success) {
                    val beforeDeleteParam = BeforeDeleteParam(
                        userId = userId, projectId = projectId, pipelineId = pipelineId ?: "", channelCode = channelCode
                    )
                    modelCheckPlugin.beforeDeleteElementInExistsModel(
                        existModel = model,
                        sourceModel = null,
                        param = beforeDeleteParam
                    )
                }
            }
        } finally {
            LogUtils.printCostTimeWE(watcher = watcher)
            pipelineBean.create(success)
            processJmxApi.execute(ProcessJmxApi.NEW_PIPELINE_CREATE, watcher.totalTimeMillis)
        }
    }

    fun createYamlPipeline(
        userId: String,
        projectId: String,
        yaml: String,
        yamlFileName: String,
        branchName: String,
        isDefaultBranch: Boolean,
        description: String? = null,
        aspects: LinkedList<IPipelineTransferAspect>? = null,
        yamlInfo: PipelineYamlVo? = null
    ): DeployPipelineResult {
        val versionStatus = if (isDefaultBranch) {
            VersionStatus.RELEASED
        } else {
            VersionStatus.BRANCH
        }
        val (newResource, yamlWithVersion) = transferModelAndSetting(
            userId = userId,
            projectId = projectId,
            yaml = yaml,
            yamlFileName = yamlFileName,
            isDefaultBranch = isDefaultBranch,
            branchName = branchName,
            aspects = aspects
        )
        // 流水线名称实际取值优先级：setting > model > fileName
        val pipelineName = newResource.setting.pipelineName.takeIf {
            it.isNotBlank()
        } ?: newResource.model.name.ifBlank {
            yamlFileName
        }
        val pipelineAsCodeSettings =
            newResource.setting.pipelineAsCodeSettings?.copy(enable = true) ?: PipelineAsCodeSettings(enable = true)
        // 通过PAC模式创建或保存的流水线均打开PAC
        // 修正创建时的流水线名和增加PAC开关参数
        val newSetting = newResource.setting.copy(
            projectId = projectId,
            pipelineName = pipelineName,
            pipelineAsCodeSettings = pipelineAsCodeSettings
        )
        return createPipeline(
            userId = userId,
            projectId = projectId,
            model = newResource.model.copy(name = pipelineName),
            setting = newSetting,
            channelCode = ChannelCode.BS,
            yaml = yamlWithVersion,
            versionStatus = versionStatus,
            branchName = branchName,
            description = description,
            yamlInfo = yamlInfo,
            pipelineDisable = newResource.setting.runLockType == PipelineRunLockType.LOCK
        )
    }

    fun updateYamlPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        yaml: String,
        yamlFileName: String,
        branchName: String,
        isDefaultBranch: Boolean,
        description: String? = null,
        aspects: LinkedList<IPipelineTransferAspect>? = null,
        yamlInfo: PipelineYamlVo? = null
    ): DeployPipelineResult {
        val versionStatus = if (isDefaultBranch) {
            VersionStatus.RELEASED
        } else {
            VersionStatus.BRANCH
        }
        val (newResource, yamlWithVersion) = transferModelAndSetting(
            userId = userId,
            projectId = projectId,
            yaml = yaml,
            yamlFileName = yamlFileName,
            isDefaultBranch = isDefaultBranch,
            branchName = branchName,
            aspects = aspects
        )
        newResource.setting.projectId = projectId
        newResource.setting.pipelineId = pipelineId
        // 通过PAC模式创建或保存的流水线均打开PAC
        // 修正创建时的流水线名和增加PAC开关参数
        val pipelineName = newResource.model.name.ifBlank { yamlFileName }
        val pipelineAsCodeSettings =
            newResource.setting.pipelineAsCodeSettings?.copy(enable = true) ?: PipelineAsCodeSettings(enable = true)
        val savedSetting = pipelineSettingFacadeService.saveSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            setting = newResource.setting.copy(
                pipelineName = pipelineName,
                pipelineAsCodeSettings = pipelineAsCodeSettings
            ),
            checkPermission = false,
            versionStatus = versionStatus,
            updateLabels = versionStatus == VersionStatus.RELEASED,
            dispatchPipelineUpdateEvent = false
        )
        return editPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            model = newResource.model.copy(name = pipelineName),
            channelCode = ChannelCode.BS,
            yaml = yamlWithVersion,
            savedSetting = savedSetting,
            versionStatus = versionStatus,
            branchName = branchName,
            description = description,
            yamlInfo = yamlInfo,
            pipelineDisable = newResource.setting.runLockType == PipelineRunLockType.LOCK
        )
    }

    fun updateBranchVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        branchName: String,
        branchVersionAction: BranchVersionAction,
        releaseBranch: Boolean? = false
    ) {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            if (releaseBranch == true || branchVersionAction != BranchVersionAction.INACTIVE) {
                // 如果是发布分支版本则直接更新
                pipelineRepositoryService.updatePipelineBranchVersion(
                    userId, projectId, pipelineId, branchName, branchVersionAction, transactionContext
                )
            } else {
                // 如果是删除分支版本则判断是否为最后一个版本
                val branchVersion = pipelineRepositoryService.getBranchVersionResource(
                    projectId, pipelineId, branchName
                )
                pipelineRepositoryService.updatePipelineBranchVersion(
                    userId, projectId, pipelineId, branchName, branchVersionAction, transactionContext
                )
                val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
                    projectId = projectId, pipelineId = pipelineId, queryDslContext = transactionContext
                )
                if (pipelineInfo?.latestVersionStatus?.isNotReleased() != true) {
                    return@transaction
                }
                val branchCount = pipelineRepositoryService.getActiveBranchVersionCount(
                    projectId = projectId, pipelineId = pipelineId, queryDslContext = transactionContext
                )
                if (branchVersion != null && branchCount == 0) {
                    pipelineRepositoryService.rollbackDraftFromVersion(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        targetVersion = branchVersion.copy(
                            model = getFixedModel(
                                branchVersion.model, projectId, pipelineId, userId, pipelineInfo
                            )
                        ),
                        ignoreBase = true,
                        transactionContext = transactionContext
                    )
                }
            }
        }
    }

    fun updateYamlPipelineSetting(
        userId: String,
        projectId: String,
        pipelineId: String,
        pipelineAsCodeSettings: PipelineAsCodeSettings
    ) {
        val setting = pipelineSettingFacadeService.getSettingInfo(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
            )
        if (setting.pipelineAsCodeSettings?.enable == true && !pipelineAsCodeSettings.enable) {
            // 关闭PAC开关时，将所有分支版本设为
            pipelineRepositoryService.updatePipelineBranchVersion(
                userId, projectId, pipelineId, null, BranchVersionAction.INACTIVE
            )
        }

        pipelineSettingFacadeService.saveSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            updateLabels = false,
            setting = setting.copy(pipelineAsCodeSettings = pipelineAsCodeSettings)
        )
    }

    private fun transferModelAndSetting(
        userId: String,
        projectId: String,
        yaml: String,
        yamlFileName: String,
        isDefaultBranch: Boolean,
        branchName: String,
        aspects: LinkedList<IPipelineTransferAspect>? = null
    ): Pair<PipelineModelAndSetting, YamlWithVersion> {
        return try {
            val result = transferService.transfer(
                userId = userId,
                projectId = projectId,
                pipelineId = null,
                actionType = TransferActionType.FULL_YAML2MODEL,
                data = TransferBody(oldYaml = yaml, yamlFileName = yamlFileName),
                aspects = aspects ?: LinkedList()
            )
            if (result.modelAndSetting == null) {
                logger.warn("TRANSFER_YAML|$projectId|$userId|$isDefaultBranch|yml=\n$yaml")
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_OCCURRED_IN_TRANSFER
                )
            }
            Pair(result.modelAndSetting!!, result.yamlWithVersion!!)
        } catch (ignore: Throwable) {
            if (ignore is ErrorCodeException) throw ignore
            logger.warn("TRANSFER_YAML|$projectId|$userId|$branchName|$isDefaultBranch|yml=\n$yaml", ignore)
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_OCCURRED_IN_TRANSFER
            )
        }
    }

    /**
     * 还原已经删除的流水线
     */
    @ActionAuditRecord(
        actionId = ActionId.PROJECT_MANAGE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PROJECT,
            instanceIds = "#pipelineId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PROJECT_MANAGE_RESTORE_PIPELINE_CONTENT
    )
    fun restorePipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode
    ): DeployPipelineResult {
        val watcher = Watcher(id = "restorePipeline|$pipelineId|$userId")
        try {
            watcher.start("isProjectManager")
            // 判断用户是否为项目管理员
            if (!pipelinePermissionService.checkProjectManager(userId, projectId)) {
                val defaultMessage = "admin"
                val permissionMsg = I18nUtil.getCodeLanMessage(
                    messageCode = "${CommonMessageCode.MSG_CODE_ROLE_PREFIX}${BkAuthGroup.MANAGER.value}",
                    defaultMessage = defaultMessage,
                    language = I18nUtil.getLanguage(userId)
                )
                throw ErrorCodeException(
                    statusCode = Response.Status.FORBIDDEN.statusCode,
                    errorCode = USER_NEED_PIPELINE_X_PERMISSION,
                    defaultMessage = defaultMessage,
                    params = arrayOf(permissionMsg)
                )
            }

            watcher.start("restorePipeline")
            val resource = pipelineRepositoryService.restorePipeline(
                projectId = projectId, pipelineId = pipelineId, userId = userId,
                channelCode = channelCode, days = deletedPipelineStoreDays.toLong()
            )
            watcher.start("createResource")
            pipelinePermissionService.createResource(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = resource.model.name
            )
            pipelineAuthorizationService.addResourceAuthorization(
                projectId = projectId,
                resourceAuthorizationList = listOf(
                    ResourceAuthorizationDTO(
                        projectCode = projectId,
                        resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                        resourceCode = pipelineId,
                        resourceName = resource.model.name,
                        handoverFrom = userId,
                        handoverTime = LocalDateTime.now().timestampmilli()
                    )
                )
            )
            ActionAuditContext.current().setInstanceName(resource.model.name)
            return DeployPipelineResult(
                pipelineId = pipelineId,
                pipelineName = resource.model.name,
                version = resource.version,
                versionNum = resource.versionNum,
                versionName = null
            )
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher)
        }
    }

    /**
     * Copy the exist pipeline
     */
    fun copyPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        pipelineCopy: PipelineCopy,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
    ): String {
        val pipeline = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
            )

        logger.info("Start to copy the pipeline $pipelineId")
        if (checkPermission) {
            val permission = AuthPermission.EDIT
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = permission,
                message = MessageUtil.getMessageByLocale(
                    USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    I18nUtil.getLanguage(userId),
                    arrayOf(
                        userId,
                        projectId,
                        permission.getI18n(I18nUtil.getLanguage(userId)),
                        pipelineId
                    )
                )
            )
//            pipelinePermissionService.validPipelinePermission(
//                userId = userId,
//                projectId = projectId,
//                pipelineId = "*",
//                permission = AuthPermission.CREATE,
//                message = "用户($userId)无权限在工程($projectId)下创建流水线"
//            )
            if (!pipelinePermissionService.checkPipelinePermission(
                    userId = userId,
                    projectId = projectId,
                    permission = AuthPermission.CREATE
                )
            ) {
                throw PermissionForbiddenException(
                    MessageUtil.getMessageByLocale(
                        USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                        I18nUtil.getLanguage(userId),
                        arrayOf(
                            userId,
                            projectId,
                            AuthPermission.CREATE.getI18n(I18nUtil.getLanguage(userId)),
                            "*"
                        )
                    )
                )
            }
        }

        if (pipeline.channelCode != channelCode) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_CHANNEL_CODE,
                params = arrayOf(pipeline.channelCode.name)
            )
        }

        val model = pipelineRepositoryService.getPipelineResourceVersion(projectId, pipelineId)?.model
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
            )
        try {
            val copyMode = Model(
                name = pipelineCopy.name,
                desc = pipelineCopy.desc ?: model.desc,
                stages = model.stages,
                staticViews = pipelineCopy.staticViews,
                labels = pipelineCopy.labels
            )
            modelCheckPlugin.clearUpModel(copyMode)
            val settingInfo = pipelineSettingFacadeService.getSettingInfo(projectId, pipelineId)
            val newPipelineId = createPipeline(
                userId = userId,
                projectId = projectId,
                model = copyMode,
                channelCode = channelCode,
                setting = settingInfo?.copy(
                    labels = pipelineCopy.labels
                )
            ).pipelineId
            return newPipelineId
        } catch (e: JsonParseException) {
            logger.error("Parse process($pipelineId) fail", e)
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ILLEGAL_PIPELINE_MODEL_JSON
            )
        } catch (e: PipelineAlreadyExistException) {
            throw ErrorCodeException(
                statusCode = Response.Status.CONFLICT.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NAME_EXISTS
            )
        } catch (e: ErrorCodeException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Fail to get the pipeline($pipelineId) definition of project($projectId)", e)
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.OPERATE_PIPELINE_FAIL,
                params = arrayOf(e.message ?: "")
            )
        }
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_EDIT_CONTENT
    )
    fun editPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        model: Model,
        yaml: YamlWithVersion?,
        channelCode: ChannelCode,
        checkPermission: Boolean = true,
        checkTemplate: Boolean = true,
        updateLastModifyUser: Boolean? = true,
        savedSetting: PipelineSetting? = null,
        versionStatus: VersionStatus? = VersionStatus.RELEASED,
        branchName: String? = null,
        description: String? = null,
        baseVersion: Int? = null,
        yamlInfo: PipelineYamlVo? = null,
        pipelineDisable: Boolean? = null
    ): DeployPipelineResult {
        if (checkTemplate && templateService.isTemplatePipeline(projectId, pipelineId)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_TEMPLATE_CAN_NOT_EDIT
            )
        }
        val apiStartEpoch = System.currentTimeMillis()
        var success = false

        try {
            if (checkPermission) {
                val permission = AuthPermission.EDIT
                pipelinePermissionService.validPipelinePermission(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    permission = permission,
                    message = MessageUtil.getMessageByLocale(
                        USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                        I18nUtil.getLanguage(userId),
                        arrayOf(
                            userId,
                            projectId,
                            permission.getI18n(I18nUtil.getLanguage(userId)),
                            pipelineId
                        )
                    )
                )
            }
            if (isPipelineExist(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    // 对于约束模式的流水线，其model中的name是其对应模板的名称，
                    // 这里若直接使用model中的名称，其名称是模板名称，可能会出现重复，故以setting为准
                    name = savedSetting?.pipelineName ?: model.name,
                    channelCode = channelCode
                )
            ) {
                logger.warn("The pipeline(${model.name}) is exist")
                throw ErrorCodeException(
                    statusCode = Response.Status.CONFLICT.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_NAME_EXISTS
                )
            }

            val pipeline = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
                ?: throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
                )

            if (pipeline.channelCode != channelCode) {
                throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_CHANNEL_CODE,
                    params = arrayOf(pipeline.channelCode.name)
                )
            }

            // 如果为分支版本的报错，必须指定分支名称
            if (versionStatus == VersionStatus.BRANCH && branchName.isNullOrBlank()) {
                throw ErrorCodeException(
                    statusCode = Response.Status.BAD_REQUEST.statusCode,
                    errorCode = CommonMessageCode.ERROR_NEED_PARAM_,
                    params = arrayOf("branchName")
                )
            }
            val existModel = pipelineRepositoryService.getPipelineResourceVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                includeDraft = true
            )?.model ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
            )
            // 只在更新操作时检查stage数量不为1
            if (model.stages.size <= 1) throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_WITH_EMPTY_STAGE,
                params = arrayOf()
            )
            if (versionStatus?.isReleasing() == true) {
                // 对已经存在的模型做处理
                val param = BeforeDeleteParam(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    channelCode = channelCode
                )
                modelCheckPlugin.beforeDeleteElementInExistsModel(existModel, model, param)
            }
            val templateId = model.templateId

            if (templateId != null) {
                // 如果是根据模板创建的流水线需为model设置srcTemplateId
                model.srcTemplateId = templateDao.getSrcTemplateId(
                    dslContext = dslContext,
                    projectId = projectId,
                    templateId = templateId,
                    type = TemplateType.CONSTRAINT.name
                )
            }

            val deployResult = pipelineRepositoryService.deployPipeline(
                model = model,
                projectId = projectId,
                signPipelineId = pipelineId,
                userId = userId,
                channelCode = channelCode,
                create = false,
                updateLastModifyUser = updateLastModifyUser,
                setting = savedSetting,
                versionStatus = versionStatus,
                branchName = branchName,
                description = description,
                yaml = yaml,
                baseVersion = baseVersion,
                yamlInfo = yamlInfo,
                pipelineDisable = pipelineDisable
            )
            // 审计
            ActionAuditContext.current()
                .addInstanceInfo(pipelineId, model.name, existModel, model)
            success = true
            return deployResult
        } finally {
            pipelineBean.edit(success)
            processJmxApi.execute(ProcessJmxApi.NEW_PIPELINE_EDIT, System.currentTimeMillis() - apiStartEpoch)
            logger.info("EDIT_PIPELINE|$pipelineId|$channelCode|p=$checkPermission|u=$userId")
        }
    }

    fun renamePipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        name: String,
        channelCode: ChannelCode
    ) {
        val setting = pipelineSettingFacadeService.userGetSetting(userId, projectId, pipelineId, channelCode)
        setting.pipelineName = name
        val savedSetting = pipelineSettingFacadeService.saveSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            setting = setting,
            checkPermission = true,
            dispatchPipelineUpdateEvent = true
        )
        updatePipelineSettingVersion(
            userId = userId,
            projectId = setting.projectId,
            pipelineId = setting.pipelineId,
            operationLogType = OperationLogType.UPDATE_PIPELINE_SETTING,
            savedSetting = savedSetting
        )
    }

    fun updatePipelineSettingVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        operationLogType: OperationLogType,
        savedSetting: PipelineSetting,
        updateLastModifyUser: Boolean? = true
    ): DeployPipelineResult {
        val result = pipelineRepositoryService.updateSettingVersion(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            savedSetting = savedSetting,
            updateLastModifyUser = updateLastModifyUser
        )
        operationLogService.addOperationLog(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = result.version,
            operationLogType = operationLogType,
            params = result.versionName ?: "",
            description = null
        )
        return result
    }

    fun updateBuildNo(
        userId: String,
        projectId: String,
        pipelineId: String,
        targetBuildNo: Int
    ) {
        operationLogService.addOperationLog(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = 0,
            operationLogType = OperationLogType.RESET_RECOMMENDED_VERSION_BUILD_NO,
            params = targetBuildNo.toString(),
            description = null
        )
        pipelineBuildSummaryDao.updateBuildNo(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildNo = targetBuildNo,
            debug = false
        )
    }

    fun resetBuildNo(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Boolean {
        val releaseVersion = pipelineRepositoryService.getPipelineResourceVersion(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
            )
        val buildNo = releaseVersion.model.getTriggerContainer().buildNo
            ?: return false
        operationLogService.addOperationLog(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = 0,
            operationLogType = OperationLogType.RESET_RECOMMENDED_VERSION_BUILD_NO,
            params = buildNo.buildNo.toString(),
            description = null
        )
        pipelineBuildSummaryDao.updateBuildNo(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildNo = buildNo.buildNo,
            debug = false
        )
        return true
    }

    fun saveAll(
        userId: String,
        projectId: String,
        pipelineId: String,
        model: Model,
        setting: PipelineSetting,
        channelCode: ChannelCode,
        checkPermission: Boolean = true,
        checkTemplate: Boolean = true,
        versionStatus: VersionStatus? = VersionStatus.RELEASED
    ): DeployPipelineResult {
        // fix 用户端可能不传入pipelineId和projectId的问题，或者传错的问题
        setting.pipelineId = pipelineId
        setting.projectId = projectId
        val savedSetting = pipelineSettingFacadeService.saveSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            setting = setting,
            checkPermission = checkPermission,
            dispatchPipelineUpdateEvent = false
        )
        val pipelineResult = editPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            model = model,
            yaml = null,
            channelCode = channelCode,
            checkPermission = checkPermission,
            checkTemplate = checkTemplate,
            savedSetting = savedSetting,
            versionStatus = versionStatus
        )
        if (setting.projectId.isBlank()) {
            setting.projectId = projectId
        }

        return pipelineResult
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE,
            instanceNames = "#$?.name",
            instanceIds = "#pipelineId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_VIEW_CONTENT
    )
    fun getPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        version: Int? = null,
        checkPermission: Boolean = true,
        includeDraft: Boolean? = false
    ): Model {
        if (checkPermission) {
            val permission = AuthPermission.VIEW
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = permission,
                message = MessageUtil.getMessageByLocale(
                    USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    I18nUtil.getLanguage(userId),
                    arrayOf(
                        userId,
                        projectId,
                        permission.getI18n(I18nUtil.getLanguage(userId)),
                        pipelineId
                    )
                )
            )
        }
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
            )

        if (pipelineInfo.channelCode != channelCode) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_CHANNEL_CODE,
                params = arrayOf(pipelineInfo.channelCode.name)
            )
        }

        val model = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            includeDraft = includeDraft
        )?.model ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
        )

        return getFixedModel(model, projectId, pipelineId, userId, pipelineInfo)
    }

    fun getFixedModel(
        model: Model,
        projectId: String,
        pipelineId: String,
        userId: String,
        pipelineInfo: PipelineInfo,
        archiveFlag: Boolean? = false
    ): Model {
        try {
            val triggerContainer = model.getTriggerContainer()
            val finalDslContext = CommonUtils.getJooqDslContext(archiveFlag, ARCHIVE_SHARDING_DSL_CONTEXT)
            // #10958 每次存储model都需要忽略当前的推荐版本号值，在返回前端时重查
            triggerContainer.buildNo?.apply {
                currentBuildNo = pipelineRepositoryService.getBuildNo(
                    projectId = projectId, pipelineId = pipelineId, queryDslContext = finalDslContext
                ) ?: buildNo
            }
            // 兼容性处理
            BuildPropertyCompatibilityTools.fix(triggerContainer.params)

            // 获取流水线labels
            val groups = pipelineGroupService.getGroups(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                archiveFlag = archiveFlag
            )
            val labels = mutableListOf<String>()
            groups.forEach {
                labels.addAll(it.labels)
            }
            model.labels = labels
            model.name = pipelineInfo.pipelineName
            model.desc = pipelineInfo.pipelineDesc
            model.pipelineCreator = pipelineInfo.creator
            model.latestVersion = pipelineInfo.version
            val defaultTagId by lazy { stageTagService.getDefaultStageTag().data?.id } // 优化
            model.stages.forEach {
                if (it.name.isNullOrBlank()) it.name = it.id
                if (it.tag == null) it.tag = defaultTagId?.let { self -> listOf(self) }
                it.resetBuildOption()
                it.transformCompatibility()
            }

            // 部分老的模板实例没有templateId，需要手动加上
            if (model.instanceFromTemplate == true) {
                model.templateId = templateService.getTemplateIdByPipeline(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    queryDslContext = finalDslContext
                )
            }
            // 静态组
            model.staticViews = pipelineViewGroupService.listViewByPipelineId(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                viewType = PipelineViewType.STATIC,
                queryDslContext = finalDslContext
            ).map { it.id }
            return model
        } catch (e: Exception) {
            logger.warn("Fail to get the pipeline($pipelineId) definition of project($projectId)", e)
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.OPERATE_PIPELINE_FAIL,
                defaultMessage = "Fail to get the pipeline",
                params = arrayOf(e.message ?: "unknown")
            )
        }
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_DELETE_CONTENT
    )
    fun deletePipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode? = null,
        checkPermission: Boolean = true,
        delete: Boolean = false,
        archiveFlag: Boolean? = false
    ): DeletePipelineResult {
        val watcher = Watcher(id = "deletePipeline|$pipelineId|$userId")
        var success = false
        try {
            if (checkPermission) {
                watcher.start("perm_v_perm")
                val userPipelinePermissionCheckStrategy =
                    UserPipelinePermissionCheckStrategyFactory.createUserPipelinePermissionCheckStrategy(archiveFlag)
                UserPipelinePermissionCheckContext(userPipelinePermissionCheckStrategy).checkUserPipelinePermission(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    permission = AuthPermission.DELETE
                )
                watcher.stop()
            }
            if (archiveFlag != true) {
                val existModel = pipelineRepositoryService.getPipelineResourceVersion(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    archiveFlag = archiveFlag
                )?.model
                    ?: throw ErrorCodeException(
                        statusCode = Response.Status.NOT_FOUND.statusCode,
                        errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
                    )
                ActionAuditContext.current().addInstanceInfo(pipelineId, existModel.name, null, null)
                // 对已经存在的模型做删除前处理
                val param = BeforeDeleteParam(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    channelCode = channelCode ?: ChannelCode.BS
                )
                modelCheckPlugin.beforeDeleteElementInExistsModel(existModel, null, param)
                watcher.start("s_c_yaml_del")
                val setting = pipelineSettingFacadeService.userGetSetting(userId, projectId, pipelineId)
                if (setting.pipelineAsCodeSettings?.enable == true) {
                    // 检查yaml是否已经在默认分支删除
                    val yamlExist = yamlFacadeService.yamlExistInDefaultBranch(
                        projectId = projectId, pipelineId = pipelineId
                    )
                    if (yamlExist) {
                        throw ErrorCodeException(
                            errorCode = ProcessMessageCode.ERROR_DELETE_YAML_PIPELINE_IN_DEFAULT_BRANCH
                        )
                    }
                    pipelineSettingFacadeService.saveSetting(
                        userId = userId, projectId = projectId, pipelineId = pipelineId,
                        setting = setting.copy(
                            pipelineAsCodeSettings = PipelineAsCodeSettings(false)
                        )
                    )
                }
            }
            watcher.start("s_r_pipeline_del")
            val deletePipelineResult = pipelineRepositoryService.deletePipeline(
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                channelCode = channelCode,
                delete = delete,
                archiveFlag = archiveFlag
            )
            watcher.stop()

            if (checkPermission) {
                watcher.start("perm_d_perm")
                pipelinePermissionService.deleteResource(projectId = projectId, pipelineId = pipelineId)
                watcher.stop()
            }
            success = true
            return deletePipelineResult
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher, warnThreshold = 2000)
            pipelineBean.delete(success)
            processJmxApi.execute(ProcessJmxApi.NEW_PIPELINE_DELETE, watcher.totalTimeMillis)
            logger.info("DEL_PIPELINE|$pipelineId|$channelCode|p=$checkPermission|u=$userId|del=$delete")
        }
    }

    fun isPipelineExist(
        projectId: String,
        pipelineId: String? = null,
        name: String,
        channelCode: ChannelCode
    ): Boolean {
        return pipelineRepositoryService.isPipelineExist(
            projectId = projectId, pipelineName = name, channelCode = channelCode, excludePipelineId = pipelineId
        )
    }

    fun getPipelineChannel(projectId: String, pipelineId: String): ChannelCode? {
        if (pipelineChannelCache.getIfPresent(pipelineId) != null) {
            return pipelineChannelCache.getIfPresent(pipelineId)
        }
        val pipelineInfo = pipelineInfoDao.getPipelineInfo(dslContext, projectId, pipelineId) ?: return null
        val channelCode = ChannelCode.getChannel(pipelineInfo.channel)
        if (channelCode != null) {
            pipelineChannelCache.put(pipelineId, channelCode)
        }
        return channelCode
    }

    fun batchUpdateModelName(modelUpdateList: List<ModelUpdate>): List<ModelUpdate> {
        val failUpdateModels = mutableListOf<ModelUpdate>()
        modelUpdateList.forEach {
            try {
                val pipelineExist = isPipelineExist(
                    projectId = it.projectId,
                    name = it.name,
                    channelCode = ChannelCode.GIT,
                    pipelineId = it.pipelineId
                )
                if (!pipelineExist) {
                    pipelineRepositoryService.updateModelName(
                        pipelineId = it.pipelineId,
                        projectId = it.projectId,
                        modelName = it.name,
                        userId = it.updateUserId
                    )
                } else {
                    it.updateResultMessage = "pipeline name exist"
                    failUpdateModels.add(it)
                }
            } catch (e: Exception) {
                it.updateResultMessage = "some wrong happen in dao update,error message:${e.message}"
                failUpdateModels.add(it)
            }
        }
        return failUpdateModels
    }

    fun lockPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        enable: Boolean
    ) {
        val pipelineInfo = locked(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            locked = !enable
        )
        auditService.createAudit(
            Audit(
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceId = pipelineId,
                resourceName = pipelineInfo.pipelineName,
                userId = userId,
                action = "edit",
                actionContent = if (enable) "UnLock Pipeline" else "Locked Pipeline",
                projectId = projectId
            )
        )
    }

    fun locked(userId: String, projectId: String, pipelineId: String, locked: Boolean): PipelineInfo {
        val language = I18nUtil.getLanguage(userId)
        val permission = AuthPermission.EDIT
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = permission,
            message = MessageUtil.getMessageByLocale(
                messageCode = USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                language = language,
                params = arrayOf(userId, projectId, permission.getI18n(language), pipelineId)
            )
        )

        val info = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
            )

        if (!pipelineRepositoryService.updateLocked(userId, projectId, pipelineId, locked)) { // 可能重复操作，不打扰用户
            logger.warn("Locked Pipeline|$userId|$projectId|$pipelineId|locked=$locked, may be duplicated")
        }
        operationLogService.addOperationLog(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = 0,
            operationLogType = if (locked) {
                OperationLogType.DISABLE_PIPELINE
            } else {
                OperationLogType.ENABLE_PIPELINE
            },
            params = "",
            description = null
        )

        return info
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineInfoFacadeService::class.java)
    }
}
