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
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.common.web.mq.alert.AlertUtils
import com.tencent.devops.dispatch.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.dao.PipelineDockerEnableDao
import com.tencent.devops.dispatch.dao.PipelineDockerHostDao
import com.tencent.devops.dispatch.dao.PipelineDockerHostZoneDao
import com.tencent.devops.dispatch.dao.PipelineDockerTaskDao
import com.tencent.devops.dispatch.pojo.ContainerInfo
import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import com.tencent.devops.dispatch.pojo.DockerHostInfo
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.pojo.redis.RedisBuild
import com.tencent.devops.dispatch.utils.CommonUtils
import com.tencent.devops.dispatch.utils.DockerHostLock
import com.tencent.devops.dispatch.utils.DockerUtils
import com.tencent.devops.dispatch.utils.redis.RedisUtils
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerBuildRecord
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.VmInfo
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import com.tencent.devops.process.pojo.mq.PipelineBuildLessDockerShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineBuildLessDockerStartupEvent
import com.tencent.devops.process.pojo.mq.PipelineBuildLessStartupDispatchEvent
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class DockerHostBuildService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineDockerEnableDao: PipelineDockerEnableDao,
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val pipelineDockerTaskDao: PipelineDockerTaskDao,
    private val pipelineDockerHostDao: PipelineDockerHostDao,
    private val pipelineDockerHostZoneDao: PipelineDockerHostZoneDao,
    private val redisUtils: RedisUtils,
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val gray: Gray,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val rabbitTemplate: RabbitTemplate
) {

    @Value("\${dispatch.dockerBuildImagePrefix:#{null}}")
    val dockerBuildImagePrefix: String? = null

    @Value("\${project.gray:#{null}}")
    private val grayFlag: String? = null

    fun enable(pipelineId: String, vmSeqId: Int?, enable: Boolean) =
        pipelineDockerEnableDao.enable(dslContext, pipelineId, vmSeqId, enable)

    fun dockerHostBuild(event: PipelineAgentStartupEvent) {
        logger.info("Start docker host build ($event)}")
        val dispatchType = event.dispatchType as DockerDispatchType
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val secretKey = ApiUtil.randomSecretKey()
            val id = pipelineDockerBuildDao.startBuild(context,
                event.projectId,
                event.pipelineId,
                event.buildId,
                event.vmSeqId.toInt(),
                secretKey,
                PipelineTaskStatus.RUNNING,
                if (null == event.zone) { Zone.SHENZHEN.name } else { event.zone!!.name })
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

            // 插入dockerTask表，等待dockerHost进程过来轮询
            val dockerImage = if (dispatchType.imageType == ImageType.THIRD) {
                dispatchType.dockerBuildVersion
            } else {
                when (dispatchType.dockerBuildVersion) {
                    DockerVersion.TLINUX1_2.value -> dockerBuildImagePrefix + TLINUX1_2_IMAGE
                    DockerVersion.TLINUX2_2.value -> dockerBuildImagePrefix + TLINUX2_2_IMAGE
                    else -> "$dockerBuildImagePrefix/bkdevops/${dispatchType.dockerBuildVersion}"
                }
            }
            logger.info("Docker images is: $dockerImage")
            var userName: String? = null
            var password: String? = null
            if (dispatchType.imageType == ImageType.THIRD) {
                if (!dispatchType.credentialId.isNullOrBlank()) {
                    val ticketsMap = CommonUtils.getCredential(client, event.projectId, dispatchType.credentialId!!, CredentialType.USERNAME_PASSWORD)
                    userName = ticketsMap["v1"] as String
                    password = ticketsMap["v2"] as String
                }
            }

            // 如果固定构建机的表中设置了该项目的母机IP，则把该母机IP也写入dockerTask表
            val dockerHost = pipelineDockerHostDao.getHost(dslContext, event.projectId)
            val lastHostIp = redisUtils.getDockerBuildLastHost(event.pipelineId, event.vmSeqId)
            val hostTag = when {
                null != dockerHost -> {
                    logger.info("Fixed build host machine, hostIp:${dockerHost.hostIp}")
                    dockerHost.hostIp
                }
                null != lastHostIp -> {
                    logger.info("Use last build hostIp: $lastHostIp")
                    val lastHostZone = pipelineDockerHostZoneDao.getHostZone(dslContext, lastHostIp)
                    if (null != lastHostZone && (event.zone != Zone.valueOf(lastHostZone.zone))) {
                        logger.info("Last build hostIp zone is different with buildMessage.zone, so clean hostTag")
                        ""
                    } else {
                        lastHostIp
                    }
                }
                else -> ""
            }

            pipelineDockerTaskDao.insertTask(context,
                projectId = event.projectId,
                agentId = agentId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                vmSeqId = event.vmSeqId.toInt(),
                status = PipelineTaskStatus.QUEUE,
                secretKey = secretKey,
                imageName = dockerImage,
                hostTag = hostTag,
                channelCode = event.channelCode,
                zone = if (null == event.zone) { Zone.SHENZHEN.name } else { event.zone!!.name },
                registryUser = userName,
                registryPwd = password,
                imageType = if (null == dispatchType.imageType) {
                    ImageType.BKDEVOPS.type
                } else {
                    dispatchType.imageType!!.type
                }
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

    fun finishDockerBuild(buildId: String, vmSeqId: String?, success: Boolean) {
        logger.info("Finish docker build of buildId($buildId) and vmSeqId($vmSeqId) with result($success)")
        if (vmSeqId.isNullOrBlank()) {
            val record = pipelineDockerBuildDao.listBuilds(dslContext, buildId)
            if (record.isEmpty()) {
                return
            }
            record.forEach {
                finishBuild(it, success)
            }
        } else {
            val record = pipelineDockerBuildDao.getBuild(dslContext, buildId, vmSeqId!!.toInt())
            if (record != null) {
                finishBuild(record, success)
            }
        }
    }

    private fun finishBuild(record: TDispatchPipelineDockerBuildRecord, success: Boolean) {
        logger.info("Finish the docker build(${record.buildId}) with result($success)")
        pipelineDockerBuildDao.updateStatus(dslContext,
            record.buildId,
            record.vmSeqId,
            if (success) PipelineTaskStatus.DONE else PipelineTaskStatus.FAILURE)
        // 更新dockerTask表
        pipelineDockerTaskDao.updateStatus(dslContext,
            record.buildId,
            record.vmSeqId,
            if (success) PipelineTaskStatus.DONE else PipelineTaskStatus.FAILURE)
        redisUtils.deleteDockerBuild(record.id, SecurityUtil.decrypt(record.secretKey))
        redisUtils.deleteHeartBeat(record.buildId, record.vmSeqId.toString())
    }

    fun startBuild(hostTag: String): Result<DockerHostBuildInfo>? {
        logger.info("[$hostTag|$grayFlag] Start to build")
        val hostZone = pipelineDockerHostZoneDao.getHostZone(dslContext, hostTag)

        val redisLock = DockerHostLock(redisOperation)
        try {
            val gray = !grayFlag.isNullOrBlank() && grayFlag!!.toBoolean()
            val grayProjectSet = redisOperation.getSetMembers(this.gray.getGrayRedisKey())?.filter { !it.isBlank() }
                ?.toSet() ?: emptySet()
            logger.info("Get the redis project set: $grayProjectSet")
            redisLock.lock()
            if (gray) {
                // 优先取设置了IP的任务（可能是固定构建机，也可能是上次用的构建机）
                var task = pipelineDockerTaskDao.getQueueTasksByProj(dslContext, grayProjectSet, hostTag)
                if (task.isNotEmpty) {
                    logger.info("Start docker build with hostIp: $hostTag")
                } else if (hostZone != null) { // 再按区域取值
                    task = pipelineDockerTaskDao.getQueueTasksByProj(dslContext, grayProjectSet, Zone.valueOf(hostZone.zone))
                    if (task.isNotEmpty) {
                        logger.info("Start docker build with zone: ${hostZone.zone}")
                    } else { // 最后随机取
                        task = pipelineDockerTaskDao.getQueueTasksByProj(dslContext, grayProjectSet)
                    }
                } else { // 客户端的区域为空，则随机取
                    task = pipelineDockerTaskDao.getQueueTasksByProj(dslContext, grayProjectSet)
                }

                if (task.isEmpty()) {
                    logger.info("Not task in queue")
                    return Result(1, "no task in queue")
                }
                val build = task[0]
                logger.info("Start the docker build(${build.buildId}) seq(${build.vmSeqId})")
                pipelineDockerTaskDao.updateStatusAndTag(dslContext, build.buildId, build.vmSeqId, PipelineTaskStatus.RUNNING, hostTag)
                redisUtils.setDockerBuildLastHost(build.pipelineId, build.vmSeqId.toString(), hostTag) // 将本次构建使用的主机IP写入redis，以方便下次直接用这台IP
                return Result(0, "success", DockerHostBuildInfo(build.projectId, build.agentId, build.pipelineId, build.buildId, build.vmSeqId,
                    build.secretKey, PipelineTaskStatus.RUNNING.status, build.imageName, "", false, build.registryUser, build.registryPwd, build.imageType))
            } else {
                // 优先取设置了IP的任务（可能是固定构建机，也可能是上次用的构建机）
                var task = pipelineDockerTaskDao.getQueueTasksExcludeProj(dslContext, grayProjectSet, hostTag)
                if (task.isNotEmpty) {
                    logger.info("Start docker build with hostIp: $hostTag")
                } else if (hostZone != null) { // 再按区域取值
                    task = pipelineDockerTaskDao.getQueueTasksExcludeProj(dslContext, grayProjectSet, Zone.valueOf(hostZone.zone))
                    if (task.isNotEmpty) {
                        logger.info("Start docker build with zone: ${hostZone.zone}")
                    } else { // 最后随机取
                        task = pipelineDockerTaskDao.getQueueTasksExcludeProj(dslContext, grayProjectSet)
                    }
                } else { // 客户端的区域为空，则随机取
                    task = pipelineDockerTaskDao.getQueueTasksExcludeProj(dslContext, grayProjectSet)
                }

                if (task.isEmpty()) {
                    logger.info("No task in queue")
                    return Result(1, "no task in queue")
                }
                val build = task[0]
                logger.info("Start the docker build(${build.buildId}) seq(${build.vmSeqId})")
                pipelineDockerTaskDao.updateStatusAndTag(dslContext, build.buildId, build.vmSeqId, PipelineTaskStatus.RUNNING, hostTag)
                redisUtils.setDockerBuildLastHost(build.pipelineId, build.vmSeqId.toString(), hostTag) // 将本次构建使用的主机IP写入redis，以方便下次直接用这台IP
                return Result(0, "success", DockerHostBuildInfo(build.projectId, build.agentId, build.pipelineId, build.buildId, build.vmSeqId,
                    build.secretKey, PipelineTaskStatus.RUNNING.status, build.imageName, "", false, build.registryUser, build.registryPwd, build.imageType))
            }
        } finally {
            redisLock.unlock()
        }
    }

    fun reportContainerId(buildId: String, vmSeqId: Int, containerId: String, hostTag: String?): Result<Boolean>? {
        logger.info("[$buildId]|reportContainerId|vmSeqId=$vmSeqId|containerId=$containerId|hostTag=$hostTag")

        pipelineDockerTaskDao.updateContainerId(dslContext, buildId, vmSeqId, containerId, hostTag)

        return Result(0, "success", true)
    }

    fun rollbackBuild(buildId: String, vmSeqId: Int, shutdown: Boolean?): Result<Boolean>? {
        logger.info("Rollback build, buildId:$buildId, vmSeqId:$vmSeqId")

        val redisLock = DockerHostLock(redisOperation)
        try {
            redisLock.lock()
            val task = pipelineDockerTaskDao.getTask(dslContext, buildId, vmSeqId)
            if (task == null) {
                logger.warn("The build task not exists, buildId:$buildId, vmSeqId:$vmSeqId")
                return Result(1, "Task not exists")
            }
            // dockerhost上报失败，则直接失败，不用回滚;
            if (true == shutdown) {
                logger.info("Shutdown is true, no need to rollback! projectId: ${task.projectId}, " +
                    "pipelineId: ${task.pipelineId}, buildId: ${task.buildId}, channelCode: ${task.channelCode}")
                client.get(ServiceBuildResource::class).serviceShutdown(task.projectId, task.pipelineId, task.buildId, ChannelCode.valueOf(task.channelCode))

                AlertUtils.doAlert(AlertLevel.HIGH, "Docker构建机启动任务异常", "构建镜像无法启动，projectId: ${task.projectId}, pipelineId: ${task.pipelineId},buildId: ${task.buildId}")
                return Result(0, "Shutdown the build")
            }
            // 或固定构建机的场景，则直接失败，不用回滚; 其他场景需要回滚，并把hostTag清空，以供其他机器执行
            val dockerHost = pipelineDockerHostDao.getHost(dslContext, task.projectId)
            if (null != dockerHost) {
                logger.info("DockerHost is not null, rollback failed, shutdown the build! projectId: ${task.projectId}, " +
                    "pipelineId: ${task.pipelineId}, buildId: ${task.buildId}, channelCode: ${task.channelCode}")
                client.get(ServiceBuildResource::class).serviceShutdown(task.projectId, task.pipelineId, task.buildId, ChannelCode.valueOf(task.channelCode))

                AlertUtils.doAlert(AlertLevel.HIGH, "Docker构建机启动任务异常", "固定的Docker构建机启动任务异常，IP：${dockerHost.hostIp}, " +
                    "projectId: ${task.projectId}, pipelineId: ${task.pipelineId},buildId: ${task.buildId}")
                return Result(0, "Rollback task finished")
            }

            if (task.status == PipelineTaskStatus.RUNNING.status) {
                pipelineDockerTaskDao.updateStatusAndTag(dslContext, buildId, vmSeqId, PipelineTaskStatus.QUEUE, "")
                AlertUtils.doAlert(AlertLevel.LOW, "Docker构建机启动任务异常", "Docker构建机启动任务异常，任务已重试，异常ip: ${task.hostTag}, " +
                    "projectId: ${task.projectId}, pipelineId: ${task.pipelineId},buildId: ${task.buildId}")
            }
        } finally {
            redisLock.unlock()
        }

        return Result(0, "success", true)
    }

    fun endBuild(hostTag: String): Result<DockerHostBuildInfo>? {
        val redisLock = DockerHostLock(redisOperation)
        try {
            redisLock.lock()
            val task = pipelineDockerTaskDao.getDoneTasks(dslContext, hostTag)
            if (task.isEmpty()) {
                return Result(1, "no task to end")
            }
            val build = task[0]
            logger.info("End the docker build(${build.buildId}) seq(${build.vmSeqId})")
            pipelineDockerTaskDao.deleteTask(dslContext, build.id)
            return Result(0, "success", DockerHostBuildInfo(build.projectId, build.agentId, build.pipelineId, build.buildId, build.vmSeqId,
                build.secretKey, build.status, build.imageName, build.containerId, false, build.registryUser, build.registryPwd, build.imageType))
        } finally {
            redisLock.unlock()
        }
    }

    /**
     * 每30分钟执行一次，清理大于两天的任务
     */
    @Scheduled(initialDelay = 30 * 1000, fixedDelay = 1800 * 1000)
    fun clearTimeoutTask() {
        val redisLock = DockerHostLock(redisOperation)
        try {
            redisLock.lock()
            val timeoutTask = pipelineDockerTaskDao.getTimeOutTask(dslContext)
            if (timeoutTask.isNotEmpty) {
                logger.info("There is ${timeoutTask.size} build task have/has already time out, clear it.")
                for (i in timeoutTask.indices) {
                    logger.info("clear pipelineId:(${timeoutTask[i].pipelineId}), vmSeqId:(${timeoutTask[i].vmSeqId}), containerId:(${timeoutTask[i].containerId})")
                }
                pipelineDockerTaskDao.updateTimeOutTask(dslContext)
            }
        } finally {
            redisLock.unlock()
        }
    }

    /**
     * 每20秒执行一次，清理固定构建机的任务IP，以让其他构建机可以认领
     */
    @Scheduled(initialDelay = 60 * 1000, fixedDelay = 20 * 1000)
    fun resetHostTag() {
        val redisLock = DockerHostLock(redisOperation)
        try {
            redisLock.lock()
            val unclaimedTask = pipelineDockerTaskDao.getUnclaimedHostTask(dslContext)
            if (unclaimedTask.isNotEmpty) {
                logger.info("There is ${unclaimedTask.size} build task have/has queued for a long time, clear hostTag.")
                for (i in unclaimedTask.indices) {
                    logger.info("clear hostTag, pipelineId:(${unclaimedTask[i].pipelineId}), vmSeqId:(${unclaimedTask[i].vmSeqId}), buildId: ${unclaimedTask[i].buildId} ")
                    redisUtils.deleteDockerBuildLastHost(unclaimedTask[i].pipelineId, unclaimedTask[i].vmSeqId.toString())
                }
                pipelineDockerTaskDao.clearHostTagForUnclaimedHostTask(dslContext)
            }
        } finally {
            redisLock.unlock()
        }
    }

    /**
     * 每40秒执行一次，重置长时间未认领的固定区域的任务，重置为深圳区域
     */
    @Scheduled(initialDelay = 90 * 1000, fixedDelay = 40 * 1000)
    fun resetTaskZone() {
        val redisLock = DockerHostLock(redisOperation)
        try {
            redisLock.lock()
            val unclaimedTask = pipelineDockerTaskDao.getUnclaimedZoneTask(dslContext)
            if (unclaimedTask.isNotEmpty) {
                logger.info("There is ${unclaimedTask.size} build task have/has queued for a long time, clear zone.")
                for (i in unclaimedTask.indices) {
                    logger.info("clear zone, pipelineId:(${unclaimedTask[i].pipelineId}), vmSeqId:(${unclaimedTask[i].vmSeqId}), buildId: ${unclaimedTask[i].buildId} ")
                    redisUtils.deleteDockerBuildLastHost(unclaimedTask[i].pipelineId, unclaimedTask[i].vmSeqId.toString())
                }
                pipelineDockerTaskDao.resetZoneForUnclaimedZoneTask(dslContext)

                AlertUtils.doAlert(AlertLevel.HIGH, "Docker构建机异常", "Docker构建机异常，区域(${unclaimedTask[0].zone})下无正常的构建机, 任务已切换至深圳地区。")
            }
        } finally {
            redisLock.unlock()
        }
    }

    fun getContainerInfo(buildId: String, vmSeqId: Int): Result<ContainerInfo> {
        logger.info("get containerId, buildId:$buildId, vmSeqId:$vmSeqId")
        val task = pipelineDockerTaskDao.getTask(dslContext, buildId, vmSeqId)
        if (task == null) {
            logger.warn("The build task not exists, buildId:$buildId, vmSeqId:$vmSeqId")
            return Result(1, "Container not exists")
        }

        return Result(0, "success", ContainerInfo(task.projectId, task.pipelineId, task.vmSeqId.toString(), task.status,
            task.imageName, task.containerId, task.hostTag, "", "", task.registryUser, task.registryPwd, task.imageType)
        )
    }

    fun buildLessDockerHost(event: PipelineBuildLessStartupDispatchEvent) {
        logger.info("[${event.buildId}]|BUILD_LESS| Start docker host build ($event)}")
        val dispatchType = event.dispatchType as DockerDispatchType
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val secretKey = ApiUtil.randomSecretKey()
            val zone = event.zone ?: Zone.SHENZHEN
            val id = pipelineDockerBuildDao.startBuild(context,
                event.projectId,
                event.pipelineId,
                event.buildId,
                event.vmSeqId.toInt(),
                secretKey,
                PipelineTaskStatus.RUNNING,
                zone.name
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
                    zone = zone,
                    atoms = event.atoms
                )
            )
            logger.info("[${event.buildId}]|BUILD_LESS| secretKey: $secretKey agentId: $agentId")

            //
            val dockerImage = when (dispatchType.dockerBuildVersion) {
                DockerVersion.TLINUX1_2.value -> dockerBuildImagePrefix + BL_TLINUX1_2_IMAGE
                DockerVersion.TLINUX2_2.value -> dockerBuildImagePrefix + BL_TLINUX2_2_IMAGE
                else -> "$dockerBuildImagePrefix/bkdevops/${dispatchType.dockerBuildVersion}"
            }
            logger.info("[${event.buildId}]|BUILD_LESS| Docker images is: $dockerImage")

            // 查找专用机 和 最近一次构建分配的机器
            val dockerHost = pipelineDockerHostDao.getHost(dslContext, event.projectId)
            val hostTag = if (dockerHost != null) {
                logger.info("[${event.buildId}]|BUILD_LESS| Fixed build host machine, hostIp:${dockerHost.hostIp}")
                dockerHost.hostIp
            } else {
                ""
            }

            // 根据机器IP 查找出路由Key
            val hostZone =
                if (hostTag.isNullOrBlank()) {
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
                        client,
                        event.projectId,
                        dispatchType.credentialId!!,
                        CredentialType.USERNAME_PASSWORD
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
                zone = if (null == event.zone) { Zone.SHENZHEN.name } else { event.zone!!.name },
                registryUser = userName,
                registryPwd = password,
                imageType = if (null == dispatchType.imageType) {
                    ImageType.BKDEVOPS.type
                } else {
                    dispatchType.imageType!!.type
                })
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

    fun log(buildId: String, red: Boolean, message: String, tag: String? = "") {
        logger.info("write log from docker host, buildId: $buildId, msg: $message")
        if (red) {
            LogUtils.addRedLine(rabbitTemplate, buildId, message, tag ?: "", "", 1)
        } else {
            LogUtils.addLine(rabbitTemplate, buildId, message, tag ?: "", "", 1)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DockerHostBuildService::class.java)

        private const val TLINUX1_2_IMAGE = "/bkdevops/docker-builder1.2:v1"
        private const val TLINUX2_2_IMAGE = "/bkdevops/docker-builder2.2:v1"

        private const val BL_TLINUX1_2_IMAGE = "/bkdevops/docker-build-less1.2:v1"
        private const val BL_TLINUX2_2_IMAGE = "/bkdevops/docker-build-less2.2:v1"
    }
}