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

package com.tencent.devops.common.environment.agent

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.agent.AgentArchType
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory

@SuppressWarnings("TooManyFunctions")
class AgentGrayUtils constructor(
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AgentGrayUtils::class.java)

        private const val CURRENT_AGENT_MASTER_VERSION = "environment.thirdparty.agent.master.version"

        private const val CURRENT_AGENT_VERSION = "environment.thirdparty.agent.verison"

        private const val CURRENT_AGENT_WINDOWS_386_JDK_VERSION = "environment.thirdparty.agent.win_386_jdk.verison"
        private const val CURRENT_AGENT_MACOS_AMD64_JDK_VERSION = "environment.thirdparty.agent.mac_amd64_jdk.verison"
        private const val CURRENT_AGENT_MACOS_ARM64_JDK_VERSION = "environment.thirdparty.agent.mac_arm64_jdk.verison"
        private const val CURRENT_AGENT_LINUX_AMD64_JDK_VERSION = "environment.thirdparty.agent.linux_amd64_jdk.verison"
        private const val CURRENT_AGENT_LINUX_ARM64_JDK_VERSION = "environment.thirdparty.agent.linux_arm64_jdk.verison"
        private const val CURRENT_AGENT_LINUX_MIPS64_JDK_VERSION =
            "environment.thirdparty.agent.linux_mips64_jdk.verison"

        private const val CAN_UPGRADE_AGENT_SET_KEY = "environment:thirdparty:can_upgrade"

        private const val LOCK_UPGRADE_AGENT_SET_KEY = "environment:thirdparty:lock_upgrade"

        private const val LOCK_UPGRADE_AGENT_WORKER_SET_KEY = "environment:thirdparty:worker:lock_upgrade"
        private const val LOCK_UPGRADE_AGENT_GO_SET_KEY = "environment:thirdparty:goagent:lock_upgrade"
        private const val LOCK_UPGRADE_AGENT_JDK_SET_KEY = "environment:thirdparty:jdk:lock_upgrade"

        private const val FORCE_UPGRADE_AGENT_SET_KEY = "environment:thirdparty:force_upgrade"

        private const val FORCE_UPGRADE_AGENT_WORKER_SET_KEY = "environment:thirdparty:worker:force_upgrade"
        private const val FORCE_UPGRADE_AGENT_GO_SET_KEY = "environment:thirdparty:goagent:force_upgrade"
        private const val FORCE_UPGRADE_AGENT_JDK_SET_KEY = "environment:thirdparty:jdk:force_upgrade"

        private const val DEFAULT_GATEWAY_KEY = "environment:thirdparty:default_gateway"
        private const val DEFAULT_FILE_GATEWAY_KEY = "environment:thirdparty:default_file_gateway"
        private const val USE_DEFAULT_GATEWAY_KEY = "environment:thirdparty:use_default_gateway"
        private const val USE_DEFAULT_FILE_GATEWAY_KEY = "environment:thirdparty:use_default_file_gateway"
        private const val PARALLEL_UPGRADE_COUNT = "environment.thirdparty.agent.parallel.upgrade.count"
        private const val DEFAULT_PARALLEL_UPGRADE_COUNT = 50
    }

    fun checkForceUpgrade(agentHashId: String, type: AgentUpgradeType?): Boolean {
        val agentId = HashUtil.decodeIdToLong(agentHashId)
        return (redisOperation.getSetMembers(getForceKeyByType(type)) ?: setOf()).contains(agentId.toString())
    }

    fun setForceUpgradeAgents(agentIds: List<Long>, type: AgentUpgradeType?) {
        agentIds.forEach {
            redisOperation.addSetValue(getForceKeyByType(type), it.toString())
        }
    }

    fun unsetForceUpgradeAgents(agentIds: List<Long>, type: AgentUpgradeType?) {
        agentIds.forEach {
            redisOperation.removeSetMember(getForceKeyByType(type), it.toString())
        }
    }

    fun getAllForceUpgradeAgents(type: AgentUpgradeType?): List<Long> {
        return (redisOperation.getSetMembers(getForceKeyByType(type))
            ?: setOf()).filter { it.isNotBlank() }.map { it.toLong() }
    }

    fun cleanAllForceUpgradeAgents(type: AgentUpgradeType?) {
        val allIds = redisOperation.getSetMembers(getForceKeyByType(type)) ?: return
        allIds.forEach {
            redisOperation.removeSetMember(getForceKeyByType(type), it)
        }
    }

    private fun getForceKeyByType(type: AgentUpgradeType?): String {
        if (type == null) {
            return FORCE_UPGRADE_AGENT_SET_KEY
        }
        return when (type) {
            AgentUpgradeType.WORKER -> FORCE_UPGRADE_AGENT_WORKER_SET_KEY
            AgentUpgradeType.GO_AGENT -> FORCE_UPGRADE_AGENT_GO_SET_KEY
            AgentUpgradeType.JDK -> FORCE_UPGRADE_AGENT_JDK_SET_KEY
        }
    }

    fun checkLockUpgrade(agentHashId: String, type: AgentUpgradeType?): Boolean {
        val agentId = HashUtil.decodeIdToLong(agentHashId)
        return (redisOperation.getSetMembers(getLockKeyByType(type))
            ?: setOf()).filter { it.isNotBlank() }.contains(agentId.toString())
    }

    fun setLockUpgradeAgents(agentIds: List<Long>, type: AgentUpgradeType?) {
        agentIds.forEach {
            redisOperation.addSetValue(getLockKeyByType(type), it.toString())
        }
    }

    fun unsetLockUpgradeAgents(agentIds: List<Long>, type: AgentUpgradeType?) {
        agentIds.forEach {
            redisOperation.removeSetMember(getLockKeyByType(type), it.toString())
        }
    }

    fun getAllLockUpgradeAgents(type: AgentUpgradeType?): List<Long> {
        return (redisOperation.getSetMembers(getLockKeyByType(type))
            ?: setOf()).filter { it.isNotBlank() }.map { it.toLong() }
    }

    fun cleanAllLockUpgradeAgents(type: AgentUpgradeType?) {
        val allIds = redisOperation.getSetMembers(getLockKeyByType(type)) ?: return
        allIds.forEach {
            redisOperation.removeSetMember(getLockKeyByType(type), it)
        }
    }

    private fun getLockKeyByType(type: AgentUpgradeType?): String {
        if (type == null) {
            return LOCK_UPGRADE_AGENT_SET_KEY
        }
        return when (type) {
            AgentUpgradeType.WORKER -> LOCK_UPGRADE_AGENT_WORKER_SET_KEY
            AgentUpgradeType.GO_AGENT -> LOCK_UPGRADE_AGENT_GO_SET_KEY
            AgentUpgradeType.JDK -> LOCK_UPGRADE_AGENT_JDK_SET_KEY
        }
    }

    fun setCanUpgradeAgents(agentIds: List<Long>) {
        logger.info("setCanUpgradeAgents, agentIds: $agentIds")
        val canUpgradeAgentSetKey = getCanUpgradeAgentSetKey()
        val existingAgentIds = (redisOperation.getSetMembers(
            key = canUpgradeAgentSetKey,
            isDistinguishCluster = true
        ) ?: setOf()).map {
            it.toLong()
        }
        val newAgentIds = agentIds.toSet()
        val toAddAgentIds = newAgentIds.filterNot { existingAgentIds.contains(it) }
        if (toAddAgentIds.isNotEmpty()) {
            toAddAgentIds.forEach {
                redisOperation.addSetValue(
                    key = canUpgradeAgentSetKey,
                    item = it.toString(),
                    isDistinguishCluster = true
                )
            }
        }
        val toDeleteAgents = existingAgentIds.filterNot { newAgentIds.contains(it) }
        if (toDeleteAgents.isNotEmpty()) {
            toDeleteAgents.forEach {
                redisOperation.removeSetMember(
                    key = canUpgradeAgentSetKey,
                    item = it.toString(),
                    isDistinguishCluster = true
                )
            }
        }
    }

    private fun getCanUpgradeAgentSetKey(): String {
        return CAN_UPGRADE_AGENT_SET_KEY
    }

    fun getCanUpgradeAgents(): List<Long> {
        return (redisOperation.getSetMembers(
            key = getCanUpgradeAgentSetKey(),
            isDistinguishCluster = true
        ) ?: setOf()).filter { it.isNotBlank() }.map { it.toLong() }
    }

    fun getAgentMasterVersionKey(): String {
        return CURRENT_AGENT_MASTER_VERSION
    }

    fun getAgentVersionKey(): String {
        return CURRENT_AGENT_VERSION
    }

    fun getJdkVersionKey(os: OS, arch: AgentArchType): String {
        return when (os) {
            OS.WINDOWS -> CURRENT_AGENT_WINDOWS_386_JDK_VERSION
            OS.MACOS -> if (arch == AgentArchType.ARM64) {
                CURRENT_AGENT_MACOS_ARM64_JDK_VERSION
            } else {
                CURRENT_AGENT_MACOS_AMD64_JDK_VERSION
            }

            OS.LINUX -> when (arch) {
                AgentArchType.ARM64 -> CURRENT_AGENT_LINUX_ARM64_JDK_VERSION
                AgentArchType.MIPS64 -> CURRENT_AGENT_LINUX_MIPS64_JDK_VERSION
                AgentArchType.AMD64 -> CURRENT_AGENT_LINUX_AMD64_JDK_VERSION
            }
        }
    }

    fun getDefaultGateway(): String? {
        return redisOperation.get(DEFAULT_GATEWAY_KEY)
    }

    fun getDefaultFileGateway(): String? {
        return redisOperation.get(DEFAULT_FILE_GATEWAY_KEY)
    }

    fun useDefaultGateway(): Boolean {
        return redisOperation.get(USE_DEFAULT_GATEWAY_KEY) == "true"
    }

    fun useDefaultFileGateway(): Boolean {
        return redisOperation.get(USE_DEFAULT_FILE_GATEWAY_KEY) == "true"
    }

    fun setMaxParallelUpgradeCount(count: Int) {
        redisOperation.set(PARALLEL_UPGRADE_COUNT, count.toString())
    }

    fun getMaxParallelUpgradeCount(): Int {
        return redisOperation.get(PARALLEL_UPGRADE_COUNT)?.toInt() ?: DEFAULT_PARALLEL_UPGRADE_COUNT
    }

    fun getParallelUpgradeCountKey(): String {
        return PARALLEL_UPGRADE_COUNT
    }

    fun getDefaultParallelUpgradeCount(): Int {
        return DEFAULT_PARALLEL_UPGRADE_COUNT
    }
}
