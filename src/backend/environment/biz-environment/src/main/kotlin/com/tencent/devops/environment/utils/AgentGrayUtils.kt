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
        private const val UPGRADE_PROJECT_KEY_PREFIX = "thirdparty.agent.upgrade.project_"
        private const val FORCE_UPGRADE_AGENT_PREFIX = "thirdparty.agent.force.upgrade.id_"
    }

    private fun getProjectUpgradeKey(projectId: String): String {
        return "$UPGRADE_PROJECT_KEY_PREFIX$projectId"
    }

    private fun getForceUpgradeAgentKey(agentId: Long): String {
        return "$FORCE_UPGRADE_AGENT_PREFIX$agentId"
    }

    fun checkUpgradeProject(projectId: String): Boolean {
        val needUpgrade = !redisOperation.get(getProjectUpgradeKey(projectId)).isNullOrBlank()
        logger.info("get upgrade project($projectId): $needUpgrade")
        return needUpgrade
    }

    fun setUpgradeProjects(projectIds: List<String>) {
        logger.info("set upgrade projects: $projectIds")
        projectIds.forEach {
            redisOperation.set(getProjectUpgradeKey(it), "true")
        }
    }

    fun unsetUpgradeProjects(projectIds: List<String>) {
        logger.info("unset upgrade projects: $projectIds")
        val keys = projectIds.map { getProjectUpgradeKey(it) }
        redisOperation.delete(keys)
    }

    fun getAllUpgradeProjects(): List<String> {
        val prefixLength = UPGRADE_PROJECT_KEY_PREFIX.length
        val projects = redisOperation.keys("$UPGRADE_PROJECT_KEY_PREFIX*")
            .map { it.substring(prefixLength) }
        logger.info("all upgrade projects: $projects")
        return projects
    }

    fun cleanAllUpgradeProjects() {
        val allProjectKeys = redisOperation.keys("$UPGRADE_PROJECT_KEY_PREFIX*")
        logger.info("clean all upgrade projects, keys: $allProjectKeys")
        if (allProjectKeys.isNotEmpty()) {
            redisOperation.delete(allProjectKeys)
        }
    }

    fun checkForceUpgrade(agentHashId: String): Boolean {
        val agentId = HashUtil.decodeIdToLong(agentHashId)
        val needUpgrade = !redisOperation.get(getForceUpgradeAgentKey(agentId)).isNullOrBlank()
        logger.info("get agent force upgrade($agentId): $needUpgrade")
        return needUpgrade
    }

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
}