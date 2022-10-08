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
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.utils.DispatchLogRedisUtils
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.docker.client.DockerHostClient
import com.tencent.devops.dispatch.docker.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerEnableDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerPoolDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerTaskDao
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

@Service
class DockerHostBuildService @Autowired constructor(
    private val dslContext: DSLContext,
    private val dockerHostClient: DockerHostClient,
    private val pipelineDockerEnableDao: PipelineDockerEnableDao,
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val pipelineDockerTaskDao: PipelineDockerTaskDao,
    private val pipelineDockerPoolDao: PipelineDockerPoolDao,
    private val pipelineDockerIPInfoDao: PipelineDockerIPInfoDao,
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val buildLogPrinter: BuildLogPrinter,
    private val dockerHostQpcService: DockerHostQpcService
) {

    fun updateContainerId(
        buildId: String,
        vmSeqId: Int,
        containerId: String
    ) {
        LOG.info("$buildId|$vmSeqId update containerId: $containerId")
        pipelineDockerBuildDao.updateContainerId(
            dslContext = dslContext,
            buildId = buildId,
            vmSeqId = vmSeqId,
            containerId = containerId
        )
    }

    fun enable(pipelineId: String, vmSeqId: Int?, enable: Boolean) =
        pipelineDockerEnableDao.enable(dslContext, pipelineId, vmSeqId, enable)

    fun getQpcGitProjectList(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        poolNo: Int
    ): List<String> {
        return if (projectId.startsWith("git_") &&
            dockerHostQpcService.checkQpcWhitelist(projectId.removePrefix("git_"))
        ) {
            return listOf(projectId.removePrefix("git_"))
        } else {
            emptyList()
        }
    }

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
            val record = pipelineDockerBuildDao.getBuild(
                dslContext = dslContext,
                buildId = event.buildId,
                vmSeqId = event.vmSeqId!!.toInt()
            )
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
                    dockerIp = record.dockerIp,
                    poolNo = record.poolNo
                )
            }

            // 只要当容器关机成功时才会更新build_history状态
            finishBuild(record = record, success = event.buildResult)
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

    private fun finishBuild(record: TDispatchPipelineDockerBuildRecord, success: Boolean) {
        LOG.info("Finish the docker build(${record.buildId}) with result($success)")
        try {
            pipelineDockerBuildDao.updateStatus(
                dslContext = dslContext,
                buildId = record.buildId,
                vmSeqId = record.vmSeqId,
                status = if (success) PipelineTaskStatus.DONE else PipelineTaskStatus.FAILURE
            )

            // 更新dockerTask表(保留之前逻辑)
            pipelineDockerTaskDao.updateStatus(dslContext,
                record.buildId,
                record.vmSeqId,
                if (success) PipelineTaskStatus.DONE else PipelineTaskStatus.FAILURE)
        } catch (e: Exception) {
            LOG.warn("Finish the docker build(${record.buildId}) error.", e)
        }
    }

    /**
     * 每天执行一次，更新大于七天状态还是running的pool，以及大于七天状态还是running的build history，并主动关机
     */
    @Scheduled(initialDelay = 120 * 1000, fixedDelay = 3600 * 24 * 1000)
    fun updateTimeoutPoolTask() {
        var message = ""
        val redisLock = RedisLock(redisOperation, "update_timeout_pool_task_nogkudla", 5L)
        try {
            if (redisLock.tryLock()) {
                // 更新大于七天状态还是running的pool
                val timeoutPoolTask = pipelineDockerPoolDao.getTimeOutPool(dslContext)
                LOG.info("CLEAR_TIME_OUT_BUILD_POOL|pool_size=${timeoutPoolTask.size}|clear it.")
                for (i in timeoutPoolTask.indices) {
                    LOG.info("CLEAR_TIME_OUT_BUILD_POOL|(${timeoutPoolTask[i].pipelineId})|" +
                        "(${timeoutPoolTask[i].vmSeq})|(${timeoutPoolTask[i].poolNo})")
                }
                pipelineDockerPoolDao.updateTimeOutPool(dslContext)
                message = "timeoutPoolTask.size=${timeoutPoolTask.size}"

                clearTimeoutBuildHistory()
            }
        } finally {
            redisLock.unlock()
            LOG.info("updateTimeoutPoolTask| $message")
        }
    }

    private fun clearTimeoutBuildHistory() {
        // 大于七天状态还是running的build history，并主动关机
        val timeoutBuildList = pipelineDockerBuildDao.getTimeOutBuild(dslContext)
        LOG.info("There is ${timeoutBuildList.size} build history have/has already time out, clear it.")
        for (i in timeoutBuildList.indices) {
            try {
                val dockerIp = timeoutBuildList[i].dockerIp
                if (dockerIp.isNullOrBlank()) {
                    continue
                }

                val dockerIpInfo = pipelineDockerIPInfoDao.getDockerIpInfo(dslContext, dockerIp)
                if (dockerIpInfo != null && dockerIpInfo.enable) {
                    dockerHostClient.endBuild(
                        projectId = timeoutBuildList[i].projectId,
                        pipelineId = timeoutBuildList[i].pipelineId,
                        buildId = timeoutBuildList[i].buildId,
                        vmSeqId = timeoutBuildList[i].vmSeqId,
                        containerId = timeoutBuildList[i].containerId,
                        dockerIp = timeoutBuildList[i].dockerIp,
                        poolNo = timeoutBuildList[i].poolNo,
                        clusterType = DockerHostClusterType.valueOf(dockerIpInfo.clusterName)
                    )

                    LOG.info("updateTimeoutBuild pipelineId:(${timeoutBuildList[i].pipelineId})," +
                            " buildId:(${timeoutBuildList[i].buildId}), " +
                            "poolNo:(${timeoutBuildList[i].poolNo})")
                }
            } catch (ignore: Exception) {
                LOG.warn("updateTimeoutBuild buildId: ${timeoutBuildList[i].buildId} failed", ignore)
            } finally {
                pipelineDockerBuildDao.updateTimeOutBuild(dslContext, timeoutBuildList[i].buildId)
            }
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

    fun log(buildId: String, red: Boolean, message: String, tag: String? = "", jobId: String? = "") {
        LOG.info("write log from docker host, buildId: $buildId, msg: $message, tag: $tag, jobId= $jobId")
        val executeCount = DispatchLogRedisUtils.getRedisExecuteCount(buildId)
        if (red) {
            buildLogPrinter.addRedLine(buildId, message, tag ?: "", jobId ?: "", executeCount)
        } else {
            buildLogPrinter.addLine(buildId, message, tag ?: "", jobId ?: "", executeCount)
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
