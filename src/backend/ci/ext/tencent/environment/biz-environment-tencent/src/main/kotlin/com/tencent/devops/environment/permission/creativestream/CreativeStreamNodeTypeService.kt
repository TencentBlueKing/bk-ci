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

package com.tencent.devops.environment.permission.creativestream

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.model.AgentProps
import com.tencent.devops.environment.pojo.enums.AgentType
import com.tencent.devops.environment.pojo.thirdpartyagent.create.AgentPropsSource
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class CreativeStreamNodeTypeService(
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao
) {
    // 同一节点的 agent 记录会被 supports/checkPermission 多次读取，批量列表时更会被反复命中，
    // 这里做短 TTL 缓存并支持批量预取，避免 getAgentByNodeId 查询放大。
    private val agentRecordCache: Cache<String, Optional<TEnvironmentThirdpartyAgentRecord>> =
        CacheBuilder.newBuilder()
            .maximumSize(AGENT_CACHE_MAX_SIZE)
            .expireAfterWrite(AGENT_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS)
            .build()

    /**
     * 批量预取一批节点的 agent 记录，仅用一次 IN 查询回填缓存，供后续逐节点判定复用。
     */
    fun prefetch(projectId: String, nodeIds: Collection<Long>) {
        val missing = nodeIds.filter { agentRecordCache.getIfPresent(cacheKey(projectId, it)) == null }
        if (missing.isEmpty()) {
            return
        }
        val recordByNodeId = thirdPartyAgentDao.getAgentsByNodeIds(dslContext, missing, projectId)
            .associateBy { it.nodeId }
        missing.forEach { nodeId ->
            agentRecordCache.put(cacheKey(projectId, nodeId), Optional.ofNullable(recordByNodeId[nodeId]))
        }
    }

    private fun getAgentRecord(projectId: String, nodeId: Long): TEnvironmentThirdpartyAgentRecord? {
        val key = cacheKey(projectId, nodeId)
        agentRecordCache.getIfPresent(key)?.let { return it.orElse(null) }
        val record = thirdPartyAgentDao.getAgentByNodeId(dslContext, nodeId, projectId)
        agentRecordCache.put(key, Optional.ofNullable(record))
        return record
    }

    private fun cacheKey(projectId: String, nodeId: Long) = "$projectId:$nodeId"

    fun getNodeType(projectId: String, nodeId: Long): CreativeStreamNodeType {
        val agentRecord = getAgentRecord(projectId, nodeId) ?: run {
            logger.warn("Creative stream node agent not found|projectId=$projectId|nodeId=$nodeId")
            return CreativeStreamNodeType.OTHER
        }

        if (agentRecord.agentType != AgentType.CREATE.name) {
            return CreativeStreamNodeType.OTHER
        }

        return when (AgentProps.getSourceFromRecord(agentRecord.agentProps, OS.parse(agentRecord.os))) {
            AgentPropsSource.DEVCLOUD -> CreativeStreamNodeType.IMATE

            AgentPropsSource.REMOTEDEV -> CreativeStreamNodeType.CLOUD_DESKTOP

            AgentPropsSource.IEG_IMATE -> CreativeStreamNodeType.OTHER
        }
    }

    fun getWorkspaceName(projectId: String, nodeId: Long): String? {
        val agentRecord = getAgentRecord(projectId, nodeId) ?: run {
            logger.warn("Creative stream node agent not found|projectId=$projectId|nodeId=$nodeId")
            return null
        }
        return agentRecord.createWorkspaceName
    }

    fun getCreatedUser(projectId: String, nodeId: Long): String? {
        return getAgentRecord(projectId, nodeId)?.createdUser
    }

    companion object {
        private const val AGENT_SOURCE_FIELD = "source"
        private const val AGENT_CACHE_MAX_SIZE = 10_000L
        private const val AGENT_CACHE_EXPIRE_SECONDS = 30L
        private val logger = LoggerFactory.getLogger(CreativeStreamNodeTypeService::class.java)
    }
}
