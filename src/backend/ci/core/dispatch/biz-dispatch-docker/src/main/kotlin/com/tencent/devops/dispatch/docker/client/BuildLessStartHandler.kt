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

package com.tencent.devops.dispatch.docker.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.buildless.pojo.BuildLessStartInfo
import com.tencent.devops.buildless.pojo.RejectedExecutionType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.dispatch.docker.client.context.BuildLessStartHandlerContext
import com.tencent.devops.dispatch.docker.common.Constants
import com.tencent.devops.dispatch.docker.common.ErrorCodeEnum
import com.tencent.devops.dispatch.docker.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.docker.exception.DockerServiceException
import com.tencent.devops.dispatch.docker.exception.NoAvailableHostException
import com.tencent.devops.dispatch.docker.pojo.enums.DockerHostClusterType
import com.tencent.devops.dispatch.docker.service.DockerHostProxyService
import com.tencent.devops.dispatch.docker.utils.DockerHostUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException

@Service
class BuildLessStartHandler @Autowired constructor(
    private val dslContext: DSLContext,
    private val dockerHostUtils: DockerHostUtils,
    private val dockerHostProxyService: DockerHostProxyService,
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val pipelineDockerIPInfoDao: PipelineDockerIPInfoDao
) : Handler<BuildLessStartHandlerContext>() {
    private val logger = LoggerFactory.getLogger(BuildLessStartHandler::class.java)

    @Suppress("NestedBlockDepth")
    override fun handlerRequest(handlerContext: BuildLessStartHandlerContext) {
        with(handlerContext) {
            val buildLessStartInfo = BuildLessStartInfo(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                vmSeqId = Integer.valueOf(event.vmSeqId),
                executionCount = event.executeCount ?: 1,
                agentId = agentId,
                secretKey = secretKey,
                rejectedExecutionType = rejectedExecutionType
            )
            val request = dockerHostProxyService.getDockerHostProxyRequest(
                dockerHostUri = Constants.BUILD_LESS_STARTUP_URI,
                dockerHostIp = buildLessHost,
                dockerHostPort = buildLessPort,
                clusterType = DockerHostClusterType.BUILD_LESS
            ).post(
                JsonUtil.toJson(buildLessStartInfo)
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            ).build()

            logger.info("$buildLogKey start buildLess $buildLessHost, ${request.url}")

            try {
                OkhttpUtils.doHttp(request).use { resp ->
                    if (resp.isSuccessful) {
                        val response: Map<String, Any> = jacksonObjectMapper().readValue(resp.body!!.string())
                        logger.info("$buildLogKey response: ${JsonUtil.toJson(response)}")
                        dealWithResponse(response, this)
                    } else {
                        // 接口异常重试
                        doRetry(resp.message, this)
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is SocketTimeoutException -> handleSocketTimeoutException(e, this)
                    is NoRouteToHostException, is ConnectException -> doRetry(e.message, this)
                }
            }
        }
    }

    fun handleSocketTimeoutException(
        e: SocketTimeoutException,
        handlerContext: BuildLessStartHandlerContext
    ) {
        with(handlerContext) {
            if (e.message == "connect timed out") {
                doRetry(e.message, this)
            } else {
                doFail(this, e.message ?: "SocketTimeoutException: read time out")
            }
        }
    }

    private fun dealWithResponse(
        response: Map<String, Any>,
        handlerContext: BuildLessStartHandlerContext
    ) {
        with(handlerContext) {
            when {
                response["status"] == 0 -> {
                    pipelineDockerBuildDao.updateDockerIp(
                        dslContext = dslContext,
                        buildId = event.buildId,
                        vmSeqId = Integer.valueOf(event.vmSeqId),
                        dockerIp = buildLessHost
                    )
                }
                // 母机无空闲容器资源
                response["status"] == 2127003 -> {
                    doRetry(response["message"] as String, this)
                }
                else -> {
                    // 非可重试异常码，不重试直接失败
                    doFail(this, response["message"] as String)
                }
            }
        }
    }

    private fun doRetry(
        errorMessage: String?,
        handlerContext: BuildLessStartHandlerContext
    ) {
        with(handlerContext) {
            if (retryTime < retryMaxTime) {
                logger.info("$buildLogKey start build less failed in $buildLessHost, retry. error: $errorMessage")
                val unAvailableIpListLocal: Set<String> = unAvailableIpList?.plus(buildLessHost) ?: setOf(buildLessHost)
                try {
                    // 过滤重试前异常IP, 并重新获取可用ip
                    val (newBuildLessHost, newBuildLessPort) = dockerHostUtils.getAvailableDockerIpWithSpecialIps(
                        projectId = event.projectId,
                        pipelineId = event.pipelineId,
                        vmSeqId = event.vmSeqId,
                        specialIpSet = emptySet(),
                        unAvailableIpList = unAvailableIpListLocal,
                        clusterName = DockerHostClusterType.BUILD_LESS
                    )

                    this.retryTime = retryTime + 1
                    this.unAvailableIpList = unAvailableIpList?.plus(buildLessHost) ?: setOf(buildLessHost)
                    this.buildLessHost = newBuildLessHost
                    this.buildLessPort = newBuildLessPort

                    handlerRequest(this)
                } catch (e: NoAvailableHostException) {
                    // 无可用节点时，主动强制下发
                    handlerContext.retryTime = retryMaxTime
                    doRetry(errorMessage, this)
                }
            } else if (retryTime == retryMaxTime) {
                logger.info("$$buildLogKey reached retry limit, switch FOLLOW policy.")
                // 清空之前不可以记录, 并重新获取可用ip，强制调用
                val (newBuildLessHost, newBuildLessPort) = dockerHostUtils.getAvailableDockerIpWithSpecialIps(
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    vmSeqId = event.vmSeqId,
                    specialIpSet = emptySet(),
                    unAvailableIpList = emptySet(),
                    clusterName = DockerHostClusterType.BUILD_LESS
                )

                this.retryTime = retryTime + 1
                this.unAvailableIpList = emptySet()
                this.buildLessHost = newBuildLessHost
                this.buildLessPort = newBuildLessPort
                this.rejectedExecutionType = RejectedExecutionType.FOLLOW_POLICY

                handlerRequest(this)
            } else {
                logger.error("$buildLogKey reached retry limit, FOLLOW policy still failed.")
                doFail(this, "Reached retry limit, FOLLOW policy still failed.")
            }
        }
    }

    private fun doFail(
        handlerContext: BuildLessStartHandlerContext,
        errorMessage: String
    ) {
        with(handlerContext) {
            // 当前IP此刻不可用，将IP状态置为false
            pipelineDockerIPInfoDao.updateDockerIpStatus(dslContext, buildLessHost, false)

            logger.error("$buildLogKey Start build less failed, message: $errorMessage")
            throw DockerServiceException(
                errorType = ErrorCodeEnum.START_VM_FAIL.errorType,
                errorCode = ErrorCodeEnum.START_VM_FAIL.errorCode,
                errorMsg = "Start build less failed, msg: $errorMessage."
            )
        }
    }
}
