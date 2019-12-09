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

package com.tencent.devops.dispatch.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.common.web.mq.alert.AlertUtils
import com.tencent.devops.dispatch.dao.PipelineDockerDebugDao
import com.tencent.devops.dispatch.dao.PipelineDockerEnableDao
import com.tencent.devops.dispatch.dao.PipelineDockerHostDao
import com.tencent.devops.dispatch.pojo.ContainerInfo
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.utils.CommonUtils
import com.tencent.devops.dispatch.utils.DockerHostDebugLock
import com.tencent.devops.dispatch.utils.redis.RedisUtils
import com.tencent.devops.store.pojo.image.exception.UnknownImageType
import com.tencent.devops.store.pojo.image.response.ImageRepoInfo
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class DockerHostDebugService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineDockerDebugDao: PipelineDockerDebugDao,
    private val pipelineDockerHostDao: PipelineDockerHostDao,
    private val pipelineDockerEnableDao: PipelineDockerEnableDao,
    private val redisUtils: RedisUtils,
    private val redisOperation: RedisOperation,
    private val client: Client,
    private val storeImageService: StoreImageService
) {

    @Value("\${dispatch.dockerBuildImagePrefix:#{null}}")
    val dockerBuildImagePrefix: String? = null

    @Value("\${project.gray:#{null}}")
    private val grayFlag: String? = null
    private val redisKey = "project:setting:gray" // 灰度项目列表存在redis的标识key

    private val TLINUX1_2_IMAGE = "/bkdevops/docker-builder1.2:v1"
    private val TLINUX2_2_IMAGE = "/bkdevops/docker-builder2.2:v1"

    fun insertDebug(
        userId: String,
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        imageCode: String?,
        imageVersion: String?,
        imageName: String?,
        buildEnvStr: String,
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
                imageCode = imageCode,
                imageVersion = imageVersion,
                defaultPrefix = dockerBuildImagePrefix
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
                DockerVersion.TLINUX1_2.value -> dockerBuildImagePrefix + TLINUX1_2_IMAGE
                DockerVersion.TLINUX2_2.value -> dockerBuildImagePrefix + TLINUX2_2_IMAGE
                else -> "$dockerBuildImagePrefix/$imageName"
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

        val dockerHost = pipelineDockerHostDao.getHost(dslContext, projectId)
        val lastHostIp = redisUtils.getDockerBuildLastHost(pipelineId, vmSeqId)
        val hostTag = when {
            null != dockerHost -> {
                logger.info("Fixed debug host machine, hostIp:${dockerHost.hostIp}, pipelineId:$pipelineId")
                dockerHost.hostIp
            }
            null != lastHostIp -> {
                logger.info("Use last build hostIp: $lastHostIp, pipelineId:$pipelineId")
                lastHostIp
            }
            else -> ""
        }

        pipelineDockerDebugDao.insertDebug(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            vmSeqId = vmSeqId,
            status = PipelineTaskStatus.QUEUE,
            token = "",
            imageName = dockerImage,
            hostTag = hostTag,
            buildEnv = buildEnvStr,
            registryUser = userName,
            registryPwd = password,
            imageType = when (imageType) {
                null -> ImageType.BKDEVOPS.type
                ImageType.THIRD -> imageType!!.type
                ImageType.BKDEVOPS -> ImageType.BKDEVOPS.type
                ImageType.BKSTORE -> imageRepoInfo!!.sourceType.type
                else -> throw UnknownImageType("imageCode:$imageCode,imageVersion:$imageVersion,imageType:$imageType")
            }
        )
    }

    fun deleteDebug(pipelineId: String, vmSeqId: String): Result<Boolean> {
        logger.info("Delete docker debug  pipelineId:($pipelineId), vmSeqId:($vmSeqId)")

        // 状态标记为完成即可
        pipelineDockerDebugDao.updateStatus(dslContext, pipelineId, vmSeqId, PipelineTaskStatus.DONE)
        return Result(0, "success")
    }

    fun getDebugStatus(pipelineId: String, vmSeqId: String): Result<ContainerInfo> {
        val debugTask = pipelineDockerDebugDao.getDebug(dslContext, pipelineId, vmSeqId)
        if (null == debugTask) {
            logger.warn("The debug task not exists, pipelineId:$pipelineId, vmSeqId:$vmSeqId")
            val msg = redisUtils.getRedisDebugMsg(pipelineId, vmSeqId)
            return Result(1, "登录调试失败,请检查镜像是否合法或重试。" + if (!msg.isNullOrBlank()) {
                "错误信息: $msg"
            } else {
                ""
            })
        }

        return Result(0, "success", ContainerInfo(debugTask.projectId, debugTask.pipelineId, debugTask.vmSeqId,
            debugTask.status, debugTask.imageName, debugTask.containerId ?: "", debugTask.hostTag
            ?: "", "", debugTask.buildEnv,
            debugTask.registryUser, debugTask.registryPwd, debugTask.imageType))
    }

    fun startDebug(hostTag: String): Result<ContainerInfo>? {
        val redisLock = DockerHostDebugLock(redisOperation)
        try {
            val gray = !grayFlag.isNullOrBlank() && grayFlag!!.toBoolean()
            val grayProjectSet = redisOperation.getSetMembers(redisKey)?.filter { !it.isBlank() }
                ?.toSet() ?: emptySet()
            logger.info("gray environment: $gray")
            redisLock.lock()
            if (gray) {
                // 优先取设置了IP的任务（可能是固定构建机，也可能是上次用的构建机）
                var debugTask = pipelineDockerDebugDao.getQueueDebugByProj(dslContext, grayProjectSet, hostTag)
                if (debugTask.isNotEmpty) {
                    logger.info("Start docker debug with hostTag: $hostTag")
                } else {
                    debugTask = pipelineDockerDebugDao.getQueueDebugByProj(dslContext, grayProjectSet)
                }

                if (debugTask.isEmpty()) {
                    return Result(1, "no debug task in queue")
                }
                val debug = debugTask[0]
                logger.info("Start the docker debug (${debug.pipelineId}) seq(${debug.vmSeqId})")
                pipelineDockerDebugDao.updateStatusAndTag(dslContext, debug.pipelineId, debug.vmSeqId, PipelineTaskStatus.RUNNING, hostTag)
                return Result(0, "success", ContainerInfo(debug.projectId, debug.pipelineId, debug.vmSeqId, PipelineTaskStatus.RUNNING.status, debug.imageName,
                    "", "", "", debug.buildEnv, debug.registryUser, debug.registryPwd, debug.imageType))
            } else {
                // 优先取设置了IP的任务（可能是固定构建机，也可能是上次用的构建机）
                var debugTask = pipelineDockerDebugDao.getQueueDebugExcludeProj(dslContext, grayProjectSet, hostTag)
                if (debugTask.isNotEmpty) {
                    logger.info("Start docker debug with hostTag: $hostTag")
                } else {
                    debugTask = pipelineDockerDebugDao.getQueueDebugExcludeProj(dslContext, grayProjectSet)
                }

                if (debugTask.isEmpty()) {
                    return Result(1, "no debug task in queue")
                }
                val debug = debugTask[0]
                logger.info("Start the docker debug (${debug.pipelineId}) seq(${debug.vmSeqId})")
                pipelineDockerDebugDao.updateStatusAndTag(dslContext, debug.pipelineId, debug.vmSeqId, PipelineTaskStatus.RUNNING, hostTag)
                return Result(0, "success", ContainerInfo(debug.projectId, debug.pipelineId, debug.vmSeqId, PipelineTaskStatus.RUNNING.status, debug.imageName,
                    "", "", "", debug.buildEnv, debug.registryUser, debug.registryPwd, debug.imageType))
            }
        } finally {
            redisLock.unlock()
        }
    }

    fun reportContainerId(pipelineId: String, vmSeqId: String, containerId: String): Result<Boolean>? {
        logger.info("Docker host debug report containerId, pipelineId:$pipelineId, vmSeqId:$vmSeqId, containerId:$containerId")

        pipelineDockerDebugDao.updateContainerId(dslContext, pipelineId, vmSeqId, containerId)

        return Result(0, "success", true)
    }

    fun rollbackDebug(pipelineId: String, vmSeqId: String, shutdown: Boolean?, message: String?): Result<Boolean>? {
        logger.info("Rollback build, pipelineId:$pipelineId, vmSeqId:$vmSeqId")

        val redisLock = DockerHostDebugLock(redisOperation)
        try {
            redisLock.lock()
            val debugTask = pipelineDockerDebugDao.getDebug(dslContext, pipelineId, vmSeqId)
            if (debugTask == null) {
                logger.warn("The debug task not exists, pipelineId:$pipelineId, vmSeqId:$vmSeqId")
                return Result(1, "Debug not exists")
            }
            if (true == shutdown) {
                logger.error("Roll back debug failed, finish.")
                if (!message.isNullOrBlank()) {
                    redisUtils.setRedisDebugMsg(pipelineId, vmSeqId, message!!)
                }
                pipelineDockerDebugDao.deleteDebug(dslContext, debugTask.id)
                return Result(1, "Debug failed.")
            }

            // 固定构建机的场景，则直接失败，不用回滚
            val dockerHost = pipelineDockerHostDao.getHost(dslContext, debugTask.projectId)
            if (null != dockerHost) {
                logger.info("DockerHost is not null, rollback failed, shutdown the build! projectId: ${debugTask.projectId}, " +
                    "pipelineId: ${debugTask.pipelineId}, vmSeqId: ${debugTask.vmSeqId}")

                AlertUtils.doAlert(AlertLevel.HIGH, "Docker构建机启动调试异常", "固定的Docker构建机启动调试异常，IP：${dockerHost.hostIp}, " +
                    "projectId: ${debugTask.projectId}, vmSeqId: ${debugTask.vmSeqId}")
                return Result(0, "Rollback task finished")
            }

            if (debugTask.status == PipelineTaskStatus.RUNNING.status) {
                pipelineDockerDebugDao.updateStatusAndTag(dslContext, pipelineId, vmSeqId, PipelineTaskStatus.QUEUE, "")
                AlertUtils.doAlert(AlertLevel.LOW, "Docker构建机启动调试异常", "Docker构建机启动调试异常，任务已重试，异常ip: ${debugTask.hostTag}, " +
                    "projectId: ${debugTask.projectId}, vmSeqId: ${debugTask.vmSeqId}")
            }
        } finally {
            redisLock.unlock()
        }

        return Result(0, "success", true)
    }

    fun endDebug(hostTag: String): Result<ContainerInfo>? {
        val redisLock = DockerHostDebugLock(redisOperation)
        try {
            redisLock.lock()
            val debugTask = pipelineDockerDebugDao.getDoneDebug(dslContext, hostTag)
            if (debugTask.isEmpty()) {
                return Result(1, "no task to end")
            }
            val debug = debugTask[0]
            logger.info("End the docker debug(${debug.pipelineId}) seq(${debug.vmSeqId})")
            pipelineDockerDebugDao.deleteDebug(dslContext, debug.id)
            return Result(0, "success", ContainerInfo(debug.projectId, debug.pipelineId, debug.vmSeqId,
                debug.status, debug.imageName, debug.containerId, debug.hostTag, debug.token, debug.buildEnv, debug.registryUser, debug.registryPwd, debug.imageType))
        } finally {
            redisLock.unlock()
        }
    }

    @Scheduled(initialDelay = 45 * 1000, fixedDelay = 600 * 1000)
    fun clearTimeoutDebugTask() {
        val redisLock = DockerHostDebugLock(redisOperation)
        try {
            redisLock.lock()
            val timeoutDebugTask = pipelineDockerDebugDao.getTimeOutDebugTask(dslContext)
            if (timeoutDebugTask.isNotEmpty) {
                logger.info("There is ${timeoutDebugTask.size} debug task have/has already time out, clear it.")
                for (i in timeoutDebugTask.indices) {
                    logger.info("clear pipelineId:(${timeoutDebugTask[i].pipelineId}), vmSeqId:(${timeoutDebugTask[i].vmSeqId}), containerId:(${timeoutDebugTask[i].containerId})")
                }
                pipelineDockerDebugDao.updateTimeOutDebugTask(dslContext)
            }
        } finally {
            redisLock.unlock()
        }
    }

    @Scheduled(initialDelay = 90 * 1000, fixedDelay = 60 * 1000)
    fun resetHostTag() {
        val redisLock = DockerHostDebugLock(redisOperation)
        try {
            redisLock.lock()
            val unclaimedDebugTask = pipelineDockerDebugDao.getUnclaimedHostDebug(dslContext)
            if (unclaimedDebugTask.isNotEmpty) {
                logger.info("There is ${unclaimedDebugTask.size} build task have/has queued for a long time, clear hostTag.")
                for (i in unclaimedDebugTask.indices) {
                    logger.info("clear hostTag, pipelineId:(${unclaimedDebugTask[i].pipelineId}), vmSeqId:(${unclaimedDebugTask[i].vmSeqId})")
                    redisUtils.deleteDockerBuildLastHost(unclaimedDebugTask[i].pipelineId, unclaimedDebugTask[i].vmSeqId)
                }
                pipelineDockerDebugDao.clearHostTagForUnclaimedHostDebug(dslContext)
            }
        } finally {
            redisLock.unlock()
        }
    }

    @Scheduled(initialDelay = 90 * 1000, fixedDelay = 100 * 1000)
    fun resetZone() {
        val redisLock = DockerHostDebugLock(redisOperation)
        try {
            redisLock.lock()
            val unclaimedDebugTask = pipelineDockerDebugDao.getUnclaimedZoneDebug(dslContext)
            if (unclaimedDebugTask.isNotEmpty) {
                logger.info("There is ${unclaimedDebugTask.size} build task have/has queued for a long time, clear zone.")
                for (i in unclaimedDebugTask.indices) {
                    logger.info("clear zone, pipelineId:(${unclaimedDebugTask[i].pipelineId}), vmSeqId:(${unclaimedDebugTask[i].vmSeqId})")
                    redisUtils.deleteDockerBuildLastHost(unclaimedDebugTask[i].pipelineId, unclaimedDebugTask[i].vmSeqId)
                }
                pipelineDockerDebugDao.resetZoneForUnclaimedZoneDebug(dslContext)
            }
        } finally {
            redisLock.unlock()
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