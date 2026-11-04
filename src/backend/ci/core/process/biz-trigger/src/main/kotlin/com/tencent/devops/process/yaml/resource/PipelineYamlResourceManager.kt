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

package com.tencent.devops.process.yaml.resource

import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.pojo.element.atom.PipelineCheckFailedErrors
import com.tencent.devops.process.api.service.ServicePipelineYamlVersionResource
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_ELEMENT_CHECK_FAILED
import com.tencent.devops.process.engine.utils.TemplateInstanceUtil
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.PipelineYamlBranchActionReq
import com.tencent.devops.process.pojo.pipeline.PipelineYamlDeployReq
import com.tencent.devops.process.pojo.pipeline.PipelineYamlPullRequestReq
import com.tencent.devops.process.trigger.client.TriggerProcessClient
import com.tencent.devops.process.yaml.mq.PipelineYamlFileEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * yaml 文件对应的流水线/模板操作,资源属于 process,通过接口调用
 */
@Service
class PipelineYamlResourceManager @Autowired constructor(
    private val triggerProcessClient: TriggerProcessClient
) {
    fun createYamlPipeline(
        userId: String,
        projectId: String,
        yaml: String,
        event: PipelineYamlFileEvent
    ): DeployPipelineResult {
        return triggerProcessClient.call(projectId, ServicePipelineYamlVersionResource::class) {
            it.createYamlPipeline(
                userId = userId,
                projectId = projectId,
                isTemplate = event.isTemplate,
                request = event.toDeployReq(yaml)
            ).data
        } ?: throw IllegalStateException("create yaml pipeline failed|$projectId|${event.filePath}")
    }

    fun updateYamlPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        yaml: String,
        event: PipelineYamlFileEvent
    ): DeployPipelineResult {
        return triggerProcessClient.call(projectId, ServicePipelineYamlVersionResource::class) {
            it.updateYamlPipeline(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                isTemplate = event.isTemplate,
                request = event.toDeployReq(yaml)
            ).data
        } ?: throw IllegalStateException("update yaml pipeline failed|$projectId|$pipelineId")
    }

    fun updateBranchAction(
        userId: String,
        projectId: String,
        pipelineId: String,
        branchName: String,
        branchVersionAction: BranchVersionAction,
        isTemplate: Boolean
    ) {
        triggerProcessClient.call(projectId, ServicePipelineYamlVersionResource::class) {
            it.updateBranchAction(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                isTemplate = isTemplate,
                request = PipelineYamlBranchActionReq(
                    branchName = branchName,
                    branchVersionAction = branchVersionAction
                )
            )
        }
    }

    fun deletePipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        isTemplate: Boolean
    ) {
        triggerProcessClient.call(projectId, ServicePipelineYamlVersionResource::class) {
            it.deletePipeline(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                isTemplate = isTemplate
            )
        }
    }

    fun getPipelineName(
        projectId: String,
        pipelineId: String,
        isTemplate: Boolean
    ): String? {
        return triggerProcessClient.call(projectId, ServicePipelineYamlVersionResource::class) {
            it.getPipelineName(
                projectId = projectId,
                pipelineId = pipelineId,
                isTemplate = isTemplate
            ).data
        }
    }

    fun existsReleaseVersion(
        projectId: String,
        pipelineId: String,
        isTemplate: Boolean
    ): Boolean {
        return triggerProcessClient.call(projectId, ServicePipelineYamlVersionResource::class) {
            it.existsReleaseVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                isTemplate = isTemplate
            ).data
        } ?: false
    }

    /**
     * 合并请求完成
     */
    fun completePullRequest(
        userId: String,
        projectId: String,
        pipelineId: String,
        pullRequestId: Long,
        pullRequestUrl: String,
        pullRequestNumber: Int,
        merged: Boolean,
        isTemplate: Boolean,
        exception: Throwable? = null
    ) {
        val instanceErrorInfo = exception?.let {
            translateException(userId = userId, projectId = projectId, pipelineId = pipelineId, exception = it)
        }
        triggerProcessClient.call(projectId, ServicePipelineYamlVersionResource::class) {
            it.completePullRequest(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                isTemplate = isTemplate,
                request = PipelineYamlPullRequestReq(
                    pullRequestId = pullRequestId,
                    pullRequestUrl = pullRequestUrl,
                    pullRequestNumber = pullRequestNumber,
                    merged = merged,
                    instanceErrorInfo = instanceErrorInfo
                )
            )
        }
    }

    private fun PipelineYamlFileEvent.toDeployReq(yaml: String) = PipelineYamlDeployReq(
        yaml = yaml,
        repoHashId = repoHashId,
        filePath = filePath,
        ref = ref,
        defaultBranch = defaultBranch,
        commitMsg = commit!!.commitMsg,
        pullRequestId = pullRequestId,
        pullRequestUrl = pullRequestUrl
    )

    /**
     * 将 yaml 处理异常转换为可展示的失败原因 json
     */
    private fun translateException(
        userId: String,
        projectId: String,
        pipelineId: String,
        exception: Throwable
    ): String {
        // 跨服务调用时流水线校验失败的错误信息是 json,需要还原
        val remoteCheckFailed = (exception as? RemoteServiceException)
            ?.takeIf { it.errorCode?.toString() == ERROR_PIPELINE_ELEMENT_CHECK_FAILED }
            ?.let { runCatching { JsonUtil.to(it.errorMessage, PipelineCheckFailedErrors::class.java) }.getOrNull() }
        val reason = remoteCheckFailed ?: TemplateInstanceUtil.translateInstanceException(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            exception = exception
        )
        return JsonUtil.toJson(reason, false)
    }
}
