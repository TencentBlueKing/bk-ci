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

package com.tencent.devops.dispatch.docker.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.common.web.mq.alert.AlertUtils
import com.tencent.devops.dispatch.docker.common.ErrorCodeEnum
import com.tencent.devops.dispatch.docker.config.DefaultImageConfig
import com.tencent.devops.dispatch.docker.dao.PipelineDockerDebugDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerEnableDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerHostDao
import com.tencent.devops.dispatch.docker.exception.DockerServiceException
import com.tencent.devops.dispatch.pojo.ContainerInfo
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.service.StoreImageService
import com.tencent.devops.dispatch.docker.utils.CommonUtils
import com.tencent.devops.dispatch.docker.utils.DockerHostDebugLock
import com.tencent.devops.dispatch.docker.utils.DockerHostUtils
import com.tencent.devops.dispatch.docker.utils.RedisUtils
import com.tencent.devops.store.api.container.ServiceContainerAppResource
import com.tencent.devops.store.pojo.app.BuildEnv
import com.tencent.devops.store.pojo.image.exception.UnknownImageType
import com.tencent.devops.store.pojo.image.response.ImageRepoInfo
import com.tencent.devops.ticket.pojo.enums.CredentialType
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch

@Service
class DockerHostDebugService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineDockerDebugDao: PipelineDockerDebugDao,
    private val pipelineDockerHostDao: PipelineDockerHostDao,
    private val pipelineDockerEnableDao: PipelineDockerEnableDao,
    private val dockerHostUtils: DockerHostUtils,
    private val redisUtils: RedisUtils,
    private val redisOperation: RedisOperation,
    private val client: Client,
    private val storeImageService: StoreImageService,
    private val gray: Gray,
    private val defaultImageConfig: DefaultImageConfig
) {

    private val grayFlag: Boolean = gray.isGray()

    @Value("\${devopsGateway.idcProxy}")
    val idcProxy: String? = null

    fun startDebug(
        dockerIp: String,
        userId: String,
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        poolNo: Int,
        imageCode: String?,
        imageVersion: String?,
        imageName: String?,
        buildEnv: Map<String, String>?,
        imageType: ImageType?,
        credentialId: String?
    ) {
        logger.info("Start docker debug  pipelineId:($pipelineId), projectId:($projectId), vmSeqId:($vmSeqId), imageName:($imageName), imageType:($imageType), imageCode:($imageCode), imageVersion:($imageVersion)")
        var imageRepoInfo: ImageRepoInfo? = null
        var finalCredentialId = credentialId
        var credentialProject = projectId
        if (imageType == ImageType.BKSTORE) {
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
            if (credentialProject.isBlank()) {
                logger.warn("insertDebug:credentialProject is blank,pipelineId=$pipelineId, imageCode=$imageCode,imageVersion=$imageVersion,credentialId=$credentialId")
            }
        }
        val dockerImage = when (imageType) {
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
        logger.info("insertDebug:Docker images is: $dockerImage")
        var userName: String? = null
        var password: String? = null
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

        val newImageType = when (imageType) {
            null -> ImageType.BKDEVOPS.type
            ImageType.THIRD -> imageType.type
            ImageType.BKDEVOPS -> ImageType.BKDEVOPS.type
            ImageType.BKSTORE -> imageRepoInfo!!.sourceType.type
            else -> throw UnknownImageType("imageCode:$imageCode,imageVersion:$imageVersion,imageType:$imageType")
        }

        val buildEnvStr = if (null != buildEnv && buildEnv.isNotEmpty()) {
            try {
                val buildEnvs = client.get(ServiceContainerAppResource::class).getApp("linux")
                val buildEnvResult = mutableListOf<BuildEnv>()
                if (!(!buildEnvs.isOk() && null != buildEnvs.data && buildEnvs.data!!.isNotEmpty())) {
                    for (buildEnvParam in buildEnv) {
                        for (buildEnv1 in buildEnvs.data!!) {
                            if (buildEnv1.name == buildEnvParam.key && buildEnv1.version == buildEnvParam.value) {
                                buildEnvResult.add(buildEnv1)
                            }
                        }
                    }
                }
                ObjectMapper().writeValueAsString(buildEnvResult)
            } catch (e: Exception) {
                logger.error("$pipelineId|$vmSeqId| start debug. get build env failed msg: $e")
                ""
            }
        } else {
            ""
        }
        logger.info("$pipelineId|$vmSeqId| start debug. Container ready to start, buildEnvStr: $buildEnvStr")

        // 根据dockerIp定向调用dockerhost
        val proxyUrl = dockerHostUtils.getIdc2DevnetProxyUrl("/api/docker/debug/start", dockerIp)
        val requestBody = ContainerInfo(projectId, pipelineId, vmSeqId, poolNo, PipelineTaskStatus.RUNNING.status, dockerImage,
            "", "", "", buildEnvStr, userName, password, newImageType)
        val request = Request.Builder().url(proxyUrl)
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JsonUtil.toJson(requestBody)))
            .addHeader("Accept", "application/json; charset=utf-8")
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .build()

        logger.info("[$projectId|$pipelineId] Start debug Docker VM $dockerIp url: $proxyUrl, requestBody: ${JsonUtil.toJson(requestBody)}")
        OkhttpUtils.doLongHttp(request).use { resp ->
            val responseBody = resp.body()!!.string()
            logger.info("[$projectId|$pipelineId] Start debug Docker VM $dockerIp responseBody: $responseBody")
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
                        imageName = dockerImage.trim(),
                        hostTag = dockerIp,
                        containerId = containerId,
                        buildEnv = buildEnvStr,
                        registryUser = userName,
                        registryPwd = password,
                        imageType = newImageType,
                        imagePublicFlag = imageRepoInfo?.publicFlag,
                        imageRDType = imageRepoInfo?.rdType
                    )
                }
                response["status"] == 1 -> {
                    // 母机负载过高
                    logger.error("[$projectId|$pipelineId] Debug docker VM overload, please wait a moment and try again.")
                    throw ErrorCodeException(
                        errorCode = "2103505",
                        defaultMessage = "Debug docker VM overload, please wait a moment and try again.",
                        params = arrayOf(pipelineId)
                    )
                }
                else -> {
                    val msg = response["message"]
                    logger.error("[$projectId|$pipelineId] Start debug Docker VM failed. $msg")
                    throw ErrorCodeException(
                        errorCode = "2103503",
                        defaultMessage = "Start debug Docker VM failed.",
                        params = arrayOf(pipelineId)
                    )
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
            val proxyUrl = dockerHostUtils.getIdc2DevnetProxyUrl("/api/docker/debug/end", dockerIp)
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
            val request = Request.Builder().url(proxyUrl)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JsonUtil.toJson(requestBody)))
                .addHeader("Accept", "application/json; charset=utf-8")
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build()

            logger.info("[$projectId|$pipelineId] Stop debug Docker VM $dockerIp url: $proxyUrl, requestBody: ${JsonUtil.toJson(requestBody)}")
            OkhttpUtils.doLongHttp(request).use { resp ->
                val responseBody = resp.body()!!.string()
                logger.info("[$projectId|$pipelineId] Stop debug Docker VM $dockerIp responseBody: $responseBody")
                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
                when {
                    response["status"] == 0 -> {
                        pipelineDockerDebugDao.deleteDebug(dslContext, pipelineDockerDebug.id)
                    }
                    else -> {
                        pipelineDockerDebugDao.updateStatus(dslContext, pipelineId, vmSeqId, PipelineTaskStatus.FAILURE)
                        val msg = response["message"]
                        logger.error("[$projectId|$pipelineId] Stop debug Docker VM failed. $msg")
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
        val proxyUrl = dockerHostUtils.getIdc2DevnetProxyUrl("/api/docker/container/$containerId/status", dockerIp)
        val request = Request.Builder().url(proxyUrl)
            .get()
            .addHeader("Accept", "application/json; charset=utf-8")
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .build()

        OkhttpUtils.doHttp(request).use { resp ->
            val responseBody = resp.body()!!.string()
            logger.info("[$projectId|$pipelineId|$vmSeqId] Get container status $dockerIp $containerId responseBody: $responseBody")
            val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            if (response["status"] == 0) {
                return response["data"] as Boolean
            } else {
                val msg = response["message"] as String
                logger.error("[$projectId|$pipelineId|$vmSeqId] Get container status $dockerIp $containerId failed, msg: $msg")
                throw DockerServiceException(ErrorType.SYSTEM, ErrorCodeEnum.GET_VM_STATUS_FAIL.errorCode, "Get container status $dockerIp $containerId failed, msg: $msg")
            }
        }
    }

    fun getDebugStatus(pipelineId: String, vmSeqId: String): Result<ContainerInfo> {
        val debugTask = pipelineDockerDebugDao.getDebug(dslContext, pipelineId, vmSeqId)
        if (null == debugTask) {
            logger.warn("The debug task not exists, pipelineId:$pipelineId, vmSeqId:$vmSeqId")
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
            logger.warn("get containerStatus error, ignore.")
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

    fun startDebug(hostTag: String): Result<ContainerInfo>? {
        val stopWatch = StopWatch()
        var message = ""
        val redisLock = DockerHostDebugLock(redisOperation)
        try {
            stopWatch.start("getGrayProject")
            val grayProjectSet = this.gray.grayProjectSet(redisOperation)
            stopWatch.stop()
            stopWatch.start("tryLock")
            val tryLock = redisLock.tryLock(timeout = 4000)
            stopWatch.stop()

            if (!tryLock) {
                message = "try lock fail in ${stopWatch.lastTaskTimeMillis}"
                return Result(status = 1, message = message)
            }
            val debugTasks = if (grayFlag) {
                stopWatch.start("grayDebugQueueTask")
                // 优先取设置了IP的任务（可能是固定构建机，也可能是上次用的构建机）
                var debugTask = pipelineDockerDebugDao.getQueueDebugByProj(dslContext, grayProjectSet, hostTag)
                if (debugTask.isNotEmpty) {
                    logger.info("[$hostTag|$grayFlag] Start docker debug with hostTag: $hostTag")
                } else {
                    debugTask = pipelineDockerDebugDao.getQueueDebugByProj(dslContext, grayProjectSet)
                }
                stopWatch.stop()
                debugTask
            } else {
                stopWatch.start("prodDebugQueueTask")
                // 优先取设置了IP的任务（可能是固定构建机，也可能是上次用的构建机）
                var debugTask = pipelineDockerDebugDao.getQueueDebugExcludeProj(dslContext, grayProjectSet, hostTag)
                if (debugTask.isNotEmpty) {
                    logger.info("[$hostTag|$grayFlag] Start docker debug with hostTag: $hostTag")
                } else {
                    debugTask = pipelineDockerDebugDao.getQueueDebugExcludeProj(dslContext, grayProjectSet)
                }
                stopWatch.stop()
                debugTask
            }

            if (debugTasks.isEmpty()) {
                message = "No debug task in queue"
                return Result(status = 1, message = message)
            }
            val debug = debugTasks[0]
            stopWatch.start("updateStatusAndTag")
            pipelineDockerDebugDao.updateStatusAndTag(
                dslContext = dslContext,
                pipelineId = debug.pipelineId,
                vmSeqId = debug.vmSeqId,
                status = PipelineTaskStatus.RUNNING,
                hostTag = hostTag
            )
            stopWatch.stop()
            return Result(
                status = 0,
                message = "success",
                data = ContainerInfo(
                    projectId = debug.projectId,
                    pipelineId = debug.pipelineId,
                    vmSeqId = debug.vmSeqId,
                    poolNo = 0,
                    status = PipelineTaskStatus.RUNNING.status,
                    imageName = debug.imageName,
                    containerId = "",
                    address = "",
                    token = "",
                    buildEnv = debug.buildEnv,
                    registryUser = debug.registryUser,
                    registryPwd = debug.registryPwd,
                    imageType = debug.imageType
                )
            )
        } finally {
            stopWatch.start("unlock")
            redisLock.unlock()
            stopWatch.stop()
            logger.info("[$hostTag|$grayFlag]|Start_Docker_Debug| $message| watch=$stopWatch")
        }
    }

    fun reportContainerId(pipelineId: String, vmSeqId: String, containerId: String): Result<Boolean>? {
        logger.info("Docker host debug report containerId, pipelineId:$pipelineId, vmSeqId:$vmSeqId, containerId:$containerId")

        pipelineDockerDebugDao.updateContainerId(dslContext, pipelineId, vmSeqId, containerId)

        return Result(0, "success", true)
    }

    fun rollbackDebug(pipelineId: String, vmSeqId: String, shutdown: Boolean?, message: String?): Result<Boolean>? {
        val stopWatch = StopWatch()
        var message1 = ""
        logger.info("Rollback build, pipelineId:$pipelineId, vmSeqId:$vmSeqId")

        val redisLock = DockerHostDebugLock(redisOperation)
        try {
            stopWatch.start("tryLock")
            val tryLock = redisLock.tryLock(timeout = 4000)
            stopWatch.stop()
            if (!tryLock) {
                message1 = "try lock fail in ${stopWatch.lastTaskTimeMillis}"
                return Result(status = 1, message = message1)
            }

            stopWatch.start("getDebugTasks")
            val debugTask = pipelineDockerDebugDao.getDebug(dslContext, pipelineId, vmSeqId)
            stopWatch.stop()
            if (debugTask == null) {
                message1 = "The debug task not exists, pipelineId:$pipelineId, vmSeqId:$vmSeqId"
                return Result(status = 1, message = message1)
            }
            if (true == shutdown) {
                message1 = "Roll back debug failed, finish."

                if (!message.isNullOrBlank()) {
                    stopWatch.start("setRedisDebugMsg")
                    redisUtils.setRedisDebugMsg(pipelineId = pipelineId, vmSeqId = vmSeqId, msg = message!!)
                    stopWatch.stop()
                }

                stopWatch.start("deleteDebug")
                pipelineDockerDebugDao.deleteDebug(dslContext = dslContext, id = debugTask.id)
                stopWatch.stop()
                return Result(status = 1, message = message1)
            }

            stopWatch.start("getHost")
            // 固定构建机的场景，则直接失败，不用回滚
            val dockerHost = pipelineDockerHostDao.getHost(dslContext, debugTask.projectId)
            stopWatch.stop()
            if (null != dockerHost) {
                logger.info("DockerHost is not null, rollback failed, shutdown the build! projectId: ${debugTask.projectId}, " +
                    "pipelineId: ${debugTask.pipelineId}, vmSeqId: ${debugTask.vmSeqId}")

                message1 = "固定的Docker构建机启动调试异常，IP：${dockerHost.hostIp}, projectId: ${debugTask.projectId}, vmSeqId: ${debugTask.vmSeqId}"
                AlertUtils.doAlert(
                    level = AlertLevel.HIGH,
                    title = "Docker构建机启动调试异常",
                    message = message1
                )

                return Result(status = 0, message = message1)
            }

            if (debugTask.status == PipelineTaskStatus.RUNNING.status) {
                stopWatch.start("v")
                pipelineDockerDebugDao.updateStatusAndTag(
                    dslContext = dslContext,
                    pipelineId = pipelineId,
                    vmSeqId = vmSeqId,
                    status = PipelineTaskStatus.QUEUE,
                    hostTag = ""
                )
                stopWatch.stop()
                AlertUtils.doAlert(
                    level = AlertLevel.LOW,
                    title = "Docker构建机启动调试异常",
                    message = "Docker构建机启动调试异常，任务已重试，异常ip: ${debugTask.hostTag}, projectId: ${debugTask.projectId}, vmSeqId: ${debugTask.vmSeqId}"
                )
            }
        } finally {
            stopWatch.start("unlock")
            redisLock.unlock()
            stopWatch.stop()
            logger.info("[$pipelineId]|rollbackDebugs|vmSeqId=$vmSeqId| $message1| watch=$stopWatch")
        }

        return Result(status = 0, message = "success", data = true)
    }

    fun endDebug(hostTag: String): Result<ContainerInfo>? {
        val stopWatch = StopWatch()
        var message = ""
        val redisLock = DockerHostDebugLock(redisOperation)
        try {
            stopWatch.start("tryLock")
            val tryLock = redisLock.tryLock(timeout = 4000)
            stopWatch.stop()
            if (!tryLock) {
                message = "try lock fail in ${stopWatch.lastTaskTimeMillis}"
                return Result(status = 1, message = message)
            }
            stopWatch.start("getDoneDebug")
            val debugTask = pipelineDockerDebugDao.getDoneDebug(dslContext, hostTag)
            stopWatch.stop()
            if (debugTask.isEmpty()) {
                message = "no task to end"
                return Result(status = 1, message = message)
            }
            val debug = debugTask[0]
            logger.info("End the docker debug(${debug.pipelineId}) seq(${debug.vmSeqId})")
            stopWatch.start("deleteDebug")
            pipelineDockerDebugDao.deleteDebug(dslContext = dslContext, id = debug.id)
            stopWatch.stop()
            return Result(
                status = 0,
                message = "success",
                data = ContainerInfo(
                    projectId = debug.projectId,
                    pipelineId = debug.pipelineId,
                    vmSeqId = debug.vmSeqId,
                    poolNo = 0,
                    status = debug.status,
                    imageName = debug.imageName,
                    containerId = debug.containerId,
                    address = debug.hostTag,
                    token = debug.token,
                    buildEnv = debug.buildEnv,
                    registryUser = debug.registryUser,
                    registryPwd = debug.registryPwd,
                    imageType = debug.imageType
                )
            )
        } finally {
            stopWatch.start("unlock")
            redisLock.unlock()
            stopWatch.stop()
            logger.info("[$hostTag]|endDebug| $message| watch=$stopWatch")
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
                logger.info("There is ${timeoutDebugTask.size} debug task have/has already time out, clear it.")
                stopWatch.start("updateTimeOutDebugTask")
                for (i in timeoutDebugTask.indices) {
                    logger.info("Delete timeout debug task, pipelineId:(${timeoutDebugTask[i].pipelineId}), vmSeqId:(${timeoutDebugTask[i].vmSeqId}), containerId:(${timeoutDebugTask[i].containerId})")
                    try {
                        deleteDebug(timeoutDebugTask[i].pipelineId, timeoutDebugTask[i].vmSeqId)
                    } catch (e: Exception) {
                        logger.warn("Delete timeout debug task failed, ${e.message}")
                    }
                }
                stopWatch.stop()
                message = "timeoutDebugTask.size=${timeoutDebugTask.size}"
            }
        } finally {
            stopWatch.start("unlock")
            redisLock.unlock()
            stopWatch.stop()
            logger.info("resetHostTag| $message| watch=$stopWatch")
        }
    }

    // FIXME 需要记录如果是从某个构建ID启动的调试必须不允许漂移，另起issue处理
    @Scheduled(initialDelay = 90 * 1000, fixedDelay = 60 * 1000)
    fun resetHostTag() {
        val stopWatch = StopWatch()
        var message = ""
        val redisLock = DockerHostDebugLock(redisOperation)
        try {
            stopWatch.start("lock")
            redisLock.lock()
            stopWatch.stop()
            stopWatch.start("getUnclaimedHostDebug")
            val unclaimedDebugTask = pipelineDockerDebugDao.getUnclaimedHostDebug(dslContext)
            stopWatch.stop()
            if (unclaimedDebugTask.isNotEmpty) {
                stopWatch.start("deleteDockerBuildLastHost")
                logger.info("There is ${unclaimedDebugTask.size} build task have/has queued for a long time, clear hostTag.")
                for (i in unclaimedDebugTask.indices) {
                    logger.info("clear hostTag, pipelineId:(${unclaimedDebugTask[i].pipelineId}), vmSeqId:(${unclaimedDebugTask[i].vmSeqId})")
                    redisUtils.deleteDockerBuildLastHost(unclaimedDebugTask[i].pipelineId, unclaimedDebugTask[i].vmSeqId)
                }
                stopWatch.stop()
                stopWatch.start("clearHostTagForUnclaimedHostDebug")
                pipelineDockerDebugDao.clearHostTagForUnclaimedHostDebug(dslContext)
                stopWatch.stop()
                message = "unClaimedDebugTask.size=${unclaimedDebugTask.size}"
            }
        } finally {
            stopWatch.start("unlock")
            redisLock.unlock()
            stopWatch.stop()
            logger.info("resetHostTag| $message| watch=$stopWatch")
        }
    }

    @Scheduled(initialDelay = 90 * 1000, fixedDelay = 100 * 1000)
    fun resetZone() {
        val stopWatch = StopWatch()
        var message = ""
        val redisLock = DockerHostDebugLock(redisOperation)
        try {
            stopWatch.start("lock")
            redisLock.lock()
            stopWatch.stop()
            stopWatch.start("getUnclaimedZoneDebug")
            val unclaimedDebugTask = pipelineDockerDebugDao.getUnclaimedZoneDebug(dslContext)
            stopWatch.stop()
            if (unclaimedDebugTask.isNotEmpty) {
                logger.info("There is ${unclaimedDebugTask.size} build task have/has queued for a long time, clear zone.")
                stopWatch.start("deleteDockerBuildLastHost")
                for (i in unclaimedDebugTask.indices) {
                    logger.info("clear zone, pipelineId:(${unclaimedDebugTask[i].pipelineId}), vmSeqId:(${unclaimedDebugTask[i].vmSeqId})")
                    redisUtils.deleteDockerBuildLastHost(unclaimedDebugTask[i].pipelineId, unclaimedDebugTask[i].vmSeqId)
                }
                stopWatch.stop()
                stopWatch.start("resetZoneForUnclaimedZoneDebug")
                pipelineDockerDebugDao.resetZoneForUnclaimedZoneDebug(dslContext)
                stopWatch.stop()
                message = "unClaimedDebugTask.size=${unclaimedDebugTask.size}"
            }
        } finally {
            stopWatch.start("unlock")
            redisLock.unlock()
            stopWatch.stop()
            logger.info("resetZone| $message| watch=$stopWatch")
        }
    }

    fun cleanIp(projectId: String, pipelineId: String, vmSeqId: String): Result<Boolean> {
        logger.info("clean pipeline docker build ip, projectId:$projectId, pipelineId:$pipelineId, vmSeqId:$vmSeqId")
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
        private val logger = LoggerFactory.getLogger(DockerHostDebugService::class.java)
    }
}
