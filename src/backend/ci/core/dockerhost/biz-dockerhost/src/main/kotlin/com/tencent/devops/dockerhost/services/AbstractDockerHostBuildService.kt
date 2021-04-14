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
import com.github.dockerjava.api.exception.NotModifiedException
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.okhttp.OkDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import com.tencent.devops.dispatch.docker.pojo.DockerHostBuildInfo
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.dispatch.DockerHostBuildResourceApi
import org.slf4j.LoggerFactory

abstract class AbstractDockerHostBuildService constructor(
    dockerHostConfig: DockerHostConfig,
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
}
