/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.github.dockerjava.core.DockerClientImpl
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
            val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerConfig(dockerHostConfig.dockerConfig)
                .withApiVersion(dockerHostConfig.apiVersion)
                .build()

            val longHttpClient: DockerHttpClient = OkDockerHttpClient.Builder()
                .dockerHost(config.dockerHost)
                .sslConfig(config.sslConfig)
                .connectTimeout(5000)
                .readTimeout(300000)
                .build()

            dockerClient = DockerClientImpl.getInstance(config, longHttpClient)

            val imageHandlerContext = ImageHandlerContext(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeqId = vmSeqId.toInt(),
                poolNo = dockerBuildParam.poolNo!!.toInt(),
                userName = dockerBuildParam.userId,
                dockerBuildParam = dockerBuildParam,
                dockerClient = dockerClient,
                pipelineTaskId = elementId,
                outer = outer,
                scanFlag = scanFlag
            )

            imageBuildHandler.setNextHandler(
                imageScanHandler.setNextHandler(
                    imagePushHandler.setNextHandler(imageDeleteHandler)
                )
            ).handlerRequest(imageHandlerContext)

            return Pair(true, imageHandlerContext.result)
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
}
