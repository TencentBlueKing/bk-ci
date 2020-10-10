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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.common.api.util.ApiUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.web.mq.alert.AlertLevel.HIGH
import com.tencent.devops.common.web.mq.alert.AlertLevel.LOW
import com.tencent.devops.common.web.mq.alert.AlertUtils
import com.tencent.devops.dispatch.docker.client.DockerHostClient
import com.tencent.devops.dispatch.docker.config.DefaultImageConfig
import com.tencent.devops.dispatch.docker.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerEnableDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerHostDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerHostZoneDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerPoolDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerTaskDao
import com.tencent.devops.dispatch.pojo.ContainerInfo
import com.tencent.devops.dispatch.docker.pojo.DockerHostBuildInfo
import com.tencent.devops.dispatch.docker.pojo.DockerHostInfo
import com.tencent.devops.dispatch.docker.pojo.enums.DockerHostType
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.pojo.redis.RedisBuild
import com.tencent.devops.dispatch.docker.utils.CommonUtils
import com.tencent.devops.dispatch.docker.utils.DockerHostLock
import com.tencent.devops.dispatch.docker.utils.DockerHostUtils
import com.tencent.devops.dispatch.docker.utils.DockerUtils
import com.tencent.devops.dispatch.utils.redis.RedisUtils
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerBuildRecord
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.VmInfo
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import com.tencent.devops.process.pojo.mq.PipelineBuildLessDockerShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineBuildLessDockerStartupEvent
import com.tencent.devops.process.pojo.mq.PipelineBuildLessStartupDispatchEvent
import com.tencent.devops.store.api.image.service.ServiceStoreImageResource
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import com.tencent.devops.store.pojo.image.exception.UnknownImageType
import com.tencent.devops.store.pojo.image.response.ImageRepoInfo
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch

@Service
class DockerHostBuildService @Autowired constructor(
    private val dslContext: DSLContext,
    private val dockerHostClient: DockerHostClient,
    private val pipelineDockerEnableDao: PipelineDockerEnableDao,
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val pipelineDockerTaskDao: PipelineDockerTaskDao,
    private val pipelineDockerPoolDao: PipelineDockerPoolDao,
    private val pipelineDockerHostDao: PipelineDockerHostDao,
    private val pipelineDockerHostZoneDao: PipelineDockerHostZoneDao,
    private val pipelineDockerIPInfoDao: PipelineDockerIPInfoDao,
    private val redisUtils: RedisUtils,
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val gray: Gray,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val dockerHostUtils: DockerHostUtils,
    private val buildLogPrinter: BuildLogPrinter,
    private val defaultImageConfig: DefaultImageConfig
) {

    private val grayFlag: Boolean = gray.isGray()

    fun enable(pipelineId: String, vmSeqId: Int?, enable: Boolean) =
        pipelineDockerEnableDao.enable(dslContext, pipelineId, vmSeqId, enable)

    fun dockerHostBuild(event: PipelineAgentStartupEvent) {
        logger.info("Start docker host build ($event)}")
        val dispatchType = event.dispatchType as DockerDispatchType
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val poolNo = dockerHostUtils.getIdlePoolNo(event.pipelineId, event.vmSeqId)
            val secretKey = ApiUtil.randomSecretKey()
            val id = pipelineDockerBuildDao.startBuild(
                dslContext = context,
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
                dockerIp = "",
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
            logger.info("secretKey: $secretKey")
            logger.info("agentId: $agentId")

            logger.info("dockerHostBuild:(${event.userId},${event.projectId},${event.pipelineId},${event.buildId},${dispatchType.imageType?.name},${dispatchType.imageCode},${dispatchType.imageVersion},${dispatchType.credentialId},${dispatchType.credentialProject})")
            // 插入dockerTask表，等待dockerHost进程过来轮询
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
                        logger.warn("dockerHostBuild:credentialProject=nullOrBlank,buildId=${event.buildId},credentialId=${dispatchType.credentialId}")
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

            // 如果固定构建机的表中设置了该项目的母机IP，则把该母机IP也写入dockerTask表
//            val dockerHost = pipelineDockerHostDao.getHost(dslContext, event.projectId)
//            val lastHostIp = redisUtils.getDockerBuildLastHost(event.pipelineId, event.vmSeqId)
//            val hostTag = when {
//                null != dockerHost -> {
//                    logger.info("Fixed build host machine, hostIp:${dockerHost.hostIp}")
//                    dockerHost.hostIp
//                }
//                null != lastHostIp -> {
//                    logger.info("Use last build hostIp: $lastHostIp")
//                    val lastHostZone = pipelineDockerHostZoneDao.getHostZone(dslContext, lastHostIp)
//                    if (null != lastHostZone && (event.zone != Zone.valueOf(lastHostZone.zone))) {
//                        logger.info("Last build hostIp zone is different with buildMessage.zone, so clean hostTag")
//                        ""
//                    } else {
//                        lastHostIp
//                    }
//                }
//                else -> ""
//            }
            // 当专用构建机不允许柔性处理，只能使用专用的构建机进行处理
            val dockerHosts = pipelineDockerHostDao.getHostIps(dslContext, event.projectId)
            val lastHostIp = redisUtils.getDockerBuildLastHost(event.pipelineId, event.vmSeqId)
            val hostTag =
                DockerUtils.getDockerHostIp(dockerHosts = dockerHosts, lastHostIp = lastHostIp, buildId = event.buildId)

            pipelineDockerTaskDao.insertTask(
                dslContext = context,
                projectId = event.projectId,
                agentId = agentId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                vmSeqId = event.vmSeqId.toInt(),
                status = PipelineTaskStatus.QUEUE,
                secretKey = secretKey,
                imageName = dockerImage!!.trim(),
                hostTag = hostTag,
                channelCode = event.channelCode,
                zone = if (null == event.zone) {
                    Zone.SHENZHEN.name
                } else {
                    event.zone!!.name
                },
                registryUser = userName,
                registryPwd = password,
                imageType = when {
                    null == dispatchType.imageType -> ImageType.BKDEVOPS.type
                    ImageType.THIRD == dispatchType.imageType -> dispatchType.imageType!!.type
                    ImageType.BKDEVOPS == dispatchType.imageType -> ImageType.BKDEVOPS.type
                    else -> throw UnknownImageType("imageCode:${dispatchType.imageCode},imageVersion:${dispatchType.imageVersion},imageType:${dispatchType.imageType}")
                },
                imagePublicFlag = dispatchType.imagePublicFlag,
                imageRDType = if (dispatchType.imageRDType == null) {
                    null
                } else {
                    ImageRDTypeEnum.getImageRDTypeByName(dispatchType.imageRDType!!)
                },
                containerHashId = event.containerHashId
            )

            saveDockerInfoToBuildDetail(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                vmSeqId = event.vmSeqId,
                dockerImage = dockerImage
            )
        }
    }

    private fun saveDockerInfoToBuildDetail(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        dockerImage: String

    ) {
        client.get(ServiceBuildResource::class).saveBuildVmInfo(
            projectId,
            pipelineId,
            buildId,
            vmSeqId,
            VmInfo("", DockerUtils.parseShortImage(dockerImage))
        )
    }

    fun finishDockerBuild(event: PipelineAgentShutdownEvent) {
        logger.info("Finish docker build of buildId(${event.buildId}) and vmSeqId(${event.vmSeqId}) with result(${event.buildResult})")
        if (event.vmSeqId.isNullOrBlank()) {
            val record = pipelineDockerBuildDao.listBuilds(dslContext, event.buildId)
            if (record.isEmpty()) {
                return
            }
            record.forEach {
                finishDockerBuild(it, event)
            }
        } else {
            val record = pipelineDockerBuildDao.getBuild(dslContext, event.buildId, event.vmSeqId!!.toInt())
            if (record != null) {
                finishDockerBuild(record, event)
            }
        }
    }

    private fun finishDockerBuild(record: TDispatchPipelineDockerBuildRecord, event: PipelineAgentShutdownEvent) {
        try {
            if (record.dockerIp.isNotEmpty()) {
                dockerHostClient.endBuild(
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    buildId = event.buildId,
                    vmSeqId = event.vmSeqId?.toInt() ?: 0,
                    containerId = record.containerId,
                    dockerIp = record.dockerIp
                )
            }

            // 只要当容器关机成功时才会更新build_history状态
            finishBuild(record, event.buildResult)
        } catch (e: Exception) {
            logger.error("Finish docker build of buildId(${event.buildId}) and vmSeqId(${event.vmSeqId}) with result(${event.buildResult}) get error", e)
        } finally {
            // 编译环境才会更新pool，无论下发关机接口成功与否，都会置pool为空闲
            pipelineDockerPoolDao.updatePoolStatus(
                dslContext,
                record.pipelineId,
                record.vmSeqId.toString(),
                record.poolNo,
                PipelineTaskStatus.DONE.status
            )
        }
    }

    private fun finishBuild(record: TDispatchPipelineDockerBuildRecord, success: Boolean) {
        logger.info("Finish the docker build(${record.buildId}) with result($success)")
        try {
            pipelineDockerBuildDao.updateStatus(dslContext,
                record.buildId,
                record.vmSeqId,
                if (success) PipelineTaskStatus.DONE else PipelineTaskStatus.FAILURE)

            // 更新dockerTask表(保留之前逻辑)
            pipelineDockerTaskDao.updateStatus(dslContext,
                record.buildId,
                record.vmSeqId,
                if (success) PipelineTaskStatus.DONE else PipelineTaskStatus.FAILURE)
            redisUtils.deleteDockerBuild(record.id, SecurityUtil.decrypt(record.secretKey))
            redisUtils.deleteHeartBeat(record.buildId, record.vmSeqId.toString())
        } catch (e: Exception) {
            logger.error("Finish the docker build(${record.buildId}) error.", e)
        }
    }

    fun startBuild(hostTag: String): Result<DockerHostBuildInfo>? {
        val stopWatch = StopWatch()
        stopWatch.start("fetchHostZone")
        val hostZone = pipelineDockerHostZoneDao.getHostZone(dslContext, hostTag)
        stopWatch.stop()
        var message = ""
        val redisLock = DockerHostLock(redisOperation)
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

            val tasks = if (grayFlag) {
                stopWatch.start("grayQueueTask")
                // 优先取设置了IP的任务（可能是固定构建机，也可能是上次用的构建机）
                var task = pipelineDockerTaskDao.getQueueTasksByProj(dslContext, grayProjectSet, hostTag)
                if (task.isNotEmpty) {
                    logger.info("[$hostTag|$grayFlag] Start docker build with hostIp: $hostTag")
                } else if (hostZone != null) { // 再按区域取值
                    task = pipelineDockerTaskDao.getQueueTasksByProj(dslContext, grayProjectSet, Zone.valueOf(hostZone.zone))
                    if (task.isNotEmpty) {
                        logger.info("[$hostTag|$grayFlag] Start docker build with zone: ${hostZone.zone}")
                    } else { // 最后随机取
                        task = pipelineDockerTaskDao.getQueueTasksByProj(dslContext, grayProjectSet)
                    }
                } else { // 客户端的区域为空，则随机取
                    task = pipelineDockerTaskDao.getQueueTasksByProj(dslContext, grayProjectSet)
                }
                stopWatch.stop()
                task
            } else {
                stopWatch.start("prodQueueTask")
                // 优先取设置了IP的任务（可能是固定构建机，也可能是上次用的构建机）
                var task = pipelineDockerTaskDao.getQueueTasksExcludeProj(dslContext, grayProjectSet, hostTag)
                if (task.isNotEmpty) {
                    logger.info("[$hostTag|$grayFlag] Start docker build with hostIp: $hostTag")
                } else if (hostZone != null) { // 再按区域取值
                    task = pipelineDockerTaskDao.getQueueTasksExcludeProj(dslContext, grayProjectSet, Zone.valueOf(hostZone.zone))
                    if (task.isNotEmpty) {
                        logger.info("[$hostTag|$grayFlag] Start docker build with zone: ${hostZone.zone}")
                    } else { // 最后随机取
                        task = pipelineDockerTaskDao.getQueueTasksExcludeProj(dslContext, grayProjectSet)
                    }
                } else { // 客户端的区域为空，则随机取
                    task = pipelineDockerTaskDao.getQueueTasksExcludeProj(dslContext, grayProjectSet)
                }
                stopWatch.stop()
                task
            }

            if (tasks.isEmpty()) {
                message = "No task in queue"
                return Result(status = 1, message = message)
            }
            val build = tasks[0]
            stopWatch.start("updateStatusAndTag")
            pipelineDockerTaskDao.updateStatusAndTag(
                dslContext = dslContext,
                buildId = build.buildId,
                vmSeqId = build.vmSeqId,
                status = PipelineTaskStatus.RUNNING,
                hostTag = hostTag
            )
            stopWatch.stop()
            stopWatch.start("setDockerBuildLastHost")
            redisUtils.setDockerBuildLastHost(
                pipelineId = build.pipelineId,
                vmSeqId = build.vmSeqId.toString(),
                hostIp = hostTag
            ) // 将本次构建使用的主机IP写入redis，以方便下次直接用这台IP
            stopWatch.stop()
            message = "buildId=${build.buildId}| vmSeqId=${build.vmSeqId}| imageName=${build.imageName}"
            return Result(0, "success", DockerHostBuildInfo(
                projectId = build.projectId,
                agentId = build.agentId,
                pipelineId = build.pipelineId,
                buildId = build.buildId,
                vmSeqId = build.vmSeqId,
                secretKey = build.secretKey,
                status = PipelineTaskStatus.RUNNING.status,
                imageName = build.imageName,
                containerId = "",
                wsInHost = false,
                poolNo = 0,
                registryUser = build.registryUser,
                registryPwd = build.registryPwd,
                imageType = build.imageType,
                imagePublicFlag = build.imagePublicFlag,
                imageRDType = ImageRDTypeEnum.getImageRDTypeStr(build.imageRdType?.toInt()),
                containerHashId = build.containerHashId
            ))
        } finally {
            stopWatch.start("unlock")
            redisLock.unlock()
            stopWatch.stop()
            logger.info("[$hostTag|$grayFlag]|Start_Docker_Build| $message| watch=$stopWatch")
        }
    }

    fun reportContainerId(buildId: String, vmSeqId: Int, containerId: String, hostTag: String?): Result<Boolean>? {
        logger.info("[$buildId]|reportContainerId|vmSeqId=$vmSeqId|containerId=$containerId|hostTag=$hostTag")

        pipelineDockerTaskDao.updateContainerId(dslContext, buildId, vmSeqId, containerId, hostTag)

        return Result(0, "success", true)
    }

    fun rollbackBuild(buildId: String, vmSeqId: Int, shutdown: Boolean?): Result<Boolean>? {
        val stopWatch = StopWatch()
        var message = "nothing"
        val redisLock = DockerHostLock(redisOperation)
        try {
            stopWatch.start("tryLock")
            redisLock.tryLock(timeout = 4000)
            stopWatch.stop()
            stopWatch.start("fetchHostZone")
            val task = pipelineDockerTaskDao.getTask(dslContext, buildId, vmSeqId)
            stopWatch.stop()
            if (task == null) {
                message = "The build task not exists, buildId:$buildId, vmSeqId:$vmSeqId"
                return Result(status = 1, message = message)
            }
            // dockerhost上报失败，则直接失败，不用回滚;
            if (true == shutdown) {
                message = "构建镜像无法启动，projectId: ${task.projectId}, pipelineId: ${task.pipelineId},buildId: ${task.buildId}"
                stopWatch.start("shutdown_pipeline")
                client.get(ServiceBuildResource::class).serviceShutdown(
                    projectId = task.projectId,
                    pipelineId = task.pipelineId,
                    buildId = task.buildId,
                    channelCode = ChannelCode.valueOf(task.channelCode)
                )
                stopWatch.stop()
                AlertUtils.doAlert(level = HIGH, title = "Docker构建机启动任务异常", message = message)
                return Result(status = 0, message = message)
            }
            // 或固定构建机的场景，则直接失败，不用回滚; 其他场景需要回滚，并把hostTag清空，以供其他机器执行
            val dockerHost = pipelineDockerHostDao.getHost(dslContext, task.projectId)
            if (null != dockerHost) {
                message = "固定的Docker构建机启动任务异常，IP：${dockerHost.hostIp}, projectId: ${task.projectId}, pipelineId: ${task.pipelineId},buildId: ${task.buildId}"
                stopWatch.start("shutdown_pipeline")
                client.get(ServiceBuildResource::class).serviceShutdown(
                    projectId = task.projectId,
                    pipelineId = task.pipelineId,
                    buildId = task.buildId,
                    channelCode = ChannelCode.valueOf(task.channelCode)
                )
                stopWatch.stop()

                AlertUtils.doAlert(level = HIGH, title = "Docker构建机启动任务异常", message = message)
                return Result(status = 0, message = message)
            }

            if (task.status == PipelineTaskStatus.RUNNING.status) {
                stopWatch.start("updateStatusAndTag")
                pipelineDockerTaskDao.updateStatusAndTag(dslContext, buildId, vmSeqId, PipelineTaskStatus.QUEUE, "")
                stopWatch.stop()
                message = "Docker构建机启动任务异常，任务已重试，异常ip: ${task.hostTag}, projectId: ${task.projectId}, pipelineId: ${task.pipelineId},buildId: ${task.buildId}"
                AlertUtils.doAlert(level = LOW, title = "Docker构建机启动任务异常", message = message)
            }
        } finally {
            stopWatch.start("unlock")
            redisLock.unlock()
            stopWatch.stop()
            logger.info("[$buildId|$vmSeqId]|rollbackBuild| $message| watch=$stopWatch")
        }

        return Result(status = 0, message = "success", data = true)
    }

    fun endBuild(hostTag: String): Result<DockerHostBuildInfo>? {
        val stopWatch = StopWatch()
        var message = "nothing"
        val redisLock = DockerHostLock(redisOperation)
        try {
            stopWatch.start("tryLock")
            redisLock.tryLock(timeout = 4000)
            stopWatch.stop()
            stopWatch.start("getDoneTasks")
            val task = pipelineDockerTaskDao.getDoneTasks(dslContext, hostTag)
            stopWatch.stop()
            if (task.isEmpty()) {
                message = "no task to end"
                return Result(status = 1, message = message)
            }
            val build = task[0]
            logger.info("End the docker build(${build.buildId}) seq(${build.vmSeqId})")
            stopWatch.start("deleteTask")
            pipelineDockerTaskDao.deleteTask(dslContext = dslContext, id = build.id)
            stopWatch.stop()
            return Result(
                status = 0,
                message = "success",
                data = DockerHostBuildInfo(
                    projectId = build.projectId,
                    agentId = build.agentId,
                    pipelineId = build.pipelineId,
                    buildId = build.buildId,
                    vmSeqId = build.vmSeqId,
                    secretKey = build.secretKey,
                    status = build.status,
                    imageName = build.imageName,
                    containerId = build.containerId,
                    wsInHost = false,
                    poolNo = 0,
                    registryUser = build.registryUser,
                    registryPwd = build.registryPwd,
                    imageType = build.imageType,
                    imagePublicFlag = build.imagePublicFlag,
                    imageRDType = ImageRDTypeEnum.getImageRDTypeStr(build.imageRdType?.toInt()),
                    containerHashId = build.containerHashId
                )
            )
        } finally {
            stopWatch.start("unlock")
            redisLock.unlock()
            stopWatch.stop()
            logger.info("[$hostTag|$grayFlag]|endBuild| $message| watch=$stopWatch")
        }
    }

/*    *//**
     * 每30分钟执行一次，清理大于两天的任务
     *//*
    @Scheduled(initialDelay = 30 * 1000, fixedDelay = 1800 * 1000)
    @Deprecated("this function is deprecated!")
    fun clearTimeoutTask() {
        val stopWatch = StopWatch()
        var message = ""
        val redisLock = DockerHostLock(redisOperation)
        try {
            stopWatch.start("lock")
            redisLock.lock()
            stopWatch.stop()
            stopWatch.start("resetTimeOutTask")
            val timeoutTask = pipelineDockerTaskDao.getTimeOutTask(dslContext)
            if (timeoutTask.isNotEmpty) {
                logger.info("There is ${timeoutTask.size} build task have/has already time out, clear it.")
                for (i in timeoutTask.indices) {
                    logger.info("clear pipelineId:(${timeoutTask[i].pipelineId}), vmSeqId:(${timeoutTask[i].vmSeqId}), containerId:(${timeoutTask[i].containerId})")
                }
                pipelineDockerTaskDao.deleteTimeOutTask(dslContext)
                message = "timeoutTask.size=${timeoutTask.size}"
            }
            stopWatch.stop()
        } finally {
            stopWatch.start("unlock")
            redisLock.unlock()
            stopWatch.stop()
            logger.info("[$grayFlag]|clearTimeoutTask| $message| watch=$stopWatch")
        }
    }*/

    /**
     * 每120分钟执行一次，更新大于两天状态还是running的pool，以及大于两天状态还是running的build history，并主动关机
     */
    @Scheduled(initialDelay = 120 * 1000, fixedDelay = 3600 * 2 * 1000)
    @Deprecated("this function is deprecated!")
    fun updateTimeoutPoolTask() {
        var message = ""
        val redisLock = RedisLock(redisOperation, "update_timeout_pool_task_nogkudla", 5L)
        try {
            if (redisLock.tryLock()) {
                // 更新大于两天状态还是running的pool
                val timeoutPoolTask = pipelineDockerPoolDao.getTimeOutPool(dslContext)
                if (timeoutPoolTask.isNotEmpty) {
                    logger.info("There is ${timeoutPoolTask.size} build pool task have/has already time out, clear it.")
                    for (i in timeoutPoolTask.indices) {
                        logger.info("update pipelineId:(${timeoutPoolTask[i].pipelineId}), vmSeqId:(${timeoutPoolTask[i].vmSeq}), poolNo:(${timeoutPoolTask[i].poolNo})")
                    }
                    pipelineDockerPoolDao.updateTimeOutPool(dslContext)
                    message = "timeoutPoolTask.size=${timeoutPoolTask.size}"
                }

                // 大于两天状态还是running的build history，并主动关机
                val timeoutBuildList = pipelineDockerBuildDao.getTimeOutBuild(dslContext)
                if (timeoutBuildList.isNotEmpty) {
                    logger.info("There is ${timeoutBuildList.size} build history have/has already time out, clear it.")
                    for (i in timeoutBuildList.indices) {
                        try {
                            val dockerIp = timeoutBuildList[i].dockerIp
                            if (dockerIp.isNotEmpty()) {
                                val dockerIpInfo = pipelineDockerIPInfoDao.getDockerIpInfo(dslContext, dockerIp)
                                if (dockerIpInfo != null && dockerIpInfo.enable) {
                                    dockerHostClient.endBuild(
                                        projectId = timeoutBuildList[i].projectId,
                                        pipelineId = timeoutBuildList[i].pipelineId,
                                        buildId = timeoutBuildList[i].buildId,
                                        vmSeqId = timeoutBuildList[i].vmSeqId,
                                        containerId = timeoutBuildList[i].containerId,
                                        dockerIp = timeoutBuildList[i].dockerIp
                                    )

                                    pipelineDockerBuildDao.updateTimeOutBuild(dslContext, timeoutBuildList[i].buildId)
                                    logger.info("updateTimeoutBuild pipelineId:(${timeoutBuildList[i].pipelineId}), buildId:(${timeoutBuildList[i].buildId}), poolNo:(${timeoutBuildList[i].poolNo})")
                                }
                            }
                        } catch (e: Exception) {
                            logger.error("updateTimeoutBuild buildId: ${timeoutBuildList[i].buildId} failed", e)
                        }
                    }
                }
            }
        } finally {
            redisLock.unlock()
            logger.info("[$grayFlag]|updateTimeoutPoolTask| $message")
        }
    }

/*    *//**
     * 每20秒执行一次，清理固定构建机的任务IP，以让其他构建机可以认领
     *//*
    @Scheduled(initialDelay = 60 * 1000, fixedDelay = 20 * 1000)
    @Deprecated("this function is deprecated!")
    fun resetHostTag() {
        val stopWatch = StopWatch()
        var message = "nothing"
        val redisLock = DockerHostLock(redisOperation)
        try {
            stopWatch.start("lock")
            redisLock.lock()
            stopWatch.stop()
            stopWatch.start("getUnclaimedHostTask")
            val unclaimedTask = pipelineDockerTaskDao.getUnclaimedHostTask(dslContext)
            stopWatch.stop()
            if (unclaimedTask.isNotEmpty) {
                stopWatch.start("clearHostTagForUnclaimedHostTask")
                val dockerhostCache = mutableMapOf<String, Set<String>>()
                logger.info("There is ${unclaimedTask.size} build task have/has queued for a long time, clear hostTag.")
                for (i in unclaimedTask.indices) {
                    val set = dockerhostCache.computeIfAbsent(unclaimedTask[i].projectId) {
                        pipelineDockerHostDao.getHostIps(dslContext, unclaimedTask[i].projectId).toSet()
                    }
                    // 在专用构建机列表的不做重置，让其仍然继续等待认领
                    if (!set.contains(unclaimedTask[i].hostTag)) {
                        logger.info("[${unclaimedTask[i].buildId}]|clear hostTag, pipelineId:(${unclaimedTask[i].pipelineId}), vmSeqId:(${unclaimedTask[i].vmSeqId})")
                        pipelineDockerTaskDao.clearHostTagForUnclaimedHostTask(
                            dslContext = dslContext,
                            buildId = unclaimedTask[i].buildId,
                            vmSeqId = unclaimedTask[i].vmSeqId
                        )
                        redisUtils.deleteDockerBuildLastHost(
                            pipelineId = unclaimedTask[i].pipelineId,
                            vmSeqId = unclaimedTask[i].vmSeqId.toString()
                        )
                    }
                }
                stopWatch.stop()
                message = "unclaimedTask.size=${unclaimedTask.size}"
            }
        } finally {
            stopWatch.start("unlock")
            redisLock.unlock()
            stopWatch.stop()
            logger.info("[$grayFlag]|resetHostTag| $message| watch=$stopWatch")
        }
    }*/

/*    *//**
     * 每40秒执行一次，重置长时间未认领的固定区域的任务，重置为深圳区域
     *//*
    @Scheduled(initialDelay = 90 * 1000, fixedDelay = 40 * 1000)
    @Deprecated("this function is deprecated!")
    fun resetTaskZone() {
        val stopWatch = StopWatch()
        var message = "nothing"
        val redisLock = DockerHostLock(redisOperation)
        try {
            stopWatch.start("lock")
            redisLock.lock()
            stopWatch.stop()
            stopWatch.start("getUnclaimedZoneTask")
            val unclaimedTask = pipelineDockerTaskDao.getUnclaimedZoneTask(dslContext)
            stopWatch.stop()
            if (unclaimedTask.isNotEmpty) {
                stopWatch.start("resetZoneForUnclaimedZoneTask")
                logger.info("There is ${unclaimedTask.size} build task have/has queued for a long time, clear zone.")
                for (i in unclaimedTask.indices) {
                    logger.info("clear zone, pipelineId:(${unclaimedTask[i].pipelineId}), vmSeqId:(${unclaimedTask[i].vmSeqId}), buildId: ${unclaimedTask[i].buildId} ")
                    redisUtils.deleteDockerBuildLastHost(
                        pipelineId = unclaimedTask[i].pipelineId,
                        vmSeqId = unclaimedTask[i].vmSeqId.toString()
                    )
                }
                pipelineDockerTaskDao.resetZoneForUnclaimedZoneTask(dslContext)
                stopWatch.stop()
                message = "Docker构建机异常，区域(${unclaimedTask[0].zone})下无正常的构建机, 任务已切换至深圳地区。"
                AlertUtils.doAlert(HIGH, "Docker构建机异常", message)
            }
        } finally {
            stopWatch.start("unlock")
            redisLock.unlock()
            stopWatch.stop()
            logger.info("[$grayFlag]|resetTaskZone| $message| watch=$stopWatch")
        }
    }*/

    fun getContainerInfo(buildId: String, vmSeqId: Int): Result<ContainerInfo> {
        logger.info("get containerId, buildId:$buildId, vmSeqId:$vmSeqId")
        val buildHistory = pipelineDockerBuildDao.getBuild(dslContext, buildId, vmSeqId)
        if (buildHistory == null) {
            logger.warn("The build history not exists, buildId:$buildId, vmSeqId:$vmSeqId")
            return Result(1, "Container not exists")
        }

        return Result(
            status = 0,
            message = "success",
            data = ContainerInfo(
                projectId = buildHistory.projectId,
                pipelineId = buildHistory.pipelineId,
                vmSeqId = buildHistory.vmSeqId.toString(),
                poolNo = buildHistory.poolNo,
                status = buildHistory.status,
                imageName = "",
                containerId = buildHistory.containerId,
                address = buildHistory.dockerIp,
                token = "",
                buildEnv = "",
                registryUser = "",
                registryPwd = "",
                imageType = ""
            )
        )
    }

    fun buildLessDockerHost(event: PipelineBuildLessStartupDispatchEvent) {
        logger.info("[${event.buildId}]|BUILD_LESS| Start docker host build ($event)}")
        val dispatchType = event.dispatchType as DockerDispatchType
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val secretKey = ApiUtil.randomSecretKey()
            val zone = event.zone ?: Zone.SHENZHEN
            val id = pipelineDockerBuildDao.startBuild(
                dslContext = context,
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                vmSeqId = event.vmSeqId.toInt(),
                secretKey = secretKey,
                status = PipelineTaskStatus.RUNNING,
                zone = zone.name,
                dockerIp = "",
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
                    zone = zone,
                    atoms = event.atoms
                )
            )
            logger.info("[${event.buildId}]|BUILD_LESS| secretKey: $secretKey agentId: $agentId")

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
            logger.info("[${event.buildId}]|BUILD_LESS| Docker images is: $dockerImage")

            // 查找专用机 和 最近一次构建分配的机器
            val dockerHost = pipelineDockerHostDao.getHostIps(
                dslContext = dslContext, projectId = event.projectId, type = DockerHostType.BUILD_LESS
            )
            val hostTag = DockerUtils.getDockerHostIp(
                dockerHosts = dockerHost,
                lastHostIp = null,
                buildId = event.buildId
            )

            // 根据机器IP 查找出路由Key
            val hostZone =
                if (hostTag.isBlank()) {
                    // 从相同区域中找出一台主机，获得他的信息
                    pipelineDockerHostZoneDao.getOneHostZoneByZone(dslContext, zone)
                } else {
                    pipelineDockerHostZoneDao.getHostZone(dslContext, hostTag)
                }

            val routeKeySuffix = hostZone?.routeKey ?: MQ.DEFAULT_BUILD_LESS_DOCKET_HOST_ROUTE_SUFFIX

            pipelineEventDispatcher.dispatch(
                PipelineBuildLessDockerStartupEvent(
                    routeKeySuffix = routeKeySuffix, // 路由Key的后缀
                    source = DockerHostBuildService::class.java.name, // 来源
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    userId = event.userId,
                    buildId = event.buildId,
                    vmSeqId = event.vmSeqId,
                    agentId = agentId,
                    secretKey = secretKey,
                    dockerImage = dockerImage,
                    retryTime = 3
                )
            )

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

            pipelineDockerTaskDao.insertTask(context,
                projectId = event.projectId,
                agentId = agentId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                vmSeqId = event.vmSeqId.toInt(),
                status = PipelineTaskStatus.RUNNING,
                secretKey = secretKey,
                imageName = dockerImage,
                hostTag = routeKeySuffix,
                channelCode = event.channelCode,
                zone = if (null == event.zone) {
                    Zone.SHENZHEN.name
                } else {
                    event.zone!!.name
                },
                registryUser = userName,
                registryPwd = password,
                imageType = if (null == dispatchType.imageType) {
                    ImageType.BKDEVOPS.type
                } else {
                    dispatchType.imageType!!.type
                },
                // 无构建环境默认每次都从仓库拉取
                imagePublicFlag = false,
                imageRDType = ImageRDTypeEnum.SELF_DEVELOPED,
                containerHashId = event.containerHashId
            )
        }
    }

    fun finishBuildLessDockerHost(buildId: String, vmSeqId: String?, userId: String, success: Boolean) {
        logger.info("[$buildId]|BUILD_LESS| Finish vmSeqId($vmSeqId) with result($success)")
        if (vmSeqId.isNullOrBlank()) {
            val records = pipelineDockerBuildDao.listBuilds(dslContext, buildId)
            if (records.isEmpty()) {
                return
            }
            records.forEach {
                dispatchStopCmd(it, userId)
                finishBuild(it, success)
            }
        } else {
            val record = pipelineDockerBuildDao.getBuild(dslContext, buildId, vmSeqId!!.toInt())
            if (record != null) {
                dispatchStopCmd(record, userId)
                finishBuild(record, success)
            }
        }
    }

    private fun dispatchStopCmd(record: TDispatchPipelineDockerBuildRecord, userId: String) {
        val dockerLessTask = pipelineDockerTaskDao.getTask(dslContext, record.buildId, record.vmSeqId)
            ?: run {
                logger.warn("[${record.buildId}]|BUILD_LESS| can not found vmSeqId(${record.vmSeqId}) task")
                return
            }

        if (dockerLessTask.hostTag.isNullOrBlank()) {
            logger.warn("[${record.buildId}]|BUILD_LESS| can not find hostTag")
            return
        }

        logger.info("[${record.buildId}]|BUILD_LESS| Finish docker(${dockerLessTask.containerId})| hostTag=${dockerLessTask.hostTag}")

        pipelineEventDispatcher.dispatch(
            PipelineBuildLessDockerShutdownEvent(
                routeKeySuffix = dockerLessTask.hostTag, // 路由Key的后缀
                source = DockerHostBuildService::class.java.name, // 来源
                projectId = record.projectId,
                pipelineId = record.pipelineId,
                userId = userId,
                buildId = record.buildId,
                dockerContainerId = dockerLessTask.containerId
            )
        )
    }

    fun getHost(hostTag: String): Result<DockerHostInfo>? {
        val hostZone = pipelineDockerHostZoneDao.getHostZone(dslContext, hostTag)
        logger.info("[getHost]| hostTag=$hostTag, hostZone=$hostZone")
        return if (hostZone == null) {
            Result(DockerHostInfo(MQ.DEFAULT_BUILD_LESS_DOCKET_HOST_ROUTE_SUFFIX))
        } else {
            Result(DockerHostInfo(hostZone.routeKey ?: MQ.DEFAULT_BUILD_LESS_DOCKET_HOST_ROUTE_SUFFIX))
        }
    }

    fun log(buildId: String, red: Boolean, message: String, tag: String? = "", jobId: String? = "") {
        logger.info("write log from docker host, buildId: $buildId, msg: $message, tag: $tag, jobId= $jobId")
        if (red) {
            buildLogPrinter.addRedLine(buildId, message, tag ?: "", jobId ?: "", 1)
        } else {
            buildLogPrinter.addLine(buildId, message, tag ?: "", jobId ?: "", 1)
        }
    }

    fun getPublicImage(): Result<List<ImageRepoInfo>> {
        logger.info("enter getPublicImage")
        return client.get(ServiceStoreImageResource::class).getSelfDevelopPublicImages()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DockerHostBuildService::class.java)
    }
}
