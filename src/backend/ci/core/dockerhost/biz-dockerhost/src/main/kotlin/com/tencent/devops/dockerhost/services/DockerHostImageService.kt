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

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.okhttp.OkDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.pojo.DockerBuildParam
import com.tencent.devops.dockerhost.services.image.ImageBuildHandler
import com.tencent.devops.dockerhost.services.image.ImageDeleteHandler
import com.tencent.devops.dockerhost.services.image.ImageHandlerContext
import com.tencent.devops.dockerhost.services.image.ImagePushHandler
import com.tencent.devops.dockerhost.services.image.ImageScanHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class DockerHostImageService(
    private val dockerHostConfig: DockerHostConfig,
/*    private val dockerHostBuildApi: DockerHostBuildResourceApi,
    private val dockerHostImageScanService: DockerHostImageScanService,*/
    private val imageBuildHandler: ImageBuildHandler,
    private val imageScanHandler: ImageScanHandler,
    private val imagePushHandler: ImagePushHandler,
    private val imageDeleteHandler: ImageDeleteHandler
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DockerHostImageService::class.java)
    }

    fun dockerBuildAndPushImage(
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        dockerBuildParam: DockerBuildParam,
        buildId: String,
        elementId: String?,
        outer: Boolean,
        scanFlag: Boolean
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

            val imageHandlerContext = ImageHandlerContext(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                userName = userName,
                dockerBuildParam = dockerBuildParam,
                dockerClient = dockerClient,
                pipelineTaskId = elementId,
                outer = outer,
                scanFlag = scanFlag
            )

            // 同步扫描
            imageBuildHandler.setNextHandler(
                imageScanHandler.setNextHandler(
                    imagePushHandler.setNextHandler(imageDeleteHandler)
                )
            ).handlerRequest(imageHandlerContext)

            return Pair(true, null)
        } catch (e: Exception) {
            logger.error("Docker build and push failed, exception: ", e)
            return Pair(false, e.message)
        } finally {
            try {
                dockerClient.close()
            } catch (e: IOException) {
                logger.error("docker client close exception: ${e.message}")
            }
        }

    }
/*
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
            ticket.forEach {
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

            val imageNameTagSet = mutableSetOf<String>()
            if (dockerBuildParam.imageTagList.isNotEmpty()) {
                dockerBuildParam.imageTagList.forEach {
                    imageNameTagSet.add(getImageNameWithTag(
                        repoAddr = repoAddr,
                        projectId = projectId,
                        imageName = dockerBuildParam.imageName,
                        imageTag = it,
                        outer = outer
                    ))
                }
            } else {
                imageNameTagSet.add(getImageNameWithTag(
                    repoAddr = repoAddr,
                    projectId = projectId,
                    imageName = dockerBuildParam.imageName,
                    imageTag = dockerBuildParam.imageTag,
                    outer = outer
                ))
            }

            logger.info("[$buildId]|[$vmSeqId] Build docker image, workspace: $workspace, buildDir:$buildDir, dockerfile: $dockerfilePath")
            logger.info("[$buildId]|[$vmSeqId] Build docker image, imageNameTag: $imageNameTagSet")
            val step = dockerClient.buildImageCmd().withNoCache(true)
                .withPull(true)
                .withBuildAuthConfigs(authConfigurations)
                .withBaseDirectory(baseDirectory)
                .withDockerfile(dockerfile)
                .withTags(imageNameTagSet)
            args.map { it.trim().split("=") }.forEach {
                step.withBuildArg(it.first(), it.last())
            }
            step.exec(MyBuildImageResultCallback(buildId, elementId, dockerHostBuildApi))
                .awaitImageId()

            // 镜像扫描
            dockerHostImageScanService.scanningDocker(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                imageTagSet = imageNameTagSet,
                dockerClient = dockerClient
            )

            imageNameTagSet.parallelStream().forEach {
                logger.info("[$buildId]|[$vmSeqId] Build image success, now push to repo, image name and tag: $it")
                dockerClient.pushImageCmd(it)
                    .withAuthConfig(authConfig)
                    .exec(MyPushImageResultCallback(buildId, elementId, dockerHostBuildApi))
                    .awaitCompletion()

                logger.info("[$buildId]|[$vmSeqId] Push image success, now remove local image, image name and tag: $it")
                try {
                    dockerClient.removeImageCmd(it).exec()
                    logger.info("[$buildId]|[$vmSeqId] Remove local image success")
                } catch (e: Throwable) {
                    logger.error("[$buildId]|[$vmSeqId] Docker rmi failed, msg: ${e.message}")
                }
            }

            return Pair(true, null)
        } catch (e: Throwable) {
            logger.error("Docker build and push failed, exception: ", e)
            val cause = if (e.cause != null && e.cause!!.message != null) {
                e.cause!!.message!!.removePrefix(getWorkspace(pipelineId = pipelineId,
                    vmSeqId = vmSeqId.toInt(),
                    poolNo = dockerBuildParam.poolNo ?: "0")
                )
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
    ) : ResultCallback.Adapter<PushResponseItem>() {
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
                if (currentProgress >= (step[lays]?.plus(25) ?: 5)) {
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
    }*/
}
