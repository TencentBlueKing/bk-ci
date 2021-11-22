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

package com.tencent.devops.buildless.service

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.NotModifiedException
import com.github.dockerjava.api.model.AccessMode
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Binds
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Volume
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.okhttp.OkDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import com.tencent.devops.buildless.common.ErrorCodeEnum
import com.tencent.devops.buildless.config.DockerHostConfig
import com.tencent.devops.buildless.exception.DockerServiceException
import com.tencent.devops.buildless.pojo.BuildLessEndInfo
import com.tencent.devops.buildless.pojo.BuildLessStartInfo
import com.tencent.devops.buildless.utils.BK_DISTCC_LOCAL_IP
import com.tencent.devops.buildless.utils.BUILDLESS_POOL_PREFIX
import com.tencent.devops.buildless.utils.CommonUtils
import com.tencent.devops.buildless.utils.ContainerStatus
import com.tencent.devops.buildless.utils.ENTRY_POINT_CMD
import com.tencent.devops.buildless.utils.ENV_BK_CI_DOCKER_HOST_IP
import com.tencent.devops.buildless.utils.ENV_DOCKER_HOST_IP
import com.tencent.devops.buildless.utils.ENV_JOB_BUILD_TYPE
import com.tencent.devops.buildless.utils.ENV_KEY_AGENT_ID
import com.tencent.devops.buildless.utils.ENV_KEY_AGENT_SECRET_KEY
import com.tencent.devops.buildless.utils.ENV_KEY_GATEWAY
import com.tencent.devops.buildless.utils.ENV_KEY_PROJECT_ID
import com.tencent.devops.buildless.utils.RandomUtil
import com.tencent.devops.buildless.utils.RedisUtils
import com.tencent.devops.buildless.utils.SLEEP_ENTRY_POINT_CMD
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


/**
 * 无构建环境的docker服务实现
 */

@Service
class BuildlessService(
    private val dockerHostConfig: DockerHostConfig,
    private val redisUtils: RedisUtils
) {

    private val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerConfig(dockerHostConfig.dockerConfig)
        .withApiVersion(dockerHostConfig.apiVersion)
        .build()

    var httpClient: DockerHttpClient = OkDockerHttpClient.Builder()
        .dockerHost(config.dockerHost)
        .sslConfig(config.sslConfig)
        .connectTimeout(5000)
        .readTimeout(30000)
        .build()

    var longHttpClient: DockerHttpClient = OkDockerHttpClient.Builder()
        .dockerHost(config.dockerHost)
        .sslConfig(config.sslConfig)
        .connectTimeout(5000)
        .readTimeout(300000)
        .build()

    val httpDockerCli: DockerClient = DockerClientBuilder
        .getInstance(config)
        .withDockerHttpClient(httpClient)
        .build()

    val httpLongDockerCli: DockerClient = DockerClientBuilder
        .getInstance(config)
        .withDockerHttpClient(longHttpClient)
        .build()

    /**
     * 检验容器池的大小
     */
    fun getPoolCoreSize(): Int {
        val containerInfo = httpLongDockerCli
            .listContainersCmd()
            .withStatusFilter(setOf("running"))
            .withLabelFilter(mapOf(BUILDLESS_POOL_PREFIX to ""))
            .exec()

        return containerInfo.size
    }

    fun createContainer(dockerHostBuildInfo: BuildLessStartInfo): String {
        try {
            val containerId = redisUtils.getIdleContainer()

            if (containerId.isNullOrBlank()) {
                throw DockerServiceException(
                    errorType = ErrorCodeEnum.NO_IDLE_CONTAINER_ERROR.errorType,
                    errorCode = ErrorCodeEnum.NO_IDLE_CONTAINER_ERROR.errorCode,
                    errorMsg = "[${dockerHostBuildInfo.buildId}]|Create container failed"
                )
            }

            val response = httpDockerCli.execCreateCmd(containerId)
                .withEnv(listOf(
                    "$ENV_KEY_PROJECT_ID=${dockerHostBuildInfo.projectId}",
                    "$ENV_KEY_AGENT_ID=${dockerHostBuildInfo.agentId}",
                    "$ENV_KEY_AGENT_SECRET_KEY=${dockerHostBuildInfo.secretKey}"
                ))
                .withCmd("/bin/sh", ENTRY_POINT_CMD)
                .exec()

            httpDockerCli.execStartCmd(response.id).start()
            logger.info("Success start container: $containerId")
            redisUtils.setBuildlessPoolContainer(containerId, ContainerStatus.BUSY)
            return containerId
        } catch (ignored: Throwable) {
            logger.error("[${dockerHostBuildInfo.buildId}]| create Container failed ", ignored)
            throw DockerServiceException(
                errorType = ErrorCodeEnum.CREATE_CONTAINER_ERROR.errorType,
                errorCode = ErrorCodeEnum.CREATE_CONTAINER_ERROR.errorCode,
                errorMsg = "[${dockerHostBuildInfo.buildId}]|Create container failed"
            )
        }
    }

    fun createBuildlessPoolContainer(): String {
        val imageName = "mirrors.tencent.com/ci/tlinux_ci:0.5.0.4"

        val volumeApps = Volume(dockerHostConfig.volumeApps)
        val volumeInit = Volume(dockerHostConfig.volumeInit)
        val volumeSleep = Volume(dockerHostConfig.volumeSleep)
        val volumeLogs = Volume(dockerHostConfig.volumeLogs)

        val gateway = dockerHostConfig.gateway
        val containerName = "$BUILDLESS_POOL_PREFIX-${RandomUtil.randomString()}"
        val binds = Binds(
            Bind(dockerHostConfig.hostPathApps, volumeApps, AccessMode.ro),
            Bind(dockerHostConfig.hostPathInit, volumeInit, AccessMode.ro),
            Bind(dockerHostConfig.hostPathSleep, volumeSleep, AccessMode.ro),
            Bind(dockerHostConfig.hostPathLogs + "/$containerName", volumeLogs)
        )

        val container = httpLongDockerCli.createContainerCmd(imageName)
            .withName(containerName)
            .withLabels(mapOf(BUILDLESS_POOL_PREFIX to ""))
            .withCmd("/bin/sh", SLEEP_ENTRY_POINT_CMD)
            .withEnv(
                listOf(
                    "$ENV_KEY_GATEWAY=$gateway",
                    "TERM=xterm-256color",
                    "$ENV_DOCKER_HOST_IP=${CommonUtils.getInnerIP()}",
                    "$BK_DISTCC_LOCAL_IP=${CommonUtils.getInnerIP()}",
                    "$ENV_BK_CI_DOCKER_HOST_IP=${CommonUtils.getInnerIP()}",
                    "$ENV_JOB_BUILD_TYPE=AGENT_LESS"
                )
            )
            .withHostConfig(
                // CPU and memory Limit
                HostConfig()
                    .withMemory(dockerHostConfig.memory)
                    .withCpuQuota(dockerHostConfig.cpuQuota.toLong())
                    .withCpuPeriod(dockerHostConfig.cpuPeriod.toLong())
                    .withBinds(binds)
                    .withNetworkMode("bridge")
            )
            .exec()

        logger.info("Created container $container")
        httpLongDockerCli.startContainerCmd(container.id).exec()

        return container.id
    }

    fun stopContainer(buildLessEndInfo: BuildLessEndInfo) {
        stopContainer(buildLessEndInfo.containerId, buildLessEndInfo.buildId)
    }

    fun stopContainer(containerId: String, buildId: String) {
        if (containerId.isEmpty()) {
            logger.error("[$buildId]| Stop the container failed, contianerId is null.")
            return
        }

        try {
            // docker stop
            val containerInfo = httpDockerCli.inspectContainerCmd(containerId).exec()
            if ("exited" != containerInfo.state.status) {
                httpDockerCli.stopContainerCmd(containerId).withTimeout(15).exec()
            }
        } catch (e: NotModifiedException) {
            logger.error("[$buildId]| Stop the container failed, containerId: $containerId already stopped.")
        } catch (ignored: Throwable) {
            logger.error(
                "[$buildId]| Stop the container failed, containerId: $containerId, " +
                        "error msg: $ignored", ignored
            )
        }

        try {
            // docker rm
            httpDockerCli.removeContainerCmd(containerId).exec()
        } catch (ignored: Throwable) {
            logger.error(
                "[$buildId]| Stop the container failed, containerId: $containerId, error msg: $ignored",
                ignored
            )
        }

        redisUtils.deleteBuildlessPoolContainer(containerId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildlessService::class.java)
    }
}
