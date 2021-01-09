package com.tencent.devops.process.engine.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStartBroadCastEvent
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.engine.dao.PipelineWebHookQueueDao
import com.tencent.devops.process.engine.pojo.PipelineWebHookQueue
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_QUEUE
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_SOURCE_BRANCH
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_SOURCE_PROJECT_ID
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_SOURCE_REPO_NAME
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_TARGET_BRANCH
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_TARGET_PROJECT_ID
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_TARGET_REPO_NAME
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

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
                logger.info("webhook queue ($buildId) is not web hook triggered")
                return
            }

            try {
                val buildHistoryResult = client.get(ServiceBuildResource::class).getBuildVars(
                    userId = event.userId, projectId = event.projectId,
                    pipelineId = event.pipelineId, buildId = buildId, channelCode = ChannelCode.GIT
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
                    logger.info("webhook queue|$projectId|$pipelineId|$buildId is running, delete it from branch queue")
                    pipelineWebHookQueueDao.deleteByBuildIds(
                        dslContext = dslContext,
                        buildIds = listOf(buildId)
                    )
                }
            } catch (t: Throwable) {
                logger.error("webhook queue failed on build start|$projectId|$pipelineId|$buildId", t)
            }
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
                        pipelineId = pipelineId,
                        sourceProjectId = sourceProjectId,
                        sourceBranch = sourceBranch,
                        targetProjectId = targetProjectId,
                        targetBranch = targetBranch
                    )
                    if (webHookBuildHistory != null && webHookBuildHistory.isNotEmpty()) {
                        webHookBuildHistory.forEach { queue ->
                            logger.info("webhook queue on webhook trigger|$projectId|$pipelineId|${queue.buildId} " +
                                "be canceled because of $buildId")
                            buildLogPrinter.addYellowLine(
                                buildId = queue.buildId,
                                message = "因【Git事件触发】插件中，" +
                                    "MR Request Hook设置了【同源同目标分支触发时，只保留最新触发的构建】配置，" +
                                    "该次构建已被新触发的构建" +
                                    "[<a target='_blank' href='${HomeHostUtil.innerServerHost()}/" +
                                    "console/pipeline/$projectId/$pipelineId/detail/$buildId'>$buildId</a>]覆盖",
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
                                buildIds = webHookBuildHistory.map { it.buildId }
                            )
                            pipelineWebHookQueueDao.save(
                                dslContext = context,
                                pipelineId = pipelineId,
                                sourceProjectId = sourceProjectId,
                                sourceRepoName = sourceRepoName,
                                sourceBranch = sourceBranch,
                                targetProjectId = targetProjectId,
                                targetRepoName = targetRepoName,
                                targetBranch = targetBranch,
                                buildId = buildId
                            )
                        }
                    } else {
                        pipelineWebHookQueueDao.save(
                            dslContext = dslContext,
                            pipelineId = pipelineId,
                            sourceProjectId = sourceProjectId,
                            sourceRepoName = sourceRepoName,
                            sourceBranch = sourceBranch,
                            targetProjectId = targetProjectId,
                            targetRepoName = targetRepoName,
                            targetBranch = targetBranch,
                            buildId = buildId
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
        val lock = RedisLock(
            redisOperation = redisOperation,
            lockKey = getRedisLockKey(
                pipelineId = pipelineId,
                sourceProjectId = sourceProjectId,
                sourceBranch = sourceBranch,
                targetProjectId = targetProjectId,
                targetBranch = targetBranch
            ),
            expiredTimeInSeconds = 30
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

    private fun getRedisLockKey(
        pipelineId: String,
        sourceProjectId: Long,
        sourceBranch: String,
        targetProjectId: Long,
        targetBranch: String
    ): String {
        return "lock:webhook:queue:$pipelineId:$sourceProjectId:$sourceBranch:$targetProjectId:$targetBranch"
    }
}
