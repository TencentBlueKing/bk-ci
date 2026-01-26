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

package com.tencent.devops.process.service.pipeline.version.handler

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.control.lock.PipelineModelLock
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.process.pojo.PipelineVersionReleaseRequest
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.PipelineResourceOnlyVersion
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import com.tencent.devops.process.service.pipeline.version.PipelineVersionGenerator
import com.tencent.devops.process.service.pipeline.version.PipelineVersionPersistenceService
import com.tencent.devops.process.yaml.PipelineYamlCommonService
import com.tencent.devops.process.yaml.PipelineYamlFacadeService
import jakarta.ws.rs.core.Response
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

/**
 * 流水线发布处理器
 */
@Service
class PipelineDraftReleaseHandler @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val pipelineVersionGenerator: PipelineVersionGenerator,
    private val pipelineVersionPersistenceService: PipelineVersionPersistenceService,
    private val dslContext: DSLContext,
    private val pipelineResourceVersionDao: PipelineResourceVersionDao,
    private val pipelineResourceDao: PipelineResourceDao,
    @Lazy
    private val pipelineYamlFacadeService: PipelineYamlFacadeService,
    private val pipelineYamlCommonService: PipelineYamlCommonService
) : PipelineVersionCreateHandler {
    override fun support(context: PipelineVersionCreateContext): Boolean {
        return context.versionAction == PipelineVersionAction.RELEASE_DRAFT
    }

    override fun handle(context: PipelineVersionCreateContext): DeployPipelineResult {
        logger.info("draft version released with context={}", JsonUtil.toJson(context, false))
        with(context) {
            if (enablePac) {
                if (targetAction == null) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.ERROR_NEED_PARAM_,
                        params = arrayOf(PipelineVersionReleaseRequest::targetAction.name)
                    )
                }
                if (yamlFileInfo == null) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.ERROR_NEED_PARAM_,
                        params = arrayOf(PipelineVersionReleaseRequest::yamlInfo.name)
                    )
                }
                if (pipelineResourceWithoutVersion.yaml == null) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_YAML_CONTENT_IS_EMPTY
                    )
                }
            }
            val lock = PipelineModelLock(redisOperation, pipelineId)
            try {
                lock.lock()
                return doHandle()
            } finally {
                lock.unlock()
            }
        }
    }

    private fun PipelineVersionCreateContext.doHandle(): DeployPipelineResult {
        val draftResource = pipelineResourceVersionDao.getVersionResource(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            includeDraft = true
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_DRAFT_EXISTS
        )
        // 加锁之后,再次验证草稿版本是否已经发布
        if (draftResource.status != VersionStatus.COMMITTING) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_RELEASE_MUST_DRAFT_VERSION
            )
        }
        val resourceOnlyVersion = pipelineVersionGenerator.generateDraftReleaseVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            draftResource = draftResource,
            enablePac = enablePac,
            repoHashId = yamlFileInfo?.repoHashId,
            targetAction = targetAction,
            targetBranch = branchName
        )
        val releaseResource = pipelineResourceDao.getReleaseVersionResource(
            dslContext = dslContext, projectId = projectId, pipelineId = pipelineId
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
            params = arrayOf(pipelineId)
        )
        // 如果当前草稿和正式版本一致则拦截发布
        if (isSameVersion(resourceOnlyVersion = resourceOnlyVersion, releaseResource = releaseResource)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_VERSION_IS_NOT_UPDATED
            )
        }

        var updateBuildNo = false
        draftResource.model.getTriggerContainer().buildNo?.let {
            val releaseBuildNo = releaseResource.model.getTriggerContainer().buildNo
            // [关闭变为开启]或[修改buildNo数值]，都属于更新行为，需要提示更新
            if (releaseBuildNo == null || releaseBuildNo.buildNo != it.buildNo) {
                updateBuildNo = true
            }
        }
        // 检查推送参数
        enablePac.takeIf { it }?.let {
            pipelineYamlCommonService.checkPushParam(
                projectId = projectId,
                pipelineId = pipelineId,
                content = pipelineResourceWithoutVersion.yaml!!,
                repoHashId = yamlFileInfo!!.repoHashId,
                filePath = yamlFileInfo.filePath,
                targetAction = targetAction!!,
                versionName = resourceOnlyVersion.versionName,
                targetBranch = targetBranch
            )
        }
        if (pipelineResourceWithoutVersion.status == VersionStatus.RELEASED) {
            pipelineVersionPersistenceService.releaseDraft2ReleaseVersion(
                context = this,
                resourceOnlyVersion = resourceOnlyVersion
            )
        } else {
            pipelineVersionPersistenceService.releaseDraft2BranchVersion(
                context = this,
                resourceOnlyVersion = resourceOnlyVersion
            )
        }

        // 推送文件
        val yamlFileReleaseResult = enablePac.takeIf { it }?.let {
            pipelineVersionPersistenceService.releaseYamlFile(
                context = this,
                resourceOnlyVersion = resourceOnlyVersion
            )
        }

        val yamlInfo = pipelineYamlFacadeService.getPipelineYamlInfo(
            projectId = projectId,
            pipelineId = pipelineId,
            version = resourceOnlyVersion.version
        )
        return DeployPipelineResult(
            pipelineId = pipelineId,
            pipelineName = pipelineBasicInfo.pipelineName,
            version = resourceOnlyVersion.version,
            versionNum = resourceOnlyVersion.versionNum,
            versionName = resourceOnlyVersion.versionName,
            yamlInfo = yamlInfo,
            targetUrl = yamlFileReleaseResult?.pullRequestUrl,
            pullRequestId = yamlFileReleaseResult?.pullRequestId,
            updateBuildNo = updateBuildNo
        )
    }

    private fun isSameVersion(
        resourceOnlyVersion: PipelineResourceOnlyVersion,
        releaseResource: PipelineResourceVersion
    ) = resourceOnlyVersion.version != releaseResource.version &&
            resourceOnlyVersion.pipelineVersion == releaseResource.pipelineVersion &&
            resourceOnlyVersion.triggerVersion == releaseResource.triggerVersion &&
            resourceOnlyVersion.settingVersion == releaseResource.settingVersion

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineDraftReleaseHandler::class.java)
    }
}
