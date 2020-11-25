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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.misc.cron

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.common.environment.agent.client.DevCloudClient
import com.tencent.devops.misc.dao.EnvironmentNodeDao
import com.tencent.devops.misc.dao.EnvironmentThirdPartyAgentDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class UpdateDevCloudVmStatus @Autowired constructor(
    private val dslContext: DSLContext,
    private val nodeDao: EnvironmentNodeDao,
    private val devCloudClient: DevCloudClient,
    private val redisOperation: RedisOperation,
    private val thirdPartyAgentDao: EnvironmentThirdPartyAgentDao,
    private val gray: Gray
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UpdateDevCloudVmStatus::class.java)
        private const val lockKey = "env_cron_updateDevCloudVmStatus"
        private const val executeKey = "env_cron_updateDevCloudVmStatus_execute"
        private const val delay = 30L
    }

    @Scheduled(initialDelay = 10000, fixedDelay = delay * 1000)
    fun run() {
        logger.info("updateDevCloudVmStatus")
        val redisLock = RedisLock(redisOperation,
            lockKey(), delay
        )
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("Lock success")
                val lock = redisOperation.get(executeKey())
                if (lock == null) {
                    redisOperation.set(executeKey(), "LOCKED",
                        delay
                    )
                    logger.info("updateDevCloudVmStatus start")
                    updateStatus()
                } else {
                    logger.info("Lock failed, ignore")
                }
            }
        } catch (e: Throwable) {
            logger.error("updateDevCloudVmStatus exception", e)
        } finally {
            redisLock.unlock()
        }
    }

    private fun lockKey(): String {
        return if (gray.isGray()) {
            "gray_$lockKey"
        } else {
            lockKey
        }
    }

    private fun executeKey(): String {
        return if (gray.isGray()) {
            "gray_$executeKey"
        } else {
            executeKey
        }
    }

    private fun updateStatus() {
        val devCloudNodeList = nodeDao.listAllNodesByType(dslContext, NodeType.DEVCLOUD)
        if (devCloudNodeList.isEmpty()) {
            return
        }
        devCloudNodeList.forEach {
            if (!gray.isGrayMatchProject(it.projectId, redisOperation)) {
                logger.info("The project[${it.projectId}] is not match the gray type[${gray.isGray()}], ignore")
                return@forEach
            }
            if (it.nodeStatus == NodeStatus.CREATING.name ||
                    it.nodeStatus == NodeStatus.DELETED.name ||
                    it.nodeStatus == NodeStatus.DELETING.name ||
                    it.nodeStatus == NodeStatus.CREATING.name ||
                    it.nodeStatus == NodeStatus.STARTING.name ||
                    it.nodeStatus == NodeStatus.STOPPING.name ||
                    it.nodeStatus == NodeStatus.RESTARTING.name ||
                    it.nodeStatus == NodeStatus.BUILDING_IMAGE.name) {
                return@forEach
            }
            val statusResponse = devCloudClient.getContainerStatus(it.operator, it.nodeName)
            val actionCode = statusResponse.optInt("actionCode")
            if (actionCode == 200) {
                val status = statusResponse.optString("data")
                val nodeStatus = when (status) {
                    "running" -> {
                        val agentNodes = thirdPartyAgentDao.getAgentsByNodeIds(dslContext, setOf(it.nodeId), it.projectId)
                        if (agentNodes.isNotEmpty && agentNodes[0].status == AgentStatus.IMPORT_OK.status) {
                            NodeStatus.NORMAL
                        } else {
                            NodeStatus.RUNNING
                        }
                    }
                    "stopped" -> NodeStatus.STOPPED
                    else -> NodeStatus.parseByName(it.nodeStatus)
                }
                if (nodeStatus.name != it.nodeStatus) {
                    logger.info("Update dev cloud node status: nodeId:${it.nodeId}, nodeName:${it.nodeName}, IP:${it.nodeIp}, status: ${it.nodeStatus}")
                    nodeDao.updateNodeStatus(dslContext, it.nodeId, nodeStatus)
                }
            } else {
                val actionMessage = statusResponse.optString("actionMessage")
                logger.error("Get container status failed, nodeName:${it.nodeName} failed msg: $actionMessage")
            }
        }
    }
}