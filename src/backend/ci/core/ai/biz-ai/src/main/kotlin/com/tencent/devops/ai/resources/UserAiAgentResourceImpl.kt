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

package com.tencent.devops.ai.resources

import com.tencent.devops.ai.api.user.UserAiAgentResource
import com.tencent.devops.ai.pojo.AgentInfo
import com.tencent.devops.ai.pojo.ServiceAgentRunRequest
import com.tencent.devops.ai.pojo.ServiceAgentRunResponse
import com.tencent.devops.ai.service.AiAgentInvocationService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.glassfish.jersey.server.ChunkedOutput
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.Executors

@RestResource
class UserAiAgentResourceImpl @Autowired constructor(
    private val aiAgentInvocationService: AiAgentInvocationService
) : UserAiAgentResource {

    override fun listAgents(
        userId: String
    ): Result<List<AgentInfo>> {
        return Result(aiAgentInvocationService.listAgents())
    }

    override fun runAgent(
        userId: String,
        agentName: String,
        request: ServiceAgentRunRequest
    ): Result<ServiceAgentRunResponse> {
        val response = aiAgentInvocationService.invokeAgent(
            agentName = agentName,
            userId = userId,
            request = request
        )
        return Result(response)
    }

    override fun streamAgent(
        userId: String,
        agentName: String,
        request: ServiceAgentRunRequest
    ): ChunkedOutput<String> {
        val output = ChunkedOutput<String>(String::class.java)
        EXECUTOR.execute {
            try {
                output.use {
                    aiAgentInvocationService.streamAgent(
                        agentName, userId, request, it
                    )
                }
            } catch (e: Exception) {
                logger.warn(
                    "[AiAgentStream] ChunkedOutput error: {}",
                    e.message
                )
            }
        }
        return output
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            UserAiAgentResourceImpl::class.java
        )
        private val EXECUTOR = Executors.newCachedThreadPool()
    }
}
