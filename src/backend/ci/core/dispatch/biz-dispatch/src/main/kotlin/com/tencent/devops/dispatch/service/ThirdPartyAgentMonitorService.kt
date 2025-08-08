/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.devops.common.api.constant.BUILD_RUNNING
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.constants.BK_BUILD_AGENT_DETAIL_LINK_ERROR
import com.tencent.devops.dispatch.constants.BK_DOCKER_BUILD_VOLUME
import com.tencent.devops.dispatch.constants.BK_DOCKER_WAS_RECENTLY_BUILT
import com.tencent.devops.dispatch.constants.BK_HEARTBEAT_TIME
import com.tencent.devops.dispatch.constants.BK_MAXIMUM_PARALLELISM
import com.tencent.devops.dispatch.constants.BK_TASK_FETCHING_TIMEOUT
import com.tencent.devops.dispatch.constants.BK_UNLIMITED
import com.tencent.devops.dispatch.constants.BK_WAS_RECENTLY_BUILT
import com.tencent.devops.dispatch.dao.ThirdPartyAgentBuildDao
import com.tencent.devops.dispatch.pojo.AgentStartMonitor
import com.tencent.devops.dispatch.pojo.TPAMonitorEvent
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import com.tencent.devops.model.dispatch.tables.records.TDispatchThirdpartyAgentBuildRecord
import com.tencent.devops.process.engine.common.VMUtils
import java.util.Date
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 三方构建机的业务监控拓展
 */
@Suppress("ALL")
@Service
class ThirdPartyAgentMonitorService @Autowired constructor(
    private val streamBridge: StreamBridge,
    private val client: Client,
    private val commonConfig: CommonConfig,
    private val dslContext: DSLContext,
    private val buildLogPrinter: BuildLogPrinter,
    private val thirdPartyAgentBuildDao: ThirdPartyAgentBuildDao
) {
    fun monitorQueue(event: TPAMonitorEvent) {
        try {
            if (monitor(event)) {
                event.sendTo(streamBridge)
            } else {
                logger.info("monitorQueue|${event.toLog()}|exit")
            }
        } catch (e: Exception) {
            logger.error("monitorQueue|${event.toLog()}|error", e)
        }
    }

    fun monitor(event: TPAMonitorEvent): Boolean {
        val record = thirdPartyAgentBuildDao.get(dslContext, event.buildId, event.vmSeqId) ?: return false
        if (record.executeCount != event.executeCount) {
            logger.warn("monitor|${event.toLog()}|executeCount not equal ${record.executeCount}")
            return false
        }

        // #5806 已经不再排队，则退出监控
        if (PipelineTaskStatus.toStatus(record.status) != PipelineTaskStatus.QUEUE) {
            return false
        }

        // 如果是第一次取创建时间，如果是重试取更新时间，作为超时依据
        val recordTime = if (record.executeCount == 1 || record.executeCount == null) {
            record.createdTime
        } else {
            record.updatedTime
        }
        if (recordTime.plusMinutes(event.timeoutMin) <= LocalDateTime.now()) {
            logger.info("monitor|${event.toLog()}|timeout")
            return false
        }

        val agentDetail = client.get(ServiceThirdPartyAgentResource::class)
            .getAgentDetail(userId = event.userId, projectId = event.projectId, agentHashId = record.agentId)
            .data ?: return false

        val tag = VMUtils.genStartVMTaskId(event.vmSeqId)
        val heartbeatInfo = agentDetail.heartbeatInfo

        val logMessage = StringBuilder(128)

        logMessage.append(
            I18nUtil.getCodeLanMessage(
                messageCode = BK_BUILD_AGENT_DETAIL_LINK_ERROR,
                params = arrayOf(event.projectId, agentDetail.nodeId),
                language = I18nUtil.getDefaultLocaleLanguage()
            )
        )

        // #7748 agent使用docker作为构建机
        var parallelTaskCount = agentDetail.parallelTaskCount
        var busyTaskSize = heartbeatInfo?.busyTaskSize
        if (record.dockerInfo != null) {
            parallelTaskCount = agentDetail.dockerParallelTaskCount
            busyTaskSize = heartbeatInfo?.dockerBusyTaskSize
        }

        if (record.dockerInfo != null) {
            logMessage.append(
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_DOCKER_BUILD_VOLUME,
                    language = I18nUtil.getDefaultLocaleLanguage()
                )
            )
        } else {
            logMessage.append(
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_MAXIMUM_PARALLELISM,
                    language = I18nUtil.getDefaultLocaleLanguage()
                )
            )
        }
        if (parallelTaskCount != "0") {
            logMessage.append(parallelTaskCount).append("/")
                .append(busyTaskSize ?: 0)
        }

        if (parallelTaskCount == "0") {
            logMessage.append(
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_UNLIMITED,
                    language = I18nUtil.getDefaultLocaleLanguage()
                )
            )
        }
        log(event, logMessage, tag)

        if (heartbeatInfo == null) {
            return true
        }

        heartbeatInfo.heartbeatTime?.let { self ->
            logMessage.append(
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_HEARTBEAT_TIME,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ) + " ${DateTimeUtil.formatDate(Date(self))}"
            )
        }

        if (record.dockerInfo != null) {
            logMessage.append(
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_DOCKER_WAS_RECENTLY_BUILT,
                    language = I18nUtil.getDefaultLocaleLanguage(),
                    params = arrayOf("${heartbeatInfo.dockerTaskList?.size ?: 0}")
                )
            )
        } else {
            logMessage.append(
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_WAS_RECENTLY_BUILT,
                    language = I18nUtil.getDefaultLocaleLanguage(),
                    params = arrayOf("${heartbeatInfo.taskList?.size ?: 0}")
                )
            )
        }

        if (record.dockerInfo != null) {
            heartbeatInfo.dockerTaskList?.forEach dockerInfoFor@{
                thirdPartyAgentBuildDao.get(dslContext, it.buildId, it.vmSeqId)?.let { r1 ->
                    if (r1.dockerInfo == null) {
                        return@dockerInfoFor
                    }
                    logMessage.append("<a href='${genBuildDetailUrl(r1.projectId, r1.pipelineId, r1.buildId)}'>")
                    logMessage.append(
                        I18nUtil.getCodeLanMessage(
                            messageCode = BUILD_RUNNING,
                            language = I18nUtil.getDefaultLocaleLanguage()
                        ) + " #${r1.buildNum}</a> (${r1.pipelineName} ${r1.taskName})\n"
                    )
                }
            }
        } else {
            heartbeatInfo.taskList?.forEach taskInfoFor@{
                thirdPartyAgentBuildDao.get(dslContext, it.buildId, it.vmSeqId)?.let { r1 ->
                    if (r1.dockerInfo != null) {
                        return@taskInfoFor
                    }
                    logMessage.append("<a href='${genBuildDetailUrl(r1.projectId, r1.pipelineId, r1.buildId)}'>")
                    logMessage.append(
                        I18nUtil.getCodeLanMessage(
                            messageCode = BUILD_RUNNING,
                            language = I18nUtil.getDefaultLocaleLanguage()
                        ) + " #${r1.buildNum}</a> (${r1.pipelineName} ${r1.taskName})\n"
                    )
                }
            }
        }

        log(event, logMessage, tag)

        return true
    }

    fun tryRollBackQueueMonitor(event: AgentStartMonitor) {
        val record = thirdPartyAgentBuildDao.get(dslContext, event.buildId, event.vmSeqId) ?: return
        if (record.executeCount != event.executeCount) {
            logger.warn("tryRollBackQueueMonitor|$event|executeCount not equal ${record.executeCount}")
            return
        }
        tryRollBackQueue(event, record)
    }

    /**
     * 3分钟如果Agent端发起了构建任务领取后还没启动，尝试回退到队列让其以便能重新领取到。(docker 10min)
     * 用于解决Agent端几类极端问题场景：
     *  1、网络问题导致Agent侧领取中断，数据包丢失，没有收到领取任务，但任务已经被改成RUNNING，需要回退。
     *  2、Agent领取后未处理构建前，进程意外退出
     * 未解决的场景：
     *  本次不涉及构建机集群重新漂移指定其他构建机，需要重新设计。
     */
    fun tryRollBackQueue(event: AgentStartMonitor, record: TDispatchThirdpartyAgentBuildRecord) {
        if (PipelineTaskStatus.toStatus(record.status) != PipelineTaskStatus.RUNNING) {
            return
        }

        record.updatedTime?.let { self ->
            val outTime = if (record.dockerInfo != null) {
                System.currentTimeMillis() - self.timestampmilli() > TimeUnit.MINUTES.toMillis(DOCKER_ROLLBACK_MIN)
            } else {
                System.currentTimeMillis() - self.timestampmilli() > TimeUnit.MINUTES.toMillis(ROLLBACK_MIN)
            }
            // Agent发起领取超过x分钟没有启动，基本上存在问题需要重回队列以便被再次调度到
            if (outTime) {
                thirdPartyAgentBuildDao.updateStatus(dslContext, record.id, PipelineTaskStatus.QUEUE)
                val sb = StringBuilder()
                sb.append(
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_TASK_FETCHING_TIMEOUT,
                        language = I18nUtil.getDefaultLocaleLanguage(),
                        params = arrayOf("$ROLLBACK_MIN")
                    )
                ).append("(Over $ROLLBACK_MIN minutes, try roll back to queue.)")
                log(event, sb, VMUtils.genStartVMTaskId(event.vmSeqId))
            }
        }
    }

    private fun log(event: TPAMonitorEvent, sb: StringBuilder, tag: String) {
        buildLogPrinter.addLine(
            buildId = event.buildId,
            message = sb.toString(),
            tag = tag,
            containerHashId = event.containerHashId,
            executeCount = event.executeCount ?: 1,
            jobId = null,
            stepId = event.stepId
        )
        sb.clear()
    }

    private fun log(event: AgentStartMonitor, sb: StringBuilder, tag: String) {
        buildLogPrinter.addLine(
            buildId = event.buildId,
            message = sb.toString(),
            tag = tag,
            containerHashId = event.containerHashId,
            executeCount = event.executeCount ?: 1,
            jobId = null,
            stepId = event.stepId
        )
        sb.clear()
    }

    private fun genBuildDetailUrl(projectId: String, pipelineId: String, buildId: String): String {
        return HomeHostUtil.getHost(commonConfig.devopsHostGateway!!) +
                "/console/pipeline/$projectId/$pipelineId/detail/$buildId"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentMonitorService::class.java)
        private const val ROLLBACK_MIN = 3L // 3分钟如果构建任务领取后没启动，尝试回退状态
        private const val DOCKER_ROLLBACK_MIN = 10L // 针对docker构建场景增加拉镜像可能需要的时间
    }
}
