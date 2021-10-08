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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.dispatch.docker.client.DockerHostClient
import com.tencent.devops.dispatch.docker.config.DefaultImageConfig
import com.tencent.devops.dispatch.docker.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerEnableDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerHostDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerHostZoneDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerPoolDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerTaskDao
import com.tencent.devops.dispatch.docker.pojo.DockerHostInfo
import com.tencent.devops.dispatch.docker.utils.DockerHostUtils
import com.tencent.devops.dispatch.docker.utils.RedisUtils
import com.tencent.devops.dispatch.docker.pojo.ContainerInfo
import com.tencent.devops.dispatch.docker.pojo.DockerHostLoad
import com.tencent.devops.dispatch.docker.pojo.Load
import com.tencent.devops.dispatch.docker.pojo.enums.DockerHostClusterType
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerBuildRecord
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.store.api.image.service.ServiceStoreImageResource
import com.tencent.devops.store.pojo.image.response.ImageRepoInfo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Suppress("ALL")
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

    fun getDockerHostLoad(userId: String): DockerHostLoad {
        try {
            return DockerHostLoad(
                // 页面展示的key和文档对齐
                clusterLoad = mapOf(
                    "docker" to getLoad(DockerHostClusterType.COMMON),
                    "agentless" to getLoad(DockerHostClusterType.AGENT_LESS),
                    "macos-10.15" to getLoad(DockerHostClusterType.MACOS)
                )
            )
        } catch (e: Exception) {
            LOG.error("$userId getDockerHostLoad error.", e)
            throw RuntimeException("getDockerHostLoad error.")
        }
    }

    private fun getLoad(clusterType: DockerHostClusterType): Load {
        val dockerIpList = pipelineDockerIPInfoDao.getDockerIpList(
            dslContext = dslContext,
            enable = true,
            grayEnv = false,
            clusterName = clusterType
        )

        if (dockerIpList.isEmpty()) {
            return Load(
                averageCpuLoad = 0,
                averageMemLoad = 0,
                averageDiskIOLoad = 0,
                averageDiskLoad = 0,
                usedNum = 0,
                enableNode = dockerIpList.size,
                totalNode = pipelineDockerIPInfoDao.getAllDockerIpCount(
                    dslContext = dslContext,
                    grayEnv = false,
                    clusterName = clusterType
                )?.toInt() ?: 0
            )
        }

        var totalCpu = 0
        var totalMem = 0
        var totalDiskIO = 0
        var totalDisk = 0
        var totalUsed = 0
        dockerIpList.forEach {
            totalCpu += it.cpuLoad
            totalMem += it.memLoad
            totalDiskIO += it.diskIoLoad
            totalDisk += it.diskLoad
            totalUsed += it.usedNum
        }

        return Load(
            averageCpuLoad = totalCpu / dockerIpList.size,
            averageMemLoad = totalMem / dockerIpList.size,
            averageDiskIOLoad = totalDiskIO / dockerIpList.size,
            averageDiskLoad = totalDisk / dockerIpList.size,
            usedNum = totalUsed / dockerIpList.size,
            enableNode = dockerIpList.size,
            totalNode = pipelineDockerIPInfoDao.getAllDockerIpCount(
                dslContext = dslContext,
                grayEnv = false,
                clusterName = clusterType
            )?.toInt() ?: 0
        )
    }

/*    fun dockerHostBuild(event: PipelineAgentStartupEvent) {
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

            LOG.info("${event.buildId}|dockerHostBuild|$agentId|$dockerImage|${dispatchType.imageCode}|" +
                "${dispatchType.imageVersion}|${dispatchType.credentialId}|${dispatchType.credentialProject})")

            var userName: String? = null
            var password: String? = null
            if (dispatchType.imageType == ImageType.THIRD) {
                if (!dispatchType.credentialId.isNullOrBlank()) {
                    val projectId = if (dispatchType.credentialProject.isNullOrBlank()) {
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
                    else -> {
                        with(dispatchType) {
                            throw UnknownImageType(
                                "imageCode:$imageCode,imageVersion:$imageVersion,imageType:$imageType")
                        }
                    }
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
    }*/

/*    private fun saveDockerInfoToBuildDetail(
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
    }*/

    fun finishDockerBuild(event: PipelineAgentShutdownEvent) {
        LOG.info("${event.buildId}|finishDockerBuild|vmSeqId(${event.vmSeqId})|result(${event.buildResult})")
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
        } catch (ignore: Exception) {
            LOG.warn("${event.buildId}|finishDockerFail|vmSeqId=${event.vmSeqId}|result=${event.buildResult}", ignore)
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

    private fun finishBuild(
        record: TDispatchPipelineDockerBuildRecord,
        success: Boolean,
        buildLessFlag: Boolean = false
    ) {
        LOG.info("Finish the docker build(${record.buildId}) with result($success)")
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
            redisUtils.deleteHeartBeat(record.buildId, record.vmSeqId.toString())

            // 无编译环境清除redisAuth
            if (buildLessFlag) {
                redisUtils.deleteDockerBuild(record.id, SecurityUtil.decrypt(record.secretKey))
            }
        } catch (e: Exception) {
            LOG.warn("Finish the docker build(${record.buildId}) error.", e)
        }
    }

/*    fun startBuild(hostTag: String): Result<DockerHostBuildInfo>? {
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
                    LOG.info("[$hostTag|$grayFlag] Start docker build with hostIp: $hostTag")
                } else if (hostZone != null) { // 再按区域取值
                    task = pipelineDockerTaskDao.getQueueTasksByProj(dslContext = dslContext,
                        projectIds = grayProjectSet,
                        zone = Zone.valueOf(hostZone.zone))
                    if (task.isNotEmpty) {
                        LOG.info("[$hostTag|$grayFlag] Start docker build with zone: ${hostZone.zone}")
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
                    LOG.info("[$hostTag|$grayFlag] Start docker build with hostIp: $hostTag")
                } else if (hostZone != null) { // 再按区域取值
                    task = pipelineDockerTaskDao.getQueueTasksExcludeProj(dslContext = dslContext,
                        projectIds = grayProjectSet,
                        zone = Zone.valueOf(hostZone.zone))
                    if (task.isNotEmpty) {
                        LOG.info("[$hostTag|$grayFlag] Start docker build with zone: ${hostZone.zone}")
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
            LOG.info("[$hostTag|$grayFlag]|Start_Docker_Build| $message| watch=$stopWatch")
        }
    }

    fun reportContainerId(buildId: String, vmSeqId: Int, containerId: String, hostTag: String?): Result<Boolean>? {
        LOG.info("[$buildId]|reportContainerId|vmSeqId=$vmSeqId|containerId=$containerId|hostTag=$hostTag")
        pipelineDockerBuildDao.updateContainerIdAndDockerIp(
            dslContext = dslContext,
            buildId = buildId,
            vmSeqId = vmSeqId,
            containerId = containerId,
            dockerIp = hostTag ?: ""
        )
        // pipelineDockerTaskDao.updateContainerId(dslContext, buildId, vmSeqId, containerId, hostTag)

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
                message =
                    "构建镜像无法启动，projectId: ${task.projectId}, pipelineId: ${task.pipelineId},buildId: ${task.buildId}"
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
                message = "固定的Docker构建机启动任务异常，IP：${dockerHost.hostIp}, projectId: ${task.projectId}, " +
                    "pipelineId: ${task.pipelineId},buildId: ${task.buildId}"
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
                pipelineDockerTaskDao.updateStatusAndTag(
                    dslContext = dslContext,
                    buildId = buildId,
                    vmSeqId = vmSeqId,
                    status = PipelineTaskStatus.QUEUE,
                    hostTag = "")
                stopWatch.stop()
                message = "Docker构建机启动任务异常，任务已重试，异常ip: ${task.hostTag}, projectId: ${task.projectId}," +
                    " pipelineId: ${task.pipelineId},buildId: ${task.buildId}"
                AlertUtils.doAlert(level = LOW, title = "Docker构建机启动任务异常", message = message)
            }
        } finally {
            stopWatch.start("unlock")
            redisLock.unlock()
            stopWatch.stop()
            LOG.info("[$buildId|$vmSeqId]|rollbackBuild| $message| watch=$stopWatch")
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
            LOG.info("End the docker build(${build.buildId}) seq(${build.vmSeqId})")
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
            LOG.info("[$hostTag|$grayFlag]|endBuild| $message| watch=$stopWatch")
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
                    LOG.info("CLEAR_TIME_OUT_BUILD_POOL|pool_size=${timeoutPoolTask.size}|clear it.")
                    for (i in timeoutPoolTask.indices) {
                        LOG.info("CLEAR_TIME_OUT_BUILD_POOL|(${timeoutPoolTask[i].pipelineId})|" +
                            "(${timeoutPoolTask[i].vmSeq})|(${timeoutPoolTask[i].poolNo})")
                    }
                    pipelineDockerPoolDao.updateTimeOutPool(dslContext)
                    message = "timeoutPoolTask.size=${timeoutPoolTask.size}"
                }

                // 大于两天状态还是running的build history，并主动关机
                val timeoutBuildList = pipelineDockerBuildDao.getTimeOutBuild(dslContext)
                if (timeoutBuildList.isNotEmpty) {
                    LOG.info("There is ${timeoutBuildList.size} build history have/has already time out, clear it.")
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
                                    LOG.info("updateTimeoutBuild pipelineId:(${timeoutBuildList[i].pipelineId})," +
                                        " buildId:(${timeoutBuildList[i].buildId}), " +
                                        "poolNo:(${timeoutBuildList[i].poolNo})")
                                }
                            }
                        } catch (ignore: Exception) {
                            LOG.warn("updateTimeoutBuild buildId: ${timeoutBuildList[i].buildId} failed", ignore)
                        }
                    }
                }
            }
        } finally {
            redisLock.unlock()
            LOG.info("[$grayFlag]|updateTimeoutPoolTask| $message")
        }
    }

    fun getContainerInfo(buildId: String, vmSeqId: Int): Result<ContainerInfo> {
        LOG.info("get containerId, buildId:$buildId, vmSeqId:$vmSeqId")
        val buildHistory = pipelineDockerBuildDao.getBuild(dslContext, buildId, vmSeqId)
        if (buildHistory == null) {
            LOG.warn("The build history not exists, buildId:$buildId, vmSeqId:$vmSeqId")
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

/*    fun buildLessDockerHost(event: PipelineBuildLessStartupDispatchEvent) {
        LOG.info("[${event.buildId}]|BUILD_LESS| Start docker host build ($event)}")
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
            LOG.info("[${event.buildId}]|BUILD_LESS| secretKey: $secretKey agentId: $agentId")

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
        }
    }*/

    fun getHost(hostTag: String): Result<DockerHostInfo>? {
        val hostZone = pipelineDockerHostZoneDao.getHostZone(dslContext, hostTag)
        LOG.info("[getHost]| hostTag=$hostTag, hostZone=$hostZone")
        return if (hostZone == null) {
            Result(DockerHostInfo(MQ.DEFAULT_BUILD_LESS_DOCKET_HOST_ROUTE_SUFFIX))
        } else {
            Result(DockerHostInfo(hostZone.routeKey ?: MQ.DEFAULT_BUILD_LESS_DOCKET_HOST_ROUTE_SUFFIX))
        }
    }

    fun log(buildId: String, red: Boolean, message: String, tag: String? = "", jobId: String? = "") {
        LOG.info("write log from docker host, buildId: $buildId, msg: $message, tag: $tag, jobId= $jobId")
        if (red) {
            buildLogPrinter.addRedLine(buildId, message, tag ?: "", jobId ?: "", 1)
        } else {
            buildLogPrinter.addLine(buildId, message, tag ?: "", jobId ?: "", 1)
        }
    }

    fun getPublicImage(): Result<List<ImageRepoInfo>> {
        LOG.info("enter getPublicImage")
        return client.get(ServiceStoreImageResource::class).getSelfDevelopPublicImages()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DockerHostBuildService::class.java)
    }
}
