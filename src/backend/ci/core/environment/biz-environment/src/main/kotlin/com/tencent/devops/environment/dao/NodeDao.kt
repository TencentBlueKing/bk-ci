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

package com.tencent.devops.environment.dao

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.environment.model.CreateNodeModel
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.model.environment.tables.TNode
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
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

    fun listNodes(dslContext: DSLContext, projectId: String): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(NODE_ID.desc())
                .fetch()
        }
    }

    fun listThirdpartyNodes(
        dslContext: DSLContext,
        projectId: String,
        nodeIds: Collection<Long>? = null
    ): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_TYPE.eq(NodeType.THIRDPARTY.name))
                .let { if (nodeIds != null) it.and(NODE_ID.`in`(nodeIds)) else it }
                .orderBy(NODE_ID.desc())
                .fetch()
        }
    }

    fun listServerNodes(dslContext: DSLContext, projectId: String): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.OTHER.name))
                .orderBy(NODE_ID.desc())
                .fetch()
        }
    }

    fun listServerAndDevCloudNodes(dslContext: DSLContext, projectId: String): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(
                    NODE_TYPE.`in`(
                        NodeType.CMDB.name,
                        NodeType.OTHER.name,
                        NodeType.DEVCLOUD.name
                    )
                )
                .orderBy(NODE_ID.desc())
                .fetch()
        }
    }

    fun listByIds(dslContext: DSLContext, projectId: String, nodeIds: Collection<Long>): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_ID.`in`(nodeIds))
                .orderBy(NODE_ID.desc())
                .fetch()
        }
    }

    fun countImportNode(dslContext: DSLContext, projectId: String): Int {
        with(TNode.T_NODE) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.OTHER.name))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countNodeByStatus(dslContext: DSLContext, projectId: String, nodeIds: Set<Long>, status: NodeStatus): Int {
        with(TNode.T_NODE) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_ID.`in`(nodeIds))
                .and(NODE_STATUS.eq(status.name))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun listServerNodesByIds(dslContext: DSLContext, projectId: String, nodeIds: Collection<Long>): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_ID.`in`(nodeIds))
                .and(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.OTHER.name))
                .orderBy(NODE_ID.desc())
                .fetch()
        }
    }

    fun listServerNodesByIds(dslContext: DSLContext, nodeIds: Collection<Long>): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(NODE_ID.`in`(nodeIds))
                .orderBy(NODE_ID.desc())
                .fetch()
        }
    }

    fun listServerNodesByIps(dslContext: DSLContext, projectId: String, ips: List<String>): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_IP.`in`(ips))
                .and(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.OTHER.name))
                .fetch()
        }
    }

    fun listAllByIds(dslContext: DSLContext, projectId: String, nodeIds: Collection<Long>): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_ID.`in`(nodeIds))
                .orderBy(NODE_ID.desc())
                .fetch()
        }
    }

    fun getByDisplayName(
        dslContext: DSLContext,
        projectId: String,
        displayName: String,
        nodeType: List<String>?
    ): Result<TNodeRecord> {
        with(TNode.T_NODE) {
            val condition = mutableListOf(PROJECT_ID.eq(projectId), DISPLAY_NAME.eq(displayName))
            if (nodeType != null && nodeType.isNotEmpty()) {
                condition.add(NODE_TYPE.`in`(nodeType))
            }
            return dslContext.selectFrom(this)
                .where(condition)
                .fetch()
        }
    }

    fun addNode(
        dslContext: DSLContext,
        projectId: String,
        ip: String,
        name: String,
        osName: String,
        status: NodeStatus,
        type: NodeType,
        userId: String
    ): Long
        /** Node ID **/
    {
        var nodeId = 0L
        with(TNode.T_NODE) {
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                nodeId = transactionContext.insertInto(
                    this,
                    PROJECT_ID,
                    NODE_IP,
                    NODE_NAME,
                    OS_NAME,
                    NODE_STATUS,
                    NODE_TYPE,
                    CREATED_USER,
                    CREATED_TIME,
                    LAST_MODIFY_USER,
                    LAST_MODIFY_TIME
                )
                    .values(
                        projectId,
                        ip,
                        name,
                        osName,
                        status.name,
                        type.name,
                        userId,
                        LocalDateTime.now(),
                        userId,
                        LocalDateTime.now()
                    )
                    .returning(NODE_ID)
                    .fetchOne()!!.nodeId
                val hashId = HashUtil.encodeLongId(nodeId)
                transactionContext.update(this)
                    .set(NODE_HASH_ID, hashId)
                    .where(NODE_ID.eq(nodeId))
                    .execute()
            }
        }
        return nodeId
    }

    fun insertNodeStringIdAndDisplayName(
        dslContext: DSLContext,
        id: Long,
        nodeStringId: String,
        displayName: String,
        userId: String
    ) {
        with(TNode.T_NODE) {
            dslContext.update(this)
                .set(NODE_STRING_ID, nodeStringId)
                .set(DISPLAY_NAME, displayName)
                .set(LAST_MODIFY_USER, userId)
                .set(LAST_MODIFY_TIME, LocalDateTime.now())
                .where(NODE_ID.eq(id))
                .execute()
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

    fun batchAddNode(dslContext: DSLContext, nodes: List<CreateNodeModel>) {
        if (nodes.isEmpty()) {
            return
        }
        val now = LocalDateTime.now()
        with(TNode.T_NODE) {
            nodes.map {
                val nodeId = dslContext.insertInto(
                    this,
                    NODE_STRING_ID,
                    PROJECT_ID,
                    NODE_IP,
                    NODE_NAME,
                    NODE_STATUS,
                    NODE_TYPE,
                    NODE_CLUSTER_ID,
                    NODE_NAMESPACE,
                    CREATED_USER,
                    CREATED_TIME,
                    EXPIRE_TIME,
                    OS_NAME,
                    OPERATOR,
                    BAK_OPERATOR,
                    AGENT_STATUS,
                    DISPLAY_NAME,
                    IMAGE,
                    TASK_ID,
                    LAST_MODIFY_TIME,
                    LAST_MODIFY_USER,
                    PIPELINE_REF_COUNT,
                    LAST_BUILD_TIME
                ).values(
                    it.nodeStringId,
                    it.projectId,
                    it.nodeIp,
                    it.nodeName,
                    it.nodeStatus,
                    it.nodeType,
                    it.nodeClusterId,
                    it.nodeNamespace,
                    it.createdUser,
                    now,
                    it.expireTime,
                    it.osName,
                    it.operator,
                    it.bakOperator,
                    it.agentStatus,
                    it.displayName,
                    it.image,
                    it.taskId,
                    now,
                    it.createdUser,
                    it.pipelineRefCount,
                    it.lastBuildTime
                ).returning(NODE_ID).fetchOne()!!.nodeId
                val hashId = HashUtil.encodeLongId(nodeId)
                dslContext.update(this)
                    .set(NODE_HASH_ID, hashId)
                    .where(NODE_ID.eq(nodeId))
                    .execute()
            }
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

    fun listNodesByType(dslContext: DSLContext, projectId: String, nodeType: String): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(NODE_TYPE.eq(nodeType))
                .and(PROJECT_ID.eq(projectId))
                .and(NODE_STATUS.ne(NodeStatus.CREATING.name))
                .and(NODE_STATUS.ne(NodeStatus.DELETING.name))
                .and(NODE_STATUS.ne(NodeStatus.DELETED.name))
                .fetch()
        }
    }

    fun listAllNodes(dslContext: DSLContext): Result<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .fetch()
        }
    }

    fun updateCreatedUser(dslContext: DSLContext, nodeId: Long, userId: String) {
        with(TNode.T_NODE) {
            dslContext.update(this)
                .set(CREATED_USER, userId)
                .where(NODE_ID.eq(nodeId))
                .execute()
        }
    }

    fun updateDisplayName(dslContext: DSLContext, nodeId: Long, nodeName: String, userId: String): Int {
        with(TNode.T_NODE) {
            return dslContext.update(this)
                .set(DISPLAY_NAME, nodeName)
                .set(LAST_MODIFY_USER, userId)
                .set(LAST_MODIFY_TIME, LocalDateTime.now())
                .where(NODE_ID.eq(nodeId))
                .execute()
        }
    }

    fun isDisplayNameExist(dslContext: DSLContext, projectId: String, nodeId: Long?, displayName: String): Boolean {
        with(TNode.T_NODE) {
            return if (nodeId != null) {
                dslContext.selectCount()
                    .from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(NODE_ID.ne(nodeId))
                    .and(DISPLAY_NAME.eq(displayName))
                    .fetchOne(0, Long::class.java)!! > 0
            } else {
                dslContext.selectCount()
                    .from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(DISPLAY_NAME.eq(displayName))
                    .fetchOne(0, Long::class.java)!! > 0
            }
        }
    }

    fun listPage(dslContext: DSLContext, page: Int, pageSize: Int, name: String?): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return if (name.isNullOrBlank()) {
                dslContext.selectFrom(this)
                    .limit(pageSize).offset((page - 1) * pageSize)
                    .fetch()
            } else {
                dslContext.selectFrom(this)
                    .where(NODE_NAME.like("%$name%"))
                    .limit(pageSize).offset((page - 1) * pageSize)
                    .fetch()
            }
        }
    }

    fun count(dslContext: DSLContext, name: String?): Int {
        with(TNode.T_NODE) {
            return if (name.isNullOrBlank()) {
                dslContext.selectCount()
                    .from(TNode.T_NODE)
                    .fetchOne(0, Int::class.java)!!
            } else {
                dslContext.selectCount()
                    .from(TNode.T_NODE)
                    .where(NODE_NAME.like("%$name%"))
                    .fetchOne(0, Int::class.java)!!
            }
        }
    }

    fun listPageForAuth(dslContext: DSLContext, offset: Int, limit: Int, projectId: String?): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return if (projectId.isNullOrBlank()) {
                dslContext.selectFrom(this)
                    .limit(limit).offset(offset)
                    .fetch()
            } else {
                dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .limit(limit).offset(offset)
                    .fetch()
            }
        }
    }

    fun countForAuth(dslContext: DSLContext, project: String?): Int {
        with(TNode.T_NODE) {
            return if (project.isNullOrBlank()) {
                dslContext.selectCount()
                    .from(TNode.T_NODE)
                    .fetchOne(0, Int::class.java)!!
            } else {
                dslContext.selectCount()
                    .from(TNode.T_NODE)
                    .where(PROJECT_ID.eq(project))
                    .fetchOne(0, Int::class.java)!!
            }
        }
    }

    fun searchByDisplayName(
        dslContext: DSLContext,
        offset: Int,
        limit: Int,
        projectId: String?,
        displayName: String
    ): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(DISPLAY_NAME.like("%$displayName%")))
                .orderBy(CREATED_TIME.desc())
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun countByDisplayName(dslContext: DSLContext, project: String?, displayName: String): Int {
        with(TNode.T_NODE) {
            return dslContext.selectCount()
                .from(TNode.T_NODE)
                .where(PROJECT_ID.eq(project).and(DISPLAY_NAME.like("%$displayName%")))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun saveNode(dslContext: DSLContext, nodeRecord: TNodeRecord) {
        dslContext.executeUpdate(nodeRecord)
    }

    fun updateLastBuildTime(dslContext: DSLContext, nodeId: Long, time: LocalDateTime) {
        with(TNode.T_NODE) {
            dslContext.update(this)
                .set(LAST_BUILD_TIME, time)
                .where(NODE_ID.eq(nodeId))
                .execute()
        }
    }

    fun updatePipelineRefCount(dslContext: DSLContext, nodeId: Long, count: Int) {
        with(TNode.T_NODE) {
            dslContext.update(this)
                .set(PIPELINE_REF_COUNT, count)
                .where(NODE_ID.eq(nodeId))
                .execute()
        }
    }

    fun getAllNode(
        dslContext: DSLContext,
        limit: Int,
        offset: Int
    ): Result<Record1<Long>>? {
        with(TNode.T_NODE) {
            return dslContext.select(NODE_ID).from(this)
                .orderBy(CREATED_TIME.desc())
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun updateHashId(
        dslContext: DSLContext,
        id: Long,
        hashId: String
    ) {
        with(TNode.T_NODE) {
            dslContext.update(this)
                .set(NODE_HASH_ID, hashId)
                .where(NODE_ID.eq(id))
                .and(NODE_HASH_ID.isNull)
                .execute()
        }
    }
}
