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

package com.tencent.devops.metrics.config

import io.micrometer.core.instrument.Clock
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint
import org.springframework.boot.actuate.metrics.export.prometheus.TextOutputFormat
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsUserConfig {

    companion object {
        const val gaugeBuildKey = "pipeline_running_time_seconds"
        const val gaugeBuildStatusKey = "pipeline_status_info"
        const val gaugeBuildJobKey = "pipeline_job_running_time_seconds"
        const val gaugeBuildStepKey = "pipeline_step_running_time_seconds"
        const val gaugeBuildStepStatusKey = "pipeline_step_status_info"
    }

    @Value("\${metrics.user.localCacheMaxSize:100000}")
    val localCacheMaxSize: Long = 100000L


    /*注册默认的 prometheusMeterRegistry*/
    @Bean
    fun prometheusMeterRegistry(
        prometheusConfig: PrometheusConfig,
        collectorRegistry: CollectorRegistry,
        clock: Clock
    ): PrometheusMeterRegistry {
        return PrometheusMeterRegistry(prometheusConfig, collectorRegistry, clock)
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

        @ReadOperation(producesFrom = TextOutputFormat::class)
        fun scrape(): String {
            return meterRegistry.scrape(
                TextFormat.CONTENT_TYPE_004, setOf(
                    gaugeBuildKey,
                    gaugeBuildStatusKey,
                    gaugeBuildJobKey,
                    gaugeBuildStepKey,
                    gaugeBuildStepStatusKey
                )
            )
        }
    }
}
