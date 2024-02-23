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

package com.tencent.devops.dispatch.kubernetes.config

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.dispatch.kubernetes.listeners.KubernetesListener
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownDemoteEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupDemoteEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import java.util.function.Consumer

@Configuration
class DispatchKubernetesMQConfiguration @Autowired constructor() {

    companion object {
        const val STREAM_CONSUMER_GROUP = "dispatch-kubernetes-service"
    }

    @EventConsumer(StreamBinding.QUEUE_AGENT_STARTUP, STREAM_CONSUMER_GROUP)
    fun startKubernetesListener(
        @Autowired kubernetesListener: KubernetesListener
    ): Consumer<Message<PipelineAgentStartupEvent>> {
        return Consumer { event: Message<PipelineAgentStartupEvent> ->
            kubernetesListener.handleStartup(event.payload)
        }
    }

    @EventConsumer(StreamBinding.QUEUE_AGENT_SHUTDOWN, STREAM_CONSUMER_GROUP)
    fun shutdownKubernetesListener(
        @Autowired kubernetesListener: KubernetesListener
    ): Consumer<Message<PipelineAgentShutdownEvent>> {
        return Consumer { event: Message<PipelineAgentShutdownEvent> ->
            kubernetesListener.handleShutdownMessage(event.payload)
        }
    }

    @EventConsumer(StreamBinding.QUEUE_AGENT_DEMOTE_STARTUP, STREAM_CONSUMER_GROUP)
    fun startDemoteKubernetesListener(
        @Autowired kubernetesListener: KubernetesListener
    ): Consumer<Message<PipelineAgentStartupDemoteEvent>> {
        return Consumer { event: Message<PipelineAgentStartupDemoteEvent> ->
            with(event.payload) {
                kubernetesListener.handleStartup(PipelineAgentStartupEvent(
                    source = source,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    pipelineName = pipelineName,
                    buildId = buildId,
                    buildNo = buildNo,
                    vmSeqId = vmSeqId,
                    taskName = taskName,
                    os = os,
                    vmNames = vmNames,
                    channelCode = channelCode,
                    dispatchType = dispatchType,
                    containerId = containerId,
                    containerHashId = containerHashId,
                    queueTimeoutMinutes = queueTimeoutMinutes,
                    atoms = atoms,
                    executeCount = executeCount,
                    customBuildEnv = customBuildEnv,
                    dockerRoutingType = dockerRoutingType,
                    routeKeySuffix = routeKeySuffix,
                    actionType = actionType,
                    delayMills = delayMills
                ))
            }
        }
    }

    @EventConsumer(StreamBinding.QUEUE_AGENT_DEMOTE_SHUTDOWN, STREAM_CONSUMER_GROUP)
    fun shutdownDemoteKubernetesListener(
        @Autowired kubernetesListener: KubernetesListener
    ): Consumer<Message<PipelineAgentShutdownDemoteEvent>> {
        return Consumer { event: Message<PipelineAgentShutdownDemoteEvent> ->
            with(event.payload) {
                kubernetesListener.handleShutdownMessage(
                    PipelineAgentShutdownEvent(
                        source = source,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        userId = userId,
                        buildId = buildId,
                        vmSeqId = vmSeqId,
                        buildResult = buildResult,
                        executeCount = executeCount,
                        dockerRoutingType = dockerRoutingType,
                        dispatchType = dispatchType,
                        routeKeySuffix = routeKeySuffix,
                        actionType = actionType,
                        delayMills = delayMills
                    )
                )
            }
        }
    }
}
