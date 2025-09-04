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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.environment.pojo.apigw.ApiGwReq
import com.tencent.devops.environment.pojo.job.agentreq.AgentInstallAgentReq
import com.tencent.devops.environment.pojo.job.agentreq.QueryAgentInstallTaskStatusReq
import com.tencent.devops.environment.pojo.job.agentreq.QueryAgentStatusFromNodeManReq
import com.tencent.devops.environment.pojo.job.agentreq.RetryAgentInstallTaskNodeManReq
import com.tencent.devops.environment.pojo.job.agentreq.TerminateAgentInstallTaskNodeManReq
import com.tencent.devops.environment.pojo.job.agentres.AgentInstallAgentResult
import com.tencent.devops.environment.pojo.job.agentres.AgentInstallChannel
import com.tencent.devops.environment.pojo.job.agentres.AgentInstallTaskLog
import com.tencent.devops.environment.pojo.job.agentres.AgentOriginalResult
import com.tencent.devops.environment.pojo.job.agentres.AgentQueryAgentTaskStatusResult
import com.tencent.devops.environment.pojo.job.agentres.AgentTerminalAgentInstallTaskResult
import com.tencent.devops.environment.pojo.job.agentres.ManualInstallCommand
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentStatusFromNodemanResp
import com.tencent.devops.environment.pojo.job.agentres.RetryAgentInstallTaskResp
import com.tencent.devops.environment.service.api.BaseApiGwApi
import okhttp3.Response
import org.slf4j.LoggerFactory

class NodeManApi(
    apiBaseUrl: String,
    appCode: String,
    appSecret: String,
    username: String?
) : BaseApiGwApi(apiBaseUrl, appCode, appSecret, username) {

    companion object {
        private const val LOG_OUTPUT_MAX_LENGTH = 4000

        private const val PATH_INSTALL_AGENT = "/job/install"
        private const val PATH_QUERY_AGENT_INSTALL_TASK_STATUS = "/job/%s/details"
        private const val PATH_QUERY_AGENT_INSTALL_TASK_LOG = "/job/%s/log/?instance_id=%s"
        private const val PATH_TERMINAL_AGENT_INSTALL_TASK = "/job/%s/revoke"
        private const val PATH_RETRY_AGENT_INSTALL_TASK = "/job/%s/retry"
        private const val PATH_QUERY_AGENT_STATUS_FROM_NODMAN = "/host/search"
        private const val PATH_QUERY_AGENT_INSTALL_CHANNEL = "/install_channel/?with_hidden=%s"
        private const val PATH_OBTAIN_MANUAL_INSTALLATION_COMMAND = "/job/%s/get_job_commands/?bk_host_id=%s"

        private val logger = LoggerFactory.getLogger(NodeManApi::class.java)

        private val nodemanOperationName = ThreadLocal<String>()
        fun setNodemanOperationName(value: String) {
            nodemanOperationName.set(value)
        }

        fun getNodemanOperationName(): String? {
            return nodemanOperationName.get()
        }

        fun removeNodemanOperationName() {
            nodemanOperationName.remove()
        }
    }

    fun queryAgentInstallChannel(
        withHidden: Boolean
    ): AgentOriginalResult<Array<AgentInstallChannel>> {
        val pathAndParams = String.format(PATH_QUERY_AGENT_INSTALL_CHANNEL, withHidden)
        return executeGetRequest(
            pathAndParams = pathAndParams,
            shortGetTag = false,
            typeReference = object : TypeReference<AgentOriginalResult<Array<AgentInstallChannel>>>() {}
        )
    }

    fun getJobCommands(
        jobId: Int,
        hostId: Long
    ): AgentOriginalResult<ManualInstallCommand> {
        val pathAndParams = String.format(PATH_OBTAIN_MANUAL_INSTALLATION_COMMAND, jobId, hostId)
        return executeGetRequest(
            pathAndParams = pathAndParams,
            shortGetTag = false,
            typeReference = object : TypeReference<AgentOriginalResult<ManualInstallCommand>>() {}
        )
    }

    fun installAgent(installAgentRequest: AgentInstallAgentReq): AgentOriginalResult<AgentInstallAgentResult> {
        return executePostRequest(
            pathAndParams = PATH_INSTALL_AGENT,
            req = installAgentRequest,
            typeReference = object : TypeReference<AgentOriginalResult<AgentInstallAgentResult>>() {}
        )
    }

    fun queryAgentInstallTaskStatus(
        jobId: Int,
        queryAgentInstallTaskStatusReq: QueryAgentInstallTaskStatusReq
    ): AgentOriginalResult<AgentQueryAgentTaskStatusResult> {
        val pathAndParams = String.format(PATH_QUERY_AGENT_INSTALL_TASK_STATUS, jobId)
        return executePostRequest(
            pathAndParams = pathAndParams,
            req = queryAgentInstallTaskStatusReq,
            typeReference = object : TypeReference<AgentOriginalResult<AgentQueryAgentTaskStatusResult>>() {}
        )
    }

    fun queryAgentInstallTaskLog(
        jobId: Int,
        instanceId: String
    ): AgentOriginalResult<Array<AgentInstallTaskLog>> {
        val pathAndParams = String.format(PATH_QUERY_AGENT_INSTALL_TASK_LOG, jobId, instanceId)
        return executeGetRequest(
            pathAndParams = pathAndParams,
            shortGetTag = true,
            typeReference = object : TypeReference<AgentOriginalResult<Array<AgentInstallTaskLog>>>() {}
        )
    }

    fun queryAgentStatus(req: QueryAgentStatusFromNodeManReq): AgentOriginalResult<QueryAgentStatusFromNodemanResp> {
        return executePostRequest(
            pathAndParams = PATH_QUERY_AGENT_STATUS_FROM_NODMAN,
            req = req,
            typeReference = object : TypeReference<AgentOriginalResult<QueryAgentStatusFromNodemanResp>>() {}
        )
    }

    fun terminateAgentInstallTask(
        jobId: Int,
        req: TerminateAgentInstallTaskNodeManReq
    ): AgentOriginalResult<AgentTerminalAgentInstallTaskResult> {
        val pathAndParams = String.format(PATH_TERMINAL_AGENT_INSTALL_TASK, jobId)
        return executePostRequest(
            pathAndParams = pathAndParams,
            req = req,
            typeReference = object : TypeReference<AgentOriginalResult<AgentTerminalAgentInstallTaskResult>>() {}
        )
    }

    fun retryAgentInstallTask(
        jobId: Int,
        req: RetryAgentInstallTaskNodeManReq
    ): AgentOriginalResult<RetryAgentInstallTaskResp> {
        val pathAndParams = String.format(PATH_RETRY_AGENT_INSTALL_TASK, jobId)
        return executePostRequest(
            pathAndParams = pathAndParams,
            req = req,
            typeReference = object : TypeReference<AgentOriginalResult<RetryAgentInstallTaskResp>>() {}
        )
    }

    private fun <T : ApiGwReq, R : Any> executePostRequest(
        pathAndParams: String,
        req: T,
        typeReference: TypeReference<AgentOriginalResult<R>>
    ): AgentOriginalResult<R> {
        doPost(pathAndParams, req).use { resp ->
            return getResultFromRes(resp, typeReference)
        }
    }

    private fun <R> executeGetRequest(
        pathAndParams: String,
        shortGetTag: Boolean = false,
        typeReference: TypeReference<AgentOriginalResult<R>>
    ): AgentOriginalResult<R> {
        if (shortGetTag) {
            doShortGet(pathAndParams).use { resp ->
                return getResultFromRes(resp, typeReference)
            }
        } else {
            doGet(pathAndParams).use { resp ->
                return getResultFromRes(resp, typeReference)
            }
        }
    }

    private fun <R> getResultFromRes(
        response: Response,
        typeReference: TypeReference<AgentOriginalResult<R>>
    ): AgentOriginalResult<R> {
        val operationName = getNodemanOperationName()
        removeNodemanOperationName()
        try {
            val responseBody = response.body?.string()
            logger.info("[$operationName] response body(origin): ${logWithLengthLimit(responseBody.toString())}")
            val agentResp = JsonUtil.to(responseBody!!, typeReference)
            if (!agentResp.result!!) {
                logger.error(
                    "[$operationName] Execute failed! Error code: ${agentResp.code}, " +
                        "Error msg: ${agentResp.message}"
                )
                throw RemoteServiceException(
                    errorCode = agentResp.code,
                    errorMessage = "Execute failed! Error code: ${agentResp.code}, " +
                        "Error msg: ${agentResp.message}"
                )
            }
            return agentResp
        } catch (exception: Exception) {
            logger.warn("Failed to execute the HTTP request. [$operationName]Exception:", exception)
            throw exception
        }
    }

    private fun logWithLengthLimit(logOrigin: String): String {
        return if (logOrigin.length > LOG_OUTPUT_MAX_LENGTH)
            logOrigin.substring(0, LOG_OUTPUT_MAX_LENGTH)
        else
            logOrigin
    }
}
