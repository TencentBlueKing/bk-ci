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

package com.tencent.bkrepo.common.service.actuator

import com.tencent.bkrepo.common.service.condition.ConditionalOnMicroService
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnMicroService
class ActuatorConfiguration {

    @Value(SERVICE_NAME)
    private lateinit var serviceName: String

    @Value(SERVICE_INSTANCE_ID)
    private lateinit var instanceId: String

    @Value(SERVER_HOST)
    private lateinit var host: String

    @Bean
    fun metricsCommonTags(commonTagProvider: CommonTagProvider): MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer { registry ->
            commonTagProvider.provide().forEach {
                registry.config().commonTags(it.key, it.value)
            }
        }
    }

    @Bean
    fun commonTagProvider() = object : CommonTagProvider {
        override fun provide(): Map<String, String> {
            return mapOf(
                "service" to serviceName,
                "instance" to instanceId,
                "host" to host
            )
        }
    }

    companion object {
        private const val SERVICE_NAME = "\${service.prefix:}\${spring.application.name}\${service.suffix:}"
        private const val SERVER_HOST = "\${spring.cloud.client.ip-address}"
        private const val SERVER_PORT = "\${server.port}"
        private const val SERVICE_INSTANCE_ID = "${SERVICE_NAME}-${SERVER_PORT}-${SERVER_HOST}"
    }
}
