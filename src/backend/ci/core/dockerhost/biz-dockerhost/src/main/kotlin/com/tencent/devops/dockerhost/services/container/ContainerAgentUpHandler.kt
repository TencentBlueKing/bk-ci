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

package com.tencent.devops.dockerhost.services.container

import com.github.dockerjava.api.command.InspectContainerResponse
import com.tencent.devops.dockerhost.common.DockerExitCodeEnum
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.dispatch.DockerHostBuildResourceApi
import com.tencent.devops.dockerhost.exception.ContainerException
import com.tencent.devops.dockerhost.services.Handler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ContainerAgentUpHandler(
    private val dockerHostConfig: DockerHostConfig,
    private val dockerHostBuildApi: DockerHostBuildResourceApi
) : Handler<ContainerHandlerContext>(dockerHostConfig, dockerHostBuildApi) {
    override fun handlerRequest(handlerContext: ContainerHandlerContext) {
        with(handlerContext) {
            var exitCode = 0L
            try {
                // 等待5s，看agent是否正常启动
                Thread.sleep(5000)
                val containerState = getContainerState(containerId!!)
                logger.info("containerState: $containerState")
                if (containerState != null) {
                    exitCode = containerState.exitCodeLong ?: 0L
                }
            } catch (e: Exception) {
                logger.error("$buildId|$vmSeqId waitAgentUp failed. containerId: $containerId", e)
            }

            if (exitCode != 0L && DockerExitCodeEnum.getValue(exitCode) != null) {
                val errorCodeEnum = DockerExitCodeEnum.getValue(exitCode)!!.errorCodeEnum
                logger.error("$buildId|$vmSeqId waitAgentUp failed. " +
                        "${errorCodeEnum.getErrorMessage()}. containerId: $containerId")
                throw ContainerException(
                    errorCodeEnum = errorCodeEnum,
                    message = "Failed to wait agent up. ${errorCodeEnum.getErrorMessage()}"
                )
            }
        }
    }

    fun getContainerState(containerId: String): InspectContainerResponse.ContainerState? {
        try {
            logger.info("Get containerState: $containerId start.")
            val inspectContainerResponse = httpDockerCli.inspectContainerCmd(containerId).exec() ?: return null
            logger.info("Get containerState: $containerId state: ${inspectContainerResponse.state}")
            return inspectContainerResponse.state
        } catch (e: Exception) {
            logger.error("check container: $containerId state failed, return ", e)
            return null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ContainerAgentUpHandler::class.java)
    }
}
