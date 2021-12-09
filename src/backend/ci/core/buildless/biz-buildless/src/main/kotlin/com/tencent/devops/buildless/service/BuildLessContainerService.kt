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
import com.tencent.devops.buildless.config.DockerHostConfig
import com.tencent.devops.buildless.pojo.BuildLessEndInfo
import com.tencent.devops.buildless.pojo.BuildLessStartInfo
import com.tencent.devops.buildless.pojo.BuildLessTask
import com.tencent.devops.buildless.rejected.RejectedExecutionFactory
import com.tencent.devops.buildless.utils.BK_DISTCC_LOCAL_IP
import com.tencent.devops.buildless.utils.BUILDLESS_POOL_PREFIX
import com.tencent.devops.buildless.utils.CORE_CONTAINER_POOL_SIZE
import com.tencent.devops.buildless.utils.CommonUtils
import com.tencent.devops.buildless.utils.ENTRY_POINT_CMD
import com.tencent.devops.buildless.utils.ENV_BK_CI_DOCKER_HOST_IP
import com.tencent.devops.buildless.utils.ENV_CONTAINER_NAME
import com.tencent.devops.buildless.utils.ENV_DOCKER_HOST_IP
import com.tencent.devops.buildless.utils.ENV_DOCKER_HOST_PORT
import com.tencent.devops.buildless.utils.ENV_JOB_BUILD_TYPE
import com.tencent.devops.buildless.utils.ENV_KEY_GATEWAY
import com.tencent.devops.buildless.utils.MAX_CONTAINER_POOL_SIZE
import com.tencent.devops.buildless.utils.RandomUtil
import com.tencent.devops.buildless.utils.RedisUtils
import com.tencent.devops.common.service.config.CommonConfig
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


/**
 * 无构建环境的docker服务实现
 */

@Service
class BuildLessContainerService(
    private val redisUtils: RedisUtils,
    private val commonConfig: CommonConfig,
    private val dockerHostConfig: DockerHostConfig,
    private val rejectedExecutionFactory: RejectedExecutionFactory
) {

    private val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerConfig(dockerHostConfig.dockerConfig)
        .withApiVersion(dockerHostConfig.apiVersion)
        .build()

    final var httpClient: DockerHttpClient = OkDockerHttpClient.Builder()
        .dockerHost(config.dockerHost)
        .sslConfig(config.sslConfig)
        .connectTimeout(5000)
        .readTimeout(120000)
        .build()

    val httpDockerCli: DockerClient = DockerClientBuilder
        .getInstance(config)
        .withDockerHttpClient(httpClient)
        .build()

    fun allocateContainer(buildLessStartInfo: BuildLessStartInfo) {
        val idlePoolSize = redisUtils.getIdlePoolSize()

        // 无空闲容器时执行拒绝策略
        logger.info("${buildLessStartInfo.buildId}|${buildLessStartInfo.vmSeqId} idlePoolSize: $idlePoolSize")
        if (idlePoolSize <= 0L) {
            val continueAllocate = rejectedExecutionFactory
                .getRejectedExecutionHandler(buildLessStartInfo.rejectedExecutionType)
                .rejectedExecution(buildLessStartInfo)

            if (!continueAllocate) {
                return
            }
        }

        // 已经进入需求池，所以减1
        redisUtils.increIdleContainer(-1)
        redisUtils.popIdleContainer()

        // 无空闲容器并且当前容器数小于最大容器数
        val runningPool = getRunningPoolCount()
        logger.info("${buildLessStartInfo.buildId}|${buildLessStartInfo.vmSeqId} runningPool: $runningPool")
        if (idlePoolSize <= 0L && (runningPool in CORE_CONTAINER_POOL_SIZE until MAX_CONTAINER_POOL_SIZE)) {
            createBuildLessPoolContainer(true)
        }

        redisUtils.leftPushBuildLessReadyTask(BuildLessTask(
            projectId = buildLessStartInfo.projectId,
            pipelineId = buildLessStartInfo.pipelineId,
            buildId = buildLessStartInfo.buildId,
            executionCount = buildLessStartInfo.executionCount,
            vmSeqId = buildLessStartInfo.vmSeqId,
            agentId = buildLessStartInfo.agentId,
            secretKey = buildLessStartInfo.secretKey
        ))
    }

    @Synchronized
    fun createBuildLessPoolContainer(oversold: Boolean = false) {
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

        // 创建之前校验当前缓存中的空闲容器数，超卖情况下校验规则不一致
        val runningContainerCount = getRunningPoolCount()
        if (!oversold && runningContainerCount >= CORE_CONTAINER_POOL_SIZE) return
        if (oversold && runningContainerCount >= MAX_CONTAINER_POOL_SIZE) return

        val container = httpDockerCli.createContainerCmd(imageName)
            .withName(containerName)
            .withLabels(mapOf(BUILDLESS_POOL_PREFIX to ""))
            .withCmd("/bin/sh", ENTRY_POINT_CMD)
            .withEnv(
                listOf(
                    "$ENV_KEY_GATEWAY=$gateway",
                    "TERM=xterm-256color",
                    "$ENV_DOCKER_HOST_IP=${CommonUtils.getHostIp()}",
                    "$ENV_DOCKER_HOST_PORT=${commonConfig.serverPort}",
                    "$BK_DISTCC_LOCAL_IP=${CommonUtils.getInnerIP()}",
                    "$ENV_BK_CI_DOCKER_HOST_IP=${CommonUtils.getInnerIP()}",
                    "$ENV_JOB_BUILD_TYPE=BUILD_LESS",
                    "$ENV_CONTAINER_NAME=$containerName"
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

        httpDockerCli.startContainerCmd(container.id).exec()

        // 超卖创建的容器不放入构建池
        redisUtils.setBuildLessPoolContainer(container.id)

        redisUtils.increIdleContainer(1)
        logger.info("===> created container $container")
    }

    fun stopContainer(buildLessEndInfo: BuildLessEndInfo) {
        with(buildLessEndInfo) {
            stopContainer(containerId, buildId)
        }

        // 容器销毁后接着拉起新的容器
        createBuildLessPoolContainer()
    }

    fun stopContainer(containerId: String, buildId: String) {
        if (containerId.isEmpty()) {
            logger.error("[$buildId]| Stop the container failed, containerId is null.")
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
    }

    /**
     * 检验容器池的大小
     */
    fun getRunningPoolCount(): Int {
        val containerInfo = httpDockerCli
            .listContainersCmd()
            .withStatusFilter(setOf("running"))
            .withLabelFilter(mapOf(BUILDLESS_POOL_PREFIX to ""))
            .exec()

        return containerInfo.size
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildLessContainerService::class.java)
    }
}
