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

package com.tencent.devops.dispatch.service.dispatcher.docker

import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.client.DockerHostClient
import com.tencent.devops.dispatch.dao.PipelineDockerTaskHistoryDao
import com.tencent.devops.dispatch.pojo.VolumeStatus
import com.tencent.devops.dispatch.service.DockerHostBuildService
import com.tencent.devops.dispatch.service.dispatcher.Dispatcher
import com.tencent.devops.dispatch.utils.DockerHostLock
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DockerDispatcher @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate,
    private val dockerHostBuildService: DockerHostBuildService,
    private val dockerHostClient: DockerHostClient,
    private val pipelineDockerTaskHistoryDao: PipelineDockerTaskHistoryDao,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation
    ) : Dispatcher {

    companion object {
        private val logger = LoggerFactory.getLogger(DockerDispatcher::class.java)
    }

    override fun canDispatch(pipelineAgentStartupEvent: PipelineAgentStartupEvent) =
        pipelineAgentStartupEvent.dispatchType is DockerDispatchType

    override fun startUp(pipelineAgentStartupEvent: PipelineAgentStartupEvent) {
        val dockerDispatch = pipelineAgentStartupEvent.dispatchType as DockerDispatchType
        LogUtils.addLine(
            rabbitTemplate,
            pipelineAgentStartupEvent.buildId,
            "Start docker ${dockerDispatch.dockerBuildVersion} for the build",
            "",
            pipelineAgentStartupEvent.containerHashId,
            pipelineAgentStartupEvent.executeCount ?: 1
        )
        // dockerHostBuildService.dockerHostBuild(pipelineAgentStartupEvent)

        val taskHistory = pipelineDockerTaskHistoryDao.getByPipelineIdAndVMSeq(dslContext, pipelineAgentStartupEvent.pipelineId, pipelineAgentStartupEvent.vmSeqId)
        val dockerIp: String
        if (taskHistory != null) {
            dockerIp = taskHistory.dockerIp
        } else {
            dockerIp = dockerHostClient.getAvailableDockerIp()

            pipelineDockerTaskHistoryDao.create(
                dslContext,
                pipelineAgentStartupEvent.pipelineId,
                pipelineAgentStartupEvent.buildId,
                pipelineAgentStartupEvent.vmSeqId,
                dockerIp,
                VolumeStatus.RUNNING.status
            )
        }

        dockerHostClient.startBuild(pipelineAgentStartupEvent, dockerIp)
    }

    override fun shutdown(pipelineAgentShutdownEvent: PipelineAgentShutdownEvent) {
        dockerHostBuildService.finishDockerBuild(
            pipelineAgentShutdownEvent.buildId,
            pipelineAgentShutdownEvent.vmSeqId,
            pipelineAgentShutdownEvent.buildResult
        )

        logger.info("On shutdown - ($pipelineAgentShutdownEvent|$)")
        val lock = DockerHostLock(redisOperation, pipelineAgentShutdownEvent.pipelineId)
        try {
            lock.lock()
            if (pipelineAgentShutdownEvent.vmSeqId != null) {
                val taskHistory = pipelineDockerTaskHistoryDao.getByPipelineIdAndVMSeq(dslContext, pipelineAgentShutdownEvent.pipelineId, pipelineAgentShutdownEvent.vmSeqId!!)
                dockerHostClient.endBuild(pipelineAgentShutdownEvent, taskHistory!!.dockerIp as String, taskHistory.containerId as String)
                pipelineDockerTaskHistoryDao.updateStatus(dslContext, pipelineAgentShutdownEvent.buildId, pipelineAgentShutdownEvent.vmSeqId!!, VolumeStatus.FINISH.status)
            } else {
                val taskHistoryList = pipelineDockerTaskHistoryDao.getByPipelineId(dslContext, pipelineAgentShutdownEvent.pipelineId)
                taskHistoryList.forEach {
                    dockerHostClient.endBuild(pipelineAgentShutdownEvent, it.dockerIp as String, it.containerId as String)
                    if (it.status == VolumeStatus.RUNNING.status) {
                        pipelineDockerTaskHistoryDao.updateStatus(dslContext, pipelineAgentShutdownEvent.buildId, it.vmSeq as String, VolumeStatus.FINISH.status)
                    }
                }
            }
        } catch (e: Exception) {
            logger.info("[${pipelineAgentShutdownEvent.projectId}|${pipelineAgentShutdownEvent.pipelineId}|${pipelineAgentShutdownEvent.buildId}] Shutdown Docker job failed. msg:${e.message}")
            throw RuntimeException("停止构建机失败，错误信息:${e.message}")
        } finally {
            lock.unlock()
        }
    }

//    override fun canDispatch(buildMessage: PipelineBuildMessage) =
//        buildMessage.dispatchType.buildType == BuildType.DOCKER
//
//    override fun build(buildMessage: PipelineBuildMessage) {
//        val dockerDispatch = buildMessage.dispatchType as DockerDispatchType
//        LogUtils.addLine(client, buildMessage.buildId, "Start docker ${dockerDispatch.dockerBuildVersion} for the build", "", buildMessage.executeCount ?: 1)
//        dockerHostBuildService.dockerHostBuild(buildMessage)
//    }
//
//    override fun finish(buildFinishMessage: PipelineFinishMessage) {
//        dockerHostBuildService.finishDockerBuild(
//            buildFinishMessage.buildId,
//            buildFinishMessage.vmSeqId,
//            buildFinishMessage.buildResult
//        )
//    }
}
