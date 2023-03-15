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

package com.tencent.devops.plugin.service.git

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQueueBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ENABLE_CHECK
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_EVENT_TYPE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_BLOCK
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_EVENT_TYPE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_MR_ID
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_REPO
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_REPO_TYPE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_REVISION
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TYPE
import com.tencent.devops.model.plugin.tables.records.TPluginGitCheckRecord
import com.tencent.devops.plugin.api.pojo.GitCommitCheckEvent
import com.tencent.devops.plugin.api.pojo.GitCommitCheckInfo
import com.tencent.devops.plugin.api.pojo.GithubCheckRun
import com.tencent.devops.plugin.api.pojo.GithubPrEvent
import com.tencent.devops.plugin.api.pojo.PluginGitCheck
import com.tencent.devops.plugin.dao.PluginGitCheckDao
import com.tencent.devops.plugin.dao.PluginGithubCheckDao
import com.tencent.devops.plugin.service.ScmCheckService
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.scm.code.git.api.GITHUB_CHECK_RUNS_CONCLUSION_FAILURE
import com.tencent.devops.scm.code.git.api.GITHUB_CHECK_RUNS_CONCLUSION_SUCCESS
import com.tencent.devops.scm.code.git.api.GITHUB_CHECK_RUNS_STATUS_COMPLETED
import com.tencent.devops.scm.code.git.api.GITHUB_CHECK_RUNS_STATUS_IN_PROGRESS
import com.tencent.devops.scm.code.git.api.GIT_COMMIT_CHECK_STATE_ERROR
import com.tencent.devops.scm.code.git.api.GIT_COMMIT_CHECK_STATE_FAILURE
import com.tencent.devops.scm.code.git.api.GIT_COMMIT_CHECK_STATE_PENDING
import com.tencent.devops.scm.code.git.api.GIT_COMMIT_CHECK_STATE_SUCCESS
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
@Suppress("ALL")
class CodeWebhookService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val pluginGitCheckDao: PluginGitCheckDao,
    private val pluginGithubCheckDao: PluginGithubCheckDao,
    private val redisOperation: RedisOperation,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val scmCheckService: ScmCheckService,
    private val gitWebhookUnlockService: GitWebhookUnlockService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CodeWebhookService::class.java)
        // GIT无目标分支，用于工蜂PUSH事件check回写
        const val GIT_COMMIT_CHECK_NONE_TARGET_BRANCH = "~NONE"
    }

    fun onBuildQueue(event: PipelineBuildQueueBroadCastEvent) {
        logger.info("Code web hook on start [${event.buildId}]")
        with(event) {
            execute(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                userId = userId,
                triggerType = triggerType
            ) { info ->
                with(info) {
                    val webhookType = CodeType.valueOf(webhookType)
                    val webhookEventType = CodeEventType.valueOf(webhookEventType)
                    val repoCondition = webhookType == CodeType.GIT || webhookType == CodeType.TGIT
                    val eventCondition = webhookEventType == CodeEventType.MERGE_REQUEST || webhookEventType ==
                        CodeEventType.PUSH
                    when {
                        enableCheck && eventCondition && repoCondition -> {
                            logger.info(
                                "$buildId|WebHook_ADD_GIT_COMMIT_CHECK|$pipelineId|$repositoryConfig|$commitId]"
                            )
                            addGitCommitCheckEvent(
                                GitCommitCheckEvent(
                                    source = "codeWebhook_pipeline_build_trigger",
                                    userId = event.userId,
                                    projectId = projectId,
                                    pipelineId = pipelineId,
                                    buildId = buildId,
                                    repositoryConfig = repositoryConfig,
                                    commitId = commitId,
                                    state = GIT_COMMIT_CHECK_STATE_PENDING,
                                    block = block,
                                    targetBranch = if (webhookEventType == CodeEventType.MERGE_REQUEST) {
                                        targetBranch
                                    } else {
                                        GIT_COMMIT_CHECK_NONE_TARGET_BRANCH
                                    }
                                )
                            )
                        }
                        webhookEventType == CodeEventType.PULL_REQUEST && webhookType == CodeType.GITHUB -> {
                            logger.info(
                                "$buildId|WebHook_ADD_GITHUB_COMMIT_CHECK|$pipelineId|$repositoryConfig|$commitId]"
                            )
                            addGithubPrEvent(
                                GithubPrEvent(
                                    source = "codeWebhook_pipeline_build_trigger",
                                    userId = event.userId,
                                    projectId = projectId,
                                    pipelineId = pipelineId,
                                    buildId = buildId,
                                    repositoryConfig = repositoryConfig,
                                    commitId = commitId,
                                    status = GITHUB_CHECK_RUNS_STATUS_IN_PROGRESS,
                                    startedAt = LocalDateTime.now().timestamp(),
                                    conclusion = null,
                                    completedAt = null
                                )
                            )
                        }
                        else -> {
                            logger.info("$buildId|code type $webhookType and event type $webhookEventType ignored")
                        }
                    }
                }
            }
        }
    }

    fun onBuildFinished(event: PipelineBuildFinishBroadCastEvent) {
        logger.info("Code web hook on finish [${event.buildId}]")
        with(event) {
            val buildStatus = BuildStatus.valueOf(event.status)
            execute(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                userId = userId,
                triggerType = triggerType
            ) { info ->
                with(info) {
                    val webhookType = CodeType.valueOf(webhookType)
                    val webhookEventType = CodeEventType.valueOf(webhookEventType)
                    val repoCondition = webhookType == CodeType.GIT || webhookType == CodeType.TGIT
                    val eventCondition = webhookEventType == CodeEventType.MERGE_REQUEST || webhookEventType ==
                        CodeEventType.PUSH
                    when {
                        enableCheck && repoCondition && eventCondition -> {
                            val state = if (buildStatus == BuildStatus.SUCCEED) {
                                GIT_COMMIT_CHECK_STATE_SUCCESS
                            } else {
                                GIT_COMMIT_CHECK_STATE_FAILURE
                            }
                            addGitCommitCheckEvent(
                                GitCommitCheckEvent(
                                    source = "codeWebhook",
                                    projectId = projectId,
                                    pipelineId = pipelineId,
                                    buildId = buildId,
                                    repositoryConfig = repositoryConfig,
                                    commitId = commitId,
                                    state = state,
                                    block = block,
                                    status = event.status,
                                    triggerType = event.triggerType,
                                    startTime = event.startTime ?: 0L,
                                    mergeRequestId = mergeRequestId,
                                    userId = event.userId,
                                    retryTime = 3,
                                    targetBranch = if (webhookEventType == CodeEventType.MERGE_REQUEST) {
                                        targetBranch
                                    } else {
                                        GIT_COMMIT_CHECK_NONE_TARGET_BRANCH
                                    }
                                )
                            )
                        }
                        webhookType == CodeType.GITHUB && webhookEventType == CodeEventType.PULL_REQUEST -> {
                            val status = GITHUB_CHECK_RUNS_STATUS_COMPLETED
                            val conclusion = if (buildStatus == BuildStatus.SUCCEED) {
                                GITHUB_CHECK_RUNS_CONCLUSION_SUCCESS
                            } else {
                                GITHUB_CHECK_RUNS_CONCLUSION_FAILURE
                            }
                            addGithubPrEvent(
                                GithubPrEvent(
                                    source = "codeWebhook",
                                    projectId = projectId,
                                    pipelineId = pipelineId,
                                    buildId = buildId,
                                    repositoryConfig = repositoryConfig,
                                    commitId = commitId,
                                    status = status,
                                    startedAt = null,
                                    conclusion = conclusion,
                                    completedAt = LocalDateTime.now().timestamp(),
                                    userId = event.userId,
                                    retryTime = 3
                                )
                            )
                        }
                        else -> {
                            logger.info("$buildId|code type $webhookType and event type $webhookEventType ignored")
                        }
                    }
                }
            }
        }
    }

    private fun execute(
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String,
        triggerType: String,
        action: (GitCommitCheckInfo) -> Unit
    ) {
        if (triggerType != StartType.WEB_HOOK.name) {
            logger.info("Process instance($buildId) is not web hook triggered")
            return
        }

        try {
            val buildHistoryResult = client.get(ServiceBuildResource::class).getBuildVars(
                userId = userId, projectId = projectId,
                pipelineId = pipelineId, buildId = buildId, channelCode = ChannelCode.GIT
            )

            if (buildHistoryResult.isNotOk() || buildHistoryResult.data == null) {
                logger.warn("Process instance($buildId) not exist: ${buildHistoryResult.message}")
                return
            }
            val buildInfo = buildHistoryResult.data!!

            val variables = buildInfo.variables
            if (variables.isEmpty()) {
                logger.warn("Process instance($buildId) variables is empty")
                return
            }

            if (variables[PIPELINE_START_CHANNEL] != ChannelCode.BS.name) {
                logger.warn("Process instance($buildId) is not bs channel")
                return
            }

            val commitId = variables[PIPELINE_WEBHOOK_REVISION]
            var repositoryId = variables[PIPELINE_WEBHOOK_REPO]
            if (repositoryId.isNullOrBlank()) {
                // 兼容老的V1的
                repositoryId = variables["hookRepo"]
            }
            val repositoryType = RepositoryType.valueOf(variables[PIPELINE_WEBHOOK_REPO_TYPE] ?: RepositoryType.ID.name)
            if (commitId.isNullOrEmpty() || repositoryId.isNullOrEmpty()) {
                logger.warn("Some variable is null or empty. " +
                    "commitId($commitId) repoHashId($repositoryId) repositoryType($repositoryType)")
            }

            val repositoryConfig = when (repositoryType) {
                RepositoryType.ID -> RepositoryConfig(repositoryId, null, repositoryType)
                RepositoryType.NAME -> RepositoryConfig(null, repositoryId, repositoryType)
            }

            val webhookTypeStr = variables[PIPELINE_WEBHOOK_TYPE]
            val webhookEventTypeStr = variables[PIPELINE_WEBHOOK_EVENT_TYPE]

            logger.info("Code web hook service hookType($webhookTypeStr) and eventType($webhookEventTypeStr)")
            if (webhookTypeStr.isNullOrEmpty() || webhookEventTypeStr.isNullOrEmpty()) {
                logger.info("Process instance($buildId) is not web hook triggered")
                return
            }

            val block = variables[PIPELINE_WEBHOOK_BLOCK]?.toBoolean() ?: false
            val mrId = variables[PIPELINE_WEBHOOK_MR_ID]?.toLong()
            val targetBranch = variables[BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH]
            val enableCheck = variables[BK_REPO_GIT_WEBHOOK_ENABLE_CHECK]?.toBoolean() ?: true
            if (CodeEventType.valueOf(webhookEventTypeStr) == CodeEventType.MERGE_REQUEST && targetBranch == null) {
                logger.warn(
                    "the webhook info miss targetBranch,commit check may not be added," +
                        "pipelineId($pipelineId)," +
                        "buildId($buildId)," +
                        "commitId($commitId)"
                )
            }
            action(
                GitCommitCheckInfo(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    repositoryConfig = repositoryConfig,
                    commitId = commitId!!,
                    block = block,
                    triggerType = triggerType,
                    mergeRequestId = mrId,
                    userId = userId,
                    webhookType = webhookTypeStr,
                    webhookEventType = webhookEventTypeStr,
                    enableCheck = enableCheck,
                    targetBranch = targetBranch
                )
            )
        } catch (ignore: Throwable) {
            logger.warn("[$buildId]|Code webhook fail", ignore)
        }
    }

    fun addGitCommitCheckEvent(gitCommitCheckEvent: GitCommitCheckEvent, delay: Int = 0) {
        logger.info("Add git commit check event($gitCommitCheckEvent)")
        gitCommitCheckEvent.delayMills = delay * 1000
        pipelineEventDispatcher.dispatch(gitCommitCheckEvent)
    }

    fun consumeGitCommitCheckEvent(event: GitCommitCheckEvent) {
        logger.info("Consume git commit check event($event)")

        try {
            event.retryTime--
            addGitCommitCheck(event)
        } catch (t: Throwable) {
            logger.warn("Consume git commit check fail. $event", t)
            when (event.retryTime) {
                2 -> addGitCommitCheckEvent(event, 5)
                1 -> addGitCommitCheckEvent(event, 10)
                0 -> addGitCommitCheckEvent(event, 30)
                else -> {
                    logger.warn("Consume git commit check retry fail")
                }
            }
        }
    }

    private fun addGitCommitCheck(event: GitCommitCheckEvent) {
        with(event) {
            logger.info(
                "Code web hook add commit check [projectId=$projectId, pipelineId=$pipelineId, buildId=$buildId, " +
                    "repoHashId=$repositoryConfig, commitId=$commitId, state=$state, block=$block]"
            )

            val buildHistoryResult = client.get(ServiceBuildResource::class).getBuildVars(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                channelCode = ChannelCode.GIT
            )

            if (buildHistoryResult.isNotOk() || buildHistoryResult.data == null) {
                logger.warn("Process instance($buildId) not exist: ${buildHistoryResult.message}")
                return
            }
            val buildInfo = buildHistoryResult.data!!

            val variables = buildInfo.variables
            if (variables.isEmpty()) {
                logger.warn("Process instance($buildId) variables is empty")
                return
            }

            val pipelineName = buildInfo.pipelineName
            val buildNum = variables[PIPELINE_BUILD_NUM]
            val webhookEventType = variables[BK_REPO_GIT_WEBHOOK_EVENT_TYPE]
            val context = "$pipelineName@$webhookEventType"

            if (buildNum == null) {
                logger.warn("Build($buildId) number is null")
                return
            }

            val serverHost = HomeHostUtil.innerServerHost()
            val targetUrl = "$serverHost/console/pipeline/$projectId/$pipelineId/detail/$buildId"
            val description = when (state) {
                GIT_COMMIT_CHECK_STATE_PENDING -> "Your pipeline [$pipelineName] is running"
                GIT_COMMIT_CHECK_STATE_ERROR -> "Your pipeline [$pipelineName] is failed"
                GIT_COMMIT_CHECK_STATE_FAILURE -> "Your pipeline [$pipelineName] is failed"
                GIT_COMMIT_CHECK_STATE_SUCCESS -> "Your pipeline [$pipelineName] is succeed"
                else -> ""
            }

            while (true) {
                val lockKey = "code_git_commit_check_lock_$pipelineId"
                val redisLock = RedisLock(redisOperation, lockKey, 60)

                redisLock.use {
                    if (!redisLock.tryLock()) {
                        logger.info("Code web hook commit check try lock($lockKey) fail")
                        Thread.sleep(100)
                        return@use
                    }
                    // 优先使用存在目标分支的信息
                    val record = pluginGitCheckDao.getOrNull(
                        dslContext = dslContext,
                        pipelineId = pipelineId,
                        repositoryConfig = repositoryConfig,
                        commitId = commitId,
                        context = context,
                        targetBranch = targetBranch
                    ) ?: pluginGitCheckDao.getOrNull(
                        dslContext = dslContext,
                        pipelineId = pipelineId,
                        repositoryConfig = repositoryConfig,
                        commitId = commitId,
                        context = context,
                        targetBranch = null
                    )
                    // 新提交，直接添加commit check
                    if (record == null) {
                        scmCheckService.addGitCommitCheck(
                            event = event,
                            targetUrl = targetUrl,
                            context = context,
                            description = description,
                            targetBranch = if (targetBranch != null) {
                                mutableListOf(targetBranch!!)
                            } else {
                                null
                            }
                        )
                        pluginGitCheckDao.create(
                            dslContext = dslContext,
                            pluginGitCheck = PluginGitCheck(
                                pipelineId = pipelineId,
                                buildNumber = buildNum.toInt(),
                                repositoryHashId = repositoryConfig.repositoryHashId,
                                repositoryName = repositoryConfig.repositoryName,
                                commitId = commitId,
                                context = context,
                                targetBranch = targetBranch
                            )
                        )
                    } else {
                        // 旧数据，更新T_PLUGIN_GIT_CHECK
                        updateCommitCheck(
                            buildNum = buildNum,
                            record = record,
                            event = event,
                            targetUrl = targetUrl,
                            pipelineName = pipelineName,
                            description = description
                        )
                    }
                    // mr锁定并且状态为pending时才需要解锁hook锁
                    if (block && state == GIT_COMMIT_CHECK_STATE_PENDING) {
                        gitWebhookUnlockService.addUnlockHookLockEvent(projectId, variables)
                    }
                    return
                }
            }
        }
    }

    private fun updateCommitCheck(
        buildNum: String,
        record: TPluginGitCheckRecord?,
        event: GitCommitCheckEvent,
        targetUrl: String,
        pipelineName: String,
        description: String
    ) {
        if (record == null) {
            logger.warn("Illegal pluginGitCheck data,Failed to add commit check information")
            return
        }
        if (buildNum.toInt() >= record.buildNumber) {
            scmCheckService.addGitCommitCheck(
                event = event,
                targetUrl = targetUrl,
                context = record.context ?: pipelineName,
                description = description,
                targetBranch = if (record.targetBranch != null) {
                    mutableListOf(record.targetBranch)
                } else {
                    null
                }
            )
            pluginGitCheckDao.update(
                dslContext = dslContext,
                id = record.id,
                buildNumber = buildNum.toInt()
            )
        } else {
            logger.info("Code web hook commit check has bigger build number(${record.buildNumber})")
        }
    }

    fun addGithubPrEvent(githubPrEvent: GithubPrEvent, delay: Int = 0) {
        logger.info("Add github pr event($githubPrEvent)")
        githubPrEvent.delayMills = delay * 1000
        pipelineEventDispatcher.dispatch(githubPrEvent)
    }

    fun consumeGitHubPrEvent(event: GithubPrEvent) {
        logger.info("Consume github pr event($event)")

        try {
            val startedAt = if (event.startedAt != null) {
                LocalDateTime.ofInstant(Instant.ofEpochSecond(event.startedAt!!), ZoneId.systemDefault())
            } else {
                null
            }
            val completedAt = if (event.completedAt != null) {
                LocalDateTime.ofInstant(Instant.ofEpochSecond(event.completedAt!!), ZoneId.systemDefault())
            } else {
                null
            }

            event.retryTime--
            addGithubPullRequestCheck(
                userId = event.userId,
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                repositoryConfig = event.repositoryConfig,
                commitId = event.commitId,
                status = event.status,
                startedAt = startedAt,
                conclusion = event.conclusion,
                completedAt = completedAt
            )
        } catch (t: Throwable) {
            logger.warn("Consume github pr event fail. $event", t)
            when (event.retryTime) {
                2 -> addGithubPrEvent(event, 5)
                1 -> addGithubPrEvent(event, 10)
                0 -> addGithubPrEvent(event, 30)
                else -> {
                    logger.warn("Consume github pr event retry fail")
                }
            }
        }
    }

    private fun addGithubPullRequestCheck(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        repositoryConfig: RepositoryConfig,
        commitId: String,
        status: String,
        startedAt: LocalDateTime?,
        conclusion: String?,
        completedAt: LocalDateTime?
    ) {
        logger.info(
            "Code web hook add pr check [projectId=$projectId, pipelineId=$pipelineId, buildId=$buildId, " +
                "repo=$repositoryConfig, commitId=$commitId, status=$status]"
        )

        val buildHistoryResult = client.get(ServiceBuildResource::class).getBuildVars(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId
        )

        if (buildHistoryResult.isNotOk() || buildHistoryResult.data == null) {
            logger.warn("Process instance($buildId) not exist: ${buildHistoryResult.message}")
            return
        }
        val buildInfo = buildHistoryResult.data!!

        val variables = buildInfo.variables
        if (variables.isEmpty()) {
            logger.warn("Process instance($buildId) variables is empty")
            return
        }
        val buildNum = variables[PIPELINE_BUILD_NUM]

        if (buildNum == null) {
            logger.warn("Build($buildId) number is null")
            return
        }

        val pipelineName = buildInfo.pipelineName
        val webhookEventType = variables[BK_REPO_GIT_WEBHOOK_EVENT_TYPE]
        val name = "$pipelineName@$webhookEventType"
        val detailUrl = "${HomeHostUtil.innerServerHost()}/console/pipeline/$projectId/$pipelineId/detail/$buildId"

        while (true) {
            val lockKey = "code_github_check_run_lock_$pipelineId"
            val redisLock = RedisLock(redisOperation, lockKey, 60)
            redisLock.use {
                if (!redisLock.tryLock()) {
                    logger.info("Code web hook check run try lock($lockKey) fail")
                    Thread.sleep(100)
                    return@use
                }

                val record = pluginGithubCheckDao.getOrNull(dslContext, pipelineId, repositoryConfig, commitId)
                if (record == null) {
                    val result = scmCheckService.addGithubCheckRuns(
                        projectId = projectId,
                        repositoryConfig = repositoryConfig,
                        name = name,
                        commitId = commitId,
                        detailUrl = detailUrl,
                        externalId = "${userId}_${projectId}_${pipelineId}_$buildId",
                        status = status,
                        startedAt = startedAt?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ISO_INSTANT),
                        conclusion = conclusion,
                        completedAt = completedAt?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ISO_INSTANT)
                    )
                    pluginGithubCheckDao.create(
                        dslContext = dslContext,
                        checkRun = GithubCheckRun(
                            pipelineId = pipelineId,
                            buildNumber = buildNum.toInt(),
                            repositoryConfig = repositoryConfig,
                            commitId = commitId,
                            checkRunId = result.id,
                            checkRunName = name
                        )
                    )
                } else {
                    if (buildNum.toInt() >= record.buildNumber) {
                        // 如果重试或者reopen，需要将状态重新置为in_progress
                        val checkRunId = if (conclusion == null) {
                            val result = scmCheckService.addGithubCheckRuns(
                                projectId = projectId,
                                repositoryConfig = repositoryConfig,
                                name = record.checkRunName ?: "$pipelineName #$buildNum",
                                commitId = commitId,
                                detailUrl = detailUrl,
                                externalId = "${userId}_${projectId}_${pipelineId}_$buildId",
                                status = status,
                                startedAt = startedAt?.atZone(ZoneId.systemDefault())?.format(
                                    DateTimeFormatter.ISO_INSTANT
                                ),
                                conclusion = conclusion,
                                completedAt = completedAt?.atZone(ZoneId.systemDefault())?.format(
                                    DateTimeFormatter.ISO_INSTANT
                                )
                            )
                            result.id
                        } else {
                            scmCheckService.updateGithubCheckRuns(
                                checkRunId = record.checkRunId,
                                projectId = projectId,
                                repositoryConfig = repositoryConfig,
                                // 兼容历史数据
                                name = record.checkRunName ?: "$pipelineName #$buildNum",
                                commitId = commitId,
                                detailUrl = detailUrl,
                                externalId = "${userId}_${projectId}_${pipelineId}_$buildId",
                                status = status,
                                startedAt = startedAt?.atZone(ZoneId.systemDefault())?.format(
                                    DateTimeFormatter.ISO_INSTANT
                                ),
                                conclusion = conclusion,
                                completedAt = completedAt?.atZone(ZoneId.systemDefault())?.format(
                                    DateTimeFormatter.ISO_INSTANT
                                )
                            )
                            record.checkRunId
                        }

                        pluginGithubCheckDao.update(dslContext, record.id, buildNum.toInt(), checkRunId)
                    } else {
                        logger.info("Code web hook check run has bigger build number(${record.buildNumber})")
                    }
                }

                return
            }
        }
    }
}
