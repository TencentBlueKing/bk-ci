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
import com.tencent.devops.environment.constant.T_NODE_NODE_ID

@Suppress("ALL")
@Repository
class NodeDao {
    fun updateDevopsAgentVersionByNodeId(dslContext: DSLContext, nodeId: Long, agentVersion: String) {
        with(TNode.T_NODE) {
            dslContext.update(this)
                .set(AGENT_VERSION, agentVersion)
                .where(NODE_ID.eq(nodeId))
                .execute()
        }
    }

    fun get(dslContext: DSLContext, projectId: String, nodeId: Long): TNodeRecord? {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(NODE_ID.eq(nodeId))
                .and(PROJECT_ID.eq(projectId))
                .fetchOne()
        }
    }

    fun listNodesWithPageLimitAndSearchCondition(
        dslContext: DSLContext,
        projectId: String,
        limit: Int,
        offset: Int,
        nodeIp: String?,
        displayName: String?,
        createdUser: String?,
        lastModifiedUser: String?,
        keywords: String?
    ): List<TNodeRecord> {
        return with(TNode.T_NODE) {
            val query = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
            if (!keywords.isNullOrEmpty()) {
                query.and(NODE_IP.like("%$keywords%").or(DISPLAY_NAME.like("%$keywords%")))
            }
            if (!nodeIp.isNullOrEmpty()) {
                query.and(NODE_IP.like("%$nodeIp%"))
            }
            if (!displayName.isNullOrEmpty()) {
                query.and(DISPLAY_NAME.like("%$displayName%"))
            }
            if (!createdUser.isNullOrEmpty()) {
                query.and(CREATED_USER.like("%$createdUser%"))
            }
            if (!lastModifiedUser.isNullOrEmpty()) {
                query.and(LAST_MODIFY_USER.like("%$lastModifiedUser%"))
            }
            query.orderBy(LAST_MODIFY_TIME.desc())
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun countForAuthWithSearchCondition(
        dslContext: DSLContext,
        projectId: String?,
        nodeIp: String?,
        displayName: String?,
        createdUser: String?,
        lastModifiedUser: String?,
        keywords: String?
    ): Int {
        with(TNode.T_NODE) {
            return if (projectId.isNullOrBlank()) {
                dslContext.selectCount()
                    .from(TNode.T_NODE)
                    .fetchOne(0, Int::class.java)!!
            } else {
                val query = dslContext.selectCount()
                    .from(TNode.T_NODE)
                    .where(PROJECT_ID.eq(projectId))
                if (!keywords.isNullOrEmpty()) {
                    query.and(NODE_IP.like("%$keywords%").or(DISPLAY_NAME.like("%$keywords%")))
                }
                if (!nodeIp.isNullOrEmpty()) {
                    query.and(NODE_IP.like("%$nodeIp%"))
                }
                if (!displayName.isNullOrEmpty()) {
                    query.and(DISPLAY_NAME.like("%$displayName%"))
                }
                if (!createdUser.isNullOrEmpty()) {
                    query.and(CREATED_USER.like("%$createdUser%"))
                }
                if (!lastModifiedUser.isNullOrEmpty()) {
                    query.and(LAST_MODIFY_USER.like("%$lastModifiedUser%"))
                }
                query.fetchOne(0, Int::class.java)!!
            }
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

    fun listByIds(dslContext: DSLContext, projectId: String, nodeIds: Collection<Long>): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_ID.`in`(nodeIds))
                .orderBy(NODE_ID.desc())
                .fetch()
        }
    }

    fun listNodesByIdListWithPageLimit(
        dslContext: DSLContext,
        projectId: String,
        limit: Int,
        offset: Int,
        nodeIds: Collection<Long>
    ): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_ID.`in`(nodeIds))
                .orderBy(NODE_ID.desc())
                .limit(limit).offset(offset)
                .fetch()
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
            if (!nodeType.isNullOrEmpty()) {
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
        userId: String,
        agentVersion: String?
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
                    LAST_MODIFY_TIME,
                    AGENT_VERSION
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
                        LocalDateTime.now(),
                        agentVersion
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
        ids: Set<Long>,
        status: NodeStatus
    ) {
        with(TNode.T_NODE) {
            dslContext.update(this)
                .set(NODE_STATUS, status.name)
                .where(NODE_ID.`in`(ids))
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

    fun countByNodeIdList(dslContext: DSLContext, projectId: String, nodeIds: Collection<Long>): Int {
        with(TNode.T_NODE) {
            return dslContext.selectCount()
                .from(TNode.T_NODE)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_ID.`in`(nodeIds))
                .fetchOne(0, Int::class.java)!!
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
            return dslContext.select(NODE_ID.`as`(T_NODE_NODE_ID)).from(this)
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
