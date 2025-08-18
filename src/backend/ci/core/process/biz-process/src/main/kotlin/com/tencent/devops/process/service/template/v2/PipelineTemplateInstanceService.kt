package com.tencent.devops.process.service.template.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceField
import com.tencent.devops.common.pipeline.pojo.element.atom.PipelineCheckFailedErrors
import com.tencent.devops.common.pipeline.pojo.element.atom.PipelineCheckFailedMsg
import com.tencent.devops.common.pipeline.pojo.element.atom.PipelineCheckFailedReason
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_ELEMENT_CHECK_FAILED
import com.tencent.devops.process.engine.cfg.PipelineIdGenerator
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import com.tencent.devops.process.engine.dao.template.TemplateInstanceBaseDao
import com.tencent.devops.process.engine.dao.template.TemplateInstanceItemDao
import com.tencent.devops.process.engine.pojo.event.PipelineTemplateInstanceEvent
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.permission.PipelinePermissionService
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
import com.tencent.devops.process.service.pipeline.version.PipelineVersionGenerator
import com.tencent.devops.process.service.pipeline.version.PipelineVersionManager
import com.tencent.devops.process.yaml.PipelineYamlService
import com.tencent.devops.repository.api.ServiceRepositoryResource
import jakarta.ws.rs.core.Response
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
    private val client: Client
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
        val instances = request.instanceReleaseInfos
        val successPipelines = mutableListOf<String>()
        val failurePipelines = mutableListOf<String>()
        val successPipelineIds = mutableListOf<String>()
        val failureMessages = mutableMapOf<String, String>()

        instances.forEach { instance ->
            try {
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
            PipelineTemplateRelatedResp(
                templateId = record.templateId,
                pipelineId = record.pipelineId,
                pipelineName = record.pipelineName,
                pipelineVersion = record.pipelineVersion,
                pipelineVersionName = pipelineVersionNameMap[record.pipelineId] ?: "init",
                fromTemplateVersion = record.version,
                fromTemplateVersionName = record.versionName,
                canEdit = canEditMap.contains(record.pipelineId),
                status = record.status,
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

    fun compare(
        userId: String,
        projectId: String,
        pipelineId: String,
        templateId: String,
        compareVersion: Long?
    ): PipelineTemplateInstanceCompareResponse {
        val comparedVersionResource = pipelineTemplateResourceService.get(
            projectId = projectId,
            templateId = templateId,
            version = compareVersion ?: pipelineTemplateInfoService.get(projectId, templateId).releasedVersion!!
        )
        val latestPipelineVersion = pipelineResourceDao.getReleaseVersionResource(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )?.version ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
            params = arrayOf(pipelineId)
        )
        val pipelineYaml = pipelineVersionFacadeService.getVersion(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = latestPipelineVersion
        ).yamlPreview?.yaml ?: ""

        return PipelineTemplateInstanceCompareResponse(
            baseVersionYaml = pipelineYaml,
            comparedVersionYaml = comparedVersionResource.yaml ?: ""
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
        val templateInstanceItems = templateInstanceItemDao.listTemplateInstanceItemByBaseIds(
            dslContext = dslContext,
            projectId = projectId,
            baseIds = listOf(baseId),
            statusList = listOf(TemplateInstanceStatus.FAILED.name),
            page = 1,
            pageSize = PageUtil.MAX_PAGE_SIZE
        )
        val pipelineId2Name = pipelineRepositoryService.listPipelineNameByIds(
            projectId = projectId,
            pipelineIds = templateInstanceItems.map { it.pipelineId }.toSet()
        )
        val errorMessages = templateInstanceItems.associateBy(
            { pipelineId2Name[it.pipelineId] ?: "" },
            { it.errorMessage ?: "" }
        )
        return PipelineTemplateInstancesTaskResult(
            baseId = baseId,
            projectId = projectId,
            templateId = instanceBase.templateId,
            templateVersion = instanceBase.templateVersion,
            status = instanceBase.status,
            totalItemNum = instanceBase.totalItemNum,
            successItemNum = instanceBase.successItemNum,
            failItemNum = instanceBase.failItemNum,
            errorMessages = errorMessages
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
                filePath = it.filePath
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

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateInstanceService::class.java)
    }
}
