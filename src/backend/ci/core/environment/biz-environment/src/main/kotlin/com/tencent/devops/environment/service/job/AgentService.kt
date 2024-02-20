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
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream

@Service("AgentService")
data class AgentService @Autowired constructor(
    private val nodeManApi: NodeManApi,
    private val chooseAgentInstallChannelIdService: ChooseAgentInstallChannelIdService,
    private val checkAgentStatusAsyncService: CheckAgentStatusAsyncService,
    private val fileService: FileService
) {
    @Value("\${environment.cc.bkBizScopeId:#{null}}")
    val bkBizScopeId: Int = 0

    companion object {
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
        val installAgentRequest = AgentInstallAgentReq(
            jobType = DEFAULT_INSTALL_AGENT_JOB_TYPE,
            hosts = installAgentReq.hosts.map {
                AgentHostForInstallAgent(
                    bkBizId = bkBizScopeId,
                    bkCloudId = it.bkCloudId ?: DEFAULT_CLOUD_ID,
                    bkHostId = null, bkAddressing = null,
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
        checkAgentStatusAsyncService.checkAgentStatus(
            userId, projectId, installAgentRes.data?.jobId, installAgentReq.hosts.mapNotNull { it.innerIp }
        )
        return installAgentRes
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