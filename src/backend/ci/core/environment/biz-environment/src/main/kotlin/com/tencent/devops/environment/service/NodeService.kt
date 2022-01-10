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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_ENV_NO_DEL_PERMISSSION
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_CHANGE_USER_NOT_SUPPORT
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NAME_DUPLICATE
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NO_EDIT_PERMISSSION
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.slave.SlaveGatewayDao
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.service.node.NodeActionFactory
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import com.tencent.devops.environment.utils.AgentStatusUtils.getAgentStatus
import com.tencent.devops.environment.utils.NodeStringIdUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory

@Service@Suppress("ALL")
class NodeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao,
    private val envNodeDao: EnvNodeDao,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val slaveGatewayService: SlaveGatewayService,
    private val environmentPermissionService: EnvironmentPermissionService,
    private val nodeWebsocketService: NodeWebsocketService,
    private val webSocketDispatcher: WebSocketDispatcher,
    private val slaveGatewayDao: SlaveGatewayDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(NodeService::class.java)
    }

    fun deleteNodes(userId: String, projectId: String, nodeHashIds: List<String>) {
        val nodeLongIds = nodeHashIds.map { HashUtil.decodeIdToLong(it) }
        val canDeleteNodeIds =
            environmentPermissionService.listNodeByPermission(userId, projectId, AuthPermission.DELETE)
        val existNodeList = nodeDao.listByIds(dslContext, projectId, nodeLongIds)
        if (existNodeList.isEmpty()) {
            return
        }
        val existNodeIdList = existNodeList.map { it.nodeId }

        val unauthorizedNodeIds = existNodeIdList.filterNot { canDeleteNodeIds.contains(it) }
        if (unauthorizedNodeIds.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ERROR_ENV_NO_DEL_PERMISSSION,
                params = arrayOf(unauthorizedNodeIds.joinToString(",") { HashUtil.encodeLongId(it) })
            )
        }

        NodeActionFactory.load(NodeActionFactory.Action.DELETE)?.action(existNodeList)

        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            nodeDao.batchDeleteNode(context, projectId, existNodeIdList)
            envNodeDao.deleteByNodeIds(context, existNodeIdList)
            existNodeIdList.forEach {
                environmentPermissionService.deleteNode(projectId, it)
            }
            webSocketDispatcher.dispatch(
                    nodeWebsocketService.buildDetailMessage(projectId, userId)
            )
        }
    }

    fun hasCreatePermission(userId: String, projectId: String): Boolean {
        return environmentPermissionService.checkNodePermission(userId, projectId, AuthPermission.CREATE)
    }

    fun list(userId: String, projectId: String): List<NodeWithPermission> {
        val nodeRecordList = nodeDao.listNodes(dslContext, projectId)
        if (nodeRecordList.isEmpty()) {
            return emptyList()
        }

        val permissionMap = environmentPermissionService.listNodeByPermissions(
            userId = userId, projectId = projectId,
            permissions = setOf(AuthPermission.USE, AuthPermission.EDIT, AuthPermission.DELETE)
        )

        val canUseNodeIds = if (permissionMap.containsKey(AuthPermission.USE)) {
            permissionMap[AuthPermission.USE]?.map { HashUtil.decodeIdToLong(it) } ?: emptyList()
        } else {
            emptyList()
        }
        val canEditNodeIds = if (permissionMap.containsKey(AuthPermission.EDIT)) {
            permissionMap[AuthPermission.EDIT]?.map { HashUtil.decodeIdToLong(it) } ?: emptyList()
        } else {
            emptyList()
        }
        val canDeleteNodeIds = if (permissionMap.containsKey(AuthPermission.DELETE)) {
            permissionMap[AuthPermission.DELETE]?.map { HashUtil.decodeIdToLong(it) } ?: emptyList()
        } else {
            emptyList()
        }

        val thirdPartyAgentNodeIds = nodeRecordList.filter { it.nodeType == NodeType.THIRDPARTY.name }.map { it.nodeId }
        val thirdPartyAgentMap =
            thirdPartyAgentDao.getAgentsByNodeIds(dslContext, thirdPartyAgentNodeIds, projectId)
                .associateBy { it.nodeId }
        return nodeRecordList.map {
            val thirdPartyAgent = thirdPartyAgentMap[it.nodeId]
            val gatewayShowName = if (thirdPartyAgent != null) {
                slaveGatewayService.getShowName(thirdPartyAgent.gateway)
            } else {
                ""
            }

            // 如果是构建机类型，则取蓝盾Node状态，否则取gseAgent状态
            val nodeStatus =
                if (it.nodeType == NodeType.THIRDPARTY.name ||
                    it.nodeType == NodeType.DEVCLOUD.name) {
                    it.nodeStatus
                } else {
                    if (getAgentStatus(it)) {
                        NodeStatus.NORMAL.name
                    } else {
                        NodeStatus.ABNORMAL.name
                    }
                }
            val nodeStringId = NodeStringIdUtils.getNodeStringId(it)
            NodeStringIdUtils.getRefineDisplayName(nodeStringId, it.displayName)
            NodeWithPermission(
                nodeHashId = HashUtil.encodeLongId(it.nodeId),
                nodeId = nodeStringId,
                name = it.nodeName,
                ip = it.nodeIp,
                nodeStatus = nodeStatus,
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
                createTime = if (null == it.createdTime) {
                    ""
                } else {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.createdTime)
                },
                lastModifyTime = if (null == it.lastModifyTime) {
                    ""
                } else {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.lastModifyTime)
                },
                lastModifyUser = it.lastModifyUser ?: "",
                agentHashId = HashUtil.encodeLongId(thirdPartyAgent?.id ?: 0L)
            )
        }
    }

    fun listByHashIds(userId: String, projectId: String, hashIds: List<String>): List<NodeWithPermission> {
        val nodeIds = hashIds.map { HashUtil.decodeIdToLong(it) }
        val nodeRecordList = nodeDao.listAllByIds(dslContext, projectId, nodeIds)
        if (nodeRecordList.isEmpty()) {
            return emptyList()
        }

        val permissionMap = environmentPermissionService.listNodeByPermissions(
            userId, projectId,
            setOf(AuthPermission.USE, AuthPermission.EDIT, AuthPermission.DELETE)
        )

        val canUseNodeIds = if (permissionMap.containsKey(AuthPermission.USE)) {
            permissionMap[AuthPermission.USE]?.map { HashUtil.decodeIdToLong(it) } ?: emptyList()
        } else {
            emptyList()
        }
        val canEditNodeIds = if (permissionMap.containsKey(AuthPermission.EDIT)) {
            permissionMap[AuthPermission.EDIT]?.map { HashUtil.decodeIdToLong(it) } ?: emptyList()
        } else {
            emptyList()
        }
        val canDeleteNodeIds = if (permissionMap.containsKey(AuthPermission.DELETE)) {
            permissionMap[AuthPermission.DELETE]?.map { HashUtil.decodeIdToLong(it) } ?: emptyList()
        } else {
            emptyList()
        }

        val thirdPartyAgentNodeIds = nodeRecordList.filter { it.nodeType == NodeType.THIRDPARTY.name }.map { it.nodeId }
        val thirdPartyAgentMap =
            thirdPartyAgentDao.getAgentsByNodeIds(dslContext, thirdPartyAgentNodeIds, projectId)
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
                createTime = if (null == it.createdTime) {
                    ""
                } else {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.createdTime)
                },
                lastModifyTime = if (null == it.lastModifyTime) {
                    ""
                } else {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.lastModifyTime)
                },
                lastModifyUser = it.lastModifyUser ?: ""
            )
        }
    }

    fun listUsableServerNodes(userId: String, projectId: String): List<NodeWithPermission> {
        val nodeRecordList = nodeDao.listServerNodes(dslContext, projectId)
        if (nodeRecordList.isEmpty()) {
            return emptyList()
        }

        val canUseNodeIds = environmentPermissionService.listNodeByPermission(userId, projectId, AuthPermission.USE)

        val validRecordList = nodeRecordList.filter { canUseNodeIds.contains(it.nodeId) }
        return validRecordList.map {
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
                canEdit = null,
                canDelete = null,
                gateway = "",
                displayName = NodeStringIdUtils.getRefineDisplayName(nodeStringId, it.displayName),
                createTime = if (null == it.createdTime) {
                    ""
                } else {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.createdTime)
                },
                lastModifyTime = if (null == it.lastModifyTime) {
                    ""
                } else {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.lastModifyTime)
                },
                lastModifyUser = it.lastModifyUser ?: ""
            )
        }
    }

    fun listRawServerNodeByIds(userId: String, projectId: String, nodeHashIds: List<String>): List<NodeBaseInfo> {
        val nodeRecords =
            nodeDao.listServerNodesByIds(dslContext, projectId, nodeHashIds.map { HashUtil.decodeIdToLong(it) })
        return nodeRecords.map { NodeStringIdUtils.getNodeBaseInfo(it) }
    }

    fun listRawServerNodeByIds(nodeHashIds: List<String>): List<NodeBaseInfo> {
        val nodeRecords =
                nodeDao.listServerNodesByIds(dslContext, nodeHashIds.map { HashUtil.decodeIdToLong(it) })
        return nodeRecords.map { NodeStringIdUtils.getNodeBaseInfo(it) }
    }

    fun listByType(userId: String, projectId: String, type: String): List<NodeBaseInfo> {
        val nodeRecords = nodeDao.listNodesByType(dslContext, projectId, type)
        return nodeRecords.map { NodeStringIdUtils.getNodeBaseInfo(it) }
    }

    fun listByNodeType(userId: String, projectId: String, nodeType: NodeType): List<NodeBaseInfo> {
        val nodeRecords = nodeDao.listNodesByType(dslContext, projectId, nodeType.name)
        return nodeRecords.map { NodeStringIdUtils.getNodeBaseInfo(it) }
    }

    fun changeCreatedUser(userId: String, projectId: String, nodeHashId: String) {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        val node = nodeDao.get(dslContext, projectId, nodeId) ?: throw ErrorCodeException(
            errorCode = ERROR_NODE_NOT_EXISTS,
            params = arrayOf(nodeHashId)
        )
        when (node.nodeType) {
            NodeType.CMDB.name -> {
                val isOperator = userId == node.operator
                val isBakOperator = node.bakOperator.split(";").contains(userId)
                if (isOperator || isBakOperator) {
                    nodeDao.updateCreatedUser(dslContext, nodeId, userId)
                } else {
                    throw ErrorCodeException(errorCode = ERROR_NODE_NO_EDIT_PERMISSSION)
                }
            }
            else -> {
                throw ErrorCodeException(
                    errorCode = ERROR_NODE_CHANGE_USER_NOT_SUPPORT,
                    params = arrayOf(NodeType.getTypeName(node.nodeType))
                )
            }
        }
    }

    private fun checkDisplayName(projectId: String, nodeId: Long?, displayName: String) {
        if (nodeDao.isDisplayNameExist(dslContext, projectId, nodeId, displayName)) {
            throw ErrorCodeException(errorCode = ERROR_NODE_NAME_DUPLICATE, params = arrayOf(displayName))
        }
    }

    fun updateDisplayName(userId: String, projectId: String, nodeHashId: String, displayName: String) {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        val nodeInDb = nodeDao.get(dslContext, projectId, nodeId) ?: throw ErrorCodeException(
            errorCode = ERROR_NODE_NOT_EXISTS,
            params = arrayOf(nodeHashId)
        )
        if (!environmentPermissionService.checkNodePermission(userId, projectId, nodeId, AuthPermission.EDIT)) {
            throw PermissionForbiddenException(
                    message = MessageCodeUtil.getCodeLanMessage(ERROR_NODE_NO_EDIT_PERMISSSION))
        }
        checkDisplayName(projectId, nodeId, displayName)
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            nodeDao.updateDisplayName(dslContext = context, nodeId = nodeId, nodeName = displayName, userId = userId)
            if (nodeInDb.displayName != displayName) {
                environmentPermissionService.updateNode(userId, projectId, nodeId, displayName)
            }
            webSocketDispatcher.dispatch(
                    nodeWebsocketService.buildDetailMessage(projectId, userId)
            )
        }
    }

    fun getByDisplayName(userId: String, projectId: String, displayName: String): List<NodeBaseInfo> {
        val nodes = nodeDao.getByDisplayName(dslContext, projectId, displayName, null)
        if (nodes.isEmpty()) {
            return emptyList()
        }

        val canUseNodeIds = environmentPermissionService.listNodeByPermission(userId, projectId, AuthPermission.USE)
        val validRecordList = nodes.filter { canUseNodeIds.contains(it.nodeId) }
        return validRecordList.map { NodeStringIdUtils.getNodeBaseInfo(it) }
    }

    fun listByPage(projectId: String, offset: Int?, limit: Int?): Page<NodeBaseInfo> {
        val nodeInfos = nodeDao.listPageForAuth(dslContext, offset!!, limit!!, projectId)
        val count = nodeDao.countForAuth(dslContext, projectId)
        return Page(
            count = count.toLong(),
            page = offset!!,
            pageSize = limit!!,
            records = nodeInfos.map { NodeStringIdUtils.getNodeBaseInfo(it) }
        )
    }

    fun searchByDisplayName(projectId: String, offset: Int?, limit: Int?, displayName: String): Page<NodeBaseInfo> {
        val nodeInfos = nodeDao.searchByDisplayName(
                dslContext = dslContext,
                offset = offset!!,
                limit = limit!!,
                projectId = projectId,
                displayName = displayName
        )
        val count = nodeDao.countByDisplayName(
                dslContext = dslContext,
                project = projectId,
                displayName = displayName
        )
        return Page(
                count = count.toLong(),
                page = offset!!,
                pageSize = limit!!,
                records = nodeInfos.map { NodeStringIdUtils.getNodeBaseInfo(it) }
        )
    }

    fun extListNodes(userId: String, projectId: String): List<NodeWithPermission> {
        val nodeRecordList = nodeDao.listThirdpartyNodes(dslContext, projectId)
        if (nodeRecordList.isEmpty()) {
            return emptyList()
        }
        return nodeRecordList.map {
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
                canUse = false,
                canEdit = false,
                canDelete = false,
                gateway = "",
                displayName = NodeStringIdUtils.getRefineDisplayName(nodeStringId, it.displayName),
                createTime = if (null == it.createdTime) {
                    ""
                } else {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.createdTime)
                },
                lastModifyTime = if (null == it.lastModifyTime) {
                    ""
                } else {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.lastModifyTime)
                },
                lastModifyUser = it.lastModifyUser ?: "",
                pipelineRefCount = it.pipelineRefCount ?: 0,
                lastBuildTime = if (null == it.lastBuildTime) {
                    ""
                } else {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.lastBuildTime)
                }
            )
        }
    }

    fun refreshGateway(oldToNewMap: Map<String, String>): Boolean {
        return try {
            slaveGatewayDao.refreshGateway(dslContext, oldToNewMap)
            thirdPartyAgentDao.refreshGateway(dslContext, oldToNewMap)
            true
        } catch (ignore: Throwable) {
            logger.error("AUTH|refreshGateway failed with error: ", ignore)
            false
        }
    }
}
