package com.tencent.devops.process.service.task.copy

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.PipelineTemplateConstant
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.enums.OperationLogType
import com.tencent.devops.process.permission.template.PipelineTemplatePermissionService
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResource
import com.tencent.devops.process.pojo.pipeline.task.TemplateVersionMapping
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoUpdateInfo
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoV2
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceCommonCondition
import com.tencent.devops.process.service.PipelineOperationLogService
import com.tencent.devops.process.service.template.v2.PipelineTemplateGenerator
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.process.service.template.v2.PipelineTemplateModelLock
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.service.template.v2.PipelineTemplateSettingService
import com.tencent.devops.store.api.template.ServiceTemplateResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线复制模板创建服务
 */
@Service
class PipelineCopyTemplateCreateService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val pipelineDependencyReplaceService: PipelineDependencyReplaceService,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineTemplateSettingService: PipelineTemplateSettingService,
    private val pipelineTemplateGenerator: PipelineTemplateGenerator,
    private val pipelineTemplatePermissionService: PipelineTemplatePermissionService,
    private val redisOperation: RedisOperation,
    private val operationLogService: PipelineOperationLogService
) {
    fun createTemplateVersion(
        userId: String,
        sourceProjectId: String,
        sourceTemplateId: String,
        sourceTemplateVersion: Long,
        targetProjectId: String,
        targetTemplateId: String,
        resourceMap: Map<String, PipelineCopyTaskResource>
    ): TemplateVersionMapping {
        val sourceTemplateInfo = pipelineTemplateInfoService.getOrNull(
            projectId = sourceProjectId,
            templateId = sourceTemplateId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_SOURCE_RESOURCE_NOT_EXISTS,
            params = arrayOf(sourceProjectId, "$sourceTemplateId@$sourceTemplateVersion")
        )
        val sourceTemplateResource = pipelineTemplateResourceService.getOrNull(
            projectId = sourceProjectId,
            templateId = sourceTemplateId,
            version = sourceTemplateVersion
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_SOURCE_RESOURCE_NOT_EXISTS,
            params = arrayOf(sourceProjectId, "$sourceTemplateId@$sourceTemplateVersion")
        )
        val sourceVersionName = sourceTemplateResource.versionName!!
        findExistingTargetReleasedVersion(
            targetProjectId = targetProjectId,
            targetTemplateId = targetTemplateId,
            sourceVersionName = sourceVersionName,
            sourceVersionNum = sourceTemplateResource.versionNum
        )?.let { existing ->
            return TemplateVersionMapping(
                sourceVersion = sourceTemplateVersion,
                sourceVersionName = sourceVersionName,
                targetVersion = existing.version,
                targetVersionName = existing.versionName ?: sourceVersionName
            )
        }
        val modelAndSetting = pipelineDependencyReplaceService.replaceTemplateResourceDependency(
            userId = userId,
            sourceProjectId = sourceProjectId,
            sourceTemplateId = sourceTemplateId,
            sourceTemplateVersion = sourceTemplateVersion,
            sourceTemplateResource = sourceTemplateResource,
            targetProjectId = targetProjectId,
            targetTemplateId = targetTemplateId,
            targetTemplateName = sourceTemplateInfo.name,
            resourceMap = resourceMap
        )
        client.get(ServiceTemplateResource::class).validateModelComponentVisibleDept(
            userId = userId,
            model = modelAndSetting.model,
            projectCode = targetProjectId
        )
        val targetVersion = pipelineTemplateGenerator.generateTemplateVersion()
        val targetTemplateSetting = modelAndSetting.setting.copy(
            version = sourceTemplateResource.settingVersion
        )
        val targetTemplateResource = buildTargetTemplateResource(
            userId = userId,
            sourceTemplateResource = sourceTemplateResource,
            targetProjectId = targetProjectId,
            targetTemplateId = targetTemplateId,
            targetVersion = targetVersion,
            model = modelAndSetting.model,
            targetTemplateSetting = targetTemplateSetting
        )
        persistTargetTemplateVersion(
            userId = userId,
            sourceTemplateInfo = sourceTemplateInfo,
            sourceTemplateResource = sourceTemplateResource,
            targetProjectId = targetProjectId,
            targetTemplateId = targetTemplateId,
            targetTemplateResource = targetTemplateResource,
            targetTemplateSetting = targetTemplateSetting
        )
        return TemplateVersionMapping(
            sourceVersion = sourceTemplateVersion,
            sourceVersionName = sourceVersionName,
            targetVersion = targetVersion,
            targetVersionName = sourceVersionName
        )
    }

    private fun findExistingTargetReleasedVersion(
        targetProjectId: String,
        targetTemplateId: String,
        sourceVersionName: String,
        sourceVersionNum: Int?
    ): PipelineTemplateResource? {
        pipelineTemplateResourceService.getOrNull(
            commonCondition = PipelineTemplateResourceCommonCondition(
                projectId = targetProjectId,
                templateId = targetTemplateId,
                versionName = sourceVersionName,
                status = VersionStatus.RELEASED
            )
        )?.let { return it }
        sourceVersionNum?.let { versionNum ->
            return pipelineTemplateResourceService.getOrNull(
                commonCondition = PipelineTemplateResourceCommonCondition(
                    projectId = targetProjectId,
                    templateId = targetTemplateId,
                    versionNum = versionNum,
                    status = VersionStatus.RELEASED
                )
            )
        }
        return null
    }

    private fun buildTargetTemplateResource(
        userId: String,
        sourceTemplateResource: PipelineTemplateResource,
        targetProjectId: String,
        targetTemplateId: String,
        targetVersion: Long,
        model: Model,
        targetTemplateSetting: PipelineSetting
    ): PipelineTemplateResource {
        model.latestVersion = targetVersion.toInt()
        val targetParams = model.getTriggerContainer().params
        val transferResult = pipelineTemplateGenerator.transfer(
            userId = userId,
            projectId = targetProjectId,
            storageType = PipelineStorageType.MODEL,
            templateType = sourceTemplateResource.type,
            templateModel = model,
            templateSetting = targetTemplateSetting,
            params = targetParams,
            yaml = null,
            fallbackOnError = true
        )
        return PipelineTemplateResource(
            projectId = targetProjectId,
            templateId = targetTemplateId,
            type = sourceTemplateResource.type,
            version = targetVersion,
            settingVersion = sourceTemplateResource.settingVersion,
            number = sourceTemplateResource.number,
            versionName = sourceTemplateResource.versionName,
            versionNum = sourceTemplateResource.versionNum,
            settingVersionNum = sourceTemplateResource.settingVersionNum,
            pipelineVersion = sourceTemplateResource.pipelineVersion,
            triggerVersion = sourceTemplateResource.triggerVersion,
            srcTemplateProjectId = sourceTemplateResource.srcTemplateProjectId,
            srcTemplateId = sourceTemplateResource.srcTemplateId,
            srcTemplateVersion = sourceTemplateResource.srcTemplateVersion,
            baseVersion = null,
            baseVersionName = null,
            params = targetParams,
            model = model,
            yaml = transferResult.yamlWithVersion?.yamlStr ?: "",
            status = VersionStatus.RELEASED,
            description = sourceTemplateResource.description,
            sortWeight = PipelineTemplateConstant.OTHER_STATUS_VERSION_SORT_WIGHT,
            creator = sourceTemplateResource.creator,
            updater = sourceTemplateResource.updater,
            releaseTime = sourceTemplateResource.releaseTime,
            createdTime = sourceTemplateResource.createdTime,
            updateTime = sourceTemplateResource.updateTime
        )
    }

    private fun persistTargetTemplateVersion(
        userId: String,
        sourceTemplateInfo: PipelineTemplateInfoV2,
        sourceTemplateResource: PipelineTemplateResource,
        targetProjectId: String,
        targetTemplateId: String,
        targetTemplateResource: PipelineTemplateResource,
        targetTemplateSetting: PipelineSetting
    ) {
        val lock = PipelineTemplateModelLock(
            redisOperation = redisOperation,
            templateId = targetTemplateId
        )
        try {
            lock.lock()
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                val existingTemplateInfo = pipelineTemplateInfoService.getOrNull(
                    projectId = targetProjectId,
                    templateId = targetTemplateId
                )
                createOrUpdateTargetTemplateInfoAndPermission(
                    transactionContext = transactionContext,
                    userId = userId,
                    sourceTemplateInfo = sourceTemplateInfo,
                    existingTemplateInfo = existingTemplateInfo,
                    targetProjectId = targetProjectId,
                    targetTemplateId = targetTemplateId,
                    targetTemplateResource = targetTemplateResource,
                    targetTemplateSetting = targetTemplateSetting
                )
                pipelineTemplateResourceService.create(
                    transactionContext = transactionContext,
                    pipelineTemplateResource = targetTemplateResource
                )
                pipelineTemplateSettingService.createOrUpdate(
                    transactionContext = transactionContext,
                    pipelineTemplateSetting = targetTemplateSetting
                )
            }
            saveOperationLog(
                userId = userId,
                sourceProjectId = sourceTemplateResource.projectId,
                sourceTemplateId = sourceTemplateResource.templateId,
                sourceVersionName = sourceTemplateResource.versionName!!,
                targetProjectId = targetProjectId,
                targetTemplateId = targetTemplateId,
                targetVersion = targetTemplateResource.version
            )
        } finally {
            lock.unlock()
        }
    }

    private fun saveOperationLog(
        userId: String,
        sourceProjectId: String,
        sourceTemplateId: String,
        sourceVersionName: String,
        targetProjectId: String,
        targetTemplateId: String,
        targetVersion: Long
    ) {
        operationLogService.addOperationLog(
            userId = userId,
            projectId = targetProjectId,
            pipelineId = targetTemplateId,
            version = targetVersion.toInt(),
            operationLogType = OperationLogType.CROSS_PROJECT_COPY_TEMPLATE_VERSION,
            params = "$sourceProjectId/$sourceTemplateId@$sourceVersionName",
            description = null
        )
    }

    private fun createOrUpdateTargetTemplateInfoAndPermission(
        transactionContext: DSLContext,
        userId: String,
        sourceTemplateInfo: PipelineTemplateInfoV2,
        existingTemplateInfo: PipelineTemplateInfoV2?,
        targetProjectId: String,
        targetTemplateId: String,
        targetTemplateResource: PipelineTemplateResource,
        targetTemplateSetting: PipelineSetting
    ) {
        if (existingTemplateInfo == null) {
            pipelineTemplateInfoService.createOrUpdate(
                transactionContext = transactionContext,
                pipelineTemplateInfo = PipelineTemplateInfoV2(
                    id = targetTemplateId,
                    projectId = targetProjectId,
                    name = sourceTemplateInfo.name,
                    desc = sourceTemplateInfo.desc,
                    mode = TemplateType.CUSTOMIZE,
                    category = sourceTemplateInfo.category,
                    type = sourceTemplateInfo.type,
                    logoUrl = sourceTemplateInfo.logoUrl,
                    enablePac = false,
                    releasedVersion = targetTemplateResource.version,
                    releasedVersionName = targetTemplateResource.versionName,
                    releasedSettingVersion = targetTemplateResource.settingVersion,
                    latestVersionStatus = VersionStatus.RELEASED,
                    creator = sourceTemplateInfo.creator,
                    updater = sourceTemplateInfo.updater,
                    createdTime = sourceTemplateInfo.createdTime,
                    updateTime = sourceTemplateInfo.updateTime
                )
            )
            pipelineTemplatePermissionService.createResource(
                userId = userId,
                projectId = targetProjectId,
                templateId = targetTemplateId,
                templateName = sourceTemplateInfo.name
            )
        } else if (shouldUpdateReleasedTemplateInfo(
                targetProjectId = targetProjectId,
                targetTemplateId = targetTemplateId,
                existingTemplateInfo = existingTemplateInfo,
                newVersionNum = targetTemplateResource.versionNum!!
            )
        ) {
            pipelineTemplateInfoService.update(
                transactionContext = transactionContext,
                record = PipelineTemplateInfoUpdateInfo(
                    name = targetTemplateSetting.pipelineName,
                    desc = targetTemplateSetting.desc,
                    releasedVersion = targetTemplateResource.version,
                    releasedVersionName = targetTemplateResource.versionName,
                    releasedSettingVersion = targetTemplateResource.settingVersion,
                    latestVersionStatus = VersionStatus.RELEASED,
                    updater = userId
                ),
                commonCondition = PipelineTemplateCommonCondition(
                    projectId = targetProjectId,
                    templateId = targetTemplateId
                )
            )
            pipelineTemplatePermissionService.modifyResource(
                userId = userId,
                projectId = targetProjectId,
                templateId = targetTemplateId,
                templateName = targetTemplateSetting.pipelineName
            )
        }
    }

    private fun shouldUpdateReleasedTemplateInfo(
        targetProjectId: String,
        targetTemplateId: String,
        existingTemplateInfo: PipelineTemplateInfoV2,
        newVersionNum: Int
    ): Boolean {
        if (existingTemplateInfo.releasedVersion == 0L) {
            return true
        }
        val releasedResource = pipelineTemplateResourceService.getOrNull(
            projectId = targetProjectId,
            templateId = targetTemplateId,
            version = existingTemplateInfo.releasedVersion
        )
        if (releasedResource == null || releasedResource.versionNum == null) {
            return true
        }
        return newVersionNum >= releasedResource.versionNum!!
    }
}
