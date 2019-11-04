/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.environment.service

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.environment.agent.client.EsbAgentClient
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.ProjectConfigDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.CcNode
import com.tencent.devops.environment.pojo.CmdbNode
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.utils.ImportServerNodeUtils
import com.tencent.devops.environment.utils.NodeStringIdUtils
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CmdbNodeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao,
    private val projectConfigDao: ProjectConfigDao,
    private val redisOperation: RedisOperation,
    private val esbAgentClient: EsbAgentClient,
    private val environmentPermissionService: EnvironmentPermissionService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CmdbNodeService::class.java)
    }

    fun getUserCmdbNodes(userId: String, offset: Int, limit: Int): List<CmdbNode> {
        val cmdbNodes =
            ImportServerNodeUtils.getUserCmdbNode(
                esbAgentClient = esbAgentClient,
                redisOperation = redisOperation,
                userId = userId,
                offset = offset,
                limit = limit
            )
        return cmdbNodes.map {
            CmdbNode(it.name, it.operator, it.bakOperator, it.ip, it.displayIp, it.agentStatus, it.osName)
        }
    }

    fun getUserCmdbNodesNew(
        userId: String,
        bakOperator: Boolean,
        page: Int?,
        pageSize: Int?,
        ips: List<String>
    ): Page<CmdbNode> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 1000
        val sqlLimit =
            if (pageSizeNotNull != -1) PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull) else null
        val offset = sqlLimit?.offset ?: 0
        val limit = sqlLimit?.limit ?: 1000

        val cmdbNodePage =
            ImportServerNodeUtils.getUserCmdbNodeNew(
                esbAgentClient = esbAgentClient,
                redisOperation = redisOperation,
                userId = userId,
                bakOperator = bakOperator,
                ips = ips,
                offset = offset,
                limit = limit
            )
        return Page(
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            count = cmdbNodePage.totalRows.toLong(),
            records = cmdbNodePage.nodes.map {
                CmdbNode(it.name, it.operator, it.bakOperator, it.ip, it.displayIp, it.agentStatus, it.osName)
            }
        )
    }

    fun getUserCcNodes(userId: String): List<CcNode> {
        val ccNodes = ImportServerNodeUtils.getUserCcNode(esbAgentClient, redisOperation, userId)
        return ccNodes.map {
            CcNode(it.name, it.assetID, it.operator, it.bakOperator, it.ip, it.displayIp, it.agentStatus, it.osName)
        }
    }

    fun addCmdbNodes(userId: String, projectId: String, nodeIps: List<String>) {
        // 验证 CMDB 节点IP和责任人
        val cmdbNodeList = esbAgentClient.getCmdbNodeByIps(userId, nodeIps).nodes
        val cmdbIpToNodeMap = cmdbNodeList.associateBy { it.ip }
        val invaliedIps = nodeIps.filter {
            if (!cmdbIpToNodeMap.containsKey(it)) true
            else {
                val isOperator = cmdbIpToNodeMap[it]!!.operator == userId
                val isBakOpertor = cmdbIpToNodeMap[it]!!.bakOperator.split(";").contains(userId)
                !isOperator && !isBakOpertor
            }
        }
        if (invaliedIps.isNotEmpty()) {
            throw OperationException("非法 IP [${invaliedIps.joinToString(",")}], 请确认是否是服务器的责任人")
        }

        // 只添加不存在的节点
        val existNodeList = nodeDao.listServerAndDevCloudNodes(dslContext, projectId)
        val existIpList = existNodeList.map { it.nodeIp }.toSet()
        val toAddIpList = nodeIps.filterNot { existIpList.contains(it) }.toSet()
        ImportServerNodeUtils.checkImportCount(
            esbAgentClient = esbAgentClient,
            dslContext = dslContext,
            projectConfigDao = projectConfigDao,
            nodeDao = nodeDao,
            projectId = projectId,
            userId = userId,
            toAddNodeCount = toAddIpList.size
        )

        val now = LocalDateTime.now()
        val agentStatusMap = esbAgentClient.getAgentStatus(userId, toAddIpList)
        val toAddNodeList = toAddIpList.map {
            val cmdbNode = cmdbIpToNodeMap[it]!!
            TNodeRecord(
                null,
                "",
                projectId,
                cmdbNode.ip,
                cmdbNode.name,
                NodeStatus.NORMAL.name,
                NodeType.CMDB.name,
                null,
                null,
                userId,
                now,
                null,
                cmdbNode.osName,
                cmdbNode.operator,
                cmdbNode.bakOperator,
                agentStatusMap[cmdbNode.ip] ?: false,
                "",
                "",
                null,
                now,
                userId
            )
        }

        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            nodeDao.batchAddNode(context, toAddNodeList)
            val insertedNodeList = nodeDao.listServerNodesByIps(context, projectId, toAddNodeList.map { it.nodeIp })
            batchRegisterNodePermission(insertedNodeList = insertedNodeList, userId = userId, projectId = projectId)
        }
    }

    fun addCcNodes(userId: String, projectId: String, nodeIps: List<String>) {
        val ccNodeList = esbAgentClient.getCcNodeByIps(userId, nodeIps)
        val ccIpToNodeMap = ccNodeList.associateBy { it.ip }
        val invalidIps = nodeIps.filter {
            var ccNode = ccIpToNodeMap[it]
            if (ccNode == null) true
            else userId != ccNode.operator && userId != ccNode.bakOperator
        }
        if (invalidIps.isNotEmpty()) {
            throw OperationException("非法 IP [${invalidIps.joinToString(",")}], 请确认是否是服务器的责任人")
        }

        // 只添加不存在的节点
        val existNodeList = nodeDao.listServerAndDevCloudNodes(dslContext, projectId)
        val existIpList = existNodeList.map { it.nodeIp }.toSet()
        val toAddIpList = nodeIps.filterNot { existIpList.contains(it) }.toSet()
        ImportServerNodeUtils.checkImportCount(
            esbAgentClient = esbAgentClient,
            dslContext = dslContext,
            projectConfigDao = projectConfigDao,
            nodeDao = nodeDao,
            projectId = projectId,
            userId = userId,
            toAddNodeCount = toAddIpList.size
        )

        val now = LocalDateTime.now()
        val agentStatusMap = esbAgentClient.getAgentStatus(userId, toAddIpList)
        val toAddNodeList = nodeIps.filterNot { existIpList.contains(it) }.map {
            val ccNode = ccIpToNodeMap[it]!!
            TNodeRecord(
                null,
                "",
                projectId,
                ccNode.ip,
                ccNode.name,
                NodeStatus.NORMAL.name,
                NodeType.CC.name,
                null,
                null,
                userId,
                now,
                null,
                ccNode.osName,
                ccNode.operator,
                ccNode.bakOperator,
                agentStatusMap[ccNode.ip] ?: false,
                "",
                "",
                null,
                now,
                userId
            )
        }

        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            nodeDao.batchAddNode(context, toAddNodeList)
            val insertedNodeList = nodeDao.listServerNodesByIps(context, projectId, toAddNodeList.map { it.nodeIp })
            batchRegisterNodePermission(insertedNodeList = insertedNodeList, userId = userId, projectId = projectId)
        }
    }

    private fun batchRegisterNodePermission(
        insertedNodeList: List<TNodeRecord>,
        userId: String,
        projectId: String
    ) {
        insertedNodeList.forEach {
            environmentPermissionService.createNode(
                userId = userId,
                projectId = projectId,
                nodeId = it.nodeId,
                nodeName = "${NodeStringIdUtils.getNodeStringId(it)}(${it.nodeIp})"
            )
        }
    }
}