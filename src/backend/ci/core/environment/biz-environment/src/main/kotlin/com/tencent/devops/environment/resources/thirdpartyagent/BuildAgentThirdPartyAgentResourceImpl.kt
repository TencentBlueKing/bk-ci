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

package com.tencent.devops.environment.resources.thirdpartyagent

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.agent.NewHeartbeatInfo
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.thirdpartyagent.BuildAgentThirdPartyAgentResource
import com.tencent.devops.environment.pojo.thirdpartyagent.HeartbeatResponse
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentHeartbeatInfo
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentPipeline
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentStartInfo
import com.tencent.devops.environment.pojo.thirdpartyagent.pipeline.PipelineResponse
import com.tencent.devops.environment.service.thirdpartyagent.AgentMetricService
import com.tencent.devops.environment.service.thirdpartyagent.ImportService
import com.tencent.devops.environment.service.thirdpartyagent.ThirdPartyAgentMgrService
import com.tencent.devops.environment.service.thirdpartyagent.ThirdPartyAgentPipelineService
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("ReturnCount", "ThrowsCount", "MagicNumber")
class BuildAgentThirdPartyAgentResourceImpl @Autowired constructor(
    private val thirdPartyAgentService: ThirdPartyAgentMgrService,
    private val thirdPartyAgentPipelineService: ThirdPartyAgentPipelineService,
    private val importService: ImportService,
    private val redisOperation: RedisOperation,
    private val agentMetricService: AgentMetricService
) : BuildAgentThirdPartyAgentResource {

    @AuditEntry(actionId = ActionId.ENV_NODE_CREATE)
    override fun agentStartup(
        projectId: String,
        agentId: String,
        secretKey: String,
        startInfo: ThirdPartyAgentStartInfo
    ): Result<AgentStatus> {
        checkParam(projectId, agentId, secretKey)
        val status = thirdPartyAgentService.agentStartup(projectId, agentId, secretKey, startInfo)
        // #4868 构建机安装完毕启动之后，不需要在web再次点击导入就自动生成节点导入
        if (AgentStatus.UN_IMPORT_OK == status) {
            thirdPartyAgentService.getAgent(projectId, agentId).data?.let {
                importService.importAgent(
                    userId = it.createUser, projectId = projectId, agentId = agentId, masterVersion = it.masterVersion
                )
            }
        }
        return Result(status)
    }

    override fun agentShutdown(
        projectId: String,
        agentId: String,
        secretKey: String,
        shutdownNormal: Boolean
    ): Result<AgentStatus> {
        checkParam(projectId, agentId, secretKey)
        return Result(thirdPartyAgentService.agentShutdown(projectId, agentId, secretKey, shutdownNormal))
    }

    override fun getAgentStatus(
        projectId: String,
        agentId: String,
        secretKey: String
    ): Result<AgentStatus> {
        checkParam(projectId, agentId, secretKey)

        val requestAgentId = agentStatusRequestCache.getIfPresent(agentId)
        if (requestAgentId != null) {
            return Result(1, "request too frequently")
        } else {
            val lockKey = "environment:thirdPartyAgent:agentStatusRequestLock_$agentId"
            val redisLock = RedisLock(redisOperation, lockKey, 1)
            if (redisLock.tryLock()) {
                agentStatusRequestCache.put(agentId, agentId)
            } else {
                return Result(1, "request too frequently")
            }
        }

        return Result(thirdPartyAgentService.getAgentStatus(projectId, agentId, secretKey))
    }

    override fun agentHeartbeat(
        projectId: String,
        agentId: String,
        secretKey: String,
        heartbeatInfo: ThirdPartyAgentHeartbeatInfo?
    ): Result<AgentStatus> {
        logger.warn("agentHeartbeat|$projectId|$agentId| request is not allowed")
        return Result(1, "request is not allowed")
//        checkParam(projectId, agentId, secretKey)
//
//        val requestAgentId = agentHeartbeatRequestCache.getIfPresent(agentId)
//        if (requestAgentId != null) {
//            logger.warn("agentHeartbeat|$projectId|$agentId| request too frequently")
//            return Result(1, "request too frequently")
//        } else {
//            val lockKey = "environment:thirdPartyAgent:agentHeartbeatRequestLock_$agentId"
//            val redisLock = RedisLock(redisOperation, lockKey, 1)
//            if (redisLock.tryLock()) {
//                agentHeartbeatRequestCache.put(agentId, agentId)
//            } else {
//                return Result(1, "request too frequently")
//            }
//        }
//
//        return Result(thirdPartyAgentService.heartBeat(projectId, agentId, secretKey, heartbeatInfo))
    }

    override fun newHeartbeat(
        projectId: String,
        agentId: String,
        secretKey: String,
        heartbeatInfo: NewHeartbeatInfo
    ): Result<HeartbeatResponse> {
        checkParam(projectId, agentId, secretKey)

        val requestAgentId = agentHeartbeatRequestCache.getIfPresent(agentId)
        if (requestAgentId != null) {
            logger.warn("newHeartbeat|$projectId|$agentId| request too frequently")
            return Result(1, "request too frequently")
        } else {
            val lockKey = "environment:thirdPartyAgent:agentHeartbeatRequestLock_$agentId"
            val redisLock = RedisLock(redisOperation, lockKey, 1)
            if (redisLock.tryLock()) {
                agentHeartbeatRequestCache.put(agentId, agentId)
            } else {
                return Result(1, "request too frequently")
            }
        }

        return Result(thirdPartyAgentService.newHeartbeat(projectId, agentId, secretKey, heartbeatInfo))
    }

    override fun getPipelines(projectId: String, agentId: String, secretKey: String): Result<ThirdPartyAgentPipeline?> {
        checkParam(projectId, agentId, secretKey)

        val requestAgentId = agentPipelineRequestCache.getIfPresent(agentId)
        if (requestAgentId != null) {
            return Result(1, "request too frequently")
        } else {
            val lockKey = "environment:thirdPartyAgent:agentPipelineRequestLock_$agentId"
            val redisLock = RedisLock(redisOperation, lockKey, 1)
            if (redisLock.tryLock()) {
                agentPipelineRequestCache.put(agentId, agentId)
            } else {
                return Result(1, "request too frequently")
            }
        }

        return Result(thirdPartyAgentPipelineService.getPipelines(projectId, agentId, secretKey))
    }

    override fun updatePipelineStatus(
        projectId: String,
        agentId: String,
        secretKey: String,
        response: PipelineResponse
    ): Result<Boolean> {
        checkParam(projectId, agentId, secretKey)
        return Result(thirdPartyAgentPipelineService.updatePipelineStatus(projectId, agentId, secretKey, response))
    }

    override fun reportAgentMetrics(
        projectId: String,
        agentId: String,
        secretKey: String,
        data: String
    ): Result<Boolean> {
        return Result(agentMetricService.reportAgentMetrics(data))
    }

    private fun checkParam(
        projectId: String,
        agentId: String,
        secretKey: String
    ) {
        if (projectId.isBlank()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_INVALID_PARAM_, params = arrayOf("projectId"))
        }
        if (agentId.isBlank()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_INVALID_PARAM_, params = arrayOf("Agent ID"))
        }
        if (secretKey.isBlank()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_INVALID_PARAM_, params = arrayOf("Secret Key"))
        }
    }

    private val agentHeartbeatRequestCache: Cache<String, String> = CacheBuilder.newBuilder().maximumSize(10000)
        .expireAfterWrite(4, TimeUnit.SECONDS).build()

    private val agentStatusRequestCache: Cache<String, String> = CacheBuilder.newBuilder().maximumSize(10000)
        .expireAfterWrite(3, TimeUnit.SECONDS).build()

    private val agentPipelineRequestCache: Cache<String, String> = CacheBuilder.newBuilder().maximumSize(10000)
        .expireAfterWrite(10, TimeUnit.SECONDS).build()

    companion object {
        private val logger = LoggerFactory.getLogger(BuildAgentThirdPartyAgentResourceImpl::class.java)
    }
}
