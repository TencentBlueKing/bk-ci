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

package com.tencent.devops.process.init

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.process.engine.listener.pipeline.MQPipelineStreamEnabledListener
import com.tencent.devops.process.engine.listener.run.callback.PipelineBuildCallBackListener
import com.tencent.devops.process.engine.pojo.event.PipelineStreamEnabledEvent
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import java.time.Duration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 流水线回调扩展配置
 */
@Configuration
@EnableConfigurationProperties(CallbackCircuitBreakerProperties::class)
class PipelineCallBackConfiguration {
    /**
     * 构建构建回调广播交换机
     */
    @EventConsumer
    fun pipelineBuildCallBackConsumer(
        @Autowired buildListener: PipelineBuildCallBackListener
    ) = ScsConsumerBuilder.build<PipelineBuildStatusBroadCastEvent> { buildListener.run(it) }

    /**
     * 构建构建回调广播交换机
     */
    @EventConsumer
    fun pipelineStreamEnabledConsumer(
        @Autowired streamEnabledListener: MQPipelineStreamEnabledListener
    ) = ScsConsumerBuilder.build<PipelineStreamEnabledEvent> { streamEnabledListener.run(it) }

    @Bean
    fun callbackCircuitBreakerRegistry(
        callbackCircuitBreakerProperties: CallbackCircuitBreakerProperties
    ): CircuitBreakerRegistry {
        val builder = CircuitBreakerConfig.custom()
        builder.enableAutomaticTransitionFromOpenToHalfOpen()
        builder.writableStackTraceEnabled(false)
        with(callbackCircuitBreakerProperties) {
            failureRateThreshold?.let { builder.failureRateThreshold(it) }
            slowCallRateThreshold?.let { builder.slowCallRateThreshold(it) }
            slowCallDurationThreshold?.let { builder.slowCallDurationThreshold(Duration.ofSeconds(it)) }
            waitDurationInOpenState?.let { builder.waitDurationInOpenState(Duration.ofSeconds(it)) }
            permittedNumberOfCallsInHalfOpenState?.let { builder.permittedNumberOfCallsInHalfOpenState(it) }
            slidingWindow?.let { builder.slidingWindowSize(it) }
            minimumNumberOfCalls?.let { builder.minimumNumberOfCalls(it) }
        }
        return CircuitBreakerRegistry.of(builder.build())
    }
}
