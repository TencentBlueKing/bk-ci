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

package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.job.AgentVersion
import com.tencent.devops.environment.pojo.job.agentreq.AgentCondition
import com.tencent.devops.environment.pojo.job.agentreq.AgentHostForInstallAgent
import com.tencent.devops.environment.pojo.job.agentreq.AgentInstallAgentReq
import com.tencent.devops.environment.pojo.job.agentreq.AgentQueryAgentStatusFromNodemanReq
import com.tencent.devops.environment.pojo.job.agentres.AgentQueryAgentTaskLog
import com.tencent.devops.environment.pojo.job.agentreq.AgentQueryAgentTaskStatusReq
import com.tencent.devops.environment.pojo.job.agentreq.AgentRetryAgentInstallTaskReq
import com.tencent.devops.environment.pojo.job.agentreq.AgentTerminateAgentInstallTaskReq
import com.tencent.devops.environment.pojo.job.agentreq.InstallAgentReq
import com.tencent.devops.environment.pojo.job.agentreq.QueryAgentStatusFromNodemanReq
import com.tencent.devops.environment.pojo.job.agentreq.QueryAgentTaskStatusReq
import com.tencent.devops.environment.pojo.job.agentreq.RetryAgentInstallTaskReq
import com.tencent.devops.environment.pojo.job.agentreq.TerminateAgentInstallTaskReq
import com.tencent.devops.environment.pojo.job.agentres.AgentOriginalResult
import com.tencent.devops.environment.pojo.job.agentres.AgentInstallAgentChannel
import com.tencent.devops.environment.pojo.job.agentres.AgentInstallAgentResult
import com.tencent.devops.environment.pojo.job.agentres.AgentQueryAgentStatusFromNodemanResult
import com.tencent.devops.environment.pojo.job.agentres.AgentQueryAgentTaskStatusResult
import com.tencent.devops.environment.pojo.job.agentres.AgentResult
import com.tencent.devops.environment.pojo.job.agentres.AgentRetryAgentInstallTaskResult
import com.tencent.devops.environment.pojo.job.agentres.AgentTerminalAgentInstallTaskResult
import com.tencent.devops.environment.pojo.job.agentres.ExtraData
import com.tencent.devops.environment.pojo.job.agentres.FilterHostInfo
import com.tencent.devops.environment.pojo.job.agentres.HostDetail
import com.tencent.devops.environment.pojo.job.agentres.IdentityInfo
import com.tencent.devops.environment.pojo.job.agentres.InstallAgentChannel
import com.tencent.devops.environment.pojo.job.agentres.InstallAgentResult
import com.tencent.devops.environment.pojo.job.agentres.IpFilter
import com.tencent.devops.environment.pojo.job.agentres.JobResultForFilterHostInfo
import com.tencent.devops.environment.pojo.job.agentres.Meta
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentInstallChannelResult
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentStatusFromNodemanResult
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentTaskLog
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentTaskLogResult
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentTaskStatusResult
import com.tencent.devops.environment.pojo.job.agentres.RetryAgentInstallTaskResult
import com.tencent.devops.environment.pojo.job.agentres.Statistics
import com.tencent.devops.environment.pojo.job.agentres.TerminalAgentInstallTaskResult
import org.jooq.DSLContext
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response

@Service("AgentService")
data class AgentService @Autowired constructor(
    private val nodeManApi: NodeManApi,
    private val chooseAgentInstallChannelIdService: ChooseAgentInstallChannelIdService,
    private val fileService: FileService,
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao,
    private val redisOperation: RedisOperation,
    private val queryFromCCService: QueryFromCCService,
    private val queryAgentStatusService: QueryAgentStatusService
) {
    @Value("\${environment.cc.bkBizScopeId:#{null}}")
    val bkBizScopeId: Int = 0

    companion object {
        private val logger = LoggerFactory.getLogger(AgentService::class.java)

        private val mapper = jacksonObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

        private const val DEFAULT_INSTALL_AGENT_JOB_TYPE = "REINSTALL_AGENT"
        private const val DEFAULT_INSTALL_AGENT_AP_ID = 1 // 节点管理预发布/正式环境 apId均固定为1
        private const val DEFAULT_INSTALL_AGENT_PORT = "36000"
        private const val DEFAULT_IS_MANUAL = false
        private const val DEFAULT_CLOUD_ID = 0
        private const val DEFAULT_PLACE_HOLDER = -1
        private const val DEFAULT_NOT_INSTALL_LATEST_PLUGINS = false

        private const val DEFAULT_PAGE = 1
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAXIMUM_RETRY_TIMES = 100
        private const val INITIAL_DELAY = 1000L // unit: ms
        private const val TASK_PERIOD = 3000L // unit: ms

        private const val AGENT_INSTALL_NORMAL = "SUCCESS"
        private val agentTaskEndStatusList = listOf(
            "FAILED", "SUCCESS", "PART_FAILED", "TERMINATED", "REMOVED", "FILTERED", "IGNORED"
        )
        private val agentRunningStatusList = listOf( // 两种正在执行状态："PENDING"(等待), "RUNNING"（正在执行）
            "PENDING", "RUNNING"
        )

        const val AGENT_ABNORMAL_NODE_STATUS = 0
        const val AGENT_NORMAL_NODE_STATUS = 1
        const val AGENT_NOT_INSTALLED_TAG = false

        private const val CHECK_NODE_STATUS_TIMEOUT_LOCK_KEY = "check_node_status_timeout_lock"
        private const val EXPIRATION_TIME_OF_THE_LOCK = 200L
    }

    fun installAgent(
        userId: String,
        projectId: String,
        keyFile: InputStream?,
        installAgentReqString: String
    ): AgentResult<InstallAgentResult> {
        NodeManApi.setNodemanOperationName("installAgent")
        val installAgentJson = JSONObject(installAgentReqString)
        val installAgentReq = mapper.readValue<InstallAgentReq>(installAgentJson.toString())
        val hostIdToqueryCCResDataMap = queryFromCCService.queryCCFindHostBizRelations(
            installAgentReq.hosts.mapNotNull { it.bkHostId?.toLong() }
        ).data?.associateBy { it.bkHostId }
        val installAgentRequest = AgentInstallAgentReq(
            jobType = DEFAULT_INSTALL_AGENT_JOB_TYPE,
            hosts = installAgentReq.hosts.map {
                AgentHostForInstallAgent(
                    bkBizId = hostIdToqueryCCResDataMap?.get(it.bkHostId)?.bkBizId ?: bkBizScopeId,
                    bkCloudId = it.bkCloudId ?: DEFAULT_CLOUD_ID,
                    bkHostId = it.bkHostId, bkAddressing = null,
                    apId = DEFAULT_INSTALL_AGENT_AP_ID,
                    installChannelId = if (it.isAutoChooseInstallChannelId) {
                        chooseAgentInstallChannelIdService.autoChooseAgentInstallChannelId(
                            it.innerIp ?: ""
                        )
                    } else it.installChannelId,
                    innerIp = it.innerIp, outerIp = null, loginIp = null, dataIp = null,
                    innerIpv6 = null, outerIpv6 = null,
                    osType = it.osType,
                    authType = it.authType,
                    account = it.account,
                    password = it.password,
                    port = DEFAULT_INSTALL_AGENT_PORT,
                    key = if ("KEY" == it.authType) fileService.convertFileContentToString(keyFile) else it.key,
                    isManual = DEFAULT_IS_MANUAL,
                    retention = null, peerExchangeSwitchForAgent = null, btSpeedLimit = null,
                    enableCompression = null, dataPath = null
                )
            },
            replaceHostId = installAgentReq.replaceHostId,
            isInstallLatestPlugins = DEFAULT_NOT_INSTALL_LATEST_PLUGINS
        )
        val agentInstallAgentRes: AgentOriginalResult<AgentInstallAgentResult> = nodeManApi.executePostRequest(
            installAgentRequest, AgentInstallAgentResult::class.java
        )
        val installAgentRes: AgentResult<InstallAgentResult> = AgentResult(
            code = agentInstallAgentRes.code,
            result = agentInstallAgentRes.result,
            message = agentInstallAgentRes.message,
            errors = agentInstallAgentRes.errors,
            data = InstallAgentResult(
                jobId = agentInstallAgentRes.data?.jobId,
                jobUrl = agentInstallAgentRes.data?.jobUrl,
                ipFilter = agentInstallAgentRes.data?.ipFilter?.map {
                    IpFilter(
                        bkBizId = it.bkBizId,
                        bkBizName = it.bkBizName,
                        ip = it.ip,
                        innerIp = it.innerIp,
                        innerIpv6 = it.innerIpv6,
                        bkHostId = it.bkHostId,
                        bkCloudName = it.bkCloudName,
                        bkCloudId = it.bkCloudId,
                        status = it.status,
                        jobId = it.jobId,
                        exception = it.exception,
                        msg = it.msg
                    )
                }
            )
        )
        checkAgentStatus(
            userId, projectId, installAgentRes.data?.jobId, installAgentReq.hosts.mapNotNull { it.innerIp }
        )
        return installAgentRes
    }

    /**
     * 安装agent状态轮询
     * 发起安装任务后，轮询安装状态：安装中 - NODE_STATUS: RUNNING
     * 执行定时轮询任务，每隔 3000ms 检查任务状态，如果结束（成功/失败）则停止轮询。
     */
    @Async("checkAgentStatus")
    fun checkAgentStatus(userId: String, projectId: String, jobId: Int?, ipList: List<String>?) {
        val redisLock = RedisLock(redisOperation, CHECK_NODE_STATUS_TIMEOUT_LOCK_KEY, EXPIRATION_TIME_OF_THE_LOCK)
        redisLock.takeIf { it.tryLock() }.run {
            try {
                checkAgentStatusTimed(userId, projectId, jobId, ipList)
            } finally {
                redisLock.unlock()
            }
        }
    }

    private fun checkAgentStatusTimed(userId: String, projectId: String, jobId: Int?, ipList: List<String>?) {
        if (null == jobId) {
            throw CustomException(
                Response.Status.INTERNAL_SERVER_ERROR,
                "Empty job id."
            )
        }
        if (null == ipList) return
        val executor = Executors.newSingleThreadScheduledExecutor()
        val runningIpList = ipList.toMutableList()
        nodeDao.updateNodeStatusByNodeIp(dslContext, ipList, NodeStatus.RUNNING.name, jobId.toLong())
        val task = object : Runnable {
            var count = 0
            override fun run() {
                val queryAgentTaskStatusReq = QueryAgentTaskStatusReq(
                    page = DEFAULT_PAGE, pageSize = DEFAULT_PAGE_SIZE
                )
                val queryAgentTaskStatusRes = queryAgentTaskStatus(
                    userId, projectId, jobId, queryAgentTaskStatusReq
                )
                queryAgentTaskStatusRes.data?.list?.filter {
                    it.ip in runningIpList
                }?.map {
                    if (logger.isDebugEnabled)
                        logger.debug("Agent install task: ip: ${it.ip}, status: ${it.status}")
                    if (it.status in agentTaskEndStatusList) { // agent安装结束(成功/失败)
                        val nodeStatus =
                            if (AGENT_INSTALL_NORMAL == it.status) NodeStatus.NORMAL.name
                            else { // agent安装失败，重新查询agent安装状态
                                val agentInfo = queryAgentStatusService.getAgentVersions(
                                    listOf(AgentVersion(ip = it.ip, bkHostId = it.bkHostId?.toLong()))
                                )
                                if (AGENT_NOT_INSTALLED_TAG == agentInfo?.get(0)?.installedTag)
                                    NodeStatus.NOT_INSTALLED.name
                                else if (AGENT_ABNORMAL_NODE_STATUS == agentInfo?.get(0)?.status)
                                    NodeStatus.ABNORMAL.name
                                else if (AGENT_NORMAL_NODE_STATUS == agentInfo?.get(0)?.status)
                                    NodeStatus.NORMAL.name
                                else
                                    NodeStatus.NOT_INSTALLED.name
                            }
                        nodeDao.updateNodeStatusByNodeIp(dslContext, listOf(it.ip), nodeStatus, null)
                        runningIpList.remove(it.ip)
                    }
                }
                logger.info("Agent install task runningIpList:${runningIpList.joinToString()}")
                if (runningIpList.isEmpty()) {
                    logger.info("Agent install task is complete.")
                    executor.shutdown()
                } else if (count > MAXIMUM_RETRY_TIMES) {
                    logger.info("Agent install task abnormal ip: $runningIpList")
                    nodeDao.updateNodeStatusByNodeIp(dslContext, runningIpList, NodeStatus.ABNORMAL.name, null)
                    executor.shutdown()
                } else {
                    if (logger.isDebugEnabled) logger.debug("Agent install task running...")
                    count++
                }
            }
        }
        executor.scheduleAtFixedRate(task, INITIAL_DELAY, TASK_PERIOD, TimeUnit.MILLISECONDS)
    }

    fun queryAgentTaskStatus(
        userId: String,
        projectId: String,
        jobId: Int,
        queryAgentTaskStatusReq: QueryAgentTaskStatusReq
    ): AgentResult<QueryAgentTaskStatusResult> {
        NodeManApi.setNodemanOperationName("queryAgentTaskStatus")
        val queryAgentTaskStatusRequest = AgentQueryAgentTaskStatusReq(
            page = queryAgentTaskStatusReq.page,
            pageSize = queryAgentTaskStatusReq.pageSize
        )
        val agentQueryAgentTaskStatusRes: AgentOriginalResult<AgentQueryAgentTaskStatusResult> =
            nodeManApi.executePostRequest(
                queryAgentTaskStatusRequest, AgentQueryAgentTaskStatusResult::class.java, jobId
            )
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
                            statusDisplay = hostDetail.statusDisplay
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
        val ipToNodeStatus = mutableMapOf<String, String>()
        val hostInfoToStatusMap = agentQueryAgentTaskStatusRes.data?.list?.associate {
            val status = when (it.status) {
                AGENT_INSTALL_NORMAL -> NodeStatus.NORMAL.name
                in agentRunningStatusList -> NodeStatus.RUNNING.name
                else -> NodeStatus.ABNORMAL.name
            }
            Pair(it.ip, it.bkHostId) to status
        }?.filter { (key, value) -> value != NodeStatus.RUNNING.name } // RUNNING的节点不更新
        hostInfoToStatusMap?.map { (key, value) -> ipToNodeStatus[key.first] = value }
        // 对于安装失败的节点，再查agent安装状态
        val ipToAgentVersionInfoMap = hostInfoToStatusMap?.filter {
            NodeStatus.ABNORMAL.name == it.value
        }?.let { hostInfoToStatus ->
            queryAgentStatusService.getAgentVersions(
                hostInfoToStatus.map {
                    AgentVersion(ip = it.key.first, bkHostId = it.key.second?.toLong())
                }
            )?.associateBy { it.ip }
        }
        val queryAgentIpList = ipToAgentVersionInfoMap?.keys?.filterNotNull()
        queryAgentIpList?.map {
            ipToNodeStatus[it] =
                if (AGENT_NOT_INSTALLED_TAG == ipToAgentVersionInfoMap[it]?.installedTag)
                    NodeStatus.NOT_INSTALLED.name
                else if (AGENT_ABNORMAL_NODE_STATUS == ipToAgentVersionInfoMap[it]?.status)
                    NodeStatus.ABNORMAL.name
                else if (AGENT_NORMAL_NODE_STATUS == ipToAgentVersionInfoMap[it]?.status)
                    NodeStatus.NORMAL.name
                else
                    NodeStatus.NOT_INSTALLED.name
        }
        nodeDao.updateNodeInCCByIp(dslContext, ipToNodeStatus)
        return queryAgentTaskStatusRes
    }

    fun queryAgentStatusFromNodeman(
        queryAgentStatusFromNodemanReq: QueryAgentStatusFromNodemanReq
    ): AgentResult<QueryAgentStatusFromNodemanResult> {
        NodeManApi.setNodemanOperationName("queryAgentStatusFromNodeman")
        val queryAgentStatusFromNodemanRequest = AgentQueryAgentStatusFromNodemanReq(
            bkHostId = queryAgentStatusFromNodemanReq.bkHostId,
            conditions = queryAgentStatusFromNodemanReq.conditions?.map {
                AgentCondition(key = it.key, value = it.value)
            },
            extraData = queryAgentStatusFromNodemanReq.extraData,
            pagesize = queryAgentStatusFromNodemanReq.pagesize,
            page = queryAgentStatusFromNodemanReq.page,
            onlyIp = queryAgentStatusFromNodemanReq.onlyIp,
            runningCount = queryAgentStatusFromNodemanReq.runningCount
        )
        val agentQueryAgentStatusRes: AgentOriginalResult<AgentQueryAgentStatusFromNodemanResult> =
            nodeManApi.executePostRequest(
                queryAgentStatusFromNodemanRequest, AgentQueryAgentStatusFromNodemanResult::class.java
            )
        val queryAgentStatusRes: AgentResult<QueryAgentStatusFromNodemanResult> = AgentResult(
            code = agentQueryAgentStatusRes.code,
            result = agentQueryAgentStatusRes.result,
            message = agentQueryAgentStatusRes.message,
            errors = agentQueryAgentStatusRes.errors,
            data = agentQueryAgentStatusRes.data?.let {
                QueryAgentStatusFromNodemanResult(
                    total = it.total,
                    list = it.list?.map { filterHostInfo ->
                        FilterHostInfo(
                            bkCloudId = filterHostInfo.bkCloudId,
                            bkBizId = filterHostInfo.bkBizId,
                            bkHostId = filterHostInfo.bkHostId,
                            bkHostName = filterHostInfo.bkHostName,
                            bkAddressing = filterHostInfo.bkAddressing,
                            bkAgentId = filterHostInfo.bkAgentId,
                            osType = filterHostInfo.osType,
                            innerIp = filterHostInfo.innerIp,
                            innerIpv6 = filterHostInfo.innerIpv6,
                            outerIp = filterHostInfo.outerIp,
                            outerIpv6 = filterHostInfo.outerIpv6,
                            apId = filterHostInfo.apId,
                            installChannelId = filterHostInfo.installChannelId,
                            loginIp = filterHostInfo.loginIp,
                            dataIp = filterHostInfo.dataIp,
                            status = filterHostInfo.status,
                            version = filterHostInfo.version,
                            createdAt = filterHostInfo.createdAt,
                            updatedAt = filterHostInfo.updatedAt,
                            isManual = filterHostInfo.isManual,
                            extraData = filterHostInfo.extraData?.let { extraData ->
                                ExtraData(
                                    btSpeedLimit = extraData.btSpeedLimit,
                                    enableCompression = extraData.enableCompression,
                                    peerExchangeSwitchForAgent = extraData.peerExchangeSwitchForAgent
                                )
                            },
                            statusDisplay = filterHostInfo.statusDisplay,
                            bkCloudName = filterHostInfo.bkCloudName,
                            installChannelName = filterHostInfo.installChannelName,
                            bkBizName = filterHostInfo.bkBizName,
                            identityInfo = filterHostInfo.identityInfo?.let { identityInfo ->
                                IdentityInfo(
                                    account = identityInfo.account,
                                    authType = identityInfo.authType,
                                    port = identityInfo.port,
                                    reCertification = identityInfo.reCertification
                                )
                            },
                            jobResult = filterHostInfo.jobResult?.let { jobResultForFilterHostInfo ->
                                JobResultForFilterHostInfo(
                                    instanceId = jobResultForFilterHostInfo.instanceId,
                                    jobId = jobResultForFilterHostInfo.jobId,
                                    status = jobResultForFilterHostInfo.status,
                                    currentStep = jobResultForFilterHostInfo.currentStep
                                )
                            },
                            topology = filterHostInfo.topology,
                            operatePermission = filterHostInfo.operatePermission
                        )
                    },
                    runningCount = it.runningCount,
                    noPermissionCount = it.noPermissionCount
                )
            }
        )
        return queryAgentStatusRes
    }

    fun queryAgentTaskLog(
        userId: String,
        projectId: String,
        jobId: Int,
        instanceId: String
    ): AgentResult<QueryAgentTaskLogResult> {
        NodeManApi.setNodemanOperationName("queryAgentTaskLog")
        val agentQueryAgentTaskLogRes: AgentOriginalResult<Array<AgentQueryAgentTaskLog>> =
            nodeManApi.executeGetRequest(
                Array<AgentQueryAgentTaskLog>::class.java, jobId, instanceId
            )
        val queryAgentTaskLogRes: AgentResult<QueryAgentTaskLogResult> = AgentResult(
            code = agentQueryAgentTaskLogRes.code,
            result = agentQueryAgentTaskLogRes.result,
            message = agentQueryAgentTaskLogRes.message,
            errors = agentQueryAgentTaskLogRes.errors,
            data = agentQueryAgentTaskLogRes.data?.let {
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

    fun terminalAgentInstallTask(
        userId: String,
        projectId: String,
        jobId: Int,
        terminateAgentInstallTaskReq: TerminateAgentInstallTaskReq
    ): AgentResult<TerminalAgentInstallTaskResult> {
        NodeManApi.setNodemanOperationName("terminalAgentInstallTask")
        val terminalAgentInstallTaskRequest = AgentTerminateAgentInstallTaskReq(
            instanceIdList = terminateAgentInstallTaskReq.instanceIdList
        )
        val agentTrmAgentInstallTaskRes: AgentOriginalResult<AgentTerminalAgentInstallTaskResult> =
            nodeManApi.executePostRequest(
                terminalAgentInstallTaskRequest, AgentTerminalAgentInstallTaskResult::class.java, jobId
            )
        val termAgentInstallTaskRes: AgentResult<TerminalAgentInstallTaskResult> = AgentResult(
            code = agentTrmAgentInstallTaskRes.code,
            result = agentTrmAgentInstallTaskRes.result,
            message = agentTrmAgentInstallTaskRes.message,
            errors = agentTrmAgentInstallTaskRes.errors,
            data = agentTrmAgentInstallTaskRes.data?.let {
                TerminalAgentInstallTaskResult(
                    taskIdList = it.taskIdList
                )
            }
        )
        return termAgentInstallTaskRes
    }

    fun retryAgentInstallTask(
        userId: String,
        projectId: String,
        jobId: Int,
        retryAgentInstallTaskReq: RetryAgentInstallTaskReq
    ): AgentResult<RetryAgentInstallTaskResult> {
        NodeManApi.setNodemanOperationName("retryAgentInstallTask")
        val retryAgentInstallTaskRequest = AgentRetryAgentInstallTaskReq(
            instanceIdList = retryAgentInstallTaskReq.instanceIdList
        )
        val agentRetryAgentInstallTaskRes: AgentOriginalResult<AgentRetryAgentInstallTaskResult> =
            nodeManApi.executePostRequest(
                retryAgentInstallTaskRequest, AgentRetryAgentInstallTaskResult::class.java, jobId
            )
        val retryAgentInstallTaskRes: AgentResult<RetryAgentInstallTaskResult> = AgentResult(
            code = agentRetryAgentInstallTaskRes.code,
            result = agentRetryAgentInstallTaskRes.result,
            message = agentRetryAgentInstallTaskRes.message,
            errors = agentRetryAgentInstallTaskRes.errors,
            data = agentRetryAgentInstallTaskRes.data?.let {
                RetryAgentInstallTaskResult(
                    taskIdList = it.taskIdList
                )
            }
        )
        return retryAgentInstallTaskRes
    }

    fun queryAgentInstallChannel(
        userId: String,
        projectId: String,
        withHidden: Boolean
    ): AgentResult<QueryAgentInstallChannelResult> {
        NodeManApi.setNodemanOperationName("queryAgentInstallChannel")
        val agentQueryAgentInsChannelRes: AgentOriginalResult<Array<AgentInstallAgentChannel>> =
            nodeManApi.executeGetRequest(
                Array<AgentInstallAgentChannel>::class.java, DEFAULT_PLACE_HOLDER, withHidden
            )
        val queryAgentInsChannelRes: AgentResult<QueryAgentInstallChannelResult> = AgentResult(
            code = agentQueryAgentInsChannelRes.code,
            result = agentQueryAgentInsChannelRes.result,
            message = agentQueryAgentInsChannelRes.message,
            errors = agentQueryAgentInsChannelRes.errors,
            data = agentQueryAgentInsChannelRes.data?.let {
                QueryAgentInstallChannelResult(
                    installChannelList = it.map { installChannel ->
                        InstallAgentChannel(
                            id = installChannel.id,
                            name = installChannel.name,
                            bkCloudId = installChannel.bkCloudId,
                            hidden = installChannel.hidden
                        )
                    }
                )
            }
        )
        return queryAgentInsChannelRes
    }
}