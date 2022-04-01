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

package com.tencent.devops.dockerhost.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.dispatch.docker.pojo.ContainerInfo
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dockerhost.dispatch.AlertApi
import com.tencent.devops.dockerhost.exception.ContainerException
import com.tencent.devops.dockerhost.exception.NoSuchImageException
import com.tencent.devops.dockerhost.service.DockerHostDebugService
import com.tencent.devops.dockerhost.utils.CommonUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceDockerDebugResourceImpl @Autowired constructor(
    private val dockerHostDebugService: DockerHostDebugService,
    private val alertApi: AlertApi
) : ServiceDockerDebugResource {
    companion object {
        private val logger = LoggerFactory.getLogger(ServiceDockerDebugResourceImpl::class.java)
        private const val maxRunningContainerNum = 200
    }

    override fun startDebug(dockerStartDebugInfo: ContainerInfo): Result<String> {
        try {
            val containerNum = dockerHostDebugService.getContainerNum()
            if (containerNum >= maxRunningContainerNum) {
                logger.warn("Too many containers in this host, break to start debug.")
                alertApi.alert(AlertLevel.HIGH.name, "Docker构建机运行的容器太多", "Docker构建机运行的容器太多, " +
                        "母机IP:${CommonUtils.getInnerIP()}， 容器数量: $containerNum")
                return Result(1, "Too many containers in this host, break to start debug.", "")
            }

            if (dockerStartDebugInfo.status == PipelineTaskStatus.RUNNING.status) {
                logger.warn("Create debug container, dockerStartDebugInfo: $dockerStartDebugInfo")
                return try {
                    val containerId = dockerHostDebugService.createContainer(dockerStartDebugInfo)
                    Result(containerId)
                } catch (e: NoSuchImageException) {
                    logger.error("Create debug container failed, no such image. pipelineId: ${dockerStartDebugInfo.pipelineId}, vmSeqId: ${dockerStartDebugInfo.vmSeqId}, err: ${e.message}")
                    Result(2, "Create debug container failed, no such image.", "")
                } catch (e: ContainerException) {
                    logger.error("Create debug container failed. pipelineId: ${dockerStartDebugInfo.pipelineId}, vmSeqId: ${dockerStartDebugInfo.vmSeqId}")
                    Result(3, "Create debug container failed.", "")
                }
            }
        } catch (t: Throwable) {
            logger.error("Start debug encounter unknown exception", t)
            return Result(4, "Start debug encounter unknown exception.", "")
        }

        return Result("")
    }

    override fun getWebSocketUrl(projectId: String, pipelineId: String, containerId: String): Result<String> {
        return Result(dockerHostDebugService.getWebSocketUrl(projectId, pipelineId, containerId))
    }

    override fun endDebug(dockerEndDebugInfo: ContainerInfo): Result<Boolean> {
        return try {
            logger.warn("Stop the container, containerId: ${dockerEndDebugInfo.containerId}")
            dockerHostDebugService.stopContainer(dockerEndDebugInfo)

            Result(true)
        } catch (t: Throwable) {
            logger.error("EndBuild encounter unknown exception", t)
            Result(1, "EndBuild encounter unknown exception", false)
        }
    }
}
