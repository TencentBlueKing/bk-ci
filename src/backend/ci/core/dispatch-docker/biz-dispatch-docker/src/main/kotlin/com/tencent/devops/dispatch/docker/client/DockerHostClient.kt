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

package com.tencent.devops.dispatch.docker.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.common.api.util.ApiUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.docker.common.Constants
import com.tencent.devops.dispatch.docker.common.ErrorCodeEnum
import com.tencent.devops.dispatch.docker.config.DefaultImageConfig
import com.tencent.devops.dispatch.docker.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerTaskSimpleDao
import com.tencent.devops.dispatch.docker.exception.DockerServiceException
import com.tencent.devops.dispatch.docker.pojo.DockerHostBuildInfo
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.pojo.redis.RedisBuild
import com.tencent.devops.dispatch.docker.utils.CommonUtils
import com.tencent.devops.dispatch.docker.utils.DockerHostUtils
import com.tencent.devops.dispatch.utils.redis.RedisUtils
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
        private val logger = LoggerFactory.getLogger(DockerHostClient::class.java)
    }

    fun startBuild(
        dispatchMessage: DispatchMessage,
        dockerIp: String,
        dockerHostPort: Int,
        poolNo: Int,
        driftIpInfo: String
    ) {
        val secretKey = ApiUtil.randomSecretKey()
        val id = pipelineDockerBuildDao.startBuild(
            dslContext = dslContext,
            projectId = dispatchMessage.projectId,
            pipelineId = dispatchMessage.pipelineId,
            buildId = dispatchMessage.buildId,
            vmSeqId = dispatchMessage.vmSeqId.toInt(),
            secretKey = secretKey,
            status = PipelineTaskStatus.RUNNING,
            zone = if (null == dispatchMessage.zone) {
                Zone.SHENZHEN.name
            } else {
                dispatchMessage.zone!!.name
            },
            dockerIp = dockerIp,
            poolNo = poolNo
        )
        val agentId = HashUtil.encodeLongId(id)
        redisUtils.setDockerBuild(
            id, secretKey,
            RedisBuild(
                vmName = agentId,
                projectId = dispatchMessage.projectId,
                pipelineId = dispatchMessage.pipelineId,
                buildId = dispatchMessage.buildId,
                vmSeqId = dispatchMessage.vmSeqId,
                channelCode = dispatchMessage.channelCode,
                zone = dispatchMessage.zone,
                atoms = dispatchMessage.atoms
            )
        )
        logger.info("secretKey: $secretKey")
        logger.info("agentId: $agentId")
        val dispatchType = dispatchMessage.dispatchType as DockerDispatchType
        logger.info("dockerHostBuild:(${dispatchMessage.userId},${dispatchMessage.projectId},${dispatchMessage.pipelineId},${dispatchMessage.buildId},${dispatchType.imageType?.name},${dispatchType.imageCode},${dispatchType.imageVersion},${dispatchType.credentialId},${dispatchType.credentialProject})")

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
        logger.info("Docker images is: $dockerImage")
        var userName: String? = null
        var password: String? = null
        if (dispatchType.imageType == ImageType.THIRD) {
            if (!dispatchType.credentialId.isNullOrBlank()) {
                val projectId = if (dispatchType.credentialProject.isNullOrBlank()) {
                    logger.warn("dockerHostBuild:credentialProject=nullOrBlank,buildId=${dispatchMessage.buildId},credentialId=${dispatchType.credentialId}")
                    dispatchMessage.projectId
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
            projectId = dispatchMessage.projectId,
            agentId = agentId,
            pipelineId = dispatchMessage.pipelineId,
            buildId = dispatchMessage.buildId,
            vmSeqId = Integer.valueOf(dispatchMessage.vmSeqId),
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
            containerHashId = dispatchMessage.containerHashId
        )

        pipelineDockerTaskSimpleDao.createOrUpdate(
            dslContext = dslContext,
            pipelineId = dispatchMessage.pipelineId,
            vmSeq = dispatchMessage.vmSeqId,
            dockerIp = dockerIp
        )

        // 准备开始构建，增加缓存计数，限流用
        redisOperation.increment("${Constants.DOCKER_IP_COUNT_KEY_PREFIX}$dockerIp", 1)

        dockerBuildStart(dockerIp, dockerHostPort, requestBody, dispatchMessage, driftIpInfo)
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
            logger.info("[$projectId|$pipelineId|$buildId] End build Docker VM $dockerIp responseBody: $responseBody")
            val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            if (response["status"] == 0) {
                response["data"] as Boolean
            } else {
                val msg = response["message"] as String
                logger.error("[$projectId|$pipelineId|$buildId] End build Docker VM failed, msg: $msg")
                throw DockerServiceException(ErrorType.SYSTEM, ErrorCodeEnum.END_VM_ERROR.errorCode, "End build Docker VM failed, msg: $msg")
            }
        }
    }

    private fun dockerBuildStart(
        dockerIp: String,
        dockerHostPort: Int,
        requestBody: DockerHostBuildInfo,
        dispatchMessage: DispatchMessage,
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

        logger.info("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|${dispatchMessage.buildId}|$retryTime] Start build Docker VM $dockerIp, url: $proxyUrl, requestBody: $requestBody")
        try {
            OkhttpUtils.doLongHttp(request).use { resp ->
                if (resp.isSuccessful) {
                    val responseBody = resp.body()!!.string()
                    logger.info("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|${dispatchMessage.buildId}|$retryTime] Start build Docker VM $dockerIp responseBody: $responseBody")
                    val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
                    when {
                        response["status"] == 0 -> {
                            val containerId = response["data"] as String
                            logger.info("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|${dispatchMessage.buildId}|$retryTime] update container: $containerId")
                            // 更新task状态以及构建历史记录，并记录漂移日志
                            dockerHostUtils.updateTaskSimpleAndRecordDriftLog(
                                dispatchMessage = dispatchMessage,
                                containerId = containerId,
                                newIp = dockerIp,
                                driftIpInfo = driftIpInfo
                            )
                        }
                        response["status"] == 2 -> {
                            // 业务逻辑异常重试
                            doRetry(dispatchMessage, retryTime, dockerIp, requestBody, driftIpInfo, resp.message(), unAvailableIpList)
                        }
                        else -> {
                            val msg = response["message"] as String
                            logger.error("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|${dispatchMessage.buildId}|$retryTime] Start build Docker VM failed, msg: $msg")
                            throw DockerServiceException(ErrorType.SYSTEM, ErrorCodeEnum.START_VM_FAIL.errorCode, "Start build Docker VM failed, msg: $msg")
                        }
                    }
                } else {
                    // 服务异常重试
                    doRetry(dispatchMessage, retryTime, dockerIp, requestBody, driftIpInfo, resp.message(), unAvailableIpList)
                }
            }
        } catch (e: SocketTimeoutException) {
            // 超时重试
            if (e.message == "timeout") {
                doRetry(dispatchMessage, retryTime, dockerIp, requestBody, driftIpInfo, e.message, unAvailableIpList)
            } else {
                logger.error("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|${dispatchMessage.buildId}|$retryTime] Start build Docker VM failed, msg: ${e.message}")
                throw DockerServiceException(ErrorType.SYSTEM, ErrorCodeEnum.START_VM_FAIL.errorCode, "Start build Docker VM failed, msg: ${e.message}")
            }
        }
    }

    private fun doRetry(
        dispatchMessage: DispatchMessage,
        retryTime: Int,
        dockerIp: String,
        requestBody: DockerHostBuildInfo,
        driftIpInfo: String,
        errorMessage: String?,
        unAvailableIpList: Set<String>?
    ) {
        if (retryTime < 3) {
            logger.warn("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|${dispatchMessage.buildId}|$retryTime] Start build Docker VM in $dockerIp failed, retry startBuild.")
            val unAvailableIpListLocal: Set<String> = unAvailableIpList?.plus(dockerIp) ?: setOf(dockerIp)
            val retryTimeLocal = retryTime + 1
            // 当前IP不可用，保险起见将当前ip可用性置为false，并重新获取可用ip
            pipelineDockerIPInfoDao.updateDockerIpStatus(dslContext, dockerIp, false)
            val dockerIpLocalPair = dockerHostUtils.getAvailableDockerIp(
                projectId = dispatchMessage.projectId,
                pipelineId = dispatchMessage.pipelineId,
                vmSeqId = dispatchMessage.vmSeqId,
                unAvailableIpList = unAvailableIpListLocal
            )
            dockerBuildStart(
                dockerIp = dockerIpLocalPair.first,
                dockerHostPort = dockerIpLocalPair.second,
                requestBody = requestBody,
                dispatchMessage = dispatchMessage,
                driftIpInfo = driftIpInfo,
                retryTime = retryTimeLocal,
                unAvailableIpList = unAvailableIpListLocal
            )
        } else {
            logger.error("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|${dispatchMessage.buildId}|$retryTime] Start build Docker VM failed, retry $retryTime times. message: $errorMessage")
            throw DockerServiceException(ErrorType.SYSTEM, ErrorCodeEnum.RETRY_START_VM_FAIL.errorCode, "Start build Docker VM failed, retry $retryTime times.")
        }
    }
}