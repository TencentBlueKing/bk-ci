package com.tencent.devops.process.trigger.scm

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.common.webhook.service.code.pojo.EventRepositoryCache
import com.tencent.devops.common.webhook.util.EventCacheUtil
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.webhook.WebhookTriggerPipeline
import com.tencent.devops.process.trigger.event.ReplayWebhookEvent
import com.tencent.devops.process.trigger.yaml.PipelineYamlService
import com.tencent.devops.repository.api.ServiceRepositoryPacResource
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * SCM 老链路：查找订阅流水线，并按流水线批量投递
 */
@Service
class PipelineBuildWebhookMatchService @Autowired constructor(
    private val client: Client,
    private val pipelineWebhookService: PipelineWebhookService,
    private val pipelineYamlService: PipelineYamlService,
    private val gitWebhookUnlockDispatcher: GitWebhookUnlockDispatcher,
    private val webhookTriggerExecutor: PipelineBuildWebhookExecutor,
    private val pipelineBuildWebhookTriggerService: PipelineBuildWebhookTriggerService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildWebhookMatchService::class.java)

        // 单条流水线触发超时上限，防止 MQ 消费线程被卡死的 worker 永久阻塞
        private const val TRIGGER_TIMEOUT_MINUTES = 3L
    }

    /**
     * 单次 Webhook 事件内允许并行触发的流水线数量上限
     *
     * 落地为 dispatchTriggerPipelines 内部的本地信号量，避免一次 Webhook
     * 因触发的流水线过多而占满整个 [PipelineBuildWebhookExecutor] 线程池，
     * 影响其他 Webhook 事件的处理
     */
    @Value("\${scm.webhook.trigger.max-concurrent-per-request:2}")
    private var maxConcurrentPerRequest: Int = 2

    fun trigger(
        scmType: ScmType,
        matcher: ScmWebhookMatcher,
        requestId: String,
        eventTime: LocalDateTime
    ) {
        val preMatch = matcher.preMatch()
        if (!preMatch.isMatch) {
            logger.info("webhook trigger pre match|${preMatch.reason}")
            return
        }
        val triggerEvent = PipelineTriggerEvent(
            requestId = requestId,
            triggerType = scmType.name,
            eventType = matcher.getEventType().name,
            triggerUser = matcher.getUsername(),
            eventDesc = matcher.getEventDesc(),
            createTime = eventTime
        )
        // yaml流水线在yaml触发时触发
        val yamlPipelineIds = getYamlPipelineIds(matcher, scmType)
        val triggerPipelines = pipelineWebhookService.getTriggerPipelines(
            name = matcher.getRepoName(),
            repositoryType = scmType,
            yamlPipelineIds = yamlPipelineIds,
            compatibilityRepoNames = matcher.getCompatibilityRepoName()
        )
        dispatchTriggerPipelines(
            matcher = matcher,
            triggerEvent = triggerEvent,
            triggerPipelines = triggerPipelines
        )
    }

    fun getYamlPipelineIds(
        matcher: ScmWebhookMatcher,
        scmType: ScmType
    ): List<String> {
        try {
            val repository = client.get(ServiceRepositoryPacResource::class).getPacRepository(
                externalId = matcher.getExternalId(), scmType = scmType
            ).data
            if (repository == null || repository.projectId.isNullOrBlank() || repository.repoHashId.isNullOrBlank()) {
                return emptyList()
            }
            return pipelineYamlService.getAllYamlPipeline(
                projectId = repository.projectId!!,
                repoHashId = repository.repoHashId!!
            ).map { it.pipelineId }
        } catch (ignored: Exception) {
            logger.info("get yaml pipelineId error when webhook trigger", ignored)
            return emptyList()
        }
    }

    fun replay(
        replayEvent: ReplayWebhookEvent,
        triggerEvent: PipelineTriggerEvent,
        matcher: ScmWebhookMatcher
    ) {
        val preMatch = matcher.preMatch()
        if (!preMatch.isMatch) {
            logger.info("webhook replay trigger pre match|${preMatch.reason}")
            return
        }

        val triggerPipelines = with(replayEvent) {
            pipelineId?.let {
                listOf(
                    WebhookTriggerPipeline(
                        projectId = projectId,
                        pipelineId = pipelineId!!
                    )
                )
            } ?: run {
                pipelineWebhookService.listTriggerPipeline(
                    projectId = projectId,
                    repositoryHashId = triggerEvent.eventSource!!,
                    eventType = triggerEvent.eventType
                )
            }
        }
        dispatchTriggerPipelines(
            matcher = matcher,
            triggerEvent = triggerEvent,
            triggerPipelines = triggerPipelines
        )
    }

    fun dispatchTriggerPipelines(
        matcher: ScmWebhookMatcher,
        triggerEvent: PipelineTriggerEvent,
        triggerPipelines: List<WebhookTriggerPipeline>
    ): Boolean {
        val watcher = Watcher(
            "old WebhookDispatch|${matcher.getRepoName()}|${triggerPipelines.size}"
        )
        // 主线程只持有共享 Map，不绑定 ThreadLocal，避免 CallerRunsPolicy 场景污染主线程状态
        val sharedEventCache = ConcurrentHashMap<String, EventRepositoryCache>()
        val traceId = MDC.get(TraceTag.BIZID)
        try {
            logger.info("dispatch pipeline webhook subscriber|repo(${matcher.getRepoName()})")
            if (triggerPipelines.isEmpty()) {
                gitWebhookUnlockDispatcher.dispatchUnlockHookLockEvent(matcher)
                return false
            }

            // 代码库触发的事件ID,一个代码库会触发多条流水线,但应该只有一条触发事件
            val repoEventIdMap = ConcurrentHashMap<String, Long>()
            if (triggerPipelines.size >= 50) {
                logger.warn(
                    "Repository webhook triggered too many pipelines|" +
                        "${matcher.getRepoName()}|${triggerPipelines.size} pipelines triggered"
                )
            }
            // 本次 Webhook 事件内的并发闸门：单次 dispatch 最多 maxConcurrentPerRequest 条流水线并行触发
            // 使用 fair 模式保证 FIFO，避免某些流水线长时间饥饿
            val perRequestLimiter = Semaphore(maxConcurrentPerRequest, true)
            watcher.start("dispatch trigger pipelines")
            // 主线程在 acquire 阻塞：限制提交到线程池的并发数 = maxConcurrentPerRequest，避免单个事件吃满线程池
            // 等待者只可能是本次 dispatch 内已提交的同伴，无超时是为了保证所有流水线都能被触发
            val futures = triggerPipelines.map { subscriber ->
                perRequestLimiter.acquire()
                webhookTriggerExecutor.submit {
                    MDC.put(TraceTag.BIZID, traceId)
                    EventCacheUtil.bindSharedEventCache(sharedEventCache)
                    try {
                        pipelineBuildWebhookTriggerService.triggerSinglePipeline(
                            subscriber = subscriber,
                            matcher = matcher,
                            triggerEvent = triggerEvent,
                            repoEventIdMap = repoEventIdMap
                        )
                    } finally {
                        EventCacheUtil.remove()
                        perRequestLimiter.release()
                    }
                }
            }
            // 单条触发加超时兜底，防止 worker 被下游卡死时 MQ 消费线程永久阻塞
            futures.forEach { future ->
                try {
                    future.get(TRIGGER_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                } catch (e: TimeoutException) {
                    logger.warn(
                        "webhook trigger timeout|repo(${matcher.getRepoName()})|" +
                            "timeout=${TRIGGER_TIMEOUT_MINUTES}min",
                        e
                    )
                    future.cancel(true)
                }
            }
            watcher.stop()
            /* #3131,当对mr的commit check有强依赖，但是蓝盾与git的commit check交互存在一定的时延，可以增加双重锁。
                git发起mr时锁住mr,称为webhook锁，由蓝盾主动发起解锁，解锁有三种情况：
                1. 仓库没有配置蓝盾的流水线，需要解锁
                2. 仓库配置了蓝盾流水线，但是流水线都不需要锁住mr，需要解锁
                3. 仓库配置了蓝盾流水线并且需要锁住mr，需要等commit check发送完成，再解锁
                 @see com.tencent.devops.plugin.service.git.CodeWebhookService.addGitCommitCheck
             */
            gitWebhookUnlockDispatcher.dispatchUnlockHookLockEvent(matcher)
            return true
        } finally {
            LogUtils.printCostTimeWE(watcher)
            if (logger.isDebugEnabled) {
                logger.debug(
                    "webhook event repository cache: ${JsonUtil.toJson(sharedEventCache, false)}"
                )
            }
        }
    }
}
