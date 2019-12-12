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

package com.tencent.devops.common.environment.agent

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray

class AgentGrayUtils constructor(
    private val redisOperation: RedisOperation,
    private val gray: Gray
) {
    companion object {
        private const val CURRENT_AGENT_MASTER_VERSION = "environment.thirdparty.agent.master.version"
        private const val GRAY_CURRENT_AGENT_MASTERT_VERSION = "environment.thirdparty.agent.gray.master.version"

        private const val CURRENT_AGENT_VERSION = "environment.thirdparty.agent.verison"
        private const val GRAY_CURRENT_AGENT_VERSION = "environment.thirdparty.agent.gray.version"

        private const val CAN_UPGRADE_AGENT_SET_KEY = "environment:thirdparty:can_upgrade"
        private const val LOCK_UPGRADE_AGENT_SET_KEY = "environment:thirdparty:lock_upgrade"
        private const val FORCE_UPGRADE_AGENT_SET_KEY = "environment:thirdparty:force_upgrade"
    }

    fun checkForceUpgrade(agentHashId: String): Boolean {
        val agentId = HashUtil.decodeIdToLong(agentHashId)
        return (redisOperation.getSetMembers(FORCE_UPGRADE_AGENT_SET_KEY) ?: setOf()).contains(agentId.toString())
    }

    fun setForceUpgradeAgents(agentIds: List<Long>) {
        agentIds.forEach {
            redisOperation.addSetValue(FORCE_UPGRADE_AGENT_SET_KEY, it.toString())
        }
    }

    fun unsetForceUpgradeAgents(agentIds: List<Long>) {
        agentIds.forEach {
            redisOperation.removeSetMember(FORCE_UPGRADE_AGENT_SET_KEY, it.toString())
        }
    }

    fun getAllForceUpgradeAgents(): List<Long> {
        return (redisOperation.getSetMembers(FORCE_UPGRADE_AGENT_SET_KEY)
            ?: setOf()).filter { !it.isBlank() }.map { it.toLong() }
    }

    fun cleanAllForceUpgradeAgents() {
        val allIds = redisOperation.getSetMembers(FORCE_UPGRADE_AGENT_SET_KEY) ?: return
        allIds.forEach {
            redisOperation.removeSetMember(FORCE_UPGRADE_AGENT_SET_KEY, it)
        }
    }

    fun checkLockUpgrade(agentHashId: String): Boolean {
        val agentId = HashUtil.decodeIdToLong(agentHashId)
        return (redisOperation.getSetMembers(LOCK_UPGRADE_AGENT_SET_KEY)
            ?: setOf()).filter { !it.isBlank() }.contains(agentId.toString())
    }

    fun setLockUpgradeAgents(agentIds: List<Long>) {
        agentIds.forEach {
            redisOperation.addSetValue(LOCK_UPGRADE_AGENT_SET_KEY, it.toString())
        }
    }

    fun unsetLockUpgradeAgents(agentIds: List<Long>) {
        agentIds.forEach {
            redisOperation.removeSetMember(LOCK_UPGRADE_AGENT_SET_KEY, it.toString())
        }
    }

    fun getAllLockUpgradeAgents(): List<Long> {
        return (redisOperation.getSetMembers(LOCK_UPGRADE_AGENT_SET_KEY)
            ?: setOf()).filter { !it.isBlank() }.map { it.toLong() }
    }

    fun cleanAllLockUpgradeAgents() {
        val allIds = redisOperation.getSetMembers(LOCK_UPGRADE_AGENT_SET_KEY) ?: return
        allIds.forEach {
            redisOperation.removeSetMember(LOCK_UPGRADE_AGENT_SET_KEY, it)
        }
    }

    fun setCanUpgradeAgents(agentIds: List<Long>) {
        val existingAgentIds = (redisOperation.getSetMembers(CAN_UPGRADE_AGENT_SET_KEY) ?: setOf()).map {
            it.toLong()
        }
        val newAgentIds = agentIds.toSet()
        val toAddAgentIds = newAgentIds.filterNot { existingAgentIds.contains(it) }
        if (toAddAgentIds.isNotEmpty()) {
            toAddAgentIds.forEach {
                redisOperation.addSetValue(CAN_UPGRADE_AGENT_SET_KEY, it.toString())
            }
        }
        val toDeleteAgents = existingAgentIds.filterNot { newAgentIds.contains(it) }
        if (toDeleteAgents.isNotEmpty()) {
            toDeleteAgents.forEach {
                redisOperation.removeSetMember(CAN_UPGRADE_AGENT_SET_KEY, it.toString())
            }
        }
    }

    fun getCanUpgradeAgents(): List<Long> {
        return (redisOperation.getSetMembers(CAN_UPGRADE_AGENT_SET_KEY)
            ?: setOf()).filter { !it.isBlank() }.map { it.toLong() }
    }

    fun getAgentMasterVersionKey(): String {
        return if (gray.isGray()) {
            GRAY_CURRENT_AGENT_MASTERT_VERSION
        } else {
            CURRENT_AGENT_MASTER_VERSION
        }
    }

    fun getAgentVersionKey(): String {
        return if (gray.isGray()) {
            GRAY_CURRENT_AGENT_VERSION
        } else {
            CURRENT_AGENT_VERSION
        }
    }
}