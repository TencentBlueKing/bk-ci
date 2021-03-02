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

package com.tencent.devops.dispatch.service.dispatcher.docker

import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.dispatch.client.DockerHostClient
import com.tencent.devops.dispatch.common.ErrorCodeEnum
import com.tencent.devops.dispatch.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.dao.PipelineDockerHostDao
import com.tencent.devops.dispatch.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.dao.PipelineDockerTaskSimpleDao
import com.tencent.devops.dispatch.exception.DockerServiceException
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.service.DockerHostBuildService
import com.tencent.devops.dispatch.service.dispatcher.Dispatcher
import com.tencent.devops.dispatch.utils.DockerHostUtils
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component@Suppress("ALL")
class DockerDispatcher @Autowired constructor(
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter,
    private val dockerHostBuildService: DockerHostBuildService,
    private val dockerHostClient: DockerHostClient,
    private val dockerHostUtils: DockerHostUtils,
    private val pipelineDockerTaskSimpleDao: PipelineDockerTaskSimpleDao,
    private val pipelineDockerIpInfoDao: PipelineDockerIPInfoDao,
    private val pipelineDockerHostDao: PipelineDockerHostDao,
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val dslContext: DSLContext
) : Dispatcher {

    companion object {
        private val logger = LoggerFactory.getLogger(DockerDispatcher::class.java)
    }

    override fun canDispatch(event: PipelineAgentStartupEvent) =
        event.dispatchType is DockerDispatchType

    override fun startUp(event: PipelineAgentStartupEvent) {
        val dockerDispatch = event.dispatchType as DockerDispatchType
        buildLogPrinter.addLine(
            buildId = event.buildId,
            message = "Start docker ${dockerDispatch.dockerBuildVersion} for the build",
            tag = VMUtils.genStartVMTaskId(event.vmSeqId),
            jobId = event.containerHashId,
            executeCount = event.executeCount ?: 1
        )

        var errorCode = "0"
        var errorMessage = ""
        val startTime = System.currentTimeMillis()

        var poolNo = 0
        try {
            // 先判断是否OP已配置专机，若配置了专机，看当前ip是否在专机列表中，若在 选择当前IP并检查负载，若不在从专机列表中选择一个容量最小的
            val specialIpSet = pipelineDockerHostDao.getHostIps(dslContext, event.projectId).toSet()
            logger.info("${event.projectId}| specialIpSet: $specialIpSet")

            val taskHistory = pipelineDockerTaskSimpleDao.getByPipelineIdAndVMSeq(
                dslContext = dslContext,
                pipelineId = event.pipelineId,
                vmSeq = event.vmSeqId
            )

            var driftIpInfo = ""
            val dockerPair: Pair<String, Int>
            poolNo = dockerHostUtils.getIdlePoolNo(event.pipelineId, event.vmSeqId)
            if (taskHistory != null) {
                val dockerIpInfo = pipelineDockerIpInfoDao.getDockerIpInfo(dslContext, taskHistory.dockerIp)
                if (dockerIpInfo == null) {
                    // 此前IP下架，重新选择，根据负载条件选择可用IP
                    dockerPair = dockerHostUtils.getAvailableDockerIpWithSpecialIps(
                        projectId = event.projectId,
                        pipelineId = event.pipelineId,
                        vmSeqId = event.vmSeqId,
                        specialIpSet = specialIpSet
                    )
                } else {
                    driftIpInfo = JsonUtil.toJson(dockerIpInfo.intoMap())

                    dockerPair = if (specialIpSet.isNotEmpty() && specialIpSet.toString() != "[]") {
                        // 该项目工程配置了专机
                        if (specialIpSet.contains(taskHistory.dockerIp) && dockerIpInfo.enable) {
                            // 上一次构建IP在专机列表中，直接重用
                            Pair(taskHistory.dockerIp, dockerIpInfo.dockerHostPort)
                        } else {
                            // 不在专机列表中，重新依据专机列表去选择负载最小的
                            driftIpInfo = "专机漂移"

                            dockerHostUtils.getAvailableDockerIpWithSpecialIps(
                                event.projectId,
                                event.pipelineId,
                                event.vmSeqId,
                                specialIpSet
                            )
                        }
                    } else {
                        // 没有配置专机，根据当前IP负载选择IP
                        val triple = dockerHostUtils.checkAndSetIP(event, specialIpSet, dockerIpInfo, poolNo)
                        if (triple.third.isNotEmpty()) {
                            driftIpInfo = triple.third
                        }
                        Pair(triple.first, triple.second)
                    }
                }
            } else {
                // 第一次构建，根据负载条件选择可用IP
                dockerPair = dockerHostUtils.getAvailableDockerIpWithSpecialIps(
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    vmSeqId = event.vmSeqId,
                    specialIpSet = specialIpSet
                )
            }

            dockerHostClient.startBuild(
                event = event,
                dockerIp = dockerPair.first,
                dockerHostPort = dockerPair.second,
                poolNo = poolNo,
                driftIpInfo = driftIpInfo
            )
        } catch (e: Exception) {
            val errMsgTriple = if (e is DockerServiceException) {
                logger.warn("${event.buildId}|Start build Docker VM failed. ${e.message}")
                Triple(e.errorType, e.errorCode, e.message!!)
            } else {
                logger.error(
                    "[${event.projectId}|${event.pipelineId}|${event.buildId}] Start build Docker VM failed.",
                    e
                )
                Triple(ErrorType.SYSTEM, ErrorCodeEnum.SYSTEM_ERROR.errorCode, "Start build Docker VM failed.")
            }

            errorCode = errMsgTriple.second.toString()
            errorMessage = errMsgTriple.third

            // 更新构建记录状态
            val result = pipelineDockerBuildDao.updateStatus(
                dslContext,
                event.buildId,
                event.vmSeqId.toInt(),
                PipelineTaskStatus.FAILURE
            )

            if (!result) {
                pipelineDockerBuildDao.startBuild(
                    dslContext = dslContext,
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    buildId = event.buildId,
                    vmSeqId = event.vmSeqId.toInt(),
                    secretKey = "",
                    status = PipelineTaskStatus.FAILURE,
                    zone = if (null == event.zone) {
                        Zone.SHENZHEN.name
                    } else {
                        event.zone!!.name
                    },
                    dockerIp = "",
                    poolNo = poolNo
                )
            }

            onFailBuild(
                client = client,
                buildLogPrinter = buildLogPrinter,
                event = event,
                errorType = errMsgTriple.first,
                errorCode = errMsgTriple.second,
                errorMsg = errMsgTriple.third,
                third = false
            )
        } finally {
            try {
                sendDispatchMonitoring(
                    client = client,
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    buildId = event.buildId,
                    vmSeqId = event.vmSeqId,
                    actionType = event.actionType.name,
                    retryTime = event.retryTime,
                    routeKeySuffix = event.routeKeySuffix ?: "dockerOnVM",
                    startTime = startTime,
                    stopTime = 0L,
                    errorCode = errorCode,
                    errorMessage = errorMessage,
                    errorType = ""
                )
            } catch (ignore: Exception) {
                logger.warn("[${event.buildId}|startup sendDispatchMonitoring error.", ignore)
            }
        }
    }

    override fun shutdown(event: PipelineAgentShutdownEvent) {
        logger.info("${event.buildId}|On shutdown")
        try {
            dockerHostBuildService.finishDockerBuild(event)
        } finally {
            try {
                sendDispatchMonitoring(
                    client = client,
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    buildId = event.buildId,
                    vmSeqId = event.vmSeqId ?: "",
                    actionType = event.actionType.name,
                    retryTime = event.retryTime,
                    routeKeySuffix = event.routeKeySuffix ?: "dockerOnVM",
                    startTime = 0L,
                    stopTime = System.currentTimeMillis(),
                    errorCode = "0",
                    errorMessage = "",
                    errorType = ""
                )
            } catch (ignored: Exception) {
                logger.warn("${event.buildId}|shutdown sendDispatchMonitoring error.", ignored)
            }
        }
    }
}
