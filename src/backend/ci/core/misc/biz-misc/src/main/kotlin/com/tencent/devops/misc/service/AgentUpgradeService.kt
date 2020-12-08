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

package com.tencent.devops.misc.service

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.environment.agent.AgentGrayUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.misc.dao.EnvironmentThirdPartyAgentDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AgentUpgradeService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val agentGrayUtils: AgentGrayUtils,
    private val dslContext: DSLContext,
    private val environmentThirdPartyAgentDao: EnvironmentThirdPartyAgentDao,
    private val gray: Gray
) {

    fun updateCanUpgradeAgentList() {
        val maxParallelCount = redisOperation.get(PARALLEL_UPGRADE_COUNT)?.toInt() ?: DEFAULT_PARALLEL_UPGRADE_COUNT
        if (maxParallelCount < 1) {
            logger.warn("parallel count set to zero")
            agentGrayUtils.setCanUpgradeAgents(listOf())
            return
        }

        val currentVersion = redisOperation.get(agentGrayUtils.getAgentVersionKey())
        val currentMasterVersion = redisOperation.get(agentGrayUtils.getAgentMasterVersionKey())
        if (currentMasterVersion.isNullOrBlank() || currentVersion.isNullOrBlank()) {
            logger.warn("invalid server agent version")
            return
        }

        val grayProjects = gray.grayProjectSet(redisOperation)
        val gray = gray.isGray()
        val importOKAgents = environmentThirdPartyAgentDao.listByStatus(dslContext, setOf(AgentStatus.IMPORT_OK)).toSet()
        val needUpgradeAgents = importOKAgents.filter {
            when {
                it.version.isNullOrBlank() || it.masterVersion.isNullOrBlank() -> false
                gray && grayProjects.contains(it.projectId) -> {
                    it.version != currentVersion || it.masterVersion != currentMasterVersion
                }
                !gray && !grayProjects.contains(it.projectId) -> {
                    it.version != currentVersion || it.masterVersion != currentMasterVersion
                }
                else -> false
            }
        }
        val canUpgraderAgent = if (needUpgradeAgents.size > maxParallelCount) {
            needUpgradeAgents.subList(0, maxParallelCount)
        } else {
            needUpgradeAgents
        }
        agentGrayUtils.setCanUpgradeAgents(canUpgraderAgent.map { it.id })
    }

    fun setMaxParallelUpgradeCount(count: Int) {
        redisOperation.set(PARALLEL_UPGRADE_COUNT, count.toString())
    }

    fun getMaxParallelUpgradeCount(): Int {
        return redisOperation.get(PARALLEL_UPGRADE_COUNT)?.toInt() ?: DEFAULT_PARALLEL_UPGRADE_COUNT
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AgentUpgradeService::class.java)
        private const val PARALLEL_UPGRADE_COUNT = "environment.thirdparty.agent.parallel.upgrade.count"
        private const val DEFAULT_PARALLEL_UPGRADE_COUNT = 50
    }
}