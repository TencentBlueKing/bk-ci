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

package com.tencent.devops.environment.service

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.dispatch.api.ServiceAgentResource
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.job.CmdbNodeDao
import com.tencent.devops.environment.dao.job.OPNodeDao
import com.tencent.devops.environment.pojo.NodeDevCloudInfo
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.utils.NodeStringIdUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class OpNodeService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao,
    private val cmdbNodeDao: CmdbNodeDao,
    private val opNodeDao: OPNodeDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(OpNodeService::class.java)
    }

    fun flushDisplayName(): Int {
        logger.info("Start to flush the node display name")
        val nodes = cmdbNodeDao.listAllNodes(dslContext)
        var updateCnt = 0
        nodes.forEach {
            if (it.displayName.isNullOrBlank()) {
                val nodeStringId = NodeStringIdUtils.getNodeStringId(it)
                logger.info(
                    "[${it.nodeId}|${it.nodeName}|${it.nodeType}|$nodeStringId]" +
                        " Start to flush node display name"
                )
                val count = nodeDao.updateDisplayName(
                    dslContext = dslContext,
                    nodeId = it.nodeId,
                    nodeName = nodeStringId,
                    userId = "system",
                    projectId = it.projectId
                )
                if (count != 1) {
                    logger.warn(
                        "[${it.nodeId}|${it.nodeName}|${it.nodeType}|$nodeStringId]" +
                            " Fail to update the node display name - $count"
                    )
                    return@forEach
                }
                updateCnt++
            }
        }
        logger.info("Finish flushing the node display name - $updateCnt")
        return updateCnt
    }

    fun flushLastBuildPipeline(): Int {
        var count = 0
        var page = 1
        while (true) {
            logger.info("Start to flush the last build pipeline, page $page")
            val limit = PageUtil.convertPageSizeToSQLLimit(page, 100)
            val nodes = opNodeDao.listAllNodes(dslContext, NodeType.THIRDPARTY, limit.limit, limit.offset)
            page += 1
            val thirdPartyAgentNodeIds = nodes.filter {
                it.lastBuildPipelineId.isNullOrBlank()
            }.map { it.nodeId }
            if (thirdPartyAgentNodeIds.isNotEmpty()) {
                val thirdPartyAgentMap = opNodeDao.getAgentsByNodeIds(dslContext, thirdPartyAgentNodeIds)
                    .associateBy({ HashUtil.encodeLongId(it.id) }, { it.nodeId })
                val agentBuilds = client.get(ServiceAgentResource::class).listLatestBuildPipelines(
                    agentIds = thirdPartyAgentMap.keys.toList()
                )
                agentBuilds.forEach {
                    opNodeDao.updateLastBuildTime(
                        dslContext = dslContext,
                        pipelineId = it.pipelineId,
                        nodeId = thirdPartyAgentMap[it.agentId] ?: return@forEach
                    )
                    count += 1
                    Thread.sleep(10)
                }
                Thread.sleep(100)
            }
            logger.info("Finish flushing the last build pipeline, page $page, count $count")
            if (nodes.size < limit.limit) {
                break
            }
        }
        return count
    }

    fun listPage(page: Int, pageSize: Int, nodeName: String?): List<NodeDevCloudInfo> {
        return cmdbNodeDao.listPage(dslContext, page, pageSize, nodeName).map {
            NodeDevCloudInfo(
                nodeHashId = HashUtil.encodeLongId(it.nodeId),
                nodeId = it.nodeId.toString(),
                name = it.nodeName,
                ip = it.nodeIp,
                nodeStatus = it.nodeStatus,
                agentStatus = it.agentStatus,
                nodeType = it.nodeType,
                osName = it.osName,
                createdUser = it.createdUser,
                projectId = it.projectId
            )
        }
    }

    fun countPage(nodeName: String?): Int {
        return cmdbNodeDao.countAllNodesOrByName(dslContext, nodeName)
    }

    /**
     *  仅删除node表，不做其他处理，用以OP系统清理数据
     */
    fun deleteNode(projectId: String, nodeHashId: String): Boolean {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        logger.info("deleteNode, projectId:$projectId, nodeId: $nodeId, nodeHashId: $nodeHashId")
        nodeDao.batchDeleteNode(dslContext, projectId, listOf(nodeId))
        return true
    }
}
