package com.tencent.devops.process.service.template.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceField
import com.tencent.devops.common.pipeline.pojo.element.atom.PipelineCheckFailedErrors
import com.tencent.devops.common.pipeline.pojo.element.atom.PipelineCheckFailedMsg
import com.tencent.devops.common.pipeline.pojo.element.atom.PipelineCheckFailedReason
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType
import com.tencent.devops.common.pipeline.pojo.transfer.TransferBody
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_ELEMENT_CHECK_FAILED
import com.tencent.devops.process.constant.ProcessMessageCode.USER_NEED_PIPELINE_X_PERMISSION
import com.tencent.devops.process.engine.cfg.PipelineIdGenerator
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import com.tencent.devops.process.engine.dao.template.TemplateInstanceBaseDao
import com.tencent.devops.process.engine.dao.template.TemplateInstanceItemDao
import com.tencent.devops.process.engine.pojo.event.PipelineTemplateInstanceEvent
import com.tencent.devops.process.engine.service.PipelineInfoService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.permission.template.PipelineTemplatePermissionService
import com.tencent.devops.process.pojo.pipeline.PrefetchReleaseResult
import com.tencent.devops.process.pojo.pipeline.version.PipelineTemplateInstanceReq
import com.tencent.devops.process.pojo.template.TemplateInstanceParams
import com.tencent.devops.process.pojo.template.TemplateInstanceStatus
import com.tencent.devops.process.pojo.template.TemplateOperationMessage
import com.tencent.devops.process.pojo.template.TemplateOperationRet
import com.tencent.devops.process.pojo.template.TemplatePipelineStatus
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstanceBase
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstanceCompareResponse
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstanceReleaseInfo
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstancesRequest
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstancesTaskDetail
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstancesTaskResult
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateRelatedResp
import com.tencent.devops.process.pojo.template.v2.TemplateInstanceType
import com.tencent.devops.process.service.ParamFacadeService
import com.tencent.devops.process.service.PipelineVersionFacadeService
import com.tencent.devops.process.service.pipeline.PipelineTransferYamlService
import com.tencent.devops.process.service.pipeline.PipelineYamlVersionResolver
import com.tencent.devops.process.service.pipeline.version.PipelineVersionGenerator
import com.tencent.devops.process.service.pipeline.version.PipelineVersionManager
import com.tencent.devops.process.yaml.PipelineYamlService
import com.tencent.devops.repository.api.ServiceRepositoryResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service

@Service
class PipelineTemplateInstanceService @Autowired constructor(
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val pipelineTemplateRelatedService: PipelineTemplateRelatedService,
    private val dslContext: DSLContext,
    private val templateInstanceItemDao: TemplateInstanceItemDao,
    private val templateInstanceBaseDao: TemplateInstanceBaseDao,
    private val pipelineIdGenerator: PipelineIdGenerator,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineVersionManager: PipelineVersionManager,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineResourceDao: PipelineResourceDao,
    private val objectMapper: ObjectMapper,
    private val paramService: ParamFacadeService,
    private val pipelineVersionFacadeService: PipelineVersionFacadeService,
    private val pipelineVersionGenerator: PipelineVersionGenerator,
    private val pipelineYamlService: PipelineYamlService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineYamlVersionResolver: PipelineYamlVersionResolver,
    private val permissionService: PipelineTemplatePermissionService,
    private val client: Client,
    private val transferService: PipelineTransferYamlService,
    private val pipelineTemplateGenerator: PipelineTemplateGenerator,
    private val pipelineTemplateSettingService: PipelineTemplateSettingService,
    private val pipelineInfoService: PipelineInfoService,
) {
    /*同步创建模板实例*/
    fun createTemplateInstances(
        projectId: String,
        userId: String,
        templateId: String,
        version: Long,
        request: PipelineTemplateInstancesRequest
    ): TemplateOperationRet {
        logger.info("template instance creation start $projectId|$userId|$templateId")
        val permissionCheck = pipelinePermissionService.checkPipelinePermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.CREATE
        )
        if (!permissionCheck) {
            throw PermissionForbiddenException(
                MessageUtil.getMessageByLocale(
                    messageCode = USER_NEED_PIPELINE_X_PERMISSION,
                    params = arrayOf(AuthPermission.CREATE.getI18n(I18nUtil.getLanguage(userId))),
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        val instances = request.instanceReleaseInfos
        val successPipelines = mutableListOf<String>()
        val failurePipelines = mutableListOf<String>()
        val successPipelineIds = mutableListOf<String>()
        val failureMessages = mutableMapOf<String, String>()

        instances.forEach { instance ->
            try {
                if (instance.pipelineName.isBlank()) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_EMPTY,
                        params = arrayOf(PipelineTemplateInstanceReleaseInfo::pipelineName.name)
                    )
                }
                val instanceCreateReq = PipelineTemplateInstanceReq(
                    projectId = projectId,
                    templateId = templateId,
                    templateVersion = version,
                    templateRefType = request.templateRefType,
                    templateRef = request.templateRef,
                    pipelineName = instance.pipelineName,
                    buildNo = instance.buildNo,
                    params = instance.param,
                    triggerConfigs = instance.triggerConfigs,
                    overrideTemplateField = instance.overrideTemplateField,
                    useTemplateSetting = request.useTemplateSettings,
                    enablePac = request.enablePac,
                    repoHashId = request.repoHashId,
                    filePath = instance.filePath,
                    targetAction = request.targetAction,
                    targetBranch = request.targetBranch
                )
                val deployPipeline = pipelineVersionManager.deployPipeline(
                    userId = userId,
                    projectId = projectId,
                    request = instanceCreateReq
                )
                successPipelines.add(instance.pipelineName)
                successPipelineIds.add(deployPipeline.pipelineId)
            } catch (ignored: Throwable) {
                val errorMessage = translateInstanceException(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = instance.pipelineId,
                    exception = ignored,
                )
                failurePipelines.add(instance.pipelineName)
                failureMessages[instance.pipelineName] = errorMessage.message
            }
        }
        return TemplateOperationRet(
            0,
            TemplateOperationMessage(
                successPipelines = successPipelines,
                failurePipelines = failurePipelines,
                failureMessages = failureMessages,
                successPipelinesId = successPipelineIds
            ),
            ""
        )
    }

    /*异步创建流水线模板实例*/
    fun asyncCreateTemplateInstances(
        projectId: String,
        userId: String,
        templateId: String,
        version: Long,
        request: PipelineTemplateInstancesRequest
    ): String {
        logger.info(
            "async template instance creation start $projectId|$userId|$templateId|$version|$request"
        )
        val permissionCheck = pipelinePermissionService.checkPipelinePermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.CREATE
        )
        if (!permissionCheck) {
            throw PermissionForbiddenException(
                MessageUtil.getMessageByLocale(
                    messageCode = USER_NEED_PIPELINE_X_PERMISSION,
                    params = arrayOf(AuthPermission.CREATE.getI18n(I18nUtil.getLanguage(userId))),
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        request.instanceReleaseInfos.forEach { instance ->
            if (instance.pipelineName.isBlank()) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf(PipelineTemplateInstanceReleaseInfo::pipelineName.name)
                )
            }
        }
        pipelineTemplateResourceService.get(projectId, templateId, version)
        val instances = request.instanceReleaseInfos.map {
            it.copy(pipelineId = pipelineIdGenerator.getNextId())
        }
        val baseId = UUIDUtil.generate()

        val pipelineIds = instances.map { it.pipelineId }.toSet()
        val templateInstanceItems = templateInstanceItemDao.getTemplateInstanceItemListByPipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            pipelineIds = pipelineIds
        )
        if (!templateInstanceItems.isNullOrEmpty()) {
            val pipelineNames = templateInstanceItems.map { it.pipelineName }
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_PIPELINE_IS_INSTANCING,
                params = arrayOf(JsonUtil.toJson(pipelineNames))
            )
        }
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            templateInstanceBaseDao.createTemplateInstanceBase(
                dslContext = context,
                baseId = baseId,
                templateId = templateId,
                templateVersion = version.toString(),
                useTemplateSettingsFlag = request.useTemplateSettings,
                projectId = projectId,
                totalItemNum = instances.size,
                status = TemplateInstanceStatus.INIT.name,
                userId = userId,
                pac = request.enablePac,
                targetAction = request.targetAction?.name,
                type = TemplateInstanceType.CREATE.name,
                repoHashId = request.repoHashId,
                targetBranch = request.targetBranch,
                description = request.description,
                templateRefType = request.templateRefType,
                templateRef = request.templateRef,
                gray = true
            )
            templateInstanceItemDao.createTemplateInstanceItemsV2(
                dslContext = context,
                projectId = projectId,
                baseId = baseId,
                instances = instances,
                status = TemplateInstanceStatus.INIT.name,
                userId = userId
            )
        }
        pipelineEventDispatcher.dispatch(
            PipelineTemplateInstanceEvent(
                projectId = projectId,
                source = "PIPELINE_TEMPLATE_INSTANCE_CREATE",
                pipelineId = "",
                userId = userId,
                templateId = templateId,
                baseId = baseId,
                templateInstanceType = TemplateInstanceType.CREATE
            )
        )
        return baseId
    }

    /*异步更新模板实例*/
    fun asyncUpdateTemplateInstances(
        projectId: String,
        userId: String,
        templateId: String,
        version: Long,
        request: PipelineTemplateInstancesRequest
    ): String {
        logger.info("asyncUpdateTemplateInstances [$projectId|$userId|$templateId|$version")
        val permission = AuthPermission.EDIT
        val canEditMap = pipelinePermissionService.getResourceByPermission(
            userId = userId,
            projectId = projectId,
            permission = permission
        )
        val notEditPermissions = request.instanceReleaseInfos.filter {
            !canEditMap.contains(it.pipelineId)
        }.map { it.pipelineId }
        if (notEditPermissions.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                params = arrayOf(
                    userId,
                    projectId,
                    permission.getI18n(I18nUtil.getLanguage(userId)),
                    notEditPermissions.joinToString(",")
                )
            )
        }
        val templateResource = pipelineTemplateResourceService.get(
            projectId = projectId,
            templateId = templateId,
            version = version
        )
        val instances = request.instanceReleaseInfos
        val baseId = UUIDUtil.generate()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            templateInstanceBaseDao.createTemplateInstanceBase(
                dslContext = context,
                baseId = baseId,
                templateId = templateId,
                templateVersion = templateResource.version.toString(),
                useTemplateSettingsFlag = request.useTemplateSettings,
                projectId = projectId,
                totalItemNum = instances.size,
                status = TemplateInstanceStatus.INIT.name,
                userId = userId,
                pac = request.enablePac,
                targetAction = request.targetAction?.name,
                type = TemplateInstanceType.UPDATE.name,
                description = request.description,
                repoHashId = request.repoHashId,
                targetBranch = request.targetBranch,
                templateRefType = request.templateRefType,
                templateRef = request.templateRef,
                gray = true
            )
            templateInstanceItemDao.createTemplateInstanceItemsV2(
                dslContext = context,
                projectId = projectId,
                baseId = baseId,
                instances = instances,
                status = TemplateInstanceStatus.INIT.name,
                userId = userId
            )
            pipelineTemplateRelatedService.updateStatus(
                transactionContext = context,
                projectId = projectId,
                pipelineIds = instances.map { it.pipelineId },
                status = TemplatePipelineStatus.UPDATING
            )
        }
        pipelineEventDispatcher.dispatch(
            PipelineTemplateInstanceEvent(
                projectId = projectId,
                source = "PIPELINE_TEMPLATE_INSTANCE_UPDATE",
                pipelineId = "",
                userId = userId,
                templateId = templateId,
                baseId = baseId,
                templateInstanceType = TemplateInstanceType.UPDATE
            )
        )
        return baseId
    }

    fun translateInstanceException(
        userId: String,
        projectId: String,
        pipelineId: String,
        exception: Throwable
    ): PipelineCheckFailedReason {
        logger.warn("Failed to instance template|$userId|$projectId|$pipelineId|${exception.message}")
        return when (exception) {
            is DuplicateKeyException -> {
                PipelineCheckFailedMsg("duplicate!")
            }

            is ErrorCodeException -> {
                val message = I18nUtil.generateResponseDataObject(
                    messageCode = exception.errorCode,
                    params = exception.params,
                    data = null,
                    defaultMessage = exception.defaultMessage
                ).message ?: exception.defaultMessage ?: "unknown!"
                // ERROR_PIPELINE_ELEMENT_CHECK_FAILED输出的是一个json,需要格式化输出
                if (exception.errorCode == ERROR_PIPELINE_ELEMENT_CHECK_FAILED) {
                    JsonUtil.to(message, PipelineCheckFailedErrors::class.java)
                } else {
                    PipelineCheckFailedMsg(message)
                }
            }

            else -> {
                PipelineCheckFailedMsg(exception.message ?: "template instance fail")
            }
        }
    }

    fun list(
        userId: String,
        projectId: String,
        templateId: String,
        pipelineName: String?,
        updater: String?,
        status: TemplatePipelineStatus?,
        templateVersion: Long?,
        repoHashId: String?,
        page: Int,
        pageSize: Int
    ): SQLPage<PipelineTemplateRelatedResp> {
        val (offset, limit) = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val pipelineIdsFilterByRepo = repoHashId?.let {
            pipelineYamlService.getAllYamlPipeline(projectId, repoHashId).map { it.pipelineId }
        } ?: emptyList()

        val canEditMap = pipelinePermissionService.getResourceByPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.EDIT
        )
        val templatePipelineRecords = pipelineTemplateRelatedService.listSimple(
            projectId = projectId,
            templateId = templateId,
            pipelineName = pipelineName,
            updater = updater,
            templateVersion = templateVersion,
            status = status,
            pipelineIds = pipelineIdsFilterByRepo,
            instanceTypeEnum = PipelineInstanceTypeEnum.CONSTRAINT,
            limit = limit,
            offset = offset
        )
        val count = pipelineTemplateRelatedService.countSimple(
            projectId = projectId,
            templateId = templateId,
            pipelineName = pipelineName,
            updater = updater,
            templateVersion = templateVersion,
            status = status,
            pipelineIds = pipelineIdsFilterByRepo,
            instanceTypeEnum = PipelineInstanceTypeEnum.CONSTRAINT
        )

        val pipelineIds = templatePipelineRecords.map { it.pipelineId }
        // 获取流水线版本名称
        val pipelineVersionNameMap = pipelineRepositoryService.getLatestVersionNames(
            projectId = projectId,
            pipelineIds = pipelineIds
        )
        // 获取流水线yaml信息
        val pipelineYamlInfoList = pipelineYamlService.listByPipelineIds(
            projectId = projectId,
            pipelineIds = pipelineIds
        )
        val yamlPipelineMap = pipelineYamlInfoList.associateBy { it.pipelineId }
        // 获取代码库别名
        val repoHashIds = pipelineYamlInfoList.map { it.repoHashId }.toSet()
        val repoAliasNameMap = repoHashIds.takeIf { it.isNotEmpty() }?.let {
            client.get(ServiceRepositoryResource::class)
                .listRepoByIds(repoHashIds).data?.associateBy({ it.repoHashId!! }, { it.aliasName })
        } ?: emptyMap()

        val results = templatePipelineRecords.map { record ->
            val yamlPipelineInfo = yamlPipelineMap[record.pipelineId]
            val finalStatus = when {
                record.status == null -> TemplatePipelineStatus.UPDATED
                record.status == TemplatePipelineStatus.UPDATED &&
                    record.version != record.releasedVersion -> TemplatePipelineStatus.PENDING_UPDATE

                else -> record.status
            }
            PipelineTemplateRelatedResp(
                templateId = record.templateId,
                pipelineId = record.pipelineId,
                pipelineName = record.pipelineName,
                pipelineVersion = record.pipelineVersion,
                pipelineVersionName = pipelineVersionNameMap[record.pipelineId] ?: "init",
                fromTemplateVersion = record.version,
                fromTemplateVersionName = record.versionName,
                canEdit = canEditMap.contains(record.pipelineId),
                status = finalStatus,
                enabledPac = yamlPipelineInfo != null,
                repoHashId = yamlPipelineInfo?.repoHashId,
                repoAliasName = yamlPipelineInfo?.repoHashId?.let { repoAliasNameMap[it] },
                pullRequestUrl = record.pullRequestUrl,
                instanceErrorInfo = record.instanceErrorInfo,
                updater = record.updater,
                updateTime = record.updatedTime
            )
        }
        return SQLPage(
            count = count.toLong(),
            records = results
        )
    }

    fun listTemplateInstancesParams(
        userId: String,
        projectId: String,
        templateId: String,
        pipelineIds: Set<String>
    ): Map<String, TemplateInstanceParams> {
        pipelineTemplateInfoService.get(
            projectId = projectId,
            templateId = templateId
        )
        val pipelineId2Name = pipelineRepositoryService.listPipelineNameByIds(
            projectId = projectId,
            pipelineIds = pipelineIds
        )
        val pipelineId2Model = pipelineResourceDao.listLatestModelResource(
            dslContext = dslContext,
            pipelineIds = pipelineIds,
            projectId = projectId
        )?.associate { resource ->
            val model: Model = objectMapper.readValue(resource.value3())
            resource.value1() to model
        } ?: emptyMap()
        val pipelineCurrentBuildNos = pipelineBuildSummaryDao.getSummaries(
            dslContext = dslContext,
            projectId = projectId,
            pipelineIds = pipelineIds
        ).associate { it.pipelineId to it.buildNo }
        // 获取流水线yaml信息
        val pipelineYamlInfoList = pipelineYamlService.listByPipelineIds(
            projectId = projectId,
            pipelineIds = pipelineIds.toList()
        )
        val yamlPipelineMap = pipelineYamlInfoList.associateBy { it.pipelineId }

        return try {
            pipelineId2Model.map {
                val pipelineId = it.key
                val instanceModel = it.value
                val instanceTriggerContainer = instanceModel.getTriggerContainer()
                val overrideTemplateField = instanceModel.overrideTemplateField ?: run {
                    TemplateInstanceField.initFromTrigger(triggerContainer = instanceTriggerContainer)
                }
                val instanceParams = paramService.filterParams(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    params = instanceTriggerContainer.params
                )
                val instanceBuildNoObj = instanceTriggerContainer.buildNo?.copy(
                    currentBuildNo = pipelineCurrentBuildNos[pipelineId]
                )
                pipelineId to TemplateInstanceParams(
                    pipelineId = pipelineId,
                    pipelineName = pipelineId2Name[pipelineId] ?: "",
                    buildNo = instanceBuildNoObj,
                    param = instanceParams,
                    repoHashId = yamlPipelineMap[pipelineId]?.repoHashId,
                    filePath = yamlPipelineMap[pipelineId]?.filePath,
                    triggerElements = instanceModel.getTriggerContainer().elements,
                    overrideTemplateField = overrideTemplateField
                )
            }.toMap()
        } catch (ignored: Throwable) {
            logger.warn("Fail to list pipeline params - [$projectId|$userId|$templateId]", ignored)
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.FAIL_TO_LIST_TEMPLATE_PARAMS
            )
        }
    }

    fun getTemplateInstanceParamsById(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long
    ): TemplateInstanceParams {
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW,
            templateId = templateId
        )
        val templateResource = pipelineTemplateResourceService.get(
            projectId = projectId,
            templateId = templateId,
            version = version
        )
        val templateModel = templateResource.model
        if (templateModel !is Model) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_TYPE_MODEL_TYPE_NOT_MATCH
            )
        }
        val triggerContainer = templateModel.getTriggerContainer()
        val instanceParams = paramService.filterParams(
            userId = userId,
            projectId = projectId,
            pipelineId = null,
            params = triggerContainer.params
        )
        val overrideTemplateField = TemplateInstanceField.initFromTrigger(
            triggerContainer = triggerContainer
        )
        return TemplateInstanceParams(
            pipelineId = "",
            pipelineName = "",
            buildNo = triggerContainer.buildNo,
            param = instanceParams,
            triggerElements = triggerContainer.elements,
            overrideTemplateField = overrideTemplateField
        )
    }

    fun getTemplateInstanceParamsByRef(
        userId: String,
        projectId: String,
        templateId: String,
        ref: String
    ): TemplateInstanceParams {
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW,
            templateId = templateId
        )
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
        return getTemplateInstanceParamsById(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = pipelineYamlVersion.version.toLong()
        )
    }

    fun preFetchTemplateInstance(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        request: PipelineTemplateInstancesRequest
    ): List<PrefetchReleaseResult> {
        return pipelineVersionGenerator.batchPreFetchInstanceVersion(
            projectId = projectId,
            templateId = templateId,
            version = version,
            request = request
        )
    }

    fun getTemplateInstanceTaskResult(
        projectId: String,
        baseId: String
    ): PipelineTemplateInstancesTaskResult {
        val instanceBase = templateInstanceBaseDao.getTemplateInstanceBase(
            dslContext = dslContext,
            projectId = projectId,
            baseId = baseId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_TEMPLATE_INSTANCE_NOT_EXISTS,
            params = arrayOf(baseId)
        )
        val failedTemplateInstances = templateInstanceItemDao.listTemplateInstanceItemByBaseIds(
            dslContext = dslContext,
            projectId = projectId,
            baseIds = listOf(baseId),
            statusList = listOf(TemplateInstanceStatus.FAILED.name),
            page = 1,
            pageSize = PageUtil.MAX_PAGE_SIZE
        )
        val errorMessages = failedTemplateInstances.associate {
            it.pipelineName to (it.errorMessage ?: "")
        }
        return PipelineTemplateInstancesTaskResult(
            baseId = baseId,
            projectId = projectId,
            templateId = instanceBase.templateId,
            templateVersion = instanceBase.templateVersion,
            status = instanceBase.status,
            totalItemNum = instanceBase.totalItemNum,
            successItemNum = instanceBase.successItemNum,
            failItemNum = instanceBase.failItemNum,
            errorMessages = errorMessages,
            pullRequestUrl = instanceBase.pullRequestUrl
        )
    }

    fun listTemplateInstanceTask(
        projectId: String,
        templateId: String,
        statusList: List<String>?,
    ): List<PipelineTemplateInstanceBase> {
        return templateInstanceBaseDao.list(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId,
            statusList = statusList
        )
    }

    fun retryTemplateInstanceTask(
        userId: String,
        projectId: String,
        baseId: String
    ): String {
        val taskDetail = getTemplateInstanceTaskDetail(
            projectId = projectId,
            baseId = baseId,
            statusList = listOf(TemplateInstanceStatus.FAILED.name)
        )
        return with(taskDetail) {
            if (instanceType == TemplateInstanceType.CREATE) {
                asyncCreateTemplateInstances(
                    userId = userId,
                    projectId = projectId,
                    templateId = templateId,
                    version = version,
                    request = request
                )
            } else {
                asyncUpdateTemplateInstances(
                    userId = userId,
                    projectId = projectId,
                    templateId = templateId,
                    version = version,
                    request = request
                )
            }
        }
    }

    fun getTemplateInstanceTaskDetail(
        projectId: String,
        baseId: String,
        statusList: List<String>?
    ): PipelineTemplateInstancesTaskDetail {
        val instanceBase = templateInstanceBaseDao.getTemplateInstanceBase(
            dslContext = dslContext,
            projectId = projectId,
            baseId = baseId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_TEMPLATE_INSTANCE_NOT_EXISTS,
            params = arrayOf(baseId)
        )
        val templateInstanceItems = templateInstanceItemDao.listTemplateInstanceItemByBaseIds(
            dslContext = dslContext,
            projectId = projectId,
            baseIds = listOf(baseId),
            statusList = statusList,
            page = 1,
            pageSize = PageUtil.MAX_PAGE_SIZE
        )
        val instanceReleaseInfos = templateInstanceItems.map {
            PipelineTemplateInstanceReleaseInfo(
                pipelineId = it.pipelineId,
                pipelineName = it.pipelineName,
                buildNo = it.buildNo,
                param = it.params,
                triggerConfigs = it.triggerConfigs,
                filePath = it.filePath,
                overrideTemplateField = it.overrideTemplateField
            )
        }
        val request = with(instanceBase) {
            PipelineTemplateInstancesRequest(
                templateRefType = templateRefType,
                templateRef = templateRef,
                useTemplateSettings = useTemplateSetting,
                enablePac = pac,
                description = description,
                targetAction = targetAction,
                repoHashId = repoHashId,
                targetBranch = targetBranch,
                instanceReleaseInfos = instanceReleaseInfos,
            )
        }
        return PipelineTemplateInstancesTaskDetail(
            projectId = instanceBase.projectId,
            templateId = instanceBase.templateId,
            instanceType = instanceBase.type,
            version = instanceBase.templateVersion,
            request = request
        )
    }

    /**
     * 比较模板YAML和流水线实例YAML之间的差异
     * 支持指定版本和是否使用模板设置
     */
    fun compareTemplateAndPipelineYaml(
        userId: String,
        projectId: String,
        templateId: String,
        templateVersion: Long,
        pipelineId: String,
        pipelineVersion: Int,
        useTemplateSettings: Boolean
    ): PipelineTemplateInstanceCompareResponse {
        // 检查模板查看权限
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW,
            templateId = templateId
        )
        val pipelineName = pipelineInfoService.getPipelineInfo(projectId, pipelineId)?.pipelineName
        val templateInfo = pipelineTemplateInfoService.get(projectId, templateId)
        val pipelineTemplateRelated = pipelineTemplateRelatedService.get(projectId, pipelineId)
        // 获取指定模板版本的资源
        val templateResource = pipelineTemplateResourceService.get(
            projectId = projectId,
            templateId = templateId,
            version = templateVersion
        )
        // 获取指定流水线版本的完整model和setting
        val pipelineModelAndSetting = pipelineVersionFacadeService.getVersion(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = pipelineVersion
        ).modelAndSetting

        // 流水线实例与模板yaml对比时，不需要extends部分
        with(pipelineModelAndSetting.model) {
            template = null
            overrideTemplateField = null
        }

        // 使用修改后的modelAndSetting转换为YAML
        val pipelineYaml = transferService.transfer(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            actionType = TransferActionType.FULL_MODEL2YAML,
            data = TransferBody(modelAndSetting = pipelineModelAndSetting)
        ).yamlWithVersion?.yamlStr ?: ""

        // 获取模板YAML
        val templateYaml = if (useTemplateSettings) {
            // 如果使用模板设置，直接返回模板的完整YAML
            templateResource.yaml ?: pipelineTemplateGenerator.transfer(
                userId = userId,
                projectId = projectId,
                storageType = PipelineStorageType.MODEL,
                templateType = templateResource.type,
                templateModel = templateResource.model,
                templateSetting = pipelineTemplateSettingService.get(
                    projectId = projectId,
                    templateId = templateId,
                    settingVersion = templateResource.settingVersion
                ),
                params = templateResource.params,
                yaml = null
            ).yamlWithVersion?.yamlStr ?: ""
        } else {
            // 如果不使用模板设置，使用默认模板的setting拼凑模板的Model，然后转换
            val defaultSetting = pipelineTemplateGenerator.getDefaultSetting(
                type = templateResource.type,
                projectId = projectId,
                templateId = templateId,
                templateName = templateInfo.name,
                desc = templateInfo.desc,
                creator = templateResource.creator
            )

            pipelineTemplateGenerator.transfer(
                userId = userId,
                projectId = projectId,
                storageType = PipelineStorageType.MODEL,
                templateType = templateResource.type,
                templateModel = templateResource.model,
                templateSetting = defaultSetting,
                params = templateResource.params,
                yaml = null
            ).yamlWithVersion?.yamlStr ?: ""
        }

        return PipelineTemplateInstanceCompareResponse(
            baseVersionYaml = pipelineYaml,
            comparedVersionYaml = templateYaml,
            instanceName = pipelineName ?: "",
            templateVersionName = pipelineTemplateRelated?.versionName ?: ""
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateInstanceService::class.java)
    }
}
