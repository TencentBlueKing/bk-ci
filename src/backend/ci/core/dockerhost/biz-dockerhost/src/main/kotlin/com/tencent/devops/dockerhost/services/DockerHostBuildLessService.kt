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

package com.tencent.devops.dockerhost.services

import com.github.dockerjava.api.model.AccessMode
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Binds
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Volume
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.PullImageResultCallback
import com.github.dockerjava.okhttp.OkDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.dispatch.AlertApi
import com.tencent.devops.dockerhost.dispatch.BuildResourceApi
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
import com.tencent.devops.process.pojo.mq.PipelineBuildLessDockerStartupEvent
import org.slf4j.LoggerFactory

/**
 * 无构建环境的docker服务实现
 * @version 1.0
 */

class DockerHostBuildLessService(
    private val dockerHostConfig: DockerHostConfig,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val dockerHostWorkSpaceService: DockerHostWorkSpaceService,
    private val buildResourceApi: BuildResourceApi,
    private val dockerHostBuildResourceApi: DockerHostBuildResourceApi,
    private val alertApi: AlertApi
) {
    private val hostTag = CommonUtils.getInnerIP()

    private val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerConfig(dockerHostConfig.dockerConfig)
        .withApiVersion(dockerHostConfig.apiVersion)
        .withRegistryUrl(dockerHostConfig.registryUrl)
        .withRegistryUsername(dockerHostConfig.registryUsername)
        .withRegistryPassword(SecurityUtil.decrypt(dockerHostConfig.registryPassword!!))
        .build()!!

    var longHttpClient: DockerHttpClient = OkDockerHttpClient.Builder()
        .dockerHost(config.dockerHost)
        .sslConfig(config.sslConfig)
        .connectTimeout(5000)
        .readTimeout(300000)
        .build()

    private val dockerCli = DockerClientBuilder.getInstance(config).withDockerHttpClient(longHttpClient).build()

    fun retryDispatch(event: PipelineBuildLessDockerStartupEvent) {
        event.retryTime = event.retryTime - 1
        if (event.retryTime > 0) {
            pipelineEventDispatcher.dispatch(event)
        } else {
            val result = buildResourceApi.dockerStartFail(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                vmSeqId = event.vmSeqId,
                status = BuildStatus.FAILED
            )
            logger.error("[${event.buildId}]| can redispatch any more, set VM status result:$result")
            alertApi.alert(
                AlertLevel.HIGH.name, "Docker重新分配事件失败", "Docker重新分配事件失败, " +
                    "母机IP:${CommonUtils.getInnerIP()}， 镜像名称：${event.dockerImage}"
            )
        }
    }

    fun createContainer(event: PipelineBuildLessDockerStartupEvent): String {
        try {
            // docker pull
            try {
                LocalImageCache.saveOrUpdate(event.dockerImage)
                dockerCli.pullImageCmd(event.dockerImage).exec(PullImageResultCallback()).awaitCompletion()
            } catch (t: Throwable) {
                logger.warn("[${event.buildId}]|Fail to pull the image ${event.dockerImage} of build ${event.buildId}", t)
            }

            val hostWorkspace = getWorkspace(event.pipelineId, event.vmSeqId.trim())
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
            logger.info("[${event.buildId}]|gateway is: $gateway")
            val binds = Binds(
                Bind("${dockerHostConfig.hostPathMavenRepo}/${event.pipelineId}/", volumeMavenRepo),
                Bind("${dockerHostConfig.hostPathNpmPrefix}/${event.pipelineId}/", volumeNpmPrefix),
                Bind("${dockerHostConfig.hostPathNpmCache}/${event.pipelineId}/", volumeNpmCache),
                Bind("${dockerHostConfig.hostPathCcache}/${event.pipelineId}/", volumeCcache),
                Bind(dockerHostConfig.hostPathApps, volumeApps, AccessMode.ro),
                Bind(dockerHostConfig.hostPathInit, volumeInit, AccessMode.ro),
                Bind(dockerHostConfig.hostPathHosts, volumeHosts, AccessMode.ro),
                Bind("${dockerHostConfig.hostPathLogs}/${event.buildId}/${event.vmSeqId}/", volumeLogs),
                Bind("${dockerHostConfig.hostPathGradleCache}/${event.pipelineId}/", volumeGradleCache),
                Bind(linkPath, volumeTmpLink),
                Bind(hostWorkspace, volumeWs)
            )
            val container = dockerCli.createContainerCmd(event.dockerImage)
                .withCmd("/bin/sh", ENTRY_POINT_CMD)
                .withEnv(
                    listOf(
                        "$ENV_KEY_PROJECT_ID=${event.projectId}",
                        "$ENV_KEY_AGENT_ID=${event.agentId}",
                        "$ENV_KEY_AGENT_SECRET_KEY=${event.secretKey}",
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

            logger.info("[${event.buildId}]|Created container $container")
            dockerCli.startContainerCmd(container.id).exec()

            return container.id
        } catch (ignored: Throwable) {
            logger.error("[${event.buildId}]| create Container failed ", ignored)
            alertApi.alert(
                AlertLevel.HIGH.name, "Docker构建机创建容器失败", "Docker构建机创建容器失败, " +
                    "母机IP:${CommonUtils.getInnerIP()}， 失败信息：${ignored.message}"
            )
            throw ContainerException("[${event.buildId}]|Create container failed")
        }
    }

    fun endBuild(): DockerHostBuildInfo? {
        val result = dockerHostBuildResourceApi.endBuild(CommonUtils.getInnerIP())
        if (result != null) {
            if (result.isNotOk()) {
                return null
            }
        }
        return result!!.data!!
    }

    fun reportContainerId(buildId: String, vmSeqId: String, containerId: String): Boolean {
        val result = buildResourceApi.reportContainerId(buildId, vmSeqId, containerId, hostTag)
        if (result != null) {
            if (result.isNotOk()) {
                logger.info("reportContainerId return msg: ${result.message}")
                return false
            }
        }
        return result!!.data!!
    }

    fun stopContainer(buildId: String, containerId: String) {
        try {
            // docker stop
            val containerInfo = dockerCli.inspectContainerCmd(containerId).exec()
            if ("exited" != containerInfo.state.status) {
                dockerCli.stopContainerCmd(containerId).withTimeout(15).exec()
            }
        } catch (ignored: Throwable) {
            logger.error("[$buildId]| Stop the container failed, containerId: $containerId, error msg: $ignored", ignored)
        }

        try {
            // docker rm
            dockerCli.removeContainerCmd(containerId).exec()
        } catch (ignored: Throwable) {
            logger.error(
                "[$buildId]| Stop the container failed, containerId: $containerId, error msg: $ignored",
                ignored
            )
        }
    }

    fun getContainerNum(): Int {
        try {
            val dockerInfo = dockerCli.infoCmd().exec()
            return dockerInfo.containersRunning ?: 0
        } catch (ignored: Throwable) {
            logger.error("Get container num failed")
        }
        return 0
    }

    fun clearContainers() {
        val containerInfo = dockerCli.listContainersCmd().withStatusFilter(setOf("exited")).exec()
        for (container in containerInfo) {
            logger.info("Clear container, containerId: ${container.id}")
            dockerCli.removeContainerCmd(container.id).exec()
        }
    }

    private fun getWorkspace(pipelineId: String, vmSeqId: String): String {
        return "${dockerHostConfig.hostPathWorkspace}/$pipelineId/$vmSeqId/"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DockerHostBuildLessService::class.java)
    }
}
