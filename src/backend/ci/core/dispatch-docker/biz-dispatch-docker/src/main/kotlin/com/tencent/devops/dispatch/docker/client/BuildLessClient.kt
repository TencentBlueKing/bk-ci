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
import com.tencent.devops.buildless.pojo.BuildLessEndInfo
import com.tencent.devops.buildless.pojo.BuildLessStartInfo
import com.tencent.devops.buildless.pojo.RejectedExecutionType
import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.common.api.util.ApiUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.dispatch.docker.common.Constants
import com.tencent.devops.dispatch.docker.common.ErrorCodeEnum
import com.tencent.devops.dispatch.docker.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.docker.exception.DockerServiceException
import com.tencent.devops.dispatch.docker.pojo.enums.DockerHostClusterType
import com.tencent.devops.dispatch.docker.service.DockerHostProxyService
import com.tencent.devops.dispatch.docker.utils.DockerHostUtils
import com.tencent.devops.dispatch.docker.utils.RedisUtils
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.pojo.redis.RedisBuild
import com.tencent.devops.process.pojo.mq.PipelineBuildLessStartupEvent
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException

@Component
class BuildLessClient @Autowired constructor(
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val pipelineDockerIPInfoDao: PipelineDockerIPInfoDao,
    private val dockerHostUtils: DockerHostUtils,
    private val dslContext: DSLContext,
    private val dockerHostProxyService: DockerHostProxyService,
    private val redisUtils: RedisUtils,
    private val gray: Gray
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(BuildLessClient::class.java)
    }

    fun startBuildLess(
        agentLessDockerIp: String,
        agentLessDockerPort: Int,
        event: PipelineBuildLessStartupEvent
    ) {
        with(event) {
            val secretKey = ApiUtil.randomSecretKey()

            val id = pipelineDockerBuildDao.saveBuildHistory(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeqId = vmSeqId.toInt(),
                secretKey = secretKey,
                status = PipelineTaskStatus.RUNNING,
                zone = if (null == event.zone) {
                    Zone.SHENZHEN.name
                } else {
                    event.zone!!.name
                },
                dockerIp = agentLessDockerIp,
                poolNo = 0
            )

            val agentId = HashUtil.encodeLongId(id)
            redisUtils.setDockerBuild(
                id = id, secretKey = secretKey,
                redisBuild = RedisBuild(
                    vmName = agentId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    vmSeqId = vmSeqId,
                    channelCode = channelCode,
                    zone = zone,
                    atoms = atoms
                )
            )

            LOG.info("$buildId|$vmSeqId BUILD_LESS| secretKey: $secretKey")
            LOG.info("$buildId|$vmSeqId BUILD_LESS| agentId: $agentId")

            val buildLessStartInfo = BuildLessStartInfo(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeqId = Integer.valueOf(vmSeqId),
                executionCount = event.executeCount ?: 1,
                agentId = agentId,
                secretKey = secretKey,
                rejectedExecutionType = RejectedExecutionType.ABORT_POLICY
            )

            val enableIpCount = pipelineDockerIPInfoDao.getEnableDockerIpCount(
                dslContext = dslContext,
                grayEnv = gray.isGray(),
                clusterName = DockerHostClusterType.BUILD_LESS
            )
            startBuildLess(
                dockerIp = agentLessDockerIp,
                dockerHostPort = agentLessDockerPort,
                buildLessStartInfo = buildLessStartInfo,
                retryMax = (enableIpCount / 2).toInt()
            )
        }
    }

    fun endBuild(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: Int,
        containerId: String,
        dockerIp: String,
        clusterType: DockerHostClusterType = DockerHostClusterType.COMMON
    ) {
        val buildLessEndInfo = BuildLessEndInfo(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            poolNo = 0,
            containerId = containerId
        )

        val request = dockerHostProxyService.getDockerHostProxyRequest(
            dockerHostUri = Constants.BUILD_LESS_END_URI,
            dockerHostIp = dockerIp,
            clusterType = clusterType
        ).delete(
            RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                JsonUtil.toJson(buildLessEndInfo)
            )).build()

        OkhttpUtils.doHttp(request).use { resp ->
            val responseBody = resp.body!!.string()
            LOG.info("[$buildId|$vmSeqId|$dockerIp] End build less, response: $responseBody")
            val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            if (response["status"] == 0) {
                response["data"] as Boolean
            } else {
                val msg = response["message"] as String
                LOG.error("[$buildId|$vmSeqId|$dockerIp] End build less failed, msg: $msg")
                throw DockerServiceException(errorType = ErrorCodeEnum.END_VM_ERROR.errorType,
                    errorCode = ErrorCodeEnum.END_VM_ERROR.errorCode,
                    errorMsg = "End build less failed, msg: $msg")
            }
        }
    }

    private fun startBuildLess(
        dockerIp: String,
        dockerHostPort: Int,
        buildLessStartInfo: BuildLessStartInfo,
        retryTime: Int = 0,
        retryMax: Int = 0,
        unAvailableIpList: Set<String>? = null
    ) {
        val request = dockerHostProxyService.getDockerHostProxyRequest(
            dockerHostUri = Constants.BUILD_LESS_STARTUP_URI,
            dockerHostIp = dockerIp,
            dockerHostPort = dockerHostPort,
            clusterType = DockerHostClusterType.BUILD_LESS
        ).post(
            RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                JsonUtil.toJson(buildLessStartInfo)
            )
        ).build()

        val buildLogKey = "${buildLessStartInfo.buildId}|${buildLessStartInfo.vmSeqId}|$retryTime"
        LOG.info("Start buildLess|$buildLogKey|$dockerIp|${request.url}")
        try {
            OkhttpUtils.doHttp(request).use { resp ->
                if (resp.isSuccessful) {
                    val responseBody = resp.body!!.string()
                    val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
                    LOG.info("Response buildLess $buildLogKey status: ${response["status"]}")
                    dealWithResponse(
                        response = response,
                        buildLessStartInfo = buildLessStartInfo,
                        dockerIp = dockerIp,
                        retryTime = retryTime,
                        retryMax = retryMax,
                        unAvailableIpList = unAvailableIpList
                    )
                } else {
                    // 接口异常重试
                    doRetry(
                        retryTime = retryTime,
                        retryMax = retryMax,
                        dockerIp = dockerIp,
                        buildLessStartInfo = buildLessStartInfo,
                        errorMessage = resp.message,
                        unAvailableIpList = unAvailableIpList
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            // 对http连接超时重试
            if (e.message == "connect timed out") {
                doRetry(
                    retryTime = retryTime,
                    retryMax = retryMax,
                    dockerIp = dockerIp,
                    buildLessStartInfo = buildLessStartInfo,
                    errorMessage = e.message,
                    unAvailableIpList = unAvailableIpList
                )
            } else {
                // read timeout, 不重试直接失败
                doFail(
                    dockerIp = dockerIp,
                    event = buildLessStartInfo,
                    errorMessage = e.message ?: "SocketTimeoutException: read time out"
                )
            }
        } catch (e: NoRouteToHostException) {
            // 对Host unreachable场景重试
            doRetry(
                retryTime = retryTime,
                retryMax = retryMax,
                dockerIp = dockerIp,
                buildLessStartInfo = buildLessStartInfo,
                errorMessage = e.message,
                unAvailableIpList = unAvailableIpList
            )
        } catch (e: ConnectException) {
            doRetry(
                retryTime = retryTime,
                retryMax = retryMax,
                dockerIp = dockerIp,
                buildLessStartInfo = buildLessStartInfo,
                errorMessage = e.message,
                unAvailableIpList = unAvailableIpList
            )
        }
    }

    private fun dealWithResponse(
        response: Map<String, Any>,
        buildLessStartInfo: BuildLessStartInfo,
        dockerIp: String,
        retryTime: Int,
        retryMax: Int,
        unAvailableIpList: Set<String>?
    ) {
        when {
            response["status"] == 0 -> {
                LOG.info("Success buildLess ${buildLessStartInfo.buildId}|${buildLessStartInfo.vmSeqId}")
                pipelineDockerBuildDao.updateDockerIp(
                    dslContext = dslContext,
                    buildId = buildLessStartInfo.buildId,
                    vmSeqId = buildLessStartInfo.vmSeqId,
                    dockerIp = dockerIp
                )
            }
            // 母机无空闲容器资源
            response["status"] == 2127003 -> {
                doRetry(
                    retryTime = retryTime,
                    retryMax = retryMax,
                    dockerIp = dockerIp,
                    buildLessStartInfo = buildLessStartInfo,
                    errorMessage = response["message"] as String,
                    unAvailableIpList = unAvailableIpList
                )
            }
            else -> {
                // 非可重试异常码，不重试直接失败
                doFail(
                    dockerIp = dockerIp,
                    event = buildLessStartInfo,
                    errorMessage = response["message"] as String
                )
            }
        }
    }

    private fun doRetry(
        retryTime: Int,
        retryMax: Int,
        dockerIp: String,
        buildLessStartInfo: BuildLessStartInfo,
        errorMessage: String?,
        unAvailableIpList: Set<String>?
    ) {
        val buildLog = "${buildLessStartInfo.buildId}|${buildLessStartInfo.vmSeqId}|$retryTime"
        if (retryTime < retryMax) {
            LOG.warn("$buildLog start build less failed in $dockerIp, retry. error: $errorMessage")
            val unAvailableIpListLocal: Set<String> = unAvailableIpList?.plus(dockerIp) ?: setOf(dockerIp)
            val retryTimeLocal = retryTime + 1
            // 过滤重试前异常IP, 并重新获取可用ip
            val dockerIpLocalPair = dockerHostUtils.getAvailableDockerIpWithSpecialIps(
                projectId = buildLessStartInfo.projectId,
                pipelineId = buildLessStartInfo.pipelineId,
                vmSeqId = buildLessStartInfo.vmSeqId.toString(),
                specialIpSet = emptySet(),
                unAvailableIpList = unAvailableIpListLocal,
                clusterName = DockerHostClusterType.BUILD_LESS
            )
            startBuildLess(
                dockerIp = dockerIpLocalPair.first,
                dockerHostPort = dockerIpLocalPair.second,
                buildLessStartInfo = buildLessStartInfo,
                retryTime = retryTimeLocal,
                unAvailableIpList = unAvailableIpListLocal
            )
        } else {
            LOG.warn("$$buildLog reached retry limit, switch FOLLOW policy.")
            // 清空之前不可以记录, 并重新获取可用ip，强制调用
            val dockerIpLocalPair = dockerHostUtils.getAvailableDockerIpWithSpecialIps(
                projectId = buildLessStartInfo.projectId,
                pipelineId = buildLessStartInfo.pipelineId,
                vmSeqId = buildLessStartInfo.vmSeqId.toString(),
                specialIpSet = emptySet(),
                unAvailableIpList = emptySet(),
                clusterName = DockerHostClusterType.BUILD_LESS
            )
            startBuildLess(
                dockerIp = dockerIpLocalPair.first,
                dockerHostPort = dockerIpLocalPair.second,
                buildLessStartInfo = buildLessStartInfo
                    .copy(rejectedExecutionType = RejectedExecutionType.FOLLOW_POLICY),
                retryTime = retryTime,
                unAvailableIpList = emptySet()
            )
        }
    }

    private fun doFail(
        dockerIp: String,
        event: BuildLessStartInfo,
        errorMessage: String
    ) {
        // 当前IP此刻不可用，将IP状态置为false
        pipelineDockerIPInfoDao.updateDockerIpStatus(dslContext, dockerIp, false)

        LOG.error("${event.buildId}|${event.vmSeqId}| Start build less failed," +
                " message: $errorMessage")
        throw DockerServiceException(
            errorType = ErrorCodeEnum.START_VM_FAIL.errorType,
            errorCode = ErrorCodeEnum.START_VM_FAIL.errorCode,
            errorMsg = "Start build less failed, msg: $errorMessage."
        )
    }
}
