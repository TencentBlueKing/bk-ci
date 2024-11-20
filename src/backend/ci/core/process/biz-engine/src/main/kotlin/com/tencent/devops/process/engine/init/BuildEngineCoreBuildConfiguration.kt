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

package com.tencent.devops.process.engine.init

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.process.engine.control.BuildCancelControl
import com.tencent.devops.process.engine.control.BuildEndControl
import com.tencent.devops.process.engine.control.BuildStartControl
import com.tencent.devops.process.engine.control.ContainerControl
import com.tencent.devops.process.engine.control.StageControl
import com.tencent.devops.process.engine.control.TaskControl
import com.tencent.devops.process.engine.listener.run.PipelineAtomTaskBuildListener
import com.tencent.devops.process.engine.listener.run.PipelineBuildStartListener
import com.tencent.devops.process.engine.listener.run.PipelineContainerBuildListener
import com.tencent.devops.process.engine.listener.run.PipelineStageBuildListener
import com.tencent.devops.process.engine.listener.run.PipelineTaskPauseListener
import com.tencent.devops.process.engine.listener.run.finish.PipelineBuildCancelListener
import com.tencent.devops.process.engine.listener.run.finish.PipelineBuildFinishListener
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildCancelEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildFinishEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStageEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStartEvent
import com.tencent.devops.process.engine.pojo.event.PipelineTaskPauseEvent
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.engine.service.record.TaskBuildRecordService
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.PipelineTaskPauseService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 流水线构建核心配置
 */
@Configuration
@Suppress("LongParameterList", "TooManyFunctions")
class BuildEngineCoreBuildConfiguration {
    /**
     * 入口：整个构建开始队列---- 并发一般
     */
    @Bean
    fun pipelineBuildStartListener(
        @Autowired buildControl: BuildStartControl,
        @Autowired pipelineEventDispatcher: PipelineEventDispatcher
    ) = PipelineBuildStartListener(
        buildControl = buildControl,
        pipelineEventDispatcher = pipelineEventDispatcher
    )

    @EventConsumer
    fun buildStartConsumer(
        @Autowired buildListener: PipelineBuildStartListener
    ) = ScsConsumerBuilder.build<PipelineBuildStartEvent> { buildListener.run(it) }

    /**
     * 构建结束队列--- 并发一般，与Stage一致
     */
    @Bean
    fun pipelineBuildFinishListener(
        @Autowired buildEndControl: BuildEndControl,
        @Autowired pipelineEventDispatcher: PipelineEventDispatcher
    ) = PipelineBuildFinishListener(
        buildEndControl = buildEndControl,
        pipelineEventDispatcher = pipelineEventDispatcher
    )

    @EventConsumer
    fun buildFinishConsumer(
        @Autowired buildListener: PipelineBuildFinishListener
    ) = ScsConsumerBuilder.build<PipelineBuildFinishEvent> { buildListener.run(it) }

    /**
     * 构建取消队列--- 并发一般，与Stage一致
     */
    @Bean
    fun pipelineBuildCancelListener(
        @Autowired buildCancelControl: BuildCancelControl,
        @Autowired pipelineEventDispatcher: PipelineEventDispatcher
    ) = PipelineBuildCancelListener(
        buildCancelControl = buildCancelControl,
        pipelineEventDispatcher = pipelineEventDispatcher
    )

    @EventConsumer
    fun buildCancelConsumer(
        @Autowired buildListener: PipelineBuildCancelListener
    ) = ScsConsumerBuilder.build<PipelineBuildCancelEvent> { buildListener.run(it) }

    /**
     * Stage构建队列---- 并发一般
     */
    @Bean
    fun pipelineStageBuildListener(
        @Autowired stageControl: StageControl,
        @Autowired pipelineEventDispatcher: PipelineEventDispatcher
    ) = PipelineStageBuildListener(
        stageControl = stageControl,
        pipelineEventDispatcher = pipelineEventDispatcher
    )

    @EventConsumer
    fun stageBuildConsumer(
        @Autowired buildListener: PipelineStageBuildListener
    ) = ScsConsumerBuilder.build<PipelineBuildStageEvent> { buildListener.run(it) }

    /**
     * Job构建队列---- 并发一般
     */
    @Bean
    fun pipelineContainerBuildListener(
        @Autowired containerControl: ContainerControl,
        @Autowired pipelineEventDispatcher: PipelineEventDispatcher
    ) = PipelineContainerBuildListener(
        containerControl = containerControl,
        pipelineEventDispatcher = pipelineEventDispatcher
    )

    @EventConsumer
    fun containerBuildConsumer(
        @Autowired buildListener: PipelineContainerBuildListener
    ) = ScsConsumerBuilder.build<PipelineBuildContainerEvent> { buildListener.run(it) }

    /**
     * 任务队列---- 并发要大
     */
    @Bean
    fun pipelineAtomTaskBuildListener(
        @Autowired taskControl: TaskControl,
        @Autowired pipelineEventDispatcher: PipelineEventDispatcher
    ) = PipelineAtomTaskBuildListener(
        taskControl = taskControl,
        pipelineEventDispatcher = pipelineEventDispatcher
    )

    @EventConsumer
    fun taskBuildConsumer(
        @Autowired buildListener: PipelineAtomTaskBuildListener
    ) = ScsConsumerBuilder.build<PipelineBuildAtomTaskEvent> { buildListener.run(it) }

    /**
     * 流水线暂停操作队列
     */
    @Bean
    fun pipelineTaskPauseListener(
        @Autowired redisOperation: RedisOperation,
        @Autowired taskBuildRecordService: TaskBuildRecordService,
        @Autowired pipelineTaskService: PipelineTaskService,
        @Autowired pipelineContainerService: PipelineContainerService,
        @Autowired pipelineTaskPauseService: PipelineTaskPauseService,
        @Autowired buildVariableService: BuildVariableService,
        @Autowired dslContext: DSLContext,
        @Autowired buildLogPrinter: BuildLogPrinter,
        @Autowired pipelineEventDispatcher: PipelineEventDispatcher
    ) = PipelineTaskPauseListener(
        redisOperation = redisOperation,
        taskBuildRecordService = taskBuildRecordService,
        pipelineTaskService = pipelineTaskService,
        pipelineContainerService = pipelineContainerService,
        pipelineTaskPauseService = pipelineTaskPauseService,
        buildVariableService = buildVariableService,
        dslContext = dslContext,
        buildLogPrinter = buildLogPrinter,
        pipelineEventDispatcher = pipelineEventDispatcher
    )

    @EventConsumer
    fun taskPauseConsumer(
        @Autowired buildListener: PipelineTaskPauseListener
    ) = ScsConsumerBuilder.build<PipelineTaskPauseEvent> { buildListener.run(it) }
}
