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

package com.tencent.devops.dispatch.docker.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.dispatch.docker.common.ErrorCodeEnum
import com.tencent.devops.dispatch.docker.config.DefaultImageConfig
import com.tencent.devops.dispatch.docker.dao.PipelineDockerDebugDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerEnableDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerHostDao
import com.tencent.devops.dispatch.docker.exception.DockerServiceException
import com.tencent.devops.dispatch.docker.pojo.ContainerInfo
import com.tencent.devops.dispatch.docker.pojo.DebugStartParam
import com.tencent.devops.dispatch.docker.pojo.Pool
import com.tencent.devops.dispatch.docker.utils.CommonUtils
import com.tencent.devops.dispatch.docker.utils.DockerHostDebugLock
import com.tencent.devops.dispatch.docker.utils.RedisUtils
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.store.api.container.ServiceContainerAppResource
import com.tencent.devops.store.pojo.app.BuildEnv
import com.tencent.devops.store.pojo.image.exception.UnknownImageType
import com.tencent.devops.store.pojo.image.response.ImageRepoInfo
import com.tencent.devops.ticket.pojo.enums.CredentialType
import okhttp3.MediaType
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch

@Service@Suppress("ALL")
class DockerHostDebugService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineDockerDebugDao: PipelineDockerDebugDao,
    private val pipelineDockerHostDao: PipelineDockerHostDao,
    private val pipelineDockerEnableDao: PipelineDockerEnableDao,
    private val redisUtils: RedisUtils,
    private val redisOperation: RedisOperation,
    private val client: Client,
    private val storeImageService: StoreImageService,
    private val gray: Gray,
    private val defaultImageConfig: DefaultImageConfig,
    private val dockerHostProxyService: DockerHostProxyService,
    private val objectMapper: ObjectMapper
) {

    private val grayFlag: Boolean = gray.isGray()

    fun startDebug(
        dockerIp: String,
        userId: String,
        poolNo: Int,
        debugStartParam: DebugStartParam,
        startupMessage: String
    ): String {
        with(debugStartParam) {
            val stopWatch = StopWatch()
            /*var imageRepoInfo: ImageRepoInfo? = null
            var finalCredentialId = credentialId
            var credentialProject = projectId

            val imageTypeEnum = ImageType.getType(debugStartParam.imageType)
            stopWatch.start("getImageRepoInfo")
            if (imageTypeEnum == ImageType.BKSTORE) {
                imageRepoInfo = storeImageService.getImageRepoInfo(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = null,
                    imageCode = imageCode,
                    imageVersion = imageVersion,
                    defaultPrefix = defaultImageConfig.dockerBuildImagePrefix
                )
                if (imageRepoInfo.ticketId.isNotBlank()) {
                    finalCredentialId = imageRepoInfo.ticketId
                }
                credentialProject = imageRepoInfo.ticketProject
            }
            stopWatch.stop()

            val dockerImage = getDebugDockerImage(imageRepoInfo, debugStartParam)

            stopWatch.start("get credentialId")
            val (userName, password) = getUserNameAndPassword(
                imageType = imageTypeEnum,
                finalCredentialId = finalCredentialId,
                credentialProject = credentialProject,
                containerPool = containerPool
            )
            stopWatch.stop()

            val newImageType = getFormatImageType(
                imageTypeEnum = imageTypeEnum,
                imageRepoInfo = imageRepoInfo,
                imageCode = imageCode,
                imageVersion = imageVersion,
                containerPool = containerPool
            )*/

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

            val request = dockerHostProxyService.getDockerHostProxyRequest(
                dockerHostUri = "/api/docker/debug/start",
                dockerHostIp = dockerIp
            ).post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JsonUtil.toJson(requestBody)))
                .build()

            LOG.info("[$projectId|$pipelineId] Start debug Docker VM $dockerIp url: ${request.url()}")
            OkhttpUtils.doLongHttp(request).use { resp ->
                val responseBody = resp.body()!!.string()
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
                            poolNo = poolNo,
                            status = PipelineTaskStatus.RUNNING,
                            token = "",
                            imageName = containerPool.container!!,
                            hostTag = dockerIp,
                            containerId = containerId,
                            buildEnv = buildEnvStr,
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
                            errorCode = "2103505",
                            defaultMessage = "Debug docker VM overload, please wait a moment and try again.",
                            params = arrayOf(pipelineId)
                        )
                    }
                    else -> {
                        val msg = response["message"]
                        LOG.error("[$projectId|$pipelineId] Start debug Docker VM failed. $msg")
                        throw ErrorCodeException(
                            errorCode = "2103503",
                            defaultMessage = "Start debug Docker VM failed.",
                            params = arrayOf(pipelineId)
                        )
                    }
                }
            }
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
            ).post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JsonUtil.toJson(requestBody)))
                .build()

            LOG.info("[$projectId|$pipelineId] Stop debug Docker VM $dockerIp url: ${request.url()}")
            OkhttpUtils.doLongHttp(request).use { resp ->
                val responseBody = resp.body()!!.string()
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

        return Result(0, "success")
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
            val responseBody = resp.body()!!.string()
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
            val responseBody = resp.body()!!.string()
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
                message = "登录调试失败,请检查镜像是否合法或重试。" + if (!msg.isNullOrBlank()) {
                    "错误信息: $msg"
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
                    message = "登录调试失败，调试容器异常关闭，请重试。"
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

    private fun getDebugDockerImage(
        imageRepoInfo: ImageRepoInfo?,
        debugStartParam: DebugStartParam
    ): String {
        with(debugStartParam) {
            if (containerPool != null) {
                val containerPool: Pool = objectMapper.readValue(debugStartParam.containerPool!!)
                return containerPool.container!!
            }

            val dockerImage = when (ImageType.getType(imageType)) {
                ImageType.THIRD -> imageName!!
                ImageType.BKSTORE -> {
                    // 研发商店镜像一定含name与tag
                    if (imageRepoInfo!!.repoUrl.isBlank()) {
                        // dockerhub镜像名称不带斜杠前缀
                        imageRepoInfo.repoName + ":" + imageRepoInfo.repoTag
                    } else {
                        // 无论蓝盾还是第三方镜像此处均需完整路径
                        imageRepoInfo.repoUrl + "/" + imageRepoInfo.repoName + ":" + imageRepoInfo.repoTag
                    }
                }
                else -> when (imageName) {
                    DockerVersion.TLINUX1_2.value -> {
                        defaultImageConfig.getTLinux1_2CompleteUri()
                    }
                    DockerVersion.TLINUX2_2.value -> {
                        defaultImageConfig.getTLinux2_2CompleteUri()
                    }
                    else -> {
                        if (defaultImageConfig.dockerBuildImagePrefix.isNullOrBlank()) {
                            imageName?.trim()?.removePrefix("/")
                        } else {
                            "${defaultImageConfig.dockerBuildImagePrefix}/bkdevops/$imageName"
                        }!!
                    }
                }
            }
            LOG.info("$pipelineId|dockerHostDebug|$dockerImage|$imageCode|$imageVersion|$credentialId")

            return dockerImage
        }
    }

    private fun getUserNameAndPassword(
        imageType: ImageType,
        finalCredentialId: String?,
        credentialProject: String,
        containerPool: String?
    ): Pair<String?, String?> {
        var userName: String? = null
        var password: String? = null

        if (containerPool != null) {
            val pool: Pool = objectMapper.readValue(containerPool)
            return Pair(pool.credential?.user, pool.credential?.password)
        }

        if (imageType == ImageType.THIRD && !finalCredentialId.isNullOrBlank()) {
            val ticketsMap =
                CommonUtils.getCredential(
                    client = client,
                    projectId = credentialProject,
                    credentialId = finalCredentialId!!,
                    type = CredentialType.USERNAME_PASSWORD
                )
            userName = ticketsMap["v1"] as String
            password = ticketsMap["v2"] as String
        }

        return Pair(userName, password)
    }

    private fun getFormatImageType(
        imageRepoInfo: ImageRepoInfo?,
        imageTypeEnum: ImageType,
        imageCode: String?,
        imageVersion: String?,
        containerPool: String?
    ): String? {
        if (containerPool != null) {
            return null
        }

        return when (imageTypeEnum) {
            ImageType.THIRD -> imageTypeEnum.type
            ImageType.BKDEVOPS -> ImageType.BKDEVOPS.type
            ImageType.BKSTORE -> imageRepoInfo!!.sourceType.type
            else -> throw UnknownImageType("imageCode:$imageCode,imageVersion:$imageVersion," +
                    "imageType:$imageTypeEnum")
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

    companion object {
        private val LOG = LoggerFactory.getLogger(DockerHostDebugService::class.java)
    }
}
