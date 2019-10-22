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

package com.tencent.devops.dockerhost.dispatch

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import com.tencent.devops.dispatch.pojo.DockerHostInfo
import org.slf4j.LoggerFactory

class DockerHostBuildResourceApi : AbstractBuildResourceApi() {
    private val logger = LoggerFactory.getLogger(DockerHostBuildResourceApi::class.java)


    // 定时任务使用，现在停用
    fun startBuild(hostTag: String): Result<DockerHostBuildInfo>? {
        val path = "/ms/dispatch/api/dockerhost/startBuild?hostTag=$hostTag"
        val request = buildPost(path)
//        val httpClient = okHttpClient.newBuilder().build()

//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("DockerHostBuildResourceApi $path fail. $responseContent")
                throw RuntimeException("DockerHostBuildResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }

    // 定时任务和dispatch触发都用到，启动成功后上报到dispatch
    fun reportContainerId(buildId: String, vmSeqId: Int, containerId: String): Result<Boolean>? {
        val path = "/ms/dispatch/api/dockerhost/containerId?buildId=$buildId&vmSeqId=$vmSeqId&containerId=$containerId"
        val request = buildPost(path)
//        val httpClient = okHttpClient.newBuilder().build()

//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("AgentThirdPartyAgentResourceApi $path fail. $responseContent")
                throw RuntimeException("AgentThirdPartyAgentResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }

    // 定时任务使用，现在停用
    fun endBuild(hostTag: String): Result<DockerHostBuildInfo>? {
        val path = "/ms/dispatch/api/dockerhost/endBuild?hostTag=$hostTag"
        val request = buildPost(path)
//        val httpClient = okHttpClient.newBuilder().build()

//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("DockerHostBuildResourceApi $path fail. $responseContent")
                throw RuntimeException("DockerHostBuildResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }


    // 不再rollback,，现在停用
    fun rollbackBuild(buildId: String, vmSeqId: Int, shutdown: Boolean): Result<Boolean>? {
        val path = "/ms/dispatch/api/dockerhost/rollbackBuild?buildId=$buildId&vmSeqId=$vmSeqId&shutdown=$shutdown"
        val request = buildPost(path)
//        val httpClient = okHttpClient.newBuilder().build()

//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("DockerHostBuildResourceApi $path fail. $responseContent")
                throw RuntimeException("DockerHostBuildResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }

    // 上报日志
    fun log(buildId: String, red: Boolean, message: String): Result<Boolean>? {
        val path = "/ms/dispatch/api/dockerhost/log?buildId=$buildId&red=$red&message=$message"
        val request = buildPost(path)
//        val httpClient = okHttpClient.newBuilder().build()

//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("DockerHostBuildResourceApi $path fail. $responseContent")
                throw RuntimeException("DockerHostBuildResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }
}
