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

package com.tencent.bkrepo.monitor.controller

import com.tencent.bkrepo.monitor.metrics.HealthEndpoint
import com.tencent.bkrepo.monitor.metrics.HealthInfo
import com.tencent.bkrepo.monitor.service.HealthSourceService
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/monitor")
class InstanceHealthController(val healthSourceService: HealthSourceService) {

    @GetMapping(path = ["/health/{healthName}"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun metricsStream(@PathVariable healthName: String): Flux<HealthInfo> {
        val healthEndpoint = HealthEndpoint.ofHealthName(healthName)
        return healthSourceService.getHealthSource(healthEndpoint)
    }

    @GetMapping(path = ["/health"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun metricsStream(): Flux<ServerSentEvent<HealthInfo>> {
        return healthSourceService.getMergedSource().map { transformServerSendEvent(it) }
    }

    private fun transformServerSendEvent(healthInfo: HealthInfo): ServerSentEvent<HealthInfo> {
        return ServerSentEvent.builder(healthInfo)
            .event(healthInfo.name)
            .build()
    }
}
