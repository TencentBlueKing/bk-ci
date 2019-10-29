package com.tencent.devops.dockerhost.service

import com.github.dockerjava.api.model.AccessMode
import com.github.dockerjava.api.model.AuthConfig
import com.github.dockerjava.api.model.AuthConfigurations
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.BuildResponseItem
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PullResponseItem
import com.github.dockerjava.api.model.PushResponseItem
import com.github.dockerjava.api.model.Volume
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.dispatch.DockerHostBuildResourceApi
import com.tencent.devops.dockerhost.pojo.DockerBuildParamNew
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Paths
import com.github.dockerjava.core.command.BuildImageResultCallback
import com.github.dockerjava.core.command.LogContainerResultCallback
import com.github.dockerjava.core.command.PullImageResultCallback
import com.github.dockerjava.core.command.PushImageResultCallback
import com.github.dockerjava.core.command.WaitContainerResultCallback
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.dockerhost.config.TXDockerHostConfig
import com.tencent.devops.dockerhost.dispatch.AlertApi
import com.tencent.devops.dockerhost.exception.ContainerException
import com.tencent.devops.dockerhost.pojo.DockerRunParam
import com.tencent.devops.dockerhost.services.DockerHostBuildService
import com.tencent.devops.dockerhost.utils.BK_DISTCC_LOCAL_IP
import com.tencent.devops.dockerhost.utils.CommonUtils
import com.tencent.devops.dockerhost.utils.ENV_DOCKER_HOST_IP
import com.tencent.devops.dockerhost.utils.ENV_KEY_GATEWAY
import com.tencent.devops.dockerhost.utils.ENV_KEY_PROJECT_ID
import com.tencent.devops.dockerhost.utils.TXCommonUtils
import org.apache.commons.lang3.StringUtils
import java.util.ArrayList

@Component
class TXDockerHostBuildService(
    private val dockerHostConfig: TXDockerHostConfig,
    private val buildService: DockerHostBuildService
) {

    private val logger = LoggerFactory.getLogger(TXDockerHostBuildService::class.java)
    private val dockerHostBuildApi: DockerHostBuildResourceApi = DockerHostBuildResourceApi()
    private val alertApi: AlertApi = AlertApi()
    private val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerHost(dockerHostConfig.dockerHost)
        .withDockerConfig(dockerHostConfig.dockerConfig)
        .withApiVersion(dockerHostConfig.apiVersion)
        .build()
    private val dockerCli = DockerClientBuilder.getInstance(config).build()

    fun dockerRun(
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String,
        dockerRunParam: DockerRunParam
    ): Pair<String, Int> {
        try {
            val authConfig = TXCommonUtils.getAuthConfig(
                ImageType.THIRD.type,
                dockerHostConfig,
                dockerRunParam.imageName,
                dockerRunParam.registryUser,
                dockerRunParam.registryPwd
            )
            val imageName = TXCommonUtils.normalizeImageName(dockerRunParam.imageName)
            // docker pull
            try {
                buildService.log(buildId, "开始拉取镜像，镜像名称：$imageName")
                dockerCli.pullImageCmd(imageName).withAuthConfig(authConfig)
                    .exec(MyPullImageResultCallback(buildId, dockerHostBuildApi)).awaitCompletion()
                buildService.log(buildId, "拉取镜像成功，准备执行命令...")
            } catch (t: Throwable) {
                logger.warn("Fail to pull the image $imageName of build $buildId", t)
                buildService.log(buildId, "拉取镜像失败，错误信息：${t.message}")
                buildService.log(buildId, "尝试使用本地镜像执行命令...")
            }
            // docker run
            val volumeWs = Volume(dockerHostConfig.volumeWorkspace)
            val volumeProjectShare = Volume(dockerHostConfig.volumeProjectShare)
            val volumeMavenRepo = Volume(dockerHostConfig.volumeMavenRepo)
            val volumeNpmPrefix = Volume(dockerHostConfig.volumeNpmPrefix)
            val volumeNpmCache = Volume(dockerHostConfig.volumeNpmCache)
            val volumeCcache = Volume(dockerHostConfig.volumeCcache)
            val volumeApps = Volume(dockerHostConfig.volumeApps)
            val volumeInit = Volume(dockerHostConfig.volumeInit)
            val volumeLogs = Volume(dockerHostConfig.volumeLogs)
            val volumeGradleCache = Volume(dockerHostConfig.volumeGradleCache)

            val gateway = System.getProperty("soda.gateway", "gw.open.oa.com")
            logger.info("gateway is: $gateway")

            val binds = mutableListOf(
                Bind("${dockerHostConfig.hostPathMavenRepo}/$pipelineId/$vmSeqId/", volumeMavenRepo),
                Bind("${dockerHostConfig.hostPathNpmPrefix}/$pipelineId/$vmSeqId/", volumeNpmPrefix),
                Bind("${dockerHostConfig.hostPathNpmCache}/$pipelineId/$vmSeqId/", volumeNpmCache),
                Bind("${dockerHostConfig.hostPathCcache}/$pipelineId/$vmSeqId/", volumeCcache),
                Bind(dockerHostConfig.hostPathApps, volumeApps, AccessMode.ro),
                Bind(dockerHostConfig.hostPathInit, volumeInit, AccessMode.ro),
                Bind("${dockerHostConfig.hostPathLogs}/$buildId/$vmSeqId/", volumeLogs),
                Bind("${dockerHostConfig.hostPathGradleCache}/$pipelineId/$vmSeqId/", volumeGradleCache),
                Bind(getWorkspace(pipelineId, vmSeqId.toInt()), volumeWs)
            )
            if (enableProjectShare(projectId)) {
                binds.add(Bind(getProjectShareDir(projectId), volumeProjectShare))
            }

            val env = mutableListOf<String>()
            env.addAll(
                listOf(
                    "$ENV_KEY_PROJECT_ID=$projectId",
                    "$ENV_KEY_GATEWAY=$gateway",
//                    "TERM=xterm-256color",
                    "landun_env=${dockerHostConfig.landunEnv ?: "prod"}",
                    "$ENV_DOCKER_HOST_IP=${CommonUtils.getInnerIP()}",
                    "$BK_DISTCC_LOCAL_IP=${CommonUtils.getInnerIP()}"
                )
            )
            if (dockerRunParam.env != null && dockerRunParam.env!!.isNotEmpty()) {
                dockerRunParam.env!!.forEach {
                    env.add(it.key + "=" + (it.value ?: ""))
                }
            }
            logger.info("env is $env")
            val container = dockerCli.createContainerCmd(imageName)
                .withCmd(dockerRunParam.command)
                .withEnv(env)
                .withVolumes(volumeWs).withVolumes(volumeApps).withVolumes(volumeInit)
                .withHostConfig(HostConfig().withBinds(binds).withNetworkMode("bridge"))
                .withWorkingDir(dockerHostConfig.volumeWorkspace)
                .exec()

            logger.info("Created container $container")
            val timestamp = (System.currentTimeMillis() / 1000).toInt()
            dockerCli.startContainerCmd(container.id).exec()

            return Pair(container.id, timestamp)
        } catch (er: Throwable) {
            logger.error(er.toString())
            logger.error(er.cause.toString())
            logger.error(er.message)
            buildService.log(buildId, true, "启动容器失败，错误信息:${er.message}")
            alertApi.alert(
                AlertLevel.HIGH.name, "Docker构建机创建容器失败", "Docker构建机创建容器失败, " +
                    "母机IP:${CommonUtils.getInnerIP()}， 失败信息：${er.message}"
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
        val dockerCli = TXCommonUtils.getDockerDefaultClient(dockerHostConfig)
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
    fun dockerBuildAndPushImageNew(
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        dockerBuildParam: DockerBuildParamNew,
        buildId: String
    ): Pair<Boolean, String?> {
        try {
            val repoAddr = dockerBuildParam.repoAddr
            val userName = dockerBuildParam.userName
            val password = dockerBuildParam.password
            val ticket = dockerBuildParam.ticket
            val args = dockerBuildParam.args
            val host = dockerBuildParam.host
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
                getImageNameWithTagNew(repoAddr, projectId, dockerBuildParam.imageName, dockerBuildParam.imageTag)

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
            step.exec(MyBuildImageResultCallback(buildId, "", dockerHostBuildApi))
                .awaitImageId()

            logger.info("Build image success, now push to repo, image name and tag: $imageNameTag")
            dockerCli.pushImageCmd(imageNameTag)
                .withAuthConfig(authConfig)
                .exec(MyPushImageResultCallback(buildId, "", dockerHostBuildApi))
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

    fun getDockerLogs(containerId: String, lastLogTime: Int): List<String> {
        val logs = ArrayList<String>()
        val dockerCli = TXCommonUtils.getDockerDefaultClient(dockerHostConfig)
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
        val dockerCli = TXCommonUtils.getDockerDefaultClient(dockerHostConfig)
        return dockerCli.waitContainerCmd(containerId)
            .exec(WaitContainerResultCallback())
            .awaitStatusCode()
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

    private fun getWorkspace(pipelineId: String, vmSeqId: Int): String {
        return "${dockerHostConfig.hostPathWorkspace}/$pipelineId/$vmSeqId/"
    }

    private fun getImageNameWithTagNew(repoAddr: String, projectId: String, imageName: String, imageTag: String): String {
        return "$repoAddr/$imageName:$imageTag"
    }

    private fun getImageNameWithTag(repoAddr: String, projectId: String, imageName: String, imageTag: String): String {
        return "$repoAddr/paas/$projectId/$imageName:$imageTag"
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