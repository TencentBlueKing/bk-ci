package com.tencent.devops.environment.service.tstack

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.environment.client.TstackClient
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.tstack.TstackNodeDao
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.pojo.tstack.TstackNode
import com.tencent.devops.environment.utils.NodeAuthUtils
import org.apache.commons.lang.StringUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TstackNodeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val tstackNodeDao: TstackNodeDao,
    private val nodeDao: NodeDao,
    private val nodeAuthUtils: NodeAuthUtils,
    private val tstackClient: TstackClient
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TstackNodeService::class.java)
    }

    fun destroyTstackVm(tstackVmId: String) {
    }

    fun updateAvailable(hashId: String, available: Boolean) {
        val id = HashUtil.decodeIdToLong(hashId)
        tstackNodeDao.updateAvailable(dslContext, id, available)
    }

    fun listAvailableVm(projectId: String): List<TstackNode> {
        val vms = tstackNodeDao.listAvailableVm(dslContext, projectId)
        return vms.map {
            TstackNode(
                    HashUtil.encodeLongId(it.id),
                    null,
                    it.projectId,
                    it.tstackVmId,
                    it.vmIp,
                    it.vmName,
                    NodeType.TSTACK.name,
                    NodeStatus.NORMAL.name,
                    OS.WINDOWS.name,
                    it.vmOsVersion,
                    it.vmCpu,
                    it.vmMemory,
                    it.available,
                    it.createdTime.timestamp(),
                    it.updatedTime.timestamp()
            )
        }
    }

    fun get(projectId: String, hashId: String): TstackNode? {
        val id = HashUtil.decodeIdToLong(hashId)
        val statckVmRecord = tstackNodeDao.getOrNull(dslContext, projectId, id) ?: return null
        return TstackNode(
                HashUtil.encodeLongId(statckVmRecord.id),
                null,
                statckVmRecord.projectId,
                statckVmRecord.tstackVmId,
                statckVmRecord.vmIp,
                statckVmRecord.vmName,
                NodeType.TSTACK.name,
                NodeStatus.NORMAL.name,
                OS.WINDOWS.name,
                statckVmRecord.vmOsVersion,
                statckVmRecord.vmCpu,
                statckVmRecord.vmMemory,
                statckVmRecord.available,
                statckVmRecord.createdTime.timestamp(),
                statckVmRecord.updatedTime.timestamp()
        )
    }

    fun getVncToken(projectId: String, nodeHashId: String): String {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        val statckVmRecord = tstackNodeDao.getByNodeId(dslContext, projectId, nodeId) ?: throw OperationException("TStack 虚拟机不存在")
        return tstackClient.createVncToken(statckVmRecord.tstackVmId)
    }

    fun assignTstackNode(projectId: String, stackNodeId: String, user: String): String {
        val longId = HashUtil.decodeIdToLong(stackNodeId)
        val tstackNode = tstackNodeDao.getOrNull(dslContext, longId) ?: throw OperationException("TStack 节点不存在")
        if (StringUtils.isNotBlank(tstackNode.projectId) || tstackNode.nodeId != null) {
            throw OperationException("TStack 节点已被分配，不能重新分配")
        }

        var nodeHashId = ""
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val nodeId = nodeDao.addNode(context,
                    projectId,
                    tstackNode.vmIp,
                    tstackNode.vmName,
                    tstackNode.vmOsVersion,
                    NodeStatus.NORMAL,
                    NodeType.TSTACK,
                    user)
            val nodeRecord = nodeDao.get(context, projectId, nodeId)!!
            tstackNodeDao.setNodeIdAndProjectId(context, longId, nodeId, projectId)
            nodeAuthUtils.batchCreateNodeResource(user, projectId, listOf(nodeRecord))

            nodeHashId = HashUtil.encodeLongId(nodeId)
        }

        return nodeHashId
    }

    fun unassignTstackNode(projectId: String, stackNodeId: String): Boolean {
        val longId = HashUtil.decodeIdToLong(stackNodeId)
        val tstackNode = tstackNodeDao.getOrNull(dslContext, longId) ?: throw OperationException("TStack 节点不存在")
        if (tstackNode.nodeId != null) {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                val nodeRecord = nodeDao.get(context, projectId, tstackNode.nodeId)
                if (nodeRecord == null) {
                    logger.warn("tstack node not exists")
                } else {
                    nodeDao.batchDeleteNode(context, projectId, listOf(tstackNode.nodeId))
                    tstackNodeDao.cleanNodeIdAndProjectId(context, longId)
                    nodeAuthUtils.deleteResource(projectId, listOf(nodeRecord))
                }
            }
        }

        return true
    }
}
