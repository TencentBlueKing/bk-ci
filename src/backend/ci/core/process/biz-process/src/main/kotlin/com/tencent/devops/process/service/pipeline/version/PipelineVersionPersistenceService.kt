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

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.dao.PipelineSettingVersionDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.process.enums.OperationLogType
import com.tencent.devops.process.pojo.pipeline.PipelineBasicInfo
import com.tencent.devops.process.pojo.pipeline.PipelineModelBasicInfo
import com.tencent.devops.process.pojo.pipeline.PipelineResourceOnlyVersion
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileReleaseReq
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileReleaseReqSource
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileReleaseResult
import com.tencent.devops.process.service.pipeline.version.processor.PipelineVersionCreatePostProcessor
import com.tencent.devops.process.yaml.PipelineYamlFacadeService
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

/**
 * 负责流水线版本持久化业务逻辑
 */
@Service
class PipelineVersionPersistenceService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineResourceDao: PipelineResourceDao,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelineResourceVersionDao: PipelineResourceVersionDao,
    private val pipelineSettingVersionDao: PipelineSettingVersionDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    @Lazy
    private val pipelineYamlFacadeService: PipelineYamlFacadeService,
    private val versionCreatePostProcessors: List<PipelineVersionCreatePostProcessor>
) {

    fun initializePipeline(
        context: PipelineVersionCreateContext,
        resourceOnlyVersion: PipelineResourceOnlyVersion
    ) {
        with(context) {
            operationLogType = OperationLogType.fetchType(pipelineResourceWithoutVersion.status)
            operationLogParams = resourceOnlyVersion.versionName ?: ""

            pipelineResourceWithoutVersion.model.latestVersion = resourceOnlyVersion.version
            val pipelineResourceVersion = PipelineResourceVersion(
                pipelineResourceWithoutVersion = pipelineResourceWithoutVersion,
                pipelineResourceOnlyVersion = resourceOnlyVersion
            )
            val pipelineSetting = pipelineSettingWithoutVersion.copy(
                version = resourceOnlyVersion.settingVersion!!
            )
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                createPipelineInfo(
                    transactionContext = transactionContext,
                    userId = userId,
                    pipelineBasicInfo = pipelineBasicInfo,
                    pipelineModelBasicInfo = pipelineModelBasicInfo,
                    version = pipelineResourceVersion.version,
                    latestVersionStatus = pipelineResourceVersion.status
                )
                createPipelineResource(
                    transactionContext = transactionContext,
                    userId = userId,
                    pipelineResourceVersion = pipelineResourceVersion
                )
                createPipelineResourceVersion(
                    transactionContext = transactionContext,
                    userId = userId,
                    pipelineResourceVersion = pipelineResourceVersion
                )
                pipelineSettingDao.saveSetting(
                    dslContext = transactionContext,
                    setting = pipelineSetting
                )
                createPipelineSettingVersion(
                    transactionContext = transactionContext,
                    pipelineSetting = pipelineSetting
                )

                pipelineBuildSummaryDao.create(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildNo = pipelineModelBasicInfo.buildNo
                )
                postProcessInTransactionVersionCreate(
                    transactionContext = transactionContext,
                    context = context,
                    pipelineResourceVersion = pipelineResourceVersion,
                    pipelineSetting = pipelineSetting
                )
            }
            postProcessAfterVersionCreate(
                context = context,
                pipelineResourceVersion = pipelineResourceVersion,
                pipelineSetting = pipelineSetting
            )
        }
    }

    fun createReleaseVersion(
        context: PipelineVersionCreateContext,
        resourceOnlyVersion: PipelineResourceOnlyVersion
    ) {
        with(context) {
            val watcher = Watcher("createReleaseVersion|$projectId|$pipelineId")
            try {
                operationLogType = OperationLogType.RELEASE_MASTER_VERSION
                operationLogParams = resourceOnlyVersion.versionName!!

                pipelineResourceWithoutVersion.model.latestVersion = resourceOnlyVersion.version
                val pipelineResourceVersion = PipelineResourceVersion(
                    pipelineResourceWithoutVersion = pipelineResourceWithoutVersion,
                    pipelineResourceOnlyVersion = resourceOnlyVersion
                )
                val pipelineSetting = pipelineSettingWithoutVersion.copy(
                    version = resourceOnlyVersion.settingVersion!!
                )
                watcher.start("postProcessBeforeVersionCreate")
                postProcessBeforeVersionCreate(
                    context = context,
                    pipelineResourceVersion = pipelineResourceVersion,
                    pipelineSetting = pipelineSetting
                )
                watcher.start("transaction")
                dslContext.transaction { configuration ->
                    val transactionContext = DSL.using(configuration)
                    updatePipelineInfo(
                        transactionContext = transactionContext,
                        userId = userId,
                        pipelineBasicInfo = pipelineBasicInfo,
                        pipelineModelBasicInfo = pipelineModelBasicInfo,
                        version = pipelineResourceVersion.version,
                        latestVersionStatus = pipelineResourceVersion.status,
                    )
                    updatePipelineResource(
                        transactionContext = transactionContext,
                        pipelineResourceVersion = pipelineResourceVersion
                    )
                    createPipelineResourceVersion(
                        transactionContext = transactionContext,
                        userId = userId,
                        pipelineResourceVersion = pipelineResourceVersion
                    )
                    pipelineSettingDao.saveSetting(
                        dslContext = transactionContext,
                        setting = pipelineSetting
                    )
                    createPipelineSettingVersion(
                        transactionContext = transactionContext,
                        pipelineSetting = pipelineSetting
                    )
                    context.pipelineModelBasicInfo.buildNo?.let {
                        if (resetBuildNo == true) {
                            logger.info("reset build no|$projectId|$pipelineId|${it.buildNo}")
                            pipelineBuildSummaryDao.updateBuildNo(
                                dslContext = dslContext,
                                projectId = projectId,
                                pipelineId = pipelineId,
                                buildNo = it.buildNo,
                                debug = false
                            )
                        }
                    }
                    postProcessInTransactionVersionCreate(
                        transactionContext = transactionContext,
                        context = context,
                        pipelineResourceVersion = pipelineResourceVersion,
                        pipelineSetting = pipelineSetting
                    )
                }
                watcher.start("postProcessAfterVersionCreate")
                postProcessAfterVersionCreate(
                    context = context,
                    pipelineResourceVersion = pipelineResourceVersion,
                    pipelineSetting = pipelineSetting
                )
            } finally {
                watcher.stop()
                LogUtils.printCostTimeWE(watcher = watcher)
            }
        }
    }

    fun createDraftVersion(
        context: PipelineVersionCreateContext,
        resourceOnlyVersion: PipelineResourceOnlyVersion
    ) {
        with(context) {
            operationLogType = OperationLogType.CREATE_DRAFT_VERSION
            operationLogParams = resourceOnlyVersion.baseVersionName ?: ""

            pipelineResourceWithoutVersion.model.latestVersion = resourceOnlyVersion.version
            val pipelineResourceVersion = PipelineResourceVersion(
                pipelineResourceWithoutVersion = pipelineResourceWithoutVersion,
                pipelineResourceOnlyVersion = resourceOnlyVersion
            )
            val pipelineSetting = pipelineSettingWithoutVersion.copy(
                version = resourceOnlyVersion.settingVersion!!
            )
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                createPipelineResourceVersion(
                    transactionContext = transactionContext,
                    userId = userId,
                    pipelineResourceVersion = pipelineResourceVersion
                )
                createPipelineSettingVersion(
                    transactionContext = transactionContext,
                    pipelineSetting = pipelineSetting
                )
            }
        }
    }

    fun updateDraftVersion(
        context: PipelineVersionCreateContext,
        resourceOnlyVersion: PipelineResourceOnlyVersion
    ) {
        with(context) {
            operationLogType = OperationLogType.UPDATE_DRAFT_VERSION

            pipelineResourceWithoutVersion.model.latestVersion = resourceOnlyVersion.version
            val pipelineResourceVersion = PipelineResourceVersion(
                pipelineResourceWithoutVersion = pipelineResourceWithoutVersion,
                pipelineResourceOnlyVersion = resourceOnlyVersion
            )
            val pipelineSetting = pipelineSettingWithoutVersion.copy(
                version = resourceOnlyVersion.settingVersion!!
            )
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                createPipelineResourceVersion(
                    transactionContext = transactionContext,
                    userId = userId,
                    pipelineResourceVersion = pipelineResourceVersion
                )
                pipelineSettingVersionDao.update(
                    dslContext = transactionContext,
                    setting = pipelineSetting
                )
            }
        }
    }

    fun createBranchVersion(
        context: PipelineVersionCreateContext,
        resourceOnlyVersion: PipelineResourceOnlyVersion
    ) {
        with(context) {
            pipelineResourceWithoutVersion.model.latestVersion = resourceOnlyVersion.version
            val pipelineResourceVersion = PipelineResourceVersion(
                pipelineResourceWithoutVersion = pipelineResourceWithoutVersion,
                pipelineResourceOnlyVersion = resourceOnlyVersion
            )
            val pipelineSetting = pipelineSettingWithoutVersion.copy(
                version = resourceOnlyVersion.settingVersion!!
            )
            val branchResource = pipelineResourceVersionDao.getBranchVersionResource(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                branchName = branchName
            )
            val latestReleaseResource = pipelineResourceDao.getReleaseVersionResource(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId
            )
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                if (branchResource != null) {
                    pipelineResourceVersionDao.updateBranchVersion(
                        dslContext = transactionContext,
                        userId = userId,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        branchName = branchName,
                        branchVersionAction = BranchVersionAction.INACTIVE
                    )
                    if (latestReleaseResource != null && latestReleaseResource.version == branchResource.version) {
                        updatePipelineResource(
                            transactionContext = transactionContext,
                            pipelineResourceVersion = pipelineResourceVersion
                        )
                    }
                    operationLogType = OperationLogType.UPDATE_BRANCH_VERSION
                    operationLogParams = resourceOnlyVersion.versionName ?: ""
                } else {
                    operationLogType = OperationLogType.CREATE_BRANCH_VERSION
                    operationLogParams = resourceOnlyVersion.versionName ?: ""
                }
                createPipelineResourceVersion(
                    transactionContext = transactionContext,
                    userId = userId,
                    pipelineResourceVersion = pipelineResourceVersion
                )
                createPipelineSettingVersion(
                    transactionContext = transactionContext,
                    pipelineSetting = pipelineSetting
                )
                postProcessInTransactionVersionCreate(
                    transactionContext = transactionContext,
                    context = context,
                    pipelineResourceVersion = pipelineResourceVersion,
                    pipelineSetting = pipelineSetting
                )
            }
            postProcessAfterVersionCreate(
                context = context,
                pipelineResourceVersion = pipelineResourceVersion,
                pipelineSetting = pipelineSetting
            )
        }
    }

    /**
     * 发布草稿到正式版本
     */
    fun releaseDraft2ReleaseVersion(
        context: PipelineVersionCreateContext,
        resourceOnlyVersion: PipelineResourceOnlyVersion
    ) {
        with(context) {
            operationLogType = OperationLogType.RELEASE_MASTER_VERSION
            operationLogParams = resourceOnlyVersion.versionName!!

            pipelineResourceWithoutVersion.model.latestVersion = resourceOnlyVersion.version
            val pipelineResourceVersion = PipelineResourceVersion(
                pipelineResourceWithoutVersion = pipelineResourceWithoutVersion,
                pipelineResourceOnlyVersion = resourceOnlyVersion
            )
            val pipelineSetting = pipelineSettingWithoutVersion.copy(
                version = resourceOnlyVersion.settingVersion!!
            )
            postProcessBeforeVersionCreate(
                context = context,
                pipelineResourceVersion = pipelineResourceVersion,
                pipelineSetting = pipelineSetting
            )
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                updatePipelineInfo(
                    transactionContext = transactionContext,
                    userId = userId,
                    pipelineBasicInfo = pipelineBasicInfo,
                    pipelineModelBasicInfo = pipelineModelBasicInfo,
                    version = pipelineResourceVersion.version,
                    latestVersionStatus = pipelineResourceVersion.status,
                )
                updatePipelineResource(
                    transactionContext = transactionContext,
                    pipelineResourceVersion = pipelineResourceVersion
                )
                // 将草稿版本转换成正式版本
                createPipelineResourceVersion(
                    transactionContext = transactionContext,
                    userId = userId,
                    pipelineResourceVersion = pipelineResourceVersion
                )
                pipelineSettingDao.saveSetting(
                    dslContext = transactionContext,
                    setting = pipelineSetting
                )
                postProcessInTransactionVersionCreate(
                    transactionContext = transactionContext,
                    context = context,
                    pipelineResourceVersion = pipelineResourceVersion,
                    pipelineSetting = pipelineSetting
                )
            }
            postProcessAfterVersionCreate(
                context = context,
                pipelineResourceVersion = pipelineResourceVersion,
                pipelineSetting = pipelineSetting
            )
        }
    }

    /**
     * 发布草稿到分支版本
     */
    fun releaseDraft2BranchVersion(
        context: PipelineVersionCreateContext,
        resourceOnlyVersion: PipelineResourceOnlyVersion
    ) {
        with(context) {
            pipelineResourceWithoutVersion.model.latestVersion = resourceOnlyVersion.version
            val pipelineResourceVersion = PipelineResourceVersion(
                pipelineResourceWithoutVersion = pipelineResourceWithoutVersion,
                pipelineResourceOnlyVersion = resourceOnlyVersion
            )
            val pipelineSetting = pipelineSettingWithoutVersion.copy(
                version = resourceOnlyVersion.settingVersion!!
            )
            val pipelineInfo = pipelineInfoDao.getPipelineInfo(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId
            )
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                // 分支版本需要将同分支版本置为无效
                val cnt = pipelineResourceVersionDao.updateBranchVersion(
                    dslContext = transactionContext,
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    branchName = branchName,
                    branchVersionAction = BranchVersionAction.INACTIVE
                )
                if (cnt > 0) {
                    operationLogType = OperationLogType.UPDATE_BRANCH_VERSION
                    operationLogParams = resourceOnlyVersion.versionName ?: ""
                } else {
                    operationLogType = OperationLogType.CREATE_BRANCH_VERSION
                    operationLogParams = resourceOnlyVersion.versionName ?: ""
                }
                // 将草稿版本转换成分支版本
                createPipelineResourceVersion(
                    transactionContext = transactionContext,
                    userId = userId,
                    pipelineResourceVersion = pipelineResourceVersion
                )
                // 将草稿版本发布成分支版本,需要把最新状态转换成分支版本,只有当流水线创建时时分支版本才会出现这种情况
                if (pipelineInfo != null &&
                    pipelineInfo.version == resourceOnlyVersion.version &&
                    pipelineInfo.latestVersionStatus == VersionStatus.COMMITTING.name
                ) {
                    pipelineInfoDao.update(
                        dslContext = transactionContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        userId = userId,
                        // 进行过至少一次发布版本后，取消仅有草稿/分支的状态
                        latestVersionStatus = VersionStatus.BRANCH
                    )
                }
                postProcessInTransactionVersionCreate(
                    transactionContext = transactionContext,
                    context = context,
                    pipelineResourceVersion = pipelineResourceVersion,
                    pipelineSetting = pipelineSetting
                )
            }
            postProcessAfterVersionCreate(
                context = context,
                pipelineResourceVersion = pipelineResourceVersion,
                pipelineSetting = pipelineSetting
            )
        }
    }

    /**
     * 发布yaml文件
     */
    fun releaseYamlFile(
        context: PipelineVersionCreateContext,
        resourceOnlyVersion: PipelineResourceOnlyVersion
    ): PipelineYamlFileReleaseResult {
        with(context) {
            val yamlFileReleaseReq = PipelineYamlFileReleaseReq(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = pipelineBasicInfo.pipelineName,
                version = resourceOnlyVersion.version,
                versionName = resourceOnlyVersion.versionName,
                repoHashId = yamlFileInfo!!.repoHashId,
                filePath = yamlFileInfo.filePath,
                content = pipelineResourceWithoutVersion.yaml!!,
                commitMessage = pipelineResourceWithoutVersion.description
                    ?: "update template ${pipelineBasicInfo.pipelineName}",
                targetAction = targetAction!!,
                targetBranch = branchName,
                source = PipelineYamlFileReleaseReqSource.TEMPLATE_INSTANCE,
                templateName = templateInstanceBasicInfo?.templateName
            )
            val yamlFileReleaseResult = pipelineYamlFacadeService.releaseYamlFile(
                yamlFileReleaseReq = yamlFileReleaseReq
            )
            return yamlFileReleaseResult
        }
    }

    private fun createPipelineInfo(
        transactionContext: DSLContext,
        userId: String,
        pipelineBasicInfo: PipelineBasicInfo,
        pipelineModelBasicInfo: PipelineModelBasicInfo,
        version: Int,
        latestVersionStatus: VersionStatus?,
    ) {
        with(pipelineBasicInfo) {
            pipelineInfoDao.create(
                dslContext = transactionContext,
                pipelineId = pipelineId,
                projectId = projectId,
                version = version,
                pipelineName = pipelineName,
                pipelineDesc = pipelineDesc ?: pipelineName,
                userId = userId,
                channelCode = channelCode,
                manualStartup = pipelineModelBasicInfo.canManualStartup,
                canElementSkip = pipelineModelBasicInfo.canElementSkip,
                taskCount = pipelineModelBasicInfo.taskCount,
                id = id,
                latestVersionStatus = latestVersionStatus,
                pipelineDisable = pipelineDisable
            )
        }
    }

    private fun updatePipelineInfo(
        transactionContext: DSLContext,
        userId: String,
        pipelineBasicInfo: PipelineBasicInfo,
        pipelineModelBasicInfo: PipelineModelBasicInfo,
        version: Int,
        latestVersionStatus: VersionStatus?
    ) {
        with(pipelineBasicInfo) {
            pipelineInfoDao.update(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                version = version,
                pipelineName = pipelineName,
                pipelineDesc = pipelineDesc,
                manualStartup = pipelineModelBasicInfo.canManualStartup,
                canElementSkip = pipelineModelBasicInfo.canElementSkip,
                taskCount = pipelineModelBasicInfo.taskCount,
                latestVersion = version,
                latestVersionStatus = latestVersionStatus,
                locked = pipelineDisable
            )
        }
    }

    private fun createPipelineResource(
        transactionContext: DSLContext,
        userId: String,
        pipelineResourceVersion: PipelineResourceVersion
    ) {
        with(pipelineResourceVersion) {
            pipelineResourceDao.create(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId,
                creator = userId,
                version = version,
                versionName = versionName,
                model = model,
                yamlStr = yaml,
                yamlVersion = yamlVersion,
                versionNum = versionNum,
                pipelineVersion = pipelineVersion,
                triggerVersion = triggerVersion,
                settingVersion = settingVersion
            )
        }
    }

    private fun updatePipelineResource(
        transactionContext: DSLContext,
        pipelineResourceVersion: PipelineResourceVersion
    ) {
        with(pipelineResourceVersion) {
            pipelineResourceDao.updateReleaseVersion(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                model = model,
                yamlStr = yaml,
                yamlVersion = yamlVersion,
                versionName = versionName,
                versionNum = versionNum,
                pipelineVersion = pipelineVersion,
                triggerVersion = triggerVersion,
                settingVersion = settingVersion
            )
        }
    }

    private fun createPipelineResourceVersion(
        transactionContext: DSLContext,
        userId: String,
        pipelineResourceVersion: PipelineResourceVersion
    ) {
        with(pipelineResourceVersion) {
            pipelineResourceVersionDao.create(
                dslContext = transactionContext,
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                versionName = versionName ?: "",
                model = model,
                baseVersion = baseVersion,
                yamlStr = yaml,
                yamlVersion = yamlVersion,
                versionNum = versionNum,
                pipelineVersion = pipelineVersion,
                triggerVersion = triggerVersion,
                settingVersion = settingVersion,
                versionStatus = status,
                branchAction = branchAction,
                description = description
            )
        }
    }

    private fun createPipelineSettingVersion(
        transactionContext: DSLContext,
        pipelineSetting: PipelineSetting
    ) {
        val id = client.get(ServiceAllocIdResource::class).generateSegmentId(
            PIPELINE_SETTING_VERSION_BIZ_TAG_NAME
        ).data
        pipelineSettingVersionDao.saveSetting(
            dslContext = transactionContext,
            setting = pipelineSetting,
            version = pipelineSetting.version,
            id = id
        )
    }

    private fun postProcessBeforeVersionCreate(
        context: PipelineVersionCreateContext,
        pipelineResourceVersion: PipelineResourceVersion,
        pipelineSetting: PipelineSetting
    ) {
        versionCreatePostProcessors.forEach {
            it.postProcessBeforeVersionCreate(
                context = context,
                pipelineResourceVersion = pipelineResourceVersion,
                pipelineSetting = pipelineSetting
            )
        }
    }

    private fun postProcessInTransactionVersionCreate(
        transactionContext: DSLContext,
        context: PipelineVersionCreateContext,
        pipelineResourceVersion: PipelineResourceVersion,
        pipelineSetting: PipelineSetting
    ) {
        versionCreatePostProcessors.forEach {
            it.postProcessInTransactionVersionCreate(
                transactionContext = transactionContext,
                context = context,
                pipelineResourceVersion = pipelineResourceVersion,
                pipelineSetting = pipelineSetting
            )
        }
    }

    private fun postProcessAfterVersionCreate(
        context: PipelineVersionCreateContext,
        pipelineResourceVersion: PipelineResourceVersion,
        pipelineSetting: PipelineSetting
    ) {
        versionCreatePostProcessors.forEach {
            it.postProcessAfterVersionCreate(
                context = context,
                pipelineResourceVersion = pipelineResourceVersion,
                pipelineSetting = pipelineSetting
            )
        }
    }


    companion object {
        private const val PIPELINE_SETTING_VERSION_BIZ_TAG_NAME = "PIPELINE_SETTING_VERSION"
        private val logger = LoggerFactory.getLogger(PipelineVersionPersistenceService::class.java)
    }
}
