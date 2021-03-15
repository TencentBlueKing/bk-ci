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

package com.tencent.devops.dockerhost.services

import com.github.dockerjava.api.exception.NotModifiedException
import com.github.dockerjava.api.model.AccessMode
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Binds
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Volume
import com.github.dockerjava.core.command.PullImageResultCallback
import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.dispatch.docker.pojo.DockerHostBuildInfo
import com.tencent.devops.dockerhost.common.ErrorCodeEnum
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.dispatch.AlertApi
import com.tencent.devops.dockerhost.dispatch.DockerEnv
import com.tencent.devops.dockerhost.dispatch.DockerHostBuildResourceApi
import com.tencent.devops.dockerhost.exception.ContainerException
import com.tencent.devops.dockerhost.utils.BK_DISTCC_LOCAL_IP
import com.tencent.devops.dockerhost.utils.CommonUtils
import com.tencent.devops.dockerhost.utils.ENTRY_POINT_CMD
import com.tencent.devops.dockerhost.utils.ENV_BK_CI_DOCKER_HOST_IP
import com.tencent.devops.dockerhost.utils.ENV_BK_CI_DOCKER_HOST_WORKSPACE
import com.tencent.devops.dockerhost.utils.ENV_DOCKER_HOST_IP
import com.tencent.devops.dockerhost.utils.ENV_KEY_AGENT_ID
import com.tencent.devops.dockerhost.utils.ENV_KEY_AGENT_SECRET_KEY
import com.tencent.devops.dockerhost.utils.ENV_KEY_GATEWAY
import com.tencent.devops.dockerhost.utils.ENV_KEY_PROJECT_ID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 无构建环境的docker服务实现
 * @version 1.0
 */

@Service
class DockerHostBuildAgentLessService(
    dockerHostBuildApi: DockerHostBuildResourceApi,
    private val dockerHostConfig: DockerHostConfig,
    private val dockerHostWorkSpaceService: DockerHostWorkSpaceService,
    private val alertApi: AlertApi
) : AbstractDockerHostBuildService(dockerHostConfig, dockerHostBuildApi) {

    override fun createContainer(dockerHostBuildInfo: DockerHostBuildInfo): String {
        try {
            // docker pull
            val imageName = CommonUtils.normalizeImageName(dockerHostBuildInfo.imageName)
            try {
                LocalImageCache.saveOrUpdate(imageName)
                httpDockerCli.pullImageCmd(imageName).exec(PullImageResultCallback()).awaitCompletion()
            } catch (t: Throwable) {
                logger.warn("[${dockerHostBuildInfo.buildId}]|Fail to pull the image $imageName of build ${dockerHostBuildInfo.buildId}", t)
            }

            val hostWorkspace = getWorkspace(dockerHostBuildInfo.pipelineId, dockerHostBuildInfo.vmSeqId.toString().trim())
            val linkPath = dockerHostWorkSpaceService.createSymbolicLink(hostWorkspace)

            // docker run
            val volumeWs = Volume(dockerHostConfig.volumeWorkspace)
            val volumeMavenRepo = Volume(dockerHostConfig.volumeMavenRepo)
            val volumeNpmPrefix = Volume(dockerHostConfig.volumeNpmPrefix)
            val volumeNpmCache = Volume(dockerHostConfig.volumeNpmCache)
            val volumeCcache = Volume(dockerHostConfig.volumeCcache)
            val volumeApps = Volume(dockerHostConfig.volumeApps)
            val volumeInit = Volume(dockerHostConfig.volumeInit)
            val volumeLogs = Volume(dockerHostConfig.volumeLogs)
            val volumeHosts = Volume(dockerHostConfig.hostPathHosts)
            val volumeGradleCache = Volume(dockerHostConfig.volumeGradleCache)
            val volumeTmpLink = Volume(linkPath)

            val gateway = DockerEnv.getGatway()
            logger.info("[${dockerHostBuildInfo.buildId}]|gateway is: $gateway")
            val binds = Binds(
                Bind("${dockerHostConfig.hostPathMavenRepo}/${dockerHostBuildInfo.pipelineId}/", volumeMavenRepo),
                Bind("${dockerHostConfig.hostPathNpmPrefix}/${dockerHostBuildInfo.pipelineId}/", volumeNpmPrefix),
                Bind("${dockerHostConfig.hostPathNpmCache}/${dockerHostBuildInfo.pipelineId}/", volumeNpmCache),
                Bind("${dockerHostConfig.hostPathCcache}/${dockerHostBuildInfo.pipelineId}/", volumeCcache),
                Bind(dockerHostConfig.hostPathApps, volumeApps, AccessMode.ro),
                Bind(dockerHostConfig.hostPathInit, volumeInit, AccessMode.ro),
                Bind(dockerHostConfig.hostPathHosts, volumeHosts, AccessMode.ro),
                Bind("${dockerHostConfig.hostPathLogs}/${dockerHostBuildInfo.buildId}/${dockerHostBuildInfo.vmSeqId}/", volumeLogs),
                Bind("${dockerHostConfig.hostPathGradleCache}/${dockerHostBuildInfo.pipelineId}/", volumeGradleCache),
                Bind(linkPath, volumeTmpLink),
                Bind(hostWorkspace, volumeWs)
            )
            val container = httpDockerCli.createContainerCmd(imageName)
                .withCmd("/bin/sh", ENTRY_POINT_CMD)
                .withEnv(
                    listOf(
                        "$ENV_KEY_PROJECT_ID=${dockerHostBuildInfo.projectId}",
                        "$ENV_KEY_AGENT_ID=${dockerHostBuildInfo.agentId}",
                        "$ENV_KEY_AGENT_SECRET_KEY=${dockerHostBuildInfo.secretKey}",
                        "$ENV_KEY_GATEWAY=$gateway",
                        "TERM=xterm-256color",
                        "$ENV_DOCKER_HOST_IP=${CommonUtils.getInnerIP()}",
                        "$BK_DISTCC_LOCAL_IP=${CommonUtils.getInnerIP()}",
                        "$ENV_BK_CI_DOCKER_HOST_IP=${CommonUtils.getInnerIP()}",
                        "$ENV_BK_CI_DOCKER_HOST_WORKSPACE=$linkPath"
                    )
                )
                .withHostConfig(
                    // CPU and memory Limit
                    HostConfig()
                        .withMemory(dockerHostConfig.memory)
                        .withMemorySwap(dockerHostConfig.memory)
                        .withCpuQuota(dockerHostConfig.cpuQuota.toLong())
                        .withCpuPeriod(dockerHostConfig.cpuPeriod.toLong())
                        .withBinds(binds)
                        .withNetworkMode("bridge")
                )
                .withVolumes(volumeWs).withVolumes(volumeApps).withVolumes(volumeInit)
                .exec()

            logger.info("[${dockerHostBuildInfo.buildId}]|Created container $container")
            httpDockerCli.startContainerCmd(container.id).exec()

            return container.id
        } catch (ignored: Throwable) {
            logger.error("[${dockerHostBuildInfo.buildId}]| create Container failed ", ignored)
            alertApi.alert(
                AlertLevel.HIGH.name, "Docker构建机创建容器失败", "Docker构建机创建容器失败, " +
                        "母机IP:${CommonUtils.getInnerIP()}， 失败信息：${ignored.message}"
            )
            throw ContainerException(
                errorCodeEnum = ErrorCodeEnum.CREATE_CONTAINER_ERROR,
                message = "[${dockerHostBuildInfo.buildId}]|Create container failed"
            )
        }
    }

    override fun stopContainer(dockerHostBuildInfo: DockerHostBuildInfo) {
        try {
            // docker stop
            val containerInfo = httpDockerCli.inspectContainerCmd(dockerHostBuildInfo.containerId).exec()
            if ("exited" != containerInfo.state.status) {
                httpDockerCli.stopContainerCmd(dockerHostBuildInfo.containerId).withTimeout(15).exec()
            }
        } catch (e: NotModifiedException) {
            logger.error("[${dockerHostBuildInfo.buildId}]| Stop the container failed, containerId: ${dockerHostBuildInfo.containerId} already stopped.")
        } catch (ignored: Throwable) {
            logger.error("[${dockerHostBuildInfo.buildId}]| Stop the container failed, containerId: ${dockerHostBuildInfo.containerId}, error msg: $ignored", ignored)
        }

        try {
            // docker rm
            httpDockerCli.removeContainerCmd(dockerHostBuildInfo.containerId).exec()
        } catch (ignored: Throwable) {
            logger.error(
                "[${dockerHostBuildInfo.buildId}]| Stop the container failed, containerId: ${dockerHostBuildInfo.containerId}, error msg: $ignored",
                ignored
            )
        }
    }

    private fun getWorkspace(pipelineId: String, vmSeqId: String): String {
        return "${dockerHostConfig.hostPathWorkspace}/$pipelineId/$vmSeqId/"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DockerHostBuildAgentLessService::class.java)
    }
}
