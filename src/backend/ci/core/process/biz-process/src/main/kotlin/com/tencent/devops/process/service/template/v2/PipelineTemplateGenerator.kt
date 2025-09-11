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

package com.tencent.devops.process.service.template.v2

import com.tencent.devops.common.api.check.Preconditions
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.TemplateModelAndSetting
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSubscriptionType
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.common.pipeline.pojo.transfer.PreviewResponse
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType
import com.tencent.devops.common.pipeline.pojo.transfer.TransferBody
import com.tencent.devops.common.pipeline.pojo.transfer.TransferMark
import com.tencent.devops.common.pipeline.template.ITemplateModel
import com.tencent.devops.common.pipeline.template.JobTemplateModel
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.common.pipeline.template.StageTemplateModel
import com.tencent.devops.common.pipeline.template.StepTemplateModel
import com.tencent.devops.process.constant.PipelineTemplateConstant
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TEMPLATE_LATEST_VERSION_NOT_EXIST
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TEMPLATE_TYPE_INVALID
import com.tencent.devops.process.engine.service.PipelineInfoExtService
import com.tencent.devops.process.pojo.setting.PipelineSettingVersion
import com.tencent.devops.process.pojo.template.v2.PTemplateModelTransferResult
import com.tencent.devops.process.pojo.template.v2.PTemplateResourceOnlyVersion
import com.tencent.devops.process.pojo.template.v2.PTemplateResourceWithoutVersion
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.service.pipeline.PipelineTransferYamlService
import com.tencent.devops.process.service.pipeline.PipelineTransferYamlService.Companion.notice_key
import com.tencent.devops.process.service.pipeline.PipelineTransferYamlService.Companion.pipeline_key
import com.tencent.devops.process.service.pipeline.PipelineTransferYamlService.Companion.setting_key
import com.tencent.devops.process.service.pipeline.PipelineTransferYamlService.Companion.trigger_key
import com.tencent.devops.process.utils.PipelineVersionUtils
import com.tencent.devops.process.yaml.transfer.TransferMapper
import com.tencent.devops.process.yaml.utils.NotifyTemplateUtils
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import com.tencent.devops.repository.api.scm.ServiceScmRepositoryApiResource
import com.tencent.devops.scm.api.pojo.repository.git.GitScmServerRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 生成流水线模版模型
 */
@Service
class PipelineTemplateGenerator @Autowired constructor(
    private val client: Client,
    private val transferService: PipelineTransferYamlService,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val pipelineTemplateSettingService: PipelineTemplateSettingService,
    private val pipelineInfoExtService: PipelineInfoExtService
) {

    fun getDefaultTemplateModel(
        name: String,
        type: PipelineTemplateType,
        userId: String
    ): ITemplateModel {
        return when (type) {
            PipelineTemplateType.PIPELINE -> Model.defaultModel(name, userId)
            PipelineTemplateType.STAGE -> StageTemplateModel.defaultStageTemplate()
            PipelineTemplateType.JOB -> JobTemplateModel.defaultJobTemplate()
            PipelineTemplateType.STEP -> StepTemplateModel.defaultStepTemplate()
            else -> {
                throw ErrorCodeException(errorCode = ERROR_TEMPLATE_TYPE_INVALID)
            }
        }
    }

    fun getDefaultSetting(
        type: PipelineTemplateType,
        projectId: String,
        templateId: String,
        templateName: String,
        desc: String?,
        creator: String
    ): PipelineSetting {
        return if (type == PipelineTemplateType.PIPELINE) {
            val failNotifyTypes = pipelineInfoExtService.failNotifyChannel()
            val failType = failNotifyTypes.split(",").filter { i -> i.isNotBlank() }
                .map { type -> PipelineSubscriptionType.valueOf(type) }.toSet()
            val failSubscription = Subscription(
                types = failType,
                groups = emptySet(),
                users = "\${{ci.actor}}",
                content = NotifyTemplateUtils.getCommonShutdownFailureContent()
            )
            PipelineSetting.defaultSetting(
                projectId = projectId,
                pipelineId = templateId,
                pipelineName = templateName,
                desc = desc,
                failSubscription = failSubscription,
                creator = creator,
                updater = creator
            )
        } else {
            PipelineSetting(
                projectId = projectId,
                pipelineId = templateId,
                pipelineName = templateName,
                version = PipelineTemplateConstant.INIT_VERSION,
                desc = desc ?: "",
                pipelineAsCodeSettings = null,
                creator = creator,
                updater = creator
            )
        }
    }

    fun generateTemplateId() = UUIDUtil.generate()

    fun generateTemplateVersion() =
        client.get(ServiceAllocIdResource::class).generateSegmentId(TEMPLATE_BIZ_TAG_NAME).data!!

    /**
     * 获取默认版本
     */
    fun getDefaultVersion(
        versionStatus: VersionStatus,
        branchName: String? = null,
        versionName: String? = null
    ): PTemplateResourceOnlyVersion {
        return when (versionStatus) {
            VersionStatus.COMMITTING -> {
                PTemplateResourceOnlyVersion(
                    version = generateTemplateVersion(),
                    number = PipelineTemplateConstant.INIT_VERSION,
                    settingVersion = PipelineTemplateConstant.INIT_VERSION
                )
            }

            VersionStatus.BRANCH -> {
                PTemplateResourceOnlyVersion(
                    version = generateTemplateVersion(),
                    number = PipelineTemplateConstant.INIT_VERSION,
                    settingVersion = PipelineTemplateConstant.INIT_VERSION,
                    versionName = branchName!!
                )
            }

            else -> {
                val fixVersionName = versionName ?: PipelineVersionUtils.getVersionName(
                    versionNum = PipelineTemplateConstant.INIT_VERSION,
                    pipelineVersion = PipelineTemplateConstant.INIT_VERSION,
                    triggerVersion = PipelineTemplateConstant.INIT_VERSION,
                    settingVersion = PipelineTemplateConstant.INIT_VERSION
                )
                PTemplateResourceOnlyVersion(
                    version = generateTemplateVersion(),
                    number = PipelineTemplateConstant.INIT_VERSION,
                    versionName = fixVersionName,
                    versionNum = PipelineTemplateConstant.INIT_VERSION,
                    pipelineVersion = PipelineTemplateConstant.INIT_VERSION,
                    triggerVersion = PipelineTemplateConstant.INIT_VERSION,
                    settingVersion = PipelineTemplateConstant.INIT_VERSION,
                    settingVersionNum = PipelineTemplateConstant.INIT_VERSION
                )
            }
        }
    }

    /**
     * 生成草稿版本
     */
    fun generateDraftVersion(
        projectId: String,
        templateId: String
    ): PTemplateResourceOnlyVersion {
        val latestResource = pipelineTemplateResourceService.getLatestVersionResource(
            projectId = projectId,
            templateId = templateId
        ) ?: throw ErrorCodeException(errorCode = ERROR_TEMPLATE_LATEST_VERSION_NOT_EXIST)
        return PTemplateResourceOnlyVersion(
            version = generateTemplateVersion(),
            number = latestResource.number + 1,
            settingVersion = latestResource.settingVersion + 1,
            baseVersion = latestResource.version,
            baseVersionName = latestResource.versionName
        )
    }

    /**
     * 生成分支版本
     */
    fun generateBranchVersion(
        projectId: String,
        templateId: String,
        branchName: String
    ): PTemplateResourceOnlyVersion {
        val latestResource = pipelineTemplateResourceService.getLatestVersionResource(
            projectId = projectId,
            templateId = templateId
        ) ?: throw ErrorCodeException(errorCode = ERROR_TEMPLATE_LATEST_VERSION_NOT_EXIST)
        // 如果已经存在分支版本,则基准版本为分支版本
        val branchResource = pipelineTemplateResourceService.getLatestBranchResource(
            projectId = projectId,
            templateId = templateId,
            branchName = branchName
        )
        return PTemplateResourceOnlyVersion(
            version = generateTemplateVersion(),
            number = latestResource.number + 1,
            versionName = branchName,
            settingVersion = latestResource.settingVersion + 1,
            baseVersion = branchResource?.version ?: latestResource.version,
            baseVersionName = branchResource?.versionName ?: latestResource.versionName
        )
    }

    /**
     * 生成草稿发布版本
     */
    fun generateDraftReleaseVersion(
        projectId: String,
        templateId: String,
        draftResource: PipelineTemplateResource,
        draftSetting: PipelineSetting,
        customVersionName: String?,
        enablePac: Boolean,
        repoHashId: String?,
        targetAction: CodeTargetAction?,
        targetBranch: String? = null
    ): Pair<VersionStatus, PTemplateResourceOnlyVersion> {
        val newResource = PTemplateResourceWithoutVersion(draftResource)
        return if (enablePac) {
            generateDraftReleaseVersionWithPac(
                projectId = projectId,
                templateId = templateId,
                draftResource = draftResource,
                newResource = newResource,
                newSetting = draftSetting,
                repoHashId = repoHashId!!,
                targetAction = targetAction,
                targetBranch = targetBranch,
                customVersionName = customVersionName
            )
        } else {
            val resourceOnlyVersion = generateReleaseVersion(
                projectId = projectId,
                templateId = templateId,
                draftResource = draftResource,
                newResource = newResource,
                newSetting = draftSetting,
                customVersionName = customVersionName
            )
            Pair(VersionStatus.RELEASED, resourceOnlyVersion)
        }.also {
            val versionStatus = it.first
            if (versionStatus == VersionStatus.RELEASED) {
                val versionName = it.second.versionName!!
                pipelineTemplateResourceService.getLatestResource(
                    projectId = projectId,
                    templateId = templateId,
                    status = VersionStatus.RELEASED,
                    versionName = it.second.versionName!!
                )?.let {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_TEMPLATE_VERSION_NAME_DUPLICATION,
                        params = arrayOf(versionName)
                    )
                }
            }
        }
    }

    fun generateDraftReleaseVersionWithPac(
        projectId: String,
        templateId: String,
        draftResource: PipelineTemplateResource,
        newResource: PTemplateResourceWithoutVersion,
        newSetting: PipelineSetting,
        repoHashId: String,
        targetAction: CodeTargetAction?,
        targetBranch: String? = null,
        customVersionName: String? = null
    ): Pair<VersionStatus, PTemplateResourceOnlyVersion> {
        return when (targetAction) {
            CodeTargetAction.COMMIT_TO_MASTER -> {
                val resourceOnlyVersion = generateReleaseVersion(
                    projectId = projectId,
                    templateId = templateId,
                    draftResource = draftResource,
                    newResource = newResource,
                    newSetting = newSetting,
                    customVersionName = customVersionName
                )
                Pair(VersionStatus.RELEASED, resourceOnlyVersion)
            }

            CodeTargetAction.COMMIT_TO_SOURCE_BRANCH,
            CodeTargetAction.COMMIT_TO_SOURCE_BRANCH_AND_REQUEST_MERGE -> {
                val baseResource = draftResource.baseVersion?.let {
                    pipelineTemplateResourceService.get(
                        projectId = projectId, templateId = templateId, version = it
                    )
                } ?: throw ErrorCodeException(errorCode = ERROR_TEMPLATE_NOT_EXISTS)
                if (baseResource.status != VersionStatus.BRANCH) {
                    throw ErrorCodeException(errorCode = ERROR_TEMPLATE_NOT_EXISTS)
                }
                val resourceOnlyVersion =
                    PTemplateResourceOnlyVersion(draftResource).copy(versionName = baseResource.versionName)
                Pair(VersionStatus.BRANCH, resourceOnlyVersion)
            }

            CodeTargetAction.CHECKOUT_BRANCH_AND_REQUEST_MERGE -> {
                val versionName = "${PAC_TEMPLATE_BRANCH_PREFIX}$templateId-${draftResource.number}"
                val resourceOnlyVersion =
                    PTemplateResourceOnlyVersion(draftResource).copy(versionName = versionName)
                Pair(VersionStatus.BRANCH, resourceOnlyVersion)
            }

            CodeTargetAction.COMMIT_TO_BRANCH -> {
                if (targetBranch == null) {
                    throw IllegalArgumentException("targetBranch is null")
                }
                val serverRepository = client.get(ServiceScmRepositoryApiResource::class).getServerRepositoryById(
                    projectId = projectId,
                    repositoryType = RepositoryType.ID,
                    repoHashIdOrName = repoHashId
                ).data
                if (serverRepository !is GitScmServerRepository) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_NOT_SUPPORT_REPOSITORY_TYPE_ENABLE_PAC
                    )
                }
                // 如果选择的是默认分支,则应该发布正式版本
                if (targetBranch == serverRepository.defaultBranch) {
                    val resourceOnlyVersion = generateReleaseVersion(
                        projectId = projectId,
                        templateId = templateId,
                        draftResource = draftResource,
                        newResource = newResource,
                        newSetting = newSetting,
                        customVersionName = customVersionName
                    )
                    Pair(VersionStatus.RELEASED, resourceOnlyVersion)
                } else {
                    val resourceOnlyVersion =
                        PTemplateResourceOnlyVersion(draftResource).copy(versionName = targetBranch)
                    Pair(VersionStatus.BRANCH, resourceOnlyVersion)
                }
            }

            else -> {
                throw IllegalArgumentException("targetAction is illegal")
            }
        }
    }

    /**
     * 生成正式版本
     *
     * @param draftResource 草稿版本,草稿发布时需传入,直接生成正式版本为空
     */
    fun generateReleaseVersion(
        projectId: String,
        templateId: String,
        draftResource: PipelineTemplateResource? = null,
        newResource: PTemplateResourceWithoutVersion,
        newSetting: PipelineSetting,
        customVersionName: String? = null
    ): PTemplateResourceOnlyVersion {
        val latestResource = pipelineTemplateResourceService.getLatestVersionResource(
            projectId = projectId, templateId = templateId
        ) ?: throw ErrorCodeException(errorCode = ERROR_TEMPLATE_LATEST_VERSION_NOT_EXIST)
        val latestReleaseResource = pipelineTemplateResourceService.getLatestReleasedResource(
            projectId = projectId, templateId = templateId
        )
        val latestReleaseSetting = latestReleaseResource?.let {
            pipelineTemplateSettingService.get(
                projectId = projectId, templateId = templateId, settingVersion = it.settingVersion
            )
        }
        // 如果从草稿发布,number和setting不需要生成,直接使用草稿版本,否则使用最新版本+1
        val (version, number, settingVersion) = if (draftResource == null) {
            Triple(
                generateTemplateVersion(),
                latestResource.number + 1,
                latestResource.settingVersion + 1
            )
        } else {
            Triple(draftResource.version, draftResource.number, draftResource.settingVersion)
        }
        // 如果没有正式版本,说明是第一次生成正式版本
        return if (latestReleaseResource == null) {
            val versionNum = PipelineTemplateConstant.INIT_VERSION
            val pipelineVersion = PipelineTemplateConstant.INIT_VERSION
            val triggerVersion = PipelineTemplateConstant.INIT_VERSION
            val settingVersionNum = PipelineTemplateConstant.INIT_VERSION

            val versionName = PipelineVersionUtils.getVersionName(
                versionNum = versionNum,
                pipelineVersion = pipelineVersion,
                triggerVersion = triggerVersion,
                settingVersion = settingVersionNum
            )
            PTemplateResourceOnlyVersion(
                version = version,
                number = number,
                versionName = customVersionName?.takeIf { it.isNotBlank() } ?: versionName,
                versionNum = versionNum,
                pipelineVersion = pipelineVersion,
                triggerVersion = triggerVersion,
                settingVersion = settingVersion,
                settingVersionNum = settingVersionNum
            )
        } else {
            val versionNum = latestReleaseResource.versionNum?.let { it + 1 } ?: number
            val pipelineVersion = PipelineVersionUtils.getPipelineVersion(
                currVersion = latestReleaseResource.pipelineVersion!!,
                originTemplateModel = latestReleaseResource.model,
                newTemplateModel = newResource.model,
                originParams = latestReleaseResource.params,
                newParams = newResource.params
            )
            val triggerVersion = if (newResource.model is Model && latestReleaseResource.model is Model) {
                PipelineVersionUtils.getTriggerVersion(
                    currVersion = latestReleaseResource.triggerVersion!!,
                    originModel = latestReleaseResource.model as Model,
                    newModel = newResource.model as Model
                )
            } else {
                PipelineTemplateConstant.INIT_VERSION
            }
            val settingVersionNum = latestReleaseSetting?.let {
                PipelineVersionUtils.getSettingVersion(
                    currVersion = latestReleaseResource.settingVersionNum!!,
                    originSetting = PipelineSettingVersion.convertFromSetting(it),
                    newSetting = PipelineSettingVersion.convertFromSetting(newSetting)
                )
            } ?: PipelineTemplateConstant.INIT_VERSION

            val versionName = PipelineVersionUtils.getVersionName(
                versionNum = versionNum,
                pipelineVersion = pipelineVersion,
                triggerVersion = triggerVersion,
                settingVersion = settingVersionNum
            )
            PTemplateResourceOnlyVersion(
                version = version,
                number = number,
                versionName = customVersionName?.takeIf { it.isNotBlank() } ?: versionName,
                versionNum = versionNum,
                pipelineVersion = pipelineVersion,
                triggerVersion = triggerVersion,
                settingVersion = settingVersion,
                settingVersionNum = settingVersionNum
            )
        }
    }

    fun transfer(
        userId: String,
        projectId: String,
        storageType: PipelineStorageType,
        templateType: PipelineTemplateType?,
        templateModel: ITemplateModel?,
        templateSetting: PipelineSetting?,
        params: List<BuildFormProperty>?,
        yaml: String?
    ): PTemplateModelTransferResult {
        return if (storageType == PipelineStorageType.YAML) {
            val newYaml = Preconditions.checkNotNull(
                yaml,
                "yaml must not be null"
            )
            val transferResult = transferService.transfer(
                userId = userId,
                projectId = projectId,
                pipelineId = null,
                actionType = TransferActionType.TEMPLATE_YAML2MODEL_PIPELINE,
                data = TransferBody(
                    oldYaml = newYaml
                )
            )
            val newTemplateModel = Preconditions.checkNotNull(
                transferResult.templateModelAndSetting?.templateModel,
                "The transfer data is incorrect, so the modelAndYaml.templateModel.model must not be null"
            )
            val newTemplateSetting = Preconditions.checkNotNull(
                transferResult.templateModelAndSetting?.setting,
                "The transfer data is incorrect, " +
                    "so the modelAndYaml.templateModel.templateSetting must not be null"
            )
            PTemplateModelTransferResult(
                templateType = PipelineTemplateType.PIPELINE,
                templateModel = newTemplateModel,
                templateSetting = newTemplateSetting,
                yamlWithVersion = transferResult.yamlWithVersion,
                // TODO pac模板 需要改造，由code转化接口中获取。
                params = (newTemplateModel as Model).getTriggerContainer().params
            )
        } else {
            val newTemplateType = Preconditions.checkNotNull(
                templateType,
                "template type must not be null"
            )
            val newTemplateModel = Preconditions.checkNotNull(
                templateModel,
                "template model must not be null"
            )
            val newTemplateSetting = Preconditions.checkNotNull(
                templateSetting,
                "template setting must not be null"
            )
            val result = when (templateType) {
                PipelineTemplateType.PIPELINE -> {
                    transferService.transfer(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = null,
                        actionType = TransferActionType.TEMPLATE_MODEL2YAML_PIPELINE,
                        data = TransferBody(
                            templateModelAndSetting = TemplateModelAndSetting(
                                templateModel = newTemplateModel,
                                setting = newTemplateSetting
                            ),
                            oldYaml = ""
                        )
                    )
                }

                PipelineTemplateType.STAGE -> {
                    transferService.transfer(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = null,
                        actionType = TransferActionType.TEMPLATE_MODEL2YAML_STAGE,
                        data = TransferBody(
                            templateModelAndSetting = TemplateModelAndSetting(
                                templateModel = templateModel!!,
                                setting = newTemplateSetting
                            ),
                            oldYaml = ""
                        )
                    )
                }

                PipelineTemplateType.JOB -> {
                    transferService.transfer(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = null,
                        actionType = TransferActionType.TEMPLATE_MODEL2YAML_JOB,
                        data = TransferBody(
                            templateModelAndSetting = TemplateModelAndSetting(
                                templateModel = templateModel!!,
                                setting = newTemplateSetting
                            ),
                            oldYaml = ""
                        )
                    )
                }

                PipelineTemplateType.STEP -> {
                    transferService.transfer(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = null,
                        actionType = TransferActionType.TEMPLATE_MODEL2YAML_STEP,
                        data = TransferBody(
                            templateModelAndSetting = TemplateModelAndSetting(
                                templateModel = templateModel!!,
                                setting = newTemplateSetting
                            ),
                            oldYaml = ""
                        )
                    )
                }

                else -> {
                    throw IllegalArgumentException("unknown template type: $templateType")
                }
            }
            PTemplateModelTransferResult(
                templateType = newTemplateType,
                templateModel = newTemplateModel,
                yamlWithVersion = result.yamlWithVersion,
                templateSetting = newTemplateSetting,
                // TODO pac模板 局部模板时，需要优化
                params = takeIf { templateType == PipelineTemplateType.PIPELINE }?.let {
                    (newTemplateModel as Model).getTriggerContainer().params
                } ?: emptyList(),
            )
        }
    }

    fun buildPreView(
        yaml: String
    ): PreviewResponse {
        val pipelineIndex = mutableListOf<TransferMark>()
        val triggerIndex = mutableListOf<TransferMark>()
        val noticeIndex = mutableListOf<TransferMark>()
        val settingIndex = mutableListOf<TransferMark>()
        try {
            TransferMapper.getYamlLevelOneIndex(yaml).forEach { (key, value) ->
                if (key in pipeline_key) pipelineIndex.add(value)
                if (key in trigger_key) triggerIndex.add(value)
                if (key in notice_key) noticeIndex.add(value)
                if (key in setting_key) settingIndex.add(value)
            }
        } catch (ignore: Throwable) {
            logger.warn("TRANSFER_YAML_FAILED", ignore)
        }
        return PreviewResponse(yaml, pipelineIndex, triggerIndex, noticeIndex, settingIndex)
    }

    companion object {
        private const val TEMPLATE_BIZ_TAG_NAME = "TEMPLATE"
        private const val PAC_TEMPLATE_BRANCH_PREFIX = "bk-ci-template-"
        private val logger = LoggerFactory.getLogger(PipelineTemplateGenerator::class.java)
    }
}
