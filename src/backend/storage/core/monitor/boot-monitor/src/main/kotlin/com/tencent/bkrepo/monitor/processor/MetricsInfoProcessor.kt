/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.monitor.processor

import com.tencent.bkrepo.monitor.config.MonitorProperties
import com.tencent.bkrepo.monitor.metrics.MetricEndpoint
import com.tencent.bkrepo.monitor.metrics.MetricsInfo
import de.codecentric.boot.admin.server.services.InstanceRegistry
import de.codecentric.boot.admin.server.web.client.InstanceWebClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.ReplayProcessor
import javax.annotation.PreDestroy

@Component
class MetricsInfoProcessor(
    private val monitorProperties: MonitorProperties,
    private val instanceWebClientBuilder: InstanceWebClient.Builder,
    private val instanceRegistry: InstanceRegistry
) {

    @Value("\${service.prefix:repo-}")
    private val servicePrefix: String = "repo-"

    @Value("\${service.suffix:}")
    private val serviceSuffix: String = ""

    private val processor = ReplayProcessor.create<MetricsInfo>(HISTORY_SIZE)
    private val metricSourceMap: MutableMap<MetricEndpoint, InstanceMetricSource> = mutableMapOf()

    init {
        monitorProperties.metrics.forEach { (metricName, applicationListString) ->
            val metricEndpoint = MetricEndpoint.ofMetricName(metricName)
            val trimmedApplicationListString = applicationListString.trim()
            val includeAll = trimmedApplicationListString.isEmpty() || trimmedApplicationListString == "*"
            val applicationList = trimmedApplicationListString.split(",").map { resolveServiceName(it) }.distinct()
            val metricSource = InstanceMetricSource(
                metricEndpoint,
                includeAll,
                applicationList,
                monitorProperties.interval,
                instanceRegistry,
                instanceWebClientBuilder.build(),
                processor
            )
            metricSourceMap[metricEndpoint] = metricSource
        }
    }

    fun getFlux(): Flux<MetricsInfo> = processor

    private fun resolveServiceName(original: String): String {
        return "$servicePrefix${original.trim()}$serviceSuffix"
    }

    @PreDestroy
    private fun stop() {
        metricSourceMap.forEach { (_, source) -> source.stop() }
    }

    companion object {
        private const val HISTORY_SIZE = 8192
    }
}
