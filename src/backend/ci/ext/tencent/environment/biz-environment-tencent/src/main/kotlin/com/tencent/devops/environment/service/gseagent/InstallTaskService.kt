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

package com.tencent.devops.environment.service.gseagent

import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.environment.config.EnvironmentProperties
import com.tencent.devops.environment.dao.job.CmdbNodeDao
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.job.AgentVersion
import com.tencent.devops.environment.pojo.job.agentreq.QueryAgentInstallTaskStatusReq
import com.tencent.devops.environment.pojo.job.agentreq.QueryAgentTaskStatusReq
import com.tencent.devops.environment.pojo.job.agentres.AgentInstallTaskLog
import com.tencent.devops.environment.pojo.job.agentres.AgentOriginalResult
import com.tencent.devops.environment.pojo.job.agentres.AgentQueryAgentTaskStatusResult
import com.tencent.devops.environment.pojo.job.agentres.AgentResult
import com.tencent.devops.environment.pojo.job.agentres.HostDetail
import com.tencent.devops.environment.pojo.job.agentres.Meta
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentTaskLog
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentTaskLogResult
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentTaskStatusResult
import com.tencent.devops.environment.pojo.job.agentres.Statistics
import com.tencent.devops.environment.service.gseagent.utils.NodeStatusUtils
import com.tencent.devops.environment.service.job.NodeManApi
import com.tencent.devops.environment.service.job.QueryAgentStatusService
import com.tencent.devops.environment.service.prometheus.AgentStatusUpdateThreadMetrics
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

/**
 * GSE Agent安装任务相关服务：查询状态、查询任务日志等
 */
@Service("InstallTaskService")
data class InstallTaskService @Autowired constructor(
    private val nodeManApi: NodeManApi,
    private val dslContext: DSLContext,
    private val cmdbNodeDao: CmdbNodeDao,
    private val queryAgentStatusService: QueryAgentStatusService,
    private val agentStatusUpdateThreadMetrics: AgentStatusUpdateThreadMetrics,
    private val environmentProperties: EnvironmentProperties
) {

    companion object {
        private val logger = LoggerFactory.getLogger(InstallTaskService::class.java)

        private const val DEFAULT_PAGE = 1
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAXIMUM_RETRY_TIMES = 100

        private const val AGENT_INSTALL_NORMAL = "SUCCESS"
        private val agentTaskEndStatusList = listOf(
            "FAILED", "SUCCESS", "PART_FAILED", "TERMINATED", "REMOVED", "FILTERED", "IGNORED"
        )
        private val agentRunningStatusList = listOf(
            "PENDING", "RUNNING"
        )

        private const val GAUGE_NAME_ACTIVE_THREAD_COUNT = "activeThreadCount" // 活跃线程数
        private const val GAUGE_NAME_CORE_THREAD_COUNT = "coreThreadCount" // 核心线程数
        private const val GAUGE_NAME_MAX_THREAD_COUNT = "maxThreadCount" // 最大线程数
    }

    lateinit var checkAgentInstallStatusExecutor: ThreadPoolExecutor

    @PostConstruct
    fun init() {
        checkAgentInstallStatusExecutor = ThreadPoolExecutor(
            environmentProperties.checkAgentStatus.corePoolSize,
            environmentProperties.checkAgentStatus.maximumPoolSize,
            environmentProperties.checkAgentStatus.keepAliveTime,
            TimeUnit.MILLISECONDS,
            SynchronousQueue()
        )
    }

    @Scheduled(cron = "0/15 * * * * ?")
    fun monitorCheckAgentStatusExecutorTask() {
        agentStatusUpdateThreadMetrics.map[GAUGE_NAME_ACTIVE_THREAD_COUNT] =
            checkAgentInstallStatusExecutor.activeCount.toDouble()
        agentStatusUpdateThreadMetrics.map[GAUGE_NAME_CORE_THREAD_COUNT] =
            checkAgentInstallStatusExecutor.corePoolSize.toDouble()
        agentStatusUpdateThreadMetrics.map[GAUGE_NAME_MAX_THREAD_COUNT] =
            checkAgentInstallStatusExecutor.maximumPoolSize.toDouble()
    }

    /**
     * 启动安装Agent任务状态轮询后台任务
     * 发起安装任务后，轮询安装状态：安装中 - NODE_STATUS: RUNNING
     * 执行定时轮询任务，每隔 3000ms 检查任务状态，如果结束（成功/失败）则停止轮询。
     */
    fun startCheckInstallStatusTask(jobId: Int, ipList: List<String>?) {
        if (null == ipList) {
            return
        }
        val runningIpList = ipList.toMutableList()
        cmdbNodeDao.updateNodeStatusByNodeIp(dslContext, ipList, NodeStatus.RUNNING.name, null, jobId.toLong())
        val task = object : Runnable {
            var count = 0
            override fun run() {
                while (count < MAXIMUM_RETRY_TIMES) {
                    val queryAgentTaskStatusReq = QueryAgentTaskStatusReq(
                        page = DEFAULT_PAGE, pageSize = DEFAULT_PAGE_SIZE
                    )
                    val queryAgentTaskStatusRes = queryAgentInstallTaskStatus(
                        jobId, queryAgentTaskStatusReq
                    )
                    queryAgentTaskStatusRes.data?.list?.filter {
                        it.ip in runningIpList
                    }?.map {
                        if (logger.isDebugEnabled) {
                            logger.debug("Agent install task: ip: ${it.ip}, status: ${it.status}")
                        }
                        // agent安装任务结束(成功/失败)
                        if (it.status in agentTaskEndStatusList) {
                            val agentInfo = queryAgentStatusService.getAgentVersions(
                                listOf(AgentVersion(ip = it.ip, bkHostId = it.bkHostId?.toLong()))
                            )
                            val nodeStatus =
                                if (AGENT_INSTALL_NORMAL == it.status) {
                                    NodeStatus.NORMAL.name
                                } else {
                                    // agent安装任务失败，重新查询节点agent安装状态
                                    NodeStatusUtils.getNodeStatus(agentInfo?.get(0))
                                }
                            val nodeAgentVersion = agentInfo?.get(0)?.version
                            cmdbNodeDao.updateNodeStatusByNodeIp(
                                dslContext, listOf(it.ip), nodeStatus, nodeAgentVersion, null
                            )
                            runningIpList.remove(it.ip)
                        }
                    }
                    logger.info("Agent install task runningIpList:${runningIpList.joinToString()}")
                    if (runningIpList.isEmpty()) {
                        logger.info("Agent install task is complete.")
                        break
                    } else {
                        logger.debug("Agent install task running...")
                        count++
                    }
                    Thread.sleep(3000L)
                }
                if (count >= MAXIMUM_RETRY_TIMES) {
                    logger.info("Agent install task abnormal ip: $runningIpList")
                    cmdbNodeDao.updateNodeStatusByNodeIp(
                        dslContext, runningIpList, NodeStatus.ABNORMAL.name, null, null
                    )
                }
            }
        }
        try {
            checkAgentInstallStatusExecutor.submit(task)
        } catch (e: Exception) {
            logger.warn("Check agent status failed. Exception: $e")
        }
    }

    /**
     * 查询agent安装任务状态
     */
    fun queryAgentInstallTaskStatus(
        jobId: Int,
        queryAgentTaskStatusReq: QueryAgentTaskStatusReq
    ): AgentResult<QueryAgentTaskStatusResult> {
        NodeManApi.setNodemanOperationName(::queryAgentInstallTaskStatus.name)
        val queryAgentInstallTaskStatusReq = QueryAgentInstallTaskStatusReq(
            page = queryAgentTaskStatusReq.page,
            pageSize = queryAgentTaskStatusReq.pageSize
        )
        val agentQueryAgentTaskStatusRes: AgentOriginalResult<AgentQueryAgentTaskStatusResult> =
            nodeManApi.queryAgentInstallTaskStatus(jobId, queryAgentInstallTaskStatusReq)
        val queryAgentTaskStatusRes: AgentResult<QueryAgentTaskStatusResult> = AgentResult(
            code = agentQueryAgentTaskStatusRes.code,
            result = agentQueryAgentTaskStatusRes.result,
            message = agentQueryAgentTaskStatusRes.message,
            errors = agentQueryAgentTaskStatusRes.errors,
            data = agentQueryAgentTaskStatusRes.data?.let {
                QueryAgentTaskStatusResult(
                    jobId = it.jobId,
                    createdBy = it.createdBy,
                    jobType = it.jobType,
                    jobTypeDisplay = it.jobTypeDisplay,
                    ipFilterList = it.ipFilterList,
                    total = it.total,
                    list = it.list?.map { hostDetail ->
                        HostDetail(
                            filterHost = hostDetail.filterHost,
                            bkHostId = hostDetail.bkHostId,
                            ip = hostDetail.ip,
                            innerIp = hostDetail.innerIp,
                            innerIpv6 = hostDetail.innerIpv6,
                            instanceId = hostDetail.instanceId,
                            bkCloudId = hostDetail.bkCloudId,
                            bkCloudName = hostDetail.bkCloudName,
                            bkBizId = hostDetail.bkBizId,
                            bkBizName = hostDetail.bkBizName,
                            jobId = hostDetail.jobId,
                            status = hostDetail.status,
                            statusDisplay = hostDetail.statusDisplay,
                            isManual = hostDetail.isManual
                        )
                    },
                    statistics = it.statistics.let { statistics ->
                        Statistics(
                            totalCount = statistics.totalCount,
                            failedCount = statistics.failedCount,
                            ignoredCount = statistics.ignoredCount,
                            pendingCount = statistics.pendingCount,
                            runningCount = statistics.runningCount,
                            successCount = statistics.successCount
                        )
                    },
                    status = it.status,
                    endTime = it.endTime,
                    startTime = it.startTime,
                    costTime = it.costTime,
                    meta = it.meta.let { meta ->
                        Meta(
                            type = meta.type,
                            stepType = meta.stepType,
                            opType = meta.opType,
                            opTypeDisplay = meta.opTypeDisplay,
                            stepTypeDisplay = meta.stepTypeDisplay
                        )
                    }
                )
            }
        )
        // 若agent安装任务结束(成功/失败)，同步更新db中节点安装的状态
        val hostIdToNodeStatus = mutableMapOf<Long, String>()
        val hostInfoToStatusMap = agentQueryAgentTaskStatusRes.data?.list?.associate {
            val status = when (it.status) {
                AGENT_INSTALL_NORMAL -> NodeStatus.NORMAL.name
                in agentRunningStatusList -> NodeStatus.RUNNING.name
                else -> NodeStatus.ABNORMAL.name
            }
            Pair(it.ip, it.bkHostId) to status
        }?.filter { it.value != NodeStatus.RUNNING.name } // RUNNING的节点不更新
        hostInfoToStatusMap?.map { (key, value) ->
            hostIdToNodeStatus[key.second.toLong()] = value
        }
        // 对于安装失败的节点，再查agent安装状态
        val hostIdToAgentVersionInfoMap = hostInfoToStatusMap?.filter {
            NodeStatus.ABNORMAL.name == it.value
        }?.let { hostInfoToStatus ->
            queryAgentStatusService.getAgentVersions(
                hostInfoToStatus.map {
                    AgentVersion(ip = it.key.first, bkHostId = it.key.second.toLong())
                }
            )?.associateBy { it.bkHostId }
        }
        val queryAgentHostIdList = hostIdToAgentVersionInfoMap?.keys?.filterNotNull()
        queryAgentHostIdList?.map {
            hostIdToNodeStatus[it] = NodeStatusUtils.getNodeStatus(hostIdToAgentVersionInfoMap[it])
        }
        cmdbNodeDao.batchUpdateNodeInCCByHostId(dslContext, hostIdToNodeStatus)
        return queryAgentTaskStatusRes
    }

    /**
     * 查询agent安装任务日志
     */
    fun queryAgentInstallTaskLog(
        jobId: Int,
        instanceId: String
    ): AgentResult<QueryAgentTaskLogResult> {
        NodeManApi.setNodemanOperationName(::queryAgentInstallTaskLog.name)
        val agentInstallTaskLogRes: AgentOriginalResult<Array<AgentInstallTaskLog>> = try {
            nodeManApi.queryAgentInstallTaskLog(
                jobId = jobId,
                instanceId = instanceId
            )
        } catch (e: RemoteServiceException) {
            // 最初未获取到日志，节点管理抛出的"订阅任务未准备好"异常，该情况可重试，后台不抛出异常
            if (GSEAgentService.NODEMAN_LOG_NOT_READY_CODE == e.errorCode) {
                AgentOriginalResult(
                    code = GSEAgentService.NODEMAN_LOG_NOT_READY_CODE,
                    result = false,
                    message = "Nodeman log is not ready.",
                    errors = null,
                    data = null
                )
            } else {
                throw e
            }
        }
        val queryAgentTaskLogRes: AgentResult<QueryAgentTaskLogResult> = AgentResult(
            code = agentInstallTaskLogRes.code,
            result = agentInstallTaskLogRes.result,
            message = agentInstallTaskLogRes.message,
            errors = agentInstallTaskLogRes.errors,
            data = agentInstallTaskLogRes.data?.let {
                QueryAgentTaskLogResult(
                    queryAgentTaskLogResult = it.map { queryAgentTaskLog ->
                        QueryAgentTaskLog(
                            step = queryAgentTaskLog.step,
                            status = queryAgentTaskLog.status,
                            log = queryAgentTaskLog.log,
                            startTime = queryAgentTaskLog.startTime,
                            finishTime = queryAgentTaskLog.finishTime
                        )
                    }
                )
            }
        )
        return queryAgentTaskLogRes
    }
}
