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

package com.tencent.devops.buildless.client

import com.tencent.devops.buildless.config.BuildLessConfig
import com.tencent.devops.buildless.pojo.BuildLessTask
import com.tencent.devops.buildless.utils.CommonUtils
import com.tencent.devops.buildless.utils.SystemInfoUtil
import com.tencent.devops.common.api.auth.AUTH_HEADER_GATEWAY_TAG
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.security.util.EnvironmentUtil
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.dispatch.docker.pojo.DockerIpInfoVO
import com.tencent.devops.dispatch.docker.pojo.enums.DockerHostClusterType
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DispatchClient @Autowired constructor(
    private val buildLessConfig: BuildLessConfig,
    private val commonConfig: CommonConfig,
    private val bkTag: BkTag
) {
    fun updateContainerId(buildLessTask: BuildLessTask, containerId: String) {
        val path = "/ms/dispatch-docker/api/service/dockerhost/builds/${buildLessTask.buildId}/vmseqs" +
                "/${buildLessTask.vmSeqId}?containerId=$containerId"

        try {
            val url = buildUrl(path)
            val request = Request
                .Builder()
                .url(url)
                .headers(makeHeaders())
                .put(
                    RequestBody.create(
                        "application/json; charset=utf-8".toMediaTypeOrNull(),
                        ""
                    )
                )
                .build()

            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.error("Update containerId $path fail. $responseContent")
                    throw TaskExecuteException(
                        errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR,
                        errorType = ErrorType.SYSTEM,
                        errorMsg = "Update containerId $path fail"
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Update containerId failed. errorInfo: ${e.message}")
        }
    }

    fun refreshStatus(containerRunningsCount: Int) {
        val dockerIp = CommonUtils.getHostIp()
        val path = "/ms/dispatch-docker/api/service/dockerhost/dockerIp/$dockerIp/refresh"
        // 节点状态默认正常
        var enable = true

        // 容器为0时 节点可能异常，告警然后设置enable=false
        if (containerRunningsCount <= 0) {
            enable = false
            logger.warn("Node: $dockerIp no running containers in containerPool.")
        }

        val dockerIpInfoVO = DockerIpInfoVO(
            id = 0L,
            dockerIp = dockerIp,
            dockerHostPort = commonConfig.serverPort,
            capacity = 100,
            usedNum = containerRunningsCount,
            averageCpuLoad = SystemInfoUtil.getAverageCpuLoad(),
            averageMemLoad = SystemInfoUtil.getAverageMemLoad(),
            averageDiskLoad = SystemInfoUtil.getAverageDiskLoad(),
            averageDiskIOLoad = SystemInfoUtil.getAverageDiskIOLoad(),
            enable = enable,
            grayEnv = isGray(),
            specialOn = null,
            createTime = null,
            clusterType = DockerHostClusterType.BUILD_LESS
        )

        try {
            val url = buildUrl(path)
            val request = Request
                .Builder()
                .url(url)
                .headers(makeHeaders())
                .post(
                    RequestBody.create(
                        "application/json; charset=utf-8".toMediaTypeOrNull(),
                        JsonUtil.toJson(dockerIpInfoVO)
                    )
                )
                .build()

            logger.info("Start refresh buildLess status $url")
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.error("Refresh buildLess status $url fail. $responseContent")
                    throw TaskExecuteException(
                        errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR,
                        errorType = ErrorType.SYSTEM,
                        errorMsg = "Refresh buildLess status $url fail"
                    )
                }
                logger.info("End refreshDockerIpStatus.")
            }
        } catch (e: Exception) {
            logger.error("Refresh buildLess status failed. errorInfo: ${e.message}")
        }
    }

    fun isGray(): Boolean {
        return bkTag.getLocalTag().contains("gray")
    }

    private fun buildUrl(path: String): String {
        return if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            fixUrl(commonConfig.devopsIdcGateway!!, path)
        }
    }

    private fun fixUrl(server: String, path: String): String {
        return if (server.startsWith("http://") || server.startsWith("https://")) {
            "$server/${path.removePrefix("/")}"
        } else {
            "http://$server/${path.removePrefix("/")}"
        }
    }

    private fun makeHeaders(): Headers {
        val gatewayHeaderTag = if (buildLessConfig.gatewayHeaderTag == null) {
            bkTag.getLocalTag()
        } else {
            buildLessConfig.gatewayHeaderTag
        } ?: ""
        val headers = mutableMapOf(AUTH_HEADER_GATEWAY_TAG to gatewayHeaderTag)
        // 新增devopsToken给网关校验
        val devopsToken = EnvironmentUtil.gatewayDevopsToken()
        if (devopsToken != null) {
            headers["X-DEVOPS-TOKEN"] = devopsToken
        }
        return headers.toHeaders()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DispatchClient::class.java)
    }
}
