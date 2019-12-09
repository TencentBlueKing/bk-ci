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

import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.AuthConfig
import com.github.dockerjava.api.model.AuthConfigurations
import com.github.dockerjava.api.model.BuildResponseItem
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PullResponseItem
import com.github.dockerjava.api.model.PushResponseItem
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.BuildImageResultCallback
import com.github.dockerjava.core.command.LogContainerResultCallback
import com.github.dockerjava.core.command.PullImageResultCallback
import com.github.dockerjava.core.command.PushImageResultCallback
import com.github.dockerjava.core.command.WaitContainerResultCallback
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
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
import com.tencent.devops.dockerhost.utils.CommonUtils
import com.tencent.devops.dockerhost.utils.ENTRY_POINT_CMD
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Paths
import java.util.Date
import javax.annotation.PostConstruct

@Component
class DockerHostBuildService(
    private val dockerHostConfig: DockerHostConfig
) {

    private val logger = LoggerFactory.getLogger(DockerHostBuildService::class.java)

    private val dockerHostBuildApi: DockerHostBuildResourceApi =
        DockerHostBuildResourceApi(if ("codecc_build" == dockerHostConfig.runMode) "ms/dispatch-codecc" else "ms/dispatch")

    private val alertApi: AlertApi =
        AlertApi(if ("codecc_build" == dockerHostConfig.runMode) "ms/dispatch-codecc" else "ms/dispatch")

    private val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerConfig(dockerHostConfig.dockerConfig)
        .withApiVersion(dockerHostConfig.apiVersion)
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

    fun pullImage(
        imageType: String?,
        imageName: String,
        registryUser: String?,
        registryPwd: String?,
        buildId: String
    ): Result<Boolean> {
        val authConfig = CommonUtils.getAuthConfig(
            imageType = imageType,
            dockerHostConfig = dockerHostConfig,
            imageName = imageName,
            registryUser = registryUser,
            registryPwd = registryPwd
        )
        val dockerImageName = CommonUtils.normalizeImageName(imageName)
        log(buildId, "开始拉取镜像，镜像名称：$dockerImageName")
        dockerCli.pullImageCmd(dockerImageName).withAuthConfig(authConfig)
            .exec(MyPullImageResultCallback(buildId, dockerHostBuildApi)).awaitCompletion()
        log(buildId, "拉取镜像成功，准备启动构建环境...")
        return Result(true)
    }

    fun checkImage(
        buildId: String,
        checkImageRequest: CheckImageRequest
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
                buildId = buildId
            )
            logger.info("pullImageResult: $pullImageResult")
            if (pullImageResult.isNotOk()) {
                return Result(pullImageResult.status, pullImageResult.message, null)
            }
        } catch (t: Throwable) {
            logger.warn("Fail to pull the image $imageName of build $buildId", t)
            log(buildId, "pull image fail，error is：${t.message}")
        }
        val dockerImageName = CommonUtils.normalizeImageName(checkImageRequest.imageName)
        // 查询镜像详细信息
        val imageInfo = dockerCli.inspectImageCmd(dockerImageName).exec()
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
            // docker pull
            try {
                LocalImageCache.saveOrUpdate(imageName)
                pullImage(
                    imageType = dockerBuildInfo.imageType,
                    imageName = dockerBuildInfo.imageName,
                    registryUser = dockerBuildInfo.registryUser,
                    registryPwd = dockerBuildInfo.registryPwd,
                    buildId = dockerBuildInfo.buildId
                )
            } catch (t: Throwable) {
                logger.warn("Fail to pull the image $imageName of build ${dockerBuildInfo.buildId}", t)
                log(dockerBuildInfo.buildId, "拉取镜像失败，错误信息：${t.message}")
                log(dockerBuildInfo.buildId, "尝试使用本地镜像启动...")
            }
            // docker run
            val binds = DockerBindLoader.loadBinds(dockerBuildInfo)

            val container = dockerCli.createContainerCmd(imageName)
                .withCmd("/bin/sh", ENTRY_POINT_CMD)
                .withEnv(DockerEnvLoader.loadEnv(dockerBuildInfo))
                .withVolumes(DockerVolumeLoader.loadVolumes(dockerBuildInfo))
                .withHostConfig(HostConfig().withBinds(binds).withNetworkMode("bridge"))
                .exec()

            logger.info("Created container $container")
            dockerCli.startContainerCmd(container.id).exec()

            return container.id
        } catch (er: Throwable) {
            logger.error(er.toString())
            logger.error(er.cause.toString())
            logger.error(er.message)
            log(dockerBuildInfo.buildId, true, "启动构建环境失败，错误信息:${er.message}")
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
            val containerInfo = dockerCli.inspectContainerCmd(dockerBuildInfo.containerId).exec()
            if ("exited" != containerInfo.state.status) {
                dockerCli.stopContainerCmd(dockerBuildInfo.containerId).withTimeout(30).exec()
            }
        } catch (e: Throwable) {
            logger.error("Stop the container failed, containerId: ${dockerBuildInfo.containerId}, error msg: $e")
        }

        try {
            // docker rm
            dockerCli.removeContainerCmd(dockerBuildInfo.containerId).exec()
        } catch (e: Throwable) {
            logger.error("Stop the container failed, containerId: ${dockerBuildInfo.containerId}, error msg: $e")
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
        dockerBuildParam: DockerBuildParam,
        buildId: String,
        elementId: String?,
        outer: Boolean
    ): Pair<Boolean, String?> {
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

            val dockerCli = DockerClientBuilder.getInstance(config).build()
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

            val workspace = getWorkspace(pipelineId, vmSeqId.toInt())
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
            val step = dockerCli.buildImageCmd().withNoCache(true)
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
            dockerCli.pushImageCmd(imageNameTag)
                .withAuthConfig(authConfig)
                .exec(MyPushImageResultCallback(buildId, elementId, dockerHostBuildApi))
                .awaitCompletion()

            logger.info("Push image success, now remove local image, image name and tag: $imageNameTag")

            try {
                dockerCli.removeImageCmd(
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
                e.cause!!.message!!.removePrefix(getWorkspace(pipelineId, vmSeqId.toInt()))
            } else {
                ""
            }
            return Pair(false, e.message + if (cause.isBlank()) "" else " cause:【$cause】")
        }
    }

    fun dockerRun(
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String,
        dockerRunParam: DockerRunParam
    ): Pair<String, Int> {
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
                    buildId = buildId
                )
            } catch (t: Throwable) {
                logger.warn("Fail to pull the image $imageName of build $buildId", t)
                log(buildId, "拉取镜像失败，错误信息：${t.message}")
                log(buildId, "尝试使用本地镜像执行命令...")
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
                registryUser = dockerRunParam.registryUser,
                registryPwd = dockerRunParam.registryPwd,
                imageType = ImageType.THIRD.type
            )
            // docker run
            val env = mutableListOf<String>()
            env.addAll(DockerEnvLoader.loadEnv(dockerBuildInfo))
            dockerRunParam.env?.forEach {
                env.add("${it.key}=${it.value ?: ""}")
            }
            logger.info("env is $env")
            val binds = DockerBindLoader.loadBinds(dockerBuildInfo)

            val container = dockerCli.createContainerCmd(imageName)
                .withCmd(dockerRunParam.command)
                .withEnv(env)
                .withVolumes(DockerVolumeLoader.loadVolumes(dockerBuildInfo))
                .withHostConfig(HostConfig().withBinds(binds).withNetworkMode("bridge"))
                .withWorkingDir(dockerHostConfig.volumeWorkspace)
                .exec()

            logger.info("Created container $container")
            val timestamp = (System.currentTimeMillis() / 1000).toInt()
            dockerCli.startContainerCmd(container.id).exec()

            return Pair(container.id, timestamp)
        } catch (er: Throwable) {
            val errorLog = "[$buildId]|启动容器失败，错误信息:${er.message}"
            logger.error(errorLog, er)
            log(buildId, true, errorLog)
            alertApi.alert(
                level = AlertLevel.HIGH.name, title = "Docker构建机创建容器失败",
                message = "Docker构建机创建容器失败, 母机IP:${CommonUtils.getInnerIP()}， 失败信息：${er.message}"
            )
            throw ContainerException("启动容器失败，错误信息:${er.message}")
        } finally {
            if (!dockerRunParam.registryUser.isNullOrEmpty()) {
                try {
                    dockerCli.removeImageCmd(dockerRunParam.imageName)
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
            val containerInfo = dockerCli.inspectContainerCmd(containerId).exec()
            if ("exited" != containerInfo.state.status) {
                dockerCli.stopContainerCmd(containerId).withTimeout(30).exec()
            }
        } catch (e: Throwable) {
            logger.error("Stop the container failed, containerId: $containerId, error msg: $e")
        }

        try {
            // docker rm
            dockerCli.removeContainerCmd(containerId).exec()
        } catch (e: Throwable) {
            logger.error("Stop the container failed, containerId: $containerId, error msg: $e")
        }
    }

    fun getDockerLogs(containerId: String, lastLogTime: Int): List<String> {
        val logs = ArrayList<String>()
        val logContainerCmd = dockerCli.logContainerCmd(containerId)
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

    fun getDockerRunExitCode(containerId: String): Int {
        return dockerCli.waitContainerCmd(containerId)
            .exec(WaitContainerResultCallback())
            .awaitStatusCode()
    }

    fun clearContainers() {
        val containerInfo = dockerCli.listContainersCmd().withStatusFilter(setOf("exited")).exec()
        for (container in containerInfo) {
            logger.info("Clear container, containerId: ${container.id}")
            dockerCli.removeContainerCmd(container.id).exec()
        }
    }

    @PostConstruct
    fun loadLocalImages() {
        try {
            val imageList = dockerCli.listImagesCmd().withShowAll(true).exec()
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
        val danglingImages = dockerCli.listImagesCmd().withDanglingFilter(true).withShowAll(true).exec()
        danglingImages.forEach {
            try {
                dockerCli.removeImageCmd(it.id).exec()
                logger.info("remove local dangling image success, image id: ${it.id}")
            } catch (e: java.lang.Exception) {
                logger.error("remove local dangling image exception ${e.message}")
            }
        }

        val imageList = dockerCli.listImagesCmd().withShowAll(true).exec()
        imageList.forEach c@{
            if (it.repoTags == null || it.repoTags.isEmpty()) {
                return@c
            }
            it.repoTags.forEach { image ->
                val lastUsedDate = LocalImageCache.getDate(image)
                if (null != lastUsedDate) {
                    if ((Date().time - lastUsedDate.time) / (1000 * 60 * 60 * 24) >= dockerHostConfig.localImageCacheDays) {
                        logger.info("remove local image, ${it.repoTags}")
                        try {
                            dockerCli.removeImageCmd(image).exec()
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
        val inspectContainerResponse = dockerCli.inspectContainerCmd(containerId).exec() ?: return false
        return inspectContainerResponse.state.running ?: false
    }

    fun log(buildId: String, message: String) {
        return log(buildId, false, message)
    }

    fun log(buildId: String, red: Boolean, message: String) {
        logger.info("write log to dispatch, buildId: $buildId, message: $message")
        try {
            dockerHostBuildApi.postLog(buildId, red, message, null)
        } catch (e: Exception) {
            logger.info("write log to dispatch failed")
        }
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

    private fun getWorkspace(pipelineId: String, vmSeqId: Int): String {
        return "${dockerHostConfig.hostPathWorkspace}/$pipelineId/$vmSeqId/"
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
        private val dockerHostBuildApi: DockerHostBuildResourceApi
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
                        buildId,
                        false,
                        "正在拉取镜像,第${lays}层，进度：$currentProgress%"
                    )
                    step[lays] = currentProgress
                }
            }
            super.onNext(item)
        }
    }
}
