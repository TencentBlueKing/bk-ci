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

package com.tencent.devops.dispatch.service.dispatcher.docker

import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.dispatch.client.DockerHostClient
import com.tencent.devops.dispatch.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.dao.PipelineDockerHostDao
import com.tencent.devops.dispatch.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.dao.PipelineDockerTaskDriftDao
import com.tencent.devops.dispatch.dao.PipelineDockerTaskSimpleDao
import com.tencent.devops.dispatch.exception.DockerServiceException
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.service.DockerHostBuildService
import com.tencent.devops.dispatch.service.dispatcher.Dispatcher
import com.tencent.devops.dispatch.utils.DockerHostUtils
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DockerDispatcher @Autowired constructor(
    private val client: Client,
    private val rabbitTemplate: RabbitTemplate,
    private val dockerHostBuildService: DockerHostBuildService,
    private val dockerHostClient: DockerHostClient,
    private val dockerHostUtils: DockerHostUtils,
    private val pipelineDockerTaskSimpleDao: PipelineDockerTaskSimpleDao,
    private val pipelineDockerTaskDriftDao: PipelineDockerTaskDriftDao,
    private val pipelineDockerIpInfoDao: PipelineDockerIPInfoDao,
    private val pipelineDockerHostDao: PipelineDockerHostDao,
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val dslContext: DSLContext
) : Dispatcher {

    companion object {
        private val logger = LoggerFactory.getLogger(DockerDispatcher::class.java)
    }

    override fun canDispatch(pipelineAgentStartupEvent: PipelineAgentStartupEvent) =
        pipelineAgentStartupEvent.dispatchType is DockerDispatchType

    override fun startUp(pipelineAgentStartupEvent: PipelineAgentStartupEvent) {
        val dockerDispatch = pipelineAgentStartupEvent.dispatchType as DockerDispatchType
        LogUtils.addLine(
            rabbitTemplate = rabbitTemplate,
            buildId = pipelineAgentStartupEvent.buildId,
            message = "Start docker ${dockerDispatch.dockerBuildVersion} for the build",
            tag = VMUtils.genStartVMTaskId(pipelineAgentStartupEvent.vmSeqId),
            jobId = pipelineAgentStartupEvent.containerHashId,
            executeCount = pipelineAgentStartupEvent.executeCount ?: 1
        )

        var poolNo = 0
        try {
            // 先判断是否OP已配置专机，若配置了专机，看当前ip是否在专机列表中，若在 选择当前IP并检查负载，若不在从专机列表中选择一个容量最小的
            val specialIpSet = pipelineDockerHostDao.getHostIps(dslContext, pipelineAgentStartupEvent.projectId).toSet()
            logger.info("${pipelineAgentStartupEvent.projectId}| specialIpSet: $specialIpSet")

            val taskHistory = pipelineDockerTaskSimpleDao.getByPipelineIdAndVMSeq(
                dslContext = dslContext,
                pipelineId = pipelineAgentStartupEvent.pipelineId,
                vmSeq = pipelineAgentStartupEvent.vmSeqId
            )

            val dockerPair: Pair<String, Int>
            poolNo = dockerHostUtils.getIdlePoolNo(pipelineAgentStartupEvent.pipelineId, pipelineAgentStartupEvent.vmSeqId)
            if (taskHistory != null) {
                dockerPair = if (specialIpSet.isNotEmpty() && specialIpSet.toString() != "[]") {
                    // 该项目工程配置了专机
                    if (specialIpSet.contains(taskHistory.dockerIp)) {
                        // 上一次构建IP在专机列表中，直接重用
                        val dockerIpInfo = pipelineDockerIpInfoDao.getDockerIpInfo(dslContext, taskHistory.dockerIp) ?: throw DockerServiceException("Docker IP: ${taskHistory.dockerIp} is not available.")
                        Pair(taskHistory.dockerIp, dockerIpInfo.dockerHostPort)
                    } else {
                        // 不在专机列表中，重新依据专机列表去选择负载最小的
                        resetDockerIp(pipelineAgentStartupEvent, specialIpSet, taskHistory.dockerIp, "专机漂移")
                    }
                } else {
                    // 没有配置专机，根据当前IP负载选择IP
                    checkAndSetIP(pipelineAgentStartupEvent, specialIpSet, taskHistory.dockerIp, poolNo)
                }
            } else {
                // 第一次构建，根据负载条件选择可用IP
                dockerPair = dockerHostUtils.getAvailableDockerIpWithSpecialIps(
                    projectId = pipelineAgentStartupEvent.projectId,
                    pipelineId = pipelineAgentStartupEvent.pipelineId,
                    vmSeqId = pipelineAgentStartupEvent.vmSeqId,
                    specialIpSet = specialIpSet
                )
                pipelineDockerTaskSimpleDao.create(
                    dslContext = dslContext,
                    pipelineId = pipelineAgentStartupEvent.pipelineId,
                    vmSeq = pipelineAgentStartupEvent.vmSeqId,
                    idcIp = dockerPair.first
                )
            }

            dockerHostClient.startBuild(pipelineAgentStartupEvent, dockerPair.first, dockerPair.second, poolNo)
        } catch (e: Exception) {
            val errMsg = if (e is DockerServiceException) {
                logger.warn("[${pipelineAgentStartupEvent.projectId}|${pipelineAgentStartupEvent.pipelineId}|${pipelineAgentStartupEvent.buildId}] Start build Docker VM failed. ${e.message}")
                e.message!!
            } else {
                logger.error(
                    "[${pipelineAgentStartupEvent.projectId}|${pipelineAgentStartupEvent.pipelineId}|${pipelineAgentStartupEvent.buildId}] Start build Docker VM failed.",
                    e
                )
                "Start build Docker VM failed."
            }

            // 更新构建记录状态
            val result = pipelineDockerBuildDao.updateStatus(
                dslContext,
                pipelineAgentStartupEvent.buildId,
                pipelineAgentStartupEvent.vmSeqId.toInt(),
                PipelineTaskStatus.FAILURE
            )

            if (!result) {
                pipelineDockerBuildDao.startBuild(
                    dslContext = dslContext,
                    projectId = pipelineAgentStartupEvent.projectId,
                    pipelineId = pipelineAgentStartupEvent.pipelineId,
                    buildId = pipelineAgentStartupEvent.buildId,
                    vmSeqId = pipelineAgentStartupEvent.vmSeqId.toInt(),
                    secretKey = "",
                    status = PipelineTaskStatus.FAILURE,
                    zone = if (null == pipelineAgentStartupEvent.zone) {
                        Zone.SHENZHEN.name
                    } else {
                        pipelineAgentStartupEvent.zone!!.name
                    },
                    dockerIp = "",
                    poolNo = poolNo
                )
            }

            onFailBuild(client, rabbitTemplate, pipelineAgentStartupEvent, errMsg)
        }
    }

    override fun shutdown(pipelineAgentShutdownEvent: PipelineAgentShutdownEvent) {
        logger.info("On shutdown - ($pipelineAgentShutdownEvent|$)")
        dockerHostBuildService.finishDockerBuild(pipelineAgentShutdownEvent)
    }

    private fun checkAndSetIP(
        pipelineAgentStartupEvent: PipelineAgentStartupEvent,
        specialIpSet: Set<String>,
        oldDockerIp: String,
        poolNo: Int
    ): Pair<String, Int> {
        val ipInfo = pipelineDockerIpInfoDao.getDockerIpInfo(dslContext, oldDockerIp)

        // 同一条流水线并发构建时，无视负载，直接下发同一个IP（避免同一条流水线并发量太大，影响其他流水线构建）
        if (poolNo > 1) {
            return Pair(oldDockerIp, ipInfo!!.dockerHostPort)
        }

        // 查看当前IP负载情况，当前IP可用，且负载未超额（内存低于90%且硬盘低于90%），可直接下发，当负载超额或者设置为专机独享，重新选择构建机
        if (ipInfo == null || !ipInfo.enable || ipInfo.diskLoad > 90 || ipInfo.memLoad > 90 || ipInfo.specialOn) {
            return resetDockerIp(pipelineAgentStartupEvent, specialIpSet, oldDockerIp, if (ipInfo != null) JsonUtil.toJson(ipInfo.intoMap()) else "")
        }

        return Pair(oldDockerIp, ipInfo.dockerHostPort)
    }

    private fun resetDockerIp(pipelineAgentStartupEvent: PipelineAgentStartupEvent, specialIpSet: Set<String>, sourceIp: String, ipInfo: String): Pair<String, Int> {
        val dockerPair = dockerHostUtils.getAvailableDockerIpWithSpecialIps(
            pipelineAgentStartupEvent.projectId,
            pipelineAgentStartupEvent.pipelineId,
            pipelineAgentStartupEvent.vmSeqId,
            specialIpSet
        )
        pipelineDockerTaskSimpleDao.updateDockerIp(
            dslContext,
            pipelineAgentStartupEvent.pipelineId,
            pipelineAgentStartupEvent.vmSeqId,
            dockerPair.first
        )

        // 记录漂移日志
        pipelineDockerTaskDriftDao.create(
            dslContext,
            pipelineAgentStartupEvent.pipelineId,
            pipelineAgentStartupEvent.buildId,
            pipelineAgentStartupEvent.vmSeqId,
            sourceIp,
            dockerPair.first,
            ipInfo
        )

        return dockerPair
    }
}
