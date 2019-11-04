/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.dockerhost.cron

import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dockerhost.dispatch.AlertApi
import com.tencent.devops.dockerhost.exception.ContainerException
import com.tencent.devops.dockerhost.exception.NoSuchImageException
import com.tencent.devops.dockerhost.service.DockerHostDebugService
import com.tencent.devops.dockerhost.utils.CommonUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

// @Component
class DebugRunner @Autowired constructor(
    private val dockerHostDebugService: DockerHostDebugService
) {
    private val logger = LoggerFactory.getLogger(DebugRunner::class.java)
    private val maxRunningContainerNum = 200
    private val alertApi: AlertApi =
        AlertApi()

//    @Scheduled(initialDelay = 30 * 1000, fixedDelay = 5 * 1000)
    fun startBuild() {
        try {
            val containerNum = dockerHostDebugService.getContainerNum()
            if (containerNum >= maxRunningContainerNum) {
                logger.warn("Too many containers in this host, break to start debug.")
                alertApi.alert(AlertLevel.HIGH.name, "Docker构建机运行的容器太多", "Docker构建机运行的容器太多, " +
                        "母机IP:${CommonUtils.getInnerIP()}， 容器数量: $containerNum")
                return
            }

            val dockerStartDebugInfo = try {
                dockerHostDebugService.startDebug()
            } catch (e: Exception) {
                logger.warn("Fail to start debug", e)
                return
            }
            if (dockerStartDebugInfo != null) {
                if (dockerStartDebugInfo.status == PipelineTaskStatus.RUNNING.status) {
                    logger.warn("Create debug container, dockerStartDebugInfo: $dockerStartDebugInfo")
                    try {
                        val containerId = dockerHostDebugService.createContainer(dockerStartDebugInfo)
                        // 上报containerId给dispatch
                        dockerHostDebugService.reportDebugContainerId(dockerStartDebugInfo.pipelineId, dockerStartDebugInfo.vmSeqId, containerId)
                    } catch (e: NoSuchImageException) {
                        logger.error("Create debug container failed, no such image. pipelineId: ${dockerStartDebugInfo.pipelineId}, vmSeqId: ${dockerStartDebugInfo.vmSeqId}, err: ${e.message}")
                        dockerHostDebugService.rollbackDebug(dockerStartDebugInfo.pipelineId, dockerStartDebugInfo.vmSeqId, true, e.message)
                    } catch (e: ContainerException) {
                        logger.error("Create debug container failed, rollback debug. pipelineId: ${dockerStartDebugInfo.pipelineId}, vmSeqId: ${dockerStartDebugInfo.vmSeqId}")
                        dockerHostDebugService.rollbackDebug(dockerStartDebugInfo.pipelineId, dockerStartDebugInfo.vmSeqId, true, e.message)
                    }
                }
            }
        } catch (t: Throwable) {
            logger.error("Start debug encounter unknown exception", t)
        }
    }

//    @Scheduled(initialDelay = 60 * 1000, fixedDelay = 10 * 1000)
    fun endBuild() {
        try {
            val dockerEndDebugInfo = try {
                dockerHostDebugService.endDebug()
            } catch (e: Exception) {
                logger.warn("Fail to end debug", e)
                return
            }
            if (dockerEndDebugInfo != null) {
                logger.warn("dockerEndDebugInfo: $dockerEndDebugInfo")
                if (dockerEndDebugInfo.status == PipelineTaskStatus.DONE.status || dockerEndDebugInfo.status == PipelineTaskStatus.FAILURE.status) {
                    logger.warn("Stop the container, containerId: ${dockerEndDebugInfo.containerId}")
                    dockerHostDebugService.stopContainer(dockerEndDebugInfo)
                }
            }
        } catch (t: Throwable) {
            logger.error("EndBuild encounter unknown exception", t)
        }
    }
}
