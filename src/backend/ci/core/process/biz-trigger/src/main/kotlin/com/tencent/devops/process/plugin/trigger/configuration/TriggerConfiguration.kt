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

package com.tencent.devops.process.plugin.trigger.configuration

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.plugin.trigger.pojo.event.PipelineTimerBuildEvent
import com.tencent.devops.process.plugin.trigger.pojo.event.PipelineTimerChangeEvent
import com.tencent.devops.process.plugin.trigger.service.PipelineTimerService
import com.tencent.devops.process.plugin.trigger.timer.SchedulerManager
import com.tencent.devops.process.plugin.trigger.timer.listener.PipelineTimerBuildListener
import com.tencent.devops.process.plugin.trigger.timer.listener.PipelineTimerChangerListener
import com.tencent.devops.process.plugin.trigger.timer.quartz.PipelineJobBean
import com.tencent.devops.process.plugin.trigger.timer.quartz.PipelineQuartzService
import com.tencent.devops.process.plugin.trigger.timer.quartz.QuartzSchedulerManager
import com.tencent.devops.process.plugin.trigger.timer.quartz.QuartzTraceJobListener
import com.tencent.devops.process.service.CreateStreamTriggerSupportService
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.trigger.PipelineTriggerMeasureService
import com.tencent.devops.process.trigger.client.TriggerProcessClient
import com.tencent.devops.process.yaml.PipelineYamlFacadeService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.quartz.QuartzProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * process 与 trigger 共用这份定时触发配置，由各自部署配置中的 trigger.standalone.timer.enabled 决定本服务是否加载
 */
@Configuration
@EnableConfigurationProperties(QuartzProperties::class)
@ConditionalOnProperty(
    prefix = "trigger.standalone.timer",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@SuppressWarnings("TooManyFunctions")
class TriggerConfiguration {
    @Bean
    @SuppressWarnings("LongParameterList")
    fun pipelineJobBean(
        pipelineEventDispatcher: PipelineEventDispatcher,
        schedulerManager: SchedulerManager,
        pipelineTimerService: PipelineTimerService,
        redisOperation: RedisOperation,
        client: Client,
        pipelineRepositoryService: PipelineRepositoryService
    ): PipelineJobBean {
        return PipelineJobBean(
            pipelineEventDispatcher = pipelineEventDispatcher,
            schedulerManager = schedulerManager,
            pipelineTimerService = pipelineTimerService,
            redisOperation = redisOperation,
            client = client,
            pipelineRepositoryService = pipelineRepositoryService
        )
    }

    @Bean
    fun schedulerManager(
        quartzProperties: QuartzProperties,
        quartzTraceJobListener: QuartzTraceJobListener
    ) = QuartzSchedulerManager(
        quartzProperties, quartzTraceJobListener
    )

    @Bean
    fun quartzTraceJobListener(
        pipelineTriggerMeasureService: PipelineTriggerMeasureService
    ) = QuartzTraceJobListener(pipelineTriggerMeasureService)

    @Bean
    fun pipelineQuartzService(
        pipelineTimerService: PipelineTimerService,
        schedulerManager: SchedulerManager
    ) = PipelineQuartzService(pipelineTimerService, schedulerManager)

    @Bean
    fun pipelineTimerChangerListener(
        pipelineEventDispatcher: PipelineEventDispatcher,
        schedulerManager: SchedulerManager
    ) = PipelineTimerChangerListener(pipelineEventDispatcher, schedulerManager)

    @Bean
    @SuppressWarnings("LongParameterList")
    fun pipelineTimerBuildListener(
        pipelineEventDispatcher: PipelineEventDispatcher,
        triggerProcessClient: TriggerProcessClient,
        pipelineTimerService: PipelineTimerService,
        scmProxyService: ScmProxyService,
        triggerEventService: PipelineTriggerEventService,
        pipelineRepositoryService: PipelineRepositoryService,
        pipelineTriggerMeasureService: PipelineTriggerMeasureService,
        creativeStreamService: CreateStreamTriggerSupportService,
        pipelineYamlFacadeService: PipelineYamlFacadeService
    ) = PipelineTimerBuildListener(
        pipelineEventDispatcher = pipelineEventDispatcher,
        triggerProcessClient = triggerProcessClient,
        pipelineTimerService = pipelineTimerService,
        scmProxyService = scmProxyService,
        triggerEventService = triggerEventService,
        pipelineRepositoryService = pipelineRepositoryService,
        pipelineTriggerMeasureService = pipelineTriggerMeasureService,
        creativeStreamService = creativeStreamService,
        pipelineYamlFacadeService = pipelineYamlFacadeService
    )

    /**
     * 定时构建队列--- 并发一般
     */
    @EventConsumer(groupName = "process\${service-suffix}-timerTriggerConsumer")
    fun timerTriggerConsumer(
        @Autowired buildListener: PipelineTimerBuildListener
    ) = ScsConsumerBuilder.build<PipelineTimerBuildEvent> { buildListener.run(it) }

    /**
     * 构建定时构建定时变化的广播交换机
     */
    @EventConsumer(true)
    fun timerChangeConsumer(
        @Autowired buildListener: PipelineTimerChangerListener
    ) = ScsConsumerBuilder.build<PipelineTimerChangeEvent> { buildListener.run(it) }
}
