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

package com.tencent.bkrepo.monitor.export

import com.tencent.bkrepo.monitor.metrics.MetricInfo
import com.tencent.bkrepo.monitor.service.MetricSourceService
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux

@Component
class InfluxMetricsExporter(
    metricSourceService: MetricSourceService,
    private val influxExportProperties: InfluxExportProperties
) {

    private val converter = InfluxMetricsConverter()
    private val webClient: WebClient
    private var databaseExists: Boolean = false
    private var influxEndpoint: String = ""
    init {
        with(influxExportProperties) {
            influxEndpoint = "/write?consistency=" + consistency.toLowerCase() + "&precision=ms&db=" + db
            retentionPolicy?.let { influxEndpoint += "&rp=$retentionPolicy" }

            val builder = WebClient.builder().baseUrl(uri)
            if (!username.isNullOrBlank()) {
                builder.defaultHeaders { it.setBasicAuth(username.orEmpty(), password.orEmpty()) }
            }
            webClient = builder.build()
            metricSourceService.getMergedSource().window(step).subscribe { exportMetricSource(it) }
        }
    }

    private fun exportMetricSource(metricSource: Flux<MetricInfo>) {
        if (!influxExportProperties.enabled) {
            return
        }
        createDatabaseIfNecessary()
        val stringSource = metricSource.map { converter.convert(it) }
        webClient.post()
            .uri(influxEndpoint)
            .contentType(MediaType.TEXT_PLAIN)
            .body(BodyInserters.fromPublisher(stringSource, String::class.java))
            .exchange()
            .flatMap { clientResponse ->
                clientResponse.bodyToMono(String::class.java)
                    .doOnSuccess {
                        if (clientResponse.statusCode().isError) {
                            logger.error("Failed to export metrics to influx")
                            logger.error("HttpStatusCode = {}", clientResponse.statusCode())
                            logger.error("HttpHeaders = {}", clientResponse.headers().asHttpHeaders())
                            logger.error("ResponseBody = {}", it)
                        }
                    }
            }.subscribe()
    }

    private fun createDatabaseIfNecessary() {
        if (!influxExportProperties.autoCreateDb || databaseExists) {
            return
        }
        val createDatabaseQuery = CreateDatabaseQueryBuilder(influxExportProperties.db)
            .setRetentionDuration(influxExportProperties.retentionDuration)
            .setRetentionPolicyName(influxExportProperties.retentionPolicy)
            .setRetentionReplicationFactor(influxExportProperties.retentionReplicationFactor)
            .setRetentionShardDuration(influxExportProperties.retentionShardDuration)
            .build()
        webClient.post()
            .uri("/query?q=$createDatabaseQuery")
            .exchange()
            .flatMap { clientResponse ->
                clientResponse.bodyToMono(String::class.java)
                    .doOnSuccess {
                        if (clientResponse.statusCode().isError) {
                            logger.error("Unable to create database '{}'", influxExportProperties.db)
                            logger.error("HttpStatusCode = {}", clientResponse.statusCode())
                            logger.error("HttpHeaders = {}", clientResponse.headers().asHttpHeaders())
                            logger.error("ResponseBody = {}", it)
                        } else {
                            logger.info("Influx database {} is ready to receive metrics", influxExportProperties.db)
                            databaseExists = true
                        }
                    }
            }.block()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(InfluxMetricsExporter::class.java)
    }
}
