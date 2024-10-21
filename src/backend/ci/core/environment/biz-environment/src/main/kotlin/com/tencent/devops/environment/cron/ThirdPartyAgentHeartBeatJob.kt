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

package com.tencent.devops.environment.cron

import com.tencent.devops.common.api.enums.AgentAction
import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.environment.constant.THIRD_PARTY_AGENT_HEARTBEAT_INTERVAL
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.service.NodeWebsocketService
import com.tencent.devops.environment.service.thirdpartyagent.ThirdPartAgentService
import com.tencent.devops.environment.utils.ThirdPartyAgentHeartbeatUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Suppress("ALL", "UNUSED")
class ThirdPartyAgentHeartBeatJob @Autowired constructor(
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val nodeDao: NodeDao,
    private val thirdPartyAgentHeartbeatUtils: ThirdPartyAgentHeartbeatUtils,
    private val redisOperation: RedisOperation,
    private val webSocketDispatcher: WebSocketDispatcher,
    private val nodeWebsocketService: NodeWebsocketService,
    private val thirdpartyAgentService: ThirdPartAgentService
) {

    @Scheduled(initialDelay = 5000, fixedDelay = 3000)
    fun heartbeat() {
        RedisLock(redisOperation = redisOperation, lockKey = LOCK_KEY, expiredTimeInSeconds = 600).use { lock ->

            if (!lock.tryLock()) {
                return
            }
            checkOKAgent()

            checkExceptionAgent()

            checkUnImportAgent()
        }
    }

    private fun checkOKAgent() {
        val nodeRecords = thirdPartyAgentDao.listByStatus(
            dslContext,
            setOf(AgentStatus.IMPORT_OK)
        )
        if (nodeRecords.isEmpty()) {
            return
        }
        nodeRecords.forEach { record ->
            val heartbeatTime = thirdPartyAgentHeartbeatUtils.getHeartbeatTime(record.id, record.projectId)
                ?: return@forEach

            val escape = System.currentTimeMillis() - heartbeatTime
            // 50s
            if (escape > 10 * THIRD_PARTY_AGENT_HEARTBEAT_INTERVAL * 1000) {
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)
                    thirdPartyAgentDao.updateStatus(
                        dslContext = context,
                        id = record.id,
                        nodeId = null,
                        projectId = record.projectId,
                        status = AgentStatus.IMPORT_EXCEPTION
                    )
                    thirdpartyAgentService.addAgentAction(
                        projectId = record.projectId,
                        agentId = record.id,
                        action = AgentAction.OFFLINE
                    )
                    if (record.nodeId == null) {
                        return@transaction
                    }
                    val nodeRecord = nodeDao.get(context, record.projectId, record.nodeId)
                    if (nodeRecord == null || nodeRecord.nodeStatus == NodeStatus.DELETED.name) {
                        deleteAgent(context, record.projectId, record.id)
                    }
                    nodeDao.updateNodeStatus(context, setOf(record.nodeId), NodeStatus.ABNORMAL)
                }
            }
        }
    }

    private fun checkUnImportAgent() {
        val nodeRecords = thirdPartyAgentDao.listByStatus(
            dslContext,
            setOf(AgentStatus.UN_IMPORT_OK)
        )
        if (nodeRecords.isEmpty()) {
            return
        }
        nodeRecords.forEach { record ->
            val heartbeatTime = thirdPartyAgentHeartbeatUtils.getHeartbeatTime(record.id, record.projectId)
                ?: return@forEach
            val escape = System.currentTimeMillis() - heartbeatTime
            if (escape > 2 * THIRD_PARTY_AGENT_HEARTBEAT_INTERVAL * 1000) {
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)
                    thirdPartyAgentDao.updateStatus(
                        dslContext = context,
                        id = record.id,
                        nodeId = null,
                        projectId = record.projectId,
                        status = AgentStatus.UN_IMPORT,
                        expectStatus = AgentStatus.UN_IMPORT_OK
                    )
                }
            }
        }
    }

    private fun checkExceptionAgent() {
        // Trying to delete the third party agents
        val exceptionRecord = thirdPartyAgentDao.listByStatus(
            dslContext,
            setOf(AgentStatus.IMPORT_EXCEPTION)
        )
        if (exceptionRecord.isEmpty()) {
            return
        }

        exceptionRecord.forEach { record ->
            if (record.nodeId == null) {
                return@forEach
            }
            val nodeRecord = nodeDao.get(dslContext, record.projectId, record.nodeId)
            if (nodeRecord == null || nodeRecord.nodeStatus == NodeStatus.DELETED.name) {
                deleteAgent(dslContext, record.projectId, record.id)
            }
        }
    }

    private fun deleteAgent(dslContext: DSLContext, projectId: String, agentId: Long) {
        logger.info("Trying to delete the agent($agentId) of project($projectId)")
        thirdPartyAgentDao.delete(dslContext, agentId, projectId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentHeartBeatJob::class.java)
        private const val LOCK_KEY = "env_cron_agent_heartbeat_check"
    }
}
