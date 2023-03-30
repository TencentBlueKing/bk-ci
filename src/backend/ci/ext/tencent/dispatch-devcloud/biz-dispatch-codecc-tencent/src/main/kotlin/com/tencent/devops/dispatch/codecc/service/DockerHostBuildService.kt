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

package com.tencent.devops.dispatch.codecc.service

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.model.dispatch.codecc.tables.records.TDispatchPipelineDockerBuildRecord
import com.tencent.devops.dispatch.codecc.client.DockerHostClient
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerPoolDao
import com.tencent.devops.dispatch.codecc.pojo.PipelineTaskStatus
import com.tencent.devops.dispatch.codecc.utils.RedisUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class DockerHostBuildService @Autowired constructor(
    private val dslContext: DSLContext,
    private val dockerHostClient: DockerHostClient,
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val pipelineDockerPoolDao: PipelineDockerPoolDao,
    private val pipelineDockerIPInfoDao: PipelineDockerIPInfoDao,
    private val redisUtils: RedisUtils,
    private val redisOperation: RedisOperation,
    private val gray: Gray,
    private val rabbitTemplate: RabbitTemplate
) {

    private val logger = LoggerFactory.getLogger(DockerHostBuildService::class.java)

    private val grayFlag: Boolean = gray.isGray()

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

/*    private fun finishDockerBuild(record: TDispatchPipelineDockerBuildRecord, event: PipelineAgentShutdownEvent) {
        finishBuild(record, event.buildResult)

        // 编译环境才会更新pool
        pipelineDockerPoolDao.updatePoolStatus(
            dslContext,
            record.pipelineId,
            record.vmSeqId.toString(),
            record.poolNo,
            if (event.buildResult) PipelineTaskStatus.DONE.status else PipelineTaskStatus.FAILURE.status
        )

        if (record.dockerIp.isNotEmpty()) {
            dockerHostClient.endBuild(
                event,
                record.dockerIp,
                record.containerId
            )
        }
    }*/

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
            // redisUtils.deleteDockerBuild(record.id, record.secretKey)
            redisUtils.deleteHeartBeat(record.buildId, record.vmSeqId.toString())

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
        } catch (e: Exception) {
            logger.error("Finish the docker build(${record.buildId}) error.", e)
        }
    }

    /**
     * 每120分钟执行一次，更新大于两天状态还是running的pool，以及大于两天状态还是running的build history，并主动关机
     */
    @Scheduled(initialDelay = 120 * 1000, fixedDelay = 3600 * 2 * 1000)
    @Deprecated("this function is deprecated!")
    fun updateTimeoutPoolTask() {
        var message = ""
        val redisLock = RedisLock(redisOperation, "update_timeout_codecc_pool_task_nogkudla", 500L)
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
                                    pipelineDockerBuildDao.updateTimeOutBuild(dslContext, timeoutBuildList[i].buildId)
                                    logger.info("updateTimeoutBuild pipelineId:(${timeoutBuildList[i].pipelineId}), buildId:(${timeoutBuildList[i].buildId}), poolNo:(${timeoutBuildList[i].poolNo})")
                                    try {
                                        dockerHostClient.endBuild(
                                            projectId = timeoutBuildList[i].projectId,
                                            pipelineId = timeoutBuildList[i].pipelineId,
                                            buildId = timeoutBuildList[i].buildId,
                                            vmSeqId = timeoutBuildList[i].vmSeqId,
                                            containerId = timeoutBuildList[i].containerId,
                                            dockerIp = timeoutBuildList[i].dockerIp
                                        )
                                    } catch (e: Exception) {
                                        logger.warn("updateTimeoutBuild endBuild failed, buildId: ${timeoutBuildList[i].buildId}")
                                    }
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
}
