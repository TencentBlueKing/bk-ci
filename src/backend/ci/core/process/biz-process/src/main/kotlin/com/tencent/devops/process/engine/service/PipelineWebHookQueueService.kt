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

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStartBroadCastEvent
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_QUEUE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_SOURCE_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_SOURCE_PROJECT_ID
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_SOURCE_REPO_NAME
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TARGET_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TARGET_PROJECT_ID
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TARGET_REPO_NAME
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.constant.ProcessMessageCode.BK_TRIGGERED_BY_GIT_EVENT_PLUGIN
import com.tencent.devops.process.engine.control.lock.PipelineWebHookQueueLock
import com.tencent.devops.process.engine.dao.PipelineWebHookQueueDao
import com.tencent.devops.process.engine.pojo.PipelineWebHookQueue
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineWebHookQueueService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val pipelineWebHookQueueDao: PipelineWebHookQueueDao,
    private val redisOperation: RedisOperation,
    private val buildLogPrinter: BuildLogPrinter
) {
    private val logger = LoggerFactory.getLogger(PipelineWebHookQueueService::class.java)

    fun onBuildStart(event: PipelineBuildStartBroadCastEvent) {
        with(event) {
            if (triggerType != StartType.WEB_HOOK.name) {
                return
            }
            deleteQueue(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId
            )
        }
    }

    /**
     * 如果构建还没启动就已经失败，比如排队超时,排队时用户取消,也需要删除webhook queue.
     */
    fun onBuildFinish(event: PipelineBuildFinishBroadCastEvent) {
        with(event) {
            val buildStatus = BuildStatus.parse(status)
            if (triggerType != StartType.WEB_HOOK.name && buildStatus.isFailure()) {
                return
            }
            deleteQueue(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId
            )
        }
    }

    private fun deleteQueue(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ) {
        try {
            val buildHistoryResult = client.get(ServiceBuildResource::class).getBuildVars(
                userId = userId, projectId = projectId,
                pipelineId = pipelineId, buildId = buildId, channelCode = ChannelCode.GIT
            )

            if (buildHistoryResult.isNotOk() || buildHistoryResult.data == null) {
                logger.warn("webhook queue ($buildId) not exist: ${buildHistoryResult.message}")
                return
            }
            val buildInfo = buildHistoryResult.data!!

            val variables = buildInfo.variables
            if (variables.isEmpty()) {
                logger.warn("webhook queue ($buildId) variables is empty")
                return
            }
            execute(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                variables = variables
            ) {
                logger.info("webhook queue|$projectId|$pipelineId|$buildId is ${buildInfo.status}, delete")
                pipelineWebHookQueueDao.deleteByBuildIds(
                    dslContext = dslContext,
                    projectId = projectId,
                    buildIds = listOf(buildId)
                )
            }
        } catch (t: Throwable) {
            logger.error("webhook queue failed on build start|$projectId|$pipelineId|$buildId", t)
        }
    }

    fun onWebHookTrigger(projectId: String, pipelineId: String, buildId: String, variables: Map<String, Any>) {
        try {
            execute(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                variables = variables.mapValues { it.value.toString() }
            ) { pipelineWebHookQueue ->
                logger.info("webhook queue start on webhook trigger|$projectId|$pipelineId|$buildId")
                with(pipelineWebHookQueue) {
                    val webHookBuildHistory = pipelineWebHookQueueDao.getWebHookBuildHistory(
                        dslContext = dslContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        sourceProjectId = sourceProjectId,
                        sourceBranch = sourceBranch,
                        targetProjectId = targetProjectId,
                        targetBranch = targetBranch
                    )
                    val id = client.get(ServiceAllocIdResource::class).generateSegmentId("PIPELINE_WEBHOOK_QUEUE").data
                    if (webHookBuildHistory != null && webHookBuildHistory.isNotEmpty()) {
                        webHookBuildHistory.forEach { queue ->
                            logger.info("webhook queue on webhook trigger|$projectId|$pipelineId|${queue.buildId} " +
                                "be canceled because of $buildId")
                            buildLogPrinter.addYellowLine(
                                buildId = queue.buildId,
                                message = I18nUtil.getCodeLanMessage(BK_TRIGGERED_BY_GIT_EVENT_PLUGIN) +
                                        "[<a target='_blank' href='" + // #4796 日志展示去掉链接上的域名前缀
                                    "/console/pipeline/$projectId/$pipelineId/detail/$buildId'>$buildId</a>]overlay",
                                tag = "",
                                jobId = "",
                                executeCount = 1
                            )
                            client.get(ServiceBuildResource::class).serviceShutdown(
                                pipelineId = pipelineId,
                                projectId = projectId,
                                buildId = queue.buildId,
                                channelCode = ChannelCode.BS
                            )
                        }
                        dslContext.transaction { configuration ->
                            val context = DSL.using(configuration)
                            pipelineWebHookQueueDao.deleteByBuildIds(
                                dslContext = context,
                                projectId = projectId,
                                buildIds = webHookBuildHistory.map { it.buildId }
                            )
                            pipelineWebHookQueueDao.save(
                                dslContext = context,
                                projectId = projectId,
                                pipelineId = pipelineId,
                                sourceProjectId = sourceProjectId,
                                sourceRepoName = sourceRepoName,
                                sourceBranch = sourceBranch,
                                targetProjectId = targetProjectId,
                                targetRepoName = targetRepoName,
                                targetBranch = targetBranch,
                                buildId = buildId,
                                id = id
                            )
                        }
                    } else {
                        pipelineWebHookQueueDao.save(
                            dslContext = dslContext,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            sourceProjectId = sourceProjectId,
                            sourceRepoName = sourceRepoName,
                            sourceBranch = sourceBranch,
                            targetProjectId = targetProjectId,
                            targetRepoName = targetRepoName,
                            targetBranch = targetBranch,
                            buildId = buildId,
                            id = id
                        )
                    }
                }
            }
        } catch (t: Throwable) {
            logger.error("webhook queue failed on webhook trigger|$projectId|$pipelineId|$buildId", t)
        }
    }

    private fun execute(
        projectId: String,
        pipelineId: String,
        buildId: String,
        variables: Map<String, String>,
        action: (PipelineWebHookQueue) -> Unit
    ) {
        val enableWebHookQueue = variables[PIPELINE_WEBHOOK_QUEUE]?.toBoolean() ?: false
        logger.info("webhook queue|$projectId|$pipelineId|$buildId|$enableWebHookQueue")
        if (!enableWebHookQueue) {
            return
        }
        val sourceProjectId = variables[PIPELINE_WEBHOOK_SOURCE_PROJECT_ID]?.toLong()
        val sourceRepoName = variables[PIPELINE_WEBHOOK_SOURCE_REPO_NAME] ?: ""
        val sourceBranch = variables[PIPELINE_WEBHOOK_SOURCE_BRANCH] ?: ""
        val targetProjectId = variables[PIPELINE_WEBHOOK_TARGET_PROJECT_ID]?.toLong()
        val targetRepoName = variables[PIPELINE_WEBHOOK_TARGET_REPO_NAME] ?: ""
        val targetBranch = variables[PIPELINE_WEBHOOK_TARGET_BRANCH] ?: ""
        if (sourceProjectId == null || sourceBranch.isBlank() ||
            targetProjectId == null || targetBranch.isBlank()
        ) {
            logger.info(
                "webhook queue|$projectId|$pipelineId|$buildId|" +
                    "sourceProjectId:$sourceProjectId|targetProjectId:$targetProjectId|" +
                    "sourceBranchName:$sourceBranch|targetBranchName:$targetBranch is empty"
            )
            return
        }
        val lock = PipelineWebHookQueueLock(
            redisOperation = redisOperation,
            pipelineId = pipelineId,
            sourceProjectId = sourceProjectId,
            sourceBranch = sourceBranch,
            targetProjectId = targetProjectId,
            targetBranch = targetBranch
        )
        try {
            lock.lock()
            action(
                PipelineWebHookQueue(
                    pipelineId = pipelineId,
                    sourceProjectId = sourceProjectId,
                    sourceRepoName = sourceRepoName,
                    sourceBranch = sourceBranch,
                    targetProjectId = targetProjectId,
                    targetRepoName = targetRepoName,
                    targetBranch = targetBranch,
                    buildId = buildId
                )
            )
        } finally {
            lock.unlock()
        }
    }
}
