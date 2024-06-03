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
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Ports
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.dockerhost.common.ErrorCodeEnum
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.dispatch.DockerHostBuildResourceApi
import com.tencent.devops.dockerhost.exception.ContainerException
import com.tencent.devops.dockerhost.pojo.DockerRunParam
import com.tencent.devops.dockerhost.pojo.DockerRunPortBinding
import com.tencent.devops.dockerhost.pojo.DockerRunResponse
import com.tencent.devops.dockerhost.services.Handler
import com.tencent.devops.dockerhost.services.generator.DockerBindLoader
import com.tencent.devops.dockerhost.services.generator.DockerEnvLoader
import com.tencent.devops.dockerhost.services.generator.DockerMountLoader
import com.tencent.devops.dockerhost.utils.CommonUtils
import com.tencent.devops.dockerhost.utils.RandomUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ContainerCustomizedRunHandler(
    private val dockerHostConfig: DockerHostConfig,
    private val dockerHostBuildApi: DockerHostBuildResourceApi
) : Handler<ContainerHandlerContext>(dockerHostConfig, dockerHostBuildApi) {
    override fun handlerRequest(handlerContext: ContainerHandlerContext) {
        with(handlerContext) {
            try {
                val env = generateEnv(dockerRunParam, this)
                logger.info("[$buildId]|[$vmSeqId] env is $env")

                val (portBindings, dockerRunPortBindingList) = generatePortBings(dockerRunParam)

                val dockerResource = dockerHostBuildApi.getResourceConfig(pipelineId, vmSeqId.toString())?.data

                val hostConfig = HostConfig()
                    .withCapAdd(Capability.SYS_PTRACE)
                    .withBinds(DockerBindLoader.loadBinds(this))
                    .withNetworkMode("bridge")
                    .withPortBindings(portBindings)
                    .withMounts(DockerMountLoader.loadMounts(this))

                if (qpcUniquePath != null && qpcUniquePath.isNotBlank()) {
                    hostConfig.withInit(true)
                }

                if (dockerResource != null) {
                    logger.info("[$buildId]|[$vmSeqId] dockerRun dockerResource: ${JsonUtil.toJson(dockerResource)}")
                    hostConfig
                        .withMemory(dockerResource.memoryLimitBytes)
                        // .withMemorySwap(dockerResource.memoryLimitBytes)
                        .withCpuQuota(dockerResource.cpuQuota.toLong())
                        .withCpuPeriod(dockerResource.cpuPeriod.toLong())
                }

                val createContainerCmd = httpLongDockerCli.createContainerCmd(formatImageName!!)
                    .withName("dockerRun-$buildId-$vmSeqId-${RandomUtil.randomString()}")
                    .withEnv(env)
                    .withHostConfig(hostConfig)
                    .withWorkingDir(dockerHostConfig.volumeWorkspace)

                if (!(dockerRunParam!!.command.isEmpty() || dockerRunParam.command.equals("[]"))) {
                    createContainerCmd.withCmd(dockerRunParam.command)
                }

                val container = createContainerCmd.exec()

                logger.info("[$buildId]|[$vmSeqId] Created container $container")
                httpLongDockerCli.startContainerCmd(container.id).exec()

                dockerRunResponse = DockerRunResponse(
                    containerId = container.id,
                    startTimeStamp = (System.currentTimeMillis() / 1000).toInt(),
                    dockerRunPortBindings = dockerRunPortBindingList
                )
            } catch (er: Throwable) {
                logger.error("[$buildId]|[$vmSeqId] customized run docker error.", er)
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
                        message = "Create container failed: $er"
                    )
                }
            } finally {
                doFinally(buildId, vmSeqId, registryUser, formatImageName!!)
            }

            nextHandler.get()?.handlerRequest(this)
        }
    }

    private fun generateEnv(
        dockerRunParam: DockerRunParam?,
        handlerContext: ContainerHandlerContext
    ): MutableList<String> {
        val env = mutableListOf<String>()
        env.addAll(DockerEnvLoader.loadEnv(handlerContext))
        env.add("bk_devops_start_source=dockerRun") // dockerRun启动标识
        dockerRunParam!!.env?.forEach {
            env.add("${it.key}=${it.value ?: ""}")
        }

        return env
    }

    private fun generatePortBings(
        dockerRunParam: DockerRunParam?
    ): Pair<Ports, List<DockerRunPortBinding>> {
        val dockerRunPortBindingList = mutableListOf<DockerRunPortBinding>()
        val hostIp = CommonUtils.getInnerIP()
        val portBindings = Ports()
        dockerRunParam!!.portList?.forEach {
            val localPort = getAvailableHostPort()
            if (localPort == 0) {
                throw ContainerException(
                    errorCodeEnum = ErrorCodeEnum.NO_AVAILABLE_PORT_ERROR,
                    message = "No enough port to use in dockerRun. " +
                        "startPort: ${dockerHostConfig.dockerRunStartPort}"
                )
            }
            val tcpContainerPort: ExposedPort = ExposedPort.tcp(it)
            portBindings.bind(tcpContainerPort, Ports.Binding.bindPort(localPort))
            dockerRunPortBindingList.add(DockerRunPortBinding(hostIp, it, localPort))
        }

        return Pair(portBindings, dockerRunPortBindingList)
    }

    private fun doFinally(
        buildId: String,
        vmSeqId: Int,
        registryUser: String?,
        imageName: String
    ) {
        if (!registryUser.isNullOrEmpty()) {
            try {
                httpLongDockerCli.removeImageCmd(imageName)
                logger.info("[$buildId]|[$vmSeqId] Delete local image successfully......")
            } catch (e: java.lang.Exception) {
                logger.info("[$buildId]|[$vmSeqId] the exception of deleteing local image is ${e.message}")
            } finally {
                logger.info("[$buildId]|[$vmSeqId] Docker run end......")
            }
        }
    }

    private fun getAvailableHostPort(): Int {
        val startPort = dockerHostConfig.dockerRunStartPort ?: 20000
        for (i in startPort..(startPort + 1000)) {
            if (!CommonUtils.isPortUsing("127.0.0.1", i)) {
                return i
            } else {
                continue
            }
        }

        return 0
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ContainerCustomizedRunHandler::class.java)
    }
}
