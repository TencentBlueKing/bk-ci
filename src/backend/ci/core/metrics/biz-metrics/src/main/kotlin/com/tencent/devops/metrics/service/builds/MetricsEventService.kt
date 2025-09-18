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
 *
 */

package com.tencent.devops.metrics.service.builds

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.KeyReplacement
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.ReplacementUtils
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.event.MetricsEvent
import com.tencent.devops.common.pipeline.type.BuildType.MACOS
import com.tencent.devops.common.pipeline.type.BuildType.PUBLIC_DEVCLOUD
import com.tencent.devops.common.pipeline.type.BuildType.THIRD_PARTY_AGENT_ENV
import com.tencent.devops.common.pipeline.type.BuildType.THIRD_PARTY_AGENT_ID
import com.tencent.devops.common.pipeline.type.BuildType.WINDOWS
import com.tencent.devops.common.pipeline.utils.EventUtils.toMetricsEventType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.RetryUtils
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import com.tencent.devops.metrics.config.MetricsUserConfig
import com.tencent.devops.metrics.pojo.po.MetricsEventPO
import com.tencent.devops.metrics.pojo.po.MetricsEventPO.Extra
import com.tencent.devops.metrics.pojo.po.MetricsEventPO.LEVEL
import com.tencent.devops.metrics.pojo.po.MetricsEventPO.NodeType
import com.tencent.devops.metrics.pojo.po.MetricsEventPO.NodeType.DEVCLOUD_DOCKER
import com.tencent.devops.metrics.pojo.po.MetricsEventPO.NodeType.DEVCLOUD_MACOS
import com.tencent.devops.metrics.pojo.po.MetricsEventPO.NodeType.DEVCLOUD_WINDOWS
import com.tencent.devops.metrics.pojo.po.MetricsEventPO.NodeType.SELF_HOST
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.api.service.ServiceVarResource
import io.micrometer.core.instrument.Gauge
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.time.LocalDateTime
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["metrics.user.enable"], havingValue = "true", matchIfMissing = false)
class MetricsEventService @Autowired constructor(
    @Qualifier("prometheusMeterRegistry")
    private val registry: PrometheusMeterRegistry,
    private val client: Client,
    private val metricsUserConfig: MetricsUserConfig,
    private val redisOperation: RedisOperation
) {
    private val queue: BlockingQueue<MetricsEventPO.Data> = ArrayBlockingQueue(10000) // 阻塞队列

    private val executor = Executors.newCachedThreadPool()

    companion object {
        val logger: Logger = LoggerFactory.getLogger(MetricsEventService::class.java)

        const val METRICS_EVENT_QUEUE_KEY = "metrics_event_queue_size"
        const val METRICS_EVENT_AGENT_CACHE_KEY = "metrics_event_agent_cache_size"
        const val METRICS_EVENT_ENV_CACHE_KEY = "metrics_event_env_cache_size"
        const val METRICS_EVENT_PIPELINE_VERSION_CACHE_KEY = "metrics_event_pipeline_version_cache_size"
        const val METRICS_EVENT_PIPELINE_CACHE_KEY = "metrics_event_pipeline_cache_size"
        const val BUILD_STATUS_KEY = "pipeline_status_info"
        const val BUILD_STAGE_STATUS_KEY = "pipeline_stage_status_info"
        const val BUILD_JOB_STATUS_KEY = "pipeline_job_status_info"
        const val BUILD_STEP_STATUS_KEY = "pipeline_step_status_info"
        const val BUFFER_SIZE = 10
        const val TIMEOUT = 10L
        const val BUILD_METRICS_WHITE_LIST_KEY = "build_metrics:white_list"
    }

    private val processes = mutableListOf<EventSendProcess>()

    private inner class EventSendProcess : Runnable {
        val buf = mutableListOf<MetricsEventPO.Data>()
        override fun run() {
            while (true) {
                /*当buf超过BUFFER_SIZE，或达到TIMEOUT限制时才上报数据*/
                val message = queue.poll(TIMEOUT, TimeUnit.SECONDS)
                if (message != null) {
                    buf.add(message)
                    if (buf.size < BUFFER_SIZE) continue
                }
                if (buf.isEmpty()) continue
                RetryUtils.execute(action = object : RetryUtils.Action<Unit> {
                    override fun execute() {
                        send()
                        buf.clear()
                    }

                    override fun fail(e: Throwable) {
                        logger.error("event send failed:${e.message}", e)
                        buf.clear()
                    }
                }, retryTime = 3, retryPeriodMills = 5000)
            }
        }

        fun send() {
            val body = JsonUtil.toJson(
                MetricsEventPO(
                    dataId = metricsUserConfig.eventDataId,
                    accessToken = metricsUserConfig.eventToken,
                    data = buf
                ),
                false
            )
            val request = Request.Builder()
                .url(metricsUserConfig.eventUrl)
                .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()

            OkhttpUtils.doHttp(request).use { response ->
                if (!response.isSuccessful) {
                    val responseContent = response.body!!.string()
                    logger.error("event send fail. $responseContent")
                }
            }
        }
    }

    fun init() {
        repeat(metricsUserConfig.eventConsumerCount) {
            val process = EventSendProcess()
            processes.add(process)
            executor.submit(process)
        }
        // 注册优雅关机钩子
        Runtime.getRuntime().addShutdownHook(Thread {
            processes.forEach { process ->
                try {
                    if (process.buf.isNotEmpty()) {
                        logger.info("Cleared buffer during shutdown|${process.buf.size}")
                        process.send()
                        process.buf.clear()
                        logger.info("Cleared buffer during shutdown done")
                    }
                } catch (e: Exception) {
                    logger.error("Failed to clear buffer during shutdown", e)
                }
            }
        })
        // 监测queue队列
        Gauge.builder(
            METRICS_EVENT_QUEUE_KEY,
            queue
        ) { cache -> cache.size.toDouble() }
            .register(registry)
        // 监测缓存大小
        Gauge.builder(
            METRICS_EVENT_AGENT_CACHE_KEY,
            agentCache
        ) { cache -> cache.estimatedSize().toDouble() }
            .register(registry)
        Gauge.builder(
            METRICS_EVENT_ENV_CACHE_KEY,
            envCache
        ) { cache -> cache.estimatedSize().toDouble() }
            .register(registry)
        Gauge.builder(
            METRICS_EVENT_PIPELINE_VERSION_CACHE_KEY,
            pipelineVersionCache
        ) { cache -> cache.estimatedSize().toDouble() }
            .register(registry)
        Gauge.builder(
            METRICS_EVENT_PIPELINE_CACHE_KEY,
            pipelineCache
        ) { cache -> cache.estimatedSize().toDouble() }
            .register(registry)
    }

    private val agentCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(12, TimeUnit.MINUTES)
        .build<String/*agentHashId*/, Map<String, String>> { key ->
            kotlin.runCatching {
                val (projectId, agentHashId) = key.split("@@")
                client.get(ServiceThirdPartyAgentResource::class).getNodeDetailSimple(
                    userId = "metrics",
                    projectId = projectId,
                    nodeHashId = null,
                    agentHashId = agentHashId,
                    checkPermission = false
                ).data?.let {
                    mapOf(
                        PipelineBuildStatusBroadCastEvent.Labels::hostIp.name to it.ip,
                        PipelineBuildStatusBroadCastEvent.Labels::hostName.name to it.displayName,
                        PipelineBuildStatusBroadCastEvent.Labels::hostOS.name to it.os,
                        PipelineBuildStatusBroadCastEvent.Labels::nodeHashId.name to it.nodeId
                    )
                }
            }.onFailure {
                logger.warn("cache agent failed", it)
            }.getOrNull() ?: emptyMap()
        }

    private val envCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(11, TimeUnit.MINUTES)
        .build<String/*envHashId*/, Map<String, String>> { key ->
            kotlin.runCatching {
                val (projectId, envHashId) = key.split("@@")
                client.get(ServiceEnvironmentResource::class)
                    .get(userId = "metrics", projectId = projectId, envHashId = envHashId, checkPermission = false)
                    .data?.let {
                        mapOf(
                            PipelineBuildStatusBroadCastEvent.Labels::dispatchIdentity.name to it.envHashId,
                            PipelineBuildStatusBroadCastEvent.Labels::dispatchName.name to it.name
                        )
                    }
            }.onFailure {
                logger.warn("cache env failed", it)
            }.getOrNull() ?: emptyMap()
        }

    private val pipelineVersionCache = Caffeine.newBuilder()
        .maximumSize(100000)
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .build<String, Int> { key ->
            kotlin.runCatching {
                val (projectId, buildId) = key.split("@@")
                client.get(ServiceBuildResource::class).getPipelineVersionFromBuildId(projectId, buildId).data
            }.getOrNull()
        }

    private val pipelineCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(15, TimeUnit.MINUTES)
        .build<String, Map<String, String>> { key ->
            kotlin.runCatching {
                val (projectId, pipelineId, pipelineVersion) = key.split("@@")
                cachePipeline(projectId, pipelineId, pipelineVersion.toIntOrNull())
            }.onFailure {
                logger.warn("cache pipeline failed", it)
            }.getOrNull()
        }

    @Suppress("NestedBlockDepth")
    private fun cachePipeline(projectId: String, pipelineId: String, pipelineVersion: Int?): Map<String, String> {
        val model = kotlin.runCatching {
            client.get(ServicePipelineResource::class).get(
                userId = "metrics",
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = ChannelCode.BS,
                version = pipelineVersion,
                checkPermission = false,
                includeDraft = true
            ).data
        }.onFailure {
            logger.warn("cache pipeline failed", it)
        }.getOrNull() ?: return emptyMap()
        val map = mutableMapOf<String, String>()
        map["${PipelineBuildStatusBroadCastEvent.Labels::pipelineName.name}.$pipelineId"] = model.name
        model.stages.forEach stage@{ stage ->
            stage.id?.let {
                map["${PipelineBuildStatusBroadCastEvent.Labels::stageName.name}.$it"] = stage.name ?: ""
            }
            stage.containers.forEach container@{ container ->
                container.jobId?.ifBlank { null }?.let {
                    map["${PipelineBuildStatusBroadCastEvent.Labels::jobName.name}.$it"] = container.name
                }
                container.containerHashId?.let {
                    map["${PipelineBuildStatusBroadCastEvent.Labels::jobName.name}.$it"] = container.name
                }
                container.elements.forEach { element ->
                    element.stepId?.ifBlank { null }?.let {
                        map["${PipelineBuildStatusBroadCastEvent.Labels::stepName.name}.$it"] = element.name
                    }
                    element.id?.ifBlank { null }?.let {
                        map["${PipelineBuildStatusBroadCastEvent.Labels::stepName.name}.$it"] = element.name
                    }
                }
                if (container is VMBuildContainer) {
                    val dispatch = container.dispatchType ?: return@container
                    val type = dispatch.buildType()
                    container.containerHashId?.let {
                        map["${PipelineBuildStatusBroadCastEvent.Labels::dispatchType.name}.$it"] = type.name
                    }
                    when (type) {
                        THIRD_PARTY_AGENT_ID -> {
                            container.containerHashId?.let {
                                val agentInfo = agentCache.get("$projectId@@${dispatch.value}")
                                map["${PipelineBuildStatusBroadCastEvent.Labels::dispatchIdentity.name}.$it"] =
                                    agentInfo[PipelineBuildStatusBroadCastEvent.Labels::nodeHashId.name]
                                        ?: dispatch.value
                                map["${PipelineBuildStatusBroadCastEvent.Labels::dispatchName.name}.$it"] =
                                    agentInfo[PipelineBuildStatusBroadCastEvent.Labels::hostName.name]
                                        ?: dispatch.value
                            }
                        }

                        THIRD_PARTY_AGENT_ENV -> {
                            container.containerHashId?.let {
                                val envInfo = envCache.get("$projectId@@${dispatch.value}")
                                map["${PipelineBuildStatusBroadCastEvent.Labels::dispatchIdentity.name}.$it"] =
                                    envInfo[PipelineBuildStatusBroadCastEvent.Labels::dispatchIdentity.name]
                                        ?: dispatch.value
                                map["${PipelineBuildStatusBroadCastEvent.Labels::dispatchName.name}.$it"] =
                                    envInfo[PipelineBuildStatusBroadCastEvent.Labels::dispatchName.name]
                                        ?: dispatch.value
                            }
                        }

                        PUBLIC_DEVCLOUD -> {
                            val dispatchMap = JsonUtil.toMap(dispatch)
                            container.containerHashId?.let {
                                map["${PipelineBuildStatusBroadCastEvent.Labels::dispatchIdentity.name}.$it"] =
                                    "${dispatchMap["imageCode"]}:${dispatchMap["imageVersion"]}"
                                map["${PipelineBuildStatusBroadCastEvent.Labels::dispatchName.name}.$it"] =
                                    "${dispatchMap["imageCode"]}:${dispatchMap["imageVersion"]}"
                            }
                        }

                        MACOS -> {
                            container.containerHashId?.let {
                                map["${PipelineBuildStatusBroadCastEvent.Labels::dispatchIdentity.name}.$it"] =
                                    dispatch.value
                                map["${PipelineBuildStatusBroadCastEvent.Labels::dispatchName.name}.$it"] =
                                    dispatch.value
                            }
                        }

                        WINDOWS -> {
                            container.containerHashId?.let {
                                map["${PipelineBuildStatusBroadCastEvent.Labels::dispatchIdentity.name}.$it"] =
                                    dispatch.value
                                map["${PipelineBuildStatusBroadCastEvent.Labels::dispatchName.name}.$it"] =
                                    dispatch.value
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
        return map
    }

    private fun getPipelineCache(projectId: String, pipelineId: String, buildId: String): Map<String, String> {
        val version = pipelineVersionCache.get("$projectId@@$buildId")
        return pipelineCache.get("$projectId@@$pipelineId@@$version")
    }

    private fun getPropertiesFromEnvironment(event: PipelineBuildStatusBroadCastEvent): Map<String, String>? {
        val agentHashId = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::agentId) ?: return null
        return agentCache.get("${event.projectId}@@$agentHashId")
    }

    private fun getPropertiesFromVariable(event: PipelineBuildStatusBroadCastEvent): Map<String, String>? {
        val trigger = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::trigger)
        if (trigger == StartType.WEB_HOOK.name) {
            return kotlin.runCatching {
                client.get(ServiceVarResource::class).getBuildVars(
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    buildId = event.buildId,
                    keys = MetricsEventPO.VARIABLES.values().map { it.name }.toSet()
                ).data
            }.getOrNull()
        }
        return null
    }

    private fun replaceVariable(command: String?, event: PipelineBuildStatusBroadCastEvent) = ReplacementUtils.replace(
        command = command ?: "",
        replacement = object : KeyReplacement {
            override fun getReplacement(key: String): String? {
                return kotlin.runCatching {
                    client.get(ServiceVarResource::class).getBuildVars(
                        projectId = event.projectId,
                        pipelineId = event.pipelineId,
                        buildId = event.buildId,
                        keys = setOf(key)
                    ).data?.get(key)
                }.getOrNull()
            }
        }
    )

    private fun priority(status: String?) = when (status) {
        BuildStatus.FAILED.name, BuildStatus.CANCELED.name -> "Warning"
        else -> "Normal"
    }

    private val eventEnableCache = Caffeine.newBuilder()
        .maximumSize(1)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, Set<String>> { key ->
            kotlin.runCatching {
                redisOperation.hkeys(key)
            }.getOrNull() ?: emptySet()
        }

    private fun check(event: PipelineBuildStatusBroadCastEvent): Boolean {
        val cache = eventEnableCache.get(BUILD_METRICS_WHITE_LIST_KEY)
        // redis 白名单
        return cache.isEmpty() || cache.contains(event.projectId)
    }

    // 事件
    fun eventExecute(event: PipelineBuildStatusBroadCastEvent) {
        if (!check(event) || !metricsUserConfig.metricsUserEnabled) return
        /*防止mq队列堆积导致的延迟信息进入处理，如果生产超过5分钟就丢弃*/
        if (event.eventTime != null && event.eventTime!! < LocalDateTime.now().plusMinutes(-5)) {
            logger.warn("event is too old, ignore it. event: ${event.buildId}|${event.type}")
            return
        }
        when (val eventType = event.toMetricsEventType()) {
            MetricsEvent.BUILD_QUEUE,
            MetricsEvent.BUILD_START,
            MetricsEvent.BUILD_END -> {
                registerBuildStatusEvent(
                    type = eventType,
                    event = event
                )
            }

            MetricsEvent.BUILD_STAGE_PAUSE,
            MetricsEvent.BUILD_STAGE_START,
            MetricsEvent.BUILD_STAGE_END -> {
                registerBuildStageStatusEvent(
                    type = eventType,
                    event = event
                )
            }

            MetricsEvent.BUILD_JOB_QUEUE,
            MetricsEvent.BUILD_JOB_START,
            MetricsEvent.BUILD_JOB_END -> {
                registerBuildJobStatusEvent(
                    type = eventType,
                    event = event
                )
            }

            MetricsEvent.BUILD_AGENT_START,
            MetricsEvent.BUILD_AGENT_END -> {
                registerBuildAgentStatusEvent(
                    type = eventType,
                    event = event
                )
            }

            MetricsEvent.BUILD_TASK_START,
            MetricsEvent.BUILD_TASK_END,
            MetricsEvent.BUILD_TASK_PAUSE -> {
                registerBuildStepStatusEvent(
                    type = eventType,
                    event = event
                )
            }

            MetricsEvent.BUILD_QUALITY -> {
                registerBuildQualityStatusEvent(
                    type = eventType,
                    event = event
                )
            }

            else -> {}
        }
    }

    private fun cacheGet(
        cache: Map<String, String>,
        event: PipelineBuildStatusBroadCastEvent,
        property: kotlin.reflect.KProperty1<PipelineBuildStatusBroadCastEvent.Labels, String>
    ): String {
        return when (property) {
            PipelineBuildStatusBroadCastEvent.Labels::pipelineName ->
                cache["${property.name}.${event.pipelineId}"]

            PipelineBuildStatusBroadCastEvent.Labels::stageName ->
                cache["${property.name}.${event.stageId}"]

            PipelineBuildStatusBroadCastEvent.Labels::jobName ->
                cache["${property.name}.${event.jobId}"] ?: cache["${property.name}.${event.containerHashId}"]

            PipelineBuildStatusBroadCastEvent.Labels::stepName ->
                event.stepId?.ifBlank { null }?.let { cache["${property.name}.${event.stepId}"] }
                    ?: cache["${property.name}.${event.taskId}"]

            PipelineBuildStatusBroadCastEvent.Labels::dispatchType ->
                cache["${property.name}.${event.containerHashId}"]

            PipelineBuildStatusBroadCastEvent.Labels::dispatchIdentity ->
                replaceVariable(cache["${property.name}.${event.containerHashId}"], event)

            PipelineBuildStatusBroadCastEvent.Labels::dispatchName ->
                replaceVariable(cache["${property.name}.${event.containerHashId}"], event)

            else -> null
        } ?: ""
    }

    @Suppress("NestedBlockDepth")
    private fun <T> labelGet(
        labels: Map<String, Any?>?,
        property: kotlin.reflect.KProperty1<PipelineBuildStatusBroadCastEvent.Labels, T>
    ): T? {
        if (labels == null) return null

        return when (val value = labels[property.name]) {
            null -> null
            else -> try {
                when (property.returnType.classifier) {
                    Long::class -> when (value) {
                        is String -> value.toLongOrNull()
                        is Long -> value
                        is Int -> value.toLong()
                        else -> value.toString().toLongOrNull()
                    }

                    else -> value
                }
            } catch (e: Exception) {
                logger.warn("Convert label value failed, property: ${property.name}, value: $value", e)
                null
            } as? T
        }
    }

    fun registerBuildStatusEvent(
        type: MetricsEvent,
        event: PipelineBuildStatusBroadCastEvent
    ) {
        if (metricsUserConfig.eventUrl.isBlank()) return
        val readPipelineCache = getPipelineCache(event.projectId, event.pipelineId, event.buildId)
        val properties = getPropertiesFromVariable(event)
        val dimension = MetricsEventPO.Dimension(
            executeCount = event.executeCount ?: 1,
            type = priority(event.buildStatus),
            level = LEVEL.PIPELINE,
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            pipelineName = cacheGet(readPipelineCache, event, PipelineBuildStatusBroadCastEvent.Labels::pipelineName),
            trigger = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::trigger),
            triggerUser = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::triggerUser),
            buildId = event.buildId,
            status = event.buildStatus ?: "",
            eventType = type,
            gitRepoUrl = properties?.get(MetricsEventPO.VARIABLES.GIT_CI_REPO_URL.name),
            gitType = properties?.get(MetricsEventPO.VARIABLES.BK_CI_HOOK_TYPE.name),
            gitBranchName = properties?.get(MetricsEventPO.VARIABLES.GIT_CI_REF.name),
            gitEventRrl = properties?.get(MetricsEventPO.VARIABLES.GIT_CI_EVENT_URL.name),
            gitEvent = properties?.get(MetricsEventPO.VARIABLES.GIT_CI_EVENT.name)
        )

        val extra = Extra(
            startTime = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::startTime),
            duration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::duration),
            queueDuration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::queueDuration),
            reviewDuration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::reviewDuration),
            executeDuration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::executeDuration),
            systemDuration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::systemDuration)
        )
        extra.check()

        val unavailable = queue.offer(
            MetricsEventPO.Data(
                eventName = BUILD_STATUS_KEY,
                event = MetricsEventPO.Event(
                    "build status for ${event.buildStatus}",
                    extra = extra
                ),
                dimension = dimension,
                timestamp = event.eventTime?.timestampmilli(),
                target = event.pipelineId
            )
        )
        if (!unavailable) {
            logger.warn("queue full and ignore")
        }
    }

    fun registerBuildStageStatusEvent(
        type: MetricsEvent,
        event: PipelineBuildStatusBroadCastEvent
    ) {
        if (metricsUserConfig.eventUrl.isBlank()) return
        val readPipelineCache = getPipelineCache(event.projectId, event.pipelineId, event.buildId)
        val dimension = MetricsEventPO.Dimension(
            executeCount = event.executeCount ?: 1,
            type = priority(event.buildStatus),
            level = LEVEL.STAGE,
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            pipelineName = cacheGet(readPipelineCache, event, PipelineBuildStatusBroadCastEvent.Labels::pipelineName),
            buildId = event.buildId,
            status = event.buildStatus ?: "",
            eventType = type,
            stageId = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::stageSeq)?.toString()
                ?: event.stageId ?: "",
            stageName = cacheGet(readPipelineCache, event, PipelineBuildStatusBroadCastEvent.Labels::stageName)
        )

        val extra = Extra(
            startTime = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::startTime),
            duration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::duration),
            queueDuration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::queueDuration),
            reviewDuration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::reviewDuration),
            executeDuration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::executeDuration),
            systemDuration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::systemDuration)
        )
        extra.check()

        val unavailable = queue.offer(
            MetricsEventPO.Data(
                eventName = BUILD_STAGE_STATUS_KEY,
                event = MetricsEventPO.Event(
                    "stage status for ${event.buildStatus}",
                    extra = extra
                ),
                dimension = dimension,
                timestamp = event.eventTime?.timestampmilli(),
                target = event.pipelineId
            )
        )
        if (!unavailable) {
            logger.warn("queue full and ignore")
        }
    }

    fun registerBuildJobStatusEvent(
        type: MetricsEvent,
        event: PipelineBuildStatusBroadCastEvent
    ) {
        if (metricsUserConfig.eventUrl.isBlank()) return
        val readPipelineCache = getPipelineCache(event.projectId, event.pipelineId, event.buildId)
        val dimension = MetricsEventPO.Dimension(
            executeCount = event.executeCount ?: 1,
            type = priority(event.buildStatus),
            level = LEVEL.JOB,
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            pipelineName = cacheGet(readPipelineCache, event, PipelineBuildStatusBroadCastEvent.Labels::pipelineName),
            buildId = event.buildId,
            status = event.buildStatus ?: "",
            eventType = type,
            stageId = event.stageId ?: "",
            stageName = cacheGet(readPipelineCache, event, PipelineBuildStatusBroadCastEvent.Labels::stageName),
            jobId = event.jobId ?: "",
            jobName = cacheGet(readPipelineCache, event, PipelineBuildStatusBroadCastEvent.Labels::jobName),
            dispatchType = cacheGet(readPipelineCache, event, PipelineBuildStatusBroadCastEvent.Labels::dispatchType),
            dispatchIdentity = cacheGet(
                cache = readPipelineCache,
                event = event,
                property = PipelineBuildStatusBroadCastEvent.Labels::dispatchIdentity
            ),
            dispatchName = cacheGet(readPipelineCache, event, PipelineBuildStatusBroadCastEvent.Labels::dispatchName),
            jobMutexType = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::jobMutexType),
            mutexGroup = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::mutexGroup),
            agentReuseMutex = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::agentReuseMutex),
            errorCode = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::errorCode),
            errorType = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::errorType),
            errorMessage = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::errorMessage)
        )

        val extra = Extra(
            startTime = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::startTime),
            duration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::duration),
            queueDuration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::queueDuration),
            reviewDuration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::reviewDuration),
            executeDuration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::executeDuration),
            systemDuration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::systemDuration)
        )
        extra.check()

        val unavailable = queue.offer(
            MetricsEventPO.Data(
                eventName = BUILD_JOB_STATUS_KEY,
                event = MetricsEventPO.Event(
                    "job status for ${event.buildStatus}",
                    extra = extra
                ),
                dimension = dimension,
                timestamp = event.eventTime?.timestampmilli(),
                target = event.pipelineId
            )
        )
        if (!unavailable) {
            logger.warn("queue full and ignore")
        }
    }

    fun registerBuildAgentStatusEvent(
        type: MetricsEvent,
        event: PipelineBuildStatusBroadCastEvent
    ) {
        if (metricsUserConfig.eventUrl.isBlank()) return
        val readPipelineCache = getPipelineCache(event.projectId, event.pipelineId, event.buildId)
        val nodeExtInfo = getPropertiesFromEnvironment(event)
        val nodeType = NodeType.getNodeType(
            labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::nodeType)
        )

        fun loadHostName(nodeType: NodeType) = when (nodeType) {
            SELF_HOST -> nodeExtInfo?.get(PipelineBuildStatusBroadCastEvent.Labels::hostName.name)
            DEVCLOUD_DOCKER -> labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::dockerContainerName)
            DEVCLOUD_MACOS -> null
            DEVCLOUD_WINDOWS -> null
        }

        fun loadHostIp(nodeType: NodeType) = when (nodeType) {
            SELF_HOST -> labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::hostIp)
            DEVCLOUD_DOCKER -> null
            DEVCLOUD_MACOS -> labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::macosIp)
            DEVCLOUD_WINDOWS -> labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::windowsIp)
        }

        fun loadHostOS(nodeType: NodeType) = when (nodeType) {
            SELF_HOST -> nodeExtInfo?.get(PipelineBuildStatusBroadCastEvent.Labels::hostOS.name)
            DEVCLOUD_DOCKER -> "LINUX"
            DEVCLOUD_MACOS -> "MACOS"
            DEVCLOUD_WINDOWS -> "WINDOWS"
        }

        val dimension = MetricsEventPO.Dimension(
            executeCount = event.executeCount ?: 1,
            type = priority(event.buildStatus),
            level = LEVEL.AGENT,
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            pipelineName = cacheGet(readPipelineCache, event, PipelineBuildStatusBroadCastEvent.Labels::pipelineName),
            buildId = event.buildId,
            status = event.buildStatus ?: "",
            eventType = type,
            stageId = event.stageId ?: "",
            stageName = cacheGet(readPipelineCache, event, PipelineBuildStatusBroadCastEvent.Labels::stageName),
            jobId = event.jobId ?: "",
            jobName = cacheGet(readPipelineCache, event, PipelineBuildStatusBroadCastEvent.Labels::jobName),
            nodeType = nodeType,
            hostName = nodeType?.let { loadHostName(it) },
            hostIp = nodeType?.let { loadHostIp(it) },
            hostOS = nodeType?.let { loadHostOS(it) }
        )

        val extra = Extra(
            startTime = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::startTime),
            duration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::duration),
            queueDuration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::queueDuration),
            reviewDuration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::reviewDuration),
            executeDuration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::executeDuration),
            systemDuration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::systemDuration)
        )
        extra.check()

        val unavailable = queue.offer(
            MetricsEventPO.Data(
                eventName = BUILD_JOB_STATUS_KEY,
                event = MetricsEventPO.Event(
                    "agent status for ${event.buildStatus}",
                    extra = extra
                ),
                dimension = dimension,
                timestamp = event.eventTime?.timestampmilli(),
                target = event.pipelineId
            )
        )
        if (!unavailable) {
            logger.warn("queue full and ignore")
        }
    }

    fun registerBuildStepStatusEvent(
        type: MetricsEvent,
        event: PipelineBuildStatusBroadCastEvent
    ) {
        if (metricsUserConfig.eventUrl.isBlank()) return
        val readPipelineCache = getPipelineCache(event.projectId, event.pipelineId, event.buildId)
        val specialStep = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::specialStep)
        val dimension = MetricsEventPO.Dimension(
            executeCount = event.executeCount ?: 1,
            type = priority(event.buildStatus),
            level = LEVEL.STEP,
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            pipelineName = cacheGet(readPipelineCache, event, PipelineBuildStatusBroadCastEvent.Labels::pipelineName),
            buildId = event.buildId,
            status = event.buildStatus ?: "",
            eventType = type,
            stageId = event.stageId ?: "",
            stageName = cacheGet(readPipelineCache, event, PipelineBuildStatusBroadCastEvent.Labels::stageName),
            jobId = event.jobId ?: "",
            jobName = cacheGet(readPipelineCache, event, PipelineBuildStatusBroadCastEvent.Labels::jobName),
            stepId = event.stepId ?: event.taskId ?: "",
            stepName = specialStep?.let { "DEVOPS_INNER_$it" }
                ?: labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::stepName)
                ?: cacheGet(readPipelineCache, event, PipelineBuildStatusBroadCastEvent.Labels::stepName),
            errorCode = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::errorCode),
            errorType = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::errorType),
            errorMessage = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::errorMessage)
        )

        val extra = Extra(
            startTime = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::startTime),
            duration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::duration),
            queueDuration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::queueDuration),
            reviewDuration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::reviewDuration),
            executeDuration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::executeDuration),
            systemDuration = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::systemDuration)
        )
        extra.check()

        val unavailable = queue.offer(
            MetricsEventPO.Data(
                eventName = BUILD_STEP_STATUS_KEY,
                event = MetricsEventPO.Event(
                    "step status for ${event.buildStatus}",
                    extra = extra
                ),
                dimension = dimension,
                timestamp = event.eventTime?.timestampmilli(),
                target = event.pipelineId
            )
        )
        if (!unavailable) {
            logger.warn("queue full and ignore")
        }
    }

    fun registerBuildQualityStatusEvent(
        type: MetricsEvent,
        event: PipelineBuildStatusBroadCastEvent
    ) {
        if (metricsUserConfig.eventUrl.isBlank()) return
        val readPipelineCache = getPipelineCache(event.projectId, event.pipelineId, event.buildId)
        val dimension = MetricsEventPO.Dimension(
            executeCount = event.executeCount ?: 1,
            type = priority(event.buildStatus),
            level = LEVEL.STEP,
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            pipelineName = cacheGet(readPipelineCache, event, PipelineBuildStatusBroadCastEvent.Labels::pipelineName),
            buildId = event.buildId,
            status = event.buildStatus ?: "",
            eventType = type,
            stageId = event.stageId ?: "",
            stageName = cacheGet(readPipelineCache, event, PipelineBuildStatusBroadCastEvent.Labels::stageName),
            jobId = event.jobId ?: "",
            jobName = cacheGet(readPipelineCache, event, PipelineBuildStatusBroadCastEvent.Labels::jobName),
            stepId = event.stepId ?: "",
            stepName = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::stepName)
        )

        val extra = Extra(
            startTime = labelGet(event.labels, PipelineBuildStatusBroadCastEvent.Labels::startTime),
            duration = null,
            queueDuration = null,
            reviewDuration = null,
            executeDuration = null,
            systemDuration = null
        )
        extra.check()

        val unavailable = queue.offer(
            MetricsEventPO.Data(
                eventName = BUILD_STEP_STATUS_KEY,
                event = MetricsEventPO.Event(
                    "quality step status for ${event.buildStatus}",
                    extra = extra
                ),
                dimension = dimension,
                timestamp = event.eventTime?.timestampmilli(),
                target = event.pipelineId
            )
        )
        if (!unavailable) {
            logger.warn("queue full and ignore")
        }
    }
}
