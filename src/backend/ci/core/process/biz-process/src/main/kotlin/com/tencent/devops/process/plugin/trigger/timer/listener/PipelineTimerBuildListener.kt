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

package com.tencent.devops.process.plugin.trigger.timer.listener

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_HASH_ID
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_BRANCH
import com.tencent.devops.process.api.service.ServiceTimerBuildResource
import com.tencent.devops.process.plugin.trigger.pojo.event.PipelineTimerBuildEvent
import com.tencent.devops.process.plugin.trigger.service.PipelineTimerService
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.process.yaml.PipelineYamlService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 *  MQ实现的流水线原子任务执行事件
 *
 * @version 1.0
 */
@Component
class PipelineTimerBuildListener @Autowired constructor(
    pipelineEventDispatcher: PipelineEventDispatcher,
    private val serviceTimerBuildResource: ServiceTimerBuildResource,
    private val pipelineTimerService: PipelineTimerService,
    private val scmProxyService: ScmProxyService,
    private val pipelineYamlService: PipelineYamlService
) : BaseListener<PipelineTimerBuildEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineTimerBuildEvent) {
        val pipelineTimer =
            pipelineTimerService.get(projectId = event.projectId, pipelineId = event.pipelineId) ?: return
        with(pipelineTimer) {
            when {
                (repoHashId == null && branchs.isNullOrEmpty()) || noScm != true ->
                    timerTrigger(event = event)

                else ->
                    repoTimerTrigger(
                        event = event,
                        repoHashId = repoHashId,
                        branchs = branchs
                    )
            }
        }
    }

    private fun timerTrigger(event: PipelineTimerBuildEvent, params: Map<String, String> = emptyMap()): String? {
        with(event) {
            try {
                val buildResult = serviceTimerBuildResource.timerTrigger(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    params = params,
                    channelCode = channelCode
                )

                // 如果是不存在的流水线，则直接删除定时任务，相当于给异常创建失败的定时流水线做清理
                if (buildResult.data.isNullOrBlank()) {
                    pipelineTimerService.deleteTimer(projectId, pipelineId, userId)
                    logger.warn("[$pipelineId]|pipeline not exist!${buildResult.message}")
                } else {
                    logger.info("[$pipelineId]|TimerTrigger start| buildId=${buildResult.data}")
                }
                return buildResult.data
            } catch (t: OperationException) {
                logger.info("[$pipelineId]|TimerTrigger no start| msg=${t.message}")
            } catch (ignored: Throwable) {
                logger.warn("[$pipelineId]|TimerTrigger fail event=$this| error=${ignored.message}")
            }
            return null
        }
    }

    private fun repoTimerTrigger(event: PipelineTimerBuildEvent, repoHashId: String?, branchs: List<String>?) {
        with(event) {
            try {
                val finalRepoHashId = when {
                    !repoHashId.isNullOrBlank() -> repoHashId
                    // 分支不为空,如果流水线开启pac,则为开启pac的代码库
                    !branchs.isNullOrEmpty() -> {
                        val pipelineYamlInfo = pipelineYamlService.getPipelineYamlInfo(
                            projectId = projectId, pipelineId = pipelineId
                        ) ?: return
                        pipelineYamlInfo.repoHashId
                    }
                    else -> {
                        logger.info("timer trigger not found repo hashId|$projectId|$pipelineId")
                        return
                    }
                }
                val repositoryConfig = RepositoryConfig(
                    repositoryHashId = finalRepoHashId,
                    repositoryName = null,
                    repositoryType = RepositoryType.ID
                )
                val finalBranchs = if (branchs.isNullOrEmpty()) {
                    val defaultBranch = scmProxyService.getDefaultBranch(
                        projectId = projectId,
                        repositoryConfig = repositoryConfig
                    ) ?: return
                    listOf(defaultBranch)
                } else {
                    branchs
                }
                finalBranchs.forEach { branch ->
                    branchTimerTrigger(event = event, repoHashId = finalRepoHashId, branch = branch)
                }
            } catch (ignored: Exception) {
                logger.warn("repo timer trigger fail|$projectId|$pipelineId|$repoHashId|$branchs")
            }
        }
    }

    private fun branchTimerTrigger(event: PipelineTimerBuildEvent, repoHashId: String, branch: String) {
        val repositoryConfig = RepositoryConfig(
            repositoryHashId = repoHashId,
            repositoryName = null,
            repositoryType = RepositoryType.ID
        )
        with(event) {
            logger.info("start to build by time trigger|$projectId|$pipelineId|$repoHashId|$branch")
            try {
                val revision = scmProxyService.recursiveFetchLatestRevision(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    repositoryConfig = repositoryConfig,
                    branchName = branch,
                    variables = emptyMap()
                ).data?.revision ?: return
                val timerBranch = pipelineTimerService.getTimerBranch(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    repoHashId = repoHashId,
                    branch = branch
                )
                if (timerBranch == null || timerBranch.revision != revision) {
                    val buildId = timerTrigger(
                        event = event,
                        params = mapOf(
                            BK_REPO_WEBHOOK_HASH_ID to repoHashId,
                            PIPELINE_WEBHOOK_BRANCH to branch
                        )
                    ) ?: return
                    logger.info("success to build by time trigger|$projectId|$pipelineId|$repoHashId|$branch|$buildId")
                    pipelineTimerService.saveTimerBranch(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        repoHashId = repoHashId,
                        branch = branch,
                        revision = revision
                    )
                } else {
                    logger.info("branch timer trigger fail,revision not change|$pipelineId|$repoHashId|$branch")
                }
            } catch (exception: Exception) {
                logger.warn("branch timer trigger fail|$projectId|$pipelineId|$repoHashId|$branch", exception)
            }
        }
    }
}
