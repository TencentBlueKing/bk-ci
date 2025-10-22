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

package com.tencent.devops.openapi.es.config

import com.tencent.devops.common.es.ESAutoConfiguration
import com.tencent.devops.common.es.ESProperties
import com.tencent.devops.common.es.client.LogClient
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.openapi.dao.MetricsForApiDao
import com.tencent.devops.openapi.dao.MetricsForProjectDao
import com.tencent.devops.openapi.es.IESService
import com.tencent.devops.openapi.es.MetricsService
import com.tencent.devops.openapi.es.impl.DefaultESServiceImpl
import com.tencent.devops.openapi.es.impl.ESServiceImpl
import com.tencent.devops.openapi.es.mq.MQDispatcher
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Suppress("ALL")
@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureAfter(ESAutoConfiguration::class)
@EnableConfigurationProperties(ESProperties::class)
class ESAutoConfiguration {

    @Value("\${log.elasticsearch.consumerCount:1}")
    val consumerCount: Int = 1

    @Bean
    @ConditionalOnProperty(prefix = "log.storage", name = ["type"], havingValue = "elasticsearch")
    fun esLogService(
        @Autowired logESClient: LogClient,
        @Autowired redisOperation: RedisOperation,
        @Autowired openapiMQDispatcher: MQDispatcher
    ): ESServiceImpl {
        return ESServiceImpl(
            logClient = logESClient,
            redisOperation = redisOperation,
            dispatcher = openapiMQDispatcher,
            configuration = this
        )
    }

    @Bean
    @ConditionalOnProperty(prefix = "log.storage", name = ["type"], havingValue = "elasticsearch")
    fun metricsService(
        @Autowired dslContext: DSLContext,
        @Autowired apiDao: MetricsForApiDao,
        @Autowired projectDao: MetricsForProjectDao,
        @Autowired esServiceImpl: ESServiceImpl,
        @Autowired redisOperation: RedisOperation
    ): MetricsService {
        return MetricsService(
            dslContext = dslContext,
            apiDao = apiDao,
            projectDao = projectDao,
            esServiceImpl = esServiceImpl,
            redisOperation = redisOperation
        )
    }

    @Bean
    @ConditionalOnMissingBean(IESService::class)
    fun defaultLogService(): DefaultESServiceImpl {
        return DefaultESServiceImpl()
    }
}
