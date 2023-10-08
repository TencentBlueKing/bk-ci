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
import com.tencent.devops.buildless.config.BuildLessConfig
import com.tencent.devops.buildless.utils.BK_DISTCC_LOCAL_IP
import com.tencent.devops.buildless.utils.BUILDLESS_POOL_PREFIX
import com.tencent.devops.buildless.utils.CommonUtils
import com.tencent.devops.buildless.utils.ContainerStatus
import com.tencent.devops.buildless.utils.ENTRY_POINT_CMD
import com.tencent.devops.buildless.utils.ENV_BK_CI_DOCKER_HOST_IP
import com.tencent.devops.buildless.utils.ENV_BK_CI_DOCKER_HOST_WORKSPACE
import com.tencent.devops.buildless.utils.ENV_CONTAINER_NAME
import com.tencent.devops.buildless.utils.ENV_DEFAULT_LOCALE_LANGUAGE
import com.tencent.devops.buildless.utils.ENV_DEVOPS_FILE_GATEWAY
import com.tencent.devops.buildless.utils.ENV_DEVOPS_GATEWAY
import com.tencent.devops.buildless.utils.ENV_DOCKER_HOST_IP
import com.tencent.devops.buildless.utils.ENV_DOCKER_HOST_PORT
import com.tencent.devops.buildless.utils.ENV_JOB_BUILD_TYPE
import com.tencent.devops.buildless.utils.ENV_KEY_BK_TAG
import com.tencent.devops.buildless.utils.ENV_KEY_GATEWAY
import com.tencent.devops.buildless.utils.RandomUtil
import com.tencent.devops.buildless.utils.RedisUtils
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.config.CommonConfig
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.TimeZone
import kotlin.streams.toList

/**
 * 无构建环境的docker服务实现
 */

@Service
class BuildLessContainerService(
    private val bkTag: BkTag,
    private val redisUtils: RedisUtils,
    private val commonConfig: CommonConfig,
    private val buildLessConfig: BuildLessConfig
) {
    private val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerConfig(buildLessConfig.dockerConfig)
        .withApiVersion(buildLessConfig.apiVersion)
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

    fun createContainer() {
        val volumeApps = Volume(buildLessConfig.volumeApps)
        val volumeInit = Volume(buildLessConfig.volumeInit)
        val volumeSleep = Volume(buildLessConfig.volumeSleep)
        val volumeLogs = Volume(buildLessConfig.volumeLogs)
        val volumeWs = Volume(buildLessConfig.volumeWorkspace)

        val containerName = "$BUILDLESS_POOL_PREFIX-${RandomUtil.randomString()}"

        val hostWorkspace = buildLessConfig.hostPathWorkspace + "/$containerName"

        val linkPath = createSymbolicLink(hostWorkspace)

        val binds = Binds(
            Bind(buildLessConfig.hostPathApps, volumeApps, AccessMode.ro),
            Bind(buildLessConfig.hostPathInit, volumeInit, AccessMode.ro),
            Bind(buildLessConfig.hostPathSleep, volumeSleep, AccessMode.ro),
            Bind(buildLessConfig.hostPathLogs + "/$containerName", volumeLogs),
            Bind(hostWorkspace, volumeWs),
            Bind(linkPath, Volume(linkPath))
        )

        try {
            val container = httpDockerCli.createContainerCmd(buildLessConfig.containerPoolBaseImage)
                .withName(containerName)
                .withLabels(mapOf(BUILDLESS_POOL_PREFIX to ""))
                .withCmd("/bin/sh", ENTRY_POINT_CMD)
                .withEnv(generateEnv(containerName, linkPath))
                .withHostConfig(
                    // CPU and memory Limit
                    HostConfig()
                        .withMemory(buildLessConfig.memory)
                        .withCpuQuota(buildLessConfig.cpuQuota.toLong())
                        .withCpuPeriod(buildLessConfig.cpuPeriod.toLong())
                        .withBinds(binds)
                        .withNetworkMode("bridge")
                )
                .exec()

            httpDockerCli.startContainerCmd(container.id).exec()

            logger.info("===> created container: $container, containerName: $containerName. ")
            redisUtils.setBuildLessPoolContainer(container.id, ContainerStatus.IDLE)
            redisUtils.increIdlePool(1)
            logger.info("===> buildLessPoolKey hset ${container.id} ${ContainerStatus.IDLE.name}.")
        } catch (e: Exception) {
            logger.error("===> failed to created container.", e)
        }
    }

    fun stopContainer(
        buildId: String,
        vmSeqId: String,
        containerId: String
    ) {
        if (containerId.isEmpty()) {
            logger.error("$buildId|$vmSeqId Stop the container failed, containerId is null.")
            return
        }

        try {
            // docker stop
            val containerInfo = httpDockerCli.inspectContainerCmd(containerId).exec()
            if ("exited" != containerInfo.state.status) {
                httpDockerCli.stopContainerCmd(containerId).withTimeout(15).exec()
            }
        } catch (e: NotModifiedException) {
            logger.warn("$buildId|$vmSeqId Stop the container failed, containerId: $containerId already stopped.")
        } catch (ignored: Throwable) {
            logger.warn(
                "$buildId|$vmSeqId Stop the container failed, containerId: $containerId, " +
                        "error msg: $ignored", ignored
            )
        }

        try {
            // docker rm
            httpDockerCli.removeContainerCmd(containerId).exec()
        } catch (ignored: Throwable) {
            logger.warn(
                "$buildId|$vmSeqId Stop the container failed, containerId: $containerId, error msg: $ignored",
                ignored
            )
        } finally {
            redisUtils.deleteBuildLessPoolContainer(containerId)
        }
    }

    /**
     * 检验容器池的大小
     */
    fun getRunningPoolSize(needCalibrationContainerPool: Boolean = false): Int {
        val containerInfo = httpDockerCli
            .listContainersCmd()
            .withStatusFilter(setOf("running"))
            .withLabelFilter(mapOf(BUILDLESS_POOL_PREFIX to ""))
            .exec()

        if (needCalibrationContainerPool) {
            // 同步缓存中的容器状态
            val containerIds = containerInfo.stream().map {
                if (it.id.length > 12) {
                    it.id.substring(0, 12)
                } else {
                    it.id
                }
            }.toList()

            // 不在containerIds列表内的同步删除缓存
            val buildLessPoolContainerMap = redisUtils.getBuildLessPoolContainerList()
            buildLessPoolContainerMap.forEach { (key, _) ->
                if (!containerIds.contains(key)) {
                    redisUtils.deleteBuildLessPoolContainer(key)
                }
            }

            // 在containerIds但是不在缓存中的，补充缓存
            containerIds.forEach {
                if (!buildLessPoolContainerMap.keys.contains(it)) {
                    logger.info("Supplemental cache buildLessPoolKey hset $it ${ContainerStatus.IDLE.name}.")
                    redisUtils.setBuildLessPoolContainer(it, ContainerStatus.IDLE)
                }
            }
        }

        return containerInfo.size
    }

    fun getDockerRunTimeoutContainers(): MutableList<String> {
        val containerInfo = httpDockerCli.listContainersCmd().withStatusFilter(setOf("running")).exec()
        val timeoutContainerList = mutableListOf<String>()
        for (container in containerInfo) {
            val startTime = httpDockerCli.inspectContainerCmd(container.id).exec().state.startedAt
            // 是否已运行超过12小时
            val buildLessPoolInfo = redisUtils.getBuildLessPoolContainer(container.id)
            if (checkStartTime(startTime) &&
                (buildLessPoolInfo == null || buildLessPoolInfo.status == ContainerStatus.IDLE)) {
                timeoutContainerList.add(container.id)
            }
        }

        return timeoutContainerList
    }

    private fun generateEnv(containerName: String, linkPath: String): List<String> {
        val envList = mutableListOf<String>()
        envList.addAll(listOf(
            "$ENV_KEY_GATEWAY=${buildLessConfig.gateway}",
            "TERM=xterm-256color",
            "$ENV_KEY_BK_TAG=${bkTag.getFinalTag()}",
            "$ENV_DOCKER_HOST_IP=${CommonUtils.getHostIp()}",
            "$ENV_DOCKER_HOST_PORT=${commonConfig.serverPort}",
            "$BK_DISTCC_LOCAL_IP=${CommonUtils.getHostIp()}",
            "$ENV_BK_CI_DOCKER_HOST_IP=${CommonUtils.getHostIp()}",
            "$ENV_JOB_BUILD_TYPE=BUILD_LESS",
            "$ENV_CONTAINER_NAME=$containerName",
            "$ENV_BK_CI_DOCKER_HOST_WORKSPACE=$linkPath",
            "$ENV_DEFAULT_LOCALE_LANGUAGE=${commonConfig.devopsDefaultLocaleLanguage}"
        ))

        buildLessConfig.idcGateway?.let {
            envList.add("$ENV_DEVOPS_GATEWAY=$it")
        }
        buildLessConfig.fileIdcGateway?.let {
            envList.add("$ENV_DEVOPS_FILE_GATEWAY=$it")
        }

        return envList
    }

    private fun createSymbolicLink(hostWorkspace: String): String {
        val hostWorkspaceFile = File(hostWorkspace)
        if (!hostWorkspaceFile.exists()) {
            hostWorkspaceFile.mkdirs() // 新建的流水线的工作空间路径为空则新建目录
        }
        val shaContent = ShaUtils.sha1(hostWorkspace.toByteArray())
        val linkFilePathDir = buildLessConfig.hostPathLinkDir
        val linkFileDir = File(linkFilePathDir)
        if (!linkFileDir.exists()) {
            linkFileDir.mkdirs()
        }
        val linkPath = "$linkFilePathDir/$shaContent"
        logger.info("hostWorkspace:$hostWorkspace linkPath is: $linkPath")
        val link = FileSystems.getDefault().getPath(linkPath)
        if (!link.toFile().exists()) {
            val target = FileSystems.getDefault().getPath(hostWorkspace)
            Files.createSymbolicLink(link, target) // 为真实工作空间地址创建软链
        }
        return linkPath
    }

    private fun checkStartTime(utcTime: String?): Boolean {
        if (utcTime != null && utcTime.isNotEmpty()) {
            val array = utcTime.split(".")
            val utcTimeLocal = array[0] + "Z"
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
            sdf.timeZone = TimeZone.getTimeZone("UTC")

            val date = sdf.parse(utcTimeLocal)
            val startTimestamp = date.time
            val nowTimestamp = System.currentTimeMillis()
            return (nowTimestamp - startTimestamp) > (12 * 3600 * 1000)
        }

        return false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildLessContainerService::class.java)
    }
}
