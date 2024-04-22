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

import com.google.common.collect.MapMaker
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.metrics.config.MetricsUserConfig
import com.tencent.devops.metrics.pojo.po.MetricsLocalPO
import com.tencent.devops.metrics.pojo.po.MetricsUserPO
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Meter
import io.micrometer.prometheus.PrometheusMeterRegistry
import java.time.Duration
import java.time.LocalDateTime
import java.util.LinkedList
import java.util.concurrent.ConcurrentMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class MetricsUserService @Autowired constructor(
    @Qualifier("userPrometheusMeterRegistry")
    private val registry: PrometheusMeterRegistry,
    private val metricsCacheService: MetricsCacheService
) {
    private val local = MapMaker()
        .concurrencyLevel(10)
        .makeMap<String, MetricsLocalPO>()
    val delayArray: LinkedList<MutableList<Pair<String, MetricsLocalPO>>> =
        LinkedList(MutableList(DELAY_LIMIT) { mutableListOf() })

    companion object {
        val logger: Logger = LoggerFactory.getLogger(MetricsUserService::class.java)
        const val DELAY_LIMIT = 5
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
                Thread.sleep(SLEEP)
            }
        }

        private fun execute() {
            delayArray.addFirst(mutableListOf())
            val ready = delayArray.removeLast()
            ready.forEach { (key, metrics) ->
                metrics.meters.forEach { meter ->
                    registry.remove(meter)
                }
                local.remove(key)
            }
        }
    }

    fun init() {
        metricsCacheService.addFunction = this::metricsAdd
        metricsCacheService.removeFunction = this::metricsRemove
        metricsCacheService.updateFunction = this::metricsUpdate
        metricsCacheService.init()
        Thread(DeleteDelayProcess(delayArray, registry, local)).start()
    }

    fun execute(event: PipelineBuildStatusBroadCastEvent) {
        val date = MetricsUserPO(event)
        when (date.eventType) {
            CallBackEvent.BUILD_START -> {
                date.startTime = checkNotNull(event.eventTime)
                metricsCacheService.buildCacheStart(event.buildId, checkNotNull(event.executeCount), date)
            }

            CallBackEvent.BUILD_JOB_START -> {
                if (event.jobId == null) {
                    // job id 用户没填写将不会上报指标
                    return
                }
                date.startTime = checkNotNull(event.eventTime)
                metricsCacheService.jobCacheStart(
                    event.buildId,
                    checkNotNull(event.jobId),
                    checkNotNull(event.executeCount),
                    date
                )
            }

            CallBackEvent.BUILD_TASK_START -> {
                date.startTime = checkNotNull(event.eventTime)
                if (event.stepId == null) {
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
                metricsCacheService.buildCacheEnd(event.buildId, checkNotNull(event.executeCount), date)

            }

            CallBackEvent.BUILD_JOB_END -> {
                if (event.jobId == null) {
                    // job id 用户没填写将不会上报指标
                    return
                }
                date.endTime = checkNotNull(event.eventTime)
                metricsCacheService.jobCacheEnd(
                    event.buildId,
                    checkNotNull(event.jobId),
                    checkNotNull(event.executeCount),
                    date
                )

            }

            CallBackEvent.BUILD_TASK_END -> {
                date.endTime = checkNotNull(event.eventTime)
                if (event.stepId == null) {
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

    fun metricsAdd(key: String, value: MetricsUserPO) {
        local[key] = MetricsLocalPO(value)
        logger.debug("metricsAdd|key={}|value={}|localSize={}", key, value, local.size)
        with(value) {
            when (eventType) {
                CallBackEvent.BUILD_START -> {
                    val buildGauge = registerBuildGauge(
                        key = key,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        description = "build metrics for $buildId"
                    )
                    local[key]?.meters?.add(buildGauge)
                    val buildStatusGauge = registerBuildStatusGauge(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        status = status,
                        description = "build status metrics for $buildId"
                    )
                    local[key]?.meters?.add(buildStatusGauge)
                }

                CallBackEvent.BUILD_JOB_START -> {
                    val buildJobGauge = registerBuildJobGauge(
                        key = key,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        jobId = checkNotNull(jobId),
                        description = "job metrics for $buildId|$jobId"
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
                        description = "step metrics for $buildId|$stepId"
                    )
                    local[key]?.meters?.add(buildStepGauge)
                    val buildStepStatusGauge = registerBuildStepStatusGauge(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        jobId = jobId!!,
                        stepId = stepId!!,
                        status = status,
                        description = "step status metrics for $buildId|$stepId"
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

    fun metricsRemove(key: String, value: MetricsUserPO) {
        val metrics = local[key]
        logger.debug("metricsRemove|key={}|value={}|metrics={}", key, value, metrics)
        if (metrics != null) {
            // 异步删除
            delayArray.first.add(key to metrics)
        }
    }

    fun metricsUpdate(key: String, oldValue: MetricsUserPO, newValue: MetricsUserPO) {
        val metrics = local[key]
        logger.debug("metricsUpdate|key={}|oldValue={}|newValue={}|metrics={}", key, oldValue, newValue, metrics)
        if (metrics != null) {
            metrics.data = newValue
            with(newValue) {
                when (eventType) {
                    CallBackEvent.BUILD_END -> {
                        metrics.meters.find { it.id.name == MetricsUserConfig.gaugeBuildStatusKey }?.run {
                            registry.remove(this)
                        }
                        registerBuildStatusGauge(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            status = status,
                            description = "build status metrics for $buildId"
                        )
                        metricsCacheService.removeCache(key)
                    }

                    CallBackEvent.BUILD_JOB_END -> {
                        metricsCacheService.removeCache(key)
                    }

                    CallBackEvent.BUILD_TASK_END -> {
                        metrics.meters.find { it.id.name == MetricsUserConfig.gaugeBuildStepStatusKey }?.run {
                            registry.remove(this)
                        }
                        registerBuildStepStatusGauge(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            jobId = jobId!!,
                            stepId = stepId!!,
                            status = status,
                            description = "step status metrics for $buildId|$stepId"
                        )
                        metricsCacheService.removeCache(key)
                    }

                    else -> {}
                }
            }
        }
    }

    private fun registerBuildGauge(
        key: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        description: String
    ): Meter {
        return Gauge.builder(
            MetricsUserConfig.gaugeBuildKey,
            local
        ) { cache -> cache[key]?.let { computeStartTime(it) } ?: 0.0 }
            .tags(
                "bk_project_id", projectId,
                "pipeline_id", pipelineId,
                "build_id", buildId
            )
            .description(description)
            .register(registry)
    }

    private fun registerBuildStatusGauge(
        projectId: String,
        pipelineId: String,
        buildId: String,
        status: String,
        description: String
    ): Meter {
        return Gauge.builder(
            MetricsUserConfig.gaugeBuildStatusKey
        ) { 1 }
            .tags(
                "bk_project_id", projectId,
                "pipeline_id", pipelineId,
                "build_id", buildId,
                "status", status
            )
            .description(description)
            .register(registry)
    }

    private fun registerBuildJobGauge(
        key: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        jobId: String,
        description: String
    ): Meter {
        return Gauge.builder(
            MetricsUserConfig.gaugeBuildJobKey,
            local
        ) { cache -> cache[key]?.let { computeStartTime(it) } ?: 0.0 }
            .tags(
                "bk_project_id", projectId,
                "pipeline_id", pipelineId,
                "build_id", buildId,
                "job_id", jobId
            )
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
        description: String
    ): Meter {
        return Gauge.builder(
            MetricsUserConfig.gaugeBuildStepKey,
            local
        ) { cache -> cache[key]?.let { computeStartTime(it) } ?: 0.0 }
            .tags(
                "bk_project_id", projectId,
                "pipeline_id", pipelineId,
                "build_id", buildId,
                "job_id", jobId,
                "step_id", stepId,
                "plugin_id", atomCode
            )
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
        description: String
    ): Meter {
        return Gauge.builder(
            MetricsUserConfig.gaugeBuildStepStatusKey
        ) { 1 }
            .tags(
                "bk_project_id", projectId,
                "pipeline_id", pipelineId,
                "build_id", buildId,
                "job_id", jobId,
                "step_id", stepId,
                "status", status
            )
            .description(description)
            .register(registry)
    }

    private fun computeStartTime(cache: MetricsLocalPO): Double {
        return Duration.between(cache.data.startTime, cache.data.endTime ?: LocalDateTime.now()).seconds.toDouble()
    }
}
