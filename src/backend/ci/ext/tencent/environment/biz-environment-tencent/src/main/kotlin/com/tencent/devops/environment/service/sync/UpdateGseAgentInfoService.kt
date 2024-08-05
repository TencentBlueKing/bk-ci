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

package com.tencent.devops.environment.service.sync

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.environment.constant.T_NODE_AGENT_VERSION
import com.tencent.devops.environment.constant.T_NODE_HOST_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_IP
import com.tencent.devops.environment.constant.T_NODE_NODE_STATUS
import com.tencent.devops.environment.constant.T_NODE_SERVER_ID
import com.tencent.devops.environment.dao.job.CmdbNodeDao
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.job.AgentVersion
import com.tencent.devops.environment.pojo.job.UpdateTNodeInfo
import com.tencent.devops.environment.service.gseagent.GSEAgentService
import com.tencent.devops.environment.service.job.QueryAgentStatusService
import com.tencent.devops.environment.utils.ComputeTimeUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service("UpdateGseAgentInfoService")
@Primary
class UpdateGseAgentInfoService @Autowired constructor(
    private val dslContext: DSLContext,
    private val cmdbNodeDao: CmdbNodeDao,
    private val queryAgentStatusService: QueryAgentStatusService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UpdateGseAgentInfoService::class.java)
        private const val DEFAULT_PAGE_SIZE = 100
        const val AGENT_NORMAL_NODE_STATUS = 1
    }

    fun updateGseAgentStatusAndVersion() {
        val startTime = LocalDateTime.now()
        val countCmdbNodes = cmdbNodeDao.countCmdbNodes(dslContext)
        logger.info("Update gse agent, node(s) quantity: $countCmdbNodes.")
        countCmdbNodes.takeIf { it > 0 }?.run {
            val totalPages = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countCmdbNodes.toLong())
            for (page in 1..totalPages) {
                val cmdbNodesRecords = cmdbNodeDao.getCmdbNodes(dslContext, page, DEFAULT_PAGE_SIZE)
                val existNodeIdToAgentVersionMap = cmdbNodesRecords.associate {
                    it[T_NODE_NODE_ID] as Long to
                        AgentVersion(
                            serverId = it[T_NODE_SERVER_ID] as? Long,
                            ip = it[T_NODE_NODE_IP] as? String,
                            bkHostId = it[T_NODE_HOST_ID] as? Long,
                            installedTag = NodeStatus.NOT_INSTALLED.name != it[T_NODE_NODE_STATUS] as String,
                            version = it[T_NODE_AGENT_VERSION] as? String,
                            status = if (NodeStatus.NORMAL.name == it[T_NODE_NODE_STATUS] as String) 1 else 0
                        )
                }
                val existAgentVersionList = existNodeIdToAgentVersionMap.values.toList()
                if (logger.isDebugEnabled)
                    logger.debug(
                        "[updateGseAgent]existAgentVersionList:" +
                            existAgentVersionList.joinToString(separator = ", ", transform = { it.toString() })
                    )
                val hostIdToExistAgentVersion = existAgentVersionList.groupBy { it.bkHostId }
                val newAgentVersionList = queryAgentStatusService.getAgentVersions(existAgentVersionList)
                if (logger.isDebugEnabled)
                    logger.debug(
                        "[updateGseAgent]newAgentVersionList:" +
                            newAgentVersionList?.joinToString(separator = ", ", transform = { it.toString() })
                    )
                // 判断 newAgentVersionList 和 existAgentVersionList 是否一致，不一致则更新对应数据库表
                val agentUpdateList = newAgentVersionList?.filterNot {
                    hostIdToExistAgentVersion[it.bkHostId]?.all { item ->
                        // 1. 判断一个分组（同一个hostId）中的AgentVersion是否相同，不同则统一重新重新写入
                        item.installedTag == hostIdToExistAgentVersion[it.bkHostId]?.get(0)?.installedTag &&
                            item.version == hostIdToExistAgentVersion[it.bkHostId]?.get(0)?.version &&
                            item.status == hostIdToExistAgentVersion[it.bkHostId]?.get(0)?.status
                    } ?: false && (
                        // 2. 这个分组（同一个hostId）的AgentVersion相同，判断第一个元素的值，新查的和db中的是否相同，不同则更新
                        it.installedTag == hostIdToExistAgentVersion[it.bkHostId]?.get(0)?.installedTag &&
                            it.version == hostIdToExistAgentVersion[it.bkHostId]?.get(0)?.version &&
                            it.status == hostIdToExistAgentVersion[it.bkHostId]?.get(0)?.status
                        )
                }
                logger.info(
                    "[updateGseAgent]agentUpdateList:" +
                        agentUpdateList?.joinToString(separator = ", ", transform = { it.toString() })
                )
                if (!agentUpdateList.isNullOrEmpty()) {
                    batchUpdateAgent(existNodeIdToAgentVersionMap, agentUpdateList)
                }
            }
        }
        logger.info(
            "[updateGseAgent]total time: ${ComputeTimeUtils.calculateDuration(startTime, LocalDateTime.now())}s, "
        )
    }

    private fun batchUpdateAgent(
        existNodeIdToAgentVersionMap: Map<Long, AgentVersion>,
        agentUpdateList: List<AgentVersion>
    ) {
        val hostIdToAgentUpdateList = agentUpdateList.associateBy { it.bkHostId }
        val agentUpdateIpList = agentUpdateList.mapNotNull { it.ip }
        val agentUpdateHostIdList = agentUpdateList.mapNotNull { it.bkHostId }
        val agentUpdateRecords = existNodeIdToAgentVersionMap.filter { (_, value) ->
            agentUpdateIpList.contains(value.ip) || agentUpdateHostIdList.contains(value.bkHostId)
        }.map { (key, value) ->
            UpdateTNodeInfo(
                nodeId = key,
                nodeStatus = getNodeStatus(hostIdToAgentUpdateList[value.bkHostId]),
                agentStatus = AGENT_NORMAL_NODE_STATUS == hostIdToAgentUpdateList[value.bkHostId]?.status,
                agentVersion = hostIdToAgentUpdateList[value.bkHostId]?.version,
                lastModifyTime = LocalDateTime.now()
            )
        }
        if (logger.isDebugEnabled)
            logger.debug(
                "[batchUpdateAgent]agentUpdateRecords:" +
                    agentUpdateRecords.joinToString(separator = ", ", transform = { it.toString() })
            )
        cmdbNodeDao.batchUpdateAgentInfo(dslContext, agentUpdateRecords)
    }

    private fun getNodeStatus(agentInfo: AgentVersion?): String {
        return if (GSEAgentService.AGENT_NOT_INSTALLED_TAG == agentInfo?.installedTag)
            NodeStatus.NOT_INSTALLED.name
        else if (GSEAgentService.AGENT_ABNORMAL_NODE_STATUS == agentInfo?.status)
            NodeStatus.ABNORMAL.name
        else if (GSEAgentService.AGENT_NORMAL_NODE_STATUS == agentInfo?.status)
            NodeStatus.NORMAL.name
        else
            NodeStatus.NOT_INSTALLED.name
    }
}
