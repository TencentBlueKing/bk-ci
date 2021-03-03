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

package com.tencent.devops.dispatch.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.common.api.util.ApiUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.common.Constants
import com.tencent.devops.dispatch.common.ErrorCodeEnum
import com.tencent.devops.dispatch.config.DefaultImageConfig
import com.tencent.devops.dispatch.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.dao.PipelineDockerTaskSimpleDao
import com.tencent.devops.dispatch.exception.DockerServiceException
import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.pojo.redis.RedisBuild
import com.tencent.devops.dispatch.utils.CommonUtils
import com.tencent.devops.dispatch.utils.DockerHostUtils
import com.tencent.devops.dispatch.utils.redis.RedisUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import com.tencent.devops.ticket.pojo.enums.CredentialType
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.SocketTimeoutException

@Component
class DockerHostClient @Autowired constructor(
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val pipelineDockerIPInfoDao: PipelineDockerIPInfoDao,
    private val pipelineDockerTaskSimpleDao: PipelineDockerTaskSimpleDao,
    private val dockerHostUtils: DockerHostUtils,
    private val redisUtils: RedisUtils,
    private val redisOperation: RedisOperation,
    private val client: Client,
    private val dslContext: DSLContext,
    private val defaultImageConfig: DefaultImageConfig
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(DockerHostClient::class.java)
    }

    fun startBuild(
        event: PipelineAgentStartupEvent,
        dockerIp: String,
        dockerHostPort: Int,
        poolNo: Int,
        driftIpInfo: String
    ) {
        val secretKey = ApiUtil.randomSecretKey()
        val id = pipelineDockerBuildDao.startBuild(
            dslContext = dslContext,
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            buildId = event.buildId,
            vmSeqId = event.vmSeqId.toInt(),
            secretKey = secretKey,
            status = PipelineTaskStatus.RUNNING,
            zone = if (null == event.zone) {
                Zone.SHENZHEN.name
            } else {
                event.zone!!.name
            },
            dockerIp = dockerIp,
            poolNo = poolNo
        )
        val agentId = HashUtil.encodeLongId(id)
        redisUtils.setDockerBuild(
            id, secretKey,
            RedisBuild(
                vmName = agentId,
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                vmSeqId = event.vmSeqId,
                channelCode = event.channelCode,
                zone = event.zone,
                atoms = event.atoms
            )
        )

        val dispatchType = event.dispatchType as DockerDispatchType
        val dockerImage = if (dispatchType.imageType == ImageType.THIRD) {
            dispatchType.dockerBuildVersion
        } else {
            when (dispatchType.dockerBuildVersion) {
                DockerVersion.TLINUX1_2.value -> {
                    defaultImageConfig.getTLinux1_2CompleteUri()
                }
                DockerVersion.TLINUX2_2.value -> {
                    defaultImageConfig.getTLinux2_2CompleteUri()
                }
                else -> {
                    defaultImageConfig.getCompleteUriByImageName(dispatchType.dockerBuildVersion)
                }
            }
        }

        LOG.info("${event.buildId}|dockerHostBuild|$agentId|$dockerImage|${dispatchType.imageCode}|" +
            "${dispatchType.imageVersion}|${dispatchType.credentialId}|${dispatchType.credentialProject}")
        var userName: String? = null
        var password: String? = null
        if (dispatchType.imageType == ImageType.THIRD) {
            if (!dispatchType.credentialId.isNullOrBlank()) {
                val projectId = if (dispatchType.credentialProject.isNullOrBlank()) {
                    event.projectId
                } else {
                    dispatchType.credentialProject!!
                }
                val ticketsMap = CommonUtils.getCredential(
                    client = client,
                    projectId = projectId,
                    credentialId = dispatchType.credentialId!!,
                    type = CredentialType.USERNAME_PASSWORD
                )
                userName = ticketsMap["v1"] as String
                password = ticketsMap["v2"] as String
            }
        }

        val requestBody = DockerHostBuildInfo(
            projectId = event.projectId,
            agentId = agentId,
            pipelineId = event.pipelineId,
            buildId = event.buildId,
            vmSeqId = Integer.valueOf(event.vmSeqId),
            secretKey = secretKey,
            status = PipelineTaskStatus.RUNNING.status,
            imageName = dockerImage!!,
            containerId = "",
            wsInHost = true,
            poolNo = poolNo,
            registryUser = userName ?: "",
            registryPwd = password ?: "",
            imageType = dispatchType.imageType?.type,
            imagePublicFlag = dispatchType.imagePublicFlag,
            imageRDType = if (dispatchType.imageRDType == null) {
                null
            } else {
                ImageRDTypeEnum.getImageRDTypeByName(dispatchType.imageRDType!!).name
            },
            containerHashId = event.containerHashId
        )

        pipelineDockerTaskSimpleDao.createOrUpdate(
            dslContext = dslContext,
            pipelineId = event.pipelineId,
            vmSeq = event.vmSeqId,
            dockerIp = dockerIp
        )

        // 准备开始构建，增加缓存计数，限流用
        redisOperation.increment("${Constants.DOCKER_IP_COUNT_KEY_PREFIX}$dockerIp", 1)

        dockerBuildStart(dockerIp, dockerHostPort, requestBody, event, driftIpInfo)
    }

    fun endBuild(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: Int,
        containerId: String,
        dockerIp: String
    ) {
        val requestBody = DockerHostBuildInfo(
            projectId = projectId,
            agentId = "",
            pipelineId = pipelineId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            secretKey = "",
            status = 0,
            imageName = "",
            containerId = containerId,
            wsInHost = true,
            poolNo = 0,
            registryUser = "",
            registryPwd = "",
            imageType = "",
            imagePublicFlag = false,
            imageRDType = null,
            containerHashId = ""
        )

        val proxyUrl = dockerHostUtils.getIdc2DevnetProxyUrl("/api/docker/build/end", dockerIp)
        val request = Request.Builder().url(proxyUrl)
            .delete(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    JsonUtil.toJson(requestBody)
                )
            )
            .addHeader("Accept", "application/json; charset=utf-8")
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .build()

        OkhttpUtils.doHttp(request).use { resp ->
            val responseBody = resp.body()!!.string()
            LOG.info("[$projectId|$pipelineId|$buildId] End build Docker VM $dockerIp responseBody: $responseBody")
            val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            if (response["status"] == 0) {
                response["data"] as Boolean
            } else {
                val msg = response["message"] as String
                LOG.error("[$projectId|$pipelineId|$buildId] End build Docker VM failed, msg: $msg")
                throw DockerServiceException(
                    errorType = ErrorType.SYSTEM,
                    errorCode = ErrorCodeEnum.END_VM_ERROR.errorCode,
                    errorMsg = "End build Docker VM failed, msg: $msg")
            }
        }
    }

    private fun dockerBuildStart(
        dockerIp: String,
        dockerHostPort: Int,
        requestBody: DockerHostBuildInfo,
        event: PipelineAgentStartupEvent,
        driftIpInfo: String,
        retryTime: Int = 0,
        unAvailableIpList: Set<String>? = null
    ) {
        val proxyUrl = dockerHostUtils.getIdc2DevnetProxyUrl("/api/docker/build/start", dockerIp, dockerHostPort)
        val request = Request.Builder().url(proxyUrl)
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JsonUtil.toJson(requestBody)))
            .addHeader("Accept", "application/json; charset=utf-8")
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .build()

        LOG.info("dockerStart|${event.projectId}|${event.pipelineId}|${event.buildId}|$retryTime|$dockerIp|$proxyUrl")
        try {
            OkhttpUtils.doLongHttp(request).use { resp ->
                if (resp.isSuccessful) {
                    val responseBody = resp.body()!!.string()
                    LOG.info("${event.buildId}|$retryTime Start build Docker VM $dockerIp responseBody: $responseBody")
                    val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
                    when {
                        response["status"] == 0 -> {
                            val containerId = response["data"] as String
                            // 更新task状态以及构建历史记录，并记录漂移日志
                            dockerHostUtils.updateTaskSimpleAndRecordDriftLog(
                                pipelineAgentStartupEvent = event,
                                containerId = containerId,
                                newIp = dockerIp,
                                driftIpInfo = driftIpInfo
                            )
                        }
                        response["status"] == 2 -> {
                            // 业务逻辑异常重试
                            doRetry(event = event,
                                retryTime = retryTime,
                                dockerIp = dockerIp,
                                requestBody = requestBody,
                                driftIpInfo = driftIpInfo,
                                errorMessage = resp.message(),
                                unAvailableIpList = unAvailableIpList)
                        }
                        else -> {
                            val msg = response["message"] as String
                            LOG.error("[${event.projectId}|${event.pipelineId}|${event.buildId}|$retryTime] " +
                                "Start build Docker VM failed, msg: $msg")
                            throw DockerServiceException(errorType = ErrorType.SYSTEM,
                                errorCode = ErrorCodeEnum.START_VM_FAIL.errorCode,
                                errorMsg = "Start build Docker VM failed, msg: $msg")
                        }
                    }
                } else {
                    // 服务异常重试
                    LOG.info("[${event.projectId}|${event.pipelineId}|${event.buildId}|$retryTime] " +
                        "dockerBuildStart response failed and do retry. resp: $resp")
                    doRetry(event, retryTime, dockerIp, requestBody, driftIpInfo, resp.message(), unAvailableIpList)
                }
            }
        } catch (e: SocketTimeoutException) {
            // 超时重试
            if (e.message == "timeout") {
                LOG.info("[${event.projectId}|${event.pipelineId}|${event.buildId}|$retryTime] " +
                    "dockerBuildStart error and do retry.", e)
                doRetry(event, retryTime, dockerIp, requestBody, driftIpInfo, e.message, unAvailableIpList)
            } else {
                LOG.error("[${event.projectId}|${event.pipelineId}|${event.buildId}|$retryTime] " +
                    "Start build Docker VM failed, msg: ${e.message}")
                throw DockerServiceException(errorType = ErrorType.SYSTEM,
                    errorCode = ErrorCodeEnum.START_VM_FAIL.errorCode,
                    errorMsg = "Start build Docker VM failed, msg: ${e.message}")
            }
        }
    }

    private fun doRetry(
        event: PipelineAgentStartupEvent,
        retryTime: Int,
        dockerIp: String,
        requestBody: DockerHostBuildInfo,
        driftIpInfo: String,
        errorMessage: String?,
        unAvailableIpList: Set<String>?
    ) {
        if (retryTime < 3) {
            val unAvailableIpListLocal: Set<String> = unAvailableIpList?.plus(dockerIp) ?: setOf(dockerIp)
            val retryTimeLocal = retryTime + 1
            // 当前IP不可用，保险起见将当前ip可用性置为false，并重新获取可用ip
            pipelineDockerIPInfoDao.updateDockerIpStatus(dslContext, dockerIp, false)
            val dockerIpLocalPair = dockerHostUtils.getAvailableDockerIp(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                vmSeqId = event.vmSeqId,
                unAvailableIpList = unAvailableIpListLocal
            )
            dockerBuildStart(
                dockerIp = dockerIpLocalPair.first,
                dockerHostPort = dockerIpLocalPair.second,
                requestBody = requestBody,
                event = event,
                driftIpInfo = driftIpInfo,
                retryTime = retryTimeLocal,
                unAvailableIpList = unAvailableIpListLocal
            )
        } else {
            LOG.error("[${event.projectId}|${event.pipelineId}|${event.buildId}|$retryTime] " +
                "Start build Docker VM failed, retry $retryTime times. message: $errorMessage")
            throw DockerServiceException(ErrorType.SYSTEM,
                ErrorCodeEnum.RETRY_START_VM_FAIL.errorCode,
                "Start build Docker VM failed, retry $retryTime times.")
        }
    }
}
