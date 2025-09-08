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
package com.tencent.devops.stream.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.event.dispatcher.mq.MQEventDispatcher
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQualityCheckBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildReviewBroadCastEvent
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.trigger.ScheduleTriggerService
import com.tencent.devops.stream.trigger.StreamTriggerRequestService
import com.tencent.devops.stream.trigger.StreamYamlTrigger
import com.tencent.devops.stream.trigger.actions.EventActionFactory
import com.tencent.devops.stream.trigger.exception.handler.StreamTriggerExceptionHandler
import com.tencent.devops.stream.trigger.git.service.GithubApiService
import com.tencent.devops.stream.trigger.git.service.TGitApiService
import com.tencent.devops.stream.trigger.listener.StreamBuildFinishListenerService
import com.tencent.devops.stream.trigger.listener.StreamBuildQualityCheckListener
import com.tencent.devops.stream.trigger.listener.StreamBuildReviewListener
import com.tencent.devops.stream.trigger.listener.components.SendCommitCheck
import com.tencent.devops.stream.trigger.listener.components.SendNotify
import com.tencent.devops.stream.trigger.listener.components.SendQualityMrComment
import com.tencent.devops.stream.trigger.mq.streamMrConflict.StreamMrConflictCheckEvent
import com.tencent.devops.stream.trigger.mq.streamMrConflict.StreamMrConflictCheckListener
import com.tencent.devops.stream.trigger.mq.streamRequest.StreamRequestEvent
import com.tencent.devops.stream.trigger.mq.streamRequest.StreamRequestListener
import com.tencent.devops.stream.trigger.mq.streamTrigger.StreamTriggerEvent
import com.tencent.devops.stream.trigger.mq.streamTrigger.StreamTriggerListener
import com.tencent.devops.stream.trigger.parsers.MergeConflictCheck
import com.tencent.devops.stream.trigger.timer.SchedulerManager
import com.tencent.devops.stream.trigger.timer.listener.StreamTimerBuildListener
import com.tencent.devops.stream.trigger.timer.listener.StreamTimerChangerListener
import com.tencent.devops.stream.trigger.timer.pojo.event.StreamChangeEvent
import com.tencent.devops.stream.trigger.timer.pojo.event.StreamTimerBuildEvent
import com.tencent.devops.stream.trigger.timer.service.StreamTimerBranchService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@Suppress("LongParameterList")
class StreamMQConfiguration {
    @Bean
    fun pipelineEventDispatcher(streamBridge: StreamBridge) = MQEventDispatcher(streamBridge)

    @Bean
    fun streamBuildFinishListenerService(
        @Autowired dslContext: DSLContext,
        @Autowired objectMapper: ObjectMapper,
        @Autowired actionFactory: EventActionFactory,
        @Autowired sendCommitCheck: SendCommitCheck,
        @Autowired sendNotify: SendNotify,
        @Autowired streamGitConfig: StreamGitConfig,
        @Autowired gitRequestEventBuildDao: GitRequestEventBuildDao,
        @Autowired gitRequestEventDao: GitRequestEventDao,
        @Autowired streamBasicSettingDao: StreamBasicSettingDao,
        @Autowired gitPipelineResourceDao: GitPipelineResourceDao
    ) = StreamBuildFinishListenerService(
        dslContext = dslContext,
        objectMapper = objectMapper,
        actionFactory = actionFactory,
        sendCommitCheck = sendCommitCheck,
        sendNotify = sendNotify,
        streamGitConfig = streamGitConfig,
        gitRequestEventBuildDao = gitRequestEventBuildDao,
        gitRequestEventDao = gitRequestEventDao,
        streamBasicSettingDao = streamBasicSettingDao,
        gitPipelineResourceDao = gitPipelineResourceDao
    )

    @EventConsumer
    fun buildFinishConsumer(
        @Autowired finishListenerService: StreamBuildFinishListenerService
    ) = ScsConsumerBuilder.build<PipelineBuildFinishBroadCastEvent> { finishListenerService.doFinish(it) }

    @Bean
    fun streamBuildQualityCheckListener(
        @Autowired dslContext: DSLContext,
        @Autowired objectMapper: ObjectMapper,
        @Autowired actionFactory: EventActionFactory,
        @Autowired streamGitConfig: StreamGitConfig,
        @Autowired gitRequestEventBuildDao: GitRequestEventBuildDao,
        @Autowired gitRequestEventDao: GitRequestEventDao,
        @Autowired gitPipelineResourceDao: GitPipelineResourceDao,
        @Autowired streamBasicSettingDao: StreamBasicSettingDao,
        @Autowired sendQualityMrComment: SendQualityMrComment
    ) = StreamBuildQualityCheckListener(
        dslContext = dslContext,
        objectMapper = objectMapper,
        actionFactory = actionFactory,
        streamGitConfig = streamGitConfig,
        gitRequestEventBuildDao = gitRequestEventBuildDao,
        gitRequestEventDao = gitRequestEventDao,
        streamBasicSettingDao = streamBasicSettingDao,
        gitPipelineResourceDao = gitPipelineResourceDao,
        sendQualityMrComment = sendQualityMrComment
    )

    @EventConsumer
    fun buildQualityCheckConsumer(
        @Autowired checkListener: StreamBuildQualityCheckListener
    ) = ScsConsumerBuilder.build<PipelineBuildQualityCheckBroadCastEvent> {
        checkListener.buildQualityCheckListener(it)
    }

    @Bean
    fun streamTriggerListener(
        @Autowired exceptionHandler: StreamTriggerExceptionHandler,
        @Autowired streamYamlTrigger: StreamYamlTrigger,
        @Autowired actionFactory: EventActionFactory
    ) = StreamTriggerListener(
        exceptionHandler = exceptionHandler,
        streamYamlTrigger = streamYamlTrigger,
        actionFactory = actionFactory
    )

    @EventConsumer
    fun triggerConsumer(
        @Autowired streamTriggerListener: StreamTriggerListener
    ) = ScsConsumerBuilder.build<StreamTriggerEvent> { streamTriggerListener.listenStreamTriggerEvent(it) }

    @Bean
    fun streamMrConflictCheckListener(
        @Autowired mergeConflictCheck: MergeConflictCheck,
        @Autowired streamTriggerRequestService: StreamTriggerRequestService,
        @Autowired eventDispatcher: SampleEventDispatcher,
        @Autowired exHandler: StreamTriggerExceptionHandler,
        @Autowired actionFactory: EventActionFactory
    ) = StreamMrConflictCheckListener(
        mergeConflictCheck = mergeConflictCheck,
        streamTriggerRequestService = streamTriggerRequestService,
        actionFactory = actionFactory,
        eventDispatcher = eventDispatcher,
        exHandler = exHandler
    )

    @EventConsumer
    fun conflictCheckConsumer(
        @Autowired checkListener: StreamMrConflictCheckListener
    ) = ScsConsumerBuilder.build<StreamMrConflictCheckEvent> { checkListener.listenGitCIRequestTriggerEvent(it) }

    @Bean
    fun streamRequestListener(
        @Autowired steamRequestService: StreamTriggerRequestService
    ) = StreamRequestListener(
        steamRequestService = steamRequestService
    )

    @EventConsumer
    fun requestConsumer(
        @Autowired requestListener: StreamRequestListener
    ) = ScsConsumerBuilder.build<StreamRequestEvent> { requestListener.listenStreamRequestEvent(it) }

    @Bean
    fun streamTimerBuildListener(
        @Autowired pipelineEventDispatcher: PipelineEventDispatcher,
        @Autowired dslContext: DSLContext,
        @Autowired streamTimerBranchService: StreamTimerBranchService,
        @Autowired scheduleTriggerService: ScheduleTriggerService,
        @Autowired streamBasicSettingDao: StreamBasicSettingDao,
        @Autowired streamGitConfig: StreamGitConfig,
        @Autowired tGitApiService: TGitApiService,
        @Autowired githubApiService: GithubApiService
    ) = StreamTimerBuildListener(
        dslContext = dslContext,
        pipelineEventDispatcher = pipelineEventDispatcher,
        streamTimerBranchService = streamTimerBranchService,
        scheduleTriggerService = scheduleTriggerService,
        streamBasicSettingDao = streamBasicSettingDao,
        streamGitConfig = streamGitConfig,
        tGitApiService = tGitApiService,
        githubApiService = githubApiService
    )

    @EventConsumer
    fun timerBuildConsumer(
        @Autowired buildListener: StreamTimerBuildListener
    ) = ScsConsumerBuilder.build<StreamTimerBuildEvent> { buildListener.run(it) }

    @Bean
    fun streamTimerChangerListener(
        @Autowired pipelineEventDispatcher: PipelineEventDispatcher,
        @Autowired schedulerManager: SchedulerManager
    ) = StreamTimerChangerListener(
        pipelineEventDispatcher = pipelineEventDispatcher,
        schedulerManager = schedulerManager
    )

    // 每个实例都需要刷新自己维护的定时任务
    @EventConsumer(true)
    fun timerChangerConsumer(
        @Autowired buildListener: StreamTimerChangerListener
    ) = ScsConsumerBuilder.build<StreamChangeEvent> { buildListener.run(it) }

    @Bean
    fun streamBuildReviewListener(
        @Autowired dslContext: DSLContext,
        @Autowired objectMapper: ObjectMapper,
        @Autowired actionFactory: EventActionFactory,
        @Autowired streamGitConfig: StreamGitConfig,
        @Autowired gitRequestEventBuildDao: GitRequestEventBuildDao,
        @Autowired gitRequestEventDao: GitRequestEventDao,
        @Autowired gitPipelineResourceDao: GitPipelineResourceDao,
        @Autowired streamBasicSettingDao: StreamBasicSettingDao,
        @Autowired sendCommitCheck: SendCommitCheck
    ) = StreamBuildReviewListener(
        dslContext = dslContext,
        objectMapper = objectMapper,
        actionFactory = actionFactory,
        sendCommitCheck = sendCommitCheck,
        streamGitConfig = streamGitConfig,
        gitRequestEventBuildDao = gitRequestEventBuildDao,
        gitRequestEventDao = gitRequestEventDao,
        streamBasicSettingDao = streamBasicSettingDao,
        gitPipelineResourceDao = gitPipelineResourceDao
    )

    @EventConsumer
    fun buildReviewConsumer(
        @Autowired listener: StreamBuildReviewListener
    ) = ScsConsumerBuilder.build<PipelineBuildReviewBroadCastEvent> { listener.buildReviewListener(it) }
}
