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

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.common.api.util.ApiUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.common.web.mq.alert.AlertUtils
import com.tencent.devops.dispatch.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.dao.PipelineDockerHostZoneDao
import com.tencent.devops.dispatch.dao.PipelineDockerTaskDao
import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.pojo.redis.RedisBuild
import com.tencent.devops.dispatch.utils.DockerHostLock
import com.tencent.devops.dispatch.utils.redis.RedisUtils
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerBuildRecord
import com.tencent.devops.process.api.ServiceBuildResource
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class DockerHostBuildService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val pipelineDockerHostZoneDao: PipelineDockerHostZoneDao,
    private val pipelineDockerTaskDao: PipelineDockerTaskDao,
    private val redisUtils: RedisUtils,
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val rabbitTemplate: RabbitTemplate
) {

    @Value("\${dispatch.dockerBuildImagePrefix:#{null}}")
    val dockerBuildImagePrefix: String? = null

    @Value("\${dispatch.dockerhostPort:80}")
    val dockerhostPort: Int? = null

    @Value("\${dispatch.defaultImageUrl:#{null}}")
    private val defaultImageUrl: String? = null

    @Value("\${dispatch.defaultImageName:#{null}}")
    private val defaultImageName: String? = null

    private val pool = Executors.newCachedThreadPool()

    fun dockerHostBuild(event: PipelineAgentStartupEvent) {
        logger.info("Start docker host build ($event)}")
        val dispatchType = event.dispatchType as DockerDispatchType
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val secretKey = ApiUtil.randomSecretKey()
            val id = pipelineDockerBuildDao.startBuild(
                context,
                event.projectId,
                event.pipelineId,
                event.buildId,
                event.vmSeqId.toInt(),
                secretKey,
                PipelineTaskStatus.RUNNING,
                if (null == event.zone) {
                    Zone.DEFAULT.name
                } else {
                    event.zone!!.name
                }
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

            // 插入dockerTask表，等待dockerHost进程过来轮询
            val dockerImage = when (dispatchType.dockerBuildVersion) {
                defaultImageName -> defaultImageUrl
                else -> "$dockerBuildImagePrefix/${dispatchType.dockerBuildVersion}"
            }
            logger.info("Docker images is: $dockerImage")

            val lastHostIp = redisUtils.getDockerBuildLastHost(event.pipelineId, event.vmSeqId)
            var hostTag = if (null != lastHostIp) {
                logger.info("Use last build hostIp: $lastHostIp")
                val lastHostZone = pipelineDockerHostZoneDao.getHostZone(dslContext, lastHostIp)
                if (null != lastHostZone) {
                    lastHostIp
                } else {
                    logger.info("Last build hostIp zone is not existed.")
                    null
                }
            } else {
                null
            }
            // 当hostTag为空的时候，我们拿一个新的hostTag
            if (hostTag.isNullOrEmpty()) {
                logger.info("Use new build hostIp.")
                val oneHostRandom = pipelineDockerHostZoneDao.getOneHostByRandom(dslContext)
                hostTag = oneHostRandom.hostIp
            }
            if (hostTag != null) {
                logger.info("build hostIp:$hostTag")
                pipelineDockerTaskDao.insertTask(
                    context,
                    projectId = event.projectId,
                    agentId = agentId,
                    pipelineId = event.pipelineId,
                    buildId = event.buildId,
                    vmSeqId = event.vmSeqId.toInt(),
                    status = PipelineTaskStatus.RUNNING,
                    secretKey = secretKey,
                    imageName = dockerImage!!,
                    hostTag = hostTag,
                    channelCode = event.channelCode,
                    zone = if (null == event.zone) {
                        Zone.DEFAULT.name
                    } else {
                        event.zone!!.name
                    }
                )
                logger.info("pipelineDockerTaskDao.insertTask success.")

                val dockerHostBuildInfo = DockerHostBuildInfo(
                    event.projectId, agentId, event.pipelineId, event.buildId, event.vmSeqId.toInt(),
                    secretKey, PipelineTaskStatus.RUNNING.status, dockerImage, "", false
                )
                logger.info("dockerHostBuildInfo：$dockerHostBuildInfo")
                pool.submit(
                    DockerDCall(
                        dslContext = dslContext,
                        pipelineDockerTaskDao = pipelineDockerTaskDao,
                        redisUtils = redisUtils,
                        dockerHostPort = dockerhostPort ?: 80,
                        dockerHostBuildInfo = dockerHostBuildInfo,
                        hostTag = hostTag
                    )
                )
            } else {
                logger.error("Fail to start a dockerHost build :($event)")
            }
        }
    }

    class DockerDCall constructor(
        private val dslContext: DSLContext,
        private val pipelineDockerTaskDao: PipelineDockerTaskDao,
        private val redisUtils: RedisUtils,
        private val dockerHostPort: Int,
        private val dockerHostBuildInfo: DockerHostBuildInfo,
        private val hostTag: String
    ) : Runnable {

        override fun run() {
            val startBuildResult = try {
                startBuildByHost(dockerHostBuildInfo, hostTag)
            } catch (ignored: Throwable) {
                logger.error("[${dockerHostBuildInfo.buildId}]|startBuildByHost exception：$ignored")
                false
            }

            // 写入最新的host ip
            if (startBuildResult) {

                redisUtils.setDockerBuildLastHost(
                    dockerHostBuildInfo.pipelineId,
                    dockerHostBuildInfo.vmSeqId.toString(),
                    hostTag
                )

                logger.info("[${dockerHostBuildInfo.buildId}]|setDockerBuildLastHost success.")
            } else {
                logger.warn("[${dockerHostBuildInfo.buildId}]|Reset DockerTask To Queue.")
                pipelineDockerTaskDao.updateStatus(
                    dslContext,
                    dockerHostBuildInfo.buildId,
                    dockerHostBuildInfo.vmSeqId,
                    PipelineTaskStatus.QUEUE
                )
            }
        }

        private fun startBuildByHost(dockerHostBuildInfo: DockerHostBuildInfo, hostTag: String): Boolean {
            val url = "http://$hostTag:$dockerHostPort/api/docker/startBuild"
            val body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                ObjectMapper().writeValueAsString(dockerHostBuildInfo)
            )
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()
            OkhttpUtils.doLongHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                return if (!response.isSuccessful) {
                    logger.error("[${dockerHostBuildInfo.buildId}]|Fail to request $request with code ${response.code()}, message ${response.message()} and body $responseContent")
                    false
                } else {
                    true
                }
            }
        }
    }

    fun finishDockerBuild(buildId: String, vmSeqId: String?, success: Boolean) {
        logger.info("[$buildId]|Finish docker build of vmSeqId($vmSeqId) with result($success)")
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

    private fun finishBuild(record: TDispatchPipelineDockerBuildRecord, success: Boolean): Boolean {
        logger.info("[${record.buildId}]|Finish the docker with result($success)")
        val task = pipelineDockerTaskDao.getTask(dslContext, record.buildId, record.vmSeqId)
        if (task != null) {
            val dockerHostBuildInfo = DockerHostBuildInfo(
                task.projectId,
                task.agentId,
                task.pipelineId,
                task.buildId,
                task.vmSeqId.toInt(),
                task.secretKey,
                if (success) PipelineTaskStatus.DONE.status else PipelineTaskStatus.FAILURE.status,
                task.imageName,
                task.containerId,
                false
            )
            val finishBuildByHostResult = finishBuildByHost(dockerHostBuildInfo, task.hostTag)
            logger.info("[${record.buildId}]|Shutdown docker[${task.hostTag}]: $finishBuildByHostResult")
            return if (finishBuildByHostResult) {
                redisUtils.deleteDockerBuild(record.id, SecurityUtil.decrypt(record.secretKey))
                redisUtils.deleteHeartBeat(record.buildId, record.vmSeqId.toString())
                pipelineDockerTaskDao.deleteTask(dslContext, task.id)
                pipelineDockerBuildDao.updateStatus(
                    dslContext,
                    record.buildId,
                    record.vmSeqId,
                    if (success) PipelineTaskStatus.DONE else PipelineTaskStatus.FAILURE
                )
                true
            } else {
                false
            }
        } else {
            return true
        }
    }

    fun finishBuildByHost(dockerHostBuildInfo: DockerHostBuildInfo, hostTag: String): Boolean {
        val url = "http://$hostTag:$dockerhostPort/api/docker/endBuild"
        val body = RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            ObjectMapper().writeValueAsString(dockerHostBuildInfo)
        )
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            return if (!response.isSuccessful) {
                logger.error("Fail to request $request with code ${response.code()}, message ${response.message()} and body $responseContent")
                false
            } else {
                true
            }
        }
    }

    fun startBuild(hostTag: String): Result<DockerHostBuildInfo>? {
        val hostZone = pipelineDockerHostZoneDao.getHostZone(dslContext, hostTag)

        val redisLock = DockerHostLock(redisOperation)
        try {
            val grayProjectSet = emptySet<String>()
            // 优先取设置了IP的任务（可能是固定构建机，也可能是上次用的构建机）
            var task = pipelineDockerTaskDao.getQueueTasksExcludeProj(dslContext, grayProjectSet, hostTag)
            if (task.isNotEmpty) {
                logger.info("Start docker build with hostIp: $hostTag")
            } else if (hostZone != null) { // 再按区域取值
                task = pipelineDockerTaskDao.getQueueTasksExcludeProj(
                    dslContext,
                    grayProjectSet,
                    Zone.valueOf(hostZone.zone)
                )
                if (task.isNotEmpty) {
                    logger.info("Start docker build with zone: ${hostZone.zone}")
                } else { // 最后随机取
                    task = pipelineDockerTaskDao.getQueueTasksExcludeProj(dslContext, grayProjectSet)
                }
            } else { // 客户端的区域为空，则随机取
                task = pipelineDockerTaskDao.getQueueTasksExcludeProj(dslContext, grayProjectSet)
            }

            if (task.isEmpty()) {
                return Result(1, "no task in queue")
            }
            val build = task[0]
            logger.info("Start the docker build(${build.buildId}) seq(${build.vmSeqId})")
            pipelineDockerTaskDao.updateStatusAndTag(
                dslContext = dslContext,
                buildId = build.buildId,
                vmSeqId = build.vmSeqId,
                status = PipelineTaskStatus.RUNNING,
                hostTag = hostTag
            )
            redisUtils.setDockerBuildLastHost(
                pipelineId = build.pipelineId,
                vmSeqId = build.vmSeqId.toString(),
                hostIp = hostTag
            ) // 将本次构建使用的主机IP写入redis，以方便下次直接用这台IP
            return Result(
                0, "success", DockerHostBuildInfo(
                    projectId = build.projectId,
                    agentId = build.agentId,
                    pipelineId = build.pipelineId,
                    buildId = build.buildId,
                    vmSeqId = build.vmSeqId,
                    secretKey = build.secretKey,
                    status = PipelineTaskStatus.RUNNING.status,
                    imageName = build.imageName,
                    containerId = "",
                    wsInHost = false
                )
            )
        } finally {
            redisLock.unlock()
        }
    }

    fun reportContainerId(buildId: String, vmSeqId: Int, containerId: String): Result<Boolean>? {
        logger.info("Docker host report containerId, buildId:$buildId, vmSeqId:$vmSeqId, containerId:$containerId")

        pipelineDockerTaskDao.updateContainerId(dslContext, buildId, vmSeqId, containerId)

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
                logger.info(
                    "Shutdown is true, no need to rollback! projectId: ${task.projectId}, " +
                        "pipelineId: ${task.pipelineId}, buildId: ${task.buildId}, channelCode: ${task.channelCode}"
                )
                client.get(ServiceBuildResource::class).serviceShutdown(
                    task.projectId,
                    task.pipelineId,
                    task.buildId,
                    ChannelCode.valueOf(task.channelCode)
                )

                AlertUtils.doAlert(
                    AlertLevel.HIGH,
                    "Docker构建机启动任务异常",
                    "构建镜像无法启动，projectId: ${task.projectId}, pipelineId: ${task.pipelineId},buildId: ${task.buildId}"
                )
                return Result(0, "Shutdown the build", true)
            }

            if (task.status == PipelineTaskStatus.RUNNING.status) {
                pipelineDockerTaskDao.updateStatusAndTag(dslContext, buildId, vmSeqId, PipelineTaskStatus.QUEUE, "")
                AlertUtils.doAlert(
                    AlertLevel.LOW, "Docker构建机启动任务异常", "Docker构建机启动任务异常，任务已重试，异常ip: ${task.hostTag}, " +
                        "projectId: ${task.projectId}, pipelineId: ${task.pipelineId},buildId: ${task.buildId}"
                )
            }
        } finally {
            redisLock.unlock()
        }

        return Result(0, "success", true)
    }

    fun log(buildId: String, red: Boolean, message: String) {
        logger.info("write log from docker host, buildId: $buildId, msg: $message")
        if (red) {
            LogUtils.addRedLine(rabbitTemplate, buildId, message, "", 1)
        } else {
            LogUtils.addLine(rabbitTemplate, buildId, message, "", 1)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DockerHostBuildService::class.java)
    }
}