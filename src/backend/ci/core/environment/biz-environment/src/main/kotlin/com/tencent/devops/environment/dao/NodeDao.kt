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

package com.tencent.devops.environment.dao

import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.model.environment.tables.TNode
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

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

    fun listServerNodes(dslContext: DSLContext, projectId: String): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_TYPE.`in`(NodeType.BCSVM.name, NodeType.CC.name, NodeType.CMDB.name, NodeType.OTHER.name))
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
                        NodeType.BCSVM.name,
                        NodeType.CC.name,
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

    fun countBcsVm(dslContext: DSLContext, projectId: String): Int {
        with(TNode.T_NODE) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_TYPE.eq(NodeType.BCSVM.name))
                .fetchOne(0, Int::class.java)
        }
    }

    fun countDevCloudVm(dslContext: DSLContext, projectId: String): Int {
        with(TNode.T_NODE) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_TYPE.eq(NodeType.DEVCLOUD.name))
                .fetchOne(0, Int::class.java)
        }
    }

    fun countImportNode(dslContext: DSLContext, projectId: String): Int {
        with(TNode.T_NODE) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_TYPE.`in`(NodeType.CC.name, NodeType.CMDB.name, NodeType.OTHER.name))
                .fetchOne(0, Int::class.java)
        }
    }

    fun countNodeByStatus(dslContext: DSLContext, projectId: String, nodeIds: Set<Long>, status: NodeStatus): Int {
        with(TNode.T_NODE) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_ID.`in`(nodeIds))
                .and(NODE_STATUS.eq(status.name))
                .fetchOne(0, Int::class.java)
        }
    }

    fun listServerNodesByIds(dslContext: DSLContext, projectId: String, nodeIds: Collection<Long>): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_ID.`in`(nodeIds))
                .and(NODE_TYPE.`in`(NodeType.BCSVM.name, NodeType.CC.name, NodeType.CMDB.name, NodeType.OTHER.name))
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
                .and(NODE_TYPE.`in`(NodeType.BCSVM.name, NodeType.CC.name, NodeType.CMDB.name, NodeType.OTHER.name))
                .fetch()
        }
    }

    fun listDevCloudNodesByIps(dslContext: DSLContext, projectId: String, ips: List<String>): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_IP.`in`(ips))
                .and(NODE_TYPE.eq(NodeType.DEVCLOUD.name))
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
            val condition = mutableListOf<Condition>(PROJECT_ID.eq(projectId), DISPLAY_NAME.eq(displayName))
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
        with(TNode.T_NODE) {
            return dslContext.insertInto(
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
                .fetchOne().nodeId
        }
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

    fun insertNodeStringId(
        dslContext: DSLContext,
        id: Long,
        nodeStringId: String
    ) {
        with(TNode.T_NODE) {
            dslContext.update(this)
                .set(NODE_STRING_ID, nodeStringId)
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

    fun updateNodeStatus(
        dslContext: DSLContext,
        id: Long,
        status: NodeStatus,
        expectStatus: NodeStatus
    ): Int {
        with(TNode.T_NODE) {
            return dslContext.update(this)
                .set(NODE_STATUS, status.name)
                .where(NODE_ID.eq(id))
                .and(NODE_STATUS.eq(expectStatus.statusName))
                .execute()
        }
    }

    fun getMaxNodeStringId(
        dslContext: DSLContext,
        projectId: String,
        id: Long
    ): TNodeRecord? {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(NODE_ID.ne(id))
                .and(PROJECT_ID.eq(projectId))
                .orderBy(CREATED_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun batchAddNode(dslContext: DSLContext, nodeList: List<TNodeRecord>) {
        if (nodeList.isEmpty()) {
            return
        }

        dslContext.batchInsert(nodeList).execute()
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

    fun listAllNodesByType(dslContext: DSLContext, nodeType: NodeType): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(NODE_TYPE.eq(nodeType.name))
                .fetch()
        }
    }

    fun listAllServerNodes(dslContext: DSLContext): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(
                    NODE_TYPE.`in`(
                        NodeType.CC.name,
                        NodeType.CMDB.name,
                        NodeType.BCSVM.name,
                        NodeType.OTHER.name,
                        NodeType.DEVCLOUD.name
                    )
                )
                .fetch()
        }
    }

    fun listAllNodes(dslContext: DSLContext): Result<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .fetch()
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
        with(TNode.T_NODE) {
            dslContext.batchUpdate(allCmdbNodes).execute()
        }
    }

    fun batchUpdateNode(dslContext: DSLContext, allCmdbNodes: List<TNodeRecord>) {
        if (allCmdbNodes.isEmpty()) {
            return
        }

        with(TNode.T_NODE) {
            dslContext.batchUpdate(allCmdbNodes).execute()
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
                    .fetchOne(0, Long::class.java) > 0
            } else {
                dslContext.selectCount()
                    .from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(DISPLAY_NAME.eq(displayName))
                    .fetchOne(0, Long::class.java) > 0
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
                    .fetchOne(0, Int::class.java)
            } else {
                dslContext.selectCount()
                    .from(TNode.T_NODE)
                    .where(NODE_NAME.like("%$name%"))
                    .fetchOne(0, Int::class.java)
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
                    .where(PROJECT_ID.like(projectId))
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
                    .fetchOne(0, Int::class.java)
            } else {
                dslContext.selectCount()
                    .from(TNode.T_NODE)
                    .where(PROJECT_ID.eq(project))
                    .fetchOne(0, Int::class.java)
            }
        }
    }

    fun saveNode(dslContext: DSLContext, nodeRecord: TNodeRecord) {
        dslContext.executeUpdate(nodeRecord)
    }
}