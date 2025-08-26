package com.tencent.devops.process.trigger.check

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQueueBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.PipelineBuildCheckRunDao
import com.tencent.devops.process.trigger.event.PipelineBuildCheckRunEvent
import com.tencent.devops.process.trigger.pojo.PipelineBuildCheckRun
import com.tencent.devops.process.trigger.pojo.PipelineBuildCheckRunContext
import com.tencent.devops.process.trigger.pojo.PipelineBuildCheckRunLock
import com.tencent.devops.process.trigger.pojo.PipelineCheckRunLock
import com.tencent.devops.repository.api.scm.ServiceScmRepositoryApiResource
import com.tencent.devops.scm.api.enums.CheckRunConclusion
import com.tencent.devops.scm.api.enums.CheckRunStatus
import com.tencent.devops.scm.api.pojo.CheckRun
import com.tencent.devops.scm.api.pojo.CheckRunInput
import com.tencent.devops.scm.api.pojo.CheckRunOutput
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
@SuppressWarnings("ALL")
class PipelineCheckRunService @Autowired constructor(
    private val pipelineBuildCheckRunDao: PipelineBuildCheckRunDao,
    private val pipelineCheckRunQualityService: PipelineCheckRunQualityService,
    private val redisOperation: RedisOperation,
    private val client: Client,
    private val dslContext: DSLContext,
    private val sampleEventDispatcher: SampleEventDispatcher,
    private val pipelineBuildCheckRunResolver: PipelineBuildCheckRunResolver
) {
    fun onBuildQueue(event: PipelineBuildQueueBroadCastEvent) {
        with(event) {
            val checkRunContext = pipelineBuildCheckRunResolver.resolveVar(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                triggerType = triggerType,
                buildStatus = BuildStatus.RUNNING
            )
            if (checkRunContext == null) {
                return
            }
            val lock = PipelineBuildCheckRunLock(redisOperation = redisOperation, buildId = buildId)
            try {
                lock.lock()
                val pipelineBuildCheckRun = PipelineBuildCheckRun(
                    checkRunContext = checkRunContext
                )
                pipelineBuildCheckRunDao.create(
                    dslContext = dslContext,
                    buildCheckRun = pipelineBuildCheckRun
                )
                sampleEventDispatcher.dispatch(
                    PipelineBuildCheckRunEvent(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        buildStatus = BuildStatus.RUNNING
                    )
                )
            } finally {
                lock.unlock()
            }
        }
    }

    fun onBuildFinished(event: PipelineBuildFinishBroadCastEvent) {
        with(event) {
            val buildStatus = BuildStatus.valueOf(status)
            val checkRunContext = pipelineBuildCheckRunResolver.resolveVar(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                triggerType = triggerType,
                buildStatus = buildStatus
            )
            if (checkRunContext == null) {
                return
            }
            val lock = PipelineBuildCheckRunLock(redisOperation, buildId)
            try {
                lock.lock()
                val buildCheckRun = pipelineBuildCheckRunDao.get(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId
                )
                if (buildCheckRun == null) {
                    logger.warn("")
                    return
                }
                pipelineBuildCheckRunDao.updateBuildStatus(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    buildStatus = checkRunContext.buildStatus.name
                )
                sampleEventDispatcher.dispatch(
                    PipelineBuildCheckRunEvent(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        buildStatus = checkRunContext.buildStatus
                    )
                )
            } finally {
                lock.unlock()
            }
        }
    }

    fun onBuildCheckRun(event: PipelineBuildCheckRunEvent) {
        with(event) {
            if (buildStatus == BuildStatus.RUNNING) {
                createBuildCheckRun(event = event)
            } else {
                updateBuildCheckRun(event = event)
            }
        }
    }

    private fun createBuildCheckRun(event: PipelineBuildCheckRunEvent) {
        with(event) {
            val lock = PipelineCheckRunLock(redisOperation = redisOperation, pipelineId = pipelineId)
            try {
                lock.lock()
                val buildCheckRun = pipelineBuildCheckRunDao.get(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId
                )
                if (buildCheckRun == null || BuildStatus.valueOf(buildCheckRun.buildStatus).isFinish()) {
                    return
                }
                val checkRunContext = pipelineBuildCheckRunResolver.resolveVar(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    triggerType = StartType.WEB_HOOK.name,
                    buildStatus = buildStatus
                )
                if (checkRunContext == null) {
                    return
                }
                val repositoryConfig = RepositoryConfigUtils.buildConfig(
                    repositoryId = buildCheckRun.repoHashId,
                    repositoryType = RepositoryType.ID
                )
                val runningCheckRunRecord = pipelineBuildCheckRunDao.get(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    repoHashId = buildCheckRun.repoHashId,
                    commitId = buildCheckRun.commitId,
                    pullRequestId = buildCheckRun.pullRequestId,
                    checkRunStatus = CheckRunStatus.IN_PROGRESS.name
                )

                when {
                    runningCheckRunRecord == null -> {
                        val checkRunInput = checkRunContext.createCheckRunInput()
                        val checkRun = addCheckRun(
                            projectId = projectId,
                            repositoryConfig = repositoryConfig,
                            checkRunInput = checkRunInput
                        )
                        pipelineBuildCheckRunDao.update(
                            dslContext = dslContext,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            checkRunId = checkRun?.id,
                            checkRunStatus = CheckRunStatus.IN_PROGRESS.name
                        )
                    }

                    buildCheckRun.buildNum > runningCheckRunRecord.buildNum -> {
                        val checkRunInput = checkRunContext.createCheckRunInput(id = runningCheckRunRecord.checkRunId)
                        updateCheckRun(
                            projectId = projectId,
                            repositoryConfig = repositoryConfig,
                            checkRunInput = checkRunInput.copy(id = runningCheckRunRecord.checkRunId)
                        )
                        dslContext.transaction { configuration ->
                            val transactionContext = DSL.using(configuration)
                            // 需要将历史的构建,重置为跳过
                            pipelineBuildCheckRunDao.update(
                                dslContext = transactionContext,
                                projectId = projectId,
                                pipelineId = pipelineId,
                                buildId = runningCheckRunRecord.buildId,
                                checkRunId = null,
                                checkRunStatus = CheckRunStatus.COMPLETED.name
                            )
                            pipelineBuildCheckRunDao.update(
                                dslContext = transactionContext,
                                projectId = projectId,
                                pipelineId = pipelineId,
                                buildId = buildId,
                                checkRunId = null,
                                checkRunStatus = CheckRunStatus.IN_PROGRESS.name
                            )
                        }
                    }

                    else -> {
                        pipelineBuildCheckRunDao.update(
                            dslContext = dslContext,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            checkRunId = null,
                            checkRunStatus = CheckRunStatus.COMPLETED.name
                        )
                    }
                }
            } finally {
                lock.unlock()
            }
        }
    }

    private fun updateBuildCheckRun(event: PipelineBuildCheckRunEvent) {
        with(event) {
            val lock = PipelineCheckRunLock(redisOperation = redisOperation, pipelineId = pipelineId)
            try {
                lock.lock()
                val buildCheckRun = pipelineBuildCheckRunDao.get(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId
                )
                if (buildCheckRun == null || BuildStatus.valueOf(buildCheckRun.buildStatus).isFinish()) {
                    logger.info("")
                    return
                }
                val checkRunContext = pipelineBuildCheckRunResolver.resolveVar(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    triggerType = StartType.WEB_HOOK.name,
                    buildStatus = buildStatus
                )
                if (checkRunContext == null) {
                    return
                }
                val checkRunInput = checkRunContext.createCheckRunInput()
                val repositoryConfig = RepositoryConfigUtils.buildConfig(
                    repositoryId = buildCheckRun.repoHashId,
                    repositoryType = RepositoryType.ID
                )
                updateCheckRun(
                    projectId = projectId,
                    repositoryConfig = repositoryConfig,
                    checkRunInput = checkRunInput
                )
                pipelineBuildCheckRunDao.update(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    checkRunId = null,
                    checkRunStatus = CheckRunStatus.IN_PROGRESS.name
                )
            } finally {
                lock.unlock()
            }
        }
    }

    private fun PipelineBuildCheckRunContext.createCheckRunInput(id: Long? = null): CheckRunInput {
        val (checkRunState, checkRunConclusion) = getCheckRunState(buildStatus)
        // 执行结束后尝试获取报表
        val quality = if (buildStatus != BuildStatus.RUNNING && supportQuality(eventType)) {
            pipelineCheckRunQualityService.getQuality(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                buildStatus = buildStatus.name,
                startTime = startTime,
                triggerType = triggerType,
                channelCode = channelCode,
                pipelineName = pipelineName,
                detailUrl = buildUrl,
                variables = buildVariables
            )
        } else {
            null
        }
        return CheckRunInput(
            id = id,
            name = context,
            ref = commitId,
            status = checkRunState,
            conclusion = checkRunConclusion,
            pullRequestId = pullRequestId,
            targetBranches = targetBranches,
            startedAt = startTime.let {
                LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
            } ?: LocalDateTime.now(),
            block = block,
            output = CheckRunOutput(
                title = pipelineName,
                summary = getSummary(pipelineName, buildStatus),
                text = quality
            ),
            completedAt = if (buildStatus != BuildStatus.RUNNING) {
                LocalDateTime.now()
            } else {
                null
            },
            detailsUrl = buildUrl,
            externalId = externalId
        )
    }

    /**
     * 获取check-run状态
     */
    private fun getCheckRunState(buildStatus: BuildStatus) = when (buildStatus) {
        BuildStatus.RUNNING -> CheckRunStatus.IN_PROGRESS to null
        BuildStatus.SUCCEED -> CheckRunStatus.COMPLETED to CheckRunConclusion.SUCCESS
        else -> CheckRunStatus.COMPLETED to CheckRunConclusion.FAILURE
    }

    private fun supportQuality(eventType: CodeEventType?) = listOf(
        CodeEventType.PULL_REQUEST,
        CodeEventType.MERGE_REQUEST,
        CodeEventType.MERGE_REQUEST_ACCEPT
    ).contains(eventType)

    private fun getSummary(pipelineName: String, buildStatus: BuildStatus) = when (buildStatus) {
        BuildStatus.RUNNING -> "Your pipeline [$pipelineName] is running"
        BuildStatus.SUCCEED -> "Your pipeline [$pipelineName] is succeed"
        else -> "Your pipeline [$pipelineName] is failed"
    }

    private fun addCheckRun(
        projectId: String,
        repositoryConfig: RepositoryConfig,
        checkRunInput: CheckRunInput
    ): CheckRun? {
        return try {
            client.get(ServiceScmRepositoryApiResource::class).createCheckRun(
                projectId,
                repoType = repositoryConfig.repositoryType,
                repoId = repositoryConfig.getRepositoryId(),
                checkRunInput = checkRunInput
            ).data
        } catch (ignored: Exception) {
            logger.warn("failed to add check-run", ignored)
            null
        }
    }

    private fun updateCheckRun(
        projectId: String,
        repositoryConfig: RepositoryConfig,
        checkRunInput: CheckRunInput
    ): CheckRun? {
        return try {
            client.get(ServiceScmRepositoryApiResource::class).updateCheckRun(
                projectId,
                repoType = repositoryConfig.repositoryType,
                repoId = repositoryConfig.getRepositoryId(),
                checkRunInput = checkRunInput
            ).data
        } catch (ignored: Exception) {
            logger.warn("failed to update check-run", ignored)
            null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineCheckRunService::class.java)
    }
}
