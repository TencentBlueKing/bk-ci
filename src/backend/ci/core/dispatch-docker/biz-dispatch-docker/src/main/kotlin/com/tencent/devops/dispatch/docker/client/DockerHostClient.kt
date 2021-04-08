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
import com.tencent.devops.dispatch.docker.pojo.enums.DockerHostClusterType
import com.tencent.devops.dispatch.docker.service.DockerHostProxyService
import com.tencent.devops.dispatch.docker.utils.CommonUtils
import com.tencent.devops.dispatch.docker.utils.DockerHostUtils
import com.tencent.devops.dispatch.docker.utils.RedisUtils
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.pojo.redis.RedisBuild
import com.tencent.devops.process.pojo.mq.PipelineBuildLessStartupDispatchEvent
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import com.tencent.devops.ticket.pojo.enums.CredentialType
import okhttp3.MediaType
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.SocketTimeoutException

@Component@Suppress("ALL")
class DockerHostClient @Autowired constructor(
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val pipelineDockerIPInfoDao: PipelineDockerIPInfoDao,
    private val pipelineDockerTaskSimpleDao: PipelineDockerTaskSimpleDao,
    private val dockerHostUtils: DockerHostUtils,
    private val redisOperation: RedisOperation,
    private val client: Client,
    private val dslContext: DSLContext,
    private val defaultImageConfig: DefaultImageConfig,
    private val dockerHostProxyService: DockerHostProxyService,
    private val redisUtils: RedisUtils
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(DockerHostClient::class.java)
    }

    fun startBuild(
        dispatchMessage: DispatchMessage,
        dockerIp: String,
        dockerHostPort: Int,
        poolNo: Int,
        driftIpInfo: String
    ) {
        pipelineDockerBuildDao.startBuild(
            dslContext = dslContext,
            projectId = dispatchMessage.projectId,
            pipelineId = dispatchMessage.pipelineId,
            buildId = dispatchMessage.buildId,
            vmSeqId = dispatchMessage.vmSeqId.toInt(),
            secretKey = dispatchMessage.secretKey,
            status = PipelineTaskStatus.RUNNING,
            zone = if (null == dispatchMessage.zone) {
                Zone.SHENZHEN.name
            } else {
                dispatchMessage.zone!!.name
            },
            dockerIp = dockerIp,
            poolNo = poolNo
        )

        val dispatchType = dispatchMessage.dispatchType as DockerDispatchType
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
        LOG.info("${dispatchMessage.buildId}|startBuild|${dispatchMessage.id}|$dockerImage" +
            "|${dispatchType.imageCode}|${dispatchType.imageVersion}|${dispatchType.credentialId}" +
            "|${dispatchType.credentialProject}")
        var userName: String? = null
        var password: String? = null
        if (dispatchType.imageType == ImageType.THIRD) {
            if (!dispatchType.credentialId.isNullOrBlank()) {
                val projectId = if (dispatchType.credentialProject.isNullOrBlank()) {
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
            agentId = dispatchMessage.id,
            pipelineId = dispatchMessage.pipelineId,
            buildId = dispatchMessage.buildId,
            vmSeqId = Integer.valueOf(dispatchMessage.vmSeqId),
            secretKey = dispatchMessage.secretKey,
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

    fun startAgentLessBuild(
        agentLessDockerIp: String,
        agentLessDockerPort: Int,
        event: PipelineBuildLessStartupDispatchEvent
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
            dockerIp = agentLessDockerIp,
            poolNo = 0
        )

        val agentId = HashUtil.encodeLongId(id)
        redisUtils.setDockerBuild(
            id = id, secretKey = secretKey,
            redisBuild = RedisBuild(
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

        LOG.info("[${event.buildId}]|BUILD_LESS| secretKey: $secretKey")
        LOG.info("[${event.buildId}]|BUILD_LESS| agentId: $agentId")
        val dispatchType = event.dispatchType as DockerDispatchType
        val dockerImage = when (dispatchType.dockerBuildVersion) {
            DockerVersion.TLINUX1_2.value -> {
                defaultImageConfig.getBuildLessTLinux1_2CompleteUri()
            }
            DockerVersion.TLINUX2_2.value -> {
                defaultImageConfig.getBuildLessTLinux2_2CompleteUri()
            }
            else -> {
                defaultImageConfig.getBuildLessCompleteUriByImageName(dispatchType.dockerBuildVersion)
            }
        }
        LOG.info("[${event.buildId}]|BUILD_LESS| Docker images is: $dockerImage")

        var userName: String? = null
        var password: String? = null
        if (dispatchType.imageType == ImageType.THIRD) {
            if (!dispatchType.credentialId.isNullOrBlank()) {
                val ticketsMap = CommonUtils.getCredential(
                    client = client,
                    projectId = event.projectId,
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
            imageName = dockerImage,
            containerId = "",
            wsInHost = true,
            poolNo = 0,
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

        dockerBuildStart(agentLessDockerIp, agentLessDockerPort, requestBody, DispatchMessage(
            id = agentId,
            secretKey = secretKey,
            gateway = "",
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            buildId = event.buildId,
            dispatchMessage = event.dispatchType.value,
            userId = event.userId,
            vmSeqId = event.vmSeqId,
            channelCode = event.channelCode,
            vmNames = "",
            atoms = event.atoms,
            zone = event.zone,
            containerHashId = event.containerHashId,
            executeCount = event.executeCount,
            containerId = event.containerId,
            containerType = "",
            stageId = "",
            dispatchType = event.dispatchType
        ), "", DockerHostClusterType.AGENT_LESS)
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

        val request = dockerHostProxyService.getDockerHostProxyRequest(
            dockerHostUri = Constants.DOCKERHOST_END_URI,
            dockerHostIp = dockerIp,
            clusterType = clusterType
        ).delete(
            RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            JsonUtil.toJson(requestBody)
        )).build()

        OkhttpUtils.doHttp(request).use { resp ->
            val responseBody = resp.body()!!.string()
            LOG.info("[$projectId|$pipelineId|$buildId] End build Docker VM $dockerIp responseBody: $responseBody")
            val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            if (response["status"] == 0) {
                response["data"] as Boolean
            } else {
                val msg = response["message"] as String
                LOG.error("[$projectId|$pipelineId|$buildId] End build Docker VM failed, msg: $msg")
                throw DockerServiceException(errorType = ErrorCodeEnum.END_VM_ERROR.errorType,
                    errorCode = ErrorCodeEnum.END_VM_ERROR.errorCode,
                    errorMsg = "End build Docker VM failed, msg: $msg")
            }
        }
    }

    private fun dockerBuildStart(
        dockerIp: String,
        dockerHostPort: Int,
        requestBody: DockerHostBuildInfo,
        dispatchMessage: DispatchMessage,
        driftIpInfo: String,
        clusterType: DockerHostClusterType = DockerHostClusterType.COMMON,
        retryTime: Int = 0,
        unAvailableIpList: Set<String>? = null
    ) {
        val dockerHostUri = if (clusterType == DockerHostClusterType.AGENT_LESS) {
            Constants.DOCKERHOST_AGENTLESS_STARTUP_URI
        } else {
            Constants.DOCKERHOST_STARTUP_URI
        }

        val request = dockerHostProxyService.getDockerHostProxyRequest(
            dockerHostUri = dockerHostUri,
            dockerHostIp = dockerIp,
            dockerHostPort = dockerHostPort,
            clusterType = clusterType
        ).post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JsonUtil.toJson(requestBody)))
            .build()

        LOG.info("dockerStart|${dispatchMessage.buildId}|$retryTime|$dockerIp|${request.url()}")
        try {
            OkhttpUtils.doLongHttp(request).use { resp ->
                if (resp.isSuccessful) {
                    val responseBody = resp.body()!!.string()
                    val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
                    when {
                        response["status"] == 0 -> {
                            val containerId = response["data"] as String
                            LOG.info("${dispatchMessage.buildId}|$retryTime| update container: $containerId")
                            // 更新task状态以及构建历史记录，并记录漂移日志
                            dockerHostUtils.updateTaskSimpleAndRecordDriftLog(
                                dispatchMessage = dispatchMessage,
                                containerId = containerId,
                                newIp = dockerIp,
                                driftIpInfo = driftIpInfo
                            )
                        }
                        // 业务逻辑重试错误码匹配
                        arrayOf("2104002").contains(response["status"]) -> {
                            doRetry(dispatchMessage, retryTime, dockerIp, requestBody, driftIpInfo, resp.message(), unAvailableIpList)
                        }
                        else -> {
                            val msg = response["message"] as String
                            LOG.error("${dispatchMessage.buildId}|$retryTime| Start build Docker VM failed, msg: $msg")
                            throw DockerServiceException(errorType = ErrorCodeEnum.START_VM_FAIL.errorType,
                                errorCode = ErrorCodeEnum.START_VM_FAIL.errorCode,
                                errorMsg = "Start build Docker VM failed, msg: $msg")
                        }
                    }
                } else {
                    // 服务异常重试
                    doRetry(
                        dispatchMessage = dispatchMessage,
                        retryTime = retryTime,
                        dockerIp = dockerIp,
                        requestBody = requestBody,
                        driftIpInfo = driftIpInfo,
                        errorMessage = resp.message(),
                        unAvailableIpList = unAvailableIpList,
                        clusterType = clusterType
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            // 超时重试
            if (e.message == "timeout") {
                doRetry(dispatchMessage, retryTime, dockerIp, requestBody, driftIpInfo, e.message, unAvailableIpList)
            } else {
                LOG.error("${dispatchMessage.buildId}|$retryTime| Start build Docker VM failed, msg: ${e.message}")
                throw DockerServiceException(errorType = ErrorCodeEnum.START_VM_FAIL.errorType,
                    errorCode = ErrorCodeEnum.START_VM_FAIL.errorCode,
                    errorMsg = "Start build Docker VM failed, msg: ${e.message}")
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
        unAvailableIpList: Set<String>?,
        clusterType: DockerHostClusterType = DockerHostClusterType.COMMON
    ) {
        if (retryTime < 3) {
            LOG.warn("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|${dispatchMessage.buildId}" +
                    "|$retryTime] Start build Docker VM in $dockerIp failed, retry startBuild.")
            val unAvailableIpListLocal: Set<String> = unAvailableIpList?.plus(dockerIp) ?: setOf(dockerIp)
            val retryTimeLocal = retryTime + 1
            // 当前IP不可用，保险起见将当前ip可用性置为false，并重新获取可用ip
            pipelineDockerIPInfoDao.updateDockerIpStatus(dslContext, dockerIp, false)
            val dockerIpLocalPair = dockerHostUtils.getAvailableDockerIp(
                projectId = dispatchMessage.projectId,
                pipelineId = dispatchMessage.pipelineId,
                vmSeqId = dispatchMessage.vmSeqId,
                unAvailableIpList = unAvailableIpListLocal,
                clusterType = clusterType
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
            LOG.error("${dispatchMessage.buildId}|$retryTime|doRetry $retryTime times. message: $errorMessage")
            throw DockerServiceException(errorType = ErrorCodeEnum.RETRY_START_VM_FAIL.errorType,
                errorCode = ErrorCodeEnum.RETRY_START_VM_FAIL.errorCode,
                errorMsg = "Start build Docker VM failed, retry $retryTime times.")
        }
    }
}
