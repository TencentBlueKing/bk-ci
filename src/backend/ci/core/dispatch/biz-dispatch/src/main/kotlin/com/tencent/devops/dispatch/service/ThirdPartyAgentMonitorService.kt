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

package com.tencent.devops.dispatch.service

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.dispatch.dao.ThirdPartyAgentBuildDao
import com.tencent.devops.dispatch.pojo.AgentStartMonitor
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.environment.api.thirdPartyAgent.ServiceThirdPartyAgentResource
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.common.VMUtils
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

/**
 * 三方构建机的业务监控拓展
 */
@Service
class ThirdPartyAgentMonitorService @Autowired constructor(
    private val client: Client,
    private val commonConfig: CommonConfig,
    private val dslContext: DSLContext,
    private val buildLogPrinter: BuildLogPrinter,
    private val thirdPartyAgentBuildDao: ThirdPartyAgentBuildDao
) {

    @Suppress("LongMethod")
    fun monitor(event: AgentStartMonitor) {

        val record = thirdPartyAgentBuildDao.get(dslContext, event.buildId, event.vmSeqId) ?: return
        // #5806 已经不再排队，则退出监控, 暂时需求如此，后续可修改
        if (PipelineTaskStatus.toStatus(record.status) != PipelineTaskStatus.QUEUE) {
            return
        }

        val logMessage = StringBuilder()

        val agentDetail = client.get(ServiceThirdPartyAgentResource::class)
            .getAgentDetail(userId = event.userId, projectId = event.projectId, agentHashId = record.agentId)
            .data

        val tag = VMUtils.genStartVMTaskId(event.vmSeqId)

        logMessage.append(
            MessageCodeUtil.getCodeLanMessage(
                messageCode = ProcessMessageCode.BUILD_AGENT_DETAIL_LINK_ERROR,
                params = arrayOf(event.projectId, record.agentId)
            )
        )

        val heartbeatInfo = agentDetail?.heartbeatInfo

        if (agentDetail != null) {
            logMessage.append("|最大并行构建量(maximum parallelism)/当前正在运行构建数量(Running): ")
            if (agentDetail.parallelTaskCount != "0") {
                logMessage.append(agentDetail.parallelTaskCount).append("/").append(heartbeatInfo?.busyTaskSize ?: 0)
            }

            if (agentDetail.parallelTaskCount == "0") {
                logMessage.append("无限制(unlimited), 注意负载(Attention)")
            }
        }

        log(event, logMessage, tag)

        if (heartbeatInfo != null) {

            heartbeatInfo.heartbeatTime?.let { self ->
                logMessage.append("构建机最近心跳时间（heartbeat Time): ${DateTimeUtil.formatDate(Date(self))}")
            }

            logMessage.append("|最近${heartbeatInfo.taskList.size}次运行中的构建:\n")

            heartbeatInfo.taskList.forEach {
                thirdPartyAgentBuildDao.get(dslContext, it.buildId, it.vmSeqId)?.let { r1 ->
                    logMessage.append("<a href='${genBuildDetailUrl(r1.projectId, r1.pipelineId, r1.buildId)}'>")
                    logMessage.append("运行中(Running) #${r1.buildNum}</a> (${r1.pipelineName} ${r1.taskName})\n")
                }
            }

            log(event, logMessage, tag)
        }
    }

    private fun log(event: AgentStartMonitor, sb: StringBuilder, tag: String) {
        buildLogPrinter.addLine(
            buildId = event.buildId,
            message = sb.toString(),
            tag = tag,
            jobId = event.containerHashId,
            executeCount = event.executeCount ?: 1
        )
        sb.clear()
    }

    private fun genBuildDetailUrl(projectId: String, pipelineId: String, buildId: String): String {
        return HomeHostUtil.getHost(commonConfig.devopsHostGateway!!) +
            "/console/pipeline/$projectId/$pipelineId/detail/$buildId"
    }
}
