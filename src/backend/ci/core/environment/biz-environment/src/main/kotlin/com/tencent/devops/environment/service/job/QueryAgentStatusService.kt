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
import com.tencent.devops.environment.pojo.job.agentreq.QueryAgentStatusFromJobReq
import com.tencent.devops.environment.pojo.job.agentreq.QueryAgentStatusFromNodemanReq
import com.tencent.devops.environment.pojo.job.agentres.AgentResult
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentStatusFromJobResult
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentStatusFromNodemanResult
import com.tencent.devops.environment.pojo.job.AgentVersion
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("QueryAgentStatusService")
class QueryAgentStatusService @Autowired constructor(
    private val agentApi: AgentApi,
    private val jobService: JobService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(QueryAgentStatusService::class.java)
        private const val DEFAULT_AGENT_CONDITION_KEY = "inner_ip"
        private const val DEFAULT_EXTRA_DATA = "job_result"
        private const val DEFAULT_PAGE_SIZE = 20
        private const val DEFAULT_PAGE = 1
        private const val DEFAULT_ONLY_IP = false
        private const val DEFAULT_RUNNING_COUNT = false
        private const val INSTALLED_AGENT_TAG = true
        private const val NOT_INSTALLED_AGENT_TAG = false
    }

    fun getAgentVersions(
        userId: String,
        projectId: String,
        ipAndHostIdList: List<AgentVersion>
    ): List<AgentVersion>? {
        val hostIdList = ipAndHostIdList.mapNotNull { it.bkHostId }
        val ipList = ipAndHostIdList.mapNotNull { it.ip }
        val nodemanRes = getAgentVersionsFromNodeman(userId, projectId, hostIdList, ipList)
        val total = nodemanRes.data?.total ?: 0
        val installedHostIdList = if (total > 0) {
            nodemanRes.data?.list?.filter { it.status != "NOT_INSTALLED" }?.mapNotNull { it.bkHostId } // 已安装agent，查job
        } else null
        val installedAgentVersionList =
            if (installedHostIdList.isNullOrEmpty())
                emptyList()
            else {
                val jobRes = getAgentVersionsFromJob(userId, projectId, installedHostIdList)
                jobRes.data?.agentInfoList?.map {
                    AgentVersion(
                        bkHostId = it.bkHostId,
                        installedTag = INSTALLED_AGENT_TAG,
                        version = it.version,
                        status = it.status
                    )
                }
            }
        val notInstalledAgentHostIdList = nodemanRes.data?.list?.filter { // 未安装agent，不再查job了
            it.status == "NOT_INSTALLED"
        }?.mapNotNull { it.bkHostId }
        val notInstalledAgentList =
            if (notInstalledAgentHostIdList.isNullOrEmpty())
                emptyList()
            else {
                notInstalledAgentHostIdList.map {
                    AgentVersion(
                        bkHostId = it,
                        installedTag = NOT_INSTALLED_AGENT_TAG
                    )
                }
            }
        if (logger.isDebugEnabled)
            logger.debug("[getAgentVersions]installedAgentVersionList:$installedAgentVersionList")
        if (logger.isDebugEnabled)
            logger.debug("[getAgentVersions]notInstalledAgentList:$notInstalledAgentList")
        return (installedAgentVersionList ?: emptyList()) + notInstalledAgentList
    }

    private fun getAgentVersionsFromNodeman(
        userId: String,
        projectId: String,
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
        return jobService.queryAgentStatusFromNodeman(userId, projectId, getAgentVersionsFromNodemanReq)
    }

    private fun getAgentVersionsFromJob(
        userId: String,
        projectId: String,
        hostIdList: List<Long>
    ): AgentResult<QueryAgentStatusFromJobResult> {
        val queryAgentStatusRequest = QueryAgentStatusFromJobReq(
            hostIdList = hostIdList
        )
        return jobService.queryAgentStatusFromJob(userId, projectId, queryAgentStatusRequest)
    }
}