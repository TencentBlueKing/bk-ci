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

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.BuildImageResultCallback
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.exception.UnauthorizedException
import com.github.dockerjava.api.model.AuthConfig
import com.github.dockerjava.api.model.AuthConfigurations
import com.github.dockerjava.api.model.BuildResponseItem
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Ports
import com.github.dockerjava.api.model.PullResponseItem
import com.github.dockerjava.api.model.PushResponseItem
import com.github.dockerjava.api.model.Statistics
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.InvocationBuilder
import com.github.dockerjava.core.command.LogContainerResultCallback
import com.github.dockerjava.core.command.PullImageResultCallback
import com.github.dockerjava.core.command.PushImageResultCallback
import com.github.dockerjava.core.command.WaitContainerResultCallback
import com.github.dockerjava.okhttp.OkDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dockerhost.common.Constants
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.dispatch.AlertApi
import com.tencent.devops.dockerhost.dispatch.DockerHostBuildResourceApi
import com.tencent.devops.dockerhost.docker.DockerBindLoader
import com.tencent.devops.dockerhost.docker.DockerEnvLoader
import com.tencent.devops.dockerhost.docker.DockerVolumeLoader
import com.tencent.devops.dockerhost.exception.ContainerException
import com.tencent.devops.dockerhost.exception.NoSuchImageException
import com.tencent.devops.dockerhost.pojo.CheckImageRequest
import com.tencent.devops.dockerhost.pojo.CheckImageResponse
import com.tencent.devops.dockerhost.pojo.DockerBuildParam
import com.tencent.devops.dockerhost.pojo.DockerRunParam
import com.tencent.devops.dockerhost.pojo.DockerRunPortBinding
import com.tencent.devops.dockerhost.utils.CommonUtils
import com.tencent.devops.dockerhost.utils.ENTRY_POINT_CMD
import com.tencent.devops.dockerhost.utils.RandomUtil
import com.tencent.devops.dockerhost.utils.SigarUtil
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

@Component
class DockerHostBuildService(
    private val dockerHostConfig: DockerHostConfig,
    private val environment: Environment
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DockerHostBuildService::class.java)
    }

    private val dockerHostBuildApi: DockerHostBuildResourceApi =
        DockerHostBuildResourceApi(if ("codecc_build" == dockerHostConfig.dockerhostMode) "ms/dispatch-codecc" else "ms/dispatch")

    private val alertApi: AlertApi =
        AlertApi(if ("codecc_build" == dockerHostConfig.dockerhostMode) "ms/dispatch-codecc" else "ms/dispatch")

    private val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerConfig(dockerHostConfig.dockerConfig)
        .withApiVersion(dockerHostConfig.apiVersion)
        .build()

    final var httpClient: DockerHttpClient = OkDockerHttpClient.Builder()
        .dockerHost(config.dockerHost)
        .sslConfig(config.sslConfig)
        .connectTimeout(5000)
        .readTimeout(30000)
        .build()

    final var longHttpClient: DockerHttpClient = OkDockerHttpClient.Builder()
        .dockerHost(config.dockerHost)
        .sslConfig(config.sslConfig)
        .connectTimeout(5000)
        .readTimeout(300000)
        .build()

    private val httpDockerCli = DockerClientBuilder.getInstance(config).withDockerHttpClient(httpClient).build()

    private val httpLongDockerCli = DockerClientBuilder.getInstance(config).withDockerHttpClient(longHttpClient).build()

    fun startBuild(): DockerHostBuildInfo? {
        val result = dockerHostBuildApi.startBuild(CommonUtils.getInnerIP())
        if (result != null) {
            if (result.isNotOk()) {
                return null
            }
        }
        return result!!.data!!
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

    fun rollbackBuild(
        buildId: String,
        vmSeqId: Int,
        shutdown: Boolean,
        containerId: String,
        containerHashId: String?
    ): Boolean {
        log(
            buildId = buildId,
            red = true,
            message = if (shutdown) "构建环境启动后即退出，请检查镜像是否合法或联系【蓝盾助手】查看，构建任务将失败退出" else "启动构建环境失败，构建任务将重试",
            tag = VMUtils.genStartVMTaskId(containerId),
            containerHashId = containerHashId
        )

        val result = dockerHostBuildApi.rollbackBuild(buildId, vmSeqId, shutdown)
        if (result != null) {
            if (result.isNotOk()) {
                logger.info("[$buildId]|rollbackBuild return msg: ${result.message}")
                return false
            }
        }
        return result!!.data!!
    }

    fun pullImage(
        imageType: String?,
        imageName: String,
        registryUser: String?,
        registryPwd: String?,
        buildId: String,
        containerId: String?,
        containerHashId: String?
    ): Result<Boolean> {
        val authConfig = CommonUtils.getAuthConfig(
            imageType = imageType,
            dockerHostConfig = dockerHostConfig,
            imageName = imageName,
            registryUser = registryUser,
            registryPwd = registryPwd
        )
        val dockerImageName = CommonUtils.normalizeImageName(imageName)
        val taskId = if (!containerId.isNullOrBlank()) VMUtils.genStartVMTaskId(containerId!!) else ""
        log(buildId, "开始拉取镜像，镜像名称：$dockerImageName", taskId, containerHashId)
        httpLongDockerCli.pullImageCmd(dockerImageName).withAuthConfig(authConfig)
            .exec(MyPullImageResultCallback(buildId, dockerHostBuildApi, taskId, containerHashId)).awaitCompletion()
        log(buildId, "拉取镜像成功，准备启动构建环境...", taskId, containerHashId)
        return Result(true)
    }

    fun checkImage(
        buildId: String,
        checkImageRequest: CheckImageRequest,
        containerId: String?,
        containerHashId: String?
    ): Result<CheckImageResponse?> {
        logger.info("checkImage buildId: $buildId, checkImageRequest: $checkImageRequest")
        // 判断用户录入的镜像信息是否能正常拉取到镜像
        val imageName = checkImageRequest.imageName
        try {
            val pullImageResult = pullImage(
                imageType = checkImageRequest.imageType,
                imageName = checkImageRequest.imageName,
                registryUser = checkImageRequest.registryUser,
                registryPwd = checkImageRequest.registryPwd,
                buildId = buildId,
                containerId = containerId,
                containerHashId = containerHashId
            )
            logger.info("pullImageResult: $pullImageResult")
            if (pullImageResult.isNotOk()) {
                return Result(pullImageResult.status, pullImageResult.message, null)
            }
        } catch (t: Throwable) {
            logger.warn("Fail to pull the image $imageName of build $buildId", t)
            log(buildId, "pull image fail，error is：${t.message}", containerId, containerHashId)
            return Result(CommonMessageCode.SYSTEM_ERROR.toInt(), t.message, null)
        }
        val dockerImageName = CommonUtils.normalizeImageName(checkImageRequest.imageName)
        // 查询镜像详细信息
        val imageInfo = httpLongDockerCli.inspectImageCmd(dockerImageName).exec()
        logger.info("imageInfo: $imageInfo")
        val checkImageResponse = CheckImageResponse(
            author = imageInfo.author,
            comment = imageInfo.comment,
            size = imageInfo.size!!,
            virtualSize = imageInfo.virtualSize,
            repoTags = imageInfo.repoTags!!
        )
        return Result(checkImageResponse)
    }

    fun createContainer(dockerBuildInfo: DockerHostBuildInfo): String {
        try {
            val imageName = CommonUtils.normalizeImageName(dockerBuildInfo.imageName)
            val taskId = VMUtils.genStartVMTaskId(dockerBuildInfo.vmSeqId.toString())
            // docker pull
            if (dockerBuildInfo.imagePublicFlag == true && dockerBuildInfo.imageRDType?.toLowerCase() == ImageRDTypeEnum.SELF_DEVELOPED.name.toLowerCase()) {
                log(
                    buildId = dockerBuildInfo.buildId,
                    message = "自研公共镜像，不从仓库拉取，直接从本地启动...",
                    tag = taskId,
                    containerHashId = dockerBuildInfo.containerHashId
                )
            } else {
                try {
                    LocalImageCache.saveOrUpdate(imageName)
                    pullImage(
                        imageType = dockerBuildInfo.imageType,
                        imageName = dockerBuildInfo.imageName,
                        registryUser = dockerBuildInfo.registryUser,
                        registryPwd = dockerBuildInfo.registryPwd,
                        buildId = dockerBuildInfo.buildId,
                        containerId = dockerBuildInfo.vmSeqId.toString(),
                        containerHashId = dockerBuildInfo.containerHashId
                    )
                } catch (t: UnauthorizedException) {
                    val errorMessage = "无权限拉取镜像：$imageName，请检查镜像路径或凭证是否正确；[buildId=${dockerBuildInfo.buildId}][containerHashId=${dockerBuildInfo.containerHashId}]"
                    logger.error(errorMessage, t)
                    // 直接失败，禁止使用本地镜像
                    throw NotFoundException(errorMessage)
                } catch (t: NotFoundException) {
                    val errorMessage = "镜像不存在：$imageName，请检查镜像路径或凭证是否正确；[buildId=${dockerBuildInfo.buildId}][containerHashId=${dockerBuildInfo.containerHashId}]"
                    logger.error(errorMessage, t)
                    // 直接失败，禁止使用本地镜像
                    throw NotFoundException(errorMessage)
                } catch (t: Throwable) {
                    logger.warn("Fail to pull the image $imageName of build ${dockerBuildInfo.buildId}", t)
                    log(
                        buildId = dockerBuildInfo.buildId,
                        message = "拉取镜像失败，错误信息：${t.message}",
                        tag = taskId,
                        containerHashId = dockerBuildInfo.containerHashId
                    )
                    log(
                        buildId = dockerBuildInfo.buildId,
                        message = "尝试使用本地镜像启动...",
                        tag = taskId,
                        containerHashId = dockerBuildInfo.containerHashId
                    )
                }
            }
            // docker run
            val binds = DockerBindLoader.loadBinds(dockerBuildInfo)

            val containerName = "dispatch-${dockerBuildInfo.buildId}-${dockerBuildInfo.vmSeqId}-${RandomUtil.randomString()}"
            val container = httpLongDockerCli.createContainerCmd(imageName)
                .withName(containerName)
                .withCmd("/bin/sh", ENTRY_POINT_CMD)
                .withEnv(DockerEnvLoader.loadEnv(dockerBuildInfo))
                .withVolumes(DockerVolumeLoader.loadVolumes(dockerBuildInfo))
                .withHostConfig(HostConfig().withBinds(binds).withNetworkMode("bridge"))
                .exec()

            logger.info("Created container $container")
            httpLongDockerCli.startContainerCmd(container.id).exec()

            return container.id
        } catch (er: Throwable) {
            logger.error(er.toString())
            logger.error(er.cause.toString())
            logger.error(er.message)
            log(
                buildId = dockerBuildInfo.buildId,
                red = true,
                message = "启动构建环境失败，错误信息:${er.message}",
                tag = VMUtils.genStartVMTaskId(dockerBuildInfo.vmSeqId.toString()),
                containerHashId = dockerBuildInfo.containerHashId
            )
            if (er is NotFoundException) {
                throw NoSuchImageException("Create container failed: ${er.message}")
            } else {
                alertApi.alert(
                    AlertLevel.HIGH.name, "Docker构建机创建容器失败", "Docker构建机创建容器失败, " +
                    "母机IP:${CommonUtils.getInnerIP()}， 失败信息：${er.message}"
                )
                throw ContainerException("Create container failed")
            }
        }
    }

    fun stopContainer(dockerBuildInfo: DockerHostBuildInfo) {
        try {
            // docker stop
            val containerInfo = httpLongDockerCli.inspectContainerCmd(dockerBuildInfo.containerId).exec()
            if ("exited" != containerInfo.state.status) {
                httpLongDockerCli.stopContainerCmd(dockerBuildInfo.containerId).withTimeout(15).exec()
            }
        } catch (e: Throwable) {
            logger.error("Stop the container failed, containerId: ${dockerBuildInfo.containerId}, error msg: $e")
        }

        try {
            // docker rm
            httpLongDockerCli.removeContainerCmd(dockerBuildInfo.containerId).exec()
        } catch (e: Throwable) {
            logger.error("Stop the container failed, containerId: ${dockerBuildInfo.containerId}, error msg: $e")
        } finally {
            // 找出所有跟本次构建关联的dockerRun启动容器并停止容器
            val containerInfo = httpLongDockerCli.listContainersCmd().withStatusFilter(setOf("running")).exec()
            for (container in containerInfo) {
                try {
                    // logger.info("${dockerBuildInfo.buildId}|${dockerBuildInfo.vmSeqId} containerName: ${container.names[0]}")
                    val containerName = container.names[0]
                    if (containerName.contains(getDockerRunStopPattern(dockerBuildInfo))) {
                        logger.info("${dockerBuildInfo.buildId}|${dockerBuildInfo.vmSeqId} stop dockerRun container, containerId: ${container.id}")
                        httpLongDockerCli.stopContainerCmd(container.id).withTimeout(15).exec()
                    }
                } catch (e: Exception) {
                    logger.error("${dockerBuildInfo.buildId}|${dockerBuildInfo.vmSeqId} Stop dockerRun container failed, containerId: ${container.id}", e)
                }
            }
        }
    }

    private fun getDockerRunStopPattern(dockerBuildInfo: DockerHostBuildInfo): String {
        // 用户取消操作
        return if (dockerBuildInfo.vmSeqId == 0) {
            "dockerRun-${dockerBuildInfo.buildId}"
        } else {
            "dockerRun-${dockerBuildInfo.buildId}-${dockerBuildInfo.vmSeqId}"
        }
    }

    fun getContainerNum(): Int {
        try {
            val dockerInfo = httpLongDockerCli.infoCmd().exec()
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
        dockerBuildParam: DockerBuildParam,
        buildId: String,
        elementId: String?,
        outer: Boolean
    ): Pair<Boolean, String?> {
        lateinit var dockerClient: DockerClient
        try {
            val repoAddr = dockerBuildParam.repoAddr
            val userName = dockerBuildParam.userName
            val password = dockerBuildParam.password
            val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerConfig(dockerHostConfig.dockerConfig)
                .withApiVersion(dockerHostConfig.apiVersion)
                .withRegistryUrl(repoAddr)
                .withRegistryUsername(userName)
                .withRegistryPassword(password)
                .build()

            val longHttpClient: DockerHttpClient = OkDockerHttpClient.Builder()
                .dockerHost(config.dockerHost)
                .sslConfig(config.sslConfig)
                .connectTimeout(5000)
                .readTimeout(300000)
                .build()

            dockerClient = DockerClientBuilder.getInstance(config).withDockerHttpClient(longHttpClient).build()
            val authConfig = AuthConfig()
                .withUsername(userName)
                .withPassword(password)
                .withRegistryAddress(repoAddr)

            val authConfigurations = AuthConfigurations()
            authConfigurations.addConfig(authConfig)

            val ticket = dockerBuildParam.ticket
            val args = dockerBuildParam.args
            ticket.forEach { it ->
                val baseConfig = AuthConfig()
                    .withUsername(it.second)
                    .withPassword(it.third)
                    .withRegistryAddress(it.first)
                authConfigurations.addConfig(baseConfig)
            }

            val workspace = getWorkspace(pipelineId, vmSeqId.toInt(), dockerBuildParam.poolNo ?: "0")
            val buildDir = Paths.get(workspace + dockerBuildParam.buildDir).normalize().toString()
            val dockerfilePath = Paths.get(workspace + dockerBuildParam.dockerFile).normalize().toString()
            val baseDirectory = File(buildDir)
            val dockerfile = File(dockerfilePath)
            val imageNameTag =
                getImageNameWithTag(
                    repoAddr = repoAddr,
                    projectId = projectId,
                    imageName = dockerBuildParam.imageName,
                    imageTag = dockerBuildParam.imageTag,
                    outer = outer
                )

            logger.info("Build docker image, workspace: $workspace, buildDir:$buildDir, dockerfile: $dockerfilePath")
            logger.info("Build docker image, imageNameTag: $imageNameTag")
            val step = dockerClient.buildImageCmd().withNoCache(true)
                .withPull(true)
                .withBuildAuthConfigs(authConfigurations)
                .withBaseDirectory(baseDirectory)
                .withDockerfile(dockerfile)
                .withTags(setOf(imageNameTag))
            args.map { it.trim().split("=") }.forEach {
                step.withBuildArg(it.first(), it.last())
            }
            step.exec(MyBuildImageResultCallback(buildId, elementId, dockerHostBuildApi))
                .awaitImageId()

            logger.info("Build image success, now push to repo, image name and tag: $imageNameTag")
            dockerClient.pushImageCmd(imageNameTag)
                .withAuthConfig(authConfig)
                .exec(MyPushImageResultCallback(buildId, elementId, dockerHostBuildApi))
                .awaitCompletion()

            logger.info("Push image success, now remove local image, image name and tag: $imageNameTag")

            try {
                httpLongDockerCli.removeImageCmd(
                    getImageNameWithTag(
                        repoAddr = repoAddr,
                        projectId = projectId,
                        imageName = dockerBuildParam.imageName,
                        imageTag = dockerBuildParam.imageTag
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
                e.cause!!.message!!.removePrefix(getWorkspace(pipelineId, vmSeqId.toInt(), dockerBuildParam.poolNo ?: "0"))
            } else {
                ""
            }
            return Pair(false, e.message + if (cause.isBlank()) "" else " cause:【$cause】")
        } finally {
            try {
                dockerClient.close()
            } catch (e: IOException) {
                logger.error("docker client close exception: ${e.message}")
            }
        }
    }

    fun dockerRun(
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String,
        dockerRunParam: DockerRunParam
    ): Triple<String, Int, List<DockerRunPortBinding>> {
        try {
            val imageName = CommonUtils.normalizeImageName(dockerRunParam.imageName)
            // docker pull
            try {
                LocalImageCache.saveOrUpdate(imageName)
                pullImage(
                    imageType = ImageType.THIRD.type,
                    imageName = dockerRunParam.imageName,
                    registryUser = dockerRunParam.registryUser,
                    registryPwd = dockerRunParam.registryPwd,
                    buildId = buildId,
                    containerId = vmSeqId,
                    containerHashId = ""
                )
            } catch (t: UnauthorizedException) {
                val errorMessage = "无权限拉取镜像：$imageName，请检查凭证"
                logger.error(errorMessage, t)
                // 直接失败，禁止使用本地镜像
                throw NotFoundException(errorMessage)
            } catch (t: NotFoundException) {
                val errorMessage = "仓库中镜像不存在：$imageName，请检查凭证"
                logger.error(errorMessage, t)
                // 直接失败，禁止使用本地镜像
                throw NotFoundException(errorMessage)
            } catch (t: Throwable) {
                logger.warn("Fail to pull the image $imageName of build $buildId", t, "")
                log(
                    buildId = buildId,
                    message = "拉取镜像失败，错误信息：${t.message}",
                    tag = VMUtils.genStartVMTaskId(vmSeqId),
                    containerHashId = ""
                )
                log(
                    buildId = buildId,
                    message = "尝试使用本地镜像执行命令...",
                    tag = VMUtils.genStartVMTaskId(vmSeqId),
                    containerHashId = ""
                )
            }

            val dockerBuildInfo = DockerHostBuildInfo(
                projectId = projectId,
                agentId = "",
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeqId = vmSeqId.toInt(),
                secretKey = "",
                status = PipelineTaskStatus.RUNNING.status,
                imageName = imageName,
                containerId = "",
                wsInHost = true,
                poolNo = if (dockerRunParam.poolNo == null) 0 else dockerRunParam.poolNo!!.toInt(),
                registryUser = dockerRunParam.registryUser,
                registryPwd = dockerRunParam.registryPwd,
                imageType = ImageType.THIRD.type,
                imagePublicFlag = false,
                imageRDType = null,
                containerHashId = ""
            )
            // docker run
            val env = mutableListOf<String>()
            env.addAll(DockerEnvLoader.loadEnv(dockerBuildInfo))
            env.add("bk_devops_start_source=dockerRun") // dockerRun启动标识
            dockerRunParam.env?.forEach {
                env.add("${it.key}=${it.value ?: ""}")
            }
            logger.info("env is $env")
            val binds = DockerBindLoader.loadBinds(dockerBuildInfo)

            val dockerRunPortBindingList = mutableListOf<DockerRunPortBinding>()
            val hostIp = CommonUtils.getInnerIP()
            val portBindings = Ports()
            dockerRunParam.portList?.forEach {
                val localPort = getAvailableHostPort()
                if (localPort == 0) {
                    throw ContainerException("No enough port to use in dockerRun. startPort: ${dockerHostConfig.startPort}")
                }
                val tcpContainerPort: ExposedPort = ExposedPort.tcp(it)
                portBindings.bind(tcpContainerPort, Ports.Binding.bindPort(localPort))
                dockerRunPortBindingList.add(DockerRunPortBinding(hostIp, it, localPort))
            }

            val containerName = "dockerRun-${dockerBuildInfo.buildId}-${dockerBuildInfo.vmSeqId}-${RandomUtil.randomString()}"

            val container = if (dockerRunParam.command.isEmpty() || dockerRunParam.command.equals("[]")) {
                httpLongDockerCli.createContainerCmd(imageName)
                    .withName(containerName)
                    .withEnv(env)
                    .withVolumes(DockerVolumeLoader.loadVolumes(dockerBuildInfo))
                    .withHostConfig(HostConfig().withBinds(binds).withNetworkMode("bridge").withPortBindings(portBindings))
                    .withWorkingDir(dockerHostConfig.volumeWorkspace)
                    .exec()
            } else {
                httpLongDockerCli.createContainerCmd(imageName)
                    .withName(containerName)
                    .withCmd(dockerRunParam.command)
                    .withEnv(env)
                    .withVolumes(DockerVolumeLoader.loadVolumes(dockerBuildInfo))
                    .withHostConfig(HostConfig().withBinds(binds).withNetworkMode("bridge").withPortBindings(portBindings))
                    .withWorkingDir(dockerHostConfig.volumeWorkspace)
                    .exec()
            }

            logger.info("Created container $container")
            val timestamp = (System.currentTimeMillis() / 1000).toInt()
            httpLongDockerCli.startContainerCmd(container.id).exec()

            return Triple(container.id, timestamp, dockerRunPortBindingList)
        } catch (er: Throwable) {
            val errorLog = "[$buildId]|启动容器失败，错误信息:${er.message}"
            logger.error(errorLog, er)
            log(buildId, true, errorLog, VMUtils.genStartVMTaskId(vmSeqId), "")
            alertApi.alert(
                level = AlertLevel.HIGH.name, title = "Docker构建机创建容器失败",
                message = "Docker构建机创建容器失败, 母机IP:${CommonUtils.getInnerIP()}， 失败信息：${er.message}"
            )
            throw ContainerException("启动容器失败，错误信息:${er.message}")
        } finally {
            if (!dockerRunParam.registryUser.isNullOrEmpty()) {
                try {
                    httpLongDockerCli.removeImageCmd(dockerRunParam.imageName)
                    logger.info("Delete local image successfully......")
                } catch (e: java.lang.Exception) {
                    logger.info("the exception of deleteing local image is ${e.message}")
                } finally {
                    logger.info("Docker run end......")
                }
            }
        }
    }

    fun dockerStop(projectId: String, pipelineId: String, vmSeqId: String, buildId: String, containerId: String) {
        try {
            // docker stop
            val containerInfo = httpLongDockerCli.inspectContainerCmd(containerId).exec()
            if ("exited" != containerInfo.state.status) {
                httpLongDockerCli.stopContainerCmd(containerId).withTimeout(15).exec()
            }
        } catch (e: Throwable) {
            logger.error("Stop the container failed, containerId: $containerId, error msg: $e")
        }

        try {
            // docker rm
            httpLongDockerCli.removeContainerCmd(containerId).exec()
        } catch (e: Throwable) {
            logger.error("Stop the container failed, containerId: $containerId, error msg: $e")
        }
    }

    fun getDockerLogs(containerId: String, lastLogTime: Int): List<String> {
        val logs = ArrayList<String>()
        val logContainerCmd = httpLongDockerCli.logContainerCmd(containerId)
            .withStdOut(true)
            .withStdErr(true)
            .withSince(lastLogTime)
            .withTimestamps(true)
        try {
            logContainerCmd.exec(object : LogContainerResultCallback() {
                override fun onNext(item: Frame) {
                    logs.add(item.toString())
                }
            }).awaitCompletion()
        } catch (e: InterruptedException) {
            logger.error("Get docker run log exception: ", e)
        }
        return logs
    }

    fun getDockerRunExitCode(containerId: String): Int? {
        return try {
            httpLongDockerCli.waitContainerCmd(containerId)
                .exec(WaitContainerResultCallback())
                .awaitStatusCode(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            logger.error("[$containerId]| getDockerRunExitCode error.", e)
            Constants.DOCKER_EXIST_CODE
        }
    }

    fun monitorSystemLoad() {
        logger.info("Monitor systemLoad cpu: ${SigarUtil.getAverageLongCpuLoad()}, mem: ${SigarUtil.getAverageLongMemLoad()}")
        Thread.sleep(10000)
        if (SigarUtil.getAverageLongCpuLoad() > 90 || SigarUtil.getAverageLongMemLoad() > 80) {
            checkContainerStats()
        }
    }

    fun checkContainerStats() {
        val containerInfo = httpLongDockerCli.listContainersCmd().withStatusFilter(setOf("running")).exec()
        for (container in containerInfo) {
            val statistics = getContainerStats(container.id)
            if (statistics != null) {
                val systemCpuUsage = statistics.cpuStats.systemCpuUsage ?: 0
                val cpuUsage = statistics.cpuStats.cpuUsage!!.totalUsage ?: 0
                val preSystemCpuUsage = statistics.preCpuStats.systemCpuUsage ?: 0
                val preCpuUsage = statistics.preCpuStats.cpuUsage!!.totalUsage ?: 0
                val cpuUsagePer = ((cpuUsage - preCpuUsage) * 100) / (systemCpuUsage - preSystemCpuUsage)

                if (statistics.memoryStats != null && statistics.memoryStats.usage != null && statistics.memoryStats.limit != null) {
                    val memUsage = statistics.memoryStats.usage!! * 100 / statistics.memoryStats.limit!!
                    logger.info("containerId: ${container.id} | checkContainerStats cpuUsagePer: $cpuUsagePer, memUsage: $memUsage")
                    if (memUsage > 80 || cpuUsagePer > 85) {
                        resetContainer(container.id)
                    }
                }
            }
        }
    }

    fun getContainerStats(containerId: String): Statistics? {
        val asyncResultCallback = InvocationBuilder.AsyncResultCallback<Statistics>()
        httpDockerCli.statsCmd(containerId).withNoStream(true).exec(asyncResultCallback)
        return try {
            val stats = asyncResultCallback.awaitResult()
            asyncResultCallback.close()
            stats
        } catch (e: Exception) {
            logger.error("containerId: $containerId get containerStats error.", e)
            null
        }
    }

    fun resetContainer(containerId: String) {
        httpDockerCli.updateContainerCmd(containerId).withMemoryReservation(10 * 1024 * 1024 * 1024L).withCpuPeriod(10000).withCpuQuota(80000).exec()
        logger.info("<<<< Trigger container reset, containerId: $containerId")
    }

    fun clearContainers() {
        val containerInfo = httpLongDockerCli.listContainersCmd().withStatusFilter(setOf("exited")).exec()
        for (container in containerInfo) {
            try {
                val finishTime = httpLongDockerCli.inspectContainerCmd(container.id).exec().state.finishedAt
                // 是否已退出30分钟
                if (checkFinishTime(finishTime)) {
                    logger.info("Clear container, containerId: ${container.id}")
                    httpLongDockerCli.removeContainerCmd(container.id).exec()
                }
            } catch (e: Exception) {
                logger.error("Clear container failed, containerId: ${container.id}", e)
            }
        }
    }

    fun clearDockerRunTimeoutContainers() {
        val containerInfo = httpLongDockerCli.listContainersCmd().withStatusFilter(setOf("running")).exec()
        for (container in containerInfo) {
            try {
                val startTime = httpLongDockerCli.inspectContainerCmd(container.id).exec().state.startedAt
                val envs = httpLongDockerCli.inspectContainerCmd(container.id).exec().config.env
                // 是否是dockerRun启动的并且已运行超过8小时
                if (envs != null && envs.contains("bk_devops_start_source=dockerRun") && checkStartTime(startTime)) {
                    logger.info("Clear dockerRun timeout container, containerId: ${container.id}")
                    httpLongDockerCli.stopContainerCmd(container.id).withTimeout(15).exec()
                }
            } catch (e: Exception) {
                logger.error("Clear dockerRun timeout container failed, containerId: ${container.id}", e)
            }
        }
    }

    @PostConstruct
    fun loadLocalImages() {
        try {
            val imageList = httpLongDockerCli.listImagesCmd().withShowAll(true).exec()
            logger.info("load local images, image count: ${imageList.size}")
            imageList.forEach c@{
                it.repoTags?.forEach { image ->
                    LocalImageCache.saveOrUpdate(image)
                }
            }
        } catch (e: java.lang.Exception) {
            logger.error("load local image, exception, msg: ${e.message}")
        }
    }

    fun clearLocalImages() {
        val danglingImages = httpLongDockerCli.listImagesCmd().withDanglingFilter(true).withShowAll(true).exec()
        danglingImages.forEach {
            try {
                httpLongDockerCli.removeImageCmd(it.id).exec()
                logger.info("remove local dangling image success, image id: ${it.id}")
            } catch (e: java.lang.Exception) {
                logger.error("remove local dangling image exception ${e.message}")
            }
        }

        val publicImages = getPublicImages()
        val imageList = httpLongDockerCli.listImagesCmd().withShowAll(true).exec()
        imageList.forEach c@{
            if (it.repoTags == null || it.repoTags.isEmpty()) {
                return@c
            }
            it.repoTags.forEach t@{ image ->
                if (publicImages.contains(image)) {
                    logger.info("skip public image: $image")
                    return@t
                }

                val lastUsedDate = LocalImageCache.getDate(image)
                if (null != lastUsedDate) {
                    if ((Date().time - lastUsedDate.time) / (1000 * 60 * 60 * 24) >= dockerHostConfig.localImageCacheDays) {
                        logger.info("remove local image, ${it.repoTags}")
                        try {
                            httpLongDockerCli.removeImageCmd(image).exec()
                            logger.info("remove local image success, image: $image")
                        } catch (e: java.lang.Exception) {
                            logger.error("remove local image exception ${e.message}")
                        }
                        return@c
                    }
                }
            }
        }
    }

    fun isContainerRunning(containerId: String): Boolean {
        try {
            logger.info("Check container: $containerId start.")
            val inspectContainerResponse = httpLongDockerCli.inspectContainerCmd(containerId).exec() ?: return false
            logger.info("Check container: $containerId status: ${inspectContainerResponse.state}")
            return inspectContainerResponse.state.running ?: false
        } catch (e: Exception) {
            logger.error("check container: $containerId status failed.", e)
            return false
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

    fun log(buildId: String, message: String, tag: String?, containerHashId: String?) {
        return log(buildId, false, message, tag, containerHashId)
    }

    fun log(buildId: String, red: Boolean, message: String, tag: String?, containerHashId: String?) {
        logger.info("write log to dispatch, buildId: $buildId, message: $message")
        try {
            dockerHostBuildApi.postLog(
                buildId = buildId,
                red = red,
                message = message,
                tag = tag,
                jobId = containerHashId
            )
        } catch (t: Throwable) {
            logger.info("write log to dispatch failed")
        }
    }

    fun refreshDockerIpStatus(): Boolean? {
        val port = environment.getProperty("local.server.port")
        return dockerHostBuildApi.refreshDockerIpStatus(port, getContainerNum())!!.data
    }

    private fun getPublicImages(): List<String> {
        val result = mutableListOf<String>()
        val publicImages = dockerHostBuildApi.getPublicImages().data!!
        publicImages.filter { it.publicFlag && it.rdType == ImageRDTypeEnum.SELF_DEVELOPED }.forEach {
            result.add("${it.repoUrl}/${it.repoName}:${it.repoTag}")
        }
        return result
    }

    private fun getImageNameWithTag(
        repoAddr: String,
        projectId: String,
        imageName: String,
        imageTag: String,
        outer: Boolean = false
    ): String {
        return if (outer) {
            "$repoAddr/$imageName:$imageTag"
        } else {
            "$repoAddr/paas/$projectId/$imageName:$imageTag"
        }
    }

    private fun getWorkspace(pipelineId: String, vmSeqId: Int, poolNo: String): String {
        return "${dockerHostConfig.hostPathWorkspace}/$pipelineId/${getTailPath(vmSeqId, poolNo.toInt())}/"
    }

    private fun getTailPath(vmSeqId: Int, poolNo: Int): String {
        return if (poolNo > 1) {
            "$vmSeqId" + "_$poolNo"
        } else {
            vmSeqId.toString()
        }
    }

    private fun checkFinishTime(utcTime: String?): Boolean {
        if (utcTime != null && utcTime.isNotEmpty()) {
            val array = utcTime.split(".")
            val utcTimeLocal = array[0] + "Z"
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
            sdf.timeZone = TimeZone.getTimeZone("UTC")

            val date = sdf.parse(utcTimeLocal)
            val finishTimestamp = date.time
            val nowTimestamp = System.currentTimeMillis()
            return (nowTimestamp - finishTimestamp) > (30 * 60 * 1000)
        }

        return true
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
            return (nowTimestamp - startTimestamp) > (8 * 3600 * 1000)
        }

        return false
    }

    private fun getAvailableHostPort(): Int {
        val startPort = dockerHostConfig.startPort ?: 20000
        for (i in startPort..(startPort + 1000)) {
            if (!CommonUtils.isPortUsing("127.0.0.1", i)) {
                return i
            } else {
                continue
            }
        }

        return 0
    }

    inner class MyBuildImageResultCallback internal constructor(
        private val buildId: String,
        private val elementId: String?,
        private val dockerHostBuildApi: DockerHostBuildResourceApi
    ) : BuildImageResultCallback() {
        override fun onNext(item: BuildResponseItem?) {
            val text = item?.stream
            if (null != text) {
                dockerHostBuildApi.postLog(
                    buildId,
                    false,
                    StringUtils.removeEnd(text, "\n"),
                    elementId
                )
            }
            super.onNext(item)
        }
    }

    inner class MyPushImageResultCallback internal constructor(
        private val buildId: String,
        private val elementId: String?,
        private val dockerHostBuildApi: DockerHostBuildResourceApi
    ) : PushImageResultCallback() {
        private val totalList = mutableListOf<Long>()
        private val step = mutableMapOf<Int, Long>()
        override fun onNext(item: PushResponseItem?) {
            val text = item?.progressDetail
            if (null != text && text.current != null && text.total != null && text.total != 0L) {
                val lays = if (!totalList.contains(text.total!!)) {
                    totalList.add(text.total!!)
                    totalList.size + 1
                } else {
                    totalList.indexOf(text.total!!) + 1
                }
                var currentProgress = text.current!! * 100 / text.total!!
                if (currentProgress > 100) {
                    currentProgress = 100
                }
                if (currentProgress >= step[lays]?.plus(25) ?: 5) {
                    dockerHostBuildApi.postLog(
                        buildId,
                        false,
                        "正在推送镜像,第${lays}层，进度：$currentProgress%",
                        elementId
                    )
                    step[lays] = currentProgress
                }
            }
            super.onNext(item)
        }
    }

    inner class MyPullImageResultCallback internal constructor(
        private val buildId: String,
        private val dockerHostBuildApi: DockerHostBuildResourceApi,
        private val startTaskId: String?,
        private val containerHashId: String?
    ) : PullImageResultCallback() {
        private val totalList = mutableListOf<Long>()
        private val step = mutableMapOf<Int, Long>()
        override fun onNext(item: PullResponseItem?) {
            val text = item?.progressDetail
            if (null != text && text.current != null && text.total != null && text.total != 0L) {
                val lays = if (!totalList.contains(text.total!!)) {
                    totalList.add(text.total!!)
                    totalList.size + 1
                } else {
                    totalList.indexOf(text.total!!) + 1
                }
                var currentProgress = text.current!! * 100 / text.total!!
                if (currentProgress > 100) {
                    currentProgress = 100
                }

                if (currentProgress >= step[lays]?.plus(25) ?: 5) {
                    dockerHostBuildApi.postLog(
                        buildId = buildId,
                        red = false,
                        message = "正在拉取镜像,第${lays}层，进度：$currentProgress%",
                        tag = startTaskId,
                        jobId = containerHashId
                    )
                    step[lays] = currentProgress
                }
            }
            super.onNext(item)
        }
    }
}
