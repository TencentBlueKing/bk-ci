package com.tencent.devops.misc.dao

import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.model.environment.tables.TNode
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class NodeDao {
    fun get(dslContext: DSLContext, projectId: String, nodeId: Long): TNodeRecord? {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                    .where(NODE_ID.eq(nodeId))
                    .and(PROJECT_ID.eq(projectId))
                    .fetchOne()
        }
    }

    fun updateNodeStatus(
        dslContext: DSLContext,
        id: Long,
        status: NodeStatus
    ) {
        with(TNode.T_NODE) {
            dslContext.update(this)
                    .set(NODE_STATUS, status.name)
                    .where(NODE_ID.eq(id))
                    .execute()
        }
    }

    fun batchDeleteNode(dslContext: DSLContext, projectId: String, nodeIds: List<Long>) {
        if (nodeIds.isEmpty()) {
            return
        }

        with(TNode.T_NODE) {
            dslContext.deleteFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(NODE_ID.`in`(nodeIds))
                    .execute()
        }
    }

    fun listDevCloudNodesByTaskId(dslContext: DSLContext, taskId: Long): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                    .where(NODE_TYPE.eq(NodeType.DEVCLOUD.name)).and(TASK_ID.eq(taskId))
                    .fetch()
        }
    }

    fun deleteDevCloudNodesByTaskId(dslContext: DSLContext, taskId: Long) {
        with(TNode.T_NODE) {
            dslContext.deleteFrom(this)
                    .where(NODE_TYPE.eq(NodeType.DEVCLOUD.name)).and(TASK_ID.eq(taskId))
                    .and(NODE_STATUS.eq(NodeStatus.DELETED.name))
                    .execute()
        }
    }

    fun updateNode(dslContext: DSLContext, allCmdbNodes: TNodeRecord) {
        dslContext.batchUpdate(allCmdbNodes).execute()
    }

    fun listAllServerNodes(dslContext: DSLContext): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(NODE_TYPE.`in`(NodeType.CC.name, NodeType.CMDB.name, NodeType.BCSVM.name, NodeType.OTHER.name, NodeType.DEVCLOUD.name))
                .fetch()
        }
    }

    fun listAllNodesByType(dslContext: DSLContext, nodeType: NodeType): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(NODE_TYPE.eq(nodeType.name))
                .fetch()
        }
    }

    fun batchUpdateNode(dslContext: DSLContext, allCmdbNodes: List<TNodeRecord>) {
        if (allCmdbNodes.isEmpty()) {
            return
        }
        dslContext.batchUpdate(allCmdbNodes).execute()
    }
}

/**

ALTER TABLE T_NODE ADD COLUMN `IMAGE` varchar(512) NULL COMMENT '镜像';
ALTER TABLE T_NODE ADD COLUMN `TASK_ID` bigint(20) NULL COMMENT '任务ID';

**/