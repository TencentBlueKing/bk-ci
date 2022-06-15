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

package com.tencent.devops.environment.service.thirdPartyAgent

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.environment.agent.AgentGrayUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentDao
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import com.tencent.devops.project.api.service.ServiceProjectTagResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AgentUpgradeService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val agentGrayUtils: AgentGrayUtils,
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val client: Client
) {

    fun updateCanUpgradeAgentList() {
        val maxParallelCount = redisOperation.get(
            key = agentGrayUtils.getParallelUpgradeCountKey()
        )?.toInt() ?: agentGrayUtils.getDefaultParallelUpgradeCount()
        if (maxParallelCount < 1) {
            logger.warn("parallel count set to zero")
            agentGrayUtils.setCanUpgradeAgents(listOf())
            return
        }

        val currentVersion = redisOperation.get(
            key = agentGrayUtils.getAgentVersionKey(),
            isDistinguishCluster = true
        )
        val currentMasterVersion = redisOperation.get(
            key = agentGrayUtils.getAgentMasterVersionKey(),
            isDistinguishCluster = true
        )
        if (currentMasterVersion.isNullOrBlank() || currentVersion.isNullOrBlank()) {
            logger.warn("invalid server agent version")
            return
        }

        val canUpgradeAgents = listCanUpdateAgents(
            currentVersion = currentVersion,
            currentMasterVersion = currentMasterVersion,
            maxParallelCount = maxParallelCount
        )

        if (canUpgradeAgents.isNotEmpty()) {
            agentGrayUtils.setCanUpgradeAgents(canUpgradeAgents.map { it.id })
        }
    }

    private fun listCanUpdateAgents(
        currentVersion: String?,
        currentMasterVersion: String?,
        maxParallelCount: Int
    ): List<TEnvironmentThirdpartyAgentRecord> {
        val importOKAgents = thirdPartyAgentDao.listByStatus(
            dslContext = dslContext,
            status = setOf(AgentStatus.IMPORT_OK)
        ).toSet()
        val needUpgradeAgents = importOKAgents.filter {
            when {
                // #5806 #5045 解决worker过老，或者异常，导致拿不到版本号，而无法自愈或升级的问题
                // it.version.isNullOrBlank() || it.masterVersion.isNullOrBlank() -> false
                checkProjectRouter(it.projectId) ->
                    it.version != currentVersion || it.masterVersion != currentMasterVersion
                else -> false
            }
        }
        return if (needUpgradeAgents.size > maxParallelCount) {
            needUpgradeAgents.subList(0, maxParallelCount)
        } else {
            needUpgradeAgents
        }
    }

    private fun checkProjectRouter(projectId: String): Boolean {
        return client.get(ServiceProjectTagResource::class).checkProjectRouter(projectId).data ?: false
    }

    fun setMaxParallelUpgradeCount(count: Int) {
        agentGrayUtils.setMaxParallelUpgradeCount(count)
    }

    fun getMaxParallelUpgradeCount(): Int {
        return agentGrayUtils.getMaxParallelUpgradeCount()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AgentUpgradeService::class.java)
    }
}
