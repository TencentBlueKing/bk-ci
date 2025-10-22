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
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.process.engine.listener.pipeline.MQPipelineCreateListener
import com.tencent.devops.process.engine.listener.pipeline.MQPipelineDeleteListener
import com.tencent.devops.process.engine.listener.pipeline.MQPipelineRestoreListener
import com.tencent.devops.process.engine.listener.pipeline.MQPipelineUpdateListener
import com.tencent.devops.process.engine.pojo.event.PipelineCreateEvent
import com.tencent.devops.process.engine.pojo.event.PipelineDeleteEvent
import com.tencent.devops.process.engine.pojo.event.PipelineRestoreEvent
import com.tencent.devops.process.engine.pojo.event.PipelineUpdateEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration

/**
 * 流水线构建核心配置
 */
@Configuration
@Suppress("LongParameterList")
class PipelineBaseConfiguration {
    /**
     * 流水线创建队列--- 并发小
     */
    @EventConsumer
    fun pipelineCreateConsumer(
        @Autowired createListener: MQPipelineCreateListener
    ) = ScsConsumerBuilder.build<PipelineCreateEvent> { createListener.run(it) }

    /**
     * 流水线删除队列--- 并发小
     */
    @EventConsumer
    fun pipelineDeleteConsumer(
        @Autowired deleteListener: MQPipelineDeleteListener
    ) = ScsConsumerBuilder.build<PipelineDeleteEvent> { deleteListener.run(it) }

    /**
     * 流水线更新队列--- 并发小
     */
    @EventConsumer
    fun pipelineUpdateConsumer(
        @Autowired updateListener: MQPipelineUpdateListener
    ) = ScsConsumerBuilder.build<PipelineUpdateEvent> { updateListener.run(it) }

    /**
     * 流水线恢复队列--- 并发一般
     */
    @EventConsumer
    fun pipelineRestoreConsumer(
        @Autowired restoreListener: MQPipelineRestoreListener
    ) = ScsConsumerBuilder.build<PipelineRestoreEvent> { restoreListener.run(it) }
}
