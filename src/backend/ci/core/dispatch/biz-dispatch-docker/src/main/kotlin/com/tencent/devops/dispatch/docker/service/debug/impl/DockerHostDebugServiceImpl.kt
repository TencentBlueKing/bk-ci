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

package com.tencent.devops.dispatch.docker.service.debug.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.pojo.docker.DockerRoutingType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.docker.common.ErrorCodeEnum
import com.tencent.devops.dispatch.docker.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerDebugDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerEnableDao
import com.tencent.devops.dispatch.docker.exception.DockerServiceException
import com.tencent.devops.dispatch.docker.pojo.ContainerInfo
import com.tencent.devops.dispatch.docker.pojo.DebugStartParam
import com.tencent.devops.dispatch.docker.pojo.Pool
import com.tencent.devops.dispatch.docker.service.DockerHostProxyService
import com.tencent.devops.dispatch.docker.service.debug.DebugInterface
import com.tencent.devops.dispatch.docker.utils.DockerHostDebugLock
import com.tencent.devops.dispatch.docker.utils.RedisUtils
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerBuildRecord
import com.tencent.devops.store.api.container.ServiceContainerAppResource
import com.tencent.devops.store.pojo.app.BuildEnv
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch

@Service@Suppress("ALL")
class DockerHostDebugServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineDockerDebugDao: PipelineDockerDebugDao,
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val pipelineDockerEnableDao: PipelineDockerEnableDao,
    private val redisUtils: RedisUtils,
    private val redisOperation: RedisOperation,
    private val client: Client,
    private val dockerHostProxyService: DockerHostProxyService,
    private val objectMapper: ObjectMapper
) : DebugInterface {

    fun startDebug(
        dockerIp: String,
        userId: String,
        poolNo: Int,
        debugStartParam: DebugStartParam,
        startupMessage: String
    ): String {
        with(debugStartParam) {
            val stopWatch = StopWatch()
            stopWatch.start("get buildEnvs")
            val buildEnvStr = if (null != buildEnv && buildEnv!!.isNotEmpty()) {
                try {
                    val buildEnvs = client.get(ServiceContainerAppResource::class).getApp("linux")
                    val buildEnvResult = mutableListOf<BuildEnv>()
                    if (!(!buildEnvs.isOk() && null != buildEnvs.data && buildEnvs.data!!.isNotEmpty())) {
                        for (buildEnvParam in buildEnv!!) {
                            for (buildEnv1 in buildEnvs.data!!) {
                                if (buildEnv1.name == buildEnvParam.key && buildEnv1.version == buildEnvParam.value) {
                                    buildEnvResult.add(buildEnv1)
                                }
                            }
                        }
                    }
                    ObjectMapper().writeValueAsString(buildEnvResult)
                } catch (e: Exception) {
                    LOG.error("$pipelineId|$vmSeqId| start debug. get build env failed msg: $e")
                    ""
                }
            } else {
                ""
            }
            stopWatch.stop()

            val containerPool: Pool = objectMapper.readValue(startupMessage)
            LOG.info("$pipelineId|$vmSeqId| start debug. Container ready to start, buildEnvStr: $buildEnvStr. stopWatch: $stopWatch")

            // 根据dockerIp定向调用dockerhost
            val requestBody = ContainerInfo(
                projectId = projectId,
                pipelineId = pipelineId,
                vmSeqId = vmSeqId,
                poolNo = poolNo,
                status = PipelineTaskStatus.RUNNING.status,
                imageName = containerPool.container!!,
                containerId = "",
                address = "",
                token = cmd ?: "/bin/sh",
                buildEnv = buildEnvStr,
                registryUser = containerPool.credential!!.user,
                registryPwd = containerPool.credential!!.password,
                imageType = containerPool.imageType
            )

            return getContainerId(
                projectId = projectId,
                pipelineId = pipelineId,
                vmSeqId = vmSeqId,
                dockerIp = dockerIp,
                containerPool = containerPool,
                requestBody = requestBody
            )
        }
    }

    fun deleteDebug(pipelineId: String, vmSeqId: String): Result<Boolean> {
        val pipelineDockerDebug = pipelineDockerDebugDao.getDebug(dslContext, pipelineId, vmSeqId)
        if (pipelineDockerDebug != null) {
            val projectId = pipelineDockerDebug.projectId
            val dockerIp = pipelineDockerDebug.hostTag

            // 根据dockerIp定向调用dockerhost
            val requestBody = ContainerInfo(
                projectId = projectId,
                pipelineId = pipelineId,
                vmSeqId = vmSeqId,
                poolNo = pipelineDockerDebug.poolNo,
                status = pipelineDockerDebug.status,
                imageName = pipelineDockerDebug.imageName,
                containerId = pipelineDockerDebug.containerId,
                address = pipelineDockerDebug.hostTag,
                token = pipelineDockerDebug.token,
                buildEnv = pipelineDockerDebug.buildEnv,
                registryUser = pipelineDockerDebug.registryUser,
                registryPwd = pipelineDockerDebug.registryPwd,
                imageType = pipelineDockerDebug.imageType
            )

            val request = dockerHostProxyService.getDockerHostProxyRequest(
                dockerHostUri = "/api/docker/debug/end",
                dockerHostIp = dockerIp
            ).post(RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                JsonUtil.toJson(requestBody)
            ))
                .build()

            LOG.info("[$projectId|$pipelineId] Stop debug Docker VM $dockerIp url: ${request.url}")
            OkhttpUtils.doLongHttp(request).use { resp ->
                val responseBody = resp.body!!.string()
                LOG.info("[$projectId|$pipelineId] Stop debug Docker VM $dockerIp responseBody: $responseBody")
                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
                when {
                    response["status"] == 0 -> {
                        pipelineDockerDebugDao.deleteDebug(dslContext, pipelineDockerDebug.id)
                    }
                    else -> {
                        pipelineDockerDebugDao.updateStatus(dslContext, pipelineId, vmSeqId, PipelineTaskStatus.FAILURE)
                        val msg = response["message"]
                        LOG.error("[$projectId|$pipelineId] Stop debug Docker VM failed. $msg")
                        throw RuntimeException("Stop debug Docker VM failed. $msg")
                    }
                }
            }
        }

        return Result(true)
    }

    fun checkContainerStatus(
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        dockerIp: String,
        containerId: String
    ): Boolean {
        val request = dockerHostProxyService.getDockerHostProxyRequest(
            dockerHostUri = "/api/docker/container/$containerId/status",
            dockerHostIp = dockerIp
        ).get().build()

        OkhttpUtils.doHttp(request).use { resp ->
            val responseBody = resp.body!!.string()
            val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            if (response["status"] == 0) {
                return response["data"] as Boolean
            } else {
                val msg = response["message"] as String
                LOG.warn("[$projectId|$pipelineId|$vmSeqId]checkContainerStatus|$dockerIp|$containerId|failed: $msg")
                throw DockerServiceException(errorType = ErrorCodeEnum.GET_VM_STATUS_FAIL.errorType,
                    errorCode = ErrorCodeEnum.GET_VM_STATUS_FAIL.errorCode,
                    errorMsg = "Get container status $dockerIp $containerId failed, msg: $msg")
            }
        }
    }

    fun getWsUrl(
        projectId: String,
        pipelineId: String,
        dockerIp: String,
        containerId: String
    ): String {
        val request = dockerHostProxyService.getDockerHostProxyRequest(
            dockerHostUri = "/api/docker/debug/getWsUrl?" +
                    "projectId=$projectId&pipelineId=$pipelineId&containerId=$containerId",
            dockerHostIp = dockerIp
        ).get().build()

        OkhttpUtils.doHttp(request).use { resp ->
            val responseBody = resp.body!!.string()
            val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            if (response["status"] == 0) {
                return response["data"] as String
            } else {
                val msg = response["message"] as String
                LOG.warn("[$projectId|$pipelineId]getWsUrl|$dockerIp|$containerId|failed: $msg")
                throw DockerServiceException(errorType = ErrorCodeEnum.GET_VM_STATUS_FAIL.errorType,
                    errorCode = ErrorCodeEnum.GET_VM_STATUS_FAIL.errorCode,
                    errorMsg = "Get websocketUrl $dockerIp $containerId failed, msg: $msg")
            }
        }
    }

    fun getDebugStatus(pipelineId: String, vmSeqId: String): Result<ContainerInfo> {
        val debugTask = pipelineDockerDebugDao.getDebug(dslContext, pipelineId, vmSeqId)
        if (null == debugTask) {
            LOG.warn("The debug task not exists, pipelineId:$pipelineId, vmSeqId:$vmSeqId")
            val msg = redisUtils.getRedisDebugMsg(pipelineId = pipelineId, vmSeqId = vmSeqId)
            return Result(
                status = 1,
                message = I18nUtil.getCodeLanMessage(
                    "${ErrorCodeEnum.IMAGE_CHECK_LEGITIMATE_OR_RETRY.errorCode}"
                ) + if (!msg.isNullOrBlank()) {
                    "errormessage: $msg"
                } else {
                    ""
                }
            )
        }

        try {
            val containerStatusRunning = checkContainerStatus(
                projectId = debugTask.projectId,
                pipelineId = debugTask.pipelineId,
                vmSeqId = debugTask.vmSeqId,
                dockerIp = debugTask.hostTag,
                containerId = debugTask.containerId
            )

            if (!containerStatusRunning) {
                pipelineDockerDebugDao.deleteDebug(dslContext, debugTask.id)
                return Result(
                    status = 1,
                    message = I18nUtil.getCodeLanMessage(
                        "${ErrorCodeEnum.DEBUG_CONTAINER_SHUTS_DOWN_ABNORMALLY.errorCode}"
                    )
                )
            }
        } catch (e: Exception) {
            LOG.warn("get containerStatus error, ignore.")
        }

        return Result(
            status = 0,
            message = "success",
            data = ContainerInfo(
                projectId = debugTask.projectId,
                pipelineId = debugTask.pipelineId,
                vmSeqId = debugTask.vmSeqId,
                poolNo = debugTask.poolNo,
                status = debugTask.status,
                imageName = debugTask.imageName,
                containerId = debugTask.containerId ?: "",
                address = debugTask.hostTag ?: "",
                token = "",
                buildEnv = debugTask.buildEnv,
                registryUser = debugTask.registryUser,
                registryPwd = debugTask.registryPwd,
                imageType = debugTask.imageType
            )
        )
    }

    fun getDebugHistory(pipelineId: String, vmSeqId: String): Pair<String, String>? {
        val debugTask = pipelineDockerDebugDao.getDebug(dslContext, pipelineId, vmSeqId)
        if (debugTask != null) {
            LOG.warn("$pipelineId $vmSeqId debug history: ${debugTask.containerId}")
            return Pair(debugTask.hostTag, debugTask.containerId)
        }

        return null
    }

    fun cleanIp(projectId: String, pipelineId: String, vmSeqId: String): Result<Boolean> {
        LOG.info("clean pipeline docker build ip, projectId:$projectId, pipelineId:$pipelineId, vmSeqId:$vmSeqId")
        redisUtils.deleteDockerBuildLastHost(pipelineId, vmSeqId)
        return Result(0, "success")
    }

    fun getGreyWebConsoleProj(): List<String> {
        val result = mutableListOf<String>()
        val record = pipelineDockerEnableDao.list(dslContext)
        if (record.isNotEmpty) {
            for (i in record.indices) {
                result.add(record[i].pipelineId)
            }
        }
        return result
    }

    override fun startDebug(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String?,
        vmSeqId: String,
        dockerRoutingType: DockerRoutingType
    ): String {
        LOG.info("[$userId]| start debug, pipelineId: $pipelineId, vmSeqId: $vmSeqId")
        // 查询是否已经有启动调试容器了，如果有，直接返回成功
        val historyPair = getDebugHistory(pipelineId, vmSeqId)
        if (historyPair != null) {
            val wsUrl = getWsUrl(
                dockerIp = historyPair.first,
                projectId = projectId,
                pipelineId = pipelineId,
                containerId = historyPair.second
            )
            LOG.info("$pipelineId|startDebug|j($vmSeqId)|Container Exist|wsUrl=$wsUrl")
            return wsUrl
        }

        var buildHistory: TDispatchPipelineDockerBuildRecord? = null
        if (buildId.isNullOrBlank()) {
            // 查询是否存在构建机可启动调试，查看当前构建机的状态，如果running且已经容器，则直接复用当前running的containerId
            val dockerBuildHistoryList = pipelineDockerBuildDao.getLatestBuild(
                dslContext = dslContext,
                pipelineId = pipelineId,
                vmSeqId = vmSeqId.toInt()
            )

            if (dockerBuildHistoryList.size > 0 && dockerBuildHistoryList[0].dockerIp.isNotEmpty()) {
                buildHistory = dockerBuildHistoryList[0]
            }
        } else {
            buildHistory = pipelineDockerBuildDao.getBuild(
                dslContext = dslContext,
                buildId = buildId,
                vmSeqId = vmSeqId.toInt()
            )
        }

        if (buildHistory != null) {
            val containerPool: Pool = objectMapper.readValue(buildHistory.startupMessage)

            // 根据dockerIp定向调用dockerhost
            val requestBody = ContainerInfo(
                projectId = projectId,
                pipelineId = pipelineId,
                vmSeqId = vmSeqId,
                poolNo = 0,
                status = PipelineTaskStatus.RUNNING.status,
                imageName = containerPool.container!!,
                containerId = "",
                address = "",
                token = "/bin/sh",
                buildEnv = "",
                registryUser = containerPool.credential!!.user,
                registryPwd = containerPool.credential!!.password,
                imageType = containerPool.imageType
            )

            val containerId = getContainerId(
                projectId = projectId,
                pipelineId = pipelineId,
                vmSeqId = vmSeqId,
                dockerIp = buildHistory.dockerIp,
                containerPool = containerPool,
                requestBody = requestBody
            )

            return getWsUrl(
                dockerIp = buildHistory.dockerIp,
                projectId = projectId,
                pipelineId = pipelineId,
                containerId = containerId
            )
        } else {
            throw ErrorCodeException(
                errorCode = "${ErrorCodeEnum.NO_CONTAINER_IS_READY_DEBUG.errorCode}",
                defaultMessage = "Can not found debug container.",
                params = arrayOf(pipelineId)
            )
        }
    }

    override fun stopDebug(
        userId: String,
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        containerName: String,
        dockerRoutingType: DockerRoutingType
    ): Boolean {
        return deleteDebug(pipelineId, vmSeqId).data ?: false
    }

    private fun getContainerId(
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        dockerIp: String,
        containerPool: Pool,
        requestBody: ContainerInfo
    ): String {
        LOG.info("$pipelineId|$vmSeqId| start debug. Container ready to start.")

        val request = dockerHostProxyService.getDockerHostProxyRequest(
            dockerHostUri = "/api/docker/debug/start",
            dockerHostIp = dockerIp
        ).post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), JsonUtil.toJson(requestBody)))
            .build()

        LOG.info("[$projectId|$pipelineId] Start debug Docker VM $dockerIp url: ${request.url}")
        OkhttpUtils.doLongHttp(request).use { resp ->
            val responseBody = resp.body!!.string()
            LOG.info("[$projectId|$pipelineId] Start debug Docker VM $dockerIp responseBody: $responseBody")
            val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            when {
                response["status"] == 0 -> {
                    val containerId = response["data"].toString()
                    pipelineDockerDebugDao.insertDebug(
                        dslContext = dslContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        vmSeqId = vmSeqId,
                        poolNo = 0,
                        status = PipelineTaskStatus.RUNNING,
                        token = "",
                        imageName = containerPool.container!!,
                        hostTag = dockerIp,
                        containerId = containerId,
                        buildEnv = "",
                        registryUser = containerPool.credential!!.user,
                        registryPwd = containerPool.credential!!.password,
                        imageType = containerPool.imageType
                    )

                    return containerId
                }
                response["status"] == 1 -> {
                    // 母机负载过高
                    LOG.error("[$projectId|$pipelineId] Debug docker VM overload, please wait a moment and try again.")
                    throw ErrorCodeException(
                        errorCode = "${ErrorCodeEnum.LOAD_TOO_HIGH.errorCode}",
                        defaultMessage = "Debug docker VM overload, please wait a moment and try again.",
                        params = arrayOf(pipelineId)
                    )
                }
                else -> {
                    val msg = response["message"]
                    LOG.error("[$projectId|$pipelineId] Start debug Docker VM failed. $msg")
                    throw ErrorCodeException(
                        errorCode = "${ErrorCodeEnum.NO_CONTAINER_IS_READY_DEBUG.errorCode}",
                        defaultMessage = "Start debug Docker VM failed.",
                        params = arrayOf(pipelineId)
                    )
                }
            }
        }
    }

    @Scheduled(initialDelay = 45 * 1000, fixedDelay = 600 * 1000)
    fun clearTimeoutDebugTask() {
        val stopWatch = StopWatch()
        var message = ""
        val redisLock = DockerHostDebugLock(redisOperation)
        try {
            stopWatch.start("lock")
            redisLock.lock()
            stopWatch.stop()
            stopWatch.start("getTimeOutDebugTask")
            val timeoutDebugTask = pipelineDockerDebugDao.getTimeOutDebugTask(dslContext)
            stopWatch.stop()
            if (timeoutDebugTask.isNotEmpty) {
                LOG.info("There is ${timeoutDebugTask.size} debug task have/has already time out, clear it.")
                stopWatch.start("updateTimeOutDebugTask")
                for (i in timeoutDebugTask.indices) {
                    LOG.info("Delete timeout debug task, pipelineId:(${timeoutDebugTask[i].pipelineId}), " +
                            "vmSeqId:(${timeoutDebugTask[i].vmSeqId}), containerId:(${timeoutDebugTask[i].containerId})")
                    try {
                        deleteDebug(timeoutDebugTask[i].pipelineId, timeoutDebugTask[i].vmSeqId)
                    } catch (e: Exception) {
                        LOG.warn("Delete timeout debug task failed, ${e.message}")
                    }
                }
                stopWatch.stop()
                message = "timeoutDebugTask.size=${timeoutDebugTask.size}"
            }
        } finally {
            stopWatch.start("unlock")
            redisLock.unlock()
            stopWatch.stop()
            LOG.info("resetHostTag| $message| watch=$stopWatch")
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DockerHostDebugServiceImpl::class.java)
    }
}
