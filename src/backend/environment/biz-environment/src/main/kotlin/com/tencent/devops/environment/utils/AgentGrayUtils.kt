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

package com.tencent.devops.environment.utils

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AgentGrayUtils @Autowired constructor(
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AgentGrayUtils::class.java)
        private const val FORCE_UPGRADE_AGENT_PREFIX = "thirdparty.agent.force.upgrade.id_"
        private const val LOCK_UPGRADE_AGENT_PREFIX = "thirdparty.agent.lock.upgrade.id_"
        private const val CAN_UPGRADE_AGENT_PREFIX = "thirdparty.agent.can.upgrade.id_"
    }

    private fun getForceUpgradeAgentKey(agentId: Long) = "$FORCE_UPGRADE_AGENT_PREFIX$agentId"

    private fun getCanUpgradeAgentKey(agentId: Long) = "$CAN_UPGRADE_AGENT_PREFIX$agentId"

    private fun getCanUpgradePrefix() = CAN_UPGRADE_AGENT_PREFIX

    private fun getLockUpgradeAgentKey(agentId: Long) = "$LOCK_UPGRADE_AGENT_PREFIX$agentId"

    fun setForceUpgradeAgents(agentIds: List<Long>) {
        logger.info("set force upgrade agents: $agentIds")
        agentIds.forEach {
            redisOperation.set(getForceUpgradeAgentKey(it), "true")
        }
    }

    fun unsetForceUpgradeAgents(agentIds: List<Long>) {
        logger.info("unset force upgrade agents: $agentIds")
        val keys = agentIds.map { getForceUpgradeAgentKey(it) }
        redisOperation.delete(keys)
    }

    fun getAllForceUpgradeAgents(): List<Long> {
        val prefixLength = FORCE_UPGRADE_AGENT_PREFIX.length
        val agentIds = redisOperation.keys("$FORCE_UPGRADE_AGENT_PREFIX*")
            .map { it.substring(prefixLength).toLong() }
        logger.info("all force upgrade agent: $agentIds")
        return agentIds
    }

    fun cleanAllForceUpgradeAgents() {
        val allKeys = redisOperation.keys("$FORCE_UPGRADE_AGENT_PREFIX*")
        logger.info("clean all force agent upgrade, keys: $allKeys")
        if (allKeys.isNotEmpty()) {
            redisOperation.delete(allKeys)
        }
    }

    fun checkLockUpgrade(agentHashId: String): Boolean {
        val agentId = HashUtil.decodeIdToLong(agentHashId)
        val lockUpgrade = !redisOperation.get(getLockUpgradeAgentKey(agentId)).isNullOrBlank()
        logger.info("get agent lock upgrade($agentId): $lockUpgrade")
        return lockUpgrade
    }

    fun setLockUpgradeAgents(agentIds: List<Long>) {
        logger.info("set lock upgrade agents: $agentIds")
        agentIds.forEach {
            redisOperation.set(getLockUpgradeAgentKey(it), "true")
        }
    }

    fun unsetLockUpgradeAgents(agentIds: List<Long>) {
        logger.info("unset lock upgrade agents: $agentIds")
        val keys = agentIds.map { getLockUpgradeAgentKey(it) }
        redisOperation.delete(keys)
    }

    fun getAllLockUpgradeAgents(): List<Long> {
        val prefixLength = LOCK_UPGRADE_AGENT_PREFIX.length
        val agentIds = redisOperation.keys("$LOCK_UPGRADE_AGENT_PREFIX*")
            .map { it.substring(prefixLength).toLong() }
        logger.info("all Lock upgrade agent: $agentIds")
        return agentIds
    }

    fun cleanAllLockUpgradeAgents() {
        val allKeys = redisOperation.keys("$LOCK_UPGRADE_AGENT_PREFIX*")
        logger.info("clean all lock agent upgrade, keys: $allKeys")
        if (allKeys.isNotEmpty()) {
            redisOperation.delete(allKeys)
        }
    }

    fun setCanUpgradeAgents(agentIds: List<Long>) {
        val redisKeyPrefix = getCanUpgradePrefix()
        val existingAgentIds = redisOperation.keys("$redisKeyPrefix*")
            .map { it.substring(redisKeyPrefix.length).toLong() }.toSet()
        val newAgentIds = agentIds.toSet()

        val toAddAgentIds = newAgentIds.filterNot { existingAgentIds.contains(it) }
        if (toAddAgentIds.isNotEmpty()) {
            toAddAgentIds.forEach {
                redisOperation.set(getCanUpgradeAgentKey(it), "true")
            }
        }

        val toDeleteAgents = existingAgentIds.filterNot { newAgentIds.contains(it) }
        if (toDeleteAgents.isNotEmpty()) {
            redisOperation.delete(toDeleteAgents.map { getCanUpgradeAgentKey(it) })
        }
    }

    fun getCanUpgradeAgents(): List<Long> {
        val redisKeyPrefix = getCanUpgradePrefix()
        val agentIds = redisOperation.keys("$redisKeyPrefix*")
            .map { it.substring(redisKeyPrefix.length).toLong() }
        logger.info("can upgrade agents: $agentIds")
        return agentIds
    }
}