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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQEventDispatcher
import com.tencent.devops.process.engine.cfg.BuildIdGenerator
import com.tencent.devops.process.engine.cfg.ModelContainerIdGenerator
import com.tencent.devops.process.engine.cfg.ModelStageIdGenerator
import com.tencent.devops.process.engine.cfg.ModelTaskIdGenerator
import com.tencent.devops.process.engine.cfg.PipelineIdGenerator
import com.tencent.devops.process.engine.extend.DefaultModelCheckPlugin
import com.tencent.devops.process.engine.interceptor.PipelineInterceptorChain
import com.tencent.devops.process.engine.interceptor.QueueInterceptor
import com.tencent.devops.process.engine.interceptor.RunLockInterceptor
import com.tencent.devops.process.service.measure.MeasureEventDispatcher
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

/**
 * 流水线引擎初始化配置类
 *
 * @version 1.0
 */

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class PipelineEngineConfiguration {

    @Bean
    fun modelContainerAgentCheckPlugin(@Autowired client: Client) = DefaultModelCheckPlugin(client)

    @Bean
    fun pipelineIdGenerator() = PipelineIdGenerator()

    @Bean
    fun buildIdGenerator() = BuildIdGenerator()

    @Bean
    fun modelContainerIdGenerator() = ModelContainerIdGenerator()

    @Bean
    fun modelStageIdGenerator() = ModelStageIdGenerator()

    @Bean
    fun modelTaskIdGenerator() = ModelTaskIdGenerator()

    @Bean
    fun pipelineInterceptorChain(
        runLockInterceptor: RunLockInterceptor,
        queueInterceptor: QueueInterceptor
    ): PipelineInterceptorChain {
        val list = listOf(
            runLockInterceptor,
            queueInterceptor
        )
        return PipelineInterceptorChain(list)
    }

    @Bean
    fun pipelineEventDispatcher(rabbitTemplate: RabbitTemplate) = MQEventDispatcher(rabbitTemplate)

    @Bean
    fun measureEventDispatcher(rabbitTemplate: RabbitTemplate) = MeasureEventDispatcher(rabbitTemplate)
}
