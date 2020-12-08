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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.monitor.service

import com.tencent.bkrepo.monitor.metrics.MetricEndpoint
import com.tencent.bkrepo.monitor.metrics.MetricInfo
import de.codecentric.boot.admin.server.domain.entities.Instance
import de.codecentric.boot.admin.server.services.InstanceRegistry
import de.codecentric.boot.admin.server.web.client.InstanceWebClient
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.ClientResponse
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.UnicastProcessor
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.time.Duration

class InstanceMetricSource(
    private val metricEndpoint: MetricEndpoint,
    private val includeAll: Boolean,
    private val applicationList: List<String>,
    interval: Duration,
    private val instanceRegistry: InstanceRegistry,
    private val instanceWebClient: InstanceWebClient
) {
    private val scheduler: Scheduler = Schedulers.newSingle(metricEndpoint.metricName)
    private val processor = UnicastProcessor.create<MetricInfo>()
    private val subscribe: Disposable
    val metricsSource = processor.publish().autoConnect()

    init {
        subscribe = Flux.interval(interval)
            .map { logger.debug("Ready to retrieve metric[${metricEndpoint.metricName}]") }
            .flatMap { instanceRegistry.instances }
            .filter { it.isRegistered && (includeAll || applicationList.contains(it.registration.name)) }
            .subscribeOn(scheduler)
            .concatMap { updateMetricsInfo(it) }
            .subscribe { processor.onNext(it) }
    }

    private fun updateMetricsInfo(instance: Instance): Mono<MetricInfo> {
        return instanceWebClient.instance(instance).get()
            .uri(metricEndpoint.getEndpoint()).exchange()
            .flatMap { convert(it) }
            .doOnError { logError(instance, it) }
            .onErrorResume { handleError(it) }
    }

    private fun convert(response: ClientResponse): Mono<MetricInfo> {
        return response.bodyToMono(MetricInfo::class.java)
    }

    private fun handleError(ex: Throwable): Mono<MetricInfo> {
        logger.error("error", ex)
        return Mono.empty()
    }

    private fun logError(instance: Instance, ex: Throwable) {
        if (instance.statusInfo.isOffline) {
            logger.debug("Couldn't retrieve metric [${metricEndpoint.metricName}] for [$instance]", ex)
        } else {
            logger.warn("Couldn't retrieve metric [${metricEndpoint.metricName}] for [$instance]", ex)
        }
    }

    fun stop() {
        scheduler.dispose()
        subscribe.dispose()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(InstanceMetricSource::class.java)
    }
}
