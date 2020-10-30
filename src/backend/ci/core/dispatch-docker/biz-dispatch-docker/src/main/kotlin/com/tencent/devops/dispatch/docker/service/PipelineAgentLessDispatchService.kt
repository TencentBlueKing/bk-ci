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

package com.tencent.devops.dispatch.docker.service

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.service.JobQuotaService
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.dispatch.docker.service.dispatcher.BuildLessDispatcher
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.mq.PipelineBuildLessShutdownDispatchEvent
import com.tencent.devops.process.pojo.mq.PipelineBuildLessStartupDispatchEvent
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineAgentLessDispatchService @Autowired constructor(
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter,
    private val jobQuotaService: JobQuotaService
) {
    private var dispatchers: Set<BuildLessDispatcher>? = null

    private fun getDispatchers(): Set<BuildLessDispatcher> {
        if (dispatchers == null) {
            synchronized(this) {
                if (dispatchers == null) {
                    val reflections = Reflections("com.tencent.devops.dispatch.docker.service.dispatcher")
                    val dispatcherClasses = reflections.getSubTypesOf(BuildLessDispatcher::class.java)
                    if (dispatcherClasses == null || dispatcherClasses.isEmpty()) {
                        logger.error("The dispatcher is empty $dispatcherClasses")
                        throw InvalidParamException("Dispatcher is empty")
                    }
                    logger.info("Get the dispatch classes $dispatcherClasses")
                    dispatchers = dispatcherClasses.map {
                        SpringContextUtil.getBean(it)
                    }.toSet()
                }
            }
        }
        return dispatchers!!
    }

    fun startUpBuildLess(pipelineBuildLessAgentStartupEvent: PipelineBuildLessStartupDispatchEvent) {
        val pipelineId = pipelineBuildLessAgentStartupEvent.pipelineId
        val buildId = pipelineBuildLessAgentStartupEvent.buildId
        val vmSeqId = pipelineBuildLessAgentStartupEvent.vmSeqId
        logger.info("[$buildId]|BUILD_LESS| pipelineId=$pipelineId, seq($vmSeqId)")
        // Check if the pipeline is running
        val record = client.get(ServicePipelineResource::class).isPipelineRunning(
            pipelineBuildLessAgentStartupEvent.projectId,
            buildId,
            ChannelCode.valueOf(pipelineBuildLessAgentStartupEvent.channelCode)
        )
        if (record.isNotOk() || record.data == null) {
            logger.warn("[$buildId]|BUILD_LESS| Fail to check if pipeline is running because of ${record.message}")
            return
        }

        if (!record.data!!) {
            logger.warn("[$buildId]|BUILD_LESS| The build is not running")
            return
        }

        if (pipelineBuildLessAgentStartupEvent.retryTime == 0) {
            buildLogPrinter.addLine(
                buildId,
                "Prepare BuildLess Job(#$vmSeqId)...",
                "",
                pipelineBuildLessAgentStartupEvent.containerHashId,
                pipelineBuildLessAgentStartupEvent.executeCount ?: 1
            )
        }

        val dispatchType = pipelineBuildLessAgentStartupEvent.dispatchType
        logger.info("[$buildId]|BUILD_LESS| Get the dispatch $dispatchType")

        getDispatchers().forEach {
            if (it.canDispatch(pipelineBuildLessAgentStartupEvent)) {
                if (!jobQuotaService.checkJobQuotaAgentLess(pipelineBuildLessAgentStartupEvent, JobQuotaVmType.AGENTLESS)) {
                    logger.error("[$buildId]|BUILD_LESS| AgentLess Job quota exceed quota.")
                    return
                }
                it.startUp(pipelineBuildLessAgentStartupEvent)
                // 到这里说明JOB已经启动成功，开始累加使用额度
                jobQuotaService.addRunningJob(pipelineBuildLessAgentStartupEvent.projectId, JobQuotaVmType.AGENTLESS, pipelineBuildLessAgentStartupEvent.buildId, pipelineBuildLessAgentStartupEvent.vmSeqId)
                return
            }
        }
        throw InvalidParamException("Fail to find the right buildLessDispatcher for the build $dispatchType")
    }

    fun shutdown(event: PipelineBuildLessShutdownDispatchEvent) {
        try {
            logger.info("[${event.buildId}]| Start to finish the pipeline build($event)")
            getDispatchers().forEach {
                it.shutdown(event)
            }
        } finally {
            buildLogPrinter.stopLog(buildId = event.buildId, tag = "", jobId = null)
            // 不管shutdown成功失败，都要回收配额；这里回收job，将自动累加agent执行时间
            jobQuotaService.removeRunningJob(event.projectId, event.buildId, event.vmSeqId)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineAgentLessDispatchService::class.java)
    }
}
