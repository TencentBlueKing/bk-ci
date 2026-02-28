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

package com.tencent.devops.process.service.pipeline.version

import com.tencent.devops.common.api.check.Preconditions
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType
import com.tencent.devops.common.pipeline.pojo.transfer.TransferBody
import com.tencent.devops.common.pipeline.pojo.transfer.YamlWithVersion
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.utils.TemplateInstanceUtil
import com.tencent.devops.process.pojo.pipeline.PipelineResourceOnlyVersion
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.pipeline.PrefetchReleaseResult
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstancesRequest
import com.tencent.devops.process.service.StageTagService
import com.tencent.devops.process.service.pipeline.PipelineSettingVersionService
import com.tencent.devops.process.service.pipeline.PipelineTransferYamlService
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.utils.PipelineVersionUtils
import com.tencent.devops.process.yaml.transfer.aspect.IPipelineTransferAspect
import com.tencent.devops.repository.api.scm.ServiceScmRepositoryApiResource
import com.tencent.devops.scm.api.pojo.repository.git.GitScmServerRepository
import jakarta.ws.rs.core.Response
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.LinkedList

/**
 * 流水线版本生成器
 */
@Service
class PipelineVersionGenerator constructor(
    private val dslContext: DSLContext,
    private val pipelineResourceVersionDao: PipelineResourceVersionDao,
    private val pipelineResourceDao: PipelineResourceDao,
    private val pipelineSettingVersionService: PipelineSettingVersionService,
    private val client: Client,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val stageTagService: StageTagService,
    private val transferService: PipelineTransferYamlService,
    private val pipelineRepositoryService: PipelineRepositoryService
) {

    /**
     * 生成流水线默认版本
     */
    fun getDefaultVersion(
        versionStatus: VersionStatus,
        branchName: String? = null
    ): PipelineResourceOnlyVersion {
        return when (versionStatus) {
            VersionStatus.COMMITTING -> {
                PipelineResourceOnlyVersion(
                    version = INIT_VERSION,
                    settingVersion = INIT_VERSION
                )
            }

            VersionStatus.BRANCH -> {
                PipelineResourceOnlyVersion(
                    version = INIT_VERSION,
                    settingVersion = INIT_VERSION,
                    versionName = branchName
                )
            }

            else -> {
                val versionName = PipelineVersionUtils.getVersionName(
                    versionNum = INIT_VERSION,
                    pipelineVersion = INIT_VERSION,
                    triggerVersion = INIT_VERSION,
                    settingVersion = INIT_VERSION
                )
                PipelineResourceOnlyVersion(
                    version = INIT_VERSION,
                    versionName = versionName,
                    versionNum = INIT_VERSION,
                    pipelineVersion = INIT_VERSION,
                    triggerVersion = INIT_VERSION,
                    settingVersion = INIT_VERSION
                )
            }
        }
    }

    /**
     * 生成草稿版本
     */
    fun generateDraftVersion(
        projectId: String,
        pipelineId: String
    ): PipelineResourceOnlyVersion {
        val releaseResource = pipelineResourceDao.getReleaseVersionResource(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
            params = arrayOf(pipelineId)
        )
        // PAC之前的历史流水线,T_PIPELINE_RESOURCE_VERSION和T_PIPELINE_SETTING_VERSION可能没有数据
        val latestResource = pipelineResourceVersionDao.getLatestVersionResource(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        val latestSetting = pipelineSettingVersionService.getLatestSettingVersion(
            context = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        return PipelineResourceOnlyVersion(
            version = (latestResource?.version ?: releaseResource.version) + 1,
            settingVersion = latestSetting?.let { it.version + 1 } ?: 1,
            baseVersion = releaseResource.version,
            baseVersionName = releaseResource.versionName,
            releaseVersion = releaseResource.version,
            releaseVersionName = releaseResource.versionName
        )
    }

    /**
     * 生成分支版本
     */
    fun generateBranchVersion(
        projectId: String,
        pipelineId: String,
        branchName: String,
        draftResource: PipelineResourceVersion? = null,
    ): PipelineResourceOnlyVersion {
        val releaseResource = pipelineResourceDao.getReleaseVersionResource(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
            params = arrayOf(pipelineId)
        )
        // PAC之前的历史流水线,T_PIPELINE_RESOURCE_VERSION和T_PIPELINE_SETTING_VERSION可能没有数据
        val latestResource = pipelineResourceVersionDao.getLatestVersionResource(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        val latestSetting = pipelineSettingVersionService.getLatestSettingVersion(
            context = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        // 分支版本的基准版本,如果当前分支有分支版本,则取当前分支,否则取最新版本
        val branchResource = pipelineResourceVersionDao.getBranchVersionResource(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            branchName = branchName
        )
        val (version, settingVersion) = if (draftResource == null) {
            Pair(
                (latestResource?.version ?: releaseResource.version) + 1,
                latestSetting?.let { it.version + 1 } ?: 1
            )
        } else {
            Pair(draftResource.version, draftResource.settingVersion)
        }
        return PipelineResourceOnlyVersion(
            version = version,
            settingVersion = settingVersion,
            baseVersion = branchResource?.version ?: releaseResource.version,
            baseVersionName = branchResource?.versionName ?: releaseResource.versionName,
            versionName = branchName,
            releaseVersion = releaseResource.version,
            releaseVersionName = releaseResource.versionName
        )
    }

    /**
     * 生成正式版本
     *
     * @param draftResource 正式版本由草稿发布,否则直接创建的正式版本
     * @param newModel 新版编排
     */
    fun generateReleaseVersion(
        projectId: String,
        pipelineId: String,
        draftResource: PipelineResourceVersion? = null,
        newModel: Model
    ): PipelineResourceOnlyVersion {
        val releaseResource = pipelineResourceDao.getReleaseVersionResource(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
            params = arrayOf(pipelineId)
        )
        // PAC之前的历史流水线,T_PIPELINE_RESOURCE_VERSION和T_PIPELINE_SETTING_VERSION可能没有数据
        val latestResource = pipelineResourceVersionDao.getLatestVersionResource(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        val latestSetting = pipelineSettingVersionService.getLatestSettingVersion(
            context = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        // 这个正式版本需要从T_PIPELINE_RESOURCE_VERSION中获取,
        // 不能从T_PIPELINE_RESOURCE中获取,因为T_PIPELINE_RESOURCE中可能是草稿或分支版本
        val latestReleaseResource = pipelineResourceVersionDao.getReleaseVersionRecord(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        val (version, settingVersion) = if (draftResource == null) {
            Pair(
                (latestResource?.version ?: releaseResource.version) + 1,
                latestSetting?.let { it.version + 1 } ?: 1
            )
        } else {
            Pair(draftResource.version, draftResource.settingVersion)
        }
        // 如果没有正式版本,说明是第一次生成正式版本
        return if (latestReleaseResource == null) {
            val versionNum = INIT_VERSION
            val pipelineVersion = INIT_VERSION
            val triggerVersion = INIT_VERSION

            val versionName = PipelineVersionUtils.getVersionName(
                versionNum = versionNum,
                pipelineVersion = pipelineVersion,
                triggerVersion = triggerVersion,
                settingVersion = settingVersion
            )
            PipelineResourceOnlyVersion(
                version = version,
                versionName = versionName,
                versionNum = versionNum,
                pipelineVersion = pipelineVersion,
                triggerVersion = triggerVersion,
                settingVersion = settingVersion,
                releaseVersion = version,
                releaseVersionName = versionName
            )
        } else {
            val versionNum = latestReleaseResource.versionNum?.let { it + 1 } ?: INIT_VERSION
            val pipelineVersion = PipelineVersionUtils.getPipelineVersion(
                currVersion = latestReleaseResource.pipelineVersion ?: latestReleaseResource.version,
                originModel = latestReleaseResource.model,
                newModel = newModel
            )
            val triggerVersion = PipelineVersionUtils.getTriggerVersion(
                currVersion = latestReleaseResource.triggerVersion ?: 0,
                originModel = latestReleaseResource.model,
                newModel = newModel
            ).coerceAtLeast(1)
            val versionName = PipelineVersionUtils.getVersionName(
                versionNum = versionNum,
                pipelineVersion = pipelineVersion,
                triggerVersion = triggerVersion,
                settingVersion = settingVersion
            )
            PipelineResourceOnlyVersion(
                version = version,
                versionName = versionName,
                versionNum = versionNum,
                pipelineVersion = pipelineVersion,
                triggerVersion = triggerVersion,
                settingVersion = settingVersion,
                baseVersion = latestReleaseResource.version,
                baseVersionName = latestReleaseResource.versionName,
                releaseVersion = version,
                releaseVersionName = versionName
            )
        }
    }

    /**
     * 生成模版实例化版本
     *
     */
    fun generateInstanceVersion(
        projectId: String,
        pipelineId: String,
        newModel: Model,
        enablePac: Boolean,
        repoHashId: String?,
        targetAction: CodeTargetAction?,
        targetBranch: String? = null,
        defaultBranch: String? = null,
        templateId: String,
        templateVersion: Long
    ): PipelineResourceOnlyVersion {
        return if (enablePac) {
            if (repoHashId.isNullOrEmpty()) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("repoHashId")
                )
            }
            val checkoutBranch = "$PAC_TEMPLATE_INSTANCE_BRANCH_PREFIX$templateId-$templateVersion"
            generateVersionWithPac(
                projectId = projectId,
                pipelineId = pipelineId,
                newModel = newModel,
                repoHashId = repoHashId,
                targetAction = targetAction,
                checkoutBranch = checkoutBranch,
                targetBranch = targetBranch,
                defaultBranch = defaultBranch
            )
        } else {
            val resourceOnlyVersion = generateReleaseVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                newModel = newModel
            )
            resourceOnlyVersion
        }
    }

    fun generateDraftReleaseVersion(
        projectId: String,
        pipelineId: String,
        draftResource: PipelineResourceVersion,
        enablePac: Boolean,
        repoHashId: String?,
        targetAction: CodeTargetAction?,
        targetBranch: String? = null
    ): PipelineResourceOnlyVersion {
        return if (enablePac) {
            if (repoHashId.isNullOrEmpty()) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("repoHashId")
                )
            }
            val checkoutBranch = "$PAC_BRANCH_PREFIX$pipelineId-${draftResource.version}"
            generateVersionWithPac(
                projectId = projectId,
                pipelineId = pipelineId,
                draftResource = draftResource,
                newModel = draftResource.model,
                repoHashId = repoHashId,
                targetAction = targetAction,
                checkoutBranch = checkoutBranch,
                baseVersion = draftResource.baseVersion,
                targetBranch = targetBranch
            )
        } else {
            generateReleaseVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                draftResource = draftResource,
                newModel = draftResource.model
            )
        }
    }

    /**
     * 生成开启PAC实例化版本
     *
     * @param checkoutBranch 当targetAction==CHECKOUT_BRANCH_AND_REQUEST_MERGE,则需要传入checkoutBranch,新建分支名
     * @param baseVersion 当targetAction==COMMIT_TO_SOURCE_BRANCH_AND_REQUEST_MERGE
     *  或COMMIT_TO_SOURCE_BRANCH_AND_REQUEST_MERGE,则需要传入baseVersion,基准版本号
     * @param targetBranch 当targetAction==COMMIT_TO_TARGET_BRANCH,则需要传入targetBranch,表示指定的分支
     * @param defaultBranch 默认分支
     */
    fun generateVersionWithPac(
        projectId: String,
        pipelineId: String,
        draftResource: PipelineResourceVersion? = null,
        newModel: Model,
        repoHashId: String,
        targetAction: CodeTargetAction?,
        checkoutBranch: String? = null,
        baseVersion: Int? = null,
        targetBranch: String? = null,
        defaultBranch: String? = null
    ): PipelineResourceOnlyVersion {
        return when (targetAction) {
            CodeTargetAction.COMMIT_TO_MASTER -> {
                generateReleaseVersion(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    draftResource = draftResource,
                    newModel = newModel
                )
            }

            CodeTargetAction.CHECKOUT_BRANCH_AND_REQUEST_MERGE -> {
                if (checkoutBranch.isNullOrEmpty()) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("checkoutBranch")
                    )
                }
                generateBranchVersion(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    branchName = checkoutBranch,
                    draftResource = draftResource
                )
            }

            CodeTargetAction.COMMIT_TO_SOURCE_BRANCH,
            CodeTargetAction.COMMIT_TO_SOURCE_BRANCH_AND_REQUEST_MERGE -> {
                if (baseVersion == null) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("baseBranch")
                    )
                }
                val pipelineVersionSimple = pipelineResourceVersionDao.getPipelineVersionSimple(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = baseVersion
                ) ?: throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_BASE_VERSION_NOT_FOUND,
                    params = arrayOf(baseVersion.toString())
                )
                generateBranchVersion(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    branchName = pipelineVersionSimple.versionName,
                    draftResource = draftResource
                )
            }

            CodeTargetAction.COMMIT_TO_BRANCH -> {
                if (targetBranch == null) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("targetBranch")
                    )
                }
                val finalDefaultBranch = defaultBranch ?: getDefaultBranch(
                    projectId = projectId, repoHashId = repoHashId
                )
                // 如果选择的是默认分支,则应该发布正式版本
                if (targetBranch == finalDefaultBranch) {
                    generateReleaseVersion(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        newModel = newModel,
                        draftResource = draftResource
                    )
                } else {
                    generateBranchVersion(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        branchName = targetBranch,
                        draftResource = draftResource
                    )
                }
            }

            else -> {
                throw IllegalArgumentException("targetAction is illegal")
            }
        }
    }

    /**
     * 获取模版实例化版本状态和分支名
     */
    fun getInstanceStatusAndBranchName(
        projectId: String,
        pipelineId: String?,
        templateId: String,
        templateVersion: Long,
        enablePac: Boolean,
        repoHashId: String?,
        targetAction: CodeTargetAction?,
        targetBranch: String? = null,
        defaultBranch: String? = null
    ): Pair<VersionStatus, String?> {
        return if (enablePac) {
            val checkoutBranch = "$PAC_TEMPLATE_INSTANCE_BRANCH_PREFIX$templateId-$templateVersion"
            getStatusAndBranchNameWithPac(
                projectId = projectId,
                pipelineId = pipelineId,
                repoHashId = repoHashId,
                targetAction = targetAction,
                checkoutBranch = checkoutBranch,
                targetBranch = targetBranch,
                defaultBranch = defaultBranch
            )
        } else {
            Pair(VersionStatus.RELEASED, null)
        }
    }

    /**
     * 获取草稿发版流水线版本状态和分支名
     */
    fun getDraftReleaseStatusAndBranchName(
        projectId: String,
        pipelineId: String,
        version: Int,
        enablePac: Boolean,
        repoHashId: String?,
        targetAction: CodeTargetAction?,
        baseVersion: Int? = null,
        targetBranch: String? = null,
    ): Pair<VersionStatus, String?> {
        return if (enablePac) {
            val checkoutBranch = "$PAC_BRANCH_PREFIX$pipelineId-$version"
            getStatusAndBranchNameWithPac(
                projectId = projectId,
                pipelineId = pipelineId,
                repoHashId = repoHashId,
                targetAction = targetAction,
                checkoutBranch = checkoutBranch,
                baseVersion = baseVersion,
                targetBranch = targetBranch,
            )
        } else {
            Pair(VersionStatus.RELEASED, null)
        }
    }

    /**
     * 获取PAC实例化版本状态和分支名
     */
    private fun getStatusAndBranchNameWithPac(
        projectId: String,
        pipelineId: String?,
        repoHashId: String?,
        targetAction: CodeTargetAction?,
        checkoutBranch: String? = null,
        baseVersion: Int? = null,
        targetBranch: String? = null,
        defaultBranch: String? = null,
    ): Pair<VersionStatus, String?> {
        if (repoHashId.isNullOrBlank()) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                params = arrayOf("repoHashId")
            )
        }
        return when (targetAction) {

            CodeTargetAction.COMMIT_TO_MASTER -> {
                Pair(VersionStatus.RELEASED, null)
            }

            CodeTargetAction.CHECKOUT_BRANCH_AND_REQUEST_MERGE -> {
                if (checkoutBranch.isNullOrEmpty()) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("checkoutBranch")
                    )
                }
                Pair(VersionStatus.BRANCH, checkoutBranch)
            }

            CodeTargetAction.COMMIT_TO_SOURCE_BRANCH,
            CodeTargetAction.COMMIT_TO_SOURCE_BRANCH_AND_REQUEST_MERGE -> {
                if (pipelineId.isNullOrEmpty()) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("pipelineId")
                    )
                }
                if (baseVersion == null) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("baseBranch")
                    )
                }
                val pipelineVersionSimple = pipelineResourceVersionDao.getPipelineVersionSimple(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = baseVersion
                ) ?: throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_BASE_VERSION_NOT_FOUND,
                    params = arrayOf(baseVersion.toString())
                )
                Pair(VersionStatus.BRANCH, pipelineVersionSimple.versionName)
            }

            CodeTargetAction.COMMIT_TO_BRANCH -> {
                val finalDefaultBranch =
                    defaultBranch ?: getDefaultBranch(projectId = projectId, repoHashId = repoHashId)
                if (defaultBranch == finalDefaultBranch) {
                    Pair(VersionStatus.RELEASED, null)
                } else {
                    Pair(VersionStatus.BRANCH, targetBranch)
                }
            }

            else -> {
                throw IllegalArgumentException("targetAction is illegal")
            }
        }
    }

    fun batchPreFetchInstanceVersion(
        projectId: String,
        templateId: String,
        version: Long,
        request: PipelineTemplateInstancesRequest
    ): List<PrefetchReleaseResult> {
        val templateResource = pipelineTemplateResourceService.get(
            projectId = projectId,
            templateId = templateId,
            version = version
        )
        with(request) {
            val defaultBranch = targetAction?.takeIf { enablePac && it == CodeTargetAction.COMMIT_TO_BRANCH }?.let {
                getDefaultBranch(projectId = projectId, repoHashId = repoHashId)
            }
            val defaultStageTagId = stageTagService.getDefaultStageTag().data?.id
            val pipelineId2Name = pipelineRepositoryService.listPipelineNameByIds(
                projectId = projectId,
                pipelineIds = instanceReleaseInfos.map { it.pipelineId }.toSet()
            )
            return instanceReleaseInfos.map { releaseInfo ->
                // 新增实例化
                val pipelineId = releaseInfo.pipelineId
                val resourceOnlyVersion = if (releaseInfo.pipelineId.isEmpty() || pipelineId2Name[pipelineId] == null) {
                    val (versionStatus, branchName) = getInstanceStatusAndBranchName(
                        projectId = projectId,
                        pipelineId = null,
                        templateId = templateId,
                        templateVersion = version,
                        enablePac = enablePac,
                        repoHashId = repoHashId,
                        targetAction = targetAction,
                        targetBranch = targetBranch,
                        defaultBranch = defaultBranch
                    )
                    getDefaultVersion(
                        versionStatus = versionStatus,
                        branchName = branchName,
                    )
                } else {
                    val instanceModel = TemplateInstanceUtil.instanceModel(
                        templateResource = templateResource,
                        pipelineName = releaseInfo.pipelineName,
                        defaultStageTagId = defaultStageTagId,
                        buildNo = releaseInfo.buildNo,
                        params = releaseInfo.param ?: emptyList(),
                        triggerConfigs = releaseInfo.triggerConfigs,
                        overrideTemplateField = releaseInfo.overrideTemplateField
                    )
                    generateInstanceVersion(
                        projectId = projectId,
                        pipelineId = releaseInfo.pipelineId,
                        newModel = instanceModel,
                        enablePac = enablePac,
                        repoHashId = repoHashId,
                        targetAction = targetAction,
                        targetBranch = targetBranch,
                        defaultBranch = defaultBranch,
                        templateId = templateId,
                        templateVersion = version
                    )
                }
                PrefetchReleaseResult(
                    pipelineId = releaseInfo.pipelineId,
                    pipelineName = releaseInfo.pipelineName,
                    version = resourceOnlyVersion.version,
                    newVersionNum = resourceOnlyVersion.versionNum ?: INIT_VERSION,
                    newVersionName = resourceOnlyVersion.versionName!!
                )

            }
        }
    }

    private fun getDefaultBranch(
        projectId: String,
        repoHashId: String?
    ): String? {
        if (repoHashId.isNullOrBlank()) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                params = arrayOf("repoHashId")
            )
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
        return serverRepository.defaultBranch
    }

    fun yaml2model(
        userId: String,
        projectId: String,
        pipelineId: String?,
        yaml: String,
        yamlFileName: String? = null,
        aspects: LinkedList<IPipelineTransferAspect>? = null
    ): Pair<PipelineModelAndSetting, YamlWithVersion?> {
        return try {
            val result = transferService.transfer(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                actionType = TransferActionType.FULL_YAML2MODEL,
                data = TransferBody(oldYaml = yaml, yamlFileName = yamlFileName),
                aspects = aspects ?: LinkedList()
            )
            if (result.modelAndSetting == null) {
                logger.warn("TRANSFER_YAML|$projectId|$userId|yml=\n$yaml")
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_OCCURRED_IN_TRANSFER
                )
            }
            Pair(result.modelAndSetting!!, result.yamlWithVersion)
        } catch (ignore: Throwable) {
            if (ignore is ErrorCodeException) throw ignore
            logger.warn("TRANSFER_YAML|$projectId|$userId|yml=\n$yaml", ignore)
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_OCCURRED_IN_TRANSFER
            )
        }
    }

    fun model2yaml(
        userId: String,
        projectId: String,
        pipelineId: String?,
        modelAndSetting: PipelineModelAndSetting,
        oldYaml: String?,
        aspects: LinkedList<IPipelineTransferAspect> = LinkedList()
    ): YamlWithVersion? {
        // MODEL形式的保存需要兼容旧数据
        Preconditions.checkNotNull(
            modelAndSetting.model, "model must not be null"
        )
        Preconditions.checkNotNull(
            modelAndSetting.setting, "setting must not be null"
        )
        return try {
            val result = transferService.transfer(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                actionType = TransferActionType.FULL_MODEL2YAML,
                data = TransferBody(
                    modelAndSetting = modelAndSetting,
                    oldYaml = oldYaml ?: "",
                ),
                aspects = aspects
            )
            result.yamlWithVersion
        } catch (ignore: Throwable) {
            // 旧流水线可能无法转换，用空YAML代替
            logger.warn("TRANSFER_YAML|$projectId|$userId", ignore)
            null
        }
    }

    companion object {
        const val INIT_VERSION = 1
        private const val PAC_TEMPLATE_INSTANCE_BRANCH_PREFIX = "bk-ci-template-instance-"
        private const val PAC_BRANCH_PREFIX = "bk-ci-pipeline-"
        private val logger = LoggerFactory.getLogger(PipelineVersionGenerator::class.java)
    }
}
