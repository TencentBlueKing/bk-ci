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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.misc.client.BcsClient
import com.tencent.devops.common.misc.client.EsbAgentClient
import com.tencent.devops.common.misc.pojo.agent.BcsVmNode
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.ProjectConfigDao
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.BcsVmParam
import com.tencent.devops.environment.pojo.CcNode
import com.tencent.devops.environment.pojo.CmdbNode
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.NodeDevCloudInfo
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import com.tencent.devops.environment.utils.AgentStatusUtils.getAgentStatus
import com.tencent.devops.environment.utils.BcsVmNodeStatusUtils
import com.tencent.devops.environment.utils.BcsVmParamCheckUtils.checkAndGetVmCreateParam
import com.tencent.devops.environment.utils.ImportServerNodeUtils
import com.tencent.devops.environment.utils.NodeStringIdUtils
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.ws.rs.NotFoundException

@Service
class NodeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao,
    private val envNodeDao: EnvNodeDao,
    private val projectConfigDao: ProjectConfigDao,
    private val bcsClient: BcsClient,
    private val redisOperation: RedisOperation,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val slaveGatewayService: SlaveGatewayService,
    private val environmentPermissionService: EnvironmentPermissionService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(NodeService::class.java)
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

    fun getUserCmdbNodes(userId: String, offset: Int, limit: Int): List<CmdbNode> {
        val cmdbNodes =
            ImportServerNodeUtils.getUserCmdbNode(redisOperation, userId, offset, limit)
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
            ImportServerNodeUtils.getUserCmdbNodeNew(redisOperation, userId, bakOperator, ips, offset, limit)
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
        val ccNodes = ImportServerNodeUtils.getUserCcNode(redisOperation, userId)
        return ccNodes.map {
            CcNode(it.name, it.assetID, it.operator, it.bakOperator, it.ip, it.displayIp, it.agentStatus, it.osName)
        }
    }

    fun addCmdbNodes(userId: String, projectId: String, nodeIps: List<String>) {
        // 验证 CMDB 节点IP和责任人
        val cmdbNodeList = EsbAgentClient.getCmdbNodeByIps(userId, nodeIps).nodes
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
            dslContext = dslContext,
            projectConfigDao = projectConfigDao,
            nodeDao = nodeDao,
            projectId = projectId,
            userId = userId,
            toAddNodeCount = toAddIpList.size
        )

        val now = LocalDateTime.now()
        val agentStatusMap = EsbAgentClient.getAgentStatus(userId, toAddIpList)
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
        val ccNodeList = EsbAgentClient.getCcNodeByIps(userId, nodeIps)
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
            dslContext = dslContext,
            projectConfigDao = projectConfigDao,
            nodeDao = nodeDao,
            projectId = projectId,
            userId = userId,
            toAddNodeCount = toAddIpList.size
        )

        val now = LocalDateTime.now()
        val agentStatusMap = EsbAgentClient.getAgentStatus(userId, toAddIpList)
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

    fun addOtherNodes(userId: String, projectId: String, nodeIps: List<String>) {
        logger.info("addOtherNodes, userId: $userId, projectId: $projectId, nodeIps: $nodeIps")

        // 只添加不存在的节点
        val existNodeList = nodeDao.listServerAndDevCloudNodes(dslContext, projectId)
        val existIpList = existNodeList.map { it.nodeIp }.toSet()
        val toAddIpList = nodeIps.filterNot { existIpList.contains(it) }.toSet()
        logger.info("toAddIpList: $toAddIpList")

        ImportServerNodeUtils.checkImportCount(
            dslContext = dslContext,
            projectConfigDao = projectConfigDao,
            nodeDao = nodeDao,
            projectId = projectId,
            userId = userId,
            toAddNodeCount = toAddIpList.size
        )

        val now = LocalDateTime.now()
        val agentStatusMap = EsbAgentClient.getAgentStatus(userId, toAddIpList)
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
                agentStatusMap[it] ?: false,
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

    fun addBcsVmNodes(userId: String, projectId: String, bcsVmParam: BcsVmParam) {
        if (!environmentPermissionService.checkNodePermission(userId, projectId, AuthPermission.CREATE)) {
            throw OperationException("没有创建节点的权限")
        }

        val existNodeList = nodeDao.listServerAndDevCloudNodes(dslContext, projectId)
        val existIpList = existNodeList.map {
            it.nodeIp
        }

        val vmCreateInfoPair =
            checkAndGetVmCreateParam(dslContext, projectConfigDao, nodeDao, projectId, userId, bcsVmParam)
        val bcsVmList = bcsClient.createVM(
            bcsVmParam.clusterId,
            projectId,
            bcsVmParam.instanceCount,
            vmCreateInfoPair.first,
            vmCreateInfoPair.second,
            vmCreateInfoPair.third
        )
        val now = LocalDateTime.now()
        val toAddNodeList = bcsVmList.filterNot { existIpList.contains(it.ip) }.map {
            TNodeRecord(
                null,
                "",
                projectId,
                it.ip,
                it.name,
                it.status,
                NodeType.BCSVM.name,
                it.clusterId,
                projectId,
                userId,
                now,
                now.plusDays(bcsVmParam.validity.toLong()),
                it.osName,
                null,
                null,
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
            throw OperationException(
                "没有删除节点的权限，节点ID：[${unauthorizedNodeIds.joinToString(",") { HashUtil.encodeLongId(it) }}]"
            )
        }

        // 回收 BCSVM 机器
        val bcsVmNodeList = existNodeList.filter { NodeType.BCSVM.name == it.nodeType }.map {
            BcsVmNode(it.nodeName, it.nodeClusterId, it.nodeNamespace, "", "", "")
        }

        if (bcsVmNodeList.isNotEmpty()) {
            try {
                bcsClient.deleteVm(bcsVmNodeList)
            } catch (e: Exception) {
                logger.error("delete bcs VM failed", e)
            }
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
        return environmentPermissionService.checkNodePermission(userId, projectId, AuthPermission.CREATE)
    }

    fun list(userId: String, projectId: String): List<NodeWithPermission> {
        val nodeRecordList = nodeDao.listNodes(dslContext, projectId)
        if (nodeRecordList.isEmpty()) {
            return emptyList()
        }

        // BCSVM状态实时更新
        BcsVmNodeStatusUtils.updateBcsVmNodeStatus(dslContext, nodeDao, bcsClient, nodeRecordList)

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
                if (it.nodeType == NodeType.THIRDPARTY.name || it.nodeType == NodeType.TSTACK.name || it.nodeType == NodeType.DEVCLOUD.name) {
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
                createTime = if (null == it.createdTime) "" else DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(
                    it.createdTime
                ),
                lastModifyTime = if (null == it.lastModifyTime) "" else DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(
                    it.lastModifyTime
                ),
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
                createTime = if (null == it.createdTime) "" else DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(
                    it.createdTime
                ),
                lastModifyTime = if (null == it.lastModifyTime) "" else DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(
                    it.lastModifyTime
                ),
                lastModifyUser = it.lastModifyUser ?: ""
            )
        }
    }

    fun listRawServerNodeByIds(userId: String, projectId: String, nodeHashIds: List<String>): List<NodeBaseInfo> {
        val nodeRecords =
            nodeDao.listServerNodesByIds(dslContext, projectId, nodeHashIds.map { HashUtil.decodeIdToLong(it) })
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

    fun listByNodeType(userId: String, projectId: String, nodeType: NodeType): List<NodeBaseInfo> {
        val nodeRecords = nodeDao.listNodesByType(dslContext, projectId, nodeType.name)
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

    fun changeCreatedUser(userId: String, projectId: String, nodeHashId: String) {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        val node = nodeDao.get(dslContext, projectId, nodeId) ?: throw NotFoundException("node not found")
        when (node.nodeType) {
            NodeType.CC.name -> {
                if (userId == node.operator || userId == node.bakOperator) {
                    nodeDao.updateCreatedUser(dslContext = dslContext, nodeId = nodeId, userId = userId)
                } else {
                    throw OperationException("没有操作权限")
                }
            }
            NodeType.CMDB.name -> {
                val isOperator = userId == node.operator
                val isBakOperator = node.bakOperator.split(";").contains(userId)
                if (isOperator || isBakOperator) {
                    nodeDao.updateCreatedUser(dslContext, nodeId, userId)
                } else {
                    throw OperationException("没有操作权限")
                }
            }
            else -> {
                throw OperationException("节点类型【${NodeType.getTypeName(node.nodeType)}】不支持修改导入人")
            }
        }
    }

    private fun checkDisplayName(projectId: String, nodeId: Long?, displayName: String) {
        if (nodeDao.isDisplayNameExist(dslContext, projectId, nodeId, displayName)) {
            throw OperationException("节点名称【$displayName】已存在")
        }
    }

    fun updateDisplayName(userId: String, projectId: String, nodeHashId: String, displayName: String) {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        val nodeInDb = nodeDao.get(dslContext, projectId, nodeId) ?: throw NotFoundException("node not found")
        if (!environmentPermissionService.checkNodePermission(userId, projectId, AuthPermission.EDIT)) {
            throw OperationException("No Permission")
        }
        checkDisplayName(projectId, nodeId, displayName)
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            nodeDao.updateDisplayName(dslContext = context, nodeId = nodeId, nodeName = displayName, userId = userId)
            if (nodeInDb.displayName != displayName) {
                environmentPermissionService.updateNode(userId, projectId, nodeId, displayName)
            }
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
}