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

package com.tencent.devops.dockerhost.listener

import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.dispatch.AlertApi
import com.tencent.devops.dockerhost.exception.ContainerException
import com.tencent.devops.dockerhost.services.DockerHostBuildLessService
import com.tencent.devops.dockerhost.utils.CommonUtils
import com.tencent.devops.process.pojo.mq.PipelineBuildLessDockerStartupEvent
import org.slf4j.LoggerFactory

/**
 * 无构建环境的容器启动消息
 * @version 1.0
 */
class BuildLessStartListener(
    private val dockerHostBuildLessService: DockerHostBuildLessService,
    private val dockerHostConfig: DockerHostConfig
) {

    private val alertApi: AlertApi = AlertApi(dockerHostConfig.grayEnv)

    private val maxRunningContainerNum = 200

    private val logger = LoggerFactory.getLogger(BuildLessStartListener::class.java)

    fun handleMessage(event: PipelineBuildLessDockerStartupEvent) {

        logger.info("[${event.buildId}]|Create container, event: $event")

        val containerId = try {
            val containerNum = dockerHostBuildLessService.getContainerNum()
            if (containerNum >= maxRunningContainerNum) {
                logger.warn("[${event.buildId}]|Too many containers in this host, break to start build.")
                dockerHostBuildLessService.retryDispatch(event)
                alertApi.alert(
                    AlertLevel.HIGH.name, "Docker无构建环境运行的容器太多", "Docker无构建环境运行的容器太多, " +
                        "母机IP:${CommonUtils.getInnerIP()}， 容器数量: $containerNum"
                )
                return
            }
            dockerHostBuildLessService.createContainer(event)
        } catch (e: ContainerException) {
            logger.error("[${event.buildId}]|Create container failed, rollback build. buildId: ${event.buildId}, vmSeqId: ${event.vmSeqId}")
            dockerHostBuildLessService.retryDispatch(event)
            return
        }
        logger.info("[${event.buildId}]|Create container=$containerId")
        dockerHostBuildLessService.reportContainerId(event.buildId, event.vmSeqId, containerId)
    }
}
