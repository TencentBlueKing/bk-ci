package com.tencent.devops.environment.service.node

import com.tencent.devops.common.misc.client.BcsClient
import com.tencent.devops.common.misc.pojo.agent.BcsVmNode
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DeleteBcsNodeActionImpl @Autowired constructor(private val bcsClient: BcsClient) : NodeAction {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(DeleteBcsNodeActionImpl::class.java)!!
    }

    override fun type(): NodeActionFactory.Action {
        return NodeActionFactory.Action.DELETE
    }

    override fun action(nodeRecords: List<TNodeRecord>) {
        // 回收 BCSVM 机器
        val bcsVmNodeList = nodeRecords.filter { NodeType.BCSVM.name == it.nodeType }.map {
            BcsVmNode(it.nodeName, it.nodeClusterId, it.nodeNamespace, "", "", "")
        }

        if (bcsVmNodeList.isNotEmpty()) {
            try {
                bcsClient.deleteVm(bcsVmNodeList)
            } catch (e: Exception) {
                logger.error("delete bcs VM failed", e)
            }
        }
    }
}