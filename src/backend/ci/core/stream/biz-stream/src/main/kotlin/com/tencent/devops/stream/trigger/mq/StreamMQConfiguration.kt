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
package com.tencent.devops.stream.trigger.mq

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQualityCheckBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildReviewBroadCastEvent
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.constant.StreamMQ
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
import org.springframework.messaging.Message
import java.util.function.Consumer

@Configuration
@Suppress("LongParameterList")
class StreamMQConfiguration {

    companion object {
        const val STREAM_CONSUMER_GROUP = "stream-service"
    }

    @Bean
    fun sampleEventDispatcher(streamBridge: StreamBridge) = SampleEventDispatcher(streamBridge)

    @Bean
    fun pipelineEventDispatcher(streamBridge: StreamBridge) = SampleEventDispatcher(streamBridge)

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

    @EventConsumer(StreamBinding.EXCHANGE_PIPELINE_BUILD_FINISH_FANOUT, STREAM_CONSUMER_GROUP)
    fun buildFinishListener(
        @Autowired finishListenerService: StreamBuildFinishListenerService
    ): Consumer<Message<PipelineBuildFinishBroadCastEvent>> {
        return Consumer { event: Message<PipelineBuildFinishBroadCastEvent> ->
            finishListenerService.doFinish(event.payload)
        }
    }

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

    @EventConsumer(StreamBinding.EXCHANGE_PIPELINE_BUILD_QUALITY_CHECK_FANOUT, STREAM_CONSUMER_GROUP)
    fun buildQualityCheckListener(
        @Autowired checkListener: StreamBuildQualityCheckListener
    ): Consumer<Message<PipelineBuildQualityCheckBroadCastEvent>> {
        return Consumer { event: Message<PipelineBuildQualityCheckBroadCastEvent> ->
            checkListener.buildQualityCheckListener(event.payload)
        }
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

    @EventConsumer(StreamMQ.QUEUE_STREAM_TRIGGER_PIPELINE_EVENT, STREAM_CONSUMER_GROUP)
    fun triggerListener(
        @Autowired streamTriggerListener: StreamTriggerListener
    ): Consumer<Message<StreamTriggerEvent>> {
        return Consumer { event: Message<StreamTriggerEvent> ->
            streamTriggerListener.listenStreamTriggerEvent(event.payload)
        }
    }

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

    @EventConsumer(StreamMQ.QUEUE_STREAM_MR_CONFLICT_CHECK_EVENT, STREAM_CONSUMER_GROUP)
    fun conflictCheckListener(
        @Autowired checkListener: StreamMrConflictCheckListener
    ): Consumer<Message<StreamMrConflictCheckEvent>> {
        return Consumer { event: Message<StreamMrConflictCheckEvent> ->
            checkListener.listenGitCIRequestTriggerEvent(event.payload)
        }
    }

    @Bean
    fun streamRequestListener(
        @Autowired steamRequestService: StreamTriggerRequestService
    ) = StreamRequestListener(
        steamRequestService = steamRequestService
    )

    @EventConsumer(StreamMQ.QUEUE_STREAM_REQUEST_EVENT, STREAM_CONSUMER_GROUP)
    fun requestListener(
        @Autowired requestListener: StreamRequestListener
    ): Consumer<Message<StreamRequestEvent>> {
        return Consumer { event: Message<StreamRequestEvent> ->
            requestListener.listenStreamRequestEvent(event.payload)
        }
    }

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

    @EventConsumer(StreamMQ.QUEUE_STREAM_TIMER, STREAM_CONSUMER_GROUP)
    fun timerBuildListener(
        @Autowired buildListener: StreamTimerBuildListener
    ): Consumer<Message<StreamTimerBuildEvent>> {
        return Consumer { event: Message<StreamTimerBuildEvent> ->
            buildListener.run(event.payload)
        }
    }

    @Bean
    fun streamTimerChangerListener(
        @Autowired pipelineEventDispatcher: PipelineEventDispatcher,
        @Autowired schedulerManager: SchedulerManager
    ) = StreamTimerChangerListener(
        pipelineEventDispatcher = pipelineEventDispatcher,
        schedulerManager = schedulerManager
    )

    // 每个实例都需要刷新自己维护的定时任务
    @EventConsumer(StreamMQ.EXCHANGE_STREAM_TIMER_CHANGE_FANOUT, STREAM_CONSUMER_GROUP, true)
    fun timerChangerListener(
        @Autowired buildListener: StreamTimerChangerListener
    ): Consumer<Message<StreamChangeEvent>> {
        return Consumer { event: Message<StreamChangeEvent> ->
            buildListener.run(event.payload)
        }
    }

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

    @EventConsumer(StreamBinding.EXCHANGE_PIPELINE_BUILD_REVIEW_FANOUT, STREAM_CONSUMER_GROUP)
    fun buildReviewListener(
        @Autowired listener: StreamBuildReviewListener
    ): Consumer<Message<PipelineBuildReviewBroadCastEvent>> {
        return Consumer { event: Message<PipelineBuildReviewBroadCastEvent> ->
            listener.buildReviewListener(event.payload)
        }
    }
}
