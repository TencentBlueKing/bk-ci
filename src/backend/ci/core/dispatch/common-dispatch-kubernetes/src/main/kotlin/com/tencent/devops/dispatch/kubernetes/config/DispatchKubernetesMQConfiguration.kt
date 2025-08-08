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

package com.tencent.devops.dispatch.kubernetes.config

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.dispatch.kubernetes.listeners.KubernetesListener
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownDemoteEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupDemoteEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Suppress("ALL")
@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class DispatchKubernetesMQConfiguration @Autowired constructor() {
    /**
     * 启动构建队列
     */
    @EventConsumer
    fun startKubernetesConsumer(
        @Autowired kubernetesListener: KubernetesListener
    ) = ScsConsumerBuilder.build<PipelineAgentStartupEvent> { kubernetesListener.handleStartup(it) }

    /**
     * 启动构建降级队列
     */
    @EventConsumer
    fun shutdownKubernetesConsumer(
        @Autowired kubernetesListener: KubernetesListener
    ) = ScsConsumerBuilder.build<PipelineAgentShutdownEvent> { kubernetesListener.handleShutdownMessage(it) }

    /**
     * 启动构建结束队列
     */
    @EventConsumer
    fun startDemoteKubernetesConsumer(
        @Autowired kubernetesListener: KubernetesListener
    ) = ScsConsumerBuilder.build<PipelineAgentStartupDemoteEvent> {
        with(it) {
            kubernetesListener.handleStartup(
                PipelineAgentStartupEvent(
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
                )
            )
        }
    }

    @EventConsumer
    fun shutdownDemoteKubernetesConsumer(
        @Autowired kubernetesListener: KubernetesListener
    ) = ScsConsumerBuilder.build<PipelineAgentShutdownDemoteEvent> {
        with(it) {
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
