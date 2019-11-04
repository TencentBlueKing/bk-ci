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
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.ProjectConfigDao
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.NodeDevCloudInfo
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import com.tencent.devops.environment.utils.NodeStringIdUtils
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OpNodeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao,
    private val envNodeDao: EnvNodeDao,
    private val projectConfigDao: ProjectConfigDao,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val slaveGatewayService: SlaveGatewayService,
    private val environmentPermissionService: EnvironmentPermissionService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(OpNodeService::class.java)
    }

    fun flushDisplayName(): Int {
        logger.info("Start to flush the node display name")
        val nodes = nodeDao.listAllNodes(dslContext)
        var updateCnt = 0
        nodes.forEach {
            if (it.displayName.isNullOrBlank()) {
                val nodeStringId = NodeStringIdUtils.getNodeStringId(it)
                logger.info("[${it.nodeId}|${it.nodeName}|${it.nodeType}|$nodeStringId] Start to flush node display name")
                val count = nodeDao.updateDisplayName(
                    dslContext = dslContext,
                    nodeId = it.nodeId,
                    nodeName = nodeStringId,
                    userId = "system"
                )
                if (count != 1) {
                    logger.warn("[${it.nodeId}|${it.nodeName}|${it.nodeType}|$nodeStringId] Fail to update the node display name - $count")
                    return@forEach
                }
                updateCnt++
            }
        }
        logger.info("Finish flushing the node display name - $updateCnt")
        return updateCnt
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

    fun addOtherNodes(userId: String, projectId: String, nodeIps: List<String>) {
        logger.info("addOtherNodes, userId: $userId, projectId: $projectId, nodeIps: $nodeIps")

        // 只添加不存在的节点
        val existNodeList = nodeDao.listServerAndDevCloudNodes(dslContext, projectId)
        val existIpList = existNodeList.map { it.nodeIp }.toSet()
        val toAddIpList = nodeIps.filterNot { existIpList.contains(it) }.toSet()
        logger.info("toAddIpList: $toAddIpList")

        checkImportCount(
            dslContext = dslContext,
            projectConfigDao = projectConfigDao,
            nodeDao = nodeDao,
            projectId = projectId,
            userId = userId,
            toAddNodeCount = toAddIpList.size
        )

        val now = LocalDateTime.now()
        val toAddNodeList = toAddIpList.map {
            TNodeRecord(
                null,
                "",
                projectId,
                it,
                it,
                NodeStatus.NORMAL.name,
                NodeType.OTHER.name,
                null,
                null,
                userId,
                now,
                null,
                "",
                "",
                "",
                false,
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

    fun listPage(page: Int, pageSize: Int, nodeName: String?): List<NodeDevCloudInfo> {
        return nodeDao.listPage(dslContext, page, pageSize, nodeName).map {
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
        return nodeDao.count(dslContext, nodeName)
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

    private fun checkImportCount(dslContext: DSLContext, projectConfigDao: ProjectConfigDao, nodeDao: NodeDao, projectId: String, userId: String, toAddNodeCount: Int) {
        val projectConfig = projectConfigDao.get(dslContext, projectId, userId)
        val importQuata = projectConfig.importQuota
        val existImportNodeCount = nodeDao.countImportNode(dslContext, projectId)
        if (toAddNodeCount + existImportNodeCount > importQuata) {
            throw OperationException("导入CC/CMDB节点数不能超过配额[$importQuata]，如有特别需求，请联系【蓝盾助手】")
        }
    }
}