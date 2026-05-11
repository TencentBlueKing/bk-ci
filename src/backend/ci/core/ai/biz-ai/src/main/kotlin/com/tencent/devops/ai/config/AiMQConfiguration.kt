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

package com.tencent.devops.ai.config

import com.tencent.devops.ai.pojo.event.AiRunStopBroadcastEvent
import com.tencent.devops.ai.service.ActiveRunManager
import com.tencent.devops.ai.service.AiRunEventService
import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.stream.ScsConsumerBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

/**
 * AI 模块 MQ 消费者配置。
 *
 * 仅注册 Stop 广播消费者 — anonymous(fanout)，所有实例消费，匹配 threadId 执行 interrupt。
 * 事件持久化和清理已改为同步直写 DB，不再通过 MQ。
 */
@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class AiMQConfiguration {

    /**
     * Stop 广播消费者。
     * anonymous = true → 每个实例都会收到 → fanout 广播语义。
     * 每个实例检查本地 ActiveRunManager 是否持有该 threadId，
     * 若持有则 interrupt + 关闭 replaySink + 移除。
     */
    @EventConsumer(anonymous = true)
    fun aiRunStopBroadcastConsumer(
        @Autowired aiRunEventService: AiRunEventService,
        @Autowired activeRunManager: ActiveRunManager
    ) = ScsConsumerBuilder.build<AiRunStopBroadcastEvent> {
        aiRunEventService.handleStopBroadcast(it, activeRunManager)
    }
}
