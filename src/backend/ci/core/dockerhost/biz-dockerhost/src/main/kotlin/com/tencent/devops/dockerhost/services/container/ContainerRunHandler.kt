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

import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.Capability
import com.github.dockerjava.api.model.HostConfig
import com.tencent.devops.dockerhost.common.ErrorCodeEnum
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.dispatch.DockerHostBuildResourceApi
import com.tencent.devops.dockerhost.exception.ContainerException
import com.tencent.devops.dockerhost.services.Handler
import com.tencent.devops.dockerhost.services.generator.DockerBindLoader
import com.tencent.devops.dockerhost.services.generator.DockerEnvLoader
import com.tencent.devops.dockerhost.services.generator.DockerMountLoader
import com.tencent.devops.dockerhost.utils.ENTRY_POINT_CMD
import com.tencent.devops.dockerhost.utils.RandomUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ContainerRunHandler(
    private val dockerHostConfig: DockerHostConfig,
    private val dockerHostBuildApi: DockerHostBuildResourceApi
) : Handler<ContainerHandlerContext>(dockerHostConfig, dockerHostBuildApi) {
    override fun handlerRequest(handlerContext: ContainerHandlerContext) {
        with(handlerContext) {
            try {
                val containerName =
                    "dispatch-$buildId-$vmSeqId-${RandomUtil.randomString()}"
                val hostConfig = HostConfig()
                    .withCapAdd(Capability.SYS_PTRACE)
                    .withBinds(DockerBindLoader.loadBinds(this))
                    .withMounts(DockerMountLoader.loadMounts(this))
                    .withNetworkMode("bridge")

                // 对开启代码加速白名单项目增加--init启动参数，用于容器销毁时回收进程
                if (qpcUniquePath != null && qpcUniquePath.isNotBlank()) {
                    hostConfig.withInit(true)
                }

                if (dockerResource != null) {
                    hostConfig
                        .withMemory(dockerResource.memoryLimitBytes)
                        // .withMemorySwap(dockerResource.memoryLimitBytes)
                        .withCpuQuota(dockerResource.cpuQuota.toLong())
                        .withCpuPeriod(dockerResource.cpuPeriod.toLong())
                }

                val container = httpLongDockerCli.createContainerCmd(formatImageName!!)
                    .withName(containerName)
                    .withCmd("/bin/sh", ENTRY_POINT_CMD)
                    .withEnv(DockerEnvLoader.loadEnv(this))
                    .withHostConfig(hostConfig)
                    .exec()

                logger.info("Created container $container")
                httpLongDockerCli.startContainerCmd(container.id).exec()

                containerId = container.id
            } catch (er: Throwable) {
                logger.error(er.toString())
                logger.error(er.message)
                log(
                    buildId = buildId,
                    red = true,
                    message = "Failed to start build environment: ${er.message}",
                    tag = taskId(),
                    containerHashId = containerHashId
                )
                if (er is NotFoundException) {
                    throw ContainerException(
                        errorCodeEnum = ErrorCodeEnum.IMAGE_NOT_EXIST_ERROR,
                        message = "Image does not exist."
                    )
                } else {
                    throw ContainerException(
                        errorCodeEnum = ErrorCodeEnum.CREATE_CONTAINER_ERROR,
                        message = "$buildId|$vmSeqId Create container failed"
                    )
                }
            }

            nextHandler.get()?.handlerRequest(this)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ContainerRunHandler::class.java)
    }
}
