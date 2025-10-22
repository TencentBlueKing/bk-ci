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
 */

package com.tencent.devops.metrics.config

import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusOutputFormat
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["metrics.user.enable"], havingValue = "true", matchIfMissing = false)
class MetricsUserConfig {

    companion object {
        const val gaugeBuildQueueKey = "pipeline_queue_time_seconds"
        const val gaugeBuildKey = "pipeline_running_time_seconds"
        const val gaugeBuildStatusKey = "pipeline_status_info"
        const val gaugeBuildJobQueueKey = "pipeline_job_queue_time_seconds"
        const val gaugeBuildJobKey = "pipeline_job_running_time_seconds"
        const val gaugeBuildAgentKey = "pipeline_agent_running_time_seconds"
        const val gaugeBuildStepKey = "pipeline_step_running_time_seconds"
        const val gaugeBuildStepStatusKey = "pipeline_step_status_info"
    }

    @Value("\${metrics.user.localCacheMaxSize:100000}")
    val localCacheMaxSize: Long = 100000L

    @Value("\${metrics.user.enable:false}")
    val metricsUserEnabled: Boolean = false

    @Value("\${metrics.event.url:}")
    val eventUrl: String = ""

    @Value("\${metrics.event.dataid:}")
    val eventDataId: Long = 0L

    @Value("\${metrics.event.token:}")
    val eventToken: String = ""

    @Value("\${metrics.event.consumerCount:1}")
    val eventConsumerCount: Int = 1

    /*注册默认的 prometheusMeterRegistry*/
    @Bean
    fun prometheusMeterRegistry(
        prometheusConfig: PrometheusConfig,
        prometheusRegistry: PrometheusRegistry,
        clock: Clock
    ): PrometheusMeterRegistry {
        return PrometheusMeterRegistry(prometheusConfig, prometheusRegistry, clock)
    }

    @Bean
    fun userPrometheusMeterRegistry(): PrometheusMeterRegistry {
        return PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }

    @Bean
    fun userPrometheusEndpoint(userPrometheusMeterRegistry: PrometheusMeterRegistry): UserPrometheusEndpoint {
        return UserPrometheusEndpoint(userPrometheusMeterRegistry)
    }

    @WebEndpoint(id = "userPrometheus")
    class UserPrometheusEndpoint(private val meterRegistry: PrometheusMeterRegistry) {

        @ReadOperation(producesFrom = PrometheusOutputFormat::class)
        fun scrape(): String {
            return meterRegistry.scrape(
                PrometheusTextFormatWriter.CONTENT_TYPE, setOf(
                    gaugeBuildQueueKey,
                    gaugeBuildKey,
                    gaugeBuildStatusKey,
                    gaugeBuildJobQueueKey,
                    gaugeBuildJobKey,
                    gaugeBuildAgentKey,
                    gaugeBuildStepKey,
                    gaugeBuildStepStatusKey
                )
            )
        }
    }
}
