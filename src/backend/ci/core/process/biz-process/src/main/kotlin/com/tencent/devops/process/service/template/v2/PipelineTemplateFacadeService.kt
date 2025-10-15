package com.tencent.devops.process.service.template.v2

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.constant.CommonMessageCode.YAML_NOT_VALID
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import com.tencent.devops.common.pipeline.enums.TemplateRefType
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.transfer.TransferMark
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.common.pipeline.template.UpgradeStrategyEnum
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_LATEST_PUBLISHED_TEMPLATE_NOT_EXIST
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_RECENTLY_INSTALL_TEMPLATE_NOT_EXIST
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TEMPLATE_TRANSFORM_TO_CUSTOM
import com.tencent.devops.process.dao.label.PipelineLabelDao
import com.tencent.devops.process.dao.label.PipelineLabelPipelineDao
import com.tencent.devops.process.engine.dao.PipelineOperationLogDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.permission.template.PipelineTemplatePermissionService
import com.tencent.devops.process.pojo.PipelineOperationDetail
import com.tencent.devops.process.pojo.PipelinePermissions
import com.tencent.devops.process.pojo.pipeline.DeployTemplateResult
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileInfo
import com.tencent.devops.process.pojo.setting.PipelineVersionSimple
import com.tencent.devops.process.pojo.template.CloneTemplateSettingExist
import com.tencent.devops.process.pojo.template.HighlightType
import com.tencent.devops.process.pojo.template.OptionalTemplate
import com.tencent.devops.process.pojo.template.OptionalTemplateList
import com.tencent.devops.process.pojo.template.PipelineTemplateListResponse
import com.tencent.devops.process.pojo.template.PipelineTemplateListSimpleResponse
import com.tencent.devops.process.pojo.template.TemplatePreviewDetail
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.v2.PTemplateModelTransferResult
import com.tencent.devops.process.pojo.template.v2.PTemplatePipelineRefInfo
import com.tencent.devops.process.pojo.template.v2.PTemplateSource2Count
import com.tencent.devops.process.pojo.template.v2.PTemplateTransferBody
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCompareResponse
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCopyCreateReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCustomCreateReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateDetailsResponse
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateDraftReleaseReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateDraftRollbackReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateDraftSaveReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoResponse
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoUpdateInfo
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoV2
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateMarketCreateReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateMarketRelatedInfo
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateSettingCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateStrategyUpdateInfo
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateYamlWebhookReq
import com.tencent.devops.process.pojo.template.v2.PreFetchTemplateReleaseResult
import com.tencent.devops.process.pojo.template.v2.TemplateVersionPair
import com.tencent.devops.process.service.PipelineVersionFacadeService
import com.tencent.devops.process.service.pipeline.PipelineModelParser
import com.tencent.devops.process.service.pipeline.PipelineYamlVersionResolver
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionManager
import com.tencent.devops.process.util.FileExportUtil
import com.tencent.devops.process.yaml.PipelineYamlFacadeService
import com.tencent.devops.process.yaml.PipelineYamlService
import com.tencent.devops.process.yaml.transfer.PipelineTransferException
import com.tencent.devops.process.yaml.transfer.TransferMapper
import com.tencent.devops.store.api.template.ServiceTemplateResource
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import jakarta.ws.rs.core.Response
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线模版门面类
 */
@Service
class PipelineTemplateFacadeService @Autowired constructor(
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineTemplatePermissionService: PipelineTemplatePermissionService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val pipelineTemplateSettingService: PipelineTemplateSettingService,
    private val pipelineTemplateVersionManager: PipelineTemplateVersionManager,
    private val pipelineTemplateGenerator: PipelineTemplateGenerator,
    private val pipelineOperationLogDao: PipelineOperationLogDao,
    private val dslContext: DSLContext,
    private val pipelineYamlFacadeService: PipelineYamlFacadeService,
    private val pipelineYamlService: PipelineYamlService,
    private val pipelineYamlVersionResolver: PipelineYamlVersionResolver,
    private val pipelineTemplatePersistenceService: PipelineTemplatePersistenceService,
    private val client: Client,
    private val pipelineTemplateMarketFacadeService: PipelineTemplateMarketFacadeService,
    private val pipelineTemplateVersionValidator: PipelineTemplateVersionValidator,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineTemplateRelatedService: PipelineTemplateRelatedService,
    private val config: CommonConfig,
    private val pipelineVersionFacadeService: PipelineVersionFacadeService,
    private val pipelineLabelDao: PipelineLabelDao,
    private val pipelineLabelPipelineDao: PipelineLabelPipelineDao,
    private val pipelineModelParser: PipelineModelParser
) {
    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_CREATE,
        instance = AuditInstanceRecord(resourceType = ResourceTypeId.PIPELINE_TEMPLATE),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_CREATE_CONTENT
    )
    fun create(
        userId: String,
        projectId: String,
        request: PipelineTemplateCustomCreateReq
    ): DeployTemplateResult {
        logger.info("$userId create template in project $projectId by $request ,body is $request")
        return pipelineTemplateVersionManager.deployTemplate(
            userId = userId,
            projectId = projectId,
            request = request
        ).also {
            ActionAuditContext.current()
                .setInstanceId(it.templateId)
                .setInstanceName(it.templateName)
        }
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_CREATE,
        instance = AuditInstanceRecord(resourceType = ResourceTypeId.PIPELINE_TEMPLATE),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_CREATE_CONTENT
    )
    fun createByMarket(
        userId: String,
        projectId: String,
        templateId: String?,
        request: PipelineTemplateMarketCreateReq
    ): DeployTemplateResult {
        logger.info("$userId create template in project $projectId by market ,body is $request")
        pipelineTemplatePermissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.CREATE
        )
        return pipelineTemplateVersionManager.deployTemplate(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            request = request
        ).also {
            ActionAuditContext.current()
                .setInstanceId(it.templateId)
                .setInstanceName(it.templateName)
        }
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_CREATE,
        instance = AuditInstanceRecord(resourceType = ResourceTypeId.PIPELINE_TEMPLATE),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_EDIT_COPY_CONTENT
    )
    fun copy(
        userId: String,
        projectId: String,
        request: PipelineTemplateCopyCreateReq
    ): DeployTemplateResult {
        logger.info("$userId create template in project $projectId by copy ,body is $request")
        return pipelineTemplateVersionManager.deployTemplate(
            userId = userId,
            projectId = projectId,
            request = request
        ).also {
            ActionAuditContext.current()
                .setInstanceId(it.templateId)
                .setInstanceName(it.templateName)
        }
    }

    /**
     * 保存草稿
     */
    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_EDIT,
        instance = AuditInstanceRecord(resourceType = ResourceTypeId.PIPELINE_TEMPLATE),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_EDIT_CONTENT
    )
    fun saveDraft(
        userId: String,
        projectId: String,
        templateId: String?,
        request: PipelineTemplateDraftSaveReq
    ): DeployTemplateResult {
        return pipelineTemplateVersionManager.deployTemplate(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            request = request
        ).also {
            ActionAuditContext.current()
                .setInstanceId(it.templateId)
                .setInstanceName(it.templateName)
        }
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_CREATE,
        instance = AuditInstanceRecord(resourceType = ResourceTypeId.PIPELINE_TEMPLATE),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_CREATE_CONTENT
    )
    fun createYamlTemplate(
        userId: String,
        projectId: String,
        yaml: String,
        yamlFileName: String,
        branchName: String,
        isDefaultBranch: Boolean,
        description: String? = null,
        yamlFileInfo: PipelineYamlFileInfo? = null
    ): DeployTemplateResult {
        val request = PipelineTemplateYamlWebhookReq(
            yaml = yaml,
            yamlFileName = yamlFileName,
            branchName = branchName,
            isDefaultBranch = isDefaultBranch,
            description = description,
            yamlFileInfo = yamlFileInfo
        )
        return pipelineTemplateVersionManager.deployTemplate(
            userId = userId,
            projectId = projectId,
            request = request
        ).also {
            ActionAuditContext.current()
                .setInstanceId(it.templateId)
                .setInstanceName(it.templateName)
        }
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_EDIT,
        instance = AuditInstanceRecord(resourceType = ResourceTypeId.PIPELINE_TEMPLATE),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_EDIT_CONTENT
    )
    fun updateYamlTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        yaml: String,
        yamlFileName: String,
        branchName: String,
        isDefaultBranch: Boolean,
        description: String? = null,
        yamlFileInfo: PipelineYamlFileInfo? = null
    ): DeployTemplateResult {
        val request = PipelineTemplateYamlWebhookReq(
            yaml = yaml,
            yamlFileName = yamlFileName,
            branchName = branchName,
            isDefaultBranch = isDefaultBranch,
            description = description,
            yamlFileInfo = yamlFileInfo
        )
        return pipelineTemplateVersionManager.deployTemplate(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            request = request
        ).also {
            ActionAuditContext.current()
                .setInstanceId(it.templateId)
                .setInstanceName(it.templateName)
        }
    }

    fun preFetchDraftVersion(
        projectId: String,
        templateId: String,
        version: Long,
        customVersionName: String?,
        enablePac: Boolean,
        repoHashId: String?,
        targetAction: CodeTargetAction?,
        targetBranch: String?
    ): PreFetchTemplateReleaseResult {
        val draftResource = pipelineTemplateResourceService.get(
            projectId = projectId, templateId = templateId, version = version
        )
        if (draftResource.status != VersionStatus.COMMITTING) {
            throw ErrorCodeException(errorCode = ERROR_TEMPLATE_NOT_EXISTS)
        }
        val templateSetting = pipelineTemplateSettingService.get(
            projectId = projectId, templateId = templateId, settingVersion = draftResource.settingVersion
        )
        val resourceOnlyVersion = pipelineTemplateGenerator.generateDraftReleaseVersion(
            projectId = projectId,
            templateId = templateId,
            draftResource = draftResource,
            draftSetting = templateSetting,
            customVersionName = customVersionName,
            enablePac = enablePac,
            repoHashId = repoHashId,
            targetAction = targetAction,
            targetBranch = targetBranch
        ).second
        return PreFetchTemplateReleaseResult(
            templateId = templateId,
            templateName = templateSetting.pipelineName,
            version = resourceOnlyVersion.version,
            number = resourceOnlyVersion.number,
            newVersionNum = resourceOnlyVersion.versionNum,
            newVersionName = resourceOnlyVersion.versionName!!
        )
    }

    /**
     * 发布草稿
     */
    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_EDIT,
        instance = AuditInstanceRecord(resourceType = ResourceTypeId.PIPELINE_TEMPLATE),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_EDIT_CONTENT
    )
    fun releaseDraft(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        request: PipelineTemplateDraftReleaseReq
    ): DeployTemplateResult {
        logger.info("release draft version|projectId:$projectId|templateId:$templateId|version:$version")
        return pipelineTemplateVersionManager.deployTemplate(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = version,
            request = request
        ).also {
            ActionAuditContext.current()
                .setInstanceId(it.templateId)
                .setInstanceName(it.templateName)
        }
    }

    /**
     * 回滚草稿到指定版本
     */
    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_EDIT,
        instance = AuditInstanceRecord(resourceType = ResourceTypeId.PIPELINE_TEMPLATE),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_EDIT_CONTENT
    )
    fun rollbackDraft(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long
    ): DeployTemplateResult {
        return pipelineTemplateVersionManager.deployTemplate(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = version,
            request = PipelineTemplateDraftRollbackReq()
        ).also {
            ActionAuditContext.current()
                .setInstanceId(it.templateId)
                .setInstanceName(it.templateName)
        }
    }

    /**
     * 删除模版版本
     */
    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE_TEMPLATE,
            instanceNames = "#templateId",
            instanceIds = "#templateId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_DELETE_CONTENT
    )
    fun deleteVersion(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long
    ) {
        pipelineTemplateVersionManager.deleteVersion(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = version
        )
    }


    /**
     * 删除模版所有版本
     */
    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE_TEMPLATE,
            instanceNames = "#templateId",
            instanceIds = "#templateId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_DELETE_CONTENT
    )
    fun deleteTemplate(
        userId: String,
        projectId: String,
        templateId: String
    ): Boolean {
        pipelineTemplateVersionManager.deleteAllVersions(
            userId = userId,
            projectId = projectId,
            templateId = templateId
        )
        return true
    }

    /**
     * 将分支版本置为不活跃
     */
    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE_TEMPLATE,
            instanceNames = "#templateId",
            instanceIds = "#templateId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_DELETE_CONTENT
    )
    fun inactiveBranch(
        userId: String,
        projectId: String,
        templateId: String,
        branch: String
    ) {
        pipelineTemplateVersionManager.inactiveBranch(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            branch = branch
        )
    }

    // 获取模板列表
    fun listTemplateInfos(
        userId: String,
        commonCondition: PipelineTemplateCommonCondition
    ): SQLPage<PipelineTemplateListResponse> {
        logger.info("list template infos {}|{}", userId, commonCondition)
        val projectId = commonCondition.projectId!!
        val enableTemplatePermissionManage = pipelineTemplatePermissionService.enableTemplatePermissionManage(projectId)
        val (count, templateInfos) = if (enableTemplatePermissionManage) {
            processWithPermissions(userId, projectId, commonCondition)
        } else {
            processWithoutPermissions(userId, projectId, commonCondition)
        }

        return SQLPage(count, templateInfos)
    }

    fun listTemplateSimpleInfos(
        userId: String,
        commonCondition: PipelineTemplateCommonCondition
    ): SQLPage<PipelineTemplateListSimpleResponse> {
        val result = listTemplateInfos(
            userId = userId,
            commonCondition = commonCondition
        )
        return SQLPage(
            count = result.count,
            records = result.records.map {
                PipelineTemplateListSimpleResponse(
                    id = it.id,
                    name = it.name,
                    type = it.type
                )
            }
        )
    }

    private fun processWithPermissions(
        userId: String,
        projectId: String,
        condition: PipelineTemplateCommonCondition
    ): Pair<Long, List<PipelineTemplateListResponse>> {
        val permissionMap = pipelineTemplatePermissionService.getResourcesByPermission(
            userId = userId,
            projectId = projectId,
            permissions = setOf(AuthPermission.VIEW, AuthPermission.LIST, AuthPermission.DELETE, AuthPermission.EDIT)
        )
        val accessibleTemplateIds = permissionMap[AuthPermission.LIST] ?: return Pair(0L, emptyList())

        val queryCondition = condition.copy(filterTemplateIds = accessibleTemplateIds)
        val allTemplates = pipelineTemplateInfoService.list(queryCondition)

        return processTemplateList(
            allTemplates = allTemplates,
            totalCount = pipelineTemplateInfoService.count(queryCondition),
            getPermission = { templateId ->
                PipelinePermissions(
                    canView = permissionMap[AuthPermission.VIEW]?.contains(templateId) ?: false,
                    canEdit = permissionMap[AuthPermission.EDIT]?.contains(templateId) ?: false,
                    canDelete = permissionMap[AuthPermission.DELETE]?.contains(templateId) ?: false,
                    canManage = false
                )
            }
        )
    }

    private fun processWithoutPermissions(
        userId: String,
        projectId: String,
        condition: PipelineTemplateCommonCondition
    ): Pair<Long, List<PipelineTemplateListResponse>> {
        val allTemplates = pipelineTemplateInfoService.list(condition)
        val isProjectManager = pipelinePermissionService.checkProjectManager(userId, projectId)

        return processTemplateList(
            allTemplates = allTemplates,
            totalCount = pipelineTemplateInfoService.count(condition),
            getPermission = { _ ->
                PipelinePermissions(
                    canView = isProjectManager,
                    canEdit = isProjectManager,
                    canDelete = isProjectManager,
                    canManage = isProjectManager
                )
            }
        )
    }

    private fun processTemplateList(
        allTemplates: List<PipelineTemplateInfoV2>,
        totalCount: Int,
        getPermission: (String) -> PipelinePermissions
    ): Pair<Long, List<PipelineTemplateListResponse>> {
        val publishedTemplates = allTemplates.filter { it.storeStatus == TemplateStatusEnum.RELEASED }
        val marketTemplates = allTemplates.filter { it.mode == TemplateType.CONSTRAINT }
        // 已上架模板的最新发布版本
        val latestReleasedVersions = publishedTemplates.fetchVersions { ids ->
            pipelineTemplateResourceService.listLatestReleasedVersions(ids)
        }
        logger.debug("latestReleasedVersions :{}", latestReleasedVersions)
        // 已上架模板的最新上架商店版本
        val latestMarketVersions = publishedTemplates.fetchVersions { ids ->
            client.get(ServiceTemplateResource::class).listLatestPublishedVersions(ids).data ?: emptyList()
        }
        logger.debug("latestMarketVersions :{}", latestMarketVersions)

        // 模板最新安装的研发商店版本
        val latestInstalledVersions = marketTemplates.fetchVersions { ids ->
            client.get(ServiceTemplateResource::class).listLatestInstalledVersions(ids).data ?: emptyList()
        }
        logger.debug("latestInstalledVersions :{}", latestInstalledVersions)

        // 父模板最新发布版本
        val latestParentVersions = marketTemplates.takeIf { it.isNotEmpty() }?.let {
            client.get(ServiceTemplateResource::class).listLatestPublishedVersions(
                it.mapNotNull { t -> t.srcTemplateId }
            ).data
        } ?: emptyList()
        logger.debug("latestParentVersions :{}", latestParentVersions)

        // 处理每个模板
        val processedTemplates = allTemplates.map { template ->
            val upgradeFlag = if (template.mode == TemplateType.CONSTRAINT) {
                val installedVersion = latestInstalledVersions.firstOrNull { it.templateCode == template.id }
                val parentVersion = latestParentVersions.firstOrNull { it.templateCode == template.srcTemplateId }
                logger.debug("{} installedVersion({})|parentVersion({})", template.id, installedVersion, parentVersion)
                installedVersion != null && parentVersion != null && installedVersion.number < parentVersion.number
            } else {
                false
            }

            // 发布检查逻辑
            val publishFlag = if (template.storeStatus == TemplateStatusEnum.RELEASED) {
                val releasedVersion = latestReleasedVersions.firstOrNull { it.pipelineId == template.id }
                val marketVersion = latestMarketVersions.firstOrNull { it.templateCode == template.id }
                logger.debug("{} releasedVersion({})|marketVersion({})", template.id, releasedVersion, marketVersion)
                releasedVersion != null && marketVersion != null &&
                    releasedVersion.number.toLong() > marketVersion.number
            } else {
                false
            }

            PipelineTemplateListResponse(
                pipelineTemplateInfo = template,
                permission = getPermission(template.id),
                upgradeFlag = upgradeFlag,
                publishFlag = publishFlag,
                storeFlag = template.storeStatus == TemplateStatusEnum.RELEASED
            )
        }

        return Pair(totalCount.toLong(), processedTemplates)
    }

    // 获取各类最新版本
    fun <T> List<PipelineTemplateInfoV2>.fetchVersions(fetch: (List<String>) -> List<T>) =
        takeIf { it.isNotEmpty() }?.let { fetch(it.map { t -> t.id }) } ?: emptyList()

    fun listAllTemplates(
        userId: String,
        projectId: String
    ): OptionalTemplateList {
        logger.info("list all templates projectId={},userId={}", projectId, userId)
        val permissionMap = pipelineTemplatePermissionService.getResourcesByPermission(
            userId = userId,
            projectId = projectId,
            permissions = setOf(AuthPermission.LIST)
        )
        val hasListPermTemplates = permissionMap[AuthPermission.LIST] ?: emptyList()
        val emptyTemplate = pipelineTemplateInfoService.get(
            PipelineTemplateCommonCondition(
                projectId = "",
                mode = TemplateType.PUBLIC
            )
        )

        takeIf { hasListPermTemplates.isEmpty() }?.let {
            return OptionalTemplateList().withEmptyTemplate(emptyTemplate)
        }

        val templateInfos = pipelineTemplateInfoService.list(
            PipelineTemplateCommonCondition(
                projectId = projectId,
                filterTemplateIds = hasListPermTemplates,
                latestVersionStatus = VersionStatus.RELEASED,
                type = PipelineTemplateType.PIPELINE
            )
        )
        val templateId2Version = templateInfos.map { TemplateVersionPair(it.id, it.releasedVersion.toInt()) }
        val templateId2SettingVersion = templateInfos.map { TemplateVersionPair(it.id, it.releasedSettingVersion) }

        val template2Resource = pipelineTemplateResourceService.list(
            PipelineTemplateResourceCommonCondition(
                projectId = projectId,
                templateVersionPairs = templateId2Version
            )
        ).associateBy { it.templateId }

        val template2Settings = pipelineTemplateSettingService.list(
            PipelineTemplateSettingCommonCondition(
                projectId = projectId,
                templateVersionPairs = templateId2SettingVersion
            )
        ).associateBy { it.pipelineId }

        val templateIds = templateInfos.map { it.id }

        val pipelinesWithLabels = pipelineLabelPipelineDao.exitsLabelPipelines(
            dslContext = dslContext,
            projectId = projectId,
            pipelineIds = templateIds.toSet()
        )

        val templates = templateInfos.mapNotNull { templateInfo ->
            try {
                val category = templateInfo.category.takeIf { categoryStr -> !categoryStr.isNullOrBlank() }?.let {
                    JsonUtil.to(it, object : TypeReference<List<String>>() {})
                } ?: emptyList()

                OptionalTemplate(
                    name = templateInfo.name,
                    templateId = templateInfo.id,
                    projectId = templateInfo.projectId,
                    version = templateInfo.releasedVersion,
                    versionName = templateInfo.releasedVersionName!!,
                    templateType = templateInfo.mode.name,
                    srcTemplateId = templateInfo.srcTemplateId,
                    templateTypeDesc = templateInfo.desc ?: "",
                    category = category,
                    logoUrl = templateInfo.logoUrl ?: "",
                    stages = (template2Resource[templateInfo.id]?.model as? Model)?.stages ?: emptyList(),
                    cloneTemplateSettingExist = CloneTemplateSettingExist.fromSetting(
                        setting = template2Settings[templateInfo.id],
                        pipelinesWithLabels = pipelinesWithLabels
                    ),
                    desc = templateInfo.desc ?: ""
                )
            } catch (ex: Exception) {
                logger.error("build optional template failed {}", templateInfo.id, ex)
                null // 在发生异常时返回 null，mapNotNull 会自动过滤掉它
            }
        }
        return OptionalTemplateList(
            count = templates.size,
            page = 1,
            pageSize = templateInfos.size,
            templates = templates.associateBy { it.templateId }
        ).withEmptyTemplate(emptyTemplate)
    }

    // 查看模板详情
    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE_TEMPLATE,
            instanceIds = "#templateId",
            instanceNames = "#templateId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_VIEW_CONTENT
    )
    fun getTemplateDetails(
        projectId: String,
        templateId: String,
        version: Long?
    ): PipelineTemplateDetailsResponse {
        val templateResource = if (version == null) {
            pipelineTemplateResourceService.getLatestReleasedResource(
                projectId = projectId,
                templateId = templateId
            )
        } else {
            pipelineTemplateResourceService.get(
                projectId = projectId,
                templateId = templateId,
                version = version
            )
        } ?: throw ErrorCodeException(errorCode = ERROR_TEMPLATE_NOT_EXISTS)
        return getTemplateDetails(
            projectId = projectId,
            templateResource = templateResource
        )
    }

    private fun getTemplateDetails(
        projectId: String,
        templateResource: PipelineTemplateResource
    ): PipelineTemplateDetailsResponse {
        val setting = pipelineTemplateSettingService.get(
            projectId = projectId,
            templateId = templateResource.templateId,
            settingVersion = templateResource.settingVersion
        ).let { pipelineSetting ->
            val labelIds = pipelineSetting.labels.map { HashUtil.decodeIdToLong(it) }.toSet()
            val labelNames = pipelineLabelDao.getByIds(dslContext, projectId, labelIds).map { it.name }
            pipelineSetting.copy(labelNames = labelNames)
        }
        val (yamlSupported, yamlPreview, msg) = try {
            val yaml = templateResource.yaml ?: pipelineTemplateGenerator.transfer(
                userId = templateResource.creator,
                projectId = templateResource.projectId,
                storageType = PipelineStorageType.MODEL,
                templateType = templateResource.type,
                templateModel = templateResource.model,
                params = templateResource.params,
                templateSetting = setting,
                yaml = null
            ).yamlWithVersion?.yamlStr ?: ""
            val response = pipelineTemplateGenerator.buildPreView(yaml)
            Triple(true, response, null)
        } catch (e: PipelineTransferException) {
            Triple(
                first = false,
                second = null,
                third = I18nUtil.getCodeLanMessage(
                    messageCode = e.errorCode,
                    params = e.params,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                    defaultMessage = e.defaultMessage
                )
            )
        }
        val buildNo = if (templateResource.type == PipelineTemplateType.PIPELINE) {
            (templateResource.model as Model).getTriggerContainer().buildNo
        } else {
            null
        }
        return PipelineTemplateDetailsResponse(
            resource = templateResource,
            setting = setting,
            buildNo = buildNo,
            params = templateResource.params,
            yamlSupported = yamlSupported,
            yamlPreview = yamlPreview,
            yamlInvalidMsg = msg
        )
    }

    fun previewTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        highlightType: HighlightType?
    ): TemplatePreviewDetail {
        logger.info("previewTemplate|projectId:$projectId|templateId:$templateId|version:$version|userId:$userId")

        // 获取模板资源
        val templateResource = pipelineTemplateResourceService.get(
            projectId = projectId,
            templateId = templateId,
            version = version
        )

        // 获取模板设置
        val setting = pipelineTemplateSettingService.get(
            projectId = projectId,
            templateId = templateId,
            settingVersion = templateResource.settingVersion
        )

        // 检查权限
        val hasPermission = pipelineTemplatePermissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            permission = AuthPermission.VIEW
        )

        // 转换为 YAML
        val (yamlSupported, yaml, yamlInvalidMsg) = try {
            val yaml = templateResource.yaml ?: pipelineTemplateGenerator.transfer(
                userId = templateResource.creator,
                projectId = templateResource.projectId,
                storageType = PipelineStorageType.MODEL,
                templateType = templateResource.type,
                templateModel = templateResource.model,
                params = templateResource.params,
                templateSetting = setting,
                yaml = null
            ).yamlWithVersion?.yamlStr ?: ""
            Triple(true, yaml, null)
        } catch (e: PipelineTransferException) {
            Triple(
                first = false,
                second = null,
                third = I18nUtil.getCodeLanMessage(
                    messageCode = e.errorCode,
                    params = e.params,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                    defaultMessage = e.defaultMessage
                )
            )
        }
        val pipelineModelKey = setOf("stages", "jobs", "steps", "finally")
        // 处理高亮标记
        val highlightMarkList = mutableListOf<TransferMark>()
        if (yaml != null && highlightType != null) {
            run outside@{
                try {
                    TransferMapper.getYamlLevelOneIndex(yaml).forEach { (key, value) ->
                        when {
                            highlightType == HighlightType.LABEL && key == "label" -> {
                                highlightMarkList.add(value)
                                return@outside
                            }

                            highlightType == HighlightType.CONCURRENCY && key == "concurrency" -> {
                                highlightMarkList.add(value)
                                return@outside
                            }

                            highlightType == HighlightType.NOTIFY && key == "notices" -> {
                                highlightMarkList.add(value)
                                return@outside
                            }

                            highlightType == HighlightType.PIPELINE_MODEL && key in pipelineModelKey -> {
                                highlightMarkList.add(value)
                                // pipelineModel 可能多个
                                return@forEach
                            }
                        }
                    }
                } catch (ignore: Throwable) {
                    logger.warn("TRANSFER_YAML|$projectId|$userId|$templateId|$version", ignore)
                }
            }
        }

        return TemplatePreviewDetail(
            template = templateResource.model,
            templateYaml = yaml,
            setting = setting,
            hasPermission = hasPermission,
            highlightMarkList = highlightMarkList,
            yamlSupported = yamlSupported,
            yamlInvalidMsg = yamlInvalidMsg
        )
    }


    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE_TEMPLATE,
            instanceIds = "#templateId",
            instanceNames = "#templateId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_VIEW_CONTENT
    )
    fun getRefTemplateDetails(
        userId: String,
        projectId: String,
        templateId: String,
        ref: String
    ): PipelineTemplateDetailsResponse {
        val pipelineYamlInfo = pipelineYamlService.getPipelineYamlInfo(
            projectId = projectId,
            pipelineId = templateId,
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_TEMPLATE_NOT_ENABLE_PAC
        )
        val pipelineYamlVersion = pipelineYamlVersionResolver.resolveTemplateRefVersion(
            projectId = projectId,
            repoHashId = pipelineYamlInfo.repoHashId,
            filePath = pipelineYamlInfo.filePath,
            ref = ref
        )
        return getTemplateDetails(
            projectId = projectId,
            templateId = templateId,
            version = pipelineYamlVersion.version.toLong()
        )
    }

    fun getPipelineRelatedTemplateDetails(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int
    ): PipelineTemplateDetailsResponse? {
        val pipelineResource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            includeDraft = true
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
            params = arrayOf(version.toString())
        )

        val instanceFromTemplate = pipelineTemplateRelatedService.isPipelineInstanceFromTemplate(
            projectId = projectId,
            pipelineId = pipelineId
        )
        if (!instanceFromTemplate || pipelineResource.model.template == null) {
            return null
        }

        val templateResource = pipelineModelParser.parseTemplateDescriptor(
            projectId = projectId,
            descriptor = pipelineResource.model.template!!,
            pipelineId = pipelineId
        )

        return getTemplateDetails(
            projectId = projectId,
            templateResource = templateResource
        )
    }

    fun getPipelineRelatedTemplateInfo(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int
    ): PTemplatePipelineRefInfo? {
        val pipelineResource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            includeDraft = true
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
            params = arrayOf(version.toString())
        )

        val instanceFromTemplate = pipelineTemplateRelatedService.isPipelineInstanceFromTemplate(
            projectId = projectId,
            pipelineId = pipelineId
        )
        val templateDescriptor = pipelineResource.model.template
        if (!instanceFromTemplate || templateDescriptor == null) {
            return null
        }

        val templateResource = pipelineModelParser.parseTemplateDescriptor(
            projectId = projectId,
            descriptor = templateDescriptor,
            pipelineId = pipelineId
        )

        val templateId = templateResource.templateId
        val templateVersion = templateResource.version
        val templateInfo = pipelineTemplateInfoService.get(templateId)
        val templateRefType = templateDescriptor.templateRefType

        return if (templateRefType == TemplateRefType.ID) {
            val templateDetailsUrl =
                String.format(templateDetailRedirectUri, projectId, templateId, templateVersion).plus("/pipeline")
            val pipelineReleaseVersion = pipelineVersionFacadeService.getPipelineDetailIncludeDraft(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId
            ).releaseVersion
            // 当前流水版本为最新版本，并且关联的模板版本不是最新版本，则需要升级
            val upgradeFlag = pipelineReleaseVersion == version && templateInfo.releasedVersion != templateVersion
            val upgradeUrl = takeIf { upgradeFlag }?.let {
                String.format(templateDetailRedirectUri, projectId, templateId, templateInfo.releasedVersion)
            }
            PTemplatePipelineRefInfo(
                templateName = templateInfo.name,
                templateId = templateInfo.id,
                templateVersionName = templateResource.versionName,
                templateVersion = templateVersion,
                refType = templateRefType,
                templateDetailsUrl = templateDetailsUrl,
                upgradeFlag = upgradeFlag,
                upgradeUrl = upgradeUrl
            )
        } else {
            val templateDetailsUrl = pipelineYamlFacadeService.getPipelineYamlInfo(
                projectId = projectId,
                pipelineId = templateId,
                version = templateVersion.toInt()
            )?.fileUrl
            PTemplatePipelineRefInfo(
                templateName = templateInfo.name,
                templateId = templateInfo.id,
                templateVersionName = templateResource.versionName,
                templateVersion = templateVersion,
                refType = templateRefType,
                templateDetailsUrl = templateDetailsUrl,
                upgradeFlag = false,
                upgradeUrl = null
            )
        }
    }

    fun getTemplateInfo(
        userId: String,
        projectId: String,
        templateId: String
    ): PipelineTemplateInfoResponse {
        val basicInfo = pipelineTemplateInfoService.get(projectId, templateId)
        val draftResource = pipelineTemplateResourceService.getDraftVersionResource(
            projectId = projectId,
            templateId = templateId
        )
        val releaseResource = pipelineTemplateResourceService.get(
            projectId = projectId,
            templateId = templateId,
            version = basicInfo.releasedVersion
        )
        val baseResource = draftResource?.baseVersion?.let {
            pipelineTemplateResourceService.get(
                projectId = projectId,
                templateId = templateId,
                version = it
            )
        }

        /**
         * 获取最新版本和版本名称
         *
         * 如果最新版本是分支版本,则需要获取分支最新的激活版本,否则最新版本可能是正式或者草稿版本
         */
        val (releaseVersion, releaseVersionName) = when (basicInfo.latestVersionStatus) {
            // 分支版本,需要获取当前分支最新的激活版本
            VersionStatus.BRANCH -> {
                val branchVersion = basicInfo.releasedVersionName?.let {
                    pipelineTemplateResourceService.getLatestBranchResource(
                        projectId = projectId,
                        templateId = templateId,
                        branchName = it
                    )
                }
                Pair(branchVersion?.version ?: releaseResource.version, branchVersion?.versionName)
            }

            else -> {
                Pair(releaseResource.version, releaseResource.versionName)
            }
        }
        // 草稿版本和版本名,如果有草稿版本,则使用草稿版本,否则使用最新版本
        val (version, versionName) = if (draftResource == null) {
            Pair(releaseVersion, releaseVersionName)
        } else {
            Pair(draftResource.version, null)
        }
        val permission2TemplatesMap = pipelineTemplatePermissionService.getResourcesByPermission(
            userId = userId,
            projectId = projectId,
            permissions = setOf(
                AuthPermission.VIEW,
                AuthPermission.DELETE,
                AuthPermission.EDIT,
                AuthPermission.MANAGE,
            )
        )
        val yamlInfo = pipelineYamlFacadeService.getPipelineYamlInfo(
            projectId = projectId,
            pipelineId = templateId,
            version = releaseVersion.toInt()
        )
        val yamlExist = pipelineYamlFacadeService.yamlExistInDefaultBranch(
            projectId = projectId,
            pipelineId = templateId
        )

        val pipelineTemplateMarketRelatedInfo = basicInfo.takeIf { it.mode == TemplateType.CONSTRAINT }?.let {
            if (it.srcTemplateProjectId == null || it.srcTemplateId == null) {
                throw IllegalArgumentException("srcTemplateProjectId or srcTemplateId is null")
            }
            val recentlyInstalledVersion = client.get(ServiceTemplateResource::class).getRecentlyInstalledVersion(
                templateCode = templateId
            ).data ?: throw ErrorCodeException(
                errorCode = ERROR_RECENTLY_INSTALL_TEMPLATE_NOT_EXIST,
                params = arrayOf(templateId)
            )

            val srcTemplateLatestReleasedVersion =
                client.get(ServiceTemplateResource::class).getLatestMarketPublishedVersion(
                    templateCode = it.srcTemplateId!!
                ).data ?: throw ErrorCodeException(
                    errorCode = ERROR_LATEST_PUBLISHED_TEMPLATE_NOT_EXIST,
                    params = arrayOf(it.srcTemplateId!!)
                )

            val srcMarketTemplateInfo = pipelineTemplateInfoService.get(
                projectId = it.srcTemplateProjectId!!,
                templateId = it.srcTemplateId!!
            )
            val marketTemplateDetails = client.get(ServiceTemplateResource::class).getTemplateDetailByCode(
                userId = userId,
                templateCode = it.srcTemplateId!!
            ).data ?: throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_SOURCE_TEMPLATE_NOT_EXISTS)
            PipelineTemplateMarketRelatedInfo(
                srcMarketProjectId = srcMarketTemplateInfo.projectId,
                srcMarketTemplateId = srcMarketTemplateInfo.id,
                srcMarketTemplateName = marketTemplateDetails.templateName,
                srcMarketTemplateLatestVersion = srcTemplateLatestReleasedVersion.version,
                srcMarketTemplateLatestVersionName = srcTemplateLatestReleasedVersion.versionName,
                latestInstalledVersion = recentlyInstalledVersion.version,
                latestInstalledVersionName = recentlyInstalledVersion.versionName,
                upgradeStrategy = it.upgradeStrategy!!,
                settingSyncStrategy = it.settingSyncStrategy!!,
                latestInstaller = recentlyInstalledVersion.creator,
                latestInstalledTime = recentlyInstalledVersion.createTime!!
            )
        }

        val latestReleasedVersionNum = pipelineTemplateResourceService.getLatestReleasedResource(
            projectId = projectId,
            templateId = templateId
        )?.number

        val latestMarketPublishedVersionNum =
            client.get(ServiceTemplateResource::class).getLatestMarketPublishedVersion(templateId).data?.number

        val publishFlag = takeIf { basicInfo.storeStatus == TemplateStatusEnum.RELEASED }?.let {
            latestReleasedVersionNum != null && latestMarketPublishedVersionNum != null &&
                latestReleasedVersionNum > latestMarketPublishedVersionNum
        } ?: false

        return PipelineTemplateInfoResponse(
            id = basicInfo.id,
            projectId = basicInfo.projectId,
            name = basicInfo.name,
            desc = basicInfo.desc,
            mode = basicInfo.mode,
            publishStrategy = basicInfo.publishStrategy,
            category = basicInfo.category,
            type = basicInfo.type,
            logoUrl = basicInfo.logoUrl,
            enablePac = basicInfo.enablePac,
            storeFlag = basicInfo.storeStatus == TemplateStatusEnum.RELEASED,
            publishFlag = publishFlag,
            srcTemplateId = basicInfo.srcTemplateId,
            srcTemplateProjectId = basicInfo.srcTemplateProjectId,
            canDebug = draftResource != null,
            debugPipelineCount = basicInfo.debugPipelineCount,
            instancePipelineCount = basicInfo.instancePipelineCount,
            creator = basicInfo.creator,
            updater = basicInfo.updater,
            createTime = basicInfo.createdTime,
            updateTime = basicInfo.updateTime,
            permissions = PipelinePermissions(
                canView = permission2TemplatesMap[AuthPermission.VIEW]?.contains(basicInfo.id) ?: false,
                canEdit = permission2TemplatesMap[AuthPermission.EDIT]?.contains(basicInfo.id) ?: false,
                canDelete = permission2TemplatesMap[AuthPermission.DELETE]?.contains(basicInfo.id) ?: false,
                canManage = permission2TemplatesMap[AuthPermission.MANAGE]?.contains(basicInfo.id) ?: false,
            ),
            canRelease = draftResource?.model != null,
            version = version,
            versionName = versionName,
            baseVersion = baseResource?.version,
            baseVersionName = baseResource?.versionName,
            baseVersionStatus = baseResource?.status,
            releaseVersion = releaseVersion,
            releaseVersionName = releaseVersionName,
            latestVersionStatus = basicInfo.latestVersionStatus,
            pipelineAsCodeSettings = PipelineAsCodeSettings(
                enable = yamlInfo != null
            ),
            yamlInfo = yamlInfo,
            yamlExist = yamlExist,
            pipelineTemplateMarketRelatedInfo = pipelineTemplateMarketRelatedInfo
        )
    }

    fun getType2Count(
        userId: String,
        projectId: String
    ): Map<String, Int> {
        val accessibleTemplateIds = pipelineTemplatePermissionService.getResourcesByPermission(
            userId = userId,
            projectId = projectId,
            permissions = setOf(AuthPermission.LIST)
        )[AuthPermission.LIST] ?: emptyList()
        return pipelineTemplateInfoService.getType2Count(projectId, accessibleTemplateIds)
    }

    fun getSource2Count(
        userId: String,
        projectId: String,
        commonCondition: PipelineTemplateCommonCondition
    ): PTemplateSource2Count {
        val accessibleTemplateIds = pipelineTemplatePermissionService.getResourcesByPermission(
            userId = userId,
            projectId = projectId,
            permissions = setOf(AuthPermission.LIST)
        )[AuthPermission.LIST] ?: emptyList()
        commonCondition.filterTemplateIds = accessibleTemplateIds
        return pipelineTemplateInfoService.getSource2Count(commonCondition)
    }

    fun getTemplateVersions(
        projectId: String,
        templateId: String,
        commonCondition: PipelineTemplateResourceCommonCondition
    ): Page<PipelineVersionSimple> {
        with(commonCondition) {
            val finCondition = upgradableVersionsQuery?.takeIf { it }?.let {
                client.get(ServiceTemplateResource::class).getLatestInstalledVersion(
                    templateCode = templateId
                ).data?.let { latestInstalled ->
                    PipelineTemplateResourceCommonCondition(
                        projectId = latestInstalled.srcMarketTemplateProjectCode,
                        templateId = latestInstalled.srcMarketTemplateCode,
                        gtNumber = latestInstalled.number,
                        status = VersionStatus.RELEASED,
                        storeStatus = TemplateStatusEnum.RELEASED
                    )
                } ?: return Page(page = -1, pageSize = -1, records = emptyList(), count = 0)
            } ?: commonCondition  // 默认使用原始条件
            val templateInfo = pipelineTemplateInfoService.get(projectId = projectId, templateId = templateId)
            val records = pipelineTemplateResourceService.getTemplateVersions(finCondition).map {
                if (it.version == templateInfo.releasedVersion.toInt()) {
                    it.latestReleasedFlag = true
                }
                it
            }
            val count = pipelineTemplateResourceService.count(finCondition)
            return Page(
                page = commonCondition.page ?: -1,
                pageSize = commonCondition.pageSize ?: -1,
                records = records,
                count = count.toLong()
            )
        }
    }

    // 模板版本对比
    fun compare(
        projectId: String,
        templateId: String,
        baseVersion: Long,
        comparedVersion: Long
    ): PipelineTemplateCompareResponse {
        pipelineTemplateInfoService.get(
            projectId = projectId,
            templateId = templateId
        )
        val baseVersionResource = pipelineTemplateResourceService.get(
            projectId = projectId,
            templateId = templateId,
            version = baseVersion
        )
        val comparedVersionResource = pipelineTemplateResourceService.get(
            projectId = projectId,
            templateId = templateId,
            version = comparedVersion
        )
        return PipelineTemplateCompareResponse(
            baseVersionResource = baseVersionResource,
            comparedVersionResource = comparedVersionResource
        )
    }

    fun transfer(
        userId: String,
        projectId: String,
        storageType: PipelineStorageType,
        body: PTemplateTransferBody
    ): PTemplateModelTransferResult {
        return pipelineTemplateGenerator.transfer(
            userId = userId,
            projectId = projectId,
            storageType = storageType,
            templateType = body.templateType,
            templateModel = body.templateModel,
            templateSetting = body.templateSetting,
            params = body.params,
            yaml = body.yaml
        )
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE_TEMPLATE,
            instanceIds = "#templateId",
            instanceNames = "#templateId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_EXPORT_CONTENT
    )
    fun exportTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long?
    ): Response {
        val templateInfo = pipelineTemplateInfoService.get(
            projectId = projectId,
            templateId = templateId
        )
        val templateResource = version?.let {
            pipelineTemplateResourceService.get(
                projectId = projectId,
                templateId = templateId,
                version = version
            )
        } ?: pipelineTemplateResourceService.getLatestVersionResource(
            projectId = projectId,
            templateId = templateId
        ) ?: throw ErrorCodeException(errorCode = ERROR_TEMPLATE_NOT_EXISTS)
        val setting = pipelineTemplateSettingService.get(
            projectId = projectId,
            templateId = templateId,
            settingVersion = templateResource.settingVersion
        )

        val yamlStr = pipelineTemplateGenerator.transfer(
            userId = userId,
            projectId = projectId,
            storageType = PipelineStorageType.MODEL,
            templateType = templateResource.type,
            templateModel = templateResource.model,
            params = templateResource.params,
            templateSetting = setting,
            yaml = templateResource.yaml
        ).yamlWithVersion?.yamlStr
        if (yamlStr == null) {
            throw ErrorCodeException(errorCode = YAML_NOT_VALID)
        }
        return FileExportUtil.exportStringToFile(
            content = yamlStr,
            fileName = "${templateInfo.name}.yaml"
        )
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE_TEMPLATE,
            instanceIds = "#templateId",
            instanceNames = "#templateId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_EDIT_CONTENT
    )
    fun transformTemplateToCustom(
        userId: String,
        projectId: String,
        templateId: String
    ): Boolean {
        val templateInfo = pipelineTemplateInfoService.get(
            projectId = projectId,
            templateId = templateId
        )
        if (templateInfo.mode != TemplateType.CONSTRAINT) {
            throw ErrorCodeException(errorCode = ERROR_TEMPLATE_TRANSFORM_TO_CUSTOM)
        }
        pipelineTemplatePersistenceService.transformTemplateToCustom(
            userId = userId,
            projectId = projectId,
            templateId = templateId
        )
        return true
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE_TEMPLATE,
            instanceIds = "#templateId",
            instanceNames = "#templateId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_VIEW_CONTENT
    )
    fun getOperationLogsInPage(
        userId: String,
        projectId: String,
        templateId: String,
        creator: String?,
        page: Int?,
        pageSize: Int?
    ): Page<PipelineOperationDetail> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        var slqLimit: SQLLimit? = null
        if (pageSizeNotNull != -1) slqLimit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val offset = slqLimit?.offset ?: 0
        val limit = slqLimit?.limit ?: -1
        val opCount = pipelineOperationLogDao.getCountByPipeline(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = templateId,
            creator = if (creator.isNullOrBlank()) null else creator
        )
        val opList = pipelineOperationLogDao.getListByPipeline(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = templateId,
            creator = if (creator.isNullOrBlank()) null else creator,
            offset = offset,
            limit = limit
        )
        val versions = mutableSetOf<Int>()
        opList.forEach { versions.add(it.version) }
        val versionMap = pipelineTemplateResourceService.getTemplateVersions(
            PipelineTemplateResourceCommonCondition(
                projectId = projectId,
                templateId = templateId,
            )
        ).associateBy { it.version }
        val detailList = opList.map {
            with(it) {
                val operationLogStr = "${operationLogType.getI18n(I18nUtil.getRequestUserLanguage())} $params"
                PipelineOperationDetail(
                    id = id,
                    projectId = projectId,
                    pipelineId = templateId,
                    version = version,
                    operator = operator,
                    operationLogType = operationLogType,
                    operationLogStr = operationLogStr,
                    params = params,
                    description = description,
                    operateTime = operateTime,
                    versionName = versionMap[it.version]?.versionName,
                    versionCreateTime = versionMap[it.version]?.createTime,
                    status = versionMap[it.version]?.status
                )
            }
        }
        return Page(
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            count = opCount.toLong(),
            records = detailList
        )
    }

    fun checkWhenPublishedTemplate(
        projectId: String,
        userId: String,
        templateId: String,
        version: Long
    ): Boolean {
        val templateResource = pipelineTemplateResourceService.get(
            projectId = projectId,
            templateId = templateId,
            version = version
        )
        val setting = pipelineTemplateSettingService.get(
            projectId = projectId,
            templateId = templateId,
            settingVersion = templateResource.settingVersion
        )
        if (templateResource.storeStatus == TemplateStatusEnum.RELEASED) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_VERSION_HAS_PUBLISHED,
                params = arrayOf(templateResource.versionName ?: templateResource.version.toString())
            )
        }
        pipelineTemplateVersionValidator.validateModelInfo(
            userId = userId,
            projectId = projectId,
            templateModel = templateResource.model,
            pipelineAsCodeSettings = setting.pipelineAsCodeSettings
        )
        return true
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_TEMPLATE_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE_TEMPLATE,
            instanceIds = "#templateId",
            instanceNames = "#templateId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_TEMPLATE_EDIT_CONTENT
    )
    fun updateUpgradeStrategy(
        userId: String,
        projectId: String,
        templateId: String,
        request: PipelineTemplateStrategyUpdateInfo
    ): Boolean {
        val templateInfo = pipelineTemplateInfoService.get(
            projectId = projectId,
            templateId = templateId
        )
        pipelineTemplateInfoService.update(
            record = PipelineTemplateInfoUpdateInfo(
                upgradeStrategy = request.upgradeStrategy,
                settingSyncStrategy = request.settingSyncStrategy,
                updater = userId
            ),
            commonCondition = PipelineTemplateCommonCondition(
                projectId = projectId,
                templateId = templateId
            )
        )
        if (templateInfo.upgradeStrategy == UpgradeStrategyEnum.MANUAL &&
            request.upgradeStrategy == UpgradeStrategyEnum.AUTO) {
            val srcTemplateResource = pipelineTemplateResourceService.getLatestReleasedResource(
                projectId = templateInfo.srcTemplateProjectId!!,
                templateId = templateInfo.srcTemplateId!!
            )!!
            pipelineTemplateMarketFacadeService.installNewVersion(
                templateInfo = templateInfo,
                srcTemplateProjectId = srcTemplateResource.projectId,
                srcTemplateId = srcTemplateResource.templateId,
                srcTemplateVersion = srcTemplateResource.version,
                srcTemplateNumber = srcTemplateResource.number,
                srcTemplateVersionName = srcTemplateResource.versionName!!
            )
        }
        return true
    }

    private val templateDetailRedirectUri = "${config.devopsHostGateway}/console/pipeline/%s/template/%s/%s"

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateFacadeService::class.java)
    }
}
