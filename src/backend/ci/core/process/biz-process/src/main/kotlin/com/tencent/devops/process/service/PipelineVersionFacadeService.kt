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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.PipelineModelWithYaml
import com.tencent.devops.common.pipeline.PipelineModelWithYamlRequest
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceCreateRequest
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.transfer.PreviewResponse
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType
import com.tencent.devops.common.pipeline.pojo.transfer.TransferBody
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.pojo.PipelineVersionWithInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRepositoryVersionService
import com.tencent.devops.process.pojo.PipelineDetail
import com.tencent.devops.process.pojo.PipelineVersionReleaseRequest
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.setting.PipelineVersionSimple
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import com.tencent.devops.process.service.pipeline.PipelineTransferYamlService
import com.tencent.devops.process.service.template.TemplateFacadeService
import com.tencent.devops.process.service.view.PipelineViewGroupService
import com.tencent.devops.process.template.service.TemplateService
import com.tencent.devops.process.yaml.PipelineYamlFacadeService
import com.tencent.devops.process.yaml.transfer.PipelineTransferException
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineVersionFacadeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineListFacadeService: PipelineListFacadeService,
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val templateService: TemplateService,
    private val transferService: PipelineTransferYamlService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val repositoryVersionService: PipelineRepositoryVersionService,
    private val pipelineYamlFacadeService: PipelineYamlFacadeService,
    private val pipelineRecentUseService: PipelineRecentUseService,
    private val templateFacadeService: TemplateFacadeService,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineViewGroupService: PipelineViewGroupService,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineBuildDao: PipelineBuildDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineVersionFacadeService::class.java)
        private const val PAC_BRANCH_PREFIX = "bk-ci-pipeline-"
    }

    fun getPipelineDetailIncludeDraft(
        userId: String,
        projectId: String,
        pipelineId: String
    ): PipelineDetail {
        val detailInfo = pipelineListFacadeService.getPipelineDetail(userId, projectId, pipelineId)
            ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_EXISTS_BY_ID,
                params = arrayOf(pipelineId)
            )
        val draftVersion = pipelineRepositoryService.getDraftVersionResource(
            projectId = projectId,
            pipelineId = pipelineId
        )
        // 存在草稿版本就可以调试
        val canDebug = draftVersion != null
        val releaseVersion = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = detailInfo.pipelineVersion,
            includeDraft = true
        )
        if (draftVersion == null && releaseVersion == null) throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_EXISTS_BY_ID,
            params = arrayOf(pipelineId)
        )
        val canRelease = draftVersion != null
        var baseVersionStatus = VersionStatus.RELEASED
        var baseVersionBranch: String? = null
        draftVersion?.let { draft ->
            val baseVersion = draft.baseVersion?.let { base ->
                pipelineRepositoryService.getPipelineResourceVersion(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = base
                )
            }
            baseVersion?.status?.let { baseVersionStatus = it }
            baseVersion?.versionName?.let { baseVersionBranch = it }
        }
        val releaseSetting = pipelineSettingFacadeService.userGetSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            detailInfo = detailInfo
        )
        val yamlInfo = if (releaseSetting.pipelineAsCodeSettings?.enable == true && releaseVersion != null) {
            pipelineYamlFacadeService.getPipelineYamlInfo(projectId, pipelineId, releaseVersion.version)
        } else null
        val version = draftVersion?.version ?: releaseVersion!!.version
        val versionName = draftVersion?.versionName ?: releaseVersion!!.versionName
        val permissions = pipelineListFacadeService.getPipelinePermissions(userId, projectId, pipelineId)
        pipelineRecentUseService.record(userId, projectId, pipelineId)
        return PipelineDetail(
            pipelineId = detailInfo.pipelineId,
            pipelineName = detailInfo.pipelineName,
            hasCollect = detailInfo.hasCollect,
            instanceFromTemplate = detailInfo.instanceFromTemplate,
            canManualStartup = detailInfo.canManualStartup,
            canDebug = canDebug,
            canRelease = canRelease,
            hasPermission = detailInfo.hasPermission,
            pipelineDesc = detailInfo.pipelineDesc,
            creator = detailInfo.creator,
            createTime = detailInfo.createTime,
            updateTime = detailInfo.updateTime,
            viewNames = detailInfo.viewNames,
            onlyDraft = detailInfo.onlyDraft == true,
            runLockType = releaseSetting.runLockType,
            permissions = permissions,
            version = version,
            versionName = versionName,
            releaseVersion = releaseVersion?.version,
            releaseVersionName = releaseVersion?.versionName,
            baseVersionStatus = baseVersionStatus,
            baseVersionBranch = baseVersionBranch,
            pipelineAsCodeSettings = releaseSetting.pipelineAsCodeSettings ?: PipelineAsCodeSettings(),
            yamlInfo = yamlInfo
        )
    }

    fun releaseDraftVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        request: PipelineVersionReleaseRequest
    ): DeployPipelineResult {
        if (templateService.isTemplatePipeline(projectId, pipelineId)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_TEMPLATE_CAN_NOT_EDIT
            )
        }
        val draftVersion = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            includeDraft = true
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_DRAFT_EXISTS
        )
        val originSetting = draftVersion.settingVersion?.let {
            pipelineSettingFacadeService.userGetSetting(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                version = it
            )
        } ?: pipelineSettingFacadeService.userGetSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId
        )
        // 根据项目PAC状态进行接口调用
        val enabled = originSetting.pipelineAsCodeSettings?.enable == true || request.enablePac
        val targetSettings = originSetting.copy(
            pipelineAsCodeSettings = PipelineAsCodeSettings(enabled)
        )
        var targetUrl: String? = null
        val (versionStatus, branchName) = if (
            enabled && request.targetAction == CodeTargetAction.CHECKOUT_BRANCH_AND_REQUEST_MERGE
        ) {
            Pair(VersionStatus.BRANCH_RELEASE, "${PAC_BRANCH_PREFIX}$pipelineId")
        } else if (enabled && request.targetAction == CodeTargetAction.PUSH_BRANCH_AND_REQUEST_MERGE) {
            val baseVersion = draftVersion.baseVersion?.let {
                pipelineRepositoryService.getPipelineResourceVersion(projectId, pipelineId, it)
            }
            if (baseVersion == null || baseVersion.status != VersionStatus.BRANCH) throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
                params = arrayOf(draftVersion.baseVersion?.toString() ?: "")
            )
            Pair(VersionStatus.BRANCH_RELEASE, baseVersion.versionName)
        } else {
            Pair(VersionStatus.RELEASED, null)
        }
        if (enabled) {
            if (request.yamlInfo == null) throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_NEED_PARAM_,
                params = arrayOf(PipelineVersionReleaseRequest::yamlInfo.name)
            )
            val yamlInfo = request.yamlInfo ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_NEED_PARAM_,
                params = arrayOf(PipelineVersionReleaseRequest::yamlInfo.name)
            )
            // 对前端的YAML信息进行校验
            val filePath = if (yamlInfo.filePath.endsWith(".yaml") || yamlInfo.filePath.endsWith(".yml")) {
                yamlInfo.filePath
            } else {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_YAML_FILENAME,
                    params = arrayOf(yamlInfo.filePath)
                )
            }
            val targetAction = request.targetAction ?: CodeTargetAction.PUSH_BRANCH_AND_REQUEST_MERGE
            // #8161 如果调用代码库同步失败则有报错或提示
            val pushResult = pipelineYamlFacadeService.pushYamlFile(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                version = draftVersion.version,
                versionName = branchName,
                content = draftVersion.yaml ?: "",
                commitMessage = request.description ?: "update",
                repoHashId = yamlInfo.repoHashId,
                scmType = yamlInfo.scmType,
                filePath = filePath,
                targetAction = targetAction
            )
            targetUrl = pushResult.mrUrl
        }
        val model = draftVersion.model
        val savedSetting = pipelineSettingFacadeService.saveSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            updateVersion = false,
            versionStatus = versionStatus,
            setting = targetSettings
        )
        val result = pipelineRepositoryService.deployPipeline(
            model = model,
            projectId = projectId,
            signPipelineId = pipelineId,
            userId = draftVersion.creator,
            channelCode = ChannelCode.BS,
            create = false,
            updateLastModifyUser = true,
            savedSetting = savedSetting,
            versionStatus = versionStatus,
            branchName = branchName,
            description = request.description?.takeIf { it.isNotBlank() } ?: draftVersion.description,
            yamlStr = draftVersion.yaml,
            baseVersion = draftVersion.baseVersion,
            pipelineAsCodeSettings = savedSetting.pipelineAsCodeSettings
        )
        // 添加标签
        pipelineGroupService.addPipelineLabel(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            labelIds = model.labels
        )
        // 添加到动态分组
        pipelineViewGroupService.updateGroupAfterPipelineUpdate(
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            creator = userId,
            pipelineName = savedSetting.pipelineName
        )
        // 添加到静态流水线组
        pipelineViewGroupService.bulkAddStatic(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            staticViewIds = request.staticViews
        )
        // #8164 发布后的流水将调试信息清空为0，重新计数，同时删除所有该版本的调试记录
        pipelineBuildSummaryDao.resetDebugInfo(dslContext, projectId, pipelineId)
        pipelineBuildDao.clearDebugHistory(dslContext, projectId, pipelineId, version)
        return DeployPipelineResult(
            pipelineId = pipelineId,
            pipelineName = draftVersion.model.name,
            version = result.version,
            versionNum = null,
            versionName = result.versionName,
            targetUrl = targetUrl
        )
    }

    fun createPipelineFromTemplate(
        userId: String,
        projectId: String,
        request: TemplateInstanceCreateRequest
    ): DeployPipelineResult {
        val (templateModel, instanceFromTemplate) = if (request.emptyTemplate == true) {
            val model = Model(
                name = request.pipelineName,
                desc = "",
                stages = listOf(
                    Stage(
                        id = "stage-1",
                        containers = listOf(
                            TriggerContainer(
                                id = "0",
                                name = "trigger",
                                elements = listOf(
                                    ManualTriggerElement(
                                        id = "T-1-1-1",
                                        name = I18nUtil.getCodeLanMessage(
                                            CommonMessageCode.BK_MANUAL_TRIGGER,
                                            language = I18nUtil.getLanguage(userId
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                ),
                pipelineCreator = userId
            )
            Pair(model, true)
        } else {
            val template = templateFacadeService.getTemplate(
                userId = userId,
                projectId = projectId,
                templateId = request.templateId,
                version = request.templateVersion
            )
            Pair(template.template, true)
        }
        return pipelineInfoFacadeService.createPipeline(
            userId = userId,
            projectId = projectId,
            model = templateModel.copy(
                name = request.pipelineName,
                templateId = request.templateId,
                instanceFromTemplate = instanceFromTemplate,
                labels = request.labels,
                staticViews = request.staticViews
            ),
            channelCode = ChannelCode.BS,
            checkPermission = true,
            instanceType = request.instanceType,
            versionStatus = VersionStatus.COMMITTING,
            useSubscriptionSettings = request.useSubscriptionSettings,
            useLabelSettings = request.useLabelSettings,
            useConcurrencyGroup = request.useConcurrencyGroup
        )
    }

    fun getVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int
    ): PipelineModelWithYaml {
        val resource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            includeDraft = true
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
            params = arrayOf(version.toString())
        )
        val setting = pipelineSettingFacadeService.userGetSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = resource.settingVersion ?: version
        )
        val modelAndSetting = PipelineModelAndSetting(
            setting = setting,
            model = pipelineInfoFacadeService.getFixedModel(
                model = resource.model,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                pipelineInfo = null
            )
        )
        val baseResource = resource.baseVersion?.let {
            repositoryVersionService.getPipelineVersionSimple(
                projectId = projectId,
                pipelineId = pipelineId,
                version = it
            )
        }
        val (yamlSupported, yamlPreview, msg) = try {
            val response = transferService.buildPreview(
                userId, projectId, pipelineId, resource
            )
            Triple(true, response, null)
        } catch (e: PipelineTransferException) {
            Triple(
                false, null, I18nUtil.getCodeLanMessage(
                    messageCode = e.errorCode,
                    params = e.params,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                    defaultMessage = e.defaultMessage
                )
            )
        }
        return PipelineModelWithYaml(
            modelAndSetting = modelAndSetting,
            yamlPreview = yamlPreview,
            description = resource.description,
            canDebug = resource.status == VersionStatus.COMMITTING,
            version = resource.version,
            versionName = resource.versionName,
            baseVersion = resource.baseVersion,
            baseVersionName = baseResource?.versionName,
            yamlSupported = yamlSupported,
            yamlInvalidMsg = msg
        )
    }

    fun preview(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int?
    ): PreviewResponse {
        val resource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            includeDraft = true
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
            params = arrayOf(version.toString())
        )
        return transferService.buildPreview(userId, projectId, pipelineId, resource)
    }

    fun savePipelineDraft(
        userId: String,
        projectId: String,
        modelAndYaml: PipelineModelWithYamlRequest
    ): DeployPipelineResult {
        val pipelineId = modelAndYaml.pipelineId
        val versionStatus = VersionStatus.COMMITTING
        val (model, setting, newYaml) = if (modelAndYaml.storageType == PipelineStorageType.YAML) {
            // YAML形式的保存需要所有插件都为支持转换的市场插件
            val transferResult = transferService.transfer(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                actionType = TransferActionType.FULL_YAML2MODEL,
                data = TransferBody(
                    modelAndSetting = modelAndYaml.modelAndSetting,
                    oldYaml = modelAndYaml.yaml ?: ""
                )
            )
            Triple(transferResult.modelAndSetting?.model, transferResult.modelAndSetting?.setting, modelAndYaml.yaml)
        } else {
            // MODEL形式的保存需要兼容旧数据
            val newYaml = try {
                transferService.transfer(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    actionType = TransferActionType.FULL_MODEL2YAML,
                    data = TransferBody(
                        modelAndSetting = modelAndYaml.modelAndSetting,
                        oldYaml = pipelineId?.let {
                            pipelineRepositoryService.getPipelineResourceVersion(
                                projectId = projectId,
                                pipelineId = it,
                                version = modelAndYaml.baseVersion,
                                includeDraft = true
                            )?.yaml
                        } ?: ""
                    )
                ).newYaml
            } catch (ignore: Throwable) {
                // 旧流水线可能无法转换，用空YAML代替
                logger.warn("TRANSFER_YAML|$projectId|$userId|${ignore.message}|modelAndYaml=\n${modelAndYaml.yaml}")
                null
            }
            Triple(modelAndYaml.modelAndSetting.model, modelAndYaml.modelAndSetting.setting, newYaml)
        }
        return if (pipelineId.isNullOrBlank()) {
            // 新建流水线产生草稿
            pipelineInfoFacadeService.createPipeline(
                userId = userId,
                projectId = projectId,
                model = model ?: modelAndYaml.modelAndSetting.model,
                channelCode = ChannelCode.BS,
                checkPermission = true,
                versionStatus = versionStatus,
                yaml = newYaml
            )
        } else {
            // 修改已存在的草稿
            val draft = pipelineRepositoryService.getDraftVersionResource(projectId, pipelineId)
            val savedSetting = pipelineSettingFacadeService.saveSetting(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                setting = setting ?: modelAndYaml.modelAndSetting.setting,
                checkPermission = false,
                versionStatus = versionStatus,
                updateVersion = draft == null,
                dispatchPipelineUpdateEvent = false
            )

            pipelineInfoFacadeService.editPipeline(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                model = model ?: modelAndYaml.modelAndSetting.model,
                channelCode = ChannelCode.BS,
                checkPermission = true,
                checkTemplate = false,
                versionStatus = versionStatus,
                description = modelAndYaml.description,
                yaml = newYaml,
                savedSetting = savedSetting
            )
        }
    }

    fun listPipelineVersion(
        projectId: String,
        pipelineId: String,
        page: Int,
        pageSize: Int,
        fromVersion: Int?,
        versionName: String?,
        creator: String? = null,
        description: String? = null
    ): Page<PipelineVersionWithInfo> {
        var slqLimit: SQLLimit? = null
        if (pageSize != -1) slqLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)

        val offset = slqLimit?.offset ?: 0
        var limit = slqLimit?.limit ?: -1
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
        // 如果有要插队的版本需要提到第一页，则在查询list时排除，单独查出来放在第一页
        val fromResource = if (fromVersion != null && page == 1) {
            limit -= 1
            repositoryVersionService.getPipelineVersionWithInfo(
                pipelineInfo = pipelineInfo,
                projectId = projectId,
                pipelineId = pipelineId,
                version = fromVersion
            )
        } else null
        val (size, pipelines) = repositoryVersionService.listPipelineVersion(
            pipelineInfo = pipelineInfo,
            projectId = projectId,
            pipelineId = pipelineId,
            creator = creator,
            description = description,
            versionName = versionName,
            excludeVersion = fromVersion,
            offset = offset,
            limit = limit
        )
        fromResource?.let { pipelines.add(it) }
        return Page(
            page = page,
            pageSize = pageSize,
            count = size.toLong(),
            records = pipelines
        )
    }

    fun rollbackDraftFromVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int
    ): PipelineVersionSimple {
        val resource = pipelineRepositoryService.rollbackDraftFromVersion(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        )
        return PipelineVersionSimple(
            pipelineId = pipelineId,
            creator = resource.creator,
            createTime = resource.createTime.timestampmilli(),
            updateTime = resource.updateTime?.timestampmilli(),
            version = resource.version,
            versionName = resource.versionName ?: "init",
            referFlag = resource.referFlag,
            referCount = resource.referCount,
            versionNum = resource.versionNum,
            pipelineVersion = resource.pipelineVersion,
            triggerVersion = resource.triggerVersion,
            settingVersion = resource.settingVersion,
            status = resource.status,
            debugBuildId = resource.debugBuildId,
            baseVersion = resource.baseVersion,
            description = resource.description
        )
    }

    fun deletePipelineVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        checkPermission: Boolean = true
    ): String {
        repositoryVersionService.deletePipelineVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        )
        return pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)?.pipelineName ?: pipelineId
    }

    fun getVersionCreatorInPage(
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?
    ): Page<String> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        var slqLimit: SQLLimit? = null
        if (pageSizeNotNull != -1) slqLimit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)

        val offset = slqLimit?.offset ?: 0
        val limit = slqLimit?.limit ?: -1
        // 数据量不多，直接全拉
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
        val (size, pipelines) = repositoryVersionService.getVersionCreatorInPage(
            pipelineInfo = pipelineInfo,
            projectId = projectId,
            pipelineId = pipelineId,
            offset = offset,
            limit = limit
        )
        return Page(
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            count = size.toLong(),
            records = pipelines
        )
    }
}
