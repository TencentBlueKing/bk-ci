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

package com.tencent.bkrepo.monitor.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.tencent.bkrepo.monitor.export.InfluxExportProperties
import com.tencent.bkrepo.monitor.notify.MessageNotifier
import de.codecentric.boot.admin.server.config.EnableAdminServer
import de.codecentric.boot.admin.server.domain.values.InstanceId
import de.codecentric.boot.admin.server.services.InstanceIdGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import reactor.core.publisher.Mono
import java.net.URL
import java.util.concurrent.Executor
import javax.annotation.PostConstruct
import javax.annotation.Resource

@Configuration
@EnableAdminServer
@EnableConfigurationProperties(MonitorProperties::class, InfluxExportProperties::class)
class MonitorConfiguration {

    @Resource
    private lateinit var taskAsyncExecutor: Executor

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Resource
    private lateinit var adminJacksonModule: SimpleModule

    /**
     * adapt for moment.js
     */
    @PostConstruct
    fun customObjectMapper() {
        objectMapper.registerModule(adminJacksonModule)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @Bean
    fun monitorWebMvcConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun configureAsyncSupport(configurer: AsyncSupportConfigurer) {
                super.configureAsyncSupport(configurer)
                configurer.setDefaultTimeout(60 * 1000L)
                configurer.setTaskExecutor(taskAsyncExecutor as AsyncTaskExecutor)
            }
        }
    }

    @Bean
    fun instanceIdGenerator(): InstanceIdGenerator = InstanceIdGenerator {
        val url = URL(it.serviceUrl)
        InstanceId.of("${url.host}-${url.port}")
    }

    @Bean
    @ConditionalOnMissingBean
    fun messageNotifier() = object : MessageNotifier {
        override fun notifyMessage(content: Any): Mono<Void> = Mono.empty()
    }
}
