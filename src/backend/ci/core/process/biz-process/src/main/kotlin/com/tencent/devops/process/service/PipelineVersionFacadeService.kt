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
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.PipelineVersionWithModel
import com.tencent.devops.common.pipeline.PipelineVersionWithModelRequest
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceCreateRequest
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.transfer.PreviewResponse
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType
import com.tencent.devops.common.pipeline.pojo.transfer.TransferBody
import com.tencent.devops.common.pipeline.pojo.transfer.YamlWithVersion
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.control.lock.PipelineReleaseLock
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.pojo.PipelineVersionWithInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRepositoryVersionService
import com.tencent.devops.process.pojo.PipelineDetail
import com.tencent.devops.process.pojo.PipelineVersionReleaseRequest
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.PrefetchReleaseResult
import com.tencent.devops.process.pojo.setting.PipelineVersionSimple
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import com.tencent.devops.process.service.pipeline.PipelineTransferYamlService
import com.tencent.devops.process.service.template.TemplateFacadeService
import com.tencent.devops.process.service.view.PipelineViewGroupService
import com.tencent.devops.process.template.service.TemplateService
import com.tencent.devops.process.utils.PipelineVersionUtils
import com.tencent.devops.process.yaml.PipelineYamlFacadeService
import com.tencent.devops.process.yaml.transfer.PipelineTransferException
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Suppress("ALL")
@Service
class PipelineVersionFacadeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val modelCheckPlugin: ModelCheckPlugin,
    private val pipelineListFacadeService: PipelineListFacadeService,
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val templateService: TemplateService,
    private val transferService: PipelineTransferYamlService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val repositoryVersionService: PipelineRepositoryVersionService,
    private val pipelineYamlFacadeService: PipelineYamlFacadeService,
    private val pipelineBuildFacadeService: PipelineBuildFacadeService,
    private val pipelineRecentUseService: PipelineRecentUseService,
    private val templateFacadeService: TemplateFacadeService,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineViewGroupService: PipelineViewGroupService,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineBuildDao: PipelineBuildDao,
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineAsCodeService: PipelineAsCodeService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineVersionFacadeService::class.java)
        private const val PAC_BRANCH_PREFIX = "bk-ci-pipeline-"
        fun getReleaseBranchName(pipelineId: String, version: Int): String =
            "$PAC_BRANCH_PREFIX$pipelineId-$version"
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
        // 有草稿且不是空白的编排才可以发布
        val canRelease = draftVersion != null && draftVersion.model.stages.size > 1
        // 存在草稿版本就可以调试
        val canDebug = draftVersion != null
        val releaseVersion = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_EXISTS_BY_ID,
            params = arrayOf(pipelineId)
        )
        val yamlInfo = pipelineYamlFacadeService.getPipelineYamlInfo(projectId, pipelineId, releaseVersion.version)
        var baseVersion: Int? = null
        var baseVersionName: String? = null
        var baseVersionStatus: VersionStatus? = null
        draftVersion?.let { draft ->
            val baseResource = draft.baseVersion?.let { base ->
                pipelineRepositoryService.getPipelineResourceVersion(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = base
                )
            }
            baseResource?.let { baseVersion = it.version }
            baseResource?.status?.let { baseVersionStatus = it }
            baseResource?.versionName?.let { baseVersionName = it }
        }
        val releaseSetting = pipelineSettingFacadeService.userGetSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            detailInfo = detailInfo
        )
        val released = detailInfo.latestVersionStatus?.isNotReleased() != true
        var versionName = releaseVersion.versionName?.takeIf { released }
        // 配合前端的展示需要，version有以下几种情况的返回值：
        // 1 发布过且有草稿：version取草稿的版本号
        // 2 发布过且有分支版本：version取最新正式的版本号
        // 3 未发布过仅有草稿版本：version取草稿的版本号
        // 4 未发布过仅有分支版本：version取最新的分支版本号
        val version = when (detailInfo.latestVersionStatus) {
            VersionStatus.COMMITTING -> {
                draftVersion?.version
            }
            VersionStatus.BRANCH -> {
                val branchVersion = pipelineRepositoryService.getBranchVersionResource(
                    projectId, pipelineId, null
                )
                versionName = branchVersion?.versionName
                branchVersion?.version
            }
            else -> {
                draftVersion?.version
            }
        } ?: releaseVersion.version
        val permissions = pipelineListFacadeService.getPipelinePermissions(userId, projectId, pipelineId)
        val yamlExist = pipelineYamlFacadeService.yamlExistInDefaultBranch(
            projectId = projectId,
            pipelineId = pipelineId
        )
        pipelineRecentUseService.record(userId, projectId, pipelineId)
        return PipelineDetail(
            pipelineId = detailInfo.pipelineId,
            pipelineName = detailInfo.pipelineName,
            hasCollect = detailInfo.hasCollect,
            instanceFromTemplate = detailInfo.instanceFromTemplate,
            templateId = detailInfo.templateId,
            templateVersion = detailInfo.templateVersion,
            canManualStartup = detailInfo.canManualStartup,
            canDebug = canDebug,
            canRelease = canRelease,
            hasPermission = detailInfo.hasPermission,
            pipelineDesc = detailInfo.pipelineDesc,
            creator = detailInfo.creator,
            createTime = detailInfo.createTime,
            updateTime = detailInfo.updateTime,
            viewNames = detailInfo.viewNames,
            latestVersionStatus = detailInfo.latestVersionStatus,
            runLockType = releaseSetting.runLockType,
            permissions = permissions,
            version = version,
            versionName = versionName,
            // 前端需要缺省当前能用的版本，用于进入页面的默认展示，但没有发布过就不提供releaseVersionName
            releaseVersion = releaseVersion.version.takeIf { released } ?: version,
            releaseVersionName = releaseVersion.versionName?.takeIf { released },
            baseVersion = baseVersion,
            baseVersionStatus = baseVersionStatus,
            baseVersionName = baseVersionName,
            pipelineAsCodeSettings = PipelineAsCodeSettings(enable = yamlInfo != null),
            yamlInfo = yamlInfo,
            yamlExist = yamlExist,
            locked = detailInfo.locked
        )
    }

    fun preFetchDraftVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int
    ): PrefetchReleaseResult {
        val draftVersion = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            includeDraft = true
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_DRAFT_EXISTS
        )
        val draftSetting = draftVersion.settingVersion?.let {
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
        val releaseVersion = pipelineRepositoryService.getPipelineResourceVersion(projectId, pipelineId)
            ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_EXISTS_BY_ID,
                params = arrayOf(pipelineId)
            )
        val newVersionNum = (releaseVersion.versionNum ?: releaseVersion.version) + 1
        val prefetchVersionName = PipelineVersionUtils.getVersionNameByModel(
            currPipelineVersion = releaseVersion.pipelineVersion ?: 1,
            currTriggerVersion = releaseVersion.triggerVersion ?: 1,
            settingVersion = draftSetting.version,
            versionNum = newVersionNum,
            originModel = releaseVersion.model,
            newModel = draftVersion.model
        )
        return PrefetchReleaseResult(
            pipelineId = pipelineId,
            pipelineName = draftVersion.model.name,
            version = draftVersion.version,
            newVersionNum = newVersionNum,
            newVersionName = prefetchVersionName
        )
    }

    fun releaseDraftVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        request: PipelineVersionReleaseRequest
    ): DeployPipelineResult {
        PipelineReleaseLock(redisOperation, pipelineId).use {
            val pipeline = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
                ?: throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
                )
//        if (templateService.isTemplatePipeline(projectId, pipelineId)) {
//            throw ErrorCodeException(
//                errorCode = ProcessMessageCode.ERROR_PIPELINE_TEMPLATE_CAN_NOT_EDIT
//            )
//        }
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
            // 提前初始化检查一次让编排报错，避免PAC代码库操作后报错
            pipelineRepositoryService.initModel(
                model = draftVersion.model, projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                create = false,
                versionStatus = VersionStatus.RELEASED,
                channelCode = pipeline.channelCode,
                yamlInfo = request.yamlInfo,
                pipelineDialect = pipelineAsCodeService.getPipelineDialect(
                    projectId = projectId,
                    asCodeSettings = originSetting.pipelineAsCodeSettings
                )
            )
            val originYaml = pipelineYamlFacadeService.getPipelineYamlInfo(projectId, pipelineId, version)
            // 如果不匹配已有状态则报错，需要用户重新刷新页面
            if (originYaml != null && !request.enablePac) throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_IS_NOT_THE_LATEST
            )
            // 根据项目PAC状态进行接口调用
            val enabled = originYaml != null || request.enablePac
            val targetSettings = originSetting.copy(
                pipelineAsCodeSettings = originSetting.pipelineAsCodeSettings?.copy(enable = enabled)
            )
            val (versionStatus, branchName) = if (
                enabled && request.targetAction == CodeTargetAction.CHECKOUT_BRANCH_AND_REQUEST_MERGE
            ) {
                Pair(VersionStatus.BRANCH_RELEASE, getReleaseBranchName(pipelineId, draftVersion.version))
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
                Pair(VersionStatus.DRAFT_RELEASE, null)
            }
            val targetAction = request.targetAction ?: CodeTargetAction.PUSH_BRANCH_AND_REQUEST_MERGE
            if (enabled) {
                if (request.yamlInfo == null) throw ErrorCodeException(
                    errorCode = CommonMessageCode.ERROR_NEED_PARAM_,
                    params = arrayOf(PipelineVersionReleaseRequest::yamlInfo.name)
                )
                if (draftVersion.yaml.isNullOrBlank()) {
                    transferService.transfer(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        actionType = TransferActionType.FULL_MODEL2YAML,
                        data = TransferBody(
                            modelAndSetting = PipelineModelAndSetting(draftVersion.model, targetSettings),
                            yamlFileName = request.yamlInfo?.filePath
                        )
                    )
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_YAML_CONTENT_IS_EMPTY
                    )
                }
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
                pipelineYamlFacadeService.checkPushParam(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = draftVersion.version,
                    versionName = branchName,
                    repoHashId = yamlInfo.repoHashId,
                    scmType = yamlInfo.scmType!!,
                    filePath = filePath,
                    content = draftVersion.yaml ?: "",
                    targetAction = targetAction
                )
            }

            val savedSetting = pipelineSettingFacadeService.saveSetting(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                updateVersion = false,
                versionStatus = versionStatus,
                setting = targetSettings
            )
            if (versionStatus.isReleasing()) {
                val existModel = pipelineRepositoryService.getPipelineResourceVersion(
                    projectId = projectId,
                    pipelineId = pipelineId
                )?.model ?: throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
                )
                // 对已经存在的模型做处理
                val param = BeforeDeleteParam(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId
                )
                modelCheckPlugin.beforeDeleteElementInExistsModel(existModel, draftVersion.model, param)
            }
            val result = pipelineRepositoryService.deployPipeline(
                model = draftVersion.model,
                projectId = projectId,
                signPipelineId = pipelineId,
                userId = userId,
                channelCode = pipeline.channelCode,
                create = false,
                updateLastModifyUser = true,
                setting = savedSetting,
                versionStatus = versionStatus,
                branchName = branchName,
                description = request.description?.takeIf { it.isNotBlank() } ?: draftVersion.description,
                yaml = YamlWithVersion(versionTag = draftVersion.yamlVersion, yamlStr = draftVersion.yaml),
                baseVersion = draftVersion.baseVersion,
                yamlInfo = request.yamlInfo
            )
            // 添加标签
            pipelineGroupService.addPipelineLabel(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                labelIds = targetSettings.labels
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
            // #8164 发布后的流水将调试信息清空为0，重新计数，同时取消该版本的调试记录
            pipelineBuildDao.getDebugHistory(dslContext, projectId, pipelineId).forEach { debug ->
                if (!debug.status.isFinish()) {
                    buildLogPrinter.addWarnLine(
                        buildId = debug.buildId, executeCount = debug.executeCount ?: 1,
                        tag = "", jobId = null, stepId = null,
                        message = ""
                    )
                    pipelineBuildFacadeService.buildManualShutdown(
                        userId = userId, projectId = projectId, pipelineId = pipelineId,
                        buildId = debug.buildId, channelCode = pipeline.channelCode, terminateFlag = true
                    )
                }
            }
            // 查询编排中的基准值，并把调试的版本号刷为基准值
            val debugBuildNo = draftVersion.model.getTriggerContainer()
                .buildNo?.buildNo ?: 0
            pipelineBuildSummaryDao.resetDebugInfo(dslContext, projectId, pipelineId, debugBuildNo)
            pipelineBuildDao.clearDebugHistory(dslContext, projectId, pipelineId)

            var targetUrl: String? = null
            // 推送代码库应该在流水线变更成功之后,蓝盾发布形成闭环,如果代码库推送失败,应该引导用户手工修复
            if (enabled) {
                val yamlInfo = request.yamlInfo!!
                // #8161 如果调用代码库同步失败则有报错或提示
                val pushResult = pipelineYamlFacadeService.pushYamlFile(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = draftVersion.version,
                    versionName = branchName,
                    pipelineName = targetSettings.pipelineName,
                    content = draftVersion.yaml ?: "",
                    commitMessage = request.description ?: "update",
                    repoHashId = yamlInfo.repoHashId,
                    scmType = yamlInfo.scmType!!,
                    filePath = yamlInfo.filePath,
                    targetAction = targetAction
                )
                targetUrl = pushResult.mrUrl
            }
            val yamlInfo = pipelineYamlFacadeService.getPipelineYamlInfo(projectId, pipelineId, version)
            return DeployPipelineResult(
                pipelineId = pipelineId,
                pipelineName = draftVersion.model.name,
                version = result.version,
                versionNum = result.versionNum,
                versionName = result.versionName,
                targetUrl = targetUrl,
                yamlInfo = yamlInfo,
                updateBuildNo = result.updateBuildNo
            )
        }
    }

    /**
     * 从自由模式下创建流水线
     */
    fun createPipelineFromFreedom(
        userId: String,
        projectId: String,
        request: TemplateInstanceCreateRequest
    ): DeployPipelineResult {
        val templateModel = if (request.emptyTemplate == true) {
            Model(
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
        } else {
            templateFacadeService.getTemplate(
                userId = userId,
                projectId = projectId,
                templateId = request.templateId,
                version = request.templateVersion
            ).template
        }
        val pipelineAsCodeSettings = PipelineAsCodeSettings.initDialect(
            inheritedDialect = request.inheritedDialect,
            pipelineDialect = request.pipelineDialect
        )
        val setting = pipelineRepositoryService.createDefaultSetting(
            projectId = projectId,
            pipelineId = "",
            pipelineName = request.pipelineName,
            channelCode = ChannelCode.BS
        ).copy(
            pipelineAsCodeSettings = pipelineAsCodeSettings,
            labels = request.labels
        )

        return pipelineInfoFacadeService.createPipeline(
            userId = userId,
            projectId = projectId,
            model = templateModel.copy(
                name = request.pipelineName,
                templateId = request.templateId,
                instanceFromTemplate = false,
                staticViews = request.staticViews
            ),
            channelCode = ChannelCode.BS,
            setting = setting,
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
    ): PipelineVersionWithModel {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
            )
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
            version = resource.settingVersion ?: 0 // 历史没有关联过setting版本应该取正式版本
        )
        val model = pipelineInfoFacadeService.getFixedModel(
            model = resource.model,
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            pipelineInfo = pipelineInfo
        )
        /* 兼容存量数据 */
        model.desc = setting.desc
        // 后端主动填充前端展示的标签名称
        val modelAndSetting = PipelineModelAndSetting(
            setting = setting,
            model = model
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
            Triple(false, null, I18nUtil.getCodeLanMessage(
                messageCode = e.errorCode,
                params = e.params,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                defaultMessage = e.defaultMessage
            ))
        }
        return PipelineVersionWithModel(
            modelAndSetting = modelAndSetting,
            yamlPreview = yamlPreview,
            description = resource.description,
            canDebug = resource.status == VersionStatus.COMMITTING,
            version = resource.version,
            versionName = resource.versionName,
            baseVersion = resource.baseVersion,
            baseVersionName = baseResource?.versionName,
            yamlSupported = yamlSupported,
            yamlInvalidMsg = msg,
            updater = resource.updater ?: resource.creator,
            updateTime = resource.updateTime?.timestampmilli()
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
        modelAndYaml: PipelineVersionWithModelRequest
    ): DeployPipelineResult {
        val pipelineId = modelAndYaml.pipelineId
        val versionStatus = VersionStatus.COMMITTING
        val model: Model?
        val setting: PipelineSetting?
        var newYaml: YamlWithVersion?
        if (modelAndYaml.storageType == PipelineStorageType.YAML) {
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
            model = transferResult.modelAndSetting?.model
            setting = transferResult.modelAndSetting?.setting
            newYaml = transferResult.yamlWithVersion
        } else {
            // MODEL形式的保存需要兼容旧数据
            try {
                val result = transferService.transfer(
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
                )
                newYaml = result.yamlWithVersion
            } catch (ignore: Throwable) {
                // 旧流水线可能无法转换，用空YAML代替
                logger.warn("TRANSFER_YAML|$projectId|$userId|${ignore.message}|modelAndYaml=\n${modelAndYaml.yaml}")
                newYaml = null
            }
            model = modelAndYaml.modelAndSetting.model
            setting = modelAndYaml.modelAndSetting.setting
        }
        return if (pipelineId.isNullOrBlank()) {
            // 新建流水线产生草稿
            pipelineInfoFacadeService.createPipeline(
                userId = userId,
                projectId = projectId,
                model = model ?: modelAndYaml.modelAndSetting.model,
                setting = setting ?: modelAndYaml.modelAndSetting.setting,
                channelCode = ChannelCode.BS,
                checkPermission = true,
                versionStatus = versionStatus,
                yaml = newYaml
            )
        } else {
            // 修改已存在的流水线
            val isTemplate = templateService.isTemplatePipeline(projectId, pipelineId)
            val release = pipelineRepositoryService.getPipelineResourceVersion(projectId, pipelineId)
            val savedSetting = pipelineSettingFacadeService.saveSetting(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                setting = setting ?: modelAndYaml.modelAndSetting.setting,
                checkPermission = false,
                versionStatus = versionStatus,
                dispatchPipelineUpdateEvent = false,
                updateLabels = false
            )
            pipelineInfoFacadeService.editPipeline(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                model = if (isTemplate) {
                    release?.model
                } else {
                    model
                } ?: modelAndYaml.modelAndSetting.model,
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

    fun listPipelineVersionInfo(
        projectId: String,
        pipelineId: String,
        page: Int,
        pageSize: Int,
        fromVersion: Int?,
        includeDraft: Boolean? = true,
        versionName: String? = null,
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
                version = fromVersion,
                includeDraft = includeDraft
            )
        } else null
        val (size, pipelines) = repositoryVersionService.listPipelineVersionWithInfo(
            pipelineInfo = pipelineInfo,
            projectId = projectId,
            pipelineId = pipelineId,
            creator = creator,
            description = description,
            versionName = versionName,
            includeDraft = includeDraft,
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

    fun getPipelineVersionInfo(
        projectId: String,
        pipelineId: String,
        version: Int
    ): PipelineVersionWithInfo {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
        return repositoryVersionService.getPipelineVersionWithInfo(
            pipelineInfo = pipelineInfo,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
            params = arrayOf(version.toString())
        )
    }

    fun listPipelineVersion(
        projectId: String,
        pipelineId: String,
        page: Int,
        pageSize: Int,
        fromVersion: Int?,
        includeDraft: Boolean? = true,
        versionName: String? = null,
        creator: String? = null,
        description: String? = null
    ): Page<PipelineVersionSimple> {
        var slqLimit: SQLLimit? = null
        if (pageSize != -1) slqLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)

        val offset = slqLimit?.offset ?: 0
        var limit = slqLimit?.limit ?: -1
        val result = mutableListOf<PipelineVersionSimple>()
        // 如果有草稿版本需要提到第一页，单独查出来放在第一页并顶置
        val draftResource = if (includeDraft != false && page == 1) {
            limit -= 1
            pipelineRepositoryService.getDraftVersionResource(
                projectId = projectId,
                pipelineId = pipelineId
            )?.toSimple()?.apply {
                baseVersionName = baseVersion?.let {
                    repositoryVersionService.getPipelineVersionSimple(
                        projectId, pipelineId, it
                    )?.versionName
                }
            }
        } else null
        // 如果有要插队的版本需要提到第一页，则在查询list时排除，单独查出来放在第一页
        val fromResource = if (fromVersion != null && page == 1) {
            limit -= 1
            repositoryVersionService.getPipelineVersionSimple(
                projectId = projectId,
                pipelineId = pipelineId,
                version = fromVersion
            )
        } else null
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
        var (size, pipelines) = repositoryVersionService.listPipelineReleaseVersion(
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
        draftResource?.let {
            size++
            result.add(it)
        }
        result.addAll(pipelines)
        fromResource?.let {
            size++
            result.add(it)
        }
        return Page(
            page = page,
            pageSize = pageSize,
            count = size.toLong(),
            records = result
        )
    }

    fun getPipelineVersion(
        projectId: String,
        pipelineId: String,
        version: Int
    ): PipelineVersionSimple {
        return repositoryVersionService.getPipelineVersionSimple(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
            params = arrayOf(version.toString())
        )
    }

    fun rollbackDraftFromVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int
    ): PipelineVersionSimple {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
            )
        // 获取目标的版本用于更新草稿
        val targetVersion = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
            params = arrayOf(version.toString())
        )
        val resource = pipelineRepositoryService.rollbackDraftFromVersion(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            targetVersion = targetVersion.copy(
                model = pipelineInfoFacadeService.getFixedModel(
                    targetVersion.model, projectId, pipelineId, userId, pipelineInfo
                )
            )
        )
        return PipelineVersionSimple(
            pipelineId = pipelineId,
            creator = resource.creator,
            createTime = resource.createTime.timestampmilli(),
            updater = resource.updater,
            updateTime = resource.updateTime?.timestampmilli(),
            version = resource.version,
            versionName = resource.versionName ?: "",
            referFlag = resource.referFlag,
            referCount = resource.referCount,
            versionNum = resource.versionNum,
            pipelineVersion = resource.pipelineVersion,
            triggerVersion = resource.triggerVersion,
            settingVersion = resource.settingVersion,
            status = resource.status,
            debugBuildId = resource.debugBuildId,
            baseVersion = resource.baseVersion,
            description = resource.description,
            yamlVersion = resource.yamlVersion
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
