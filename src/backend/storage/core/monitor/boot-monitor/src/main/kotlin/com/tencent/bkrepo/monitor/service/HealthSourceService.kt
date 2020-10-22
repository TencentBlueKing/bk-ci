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

import com.tencent.bkrepo.monitor.config.MonitorProperties
import com.tencent.bkrepo.monitor.metrics.HealthEndpoint
import com.tencent.bkrepo.monitor.metrics.HealthInfo
import de.codecentric.boot.admin.server.services.InstanceRegistry
import de.codecentric.boot.admin.server.web.client.InstanceWebClient
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import javax.annotation.PreDestroy

@Component
class HealthSourceService(
    private val monitorProperties: MonitorProperties,
    private val instanceRegistry: InstanceRegistry,
    instanceWebClientBuilder: InstanceWebClient.Builder
) {

    private val instanceWebClient = instanceWebClientBuilder.build()
    val healthSourceMap: MutableMap<HealthEndpoint, InstanceHealthSource> = mutableMapOf()

    init {
        monitorProperties.health.forEach { (healthName, applicationListString) ->
            val healthEndpoint = HealthEndpoint.ofHealthName(healthName)
            val trimmedApplicationListString = applicationListString.trim()
            val includeAll = trimmedApplicationListString.isEmpty() || trimmedApplicationListString == "*"
            val applicationList = trimmedApplicationListString.split(",").map { it.trim() }.distinct()
            val healthSource = InstanceHealthSource(healthEndpoint, includeAll, applicationList, monitorProperties.interval, instanceRegistry, instanceWebClient)
            healthSourceMap[healthEndpoint] = healthSource
        }
    }

    fun getHealthSource(health: HealthEndpoint) = healthSourceMap[health]?.healthSource ?: Flux.empty()

    fun getMergedSource(): Flux<HealthInfo> {
        return Flux.merge(Flux.fromIterable(healthSourceMap.entries).map { it.value.healthSource })
    }

    @PreDestroy
    private fun stop() {
        healthSourceMap.forEach { (_, source) -> source.stop() }
    }
}
