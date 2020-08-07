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

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.dao.TstackBuildDao
import com.tencent.devops.dispatch.dao.TstackConfigDao
import com.tencent.devops.dispatch.dao.TstackVmDao
import com.tencent.devops.dispatch.dao.TstackVolumeDao
import com.tencent.devops.dispatch.pojo.TstackConfig
import com.tencent.devops.dispatch.pojo.TstackContainerInfo
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.pojo.enums.TstackContainerStatus
import com.tencent.devops.dispatch.pojo.enums.TstackVmStatus
import com.tencent.devops.dispatch.pojo.redis.RedisBuild
import com.tencent.devops.dispatch.service.vm.TstackClient
import com.tencent.devops.dispatch.utils.TstackRedisUtils
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.model.dispatch.tables.records.TDispatchTstackBuildRecord
import com.tencent.devops.model.dispatch.tables.records.TDispatchTstackVmRecord
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import com.tencent.devops.process.pojo.mq.PipelineBuildMessage
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class TstackBuildService @Autowired constructor(
    private val dslContext: DSLContext,
    private val tstackConfigDao: TstackConfigDao,
    private val tstackVolumeDao: TstackVolumeDao,
    private val tstackBuildDao: TstackBuildDao,
    private val tstackVmDao: TstackVmDao,
    private val tstackClient: TstackClient,
    private val redisUtils: TstackRedisUtils,
    private val redisOperation: RedisOperation,
    private val buildLogPrinter: BuildLogPrinter
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TstackBuildService::class.java)
        private val executorService = Executors.newFixedThreadPool(20)
    }

    fun startDebug(projectId: String, pipelineId: String, vmSeqId: String): Boolean {
        val containerKey = buildContainerKey(projectId, pipelineId, vmSeqId)
        var containerInfo = redisUtils.getTstackContainerInfo(containerKey)
        if (containerInfo == null || containerInfo.status == TstackContainerStatus.ERROR) {
            startPrepareContainer(projectId, pipelineId, vmSeqId, true)
        } else {
            containerInfo.debugOn = true
            redisUtils.setTstackContainerInfo(containerKey, containerInfo)
        }
        return true
    }

    fun stopDebug(projectId: String, pipelineId: String, vmSeqId: String): Boolean {
        val containerKey = buildContainerKey(projectId, pipelineId, vmSeqId)
        var containerInfo = redisUtils.getTstackContainerInfo(containerKey)
        if (containerInfo != null && containerInfo.status != TstackContainerStatus.ERROR) {
            val build = redisUtils.getTstackRedisBuild(containerInfo.vmIp)
            if (build != null) {
                containerInfo.debugOn = false
                redisUtils.setTstackContainerInfo(containerKey, containerInfo)
            } else {
                redisUtils.deleteTstackContainerInfo(containerKey)
                try {
                    logger.info("start detach volumn ${containerInfo.volumeId} from vm tstackBuildRecord.vmId")
                    if (containerInfo.volumeId != null) {
                        tstackClient.detachVolume(containerInfo.tstackVmId, containerInfo.volumeId!!)
                    }
                    logger.info("detach volumn ${containerInfo.volumeId}.volumeId from vm tstackBuildRecord.vmId success")
                } catch (e: Exception) {
                    logger.warn("detach tstack volume failed")
                }
                tstackVmDao.updateStatus(dslContext, containerInfo.vmId, TstackVmStatus.RECYCLABLE.name)
            }
        }
        return true
    }

    fun getContainerInfo(projectId: String, pipelineId: String, vmSeqId: String): TstackContainerInfo? {
        val containerKey = buildContainerKey(projectId, pipelineId, vmSeqId)
        return redisUtils.getTstackContainerInfo(containerKey)
    }

    fun getContainerInfoWithToken(projectId: String, pipelineId: String, vmSeqId: String): TstackContainerInfo? {
        val containerKey = buildContainerKey(projectId, pipelineId, vmSeqId)
        var tstackContainerInfo = redisUtils.getTstackContainerInfo(containerKey) ?: return null
        if (tstackContainerInfo.status == TstackContainerStatus.RUNNING) {
            tstackContainerInfo.vncToken = tstackClient.createVncToken(tstackContainerInfo.tstackVmId!!)
        }
        return tstackContainerInfo
    }

    fun startPrepareContainer(projectId: String, pipelineId: String, vmSeqId: String, debugOn: Boolean): TstackContainerInfo {
        logger.info("enter startPrepareContainer, projectId: $projectId, pipelineId: $pipelineId, vmSeqId: $vmSeqId, debugOn: $debugOn")
        val containerKey = buildContainerKey(projectId, pipelineId, vmSeqId)
        var containerInfo = redisUtils.getTstackContainerInfo(containerKey)
        if (containerInfo != null && containerInfo.status != TstackContainerStatus.ERROR) {
            logger.info("container is preparing or running, skip")
            return containerInfo
        }

        val availableBuildVms = tstackVmDao.listVmByStatus(dslContext, TstackVmStatus.AVAILABLE.name)
        if (availableBuildVms.isEmpty()) {
            throw RuntimeException("no available tstack vm")
        }
        val buildVm: TDispatchTstackVmRecord = selectOneVmAndLock(availableBuildVms)
        containerInfo = TstackContainerInfo(
                projectId,
                pipelineId,
                vmSeqId,
                buildVm.id,
                buildVm.tstackVmId,
                buildVm.vmIp,
                buildVm.vmName,
                null,
                null,
                debugOn,
                TstackContainerStatus.PREPARRING
        )
        redisUtils.setTstackContainerInfo(containerKey, containerInfo)
        executorService.execute {
            try {
                val pVolumeId = attachVolume(buildVm.tstackVmId, pipelineId, vmSeqId)
                containerInfo.apply {
                    volumeId = pVolumeId
                    status = TstackContainerStatus.RUNNING
                }
                redisUtils.setTstackContainerInfo(containerKey, containerInfo)
            } catch (e: Exception) {
                logger.error("prepare tstack container failed", e)
                if (buildVm != null) {
                    tstackVmDao.updateStatus(dslContext, buildVm.id, TstackVmStatus.AVAILABLE.name)
                }
                containerInfo.status = TstackContainerStatus.ERROR
                redisUtils.setTstackContainerInfo(containerKey, containerInfo)
            }
        }
        return containerInfo
    }

    fun selectOneVmAndLock(availableBuildVms: List<TDispatchTstackVmRecord>): TDispatchTstackVmRecord {
        availableBuildVms.forEach { buildVm ->
            val lockKey = buildVmLockKey(buildVm.vmIp)
            val redisLock = RedisLock(redisOperation, lockKey, 10L)
            try {
                var lockSuccess = redisLock.tryLock()
                if (lockSuccess) {
                    logger.info("lock vm(${buildVm.tstackVmId}|${buildVm.vmIp}) success")
                    tstackVmDao.updateStatus(dslContext, buildVm.id, TstackVmStatus.BUILDING.name)
                    return buildVm
                }
            } finally {
                redisLock.unlock()
            }
        }
        throw RuntimeException("select stack vm failed")
    }

    fun getTstackConfig(projectId: String): TstackConfig {
        val tstackConfig = tstackConfigDao.getConfig(dslContext, projectId)
        return if (tstackConfig == null) {
            TstackConfig(projectId, false)
        } else {
            TstackConfig(projectId, tstackConfig.tstackEnable)
        }
    }

    private fun buildContainerKey(projectId: String, pipelineId: String, vmSeqId: String): String {
        return "tstack_container_${projectId}_${pipelineId}_$vmSeqId"
    }

    private fun buildVmLockKey(ip: String): String {
        return "tstack_vm_lock_key_$ip"
    }

    fun startTstackBuild(buildMessage: PipelineBuildMessage): Boolean {
        logger.info("try start Tstack build")
        val tstackConfig = tstackConfigDao.getConfig(dslContext, buildMessage.projectId)
        if (tstackConfig == null || !tstackConfig.tstackEnable) {
            logger.error("TStack VM Build is disable. Please check project config and retry")
            buildLogPrinter.addLine(buildMessage.buildId, "TStack VM build is disable. Please check project config and retry",
                "", "", buildMessage.executeCount ?: 1)
            throw RuntimeException("TStack VM Build is disabled. Please check project config and retry")
        }

        var containerInfo = getContainerInfo(buildMessage.projectId, buildMessage.pipelineId, buildMessage.vmSeqId)
        if (containerInfo == null || containerInfo.status == TstackContainerStatus.ERROR) {
            startPrepareContainer(buildMessage.projectId, buildMessage.pipelineId, buildMessage.vmSeqId, false)
        }

        val start = System.currentTimeMillis()
        loop@ while (true) {
            Thread.sleep(5 * 1000)
            val now = System.currentTimeMillis()
            if (now - start > 1800 * 1000) {
                logger.error("tstack container timeout(1800s)")
                throw RuntimeException("tstack container timeout(1800s)")
            }

            containerInfo = getContainerInfo(buildMessage.projectId, buildMessage.pipelineId, buildMessage.vmSeqId)
            when {
                containerInfo == null -> {
                    logger.error("tstack container not found")
                    throw RuntimeException("tstack container not found")
                }
                containerInfo.status == TstackContainerStatus.ERROR -> {
                    logger.error("tstack container state error")
                    throw RuntimeException("tstack container state error")
                }
                containerInfo.status == TstackContainerStatus.RUNNING -> {
                    logger.info("tstack container is running")
                    buildLogPrinter.addLine(buildMessage.buildId, "tstack vm(${containerInfo.vmName}) is ready for current build",
                        "", "", buildMessage.executeCount ?: 1)
                    val build = RedisBuild(
                            containerInfo.vmName,
                            buildMessage.projectId,
                            buildMessage.pipelineId,
                            buildMessage.buildId,
                            buildMessage.vmSeqId,
                            buildMessage.channelCode.name,
                            buildMessage.zone
                    )
                    redisUtils.setTstackRedisBuild(containerInfo.vmIp, build)
                    tstackBuildDao.insertBuild(dslContext,
                            buildMessage.projectId,
                            buildMessage.pipelineId,
                            buildMessage.buildId,
                            buildMessage.vmSeqId,
                            containerInfo.vmId.toString(),
                            containerInfo.tstackVmId!!,
                            containerInfo.vmIp,
                            containerInfo.volumeId!!,
                            PipelineTaskStatus.RUNNING)
                    return true
                }
                else -> continue@loop
            }
        }
    }

    fun startTstackBuild(pipelineAgentStartupEvent: PipelineAgentStartupEvent): Boolean {
        logger.info("try start Tstack build")
        val tstackConfig = tstackConfigDao.getConfig(dslContext, pipelineAgentStartupEvent.projectId)
        if (tstackConfig == null || !tstackConfig.tstackEnable) {
            logger.error("TStack VM Build is disable. Please check project config and retry")
            buildLogPrinter.addLine(pipelineAgentStartupEvent.buildId, "TStack VM build is disable. Please check project config and retry", "",
                pipelineAgentStartupEvent.containerHashId, pipelineAgentStartupEvent.executeCount ?: 1)
            throw RuntimeException("TStack VM Build is disabled. Please check project config and retry")
        }

        var containerInfo = getContainerInfo(pipelineAgentStartupEvent.projectId, pipelineAgentStartupEvent.pipelineId, pipelineAgentStartupEvent.vmSeqId)
        if (containerInfo == null || containerInfo.status == TstackContainerStatus.ERROR) {
            startPrepareContainer(pipelineAgentStartupEvent.projectId, pipelineAgentStartupEvent.pipelineId, pipelineAgentStartupEvent.vmSeqId, false)
        }

        val start = System.currentTimeMillis()
        loop@ while (true) {
            Thread.sleep(5 * 1000)
            val now = System.currentTimeMillis()
            if (now - start > 1800 * 1000) {
                logger.error("tstack container timeout(1800s)")
                throw RuntimeException("tstack container timeout(1800s)")
            }

            containerInfo = getContainerInfo(pipelineAgentStartupEvent.projectId, pipelineAgentStartupEvent.pipelineId, pipelineAgentStartupEvent.vmSeqId)
            when {
                containerInfo == null -> {
                    logger.error("tstack container not found")
                    throw RuntimeException("tstack container not found")
                }
                containerInfo.status == TstackContainerStatus.ERROR -> {
                    logger.error("tstack container state error")
                    throw RuntimeException("tstack container state error")
                }
                containerInfo.status == TstackContainerStatus.RUNNING -> {
                    logger.info("tstack container is running")
                    buildLogPrinter.addLine(pipelineAgentStartupEvent.buildId, "tstack vm(${containerInfo.vmName}) is ready for current build",
                        "", pipelineAgentStartupEvent.containerHashId, pipelineAgentStartupEvent.executeCount ?: 1)
                    val build = RedisBuild(
                        containerInfo.vmName,
                        pipelineAgentStartupEvent.projectId,
                        pipelineAgentStartupEvent.pipelineId,
                        pipelineAgentStartupEvent.buildId,
                        pipelineAgentStartupEvent.vmSeqId,
                        pipelineAgentStartupEvent.channelCode,
                        pipelineAgentStartupEvent.zone,
                        pipelineAgentStartupEvent.atoms
                    )
                    redisUtils.setTstackRedisBuild(containerInfo.vmIp, build)
                    tstackBuildDao.insertBuild(dslContext,
                        pipelineAgentStartupEvent.projectId,
                        pipelineAgentStartupEvent.pipelineId,
                        pipelineAgentStartupEvent.buildId,
                        pipelineAgentStartupEvent.vmSeqId,
                        containerInfo.vmId.toString(),
                        containerInfo.tstackVmId,
                        containerInfo.vmIp,
                        containerInfo.volumeId!!,
                        PipelineTaskStatus.RUNNING)
                    return true
                }
                else -> continue@loop
            }
        }
    }

    private fun attachVolume(tstackVmId: String, pipelineId: String, vmSeqId: String): String {
        val existVolume = tstackVolumeDao.getVolume(dslContext, pipelineId, vmSeqId)
        return if (existVolume == null) {
            val volumeId = tstackClient.syncCreateVolume(pipelineId, vmSeqId)
            tstackClient.attachVolume(tstackVmId, volumeId)
            tstackVolumeDao.insertVolume(dslContext, volumeId, pipelineId, vmSeqId)
            volumeId
        } else {
            tstackClient.attachVolume(tstackVmId, existVolume.volumeId)
            existVolume.volumeId
        }
    }

    fun finishTstackBuild(buildId: String, vmSeqId: String?, success: Boolean) {
        logger.info("Finish tstack build of buildId($buildId) and vmSeqId($vmSeqId) with result($success)")
        if (vmSeqId.isNullOrBlank()) {
            val record = tstackBuildDao.listBuilds(dslContext, buildId)
            if (record.isEmpty()) {
                return
            }
            record.forEach {
                finishBuild(it, success)
            }
        } else {
            val record = tstackBuildDao.getBuild(dslContext, buildId, vmSeqId!!)
            if (record != null) {
                finishBuild(record, success)
            }
        }
    }

    private fun finishBuild(tstackBuildRecord: TDispatchTstackBuildRecord, success: Boolean) {
        logger.info("enter finishBuild")
        tstackBuildDao.updateStatus(dslContext,
                tstackBuildRecord.buildId,
                tstackBuildRecord.vmSeqId,
                if (success) PipelineTaskStatus.DONE else PipelineTaskStatus.FAILURE)

        redisUtils.deleteTstackRedisBuild(tstackBuildRecord.vmIp)

        val containerKey = buildContainerKey(tstackBuildRecord.projectId, tstackBuildRecord.pipelineId, tstackBuildRecord.vmSeqId)
        var containerInfo = redisUtils.getTstackContainerInfo(containerKey)
        if (containerInfo != null && containerInfo.debugOn) {
            logger.info("container debug is on, keep container")
            return
        }

        redisUtils.deleteTstackContainerInfo(containerKey)
        try {
            logger.info("start detach volumn ${tstackBuildRecord.volumeId} from vm ${tstackBuildRecord.vmId}")
            tstackClient.detachVolume(tstackBuildRecord.vmId, tstackBuildRecord.volumeId)
            logger.info("detach volumn ${tstackBuildRecord.volumeId} from vm ${tstackBuildRecord.vmId} success")
        } catch (e: Exception) {
            logger.warn("detach tstack volume failed")
        }
        tstackVmDao.updateStatus(dslContext, tstackBuildRecord.agentId.toLong(), TstackVmStatus.RECYCLABLE.name)
    }

    fun getGreyWebConsoleProject(): List<String> {
        return tstackConfigDao.getGreyWebConsoleProjects(dslContext).map { it.projectId }
    }
}
