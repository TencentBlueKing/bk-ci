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
import com.tencent.devops.process.trigger.pojo.PipelineBuildCheckRunStatus
import com.tencent.devops.process.trigger.pojo.PipelineCheckRunLock
import com.tencent.devops.repository.api.scm.ServiceScmRepositoryApiResource
import com.tencent.devops.scm.api.enums.CheckRunConclusion
import com.tencent.devops.scm.api.enums.CheckRunStatus
import com.tencent.devops.scm.api.pojo.CheckRun
import com.tencent.devops.scm.api.pojo.CheckRunInput
import com.tencent.devops.scm.api.pojo.CheckRunOutput
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
@SuppressWarnings("ALL")
class PipelineBuildCheckRunService @Autowired constructor(
    private val pipelineBuildCheckRunDao: PipelineBuildCheckRunDao,
    private val pipelineBuildCheckRunQualityService: PipelineBuildCheckRunQualityService,
    private val redisOperation: RedisOperation,
    private val client: Client,
    private val dslContext: DSLContext,
    private val sampleEventDispatcher: SampleEventDispatcher,
    private val pipelineBuildCheckRunResolver: PipelineBuildCheckRunResolver
) {
    fun onBuildQueue(event: PipelineBuildQueueBroadCastEvent) {
        with(event) {
            handleBuildEvent(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                triggerType = triggerType,
                buildStatus = BuildStatus.RUNNING
            )
        }
    }

    fun onBuildFinished(event: PipelineBuildFinishBroadCastEvent) {
        with(event) {
            val buildStatus = BuildStatus.valueOf(status)
            handleBuildEvent(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                triggerType = triggerType,
                buildStatus = buildStatus
            )
        }
    }

    /**
     * 处理构建事件,收到构建排队和结束时间,将数据插入到checkRun表中,checkRunStatus都为PENDING
     */
    private fun handleBuildEvent(
        projectId: String,
        pipelineId: String,
        buildId: String,
        triggerType: String,
        buildStatus: BuildStatus
    ) {
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
        logger.info("handle build event|$projectId|$pipelineId|$buildId|$triggerType|$buildStatus")
        val lock = PipelineBuildCheckRunLock(redisOperation = redisOperation, buildId = buildId)
        try {
            lock.lock()
            val pipelineBuildCheckRun = PipelineBuildCheckRun(
                checkRunContext = checkRunContext
            )
            val buildCheckRun = pipelineBuildCheckRunDao.get(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId
            )
            if (buildCheckRun == null) {
                pipelineBuildCheckRunDao.create(
                    dslContext = dslContext,
                    buildCheckRun = pipelineBuildCheckRun
                )
            } else {
                pipelineBuildCheckRunDao.update(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    buildStatus = checkRunContext.buildStatus.name,
                    checkRunStatus = PipelineBuildCheckRunStatus.PENDING.name
                )
            }
            sampleEventDispatcher.dispatch(
                PipelineBuildCheckRunEvent(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    buildStatus = checkRunContext.buildStatus
                )
            )
        } catch (ignored: Exception) {
            logger.info(
                "Failed to handle build event|$projectId|$pipelineId|$buildId|$triggerType|$buildStatus",
                ignored
            )
            throw ignored
        } finally {
            lock.unlock()
        }
    }

    fun onBuildCheckRun(event: PipelineBuildCheckRunEvent) {
        with(event) {
            logger.info("handle build check run|$projectId|$pipelineId|$buildId|$buildStatus")
            val lock = PipelineCheckRunLock(redisOperation = redisOperation, pipelineId = pipelineId)
            try {
                lock.lock()
                val buildCheckRun = pipelineBuildCheckRunDao.get(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId
                )
                // 如果当前构建没有记录或者已经标记为跳过,则不处理
                if (buildCheckRun == null) {
                    logger.info("pipeline build check run not found|$projectId|$pipelineId|$buildId|$buildStatus")
                    return
                }
                val isFinished = buildCheckRun.checkRunStatus?.let {
                    PipelineBuildCheckRunStatus.valueOf(it).isFinished()
                } ?: false
                if (isFinished) {
                    logger.info(
                        "pipeline build check run finished|" +
                                "$projectId|$pipelineId|$buildId|${buildCheckRun.checkRunStatus}"
                    )
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

                // 获取最新的构建检查,
                val latestCheckRunRecord = pipelineBuildCheckRunDao.getLatestCheckRun(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    repoHashId = buildCheckRun.repoHashId,
                    commitId = buildCheckRun.commitId,
                    pullRequestId = buildCheckRun.pullRequestId
                )
                when {
                    // 如果当前没有最新的构建检查或者最近的构建检查是当前构建,则写入当前构建到检查
                    // 如果当前构建的构建号大于最新的构建检查,则用当前构建覆盖构建检查
                    latestCheckRunRecord == null || buildCheckRun.buildNum >= latestCheckRunRecord.buildNum -> {
                        writeBuildCheckRun(
                            repoHashId = buildCheckRun.repoHashId,
                            checkRunId = buildCheckRun.checkRunId,
                            checkRunContext = checkRunContext
                        )
                    }

                    // 如果当前有正在运行的检查,且当前构建的构建号小于正在运行的检查的构建号,则需要跳过写入
                    else -> {
                        logger.info(
                            "build check run has bigger build number,skip|" +
                                    "${latestCheckRunRecord.buildId}|(${latestCheckRunRecord.buildNum})"
                        )
                        pipelineBuildCheckRunDao.update(
                            dslContext = dslContext,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            checkRunId = null,
                            checkRunStatus = PipelineBuildCheckRunStatus.SKIP.name
                        )
                    }
                }
            } catch (ignored: Exception) {
                logger.info(
                    "Failed to handle build check run event|$projectId|$pipelineId|$buildId|$buildStatus",
                    ignored
                )
                throw ignored
            } finally {
                lock.unlock()
            }
        }
    }

    /**
     * 写入
     */
    private fun PipelineBuildCheckRunEvent.writeBuildCheckRun(
        repoHashId: String,
        checkRunId: Long? = null,
        checkRunContext: PipelineBuildCheckRunContext
    ) {
        val buildCheckRunStatus = if (buildStatus == BuildStatus.RUNNING) {
            PipelineBuildCheckRunStatus.RUNNING
        } else {
            PipelineBuildCheckRunStatus.SUCCESS
        }
        val repositoryConfig = RepositoryConfigUtils.buildConfig(
            repositoryId = repoHashId,
            repositoryType = RepositoryType.ID
        )
        val checkRunInput = checkRunContext.convertCheckRunInput(checkRunId)
        // 重试
        val checkRun = if (checkRunId != null && buildStatus.isFinish()) {
            updateCheckRun(
                projectId = projectId,
                repositoryConfig = repositoryConfig,
                checkRunInput = checkRunInput
            )
        } else {
            addCheckRun(
                projectId = projectId,
                repositoryConfig = repositoryConfig,
                checkRunInput = checkRunInput
            )
        }
        if (checkRun != null) {
            pipelineBuildCheckRunDao.update(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                checkRunId = checkRun.id,
                checkRunStatus = buildCheckRunStatus.name
            )
        } else {
            pipelineBuildCheckRunDao.update(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                checkRunStatus = PipelineBuildCheckRunStatus.FAILED.name
            )
        }
    }

    private fun PipelineBuildCheckRunContext.convertCheckRunInput(id: Long? = null): CheckRunInput {
        val (checkRunState, checkRunConclusion) = getCheckRunState(buildStatus)
        // 执行结束后尝试获取报表
        val quality = if (buildStatus != BuildStatus.RUNNING && supportQuality(eventType)) {
            pipelineBuildCheckRunQualityService.getQuality(
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
        private val logger = LoggerFactory.getLogger(PipelineBuildCheckRunService::class.java)
    }
}
