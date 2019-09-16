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
import com.github.dockerjava.api.model.AuthConfig
import com.github.dockerjava.api.model.AuthConfigurations
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Volume
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.BuildImageResultCallback
import com.github.dockerjava.core.command.PullImageResultCallback
import com.github.dockerjava.core.command.PushImageResultCallback
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.dispatch.DockerHostBuildResourceApi
import com.tencent.devops.dockerhost.exception.ContainerException
import com.tencent.devops.dockerhost.pojo.DockerBuildParam
import com.tencent.devops.dockerhost.utils.BK_DISTCC_LOCAL_IP
import com.tencent.devops.dockerhost.utils.CommonUtils
import com.tencent.devops.dockerhost.utils.ENTRY_POINT_CMD
import com.tencent.devops.dockerhost.utils.ENV_DOCKER_HOST_IP
import com.tencent.devops.dockerhost.utils.ENV_DOCKER_HOST_PORT
import com.tencent.devops.dockerhost.utils.ENV_KEY_AGENT_ID
import com.tencent.devops.dockerhost.utils.ENV_KEY_AGENT_SECRET_KEY
import com.tencent.devops.dockerhost.utils.ENV_KEY_GATEWAY
import com.tencent.devops.dockerhost.utils.ENV_KEY_PROJECT_ID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Paths

@Component
class DockerHostBuildService(
    private val dockerHostConfig: DockerHostConfig,
    private val commonConfig: CommonConfig
) {

    private val etcHosts = "/etc/hosts"
    private val defaultGateway: String = commonConfig.devopsBuildGateway!!

    private val logger = LoggerFactory.getLogger(DockerHostBuildService::class.java)

    private val dockerHostBuildApi: DockerHostBuildResourceApi = DockerHostBuildResourceApi()

    private val maxRunningContainerNum = 200

    private val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerHost(dockerHostConfig.dockerHost)
        .withDockerConfig(dockerHostConfig.dockerConfig)
        .withApiVersion(dockerHostConfig.apiVersion)
        .withRegistryUrl(dockerHostConfig.registryUrl)
        .withRegistryUsername(dockerHostConfig.registryUsername)
        .withRegistryPassword(
            try {
                dockerHostConfig.registryPassword!!
            } catch (ignored: Throwable) {
                dockerHostConfig.registryPassword!!
            }
        )
        .build()

    private val dockerCli = DockerClientBuilder.getInstance(config).build()

    fun startBuild(): DockerHostBuildInfo? {
        val result = dockerHostBuildApi.startBuild(CommonUtils.getInnerIP())
        if (result != null) {
            if (result.isNotOk()) {
                return null
            }
        }
        return result!!.data!!
    }

    fun startBuildByDispatch(dockerHostBuildInfo: DockerHostBuildInfo): Boolean {
        try {
            val containerNum = getContainerNum()
            if (containerNum >= maxRunningContainerNum) {
                logger.warn("[${dockerHostBuildInfo.buildId}]|Too many containers in this host, break to start build.")
                return false
            }

            if (dockerHostBuildInfo.status == PipelineTaskStatus.RUNNING.status) {
                logger.info("[${dockerHostBuildInfo.buildId}]|Create container, dockerStartBuildInfo: $dockerHostBuildInfo")

                try {
                    val containerId = createContainer(dockerHostBuildInfo)
                    // 上报containerId给dispatch
                    reportContainerId(dockerHostBuildInfo.buildId, dockerHostBuildInfo.vmSeqId, containerId)

                    if (isContainerRunning(containerId)) {
                        log(dockerHostBuildInfo.buildId, "构建环境启动成功，等待Agent启动...")
                    } else {
                        return false
                    }
                } catch (e: ContainerException) {
                    logger.error("[${dockerHostBuildInfo.buildId}]|Create container failed, rollback build. buildId: ${dockerHostBuildInfo.buildId}, vmSeqId: ${dockerHostBuildInfo.vmSeqId}")
                    return false
                }
                return true
            }
            return false
        } catch (t: Throwable) {
            logger.error("[${dockerHostBuildInfo.buildId}]|StartBuild encounter unknown exception", t)
            return false
        }
    }

    fun endBuildByDispatch(dockerHostBuildInfo: DockerHostBuildInfo): Boolean {
        return try {
            logger.info("[${dockerHostBuildInfo.buildId}]|dockerEndBuidlInfo: ${dockerHostBuildInfo.status}")
            if (dockerHostBuildInfo.status == PipelineTaskStatus.DONE.status || dockerHostBuildInfo.status == PipelineTaskStatus.FAILURE.status) {
                logger.info("[${dockerHostBuildInfo.buildId}]|Stop the container, containerId: ${dockerHostBuildInfo.containerId}")
                stopContainer(dockerHostBuildInfo)
            }
            true
        } catch (t: Throwable) {
            logger.error("[${dockerHostBuildInfo.buildId}]|EndBuild encounter unknown exception", t)
            false
        }
    }

    fun endBuild(): DockerHostBuildInfo? {
        val result = dockerHostBuildApi.endBuild(CommonUtils.getInnerIP())
        if (result != null) {
            if (result.isNotOk()) {
                return null
            }
        }
        return result!!.data!!
    }

    fun reportContainerId(buildId: String, vmSeqId: Int, containerId: String): Boolean {
        val result = dockerHostBuildApi.reportContainerId(buildId, vmSeqId, containerId)
        if (result != null) {
            if (result.isNotOk()) {
                logger.info("[$buildId]|reportContainerId return msg: ${result.message}")
                return false
            }
        }
        return result!!.data!!
    }

    fun rollbackBuild(buildId: String, vmSeqId: Int, shutdown: Boolean): Boolean {
        log(buildId, true, if (shutdown) "构建环境启动后即退出，请检查镜像是否合法或联系【蓝盾助手】查看，构建任务将失败退出" else "启动构建环境失败，构建任务将重试")

        val result = dockerHostBuildApi.rollbackBuild(buildId, vmSeqId, shutdown)
        if (result != null) {
            if (result.isNotOk()) {
                logger.info("[$buildId]|rollbackBuild return msg: ${result.message}")
                return false
            }
        }
        return result!!.data!!
    }

    fun createContainer(dockerBuildInfo: DockerHostBuildInfo): String {
        try {
            // docker pull
            try {
                log(dockerBuildInfo.buildId, "开始拉取镜像，镜像名称：${dockerBuildInfo.imageName}")
                dockerCli.pullImageCmd(dockerBuildInfo.imageName).exec(PullImageResultCallback()).awaitCompletion()
            } catch (t: Throwable) {
                logger.warn(
                    "[${dockerBuildInfo.buildId}]|Fail to pull the image ${dockerBuildInfo.imageName} ",
                    t
                )
                log(dockerBuildInfo.buildId, "拉取镜像失败，尝试使用本地镜像启动")
            }
            log(dockerBuildInfo.buildId, "拉取镜像成功，准备启动构建环境...")

            // docker run
            val volumeWs = Volume(dockerHostConfig.volumeWorkspace)
            val volumeProjectShare = Volume(dockerHostConfig.volumeProjectShare)
            val volumeMavenRepo = Volume(dockerHostConfig.volumeMavenRepo)
            val volumeNpmPrefix = Volume(dockerHostConfig.volumeNpmPrefix)
            val volumeNpmCache = Volume(dockerHostConfig.volumeNpmCache)
            val volumeCcache = Volume(dockerHostConfig.volumeCcache)
            val volumeApps = Volume(dockerHostConfig.volumeApps)
            val volumeCodecc = Volume(dockerHostConfig.volumeCodecc)
            val volumeInit = Volume(dockerHostConfig.volumeInit)
            val volumeLogs = Volume(dockerHostConfig.volumeLogs)
            val volumeGradleCache = Volume(dockerHostConfig.volumeGradleCache)
            val volumeHosts = Volume(etcHosts)

            val gateway = System.getProperty("devops.gateway", defaultGateway)
            logger.info("[${dockerBuildInfo.buildId}]|gateway is: $gateway")

            val binds = mutableListOf(Bind("${dockerHostConfig.hostPathMavenRepo}/${dockerBuildInfo.pipelineId}/${dockerBuildInfo.vmSeqId}/", volumeMavenRepo),
                    Bind("${dockerHostConfig.hostPathNpmPrefix}/${dockerBuildInfo.pipelineId}/${dockerBuildInfo.vmSeqId}/", volumeNpmPrefix),
                    Bind("${dockerHostConfig.hostPathNpmCache}/${dockerBuildInfo.pipelineId}/${dockerBuildInfo.vmSeqId}/", volumeNpmCache),
                    Bind("${dockerHostConfig.hostPathCcache}/${dockerBuildInfo.pipelineId}/${dockerBuildInfo.vmSeqId}/", volumeCcache),
                    Bind(dockerHostConfig.hostPathApps, volumeApps, AccessMode.ro),
                    Bind(dockerHostConfig.hostPathCodecc, volumeCodecc, AccessMode.ro),
                    Bind(dockerHostConfig.hostPathInit, volumeInit, AccessMode.ro),
                    Bind(etcHosts, volumeHosts, AccessMode.ro),
                    Bind("${dockerHostConfig.hostPathLogs}/${dockerBuildInfo.buildId}/${dockerBuildInfo.vmSeqId}/", volumeLogs),
                    Bind("${dockerHostConfig.hostPathGradleCache}/${dockerBuildInfo.pipelineId}/${dockerBuildInfo.vmSeqId}/", volumeGradleCache),
                    Bind(getWorkspace(dockerBuildInfo.pipelineId, dockerBuildInfo.vmSeqId), volumeWs))
            if (enableProjectShare(dockerBuildInfo.projectId)) {
                binds.add(Bind(getProjectShareDir(dockerBuildInfo.projectId), volumeProjectShare))
            }

            val container = dockerCli.createContainerCmd(dockerBuildInfo.imageName)
                .withCmd("/bin/sh", ENTRY_POINT_CMD)
                .withEnv(
                    listOf(
                        "$ENV_KEY_PROJECT_ID=${dockerBuildInfo.projectId}",
                        "$ENV_KEY_AGENT_ID=${dockerBuildInfo.agentId}",
                        "$ENV_KEY_AGENT_SECRET_KEY=${dockerBuildInfo.secretKey}",
                        "$ENV_KEY_GATEWAY=$gateway",
                        "TERM=xterm-256color",
                        "$ENV_DOCKER_HOST_IP=${CommonUtils.getInnerIP()}",
                        "$ENV_DOCKER_HOST_PORT=${commonConfig.serverPort}",
                        "$BK_DISTCC_LOCAL_IP=${CommonUtils.getInnerIP()}"
                    )
                )
                .withVolumes(volumeWs).withVolumes(volumeApps).withVolumes(volumeInit)
                .withHostConfig(HostConfig().withBinds(binds).withNetworkMode("bridge"))
                .exec()

            logger.info("[${dockerBuildInfo.buildId}]|Created container $container")
            dockerCli.startContainerCmd(container.id).exec()

            return container.id
        } catch (er: Throwable) {
            logger.error(er.toString())
            logger.error(er.cause.toString())
            logger.error(er.message)
            log(dockerBuildInfo.buildId, true, "启动构建环境失败，错误信息:${er.message}")
            throw ContainerException("Create container failed")
        }
    }

    fun stopContainer(dockerBuildInfo: DockerHostBuildInfo) {
        try {
            // docker stop
            val containerInfo = dockerCli.inspectContainerCmd(dockerBuildInfo.containerId).exec()
            if ("exited" != containerInfo.state.status) {
                dockerCli.stopContainerCmd(dockerBuildInfo.containerId).withTimeout(30).exec()
            }
        } catch (e: Throwable) {
            logger.error("[${dockerBuildInfo.buildId}]|Stop the container failed, containerId: ${dockerBuildInfo.containerId}, error msg: $e")
        }

        try {
            // docker rm
            dockerCli.removeContainerCmd(dockerBuildInfo.containerId).exec()
        } catch (e: Throwable) {
            logger.error("[${dockerBuildInfo.buildId}]|Stop the container failed, containerId: ${dockerBuildInfo.containerId}, error msg: $e")
        }
    }

    fun getContainerNum(): Int {
        try {
            val dockerInfo = dockerCli.infoCmd().exec()
            return dockerInfo.containersRunning ?: 0
        } catch (e: Throwable) {
            logger.error("Get container num failed")
        }
        return 0
    }

    fun dockerBuildAndPushImage(
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        dockerBuildParam: DockerBuildParam
    ): Pair<Boolean, String?> {
        try {
            val repoAddr = dockerBuildParam.repoAddr
            val userName = dockerBuildParam.userName
            val password = dockerBuildParam.password

            val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHostConfig.dockerHost)
                .withDockerConfig(dockerHostConfig.dockerConfig)
                .withApiVersion(dockerHostConfig.apiVersion)
                .withRegistryUrl(repoAddr)
                .withRegistryUsername(userName)
                .withRegistryPassword(password)
                .build()

            val dockerCli = DockerClientBuilder.getInstance(config).build()
            val authConfig = AuthConfig()
                .withUsername(userName)
                .withPassword(password)
                .withRegistryAddress(repoAddr)
            val authConfigurations = AuthConfigurations()
            authConfigurations.addConfig(authConfig)

            val workspace = getWorkspace(pipelineId, vmSeqId.toInt())
            val buildDir = Paths.get(workspace + dockerBuildParam.buildDir).normalize().toString()
            val dockerfilePath = Paths.get(workspace + dockerBuildParam.dockerFile).normalize().toString()
            val baseDirectory = File(buildDir)
            val dockerfile = File(dockerfilePath)
            val imageNameTag =
                getImageNameWithTag(repoAddr, projectId, dockerBuildParam.imageName, dockerBuildParam.imageTag)

            logger.info("Build docker image, workspace: $workspace, buildDir:$buildDir, dockerfile: $dockerfilePath")
            logger.info("Build docker image, imageNameTag: $imageNameTag")
            dockerCli.buildImageCmd().withNoCache(true)
                .withPull(true)
                .withBuildAuthConfigs(authConfigurations)
                .withBaseDirectory(baseDirectory)
                .withDockerfile(dockerfile)
                .withTags(setOf(imageNameTag))
                .exec(BuildImageResultCallback())
                .awaitImageId()

            logger.info("Build image success, now push to repo, image name and tag: $imageNameTag")
            dockerCli.pushImageCmd(imageNameTag)
                .withAuthConfig(authConfig)
                .exec(PushImageResultCallback())
                .awaitCompletion()

            logger.info("Push image success, now remove local image, image name and tag: $imageNameTag")

            try {
                dockerCli.removeImageCmd(
                    getImageNameWithTag(
                        repoAddr,
                        projectId,
                        dockerBuildParam.imageName,
                        dockerBuildParam.imageTag
                    )
                ).exec()
                logger.info("Remove local image success")
            } catch (e: Throwable) {
                logger.error("Docker rmi failed, msg: ${e.message}")
            }

            return Pair(true, null)
        } catch (e: Throwable) {
            logger.error("Docker build and push failed, exception: ", e)
            val cause = if (e.cause != null && e.cause!!.message != null) {
                e.cause!!.message!!.removePrefix(getWorkspace(pipelineId, vmSeqId.toInt()))
            } else {
                ""
            }
            return Pair(false, e.message + if (cause.isBlank()) "" else " cause:【$cause】")
        }
    }

    fun clearContainers() {
        val containerInfo = dockerCli.listContainersCmd().withStatusFilter(setOf("exited")).exec()
        for (container in containerInfo) {
            logger.info("Clear container, containerId: ${container.id}")
            dockerCli.removeContainerCmd(container.id).exec()
        }
    }

    fun isContainerRunning(containerId: String): Boolean {
        // 正常情况下此时容器已经启动了，至少能存活5s，如果不能，说明启动命令有问题，启动后立即退出了
        for (i in 1..5) {
            Thread.sleep(1000)
            val inspectContainerResponse = dockerCli.inspectContainerCmd(containerId).exec() ?: return false
            if (false == inspectContainerResponse.state.running) return false
        }
        return true
    }

    fun log(buildId: String, message: String) {
        return log(buildId, false, message)
    }

    fun log(buildId: String, red: Boolean, message: String) {
        logger.info("[$buildId]|write log to dispatch, message: $message")
        try {
            dockerHostBuildApi.log(buildId, red, message)
        } catch (e: Exception) {
            logger.info("[$buildId]|write log to dispatch failed")
        }
    }

    private fun getImageNameWithTag(repoAddr: String, projectId: String, imageName: String, imageTag: String): String {
        return "$repoAddr/paas/$projectId/$imageName:$imageTag"
    }

    private fun getWorkspace(pipelineId: String, vmSeqId: Int): String {
        return "${dockerHostConfig.hostPathWorkspace}/$pipelineId/$vmSeqId/"
    }

    private fun getProjectShareDir(projectCode: String): String {
        return "${dockerHostConfig.hostPathProjectShare}/$projectCode/"
    }

    private fun enableProjectShare(projectCode: String): Boolean {
        if (dockerHostConfig.shareProjectCodeWhiteList.isNullOrBlank()) {
            return false
        }
        val whiteList = dockerHostConfig.shareProjectCodeWhiteList!!.split(",").map { it.trim() }
        return whiteList.contains(projectCode)
    }
}
