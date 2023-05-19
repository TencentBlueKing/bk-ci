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
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.exception.NotModifiedException
import com.github.dockerjava.api.exception.UnauthorizedException
import com.github.dockerjava.api.model.PullResponseItem
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.okhttp.OkDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.docker.pojo.DockerHostBuildInfo
import com.tencent.devops.dockerhost.common.ErrorCodeEnum
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.dispatch.DockerHostBuildResourceApi
import com.tencent.devops.dockerhost.exception.ContainerException
import com.tencent.devops.dockerhost.utils.CommonUtils
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import java.io.File
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory

abstract class AbstractDockerHostBuildService constructor(
    private val dockerHostConfig: DockerHostConfig,
    private val dockerHostBuildApi: DockerHostBuildResourceApi
) {

    private val logger = LoggerFactory.getLogger(AbstractDockerHostBuildService::class.java)

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

    abstract fun createContainer(dockerHostBuildInfo: DockerHostBuildInfo): String

    abstract fun stopContainer(dockerHostBuildInfo: DockerHostBuildInfo)

    fun stopContainer(containerId: String, buildId: String) {
        if (containerId.isEmpty()) {
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

    fun createPullImage(dockerBuildInfo: DockerHostBuildInfo) {
        val imageName = CommonUtils.normalizeImageName(dockerBuildInfo.imageName)
        val taskId = VMUtils.genStartVMTaskId(dockerBuildInfo.vmSeqId.toString())
        // docker pull
        if (dockerBuildInfo.imagePublicFlag == true &&
            dockerBuildInfo.imageRDType.equals(ImageRDTypeEnum.SELF_DEVELOPED.name, ignoreCase = true)) {
            log(
                buildId = dockerBuildInfo.buildId,
                message = "Public image, directly started from the local...",
                tag = taskId,
                containerHashId = dockerBuildInfo.containerHashId
            )
        } else {
            try {
                LocalImageCache.saveOrUpdate(imageName)
                createPullImage(
                    imageType = dockerBuildInfo.imageType,
                    imageName = dockerBuildInfo.imageName,
                    registryUser = dockerBuildInfo.registryUser,
                    registryPwd = dockerBuildInfo.registryPwd,
                    buildId = dockerBuildInfo.buildId,
                    containerId = dockerBuildInfo.vmSeqId.toString(),
                    containerHashId = dockerBuildInfo.containerHashId
                )
            } catch (t: UnauthorizedException) {
                val errorMessage = "No permission to pull image $imageName，Please check if the image path or " +
                    "credentials are correct. [buildId=${dockerBuildInfo.buildId}]" +
                    "[containerHashId=${dockerBuildInfo.containerHashId}]"
                logger.error(errorMessage, t)
                // 直接失败，禁止使用本地镜像
                throw ContainerException(
                    errorCodeEnum = ErrorCodeEnum.NO_AUTH_PULL_IMAGE_ERROR,
                    message = errorMessage
                )
            } catch (t: NotFoundException) {
                val errorMessage = "Image does not exist $imageName!!，" +
                    "Please check if the image path or credentials are correct." +
                        "[buildId=${dockerBuildInfo.buildId}][containerHashId=${dockerBuildInfo.containerHashId}]"
                logger.error(errorMessage, t)
                // 直接失败，禁止使用本地镜像
    /*            throw ContainerException(
                    errorCodeEnum = ErrorCodeEnum.IMAGE_NOT_EXIST_ERROR,
                    message = errorMessage
                )*/
            } catch (t: Throwable) {
                logger.warn("Fail to pull the image $imageName of build ${dockerBuildInfo.buildId}", t)
                log(
                    buildId = dockerBuildInfo.buildId,
                    message = "Failed to pull image：${t.message}",
                    tag = taskId,
                    containerHashId = dockerBuildInfo.containerHashId
                )
                log(
                    buildId = dockerBuildInfo.buildId,
                    message = "Trying to boot from a local image...",
                    tag = taskId,
                    containerHashId = dockerBuildInfo.containerHashId
                )
            }
        }
    }

    fun createPullImage(
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
        log(buildId, "Start pulling image $dockerImageName", taskId, containerHashId)
        httpLongDockerCli.pullImageCmd(dockerImageName).withAuthConfig(authConfig)
            .exec(MyPullImageResultCallback(buildId, dockerHostBuildApi, taskId, containerHashId)).awaitCompletion()
        log(buildId, "Pull the image successfully, ready to start the build environment...", taskId, containerHashId)
        return Result(true)
    }

    fun log(buildId: String, message: String, tag: String?, containerHashId: String?) {
        return log(buildId, false, message, tag, containerHashId)
    }

    fun log(
        buildId: String,
        red: Boolean,
        message: String,
        tag: String?,
        containerHashId: String?
    ) {
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

    fun afterOverlayFs(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: Int,
        poolNo: Int
    ) {
        try {
            val qpcGitProjectList = dockerHostBuildApi.getQpcGitProjectList(
                projectId = projectId,
                buildId = buildId,
                vmSeqId = vmSeqId.toString(),
                poolNo = poolNo
            )?.data

            // 针对overlayfs白名单项目清理工作空间
            if (qpcGitProjectList != null && qpcGitProjectList.isNotEmpty()) {
                val upperDir = "${dockerHostConfig.hostPathWorkspace}/$buildId/${getTailPath(vmSeqId, poolNo)}"
                FileUtils.deleteQuietly(File(upperDir))
            }
        } catch (e: Throwable) {
            logger.info("afterOverlayFs $buildId $vmSeqId $poolNo error: ${e.message}")
        }
    }

    fun getWorkspace(
        pipelineId: String,
        vmSeqId: Int,
        poolNo: Int,
        path: String
    ): String {
        return "$path/$pipelineId/${getTailPath(vmSeqId, poolNo)}/"
    }

    private fun getTailPath(vmSeqId: Int, poolNo: Int): String {
        return if (poolNo > 1) {
            "$vmSeqId" + "_$poolNo"
        } else {
            vmSeqId.toString()
        }
    }

    inner class MyPullImageResultCallback internal constructor(
        private val buildId: String,
        private val dockerHostBuildApi: DockerHostBuildResourceApi,
        private val startTaskId: String?,
        private val containerHashId: String?
    ) : ResultCallback.Adapter<PullResponseItem>() {
        private val totalList = mutableListOf<Long>()
        private val step = mutableMapOf<Int, Long>()
        override fun onNext(item: PullResponseItem?) {
            val text = item?.progressDetail
            if (text?.current != null && text?.total != null && text.total != 0L) {
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
                        buildId = buildId,
                        red = false,
                        message = "Pulling image, layer $lays，process：$currentProgress%",
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
