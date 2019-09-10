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

package com.tencent.devops.environment.service

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import com.tencent.devops.environment.utils.AgentStatusUtils.getAgentStatus
import com.tencent.devops.environment.utils.NodeStringIdUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import javax.ws.rs.NotFoundException

@Service
class NodeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao,
    private val envNodeDao: EnvNodeDao,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val slaveGatewayService: SlaveGatewayService,
    private val environmentPermissionService: EnvironmentPermissionService
) {
    fun deleteNodes(userId: String, projectId: String, nodeHashIds: List<String>) {
        val nodeLongIds = nodeHashIds.map { HashUtil.decodeIdToLong(it) }
        val canDeleteNodeIds =
            environmentPermissionService.listNodeByPermission(userId, projectId, BkAuthPermission.DELETE)
        val existNodeList = nodeDao.listByIds(dslContext, projectId, nodeLongIds)
        if (existNodeList.isEmpty()) {
            return
        }
        val existNodeIdList = existNodeList.map { it.nodeId }

        val unauthorizedNodeIds = existNodeIdList.filterNot { canDeleteNodeIds.contains(it) }
        if (unauthorizedNodeIds.isNotEmpty()) {
            throw OperationException("没有删除节点的权限，节点ID：[${unauthorizedNodeIds.map { HashUtil.encodeLongId(it) }.joinToString(",")}]")
        }

        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            nodeDao.batchDeleteNode(context, projectId, existNodeIdList)
            envNodeDao.deleteByNodeIds(context, existNodeIdList)
            existNodeIdList.forEach {
                environmentPermissionService.deleteNode(projectId, it)
            }
        }
    }

    fun hasCreatePermission(userId: String, projectId: String): Boolean {
        return environmentPermissionService.checkNodePermission(userId, projectId, BkAuthPermission.CREATE)
    }

    fun list(userId: String, projectId: String): List<NodeWithPermission> {
        val nodeRecordList = nodeDao.listNodes(dslContext, projectId)
        if (nodeRecordList.isEmpty()) {
            return emptyList()
        }

        val permissionMap = environmentPermissionService.listNodeByPermissions(userId, projectId,
            setOf(BkAuthPermission.USE, BkAuthPermission.EDIT, BkAuthPermission.DELETE))
        val canUseNodeIds = if (permissionMap.containsKey(BkAuthPermission.USE)) {
            permissionMap[BkAuthPermission.USE]!!.map { HashUtil.decodeIdToLong(it) }
        } else {
            emptyList()
        }
        val canEditNodeIds = if (permissionMap.containsKey(BkAuthPermission.EDIT)) {
            permissionMap[BkAuthPermission.EDIT]!!.map { HashUtil.decodeIdToLong(it) }
        } else {
            emptyList()
        }
        val canDeleteNodeIds = if (permissionMap.containsKey(BkAuthPermission.DELETE)) {
            permissionMap[BkAuthPermission.DELETE]!!.map { HashUtil.decodeIdToLong(it) }
        } else {
            emptyList()
        }

        val thirdPartyAgentNodeIds = nodeRecordList.filter { it.nodeType == NodeType.THIRDPARTY.name }
            .map { it.nodeId }
        val thirdPartyAgentMap = thirdPartyAgentDao.getAgentsByNodeIds(dslContext, thirdPartyAgentNodeIds, projectId)
            .associateBy { it.nodeId }
        return nodeRecordList.map {
            val thirdPartyAgent = thirdPartyAgentMap[it.nodeId]
            val gatewayShowName = if (thirdPartyAgent != null) {
                slaveGatewayService.getShowName(thirdPartyAgent.gateway)
            } else {
                ""
            }

            val nodeStringId = NodeStringIdUtils.getNodeStringId(it)
            NodeStringIdUtils.getRefineDisplayName(nodeStringId, it.displayName)
            NodeWithPermission(
                nodeHashId = HashUtil.encodeLongId(it.nodeId),
                nodeId = nodeStringId,
                name = it.nodeName,
                ip = it.nodeIp,
                nodeStatus = it.nodeStatus,
                agentStatus = getAgentStatus(it),
                nodeType = it.nodeType,
                osName = it.osName,
                createdUser = it.createdUser,
                operator = it.operator,
                bakOperator = it.bakOperator,
                canUse = canUseNodeIds.contains(it.nodeId),
                canEdit = canEditNodeIds.contains(it.nodeId),
                canDelete = canDeleteNodeIds.contains(it.nodeId),
                gateway = gatewayShowName,
                displayName = NodeStringIdUtils.getRefineDisplayName(nodeStringId, it.displayName),
                createTime = if (null == it.createdTime) "" else
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.createdTime),
                lastModifyTime = if (null == it.lastModifyTime) "" else
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.lastModifyTime),
                lastModifyUser = it.lastModifyUser ?: ""
            )
        }
    }

    fun listByHashIds(userId: String, projectId: String, hashIds: List<String>): List<NodeWithPermission> {
        val nodeIds = hashIds.map { HashUtil.decodeIdToLong(it) }
        val nodeRecordList = nodeDao.listAllByIds(dslContext, projectId, nodeIds)
        if (nodeRecordList.isEmpty()) {
            return emptyList()
        }

        val permissionMap = environmentPermissionService.listNodeByPermissions(userId, projectId,
            setOf(BkAuthPermission.USE, BkAuthPermission.EDIT, BkAuthPermission.DELETE))

        val canUseNodeIds = if (permissionMap.containsKey(BkAuthPermission.USE)) {
            permissionMap[BkAuthPermission.USE]!!.map { HashUtil.decodeIdToLong(it) }
        } else {
            emptyList()
        }
        val canEditNodeIds = if (permissionMap.containsKey(BkAuthPermission.EDIT)) {
            permissionMap[BkAuthPermission.EDIT]!!.map { HashUtil.decodeIdToLong(it) }
        } else {
            emptyList()
        }
        val canDeleteNodeIds = if (permissionMap.containsKey(BkAuthPermission.DELETE)) {
            permissionMap[BkAuthPermission.DELETE]!!.map { HashUtil.decodeIdToLong(it) }
        } else {
            emptyList()
        }

        val thirdPartyAgentNodeIds = nodeRecordList.filter { it.nodeType == NodeType.THIRDPARTY.name }
            .map { it.nodeId }
        val thirdPartyAgentMap = thirdPartyAgentDao.getAgentsByNodeIds(dslContext, thirdPartyAgentNodeIds, projectId)
            .associateBy { it.nodeId }
        return nodeRecordList.map {
            val thirdPartyAgent = thirdPartyAgentMap[it.nodeId]
            val gatewayShowName = if (thirdPartyAgent != null) {
                slaveGatewayService.getShowName(thirdPartyAgent.gateway)
            } else {
                ""
            }
            val nodeStringId = NodeStringIdUtils.getNodeStringId(it)
            NodeWithPermission(
                nodeHashId = HashUtil.encodeLongId(it.nodeId),
                nodeId = nodeStringId,
                name = it.nodeName,
                ip = it.nodeIp,
                nodeStatus = NodeStatus.getStatusName(it.nodeStatus),
                agentStatus = getAgentStatus(it),
                nodeType = NodeType.getTypeName(it.nodeType),
                osName = it.osName,
                createdUser = it.createdUser,
                operator = it.operator,
                bakOperator = it.bakOperator,
                canUse = canUseNodeIds.contains(it.nodeId),
                canEdit = canEditNodeIds.contains(it.nodeId),
                canDelete = canDeleteNodeIds.contains(it.nodeId),
                gateway = gatewayShowName,
                displayName = NodeStringIdUtils.getRefineDisplayName(nodeStringId, it.displayName),
                createTime = if (null == it.createdTime) "" else
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.createdTime),
                lastModifyTime = if (null == it.lastModifyTime) "" else
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.lastModifyTime),
                lastModifyUser = it.lastModifyUser ?: ""
            )
        }
    }

    fun listByType(userId: String, projectId: String, type: String): List<NodeBaseInfo> {
        val nodeRecords = nodeDao.listNodesByType(dslContext, projectId, type)
        return nodeRecords.map {
            val nodeStringId = NodeStringIdUtils.getNodeStringId(it)
            NodeBaseInfo(
                nodeHashId = HashUtil.encodeLongId(it.nodeId),
                nodeId = nodeStringId,
                name = it.nodeName,
                ip = it.nodeIp,
                nodeStatus = it.nodeStatus,
                agentStatus = getAgentStatus(it),
                nodeType = it.nodeType,
                osName = it.osName,
                createdUser = it.createdUser,
                operator = it.operator,
                bakOperator = it.bakOperator,
                gateway = "",
                displayName = NodeStringIdUtils.getRefineDisplayName(nodeStringId, it.displayName)
            )
        }
    }

    private fun checkDisplayName(projectId: String, nodeId: Long?, displayName: String) {
        if (nodeDao.isDisplayNameExist(dslContext, projectId, nodeId, displayName)) {
            throw OperationException("节点名称【$displayName】已存在")
        }
    }

    fun updateDisplayName(userId: String, projectId: String, nodeHashId: String, displayName: String) {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        nodeDao.get(dslContext, projectId, nodeId) ?: throw NotFoundException("node not found")
        if (!environmentPermissionService.checkNodePermission(userId, projectId, BkAuthPermission.EDIT)) {
            throw OperationException("No Permission")
        }
        checkDisplayName(projectId, nodeId, displayName)
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            nodeDao.updateDisplayName(dslContext = context, nodeId = nodeId, nodeName = displayName, userId = userId)
        }
    }
}