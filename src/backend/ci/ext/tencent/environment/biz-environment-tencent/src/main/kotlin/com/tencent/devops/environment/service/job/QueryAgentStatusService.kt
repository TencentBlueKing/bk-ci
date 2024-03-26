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

import com.tencent.devops.environment.pojo.job.agentreq.Condition
import com.tencent.devops.environment.pojo.job.jobreq.QueryAgentStatusFromJobReq
import com.tencent.devops.environment.pojo.job.agentreq.QueryAgentStatusFromNodemanReq
import com.tencent.devops.environment.pojo.job.agentres.AgentResult
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentStatusFromJobResult
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentStatusFromNodemanResult
import com.tencent.devops.environment.pojo.job.AgentVersion
import com.tencent.devops.environment.pojo.job.agentreq.AgentCondition
import com.tencent.devops.environment.pojo.job.agentreq.AgentQueryAgentStatusFromNodemanReq
import com.tencent.devops.environment.pojo.job.agentres.AgentOriginalResult
import com.tencent.devops.environment.pojo.job.agentres.AgentQueryAgentStatusFromNodemanResult
import com.tencent.devops.environment.pojo.job.agentres.ExtraData
import com.tencent.devops.environment.pojo.job.agentres.FilterHostInfo
import com.tencent.devops.environment.pojo.job.agentres.IdentityInfo
import com.tencent.devops.environment.pojo.job.agentres.JobResultForFilterHostInfo
import com.tencent.devops.environment.pojo.job.jobresp.JobResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("QueryAgentStatusService")
class QueryAgentStatusService @Autowired constructor(
    private val jobService: JobService,
    private val nodeManApi: NodeManApi
) {
    companion object {
        private val logger = LoggerFactory.getLogger(QueryAgentStatusService::class.java)
        private const val DEFAULT_AGENT_CONDITION_KEY = "inner_ip"
        private const val DEFAULT_EXTRA_DATA = "job_result"
        private const val DEFAULT_PAGE_SIZE = 100
        private const val DEFAULT_PAGE = 1
        private const val DEFAULT_ONLY_IP = false
        private const val DEFAULT_RUNNING_COUNT = false
        private const val INSTALLED_AGENT_TAG = true
        private const val NOT_INSTALLED_AGENT_TAG = false
        private const val AGENT_ABNORMAL_STATUS = 0
    }

    fun getAgentVersions(ipAndHostIdList: List<AgentVersion>): List<AgentVersion>? {
        val hostIdList = ipAndHostIdList.mapNotNull { it.bkHostId }.distinct()
        val ipList = ipAndHostIdList.mapNotNull { it.ip }.distinct()
        val hostIdToAgentVersionMap = ipAndHostIdList.associateBy { it.bkHostId }
        // 1. 调用nodeman接口查询 是否已安装agent
        val nodemanRes = getAgentVersionsFromNodeman(hostIdList, ipList)
        val total = nodemanRes.data?.total ?: 0
        val installedHostIdList = if (total > 0) {
            nodemanRes.data?.list?.filter {
                it?.status != "NOT_INSTALLED"
            }?.mapNotNull { it?.bkHostId }
        } else null
        if (logger.isDebugEnabled)
            logger.debug("[getAgentVersions]installedHostIdList:${installedHostIdList?.joinToString()}")
        // 2.1 已安装agent，调用job的接口查询agent状态
        val installedAgentVersionList =
            if (installedHostIdList.isNullOrEmpty()) {
                logger.info("[getAgentVersions]installedHostIdList is null or empty.")
                emptyList()
            } else {
                // 2.1.1 调用job的接口查询agent状态，有对应返回
                val jobRes = getAgentVersionsFromJob(installedHostIdList)
                val hostIdMutableList = hostIdList.toMutableList()
                val jobResAgentVersion = jobRes.data?.agentInfoList?.map {
                    hostIdMutableList.remove(it.bkHostId)
                    AgentVersion(
                        ip = hostIdToAgentVersionMap[it.bkHostId]?.ip,
                        bkHostId = it.bkHostId,
                        installedTag = INSTALLED_AGENT_TAG,
                        version = it.version,
                        status = it.status
                    )
                }
                // 2.1.2 调用job的接口查询agent状态，没有对应返回 (查询节点管理 - 已安装agent，查询job - 无对应返回，认为此时agent异常)
                val agentAbnormalList = if (hostIdMutableList.isNotEmpty()) {
                    hostIdMutableList.map {
                        AgentVersion(
                            ip = hostIdToAgentVersionMap[it]?.ip,
                            bkHostId = it,
                            installedTag = INSTALLED_AGENT_TAG,
                            version = null,
                            status = AGENT_ABNORMAL_STATUS
                        )
                    }
                } else emptyList()
                jobResAgentVersion?.plus(agentAbnormalList)
            }
        logger.info(
            "[getAgentVersions]installedAgentVersionList: " +
                installedAgentVersionList?.joinToString(separator = ", ", transform = { it.toString() })
        )
        // 2.2 未安装agent
        val notInstalledAgentHostIdList = nodemanRes.data?.list?.filter { // 未安装agent，不再查job了
            it?.status == "NOT_INSTALLED"
        }?.mapNotNull { it?.bkHostId }
        val notInstalledAgentVersionList =
            if (notInstalledAgentHostIdList.isNullOrEmpty())
                emptyList()
            else {
                notInstalledAgentHostIdList.map {
                    AgentVersion(
                        ip = hostIdToAgentVersionMap[it]?.ip,
                        bkHostId = it,
                        installedTag = NOT_INSTALLED_AGENT_TAG
                    )
                }
            }
        logger.info(
            "[getAgentVersions]notInstalledAgentList: " +
                notInstalledAgentVersionList.joinToString(separator = ", ", transform = { it.toString() })
        )
        return (installedAgentVersionList ?: emptyList()) + notInstalledAgentVersionList
    }

    private fun getAgentVersionsFromNodeman(
        hostIdList: List<Long>,
        ipList: List<String>
    ): AgentResult<QueryAgentStatusFromNodemanResult> {
        val getAgentVersionsFromNodemanReq = QueryAgentStatusFromNodemanReq(
            bkHostId = hostIdList,
            conditions = listOf(
                Condition(
                    key = DEFAULT_AGENT_CONDITION_KEY,
                    value = ipList
                )
            ),
            extraData = listOf(DEFAULT_EXTRA_DATA),
            pagesize = DEFAULT_PAGE_SIZE,
            page = DEFAULT_PAGE,
            onlyIp = DEFAULT_ONLY_IP,
            runningCount = DEFAULT_RUNNING_COUNT
        )
        return queryAgentStatusFromNodeman(getAgentVersionsFromNodemanReq)
    }

    private fun queryAgentStatusFromNodeman(
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

    private fun getAgentVersionsFromJob(
        hostIdList: List<Long>
    ): JobResult<QueryAgentStatusFromJobResult> {
        val queryAgentStatusRequest = QueryAgentStatusFromJobReq(
            hostIdList = hostIdList
        )
        return jobService.queryAgentStatusFromJob(queryAgentStatusRequest)
    }
}