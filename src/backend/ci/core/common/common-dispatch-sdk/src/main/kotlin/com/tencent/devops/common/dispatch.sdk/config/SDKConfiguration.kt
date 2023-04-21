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

package com.tencent.devops.common.dispatch.sdk.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQEventDispatcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.dispatch.sdk.service.DispatchService
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.service.DockerRoutingSdkService
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.dispatch.sdk.service.JobQuotaService
import com.tencent.devops.common.dispatch.sdk.utils.ChannelUtils
import com.tencent.devops.common.service.config.CommonConfig

@Configuration
class SDKConfiguration {
    @Value("\${gateway.url:#{null}}")
    private val gateway: String? = ""

    @Bean
    fun dispatchService(
        @Autowired redisOperation: RedisOperation,
        @Autowired pipelineEventDispatcher: PipelineEventDispatcher,
        @Autowired objectMapper: ObjectMapper,
        @Autowired client: Client,
        @Autowired channelUtils: ChannelUtils,
        @Autowired buildLogPrinter: BuildLogPrinter,
        @Autowired commonConfig: CommonConfig
    ) =
        DispatchService(
            redisOperation = redisOperation,
            objectMapper = objectMapper,
            pipelineEventDispatcher = pipelineEventDispatcher,
            gateway = gateway,
            client = client,
            channelUtils = channelUtils,
            buildLogPrinter = buildLogPrinter,
            commonConfig = commonConfig
        )

    @Bean
    fun jobQuotaService(
        @Autowired client: Client,
        @Autowired buildLogPrinter: BuildLogPrinter
    ) =
        JobQuotaService(client, buildLogPrinter)

    @Bean
    fun dockerRoutingSdkService(
        @Autowired redisOperation: RedisOperation
    ) =
        DockerRoutingSdkService(redisOperation)

    @Bean
    fun pipelineEventDispatcher(@Autowired rabbitTemplate: RabbitTemplate): PipelineEventDispatcher {
        return MQEventDispatcher(rabbitTemplate)
    }
}
