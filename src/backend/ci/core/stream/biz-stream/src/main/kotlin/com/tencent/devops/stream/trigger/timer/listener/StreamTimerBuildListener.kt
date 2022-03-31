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

package com.tencent.devops.stream.trigger.timer.listener

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.api.service.ServiceGitBasicSettingResource
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting
import com.tencent.devops.stream.trigger.ScheduleTriggerService
import com.tencent.devops.stream.trigger.timer.pojo.StreamTimerBranch
import com.tencent.devops.stream.trigger.timer.pojo.event.StreamTimerBuildEvent
import com.tencent.devops.stream.trigger.timer.service.StreamTimerBranchService
import com.tencent.devops.stream.v2.service.StreamScmService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 *  MQ实现的流水线原子任务执行事件
 *
 * @version 1.0
 */
@Component
class StreamTimerBuildListener @Autowired constructor(
    pipelineEventDispatcher: PipelineEventDispatcher,
    private val streamTimerBranchService: StreamTimerBranchService,
    private val client: Client,
    private val scheduleTriggerService: ScheduleTriggerService,
    private val scmService: StreamScmService
) : BaseListener<StreamTimerBuildEvent>(pipelineEventDispatcher) {

    override fun run(event: StreamTimerBuildEvent) {
        with(event) {
            try {
                val gitCIConfResult = client.get(ServiceGitBasicSettingResource::class)
                    .getGitCIConf(userId = userId, projectId = projectId)
                if (gitCIConfResult.isNotOk() || gitCIConfResult.data == null) {
                    logger.warn("[$pipelineId]|git config not exist")
                    return
                }
                val gitTokenResult = client.get(ServiceOauthResource::class).gitGet(gitCIConfResult.data!!.enableUserId)
                if (gitTokenResult.isNotOk() || gitTokenResult.data == null) {
                    logger.warn("[$pipelineId]|get git token failed")
                    return
                }

                val token = gitTokenResult.data!!
                // 如果分支不存在，每次获取最新的默认分支
                val realBranches = if (branchs.isNullOrEmpty()) {
                    listOf(
                        scmService.getProjectInfoRetry(
                            token = token.accessToken,
                            gitProjectId = event.gitProjectId.toString(),
                            useAccessToken = true
                        ).defaultBranch!!
                    )
                } else {
                    branchs
                }

                realBranches.forEach { branch ->
                    timerTrigger(
                        gitCIConf = gitCIConfResult.data!!,
                        branch = branch,
                        token = token.accessToken
                    )
                }
            } catch (t: OperationException) {
                logger.info("[$pipelineId]|TimerTrigger no start| msg=${t.message}")
            } catch (ignored: Throwable) {
                logger.warn("[$pipelineId]|TimerTrigger fail event=$event| error=${ignored.message}")
            }
        }
    }

    private fun StreamTimerBuildEvent.timerTrigger(
        gitCIConf: GitCIBasicSetting,
        branch: String,
        token: String
    ) {
        try {
            val latestRevisionInfo = scmService.getLatestRevisionRetry(
                pipelineId = pipelineId,
                gitToken = token,
                projectName = GitUtils.getProjectName(gitCIConf.url),
                url = gitCIConf.url,
                type = ScmType.CODE_GIT,
                branchName = branch,
                userName = userId
            ) ?: return

            if (!always) {
                branchChangeTimerTrigger(branch = branch, latestRevisionInfo = latestRevisionInfo)
            } else {
                scheduleTriggerService.triggerBuild(this, branch, latestRevisionInfo)
            }
        } catch (ignored: Throwable) {
            logger.warn("[$pipelineId]|branch:$branch|TimerTrigger fail| error=${ignored.message}")
        }
    }

    private fun StreamTimerBuildEvent.branchChangeTimerTrigger(branch: String, latestRevisionInfo: RevisionInfo) {
        val latestRevision = latestRevisionInfo.revision
        val timerBranch = streamTimerBranchService.get(
            pipelineId = pipelineId,
            gitProjectId = gitProjectId,
            branch = branch
        )
        if ((timerBranch == null || timerBranch.revision != latestRevision)
        ) {
            val buildId = scheduleTriggerService.triggerBuild(this, branch, latestRevisionInfo)
            logger.info(
                "[$pipelineId]|branch:$branch|revision:$latestRevision|TimerTrigger start| buildId=${buildId?.id}"
            )
            if (buildId != null) {
                streamTimerBranchService.save(
                    StreamTimerBranch(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        gitProjectId = gitProjectId,
                        branch = branch,
                        revision = latestRevision
                    )
                )
            }
        } else {
            logger.info("$pipelineId|branch:$branch|revision:${timerBranch.revision}|revision not change")
        }
    }
}
