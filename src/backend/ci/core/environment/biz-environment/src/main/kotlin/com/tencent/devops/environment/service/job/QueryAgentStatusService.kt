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

import com.tencent.devops.environment.pojo.job.agentreq.AgentCondition
import com.tencent.devops.environment.pojo.job.agentreq.AgentQueryAgentStatusFromNodemanReq
import com.tencent.devops.environment.pojo.job.agentreq.AgentQueryAgentStatusFromJobReq
import com.tencent.devops.environment.pojo.job.agentreq.Condition
import com.tencent.devops.environment.pojo.job.agentreq.QueryAgentStatusFromJobReq
import com.tencent.devops.environment.pojo.job.agentreq.QueryAgentStatusFromNodemanReq
import com.tencent.devops.environment.pojo.job.agentres.AgentInfo
import com.tencent.devops.environment.pojo.job.agentres.AgentQueryAgentStatusFromJobResult
import com.tencent.devops.environment.pojo.job.agentres.AgentResult
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentStatusFromJobResult
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentStatusFromNodemanResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
    }

    fun getAgentVersions() {
        TODO()
    }

    private fun getAgentVersionsFromNodeman(
        userId: String,
        projectId: String,
        hostIdList: List<Int>,
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
        hostIdList: List<Int>
    ): AgentResult<QueryAgentStatusFromJobResult> {
        val queryAgentStatusRequest = QueryAgentStatusFromJobReq(
            hostIdList = hostIdList
        )
        return jobService.queryAgentStatusFromJob(userId, projectId, queryAgentStatusRequest)
    }
}