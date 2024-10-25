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
 *
 */

package com.tencent.devops.metrics.service.builds

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.common.collect.MapMaker
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.metrics.config.MetricsUserConfig
import com.tencent.devops.metrics.pojo.po.MetricsLocalPO
import com.tencent.devops.metrics.pojo.po.MetricsUserPO
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheus.PrometheusMeterRegistry
import java.time.Duration
import java.time.LocalDateTime
import java.util.Collections
import java.util.LinkedList
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["metrics.user.enable"], havingValue = "true", matchIfMissing = false)
class MetricsUserService @Autowired constructor(
    @Qualifier("userPrometheusMeterRegistry")
    private val registry: PrometheusMeterRegistry,
    private val metricsCacheService: MetricsCacheService,
    private val metricsUserConfig: MetricsUserConfig,
    private val client: Client
) {
    private val local = MapMaker()
        .concurrencyLevel(10)
        .makeMap<String, MetricsLocalPO>()

    /* 延迟删除队列 需要线程安全*/
    val delayArray: LinkedList<MutableList<Pair<String, MetricsLocalPO>>> =
        LinkedList(MutableList(DELAY_LIMIT) { Collections.synchronizedList(LinkedList()) })

    /* 疑似构建状态未同步队列,以buildId为单位 */
    val uncheckArray: MutableSet<String> = mutableSetOf()

    private val buildMetricsCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, Boolean> { key ->
            kotlin.runCatching {
                client.get(ServiceProjectResource::class).get(
                    englishName = key
                ).data?.properties?.buildMetrics
            }.getOrNull() ?: false
        }

    /**
     * 定时任务：检查构建状态。
     *
     * 该方法会定时执行: 每10分钟运行一次
     *
     * 方法首先生成一个未检查的构建ID列表的快照，并初始化一个待删除的构建ID列表。
     * 然后，将未检查的构建ID列表按照指定的块大小进行分块处理，每次处理一个块。
     * 对于每个块，方法会调用接口批量获取构建的基本信息，并获取返回结果中已完成的构建ID列表。
     * 将这些已完成的构建ID添加到待删除的构建ID列表中。
     *
     * 接下来，方法会生成本地缓存的键列表，并遍历每个键。
     * 对于每个键，方法会获取对应的数据，并检查其构建ID是否在待删除的构建ID列表中。
     * 如果是，则从缓存中移除该键，并从未检查的构建ID列表中移除该构建ID。
     *
     * @return 无
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    fun checkBuildStatusJob() {
        logger.info(
            "=========>> check build status job start|${local.size}|" +
                "${uncheckArray.size}|${registry.meters.size}<<========="
        )
        // 生成快照
        val unchecks = uncheckArray.toList()
        val ready2delete = mutableListOf<String>()
        unchecks.chunked(CHUNK_SIZE).forEach { chunk ->
            val res = kotlin.runCatching {
                client.get(ServiceBuildResource::class).batchServiceBasic(
                    buildIds = chunk.toSet()
                ).data
            }.getOrNull() ?: return@forEach
            logger.info("checkBuildStatusJob|check|$chunk|${res.mapValues { it.value.status }}")
            ready2delete.addAll(res.filter { it.value.status?.isFinish() == true }.map { it.key })
        }
        // 生成local快照
        val keys = local.keys.toList()
        keys.forEach { key ->
            val value = local[key] ?: return@forEach
            if (value.data.buildId !in ready2delete) return@forEach
            logger.info(
                "checkBuildStatusJob|ready to remove|" +
                    "$key|${local[key]?.data?.buildId}|${local[key]?.data?.eventType}"
            )
            metricsCacheService.removeCache(key)
        }
        uncheckArray.clear()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(MetricsUserService::class.java)
        const val DELAY_LIMIT = 5
        const val CHUNK_SIZE = 100
    }

    class DeleteDelayProcess(
        private val delayArray: LinkedList<MutableList<Pair<String, MetricsLocalPO>>>,
        private val registry: PrometheusMeterRegistry,
        private val local: ConcurrentMap<String, MetricsLocalPO>
    ) : Runnable {

        companion object {
            const val SLEEP = 60000L
        }

        override fun run() {
            while (true) {
                kotlin.runCatching { execute() }
                    .onFailure { logger.error("DeleteDelayProcess error ${it.message}", it) }
                Thread.sleep(SLEEP)
            }
        }

        /**
         * 延迟删除操作。
         *
         * 该方法会从延迟数组中获取待执行的操作列表，并逐个执行。
         * 对于每个操作，方法会遍历其指标列表，并从注册表中移除对应的指标。
         * 同时，方法会从本地缓存中移除对应的键。
         *
         * @return 无
         */
        private fun execute() {
            delayArray.addFirst(Collections.synchronizedList(LinkedList()))
            val ready = delayArray.removeLast()
            logger.info("DeleteDelayProcess|ready to delete|${ready.size}")
            ready.forEachIndexed { index, data ->
                kotlin.runCatching {
                    data.second.meters.forEach { meter ->
                        registry.remove(meter)
                    }
                    local.remove(data.first)
                }.onFailure {
                    logger.error("DeleteDelayProcess error in $index|$data|${it.message}", it)
                }
            }
        }
    }

    fun init() {
        metricsCacheService.addFunction = this::metricsAdd
        metricsCacheService.removeFunction = this::metricsRemove
        metricsCacheService.updateFunction = this::metricsUpdate
        metricsCacheService.init(uncheckArray)
        Thread(DeleteDelayProcess(delayArray, registry, local)).start()
    }

    private fun check(event: PipelineBuildStatusBroadCastEvent): Boolean {
        return buildMetricsCache.get(event.projectId) ?: false
    }

    @Suppress("ComplexMethod")
    fun execute(event: PipelineBuildStatusBroadCastEvent) {
        if (!check(event) || !metricsUserConfig.metricsUserEnabled) return
        val date = MetricsUserPO(event)
        /*防止mq队列堆积导致的延迟信息进入处理，如果生产超过5分钟就丢弃*/
        if (date.startTime < LocalDateTime.now().plusMinutes(-5)) return
        when (date.eventType) {
            CallBackEvent.BUILD_QUEUE -> {
                date.startTime = checkNotNull(event.eventTime)
                metricsCacheService.buildQueue(event.buildId, checkNotNull(event.executeCount), date)
            }

            CallBackEvent.BUILD_START -> {
                date.startTime = checkNotNull(event.eventTime)
                metricsCacheService.buildStart(event.buildId, checkNotNull(event.executeCount), date)
            }

            CallBackEvent.BUILD_JOB_QUEUE -> {
                if (event.jobId.isNullOrBlank()) {
                    // job id 用户没填写将不会上报指标
                    return
                }
                date.startTime = checkNotNull(event.eventTime)
                metricsCacheService.jobQueue(
                    event.buildId,
                    checkNotNull(event.jobId),
                    checkNotNull(event.executeCount),
                    date
                )
            }

            CallBackEvent.BUILD_JOB_START -> {
                if (event.jobId.isNullOrBlank()) {
                    // job id 用户没填写将不会上报指标
                    return
                }
                date.startTime = checkNotNull(event.eventTime)
                metricsCacheService.jobStart(
                    event.buildId,
                    checkNotNull(event.jobId),
                    checkNotNull(event.executeCount),
                    date
                )
            }

            CallBackEvent.BUILD_AGENT_START -> {
                if (event.jobId.isNullOrBlank()) {
                    // job id 用户没填写将不会上报指标
                    return
                }
                date.startTime = checkNotNull(event.eventTime)
                metricsCacheService.agentStart(
                    event.buildId,
                    checkNotNull(event.jobId),
                    checkNotNull(event.executeCount),
                    date
                )
            }

            CallBackEvent.BUILD_TASK_START -> {
                date.startTime = checkNotNull(event.eventTime)
                if (event.stepId.isNullOrBlank()) {
                    // stepId id 用户没填写将不会上报指标
                    return
                }
                metricsCacheService.stepCacheStart(
                    event.buildId,
                    checkNotNull(event.stepId),
                    checkNotNull(event.executeCount),
                    date
                )
            }

            CallBackEvent.BUILD_END -> {
                date.endTime = checkNotNull(event.eventTime)
                metricsCacheService.buildEnd(event.buildId, checkNotNull(event.executeCount), date)
            }

            CallBackEvent.BUILD_JOB_END -> {
                if (event.jobId.isNullOrBlank()) {
                    // job id 用户没填写将不会上报指标
                    return
                }
                /*job skip时没start事件，所以在end时直接去掉*/
                if (event.buildStatus == BuildStatus.SKIP.name) {
                    return
                }
                date.endTime = checkNotNull(event.eventTime)
                metricsCacheService.jobEnd(
                    event.buildId,
                    checkNotNull(event.jobId),
                    checkNotNull(event.executeCount),
                    date
                )
                metricsCacheService.agentEnd(
                    event.buildId,
                    checkNotNull(event.jobId),
                    checkNotNull(event.executeCount),
                    date
                )
            }

            CallBackEvent.BUILD_TASK_END -> {
                date.endTime = checkNotNull(event.eventTime)
                if (event.stepId.isNullOrBlank()) {
                    // stepId id 用户没填写将不会上报指标
                    return
                }
                metricsCacheService.stepCacheEnd(
                    event.buildId,
                    checkNotNull(event.stepId),
                    checkNotNull(event.executeCount),
                    date
                )
            }

            else -> {}
        }
    }

    /* 请勿直接调用该方法 */
    private fun metricsAdd(key: String, value: MetricsUserPO) {
        local[key] = MetricsLocalPO(value)
        logger.debug("metricsAdd|key={}|value={}|localSize={}", key, value, local.size)
        with(value) {
            when (eventType) {
                CallBackEvent.BUILD_QUEUE -> {
                    val buildGauge = registerBuildQueueGauge(
                        key = key,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        description = "build queue metrics for $buildId",
                        labels = labels
                    )
                    local[key]?.meters?.add(buildGauge)
                    val buildStatusGauge = registerBuildStatusGauge(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        status = status,
                        description = "build status metrics for $buildId",
                        labels = labels
                    )
                    local[key]?.meters?.add(buildStatusGauge)
                }

                CallBackEvent.BUILD_START -> {
                    val buildGauge = registerBuildGauge(
                        key = key,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        description = "build metrics for $buildId",
                        labels = labels
                    )
                    local[key]?.meters?.add(buildGauge)
                    val buildStatusGauge = registerBuildStatusGauge(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        status = status,
                        description = "build status metrics for $buildId",
                        labels = labels
                    )
                    local[key]?.meters?.add(buildStatusGauge)
                }

                CallBackEvent.BUILD_JOB_QUEUE -> {
                    val buildJobGauge = registerBuildJobQueueGauge(
                        key = key,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        jobId = checkNotNull(jobId),
                        description = "job queue metrics for $buildId|$jobId",
                        labels = labels
                    )
                    local[key]?.meters?.add(buildJobGauge)
                }

                CallBackEvent.BUILD_JOB_START -> {
                    val buildJobGauge = registerBuildJobGauge(
                        key = key,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        jobId = checkNotNull(jobId),
                        description = "job metrics for $buildId|$jobId",
                        labels = labels
                    )
                    local[key]?.meters?.add(buildJobGauge)
                }

                CallBackEvent.BUILD_AGENT_START -> {
                    val buildJobGauge = registerBuildAgentGauge(
                        key = key,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        jobId = checkNotNull(jobId),
                        description = "agent metrics for $buildId|$jobId",
                        labels = labels
                    )
                    local[key]?.meters?.add(buildJobGauge)
                }

                CallBackEvent.BUILD_TASK_START -> {
                    val buildStepGauge = registerBuildStepGauge(
                        key = key,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        jobId = checkNotNull(jobId),
                        stepId = checkNotNull(stepId),
                        atomCode = checkNotNull(atomCode),
                        description = "step metrics for $buildId|$stepId",
                        labels = labels
                    )
                    local[key]?.meters?.add(buildStepGauge)
                    val buildStepStatusGauge = registerBuildStepStatusGauge(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        jobId = jobId!!,
                        stepId = stepId!!,
                        status = status,
                        description = "step status metrics for $buildId|$stepId",
                        labels = labels
                    )
                    local[key]?.meters?.add(buildStepStatusGauge)
                }

                else -> {
                    /*其余情况属于END状态，应当去除*/
                    metricsCacheService.removeCache(key)
                }
            }
        }
    }

    /* 请勿直接调用该方法 */
    private fun metricsRemove(key: String, value: MetricsUserPO) {
        val metrics = local[key]
        logger.debug("metricsRemove|key={}|value={}|metrics={}", key, value, metrics)
        if (metrics != null) {
            // 异步删除
            delayArray.first.add(key to metrics)
        }
    }

    @Suppress("NestedBlockDepth")
    /* 请勿直接调用该方法 */
    private fun metricsUpdate(key: String, oldValue: MetricsUserPO, newValue: MetricsUserPO) {
        val metrics = local[key]
        logger.debug("metricsUpdate|key={}|oldValue={}|newValue={}|metrics={}", key, oldValue, newValue, metrics)
        if (metrics != null) {
            metrics.data = newValue
            with(newValue) {
                when (eventType) {
                    CallBackEvent.BUILD_START -> {
                        /*去掉构建排队指标*/
                        metrics.meters.find { it.id.name == MetricsUserConfig.gaugeBuildQueueKey }?.run {
                            metricsCacheService.removeCache(key)
                        }
                        metrics.meters.find { it.id.name == MetricsUserConfig.gaugeBuildStatusKey }?.run {
                            metricsCacheService.removeCache(key)
                        }
                    }

                    CallBackEvent.BUILD_END -> {
                        metrics.meters.find { it.id.name == MetricsUserConfig.gaugeBuildStatusKey }?.run {
                            registry.remove(this)
                        }
                        metrics.meters.add(
                            registerBuildStatusGauge(
                                projectId = projectId,
                                pipelineId = pipelineId,
                                buildId = buildId,
                                status = status,
                                description = "build status metrics for $buildId",
                                labels = labels
                            )
                        )
                        metricsCacheService.removeCache(key)
                    }

                    CallBackEvent.BUILD_JOB_START -> {
                        /*去掉job排队指标*/
                        metrics.meters.find { it.id.name == MetricsUserConfig.gaugeBuildJobQueueKey }?.run {
                            metricsCacheService.removeCache(key)
                        }
                    }

                    CallBackEvent.BUILD_JOB_END -> {
                        metricsCacheService.removeCache(key)
                    }

                    CallBackEvent.BUILD_TASK_END -> {
                        metrics.meters.find { it.id.name == MetricsUserConfig.gaugeBuildStepStatusKey }?.run {
                            registry.remove(this)
                        }
                        metrics.meters.add(
                            registerBuildStepStatusGauge(
                                projectId = projectId,
                                pipelineId = pipelineId,
                                buildId = buildId,
                                jobId = jobId!!,
                                stepId = stepId!!,
                                status = status,
                                description = "step status metrics for $buildId|$stepId",
                                labels = labels
                            )
                        )
                        metricsCacheService.removeCache(key)
                    }

                    else -> {}
                }
            }
        }
    }

    private fun deserializeTag(labels: String?): List<Tag> {
        return labels?.split(";")
            ?.mapNotNull {
                val parts = it.split("=")
                if (parts.size == 2) Tag.of(parts[0], parts[1]) else null
            } ?: emptyList()
    }

    private fun registerBuildQueueGauge(
        key: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        description: String,
        labels: String?
    ): Meter {
        return Gauge.builder(
            MetricsUserConfig.gaugeBuildQueueKey,
            local
        ) { cache -> cache[key]?.let { computeStartTime(it) } ?: 0.0 }
            .tags(
                "projectId", projectId,
                "pipeline_id", pipelineId,
                "build_id", buildId
            )
            .tags(deserializeTag(labels))
            .description(description)
            .register(registry)
    }

    private fun registerBuildGauge(
        key: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        description: String,
        labels: String?
    ): Meter {
        return Gauge.builder(
            MetricsUserConfig.gaugeBuildKey,
            local
        ) { cache -> cache[key]?.let { computeStartTime(it) } ?: 0.0 }
            .tags(
                "projectId", projectId,
                "pipeline_id", pipelineId,
                "build_id", buildId
            )
            .tags(deserializeTag(labels))
            .description(description)
            .register(registry)
    }

    private fun registerBuildStatusGauge(
        projectId: String,
        pipelineId: String,
        buildId: String,
        status: String,
        description: String,
        labels: String?
    ): Meter {
        return Gauge.builder(
            MetricsUserConfig.gaugeBuildStatusKey
        ) { 1 }
            .tags(
                "projectId", projectId,
                "pipeline_id", pipelineId,
                "build_id", buildId,
                "status", status
            )
            .tags(deserializeTag(labels))
            .description(description)
            .register(registry)
    }

    private fun registerBuildJobQueueGauge(
        key: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        jobId: String,
        description: String,
        labels: String?
    ): Meter {
        return Gauge.builder(
            MetricsUserConfig.gaugeBuildJobQueueKey,
            local
        ) { cache -> cache[key]?.let { computeStartTime(it) } ?: 0.0 }
            .tags(
                "projectId", projectId,
                "pipeline_id", pipelineId,
                "build_id", buildId,
                "job_id", jobId
            )
            .tags(deserializeTag(labels))
            .description(description)
            .register(registry)
    }

    private fun registerBuildJobGauge(
        key: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        jobId: String,
        description: String,
        labels: String?
    ): Meter {
        return Gauge.builder(
            MetricsUserConfig.gaugeBuildJobKey,
            local
        ) { cache -> cache[key]?.let { computeStartTime(it) } ?: 0.0 }
            .tags(
                "projectId", projectId,
                "pipeline_id", pipelineId,
                "build_id", buildId,
                "job_id", jobId
            )
            .tags(deserializeTag(labels))
            .description(description)
            .register(registry)
    }

    private fun registerBuildAgentGauge(
        key: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        jobId: String,
        description: String,
        labels: String?
    ): Meter {
        return Gauge.builder(
            MetricsUserConfig.gaugeBuildAgentKey,
            local
        ) { cache -> cache[key]?.let { computeStartTime(it) } ?: 0.0 }
            .tags(
                "projectId", projectId,
                "pipeline_id", pipelineId,
                "build_id", buildId,
                "job_id", jobId
            )
            .tags(deserializeTag(labels))
            .description(description)
            .register(registry)
    }

    private fun registerBuildStepGauge(
        key: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        jobId: String,
        stepId: String,
        atomCode: String,
        description: String,
        labels: String?
    ): Meter {
        return Gauge.builder(
            MetricsUserConfig.gaugeBuildStepKey,
            local
        ) { cache -> cache[key]?.let { computeStartTime(it) } ?: 0.0 }
            .tags(
                "projectId", projectId,
                "pipeline_id", pipelineId,
                "build_id", buildId,
                "job_id", jobId,
                "step_id", stepId,
                "plugin_id", atomCode
            )
            .tags(deserializeTag(labels))
            .description(description)
            .register(registry)
    }

    private fun registerBuildStepStatusGauge(
        projectId: String,
        pipelineId: String,
        buildId: String,
        jobId: String,
        stepId: String,
        status: String,
        description: String,
        labels: String?
    ): Meter {
        return Gauge.builder(
            MetricsUserConfig.gaugeBuildStepStatusKey
        ) { 1 }
            .tags(
                "projectId", projectId,
                "pipeline_id", pipelineId,
                "build_id", buildId,
                "job_id", jobId,
                "step_id", stepId,
                "status", status
            )
            .tags(deserializeTag(labels))
            .description(description)
            .register(registry)
    }

    private fun computeStartTime(cache: MetricsLocalPO): Double {
        return Duration.between(cache.data.startTime, cache.data.endTime ?: LocalDateTime.now()).seconds.toDouble()
    }
}
