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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.NUM_ZERO
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.db.pojo.ARCHIVE_SHARDING_DSL_CONTEXT
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.PipelineVersionWithModel
import com.tencent.devops.common.pipeline.PipelineVersionWithModelRequest
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceCreateRequest
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.transfer.PreviewResponse
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.pojo.PipelineVersionWithInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRepositoryVersionService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.PipelineDetail
import com.tencent.devops.process.pojo.PipelineVersionReleaseRequest
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.pipeline.PrefetchReleaseResult
import com.tencent.devops.process.pojo.pipeline.version.PipelineDraftSaveReq
import com.tencent.devops.process.pojo.setting.PipelineVersionSimple
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import com.tencent.devops.process.service.pipeline.PipelineTransferYamlService
import com.tencent.devops.process.service.pipeline.version.PipelineVersionManager
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.process.service.template.TemplateFacadeService
import com.tencent.devops.process.service.template.v2.PipelineTemplateRelatedService
import com.tencent.devops.process.utils.PipelineVersionUtils
import com.tencent.devops.process.yaml.PipelineYamlFacadeService
import com.tencent.devops.process.yaml.transfer.PipelineTransferException
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineVersionFacadeService @Autowired constructor(
    private val pipelineListFacadeService: PipelineListFacadeService,
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val transferService: PipelineTransferYamlService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val repositoryVersionService: PipelineRepositoryVersionService,
    private val pipelineYamlFacadeService: PipelineYamlFacadeService,
    private val pipelineRecentUseService: PipelineRecentUseService,
    private val templateFacadeService: TemplateFacadeService,
    private val scmProxyService: ScmProxyService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineVersionManager: PipelineVersionManager,
    private val pipelineTemplateRelatedService: PipelineTemplateRelatedService,
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
        pipelineId: String,
        archiveFlag: Boolean? = false
    ): PipelineDetail {
        val detailInfo = pipelineListFacadeService.getPipelineDetail(
            userId = userId, projectId = projectId, pipelineId = pipelineId, archiveFlag = archiveFlag
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_EXISTS_BY_ID, params = arrayOf(pipelineId)
        )
        val draftResource = pipelineRepositoryService.getDraftVersionResource(
            projectId = projectId, pipelineId = pipelineId, archiveFlag = archiveFlag
        )
        // 有草稿且不是空白的编排才可以发布
        val canRelease = draftResource != null && draftResource.model.stages.size > 1
        // 存在草稿版本就可以调试
        val canDebug = draftResource != null
        val releaseResource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = detailInfo.pipelineVersion,
            archiveFlag = archiveFlag
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_EXISTS_BY_ID,
            params = arrayOf(pipelineId)
        )
        val yamlInfo = if (archiveFlag != true) {
            pipelineYamlFacadeService.getPipelineYamlInfo(projectId, pipelineId, releaseResource.version)
        } else {
            null
        }
        var baseVersion: Int? = null
        var baseVersionName: String? = null
        var baseVersionStatus: VersionStatus? = null
        draftResource?.let { draft ->
            val baseResource = draft.baseVersion?.let { base ->
                pipelineRepositoryService.getPipelineResourceVersion(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = base,
                    archiveFlag = archiveFlag
                )
            }
            baseResource?.let { baseVersion = it.version }
            baseResource?.status?.let { baseVersionStatus = it }
            baseResource?.versionName?.let { baseVersionName = it }
        }
        val releaseSetting = if (archiveFlag != true) {
            pipelineSettingFacadeService.userGetSetting(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                detailInfo = detailInfo
            )
        } else {
            null
        }
        /**
         * 获取最新版本和版本名称
         *
         * 如果最新版本是分支版本,则需要获取分支最新的激活版本,否则最新版本可能是正式或者草稿版本
         */
        val (releaseVersion, releaseVersionName) = when (releaseResource.status) {
            // 分支版本,需要获取当前分支最新的激活版本
            VersionStatus.BRANCH -> {
                val branchVersion = pipelineRepositoryService.getBranchVersionResource(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    branchName = releaseResource.versionName,
                    archiveFlag = archiveFlag
                )
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
        val permissions = pipelineListFacadeService.getPipelinePermissions(userId, projectId, pipelineId)
        val yamlExist = archiveFlag.takeUnless { it == true }?.run {
            pipelineRecentUseService.record(userId, projectId, pipelineId)
            pipelineYamlFacadeService.yamlExistInDefaultBranch(projectId, pipelineId)
        }
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
            runLockType = releaseSetting?.runLockType,
            permissions = permissions,
            version = version,
            versionName = versionName,
            releaseVersion = releaseVersion,
            releaseVersionName = releaseVersionName,
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
        version: Int,
        targetAction: CodeTargetAction?,
        repoHashId: String?,
        targetBranch: String?
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
        val prefetchVersionName = targetAction?.let {
            getVersionStatusAndName(
                projectId = projectId,
                pipelineId = pipelineId,
                draftVersion = draftVersion,
                targetAction = targetAction,
                repoHashId = repoHashId,
                targetBranch = targetBranch
            ).second
        } ?: PipelineVersionUtils.getVersionNameByModel(
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
        return pipelineVersionManager.deployPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            request = request
        )
    }

    private fun getVersionStatusAndName(
        projectId: String,
        pipelineId: String,
        draftVersion: PipelineResourceVersion,
        targetAction: CodeTargetAction,
        repoHashId: String?,
        targetBranch: String?
    ): Pair<VersionStatus, String?> {
        return when (targetAction) {
            // 新建分支创建MR, 创建分支版本
            CodeTargetAction.CHECKOUT_BRANCH_AND_REQUEST_MERGE -> {
                Pair(VersionStatus.BRANCH_RELEASE, getReleaseBranchName(pipelineId, draftVersion.version))
            }

            // 提交到源分支,创建分支版本
            CodeTargetAction.COMMIT_TO_SOURCE_BRANCH,
            CodeTargetAction.COMMIT_TO_SOURCE_BRANCH_AND_REQUEST_MERGE -> {
                val baseVersion = draftVersion.baseVersion?.let {
                    pipelineRepositoryService.getPipelineResourceVersion(projectId, pipelineId, it)
                }
                if (baseVersion == null) {
                    Pair(VersionStatus.DRAFT_RELEASE, null)
                } else {
                    Pair(VersionStatus.BRANCH_RELEASE, baseVersion.versionName)
                }
            }

            // 提交到指定分支,需要判断是否是默认分支,如果是默认分支,则发布成正式版本
            CodeTargetAction.COMMIT_TO_BRANCH -> {
                if (targetBranch.isNullOrEmpty()) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_COMMIT_BRANCH_IS_NOT_EMPTY
                    )
                }
                if (repoHashId.isNullOrEmpty()) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_REPO_HASH_ID_IS_NOT_EMPTY
                    )
                }
                val repositoryConfig = RepositoryConfig(
                    repositoryHashId = repoHashId,
                    repositoryName = null,
                    repositoryType = RepositoryType.ID
                )
                val defaultBranch = scmProxyService.getDefaultBranch(
                    projectId = projectId,
                    repositoryConfig = repositoryConfig
                )
                // 提交到默认分支,应该发布成正式版本
                if (defaultBranch == targetBranch) {
                    Pair(VersionStatus.DRAFT_RELEASE, null)
                } else {
                    Pair(VersionStatus.BRANCH_RELEASE, targetBranch)
                }
            }

            else -> {
                Pair(VersionStatus.DRAFT_RELEASE, null)
            }
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
                                            language = I18nUtil.getLanguage(
                                                userId
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
                staticViews = request.staticViews,
                labels = request.labels
            ),
            channelCode = ChannelCode.BS,
            setting = setting,
            checkPermission = true,
            instanceType = request.instanceType,
            versionStatus = VersionStatus.COMMITTING,
            useSubscriptionSettings = request.useSubscriptionSettings,
            useConcurrencyGroup = request.useConcurrencyGroup
        )
    }

    fun getVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        archiveFlag: Boolean? = false
    ): PipelineVersionWithModel {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
            projectId = projectId,
            pipelineId = pipelineId,
            queryDslContext = CommonUtils.getJooqDslContext(archiveFlag, ARCHIVE_SHARDING_DSL_CONTEXT)
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode, errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
        )
        val editPermission = pipelinePermissionService.checkPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.EDIT
        )
        val resource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            includeDraft = true,
            encryptedFlag = !editPermission,
            archiveFlag = archiveFlag
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
            params = arrayOf(version.toString())
        )
        val setting = pipelineSettingFacadeService.userGetSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = resource.settingVersion ?: NUM_ZERO, // 历史没有关联过setting版本应该取正式版本
            archiveFlag = archiveFlag
        )
        // 判断是否需要再重新生成yaml,这行代码需要放在getFixedModel之前,不然getFixedModel会把template字段补充
        val force = resource.version == pipelineInfo.version &&
                resource.model.instanceFromTemplate == true &&
                resource.model.template == null
        val model = pipelineInfoFacadeService.getFixedModel(
            resource = resource,
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            pipelineInfo = pipelineInfo,
            archiveFlag = archiveFlag
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
                version = it,
                archiveFlag = archiveFlag
            )
        }
        val (yamlSupported, yamlPreview, msg) = try {
            val response = transferService.buildPreview(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                resource = resource,
                editPermission = editPermission,
                archiveFlag = archiveFlag,
                force = force
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
        return pipelineVersionManager.deployPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = modelAndYaml.pipelineId,
            request = PipelineDraftSaveReq(modelAndYaml)
        )
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
        description: String? = null,
        buildOnly: Boolean? = false,
        archiveFlag: Boolean? = false
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
                pipelineId = pipelineId,
                archiveFlag = archiveFlag
            )?.toSimple()?.apply {
                baseVersionName = baseVersion?.let {
                    repositoryVersionService.getPipelineVersionSimple(
                        projectId = projectId, pipelineId = pipelineId, version = it, archiveFlag = archiveFlag
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
                version = fromVersion,
                archiveFlag = archiveFlag
            )
        } else null
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
            projectId = projectId,
            pipelineId = pipelineId,
            queryDslContext = CommonUtils.getJooqDslContext(archiveFlag, ARCHIVE_SHARDING_DSL_CONTEXT)
        )
        var (size, pipelines) = repositoryVersionService.listPipelineReleaseVersion(
            pipelineInfo = pipelineInfo,
            projectId = projectId,
            pipelineId = pipelineId,
            creator = creator,
            description = description,
            versionName = versionName,
            excludeVersion = fromVersion,
            offset = offset,
            limit = limit,
            buildOnly = buildOnly,
            archiveFlag = archiveFlag
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
        version: Int,
        archiveFlag: Boolean? = false
    ): PipelineVersionSimple {
        return repositoryVersionService.getPipelineVersionSimple(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            archiveFlag = archiveFlag
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
        val isPipelineInstanceFromTemplate = pipelineTemplateRelatedService.isPipelineInstanceFromTemplate(
            projectId = projectId,
            pipelineId = pipelineId
        )
        // 存量的实例化版本，不支持一键回滚
        if (isPipelineInstanceFromTemplate && targetVersion.model.template == null) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_LEGACY_INSTANCE_CANNOT_ROLLBACK
            )
        }
        // 补全模型信息
        val fixedModel = pipelineInfoFacadeService.getFixedModel(
            resource = targetVersion,
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            pipelineInfo = pipelineInfo
        )
        val resource = pipelineRepositoryService.rollbackDraftFromVersion(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            targetVersion = targetVersion.copy(model = fixedModel)
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

    fun canRollbackFromVersion(
        projectId: String,
        pipelineId: String,
        version: Int
    ): Boolean {
        // 如果没有关联模版,可以回滚
        pipelineTemplateRelatedService.get(
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: return true
        val pipelineResource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            includeDraft = true
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
            params = arrayOf(version.toString())
        )
        // 如果是新版的模版,已经保存模版的信息,可以回滚
        if (pipelineResource.model.template != null) {
            return true
        }
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
            params = arrayOf(pipelineId)
        )
        // 如果是旧版的模版,没有保存模版的信息,需要流水线是最新版本才能回滚
        return pipelineInfo.version == version
    }

    fun deletePipelineVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        checkPermission: Boolean = true
    ): String {
        repositoryVersionService.deletePipelineVersion(
            userId = userId,
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
