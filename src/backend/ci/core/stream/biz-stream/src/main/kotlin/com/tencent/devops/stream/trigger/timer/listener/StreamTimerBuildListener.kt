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
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.trigger.ScheduleTriggerService
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamRevisionInfo
import com.tencent.devops.stream.trigger.git.pojo.github.GithubCred
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitCred
import com.tencent.devops.stream.trigger.git.service.GithubApiService
import com.tencent.devops.stream.trigger.git.service.TGitApiService
import com.tencent.devops.stream.trigger.timer.pojo.StreamTimerBranch
import com.tencent.devops.stream.trigger.timer.pojo.event.StreamTimerBuildEvent
import com.tencent.devops.stream.trigger.timer.service.StreamTimerBranchService
import com.tencent.devops.stream.util.GitCommonUtils
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 *  MQ实现的流水线原子任务执行事件
 *
 * @version 1.0
 */
@Suppress("ALL")
@Component
class StreamTimerBuildListener @Autowired constructor(
    pipelineEventDispatcher: PipelineEventDispatcher,
    private val dslContext: DSLContext,
    private val streamTimerBranchService: StreamTimerBranchService,
    private val scheduleTriggerService: ScheduleTriggerService,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    private val streamGitConfig: StreamGitConfig,
    private val tGitApiService: TGitApiService,
    private val githubApiService: GithubApiService
) : BaseListener<StreamTimerBuildEvent>(pipelineEventDispatcher) {

    override fun run(event: StreamTimerBuildEvent) {
        with(event) {
            try {
                val record = streamBasicSettingDao.getSetting(
                    dslContext = dslContext,
                    gitProjectId = GitCommonUtils.getGitProjectId(projectId),
                    hasLastInfo = false
                )
                if (record == null) {
                    logger.warn("StreamTimerBuildListener|run|git config not exist|pipelineiId|$pipelineId")
                    return
                }

                // 如果分支不存在，每次获取最新的默认分支
                val realBranches = if (branchs.isNullOrEmpty()) {
                    when (streamGitConfig.getScmType()) {
                        ScmType.CODE_GIT -> {
                            listOf(
                                tGitApiService.getGitProjectInfo(
                                    cred = TGitCred(record.enableUserId),
                                    gitProjectId = event.gitProjectId.toString(),
                                    retry = ApiRequestRetryInfo(true)
                                )!!.defaultBranch!!
                            )
                        }
                        ScmType.GITHUB -> {
                            listOf(
                                githubApiService.getGitProjectInfo(
                                    cred = GithubCred(record.enableUserId),
                                    gitProjectId = event.gitProjectId.toString(),
                                    retry = ApiRequestRetryInfo(true)
                                )!!.defaultBranch!!
                            )
                        }
                        else -> TODO("对接其他Git平台时需要补充")
                    }
                } else {
                    branchs
                }

                realBranches.forEach { branch ->
                    timerTrigger(
                        gitUrl = record.url,
                        enableUserId = record.enableUserId,
                        branch = branch
                    )
                }
            } catch (t: OperationException) {
                logger.info("StreamTimerBuildListener|run|no start|pipelineId|$pipelineId|msg|${t.message}")
            } catch (ignored: Throwable) {
                logger.warn(
                    "StreamTimerBuildListener|run|TimerTrigger fail" +
                        "|pipelineId|$pipelineId|event|$event|error|${ignored.message}"
                )
            }
        }
    }

    private fun StreamTimerBuildEvent.timerTrigger(
        gitUrl: String,
        enableUserId: String,
        branch: String
    ) {
        try {
            val latestRevisionInfo = when (streamGitConfig.getScmType()) {
                ScmType.CODE_GIT -> tGitApiService.getLatestRevision(
                    pipelineId = pipelineId,
                    projectName = GitUtils.getProjectName(gitUrl),
                    gitUrl = gitUrl,
                    branch = branch,
                    userName = userId,
                    enableUserId = enableUserId,
                    retry = ApiRequestRetryInfo(true)
                )
                ScmType.GITHUB -> githubApiService.getLatestRevision(
                    pipelineId = pipelineId,
                    projectName = gitProjectId.toString(),
                    gitUrl = gitUrl,
                    branch = branch,
                    userName = userId,
                    enableUserId = enableUserId,
                    retry = ApiRequestRetryInfo(true)
                )
                else -> TODO("对接其他Git平台时需要补充")
            } ?: return

            if (!always) {
                branchChangeTimerTrigger(branch = branch, latestRevisionInfo = latestRevisionInfo)
            } else {
                scheduleTriggerService.triggerBuild(this, branch, latestRevisionInfo)
            }
        } catch (ignored: Throwable) {
            logger.warn(
                "StreamTimerBuildListener|timerTrigger|fail" +
                    "|pipelineId|$pipelineId|branch|$branch|error",
                ignored
            )
        }
    }

    private fun StreamTimerBuildEvent.branchChangeTimerTrigger(branch: String, latestRevisionInfo: StreamRevisionInfo) {
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
                "StreamTimerBuildListener|branchChangeTimerTrigger|start" +
                    "|pipelineId|$pipelineId|branch|$branch|revision|$latestRevision|buildId|${buildId?.id}"
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
            logger.info(
                "StreamTimerBuildListener|branchChangeTimerTrigger|revision not change" +
                    "|pipelineId|$pipelineId|branch:$branch|revision:${timerBranch.revision}"
            )
        }
    }
}
