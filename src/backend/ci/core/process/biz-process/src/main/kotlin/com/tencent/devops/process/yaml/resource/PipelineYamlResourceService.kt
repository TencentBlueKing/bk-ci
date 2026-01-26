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

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.atom.PipelineCheckFailedMsg
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileInfo
import com.tencent.devops.process.pojo.pipeline.version.PipelineYamlWebhookReq
import com.tencent.devops.process.pojo.template.TemplatePipelineStatus
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.process.service.pipeline.version.PipelineVersionManager
import com.tencent.devops.process.service.template.v2.PipelineTemplateRelatedService
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.process.yaml.mq.PipelineYamlFileEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class PipelineYamlResourceService @Autowired constructor(
    @Lazy private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineVersionManager: PipelineVersionManager,
    private val pipelineTemplateRelatedService: PipelineTemplateRelatedService
) : IPipelineYamlResourceService {
    override fun createYamlPipeline(
        userId: String,
        projectId: String,
        yaml: String,
        event: PipelineYamlFileEvent
    ): DeployPipelineResult {
        with(event) {
            val isDefaultBranch = ref == defaultBranch
            val yamlFileInfo = PipelineYamlFileInfo(repoHashId = repoHashId, filePath = filePath)
            val yamlFileName = GitActionCommon.getCiFileName(filePath)
            val pipelineYamlWebhookReq = PipelineYamlWebhookReq(
                yaml = yaml,
                yamlFileName = yamlFileName,
                branchName = ref,
                isDefaultBranch = isDefaultBranch,
                description = commit!!.commitMsg,
                yamlFileInfo = yamlFileInfo,
                pullRequestId = pullRequestId,
                pullRequestUrl = pullRequestUrl
            )
            return pipelineVersionManager.deployPipeline(
                userId = userId,
                projectId = projectId,
                request = pipelineYamlWebhookReq
            )
        }
    }

    override fun updateYamlPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        yaml: String,
        event: PipelineYamlFileEvent
    ): DeployPipelineResult {
        with(event) {
            val isDefaultBranch = ref == defaultBranch
            val yamlFileInfo = PipelineYamlFileInfo(repoHashId = repoHashId, filePath = filePath)
            val yamlFileName = GitActionCommon.getCiFileName(filePath)
            val pipelineYamlWebhookReq = PipelineYamlWebhookReq(
                yaml = yaml,
                yamlFileName = yamlFileName,
                branchName = ref,
                isDefaultBranch = isDefaultBranch,
                description = commit!!.commitMsg,
                yamlFileInfo = yamlFileInfo,
                pullRequestId = pullRequestId,
                pullRequestUrl = pullRequestUrl
            )
            return pipelineVersionManager.deployPipeline(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                request = pipelineYamlWebhookReq
            )
        }
    }

    override fun updateBranchAction(
        userId: String,
        projectId: String,
        pipelineId: String,
        branchName: String,
        branchVersionAction: BranchVersionAction
    ) {
        pipelineInfoFacadeService.updateBranchVersion(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            branchName = branchName,
            releaseBranch = true,
            branchVersionAction = branchVersionAction
        )
    }

    override fun deletePipeline(userId: String, projectId: String, pipelineId: String) {
        pipelineInfoFacadeService.deletePipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = ChannelCode.BS
        )
    }

    override fun getPipelineName(projectId: String, pipelineId: String): String? {
        return pipelineRepositoryService.getPipelineInfo(
            projectId = projectId,
            pipelineId = pipelineId
        )?.pipelineName
    }

    override fun existsReleaseVersion(projectId: String, pipelineId: String): Boolean {
        return pipelineRepositoryService.getReleaseVersionRecord(
            projectId = projectId, pipelineId = pipelineId
        ) != null
    }

    override fun completePullRequest(
        projectId: String,
        pipelineId: String,
        pullRequestId: Long,
        pullRequestUrl: String,
        pullRequestNumber: Int,
        merged: Boolean
    ) {
        val (status, instanceErrorInfo) = if (merged) {
            Pair(TemplatePipelineStatus.UPDATED, null)
        } else {
            val message = I18nUtil.getCodeLanMessage(
                messageCode = ProcessMessageCode.BK_YAML_INSTANCE_PULL_REQUEST_CLOSED,
                params = arrayOf(pullRequestUrl, pullRequestNumber.toString())
            )
            Pair(TemplatePipelineStatus.FAILED, PipelineCheckFailedMsg(message))
        }
        pipelineTemplateRelatedService.updateStatusByPullRequestId(
            projectId = projectId,
            pipelineId = pipelineId,
            status = status,
            instanceErrorInfo = instanceErrorInfo?.let { JsonUtil.toJson(it, false) },
            pullRequestId = pullRequestId
        )
    }
}
