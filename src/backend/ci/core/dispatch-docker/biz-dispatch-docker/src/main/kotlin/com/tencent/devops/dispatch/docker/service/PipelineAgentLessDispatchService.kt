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

import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.service.JobQuotaService
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.dispatch.docker.client.BuildLessClient
import com.tencent.devops.dispatch.docker.client.DockerHostClient
import com.tencent.devops.dispatch.docker.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.docker.pojo.enums.DockerHostClusterType
import com.tencent.devops.dispatch.docker.utils.DockerHostUtils
import com.tencent.devops.dispatch.docker.utils.RedisUtils
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerBuildRecord
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.mq.PipelineBuildLessShutdownDispatchEvent
import com.tencent.devops.process.pojo.mq.PipelineBuildLessStartupDispatchEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineAgentLessDispatchService @Autowired constructor(
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter,
    private val jobQuotaService: JobQuotaService,
    private val dockerHostClient: DockerHostClient,
    private val buildLessClient: BuildLessClient,
    private val dockerHostUtils: DockerHostUtils,
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val redisUtils: RedisUtils,
    private val dslContext: DSLContext,
    private val buildLessWhitelistService: BuildLessWhitelistService
) {
    fun startUpBuildLess(event: PipelineBuildLessStartupDispatchEvent) {
        val pipelineId = event.pipelineId
        val buildId = event.buildId
        val vmSeqId = event.vmSeqId
        LOG.info("[$buildId]|BUILD_LESS| pipelineId=$pipelineId, seq($vmSeqId)")
        // Check if the pipeline is running
        val record = client.get(ServicePipelineResource::class).isPipelineRunning(
            projectId = event.projectId,
            buildId = buildId,
            channelCode = ChannelCode.valueOf(event.channelCode)
        )
        if (record.isNotOk() || record.data == null) {
            LOG.warn("[$buildId]|BUILD_LESS| Fail to check if pipeline is running because of ${record.message}")
            return
        }

        if (!record.data!!) {
            LOG.warn("[$buildId]|BUILD_LESS| The build is not running")
            return
        }

        if (event.retryTime == 0) {
            buildLogPrinter.addLine(
                buildId = buildId,
                message = "Prepare BuildLess Job(#$vmSeqId)...",
                tag = "",
                jobId = event.containerHashId,
                executeCount = event.executeCount ?: 1
            )
        }

        if (buildLessWhitelistService.checkBuildLessWhitelist(event.projectId)) {
            val agentLessDockerIp = dockerHostUtils.getAvailableDockerIpWithSpecialIps(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                vmSeqId = event.vmSeqId,
                specialIpSet = emptySet(),
                unAvailableIpList = emptySet(),
                clusterName = DockerHostClusterType.BUILD_LESS
            )
            buildLessClient.startBuildLess(agentLessDockerIp.first, agentLessDockerIp.second, event)
        } else {
            val agentLessDockerIp = dockerHostUtils.getAvailableDockerIpWithSpecialIps(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                vmSeqId = event.vmSeqId,
                specialIpSet = emptySet(),
                unAvailableIpList = emptySet(),
                clusterName = DockerHostClusterType.AGENT_LESS
            )
            dockerHostClient.startAgentLessBuild(agentLessDockerIp.first, agentLessDockerIp.second, event)
        }
    }

    fun shutdown(event: PipelineBuildLessShutdownDispatchEvent) {
        try {
            LOG.info("[${event.buildId}]| Start to finish the pipeline build($event)")
            if (event.vmSeqId.isNullOrBlank()) {
                val records = pipelineDockerBuildDao
                    .listBuilds(dslContext, event.buildId)
                records.forEach {
                    finishBuild(it, event.buildResult)
                }
            } else {
                val record = pipelineDockerBuildDao
                    .getBuild(dslContext, event.buildId, event.vmSeqId!!.toInt())
                if (record != null) {
                    finishBuild(record, event.buildResult)
                }
            }
        } finally {
            buildLogPrinter.stopLog(buildId = event.buildId, tag = "", jobId = null)
        }
    }

    private fun finishBuild(
        record: TDispatchPipelineDockerBuildRecord,
        success: Boolean
    ) {
        LOG.info("Finish the docker buildless (${record.buildId}) with result($success)")
        try {
            if (record.dockerIp.isNotEmpty()) {
                if (buildLessWhitelistService.checkBuildLessWhitelist(record.projectId)) {
                    buildLessClient.endBuild(
                        projectId = record.projectId,
                        pipelineId = record.pipelineId,
                        buildId = record.buildId,
                        vmSeqId = record.vmSeqId?.toInt() ?: 0,
                        containerId = record.containerId,
                        dockerIp = record.dockerIp,
                        clusterType = DockerHostClusterType.BUILD_LESS
                    )
                } else {
                    dockerHostClient.endBuild(
                        projectId = record.projectId,
                        pipelineId = record.pipelineId,
                        buildId = record.buildId,
                        vmSeqId = record.vmSeqId?.toInt() ?: 0,
                        containerId = record.containerId,
                        dockerIp = record.dockerIp,
                        clusterType = DockerHostClusterType.AGENT_LESS
                    )
                }
            }

            pipelineDockerBuildDao.updateStatus(dslContext,
                record.buildId,
                record.vmSeqId,
                if (success) PipelineTaskStatus.DONE else PipelineTaskStatus.FAILURE)

            redisUtils.deleteHeartBeat(record.buildId, record.vmSeqId.toString())

            // 无编译环境清除redisAuth
            redisUtils.deleteDockerBuild(record.id, SecurityUtil.decrypt(record.secretKey))
        } catch (e: Exception) {
            LOG.warn("Finish the docker buildless (${record.buildId}) error.", e)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PipelineAgentLessDispatchService::class.java)
    }
}
