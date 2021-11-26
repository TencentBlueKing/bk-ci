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
import com.tencent.devops.common.api.util.script.CommandLineUtils
import com.tencent.devops.dispatch.docker.pojo.DockerHostBuildInfo
import com.tencent.devops.dockerhost.common.ErrorCodeEnum
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.dispatch.DockerHostBuildResourceApi
import com.tencent.devops.dockerhost.exception.ContainerException
import com.tencent.devops.dockerhost.utils.CommonUtils
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import org.slf4j.LoggerFactory
import java.io.File

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
                message = "自研公共镜像，不从仓库拉取，直接从本地启动...",
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
                val errorMessage = "无权限拉取镜像：$imageName，请检查镜像路径或凭证是否正确；" +
                        "[buildId=${dockerBuildInfo.buildId}][containerHashId=${dockerBuildInfo.containerHashId}]"
                logger.error(errorMessage, t)
                // 直接失败，禁止使用本地镜像
                throw ContainerException(
                    errorCodeEnum = ErrorCodeEnum.NO_AUTH_PULL_IMAGE_ERROR,
                    message = errorMessage
                )
            } catch (t: NotFoundException) {
                val errorMessage = "镜像不存在：$imageName，请检查镜像路径或凭证是否正确；" +
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
        log(buildId, "开始拉取镜像，镜像名称：$dockerImageName", taskId, containerHashId)
        httpLongDockerCli.pullImageCmd(dockerImageName).withAuthConfig(authConfig)
            .exec(MyPullImageResultCallback(buildId, dockerHostBuildApi, taskId, containerHashId)).awaitCompletion()
        log(buildId, "拉取镜像成功，准备启动构建环境...", taskId, containerHashId)
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

    fun getWorkspace(
        pipelineId: String,
        vmSeqId: Int,
        poolNo: Int,
        path: String
    ): String {
        return "$path/$pipelineId/${getTailPath(vmSeqId, poolNo)}/"
    }

/*    fun mountOverlayfs(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: Int,
        poolNo: Int,
        hostConfig: HostConfig
    ) {
        val qpcGitProjectList = dockerHostBuildApi.getQpcGitProjectList(
            projectId = projectId,
            buildId = buildId,
            vmSeqId = vmSeqId.toString(),
            poolNo = poolNo
        )?.data

        var qpcUniquePath = ""
        if (qpcGitProjectList != null && qpcGitProjectList.isNotEmpty()) {
            qpcUniquePath = qpcGitProjectList.first()
        }

        mountOverlayfs(pipelineId, vmSeqId, poolNo, qpcUniquePath, hostConfig)
    }

    fun mountOverlayfs(
        pipelineId: String,
        vmSeqId: Int,
        poolNo: Int,
        qpcUniquePath: String?,
        hostConfig: HostConfig
    ) {
        if (qpcUniquePath != null && qpcUniquePath.isNotBlank()) {
            val upperDir = "${getWorkspace(pipelineId, vmSeqId, poolNo, dockerHostConfig.hostPathWorkspace!!)}upper"
            val workDir = "${getWorkspace(pipelineId, vmSeqId, poolNo, dockerHostConfig.hostPathWorkspace!!)}work"
            val lowerDir = "${dockerHostConfig.hostPathOverlayfsCache}/$qpcUniquePath"

            if (!File(upperDir).exists()) {
                File(upperDir).mkdirs()
            }

            if (!File(workDir).exists()) {
                File(workDir).mkdirs()
            }

            if (!File(lowerDir).exists()) {
                File(lowerDir).mkdirs()
            }

            val mount = Mount().withType(MountType.VOLUME)
                .withTarget(dockerHostConfig.volumeWorkspace)
                .withVolumeOptions(
                    VolumeOptions().withDriverConfig(
                        Driver().withName("local").withOptions(
                            mapOf(
                                "type" to "overlay",
                                "device" to "overlay",
                                "o" to "lowerdir=$lowerDir,upperdir=$upperDir,workdir=$workDir"
                            )
                        )
                    )
                )

            hostConfig.withMounts(listOf(mount))
        }
    }

    fun mountBazelOverlayfs(
        pipelineId: String,
        vmSeqId: Int,
        poolNo: Int,
        hostConfig: HostConfig
    ) {
        val upperDir = "${getWorkspace(pipelineId, vmSeqId, poolNo, dockerHostConfig.bazelUpperPath!!)}upper"
        val workDir = "${getWorkspace(pipelineId, vmSeqId, poolNo, dockerHostConfig.bazelUpperPath!!)}work"
        val lowerDir = "${dockerHostConfig.bazelLowerPath}"

        if (!File(upperDir).exists()) {
            File(upperDir).mkdirs()
        }

        if (!File(workDir).exists()) {
            File(workDir).mkdirs()
        }

        if (!File(lowerDir).exists()) {
            File(lowerDir).mkdirs()
        }

        val mount = Mount().withType(MountType.VOLUME)
            .withTarget(dockerHostConfig.bazelContainerPath)
            .withVolumeOptions(
                VolumeOptions().withDriverConfig(
                    Driver().withName("local").withOptions(
                        mapOf(
                            "type" to "overlay",
                            "device" to "overlay",
                            "o" to "lowerdir=$lowerDir,upperdir=$upperDir,workdir=$workDir"
                        )
                    )
                )
            )

        hostConfig.withMounts(listOf(mount))
    }*/

    fun reWriteBazelCache(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: Int,
        poolNo: Int
    ) {
        // 出现错误也不影响执行
        try {
            val qpcGitProjectList = dockerHostBuildApi.getQpcGitProjectList(
                projectId = projectId,
                buildId = buildId,
                vmSeqId = vmSeqId.toString(),
                poolNo = poolNo
            )?.data

            // 针对白名单项目做bazel cache处理
            if (qpcGitProjectList != null && qpcGitProjectList.isNotEmpty()) {
                val upperDir = "${getWorkspace(pipelineId, vmSeqId, poolNo, dockerHostConfig.bazelUpperPath!!)}upper"
                CommandLineUtils.execute(
                    command = "time flock -xn ${dockerHostConfig.bazelLowerPath}  " +
                            "rsync --stats -ah --ignore-errors --include=\"cache/\" " +
                            "--include=\"install/\" --exclude=\"*/\" " +
                            " $upperDir/ ${dockerHostConfig.bazelLowerPath}/",
                    workspace = File(dockerHostConfig.bazelLowerPath!!),
                    print2Logger = true
                )
            }
        } catch (e: Throwable) {
            logger.info("reWriteBazelCache $pipelineId $vmSeqId $poolNo error: ${e.message}")
        }
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
