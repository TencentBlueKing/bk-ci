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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.dispatch.docker.common.Constants
import com.tencent.devops.dispatch.docker.common.ErrorCodeEnum
import com.tencent.devops.dispatch.docker.config.DefaultImageConfig
import com.tencent.devops.dispatch.docker.dao.DockerResourceOptionsDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerTaskSimpleDao
import com.tencent.devops.dispatch.docker.exception.DockerServiceException
import com.tencent.devops.dispatch.docker.pojo.DockerHostBuildInfo
import com.tencent.devops.dispatch.docker.pojo.Pool
import com.tencent.devops.dispatch.docker.pojo.enums.DockerHostClusterType
import com.tencent.devops.dispatch.docker.pojo.resource.DockerResourceOptionsVO
import com.tencent.devops.dispatch.docker.service.DockerHostProxyService
import com.tencent.devops.dispatch.docker.service.DockerHostQpcService
import com.tencent.devops.dispatch.docker.utils.DockerHostUtils
import com.tencent.devops.dispatch.docker.utils.RedisUtils
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException

@Component@Suppress("ALL")
class DockerHostClient @Autowired constructor(
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val pipelineDockerIPInfoDao: PipelineDockerIPInfoDao,
    private val pipelineDockerTaskSimpleDao: PipelineDockerTaskSimpleDao,
    private val dockerResourceOptionsDao: DockerResourceOptionsDao,
    private val dockerHostUtils: DockerHostUtils,
    private val client: Client,
    private val dslContext: DSLContext,
    private val defaultImageConfig: DefaultImageConfig,
    private val dockerHostProxyService: DockerHostProxyService,
    private val dockerHostQpcService: DockerHostQpcService,
    private val redisUtils: RedisUtils,
    private val buildLogPrinter: BuildLogPrinter
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(DockerHostClient::class.java)
        private const val RETRY_BUILD_TIME = 3
    }

    fun startBuild(
        dispatchMessage: DispatchMessage,
        dockerIp: String,
        dockerHostPort: Int,
        poolNo: Int,
        driftIpInfo: String,
        containerPool: Pool
    ) {
        val dispatchType = dispatchMessage.dispatchType as DockerDispatchType
        pipelineDockerBuildDao.saveBuildHistory(
            dslContext = dslContext,
            projectId = dispatchMessage.projectId,
            pipelineId = dispatchMessage.pipelineId,
            buildId = dispatchMessage.buildId,
            vmSeqId = dispatchMessage.vmSeqId.toInt(),
            secretKey = dispatchMessage.secretKey,
            status = PipelineTaskStatus.RUNNING,
            zone = Zone.SHENZHEN.name,
            dockerIp = dockerIp,
            poolNo = poolNo,
            startupMessage = JsonUtil.toJson(Pool(
                container = containerPool.container,
                credential = containerPool.credential,
                env = null,
                imageType = dispatchType.imageType?.type
            ))
        )

        val requestBody = DockerHostBuildInfo(
            projectId = dispatchMessage.projectId,
            agentId = dispatchMessage.id,
            pipelineId = dispatchMessage.pipelineId,
            buildId = dispatchMessage.buildId,
            vmSeqId = Integer.valueOf(dispatchMessage.vmSeqId),
            secretKey = dispatchMessage.secretKey,
            status = PipelineTaskStatus.RUNNING.status,
            imageName = containerPool.container!!,
            containerId = "",
            poolNo = poolNo,
            registryUser = containerPool.credential?.user ?: "",
            registryPwd = containerPool.credential?.password ?: "",
            imageType = dispatchType.imageType?.type,
            imagePublicFlag = dispatchType.imagePublicFlag,
            imageRDType = if (dispatchType.imageRDType == null) {
                null
            } else {
                ImageRDTypeEnum.getImageRDTypeByName(dispatchType.imageRDType!!).name
            },
            containerHashId = dispatchMessage.containerHashId,
            customBuildEnv = dispatchMessage.customBuildEnv,
            dockerResource = getDockerResource(dispatchType),
            qpcUniquePath = getQpcUniquePath(dispatchMessage),
            specialProjectList = getSpecialProjectList()
        )

        pipelineDockerTaskSimpleDao.createOrUpdate(
            dslContext = dslContext,
            pipelineId = dispatchMessage.pipelineId,
            vmSeq = dispatchMessage.vmSeqId,
            dockerIp = dockerIp,
            dockerResourceOptionsId = dispatchType.performanceConfigId
        )

        dockerBuildStart(dockerIp, dockerHostPort, requestBody, driftIpInfo)
    }

    fun endBuild(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: Int,
        containerId: String,
        dockerIp: String,
        poolNo: Int,
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
            poolNo = poolNo,
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
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                JsonUtil.toJson(requestBody)
            )).build()

        OkhttpUtils.doHttp(request).use { resp ->
            val responseBody = resp.body!!.string()
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
        dockerHostBuildInfo: DockerHostBuildInfo,
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
        ).post(RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            JsonUtil.toJson(dockerHostBuildInfo)
        ))
            .build()

        LOG.info("dockerStart|${dockerHostBuildInfo.buildId}|$retryTime|$dockerIp|${request.url}")
        try {
            OkhttpUtils.doLongHttp(request).use { resp ->
                if (resp.isSuccessful) {
                    val responseBody = resp.body!!.string()
                    val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
                    when {
                        response["status"] == 0 -> {
                            val containerId = response["data"] as String
                            LOG.info("${dockerHostBuildInfo.buildId}|$retryTime| update container: $containerId")
                            // 更新task状态以及构建历史记录，并记录漂移日志
                            dockerHostUtils.updateTaskSimpleAndRecordDriftLog(
                                pipelineId = dockerHostBuildInfo.pipelineId,
                                buildId = dockerHostBuildInfo.buildId,
                                vmSeqId = dockerHostBuildInfo.vmSeqId.toString(),
                                containerId = containerId,
                                newIp = dockerIp,
                                driftIpInfo = driftIpInfo
                            )
                        }
                        // 业务逻辑重试错误码匹配
                        arrayOf("2104002").contains(response["status"]) -> {
                            doRetry(
                                retryTime = retryTime,
                                dockerIp = dockerIp,
                                dockerHostBuildInfo = dockerHostBuildInfo,
                                driftIpInfo = driftIpInfo,
                                errorMessage = response["message"] as String,
                                unAvailableIpList = unAvailableIpList,
                                clusterType = clusterType
                            )
                        }
                        else -> {
                            // 非可重试异常码，不重试直接失败
                            doFail(
                                dockerIp = dockerIp,
                                dockerHostBuildInfo = dockerHostBuildInfo,
                                errorMessage = response["message"] as String
                            )
                        }
                    }
                } else {
                    // 服务异常重试
                    doRetry(
                        retryTime = retryTime,
                        dockerIp = dockerIp,
                        dockerHostBuildInfo = dockerHostBuildInfo,
                        driftIpInfo = driftIpInfo,
                        errorMessage = resp.message,
                        unAvailableIpList = unAvailableIpList,
                        clusterType = clusterType
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            // 只针对http连接超时重试
            if (e.message == "connect timed out") {
                doRetry(
                    retryTime = retryTime,
                    dockerIp = dockerIp,
                    dockerHostBuildInfo = dockerHostBuildInfo,
                    driftIpInfo = driftIpInfo,
                    errorMessage = e.message,
                    unAvailableIpList = unAvailableIpList,
                    clusterType = clusterType
                )
            } else {
                // read timeout, 不重试直接失败
                doFail(
                    dockerIp = dockerIp,
                    dockerHostBuildInfo = dockerHostBuildInfo,
                    errorMessage = e.message ?: "SocketTimeoutException: read time out"
                )
            }
        } catch (e: NoRouteToHostException) {
            // 针对Host unreachable场景重试
            doRetry(
                retryTime = retryTime,
                dockerIp = dockerIp,
                dockerHostBuildInfo = dockerHostBuildInfo,
                driftIpInfo = driftIpInfo,
                errorMessage = e.message,
                unAvailableIpList = unAvailableIpList,
                clusterType = clusterType
            )
        }
    }

    private fun doRetry(
        retryTime: Int,
        dockerIp: String,
        dockerHostBuildInfo: DockerHostBuildInfo,
        driftIpInfo: String,
        errorMessage: String?,
        unAvailableIpList: Set<String>?,
        clusterType: DockerHostClusterType = DockerHostClusterType.COMMON
    ) {
        // 当前IP此刻不可用，将IP状态置为false
        pipelineDockerIPInfoDao.updateDockerIpStatus(dslContext, dockerIp, false)

        if (retryTime < RETRY_BUILD_TIME) {
            LOG.warn("[${dockerHostBuildInfo.projectId}|${dockerHostBuildInfo.pipelineId}|${dockerHostBuildInfo.buildId}" +
                    "|$retryTime] Start build Docker VM in $dockerIp failed, retry startBuild. errorMessage: $errorMessage")

            buildLogPrinter.addYellowLine(
                buildId = dockerHostBuildInfo.buildId,
                message = "Start build Docker VM in $dockerIp failed, retry startBuild.",
                tag = VMUtils.genStartVMTaskId(dockerHostBuildInfo.vmSeqId.toString()),
                jobId = dockerHostBuildInfo.containerHashId,
                executeCount = 1
            )

            val unAvailableIpListLocal: Set<String> = unAvailableIpList?.plus(dockerIp) ?: setOf(dockerIp)
            val retryTimeLocal = retryTime + 1
            // 过滤重试前异常IP, 并重新获取可用ip
            val dockerIpLocalPair = dockerHostUtils.getAvailableDockerIp(
                projectId = dockerHostBuildInfo.projectId,
                pipelineId = dockerHostBuildInfo.pipelineId,
                vmSeqId = dockerHostBuildInfo.vmSeqId.toString(),
                unAvailableIpList = unAvailableIpListLocal,
                clusterType = clusterType
            )
            dockerBuildStart(
                dockerIp = dockerIpLocalPair.first,
                dockerHostPort = dockerIpLocalPair.second,
                dockerHostBuildInfo = dockerHostBuildInfo,
                driftIpInfo = driftIpInfo,
                clusterType = clusterType,
                retryTime = retryTimeLocal,
                unAvailableIpList = unAvailableIpListLocal
            )
        } else {
            LOG.error("${dockerHostBuildInfo.buildId}|${dockerHostBuildInfo.vmSeqId}|doRetry $retryTime times." +
                    " message: $errorMessage")
            throw DockerServiceException(
                errorType = ErrorCodeEnum.START_VM_FAIL.errorType,
                errorCode = ErrorCodeEnum.START_VM_FAIL.errorCode,
                errorMsg = "Start build Docker VM failed, msg: $errorMessage."
            )
        }
    }

    private fun getQpcUniquePath(dispatchMessage: DispatchMessage): String? {
        val projectId = dispatchMessage.projectId
        return if (projectId.startsWith("git_") &&
            dockerHostQpcService.checkQpcWhitelist(projectId.removePrefix("git_"))
        ) {
            return projectId.removePrefix("git_")
        } else {
            null
        }
    }

    fun getSpecialProjectList(): String? {
        return redisUtils.getSpecialProjectListKey()
    }

    private fun getDockerResource(dockerDispatchType: DockerDispatchType): DockerResourceOptionsVO {
        if (dockerDispatchType.performanceConfigId != 0) {
            val dockerResourceOptionRecord = dockerResourceOptionsDao.get(
                dslContext = dslContext,
                id = dockerDispatchType.performanceConfigId.toLong()
            )

            if (dockerResourceOptionRecord != null) {
                return DockerResourceOptionsVO(
                    memoryLimitBytes = dockerResourceOptionRecord.memoryLimitBytes,
                    cpuPeriod = dockerResourceOptionRecord.cpuPeriod,
                    cpuQuota = dockerResourceOptionRecord.cpuQuota,
                    blkioDeviceReadBps = dockerResourceOptionRecord.blkioDeviceReadBps,
                    blkioDeviceWriteBps = dockerResourceOptionRecord.blkioDeviceWriteBps,
                    disk = dockerResourceOptionRecord.disk,
                    description = ""
                )
            } else {
                return DockerResourceOptionsVO(
                    memoryLimitBytes = defaultImageConfig.memory,
                    cpuPeriod = defaultImageConfig.cpuPeriod,
                    cpuQuota = defaultImageConfig.cpuQuota,
                    blkioDeviceReadBps = defaultImageConfig.blkioDeviceReadBps,
                    blkioDeviceWriteBps = defaultImageConfig.blkioDeviceWriteBps,
                    disk = 100,
                    description = ""
                )
            }
        } else {
            return DockerResourceOptionsVO(
                memoryLimitBytes = defaultImageConfig.memory,
                cpuPeriod = defaultImageConfig.cpuPeriod,
                cpuQuota = defaultImageConfig.cpuQuota,
                blkioDeviceReadBps = defaultImageConfig.blkioDeviceReadBps,
                blkioDeviceWriteBps = defaultImageConfig.blkioDeviceWriteBps,
                disk = 100,
                description = ""
            )
        }
    }

    private fun doFail(
        dockerIp: String,
        dockerHostBuildInfo: DockerHostBuildInfo,
        errorMessage: String
    ) {
        // 当前IP此刻不可用，将IP状态置为false
        pipelineDockerIPInfoDao.updateDockerIpStatus(dslContext, dockerIp, false)

        LOG.error("${dockerHostBuildInfo.buildId}|${dockerHostBuildInfo.vmSeqId}| Start build Docker VM failed," +
                " message: $errorMessage")
        throw DockerServiceException(
            errorType = ErrorCodeEnum.START_VM_FAIL.errorType,
            errorCode = ErrorCodeEnum.START_VM_FAIL.errorCode,
            errorMsg = "Start build Docker VM failed, msg: $errorMessage."
        )
    }
}
