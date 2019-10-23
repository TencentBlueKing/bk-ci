package com.tencent.devops.environment.utils

import com.tencent.devops.common.environment.agent.client.BcsClient
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.DSLContext

object BcsVmNodeStatusUtils {

    fun updateBcsVmNodeStatus(dslContext: DSLContext, nodeDao: NodeDao, bcsClient: BcsClient, nodeRecordList: List<TNodeRecord>) {
        val abnormalNodes = nodeRecordList.filter {
            it.nodeType == NodeType.BCSVM.name && it.nodeStatus != NodeStatus.NORMAL.name
        }

        if (abnormalNodes.isEmpty()) {
            return
        }

        abnormalNodes.forEach {
            val statusAndOs = bcsClient.inspectVmList(
                clusterId = it.nodeClusterId,
                nodeNames = it.nodeName,
                namespace = it.nodeNamespace
            )
            if (statusAndOs != null) {
                it.nodeStatus = statusAndOs.nodeStatus
                it.osName = statusAndOs.osName
            }
        }

        nodeDao.batchUpdateNode(dslContext, abnormalNodes)
    }
}