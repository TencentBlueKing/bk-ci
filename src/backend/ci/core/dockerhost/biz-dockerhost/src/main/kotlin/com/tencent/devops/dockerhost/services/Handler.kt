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
import com.github.dockerjava.api.model.PullResponseItem
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.okhttp.OkDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import com.tencent.devops.common.api.constant.BK_PULLING_IMAGE
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.dispatch.DockerHostBuildResourceApi
import org.slf4j.LoggerFactory

abstract class Handler<T : HandlerContext> constructor(
    private val dockerHostConfig: DockerHostConfig,
    private val dockerHostBuildApi: DockerHostBuildResourceApi
) {
    protected var nextHandler = ThreadLocal<Handler<T>?>()

    private val logger = LoggerFactory.getLogger(Handler::class.java)

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

    abstract fun handlerRequest(handlerContext: T)

    fun setNextHandler(handler: Handler<T>): Handler<T> {
        this.nextHandler.set(handler)
        return this
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
                        message = I18nUtil.getCodeLanMessage(
                            messageCode = BK_PULLING_IMAGE,
                            params = arrayOf("$lays", "$currentProgress")
                        ),
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
