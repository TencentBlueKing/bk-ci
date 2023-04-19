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

package com.tencent.devops.dispatch.docker.listener

import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.listener.BuildListener
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.dispatch.sdk.pojo.docker.DockerRoutingType
import com.tencent.devops.common.dispatch.sdk.service.DockerRoutingSdkService
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.common.pipeline.type.DispatchRouteKeySuffix
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.pipeline.type.kubernetes.KubernetesDispatchType
import com.tencent.devops.dispatch.docker.client.DockerHostClient
import com.tencent.devops.dispatch.docker.common.ErrorCodeEnum
import com.tencent.devops.dispatch.docker.config.DefaultImageConfig
import com.tencent.devops.dispatch.docker.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerHostDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerTaskSimpleDao
import com.tencent.devops.dispatch.docker.exception.DockerServiceException
import com.tencent.devops.dispatch.docker.pojo.Credential
import com.tencent.devops.dispatch.docker.pojo.Pool
import com.tencent.devops.dispatch.docker.service.DockerHostBuildService
import com.tencent.devops.dispatch.docker.utils.CommonUtils
import com.tencent.devops.dispatch.docker.utils.DockerHostUtils
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service@Suppress("ALL")
class DockerVMListener @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val buildLogPrinter: BuildLogPrinter,
    private val defaultImageConfig: DefaultImageConfig,
    private val dockerHostBuildService: DockerHostBuildService,
    private val dockerHostClient: DockerHostClient,
    private val dockerHostUtils: DockerHostUtils,
    private val pipelineDockerTaskSimpleDao: PipelineDockerTaskSimpleDao,
    private val pipelineDockerIpInfoDao: PipelineDockerIPInfoDao,
    private val pipelineDockerHostDao: PipelineDockerHostDao,
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val dockerRoutingSdkService: DockerRoutingSdkService,
    private val pipelineEventDispatcher: SampleEventDispatcher
) : BuildListener {

    companion object {
        private val logger = LoggerFactory.getLogger(DockerVMListener::class.java)
    }

    override fun getShutdownQueue(): String {
        return DispatchRouteKeySuffix.DOCKER_VM.routeKeySuffix
    }

    override fun getStartupQueue(): String {
        return DispatchRouteKeySuffix.DOCKER_VM.routeKeySuffix
    }

    override fun getStartupDemoteQueue(): String {
        return DispatchRouteKeySuffix.DOCKER_VM_DEMOTE.routeKeySuffix
    }

    override fun getVmType(): JobQuotaVmType? {
        return JobQuotaVmType.DOCKER_VM
    }

    override fun onStartup(dispatchMessage: DispatchMessage) {
        logger.info("On startup - ($dispatchMessage)")
        parseRoutingStartup(dispatchMessage)
    }

    override fun onStartupDemote(dispatchMessage: DispatchMessage) {
        logger.info("On startup demote - ($dispatchMessage)")
        parseRoutingStartup(dispatchMessage, true)
    }

    override fun onShutdown(event: PipelineAgentShutdownEvent) {
        // 广播消息中非DockerDispatchType的消息直接返回
        if (event.dispatchType !is DockerDispatchType) {
            return
        }

        logger.info("On shutdown - ($event)")

        val dockerRoutingType = dockerRoutingSdkService.getDockerRoutingType(event.projectId)
        if (dockerRoutingType == DockerRoutingType.VM) {
            dockerHostBuildService.finishDockerBuild(event)
        } else {
            pipelineEventDispatcher.dispatch(event.copy(
                routeKeySuffix = DispatchRouteKeySuffix.KUBERNETES.routeKeySuffix,
                dockerRoutingType = dockerRoutingType.name
            ))
        }
    }

    private fun parseRoutingStartup(dispatchMessage: DispatchMessage, demoteFlag: Boolean = false) {
        // 广播消息中非DockerDispatchType的消息直接返回
        if (dispatchMessage.dispatchType !is DockerDispatchType) {
            return
        }

        val dispatchType = dispatchMessage.dispatchType as DockerDispatchType
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
        logger.info(
            "${dispatchMessage.buildId}|startBuild|${dispatchMessage.id}|$dockerImage" +
                "|${dispatchType.imageCode}|${dispatchType.imageVersion}|${dispatchType.credentialId}" +
                "|${dispatchType.credentialProject}"
        )
        var userName = dispatchType.imageRepositoryUserName
        var password = dispatchType.imageRepositoryPassword
        if (dispatchType.imageType == ImageType.THIRD) {
            if (!dispatchType.credentialId.isNullOrBlank()) {
                val projectId = if (dispatchType.credentialProject.isNullOrBlank()) {
                    dispatchMessage.projectId
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

        val containerPool = Pool(
            container = dockerImage,
            credential = Credential(userName, password),
            env = null,
            imageType = dispatchType.imageType?.type
        )

        val dockerRoutingType = dockerRoutingSdkService.getDockerRoutingType(dispatchMessage.projectId)
        if (dockerRoutingType == DockerRoutingType.VM) {
            startup(dispatchMessage, containerPool)
        } else {
            startKubernetesDocker(dispatchMessage, containerPool, dockerRoutingType, demoteFlag)
        }
    }

    private fun startKubernetesDocker(
        dispatchMessage: DispatchMessage,
        containerPool: Pool,
        dockerRoutingType: DockerRoutingType = DockerRoutingType.VM,
        demoteFlag: Boolean = false
    ) {
        with(dispatchMessage) {
            pipelineEventDispatcher.dispatch(
                PipelineAgentStartupEvent(
                    source = "vmStartupTaskAtom",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    pipelineName = "",
                    userId = userId,
                    buildId = buildId,
                    buildNo = 0,
                    vmSeqId = containerId,
                    taskName = "",
                    os = "",
                    vmNames = vmNames,
                    channelCode = channelCode,
                    dispatchType = KubernetesDispatchType(
                        kubernetesBuildVersion = JsonUtil.toJson(containerPool),
                        imageType = ImageType.THIRD,
                        performanceConfigId = 0
                    ),
                    atoms = atoms,
                    executeCount = executeCount,
                    routeKeySuffix = if (!demoteFlag) DispatchRouteKeySuffix.KUBERNETES.routeKeySuffix
                    else DispatchRouteKeySuffix.KUBERNETES_DEMOTE.routeKeySuffix,
                    containerId = containerId,
                    containerHashId = containerHashId,
                    customBuildEnv = customBuildEnv,
                    dockerRoutingType = dockerRoutingType.name
                )
            )
        }
    }

    private fun startup(
        dispatchMessage: DispatchMessage,
        containerPool: Pool
    ) {
        val dockerDispatch = dispatchMessage.dispatchType as DockerDispatchType
        buildLogPrinter.addLine(
            buildId = dispatchMessage.buildId,
            message = "Start docker ${dockerDispatch.dockerBuildVersion} for the build",
            tag = VMUtils.genStartVMTaskId(dispatchMessage.vmSeqId),
            jobId = dispatchMessage.containerHashId,
            executeCount = dispatchMessage.executeCount ?: 1
        )

        var poolNo = 0
        try {
            // 先判断是否OP已配置专机，若配置了专机，看当前ip是否在专机列表中，若在 选择当前IP并检查负载，若不在从专机列表中选择一个容量最小的
            val specialIpSet = pipelineDockerHostDao.getHostIps(dslContext, dispatchMessage.projectId)
            logger.info("${dispatchMessage.projectId}| specialIpSet: $specialIpSet -- ${specialIpSet.size}")

            val taskHistory = pipelineDockerTaskSimpleDao.getByPipelineIdAndVMSeq(
                dslContext = dslContext,
                pipelineId = dispatchMessage.pipelineId,
                vmSeq = dispatchMessage.vmSeqId
            )

            var driftIpInfo = ""
            val dockerPair: Pair<String, Int>
            poolNo = dockerHostUtils.getIdlePoolNo(dispatchMessage.pipelineId, dispatchMessage.vmSeqId)
            if (taskHistory != null) {
                val dockerIpInfo = pipelineDockerIpInfoDao.getDockerIpInfo(dslContext, taskHistory.dockerIp)
                if (dockerIpInfo == null) {
                    // 此前IP下架，重新选择，根据负载条件选择可用IP
                    dockerPair = dockerHostUtils.getAvailableDockerIpWithSpecialIps(
                        projectId = dispatchMessage.projectId,
                        pipelineId = dispatchMessage.pipelineId,
                        vmSeqId = dispatchMessage.vmSeqId,
                        specialIpSet = specialIpSet
                    )
                } else {
                    driftIpInfo = JsonUtil.toJson(dockerIpInfo.intoMap())
                    // 根据当前IP负载选择IP
                    val pair = dockerHostUtils.checkAndSetIP(dispatchMessage, specialIpSet, dockerIpInfo, poolNo)
                    dockerPair = Pair(pair.first, pair.second)
                }
            } else {
                // 第一次构建，根据负载条件选择可用IP
                dockerPair = dockerHostUtils.getAvailableDockerIpWithSpecialIps(
                    projectId = dispatchMessage.projectId,
                    pipelineId = dispatchMessage.pipelineId,
                    vmSeqId = dispatchMessage.vmSeqId,
                    specialIpSet = specialIpSet
                )
            }

            dockerHostClient.startBuild(
                dispatchMessage = dispatchMessage,
                dockerIp = dockerPair.first,
                dockerHostPort = dockerPair.second,
                poolNo = poolNo,
                driftIpInfo = driftIpInfo,
                containerPool = containerPool
            )
        } catch (e: Exception) {
            val errMsgTriple = if (e is DockerServiceException) {
                logger.warn("${dispatchMessage.buildId}| Start build Docker VM failed. ${e.message}")
                Triple(e.errorType, e.errorCode, e.message!!)
            } else {
                logger.error("${dispatchMessage.buildId}| Start build Docker VM failed.", e)
                Triple(first = ErrorCodeEnum.SYSTEM_ERROR.errorType,
                    second = ErrorCodeEnum.SYSTEM_ERROR.errorCode,
                    third = "Start build Docker VM failed.")
            }

            // 更新构建记录状态
            val result = pipelineDockerBuildDao.updateStatus(
                dslContext,
                dispatchMessage.buildId,
                dispatchMessage.vmSeqId.toInt(),
                PipelineTaskStatus.FAILURE
            )

            if (!result) {
                pipelineDockerBuildDao.saveBuildHistory(
                    dslContext = dslContext,
                    projectId = dispatchMessage.projectId,
                    pipelineId = dispatchMessage.pipelineId,
                    buildId = dispatchMessage.buildId,
                    vmSeqId = dispatchMessage.vmSeqId.toInt(),
                    secretKey = "",
                    status = PipelineTaskStatus.FAILURE,
                    zone = Zone.SHENZHEN.name,
                    dockerIp = "",
                    poolNo = poolNo
                )
            }

            onFailure(
                errorType = errMsgTriple.first,
                errorCode = errMsgTriple.second,
                formatErrorMessage = errMsgTriple.third,
                message = errMsgTriple.third
            )
        }
    }
}
