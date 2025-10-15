/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.tencent.devops.environment.constant.T_NODE_NODE_ID
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.model.environment.tables.TNode
import com.tencent.devops.model.environment.tables.TNodeTags
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import java.sql.Timestamp
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.OrderField
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Result
import org.jooq.SelectConditionStep
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

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
        keywords: String?,
        nodeType: NodeType?,
        nodeStatus: NodeStatus?,
        agentVersion: String?,
        osName: String?,
        latestBuildPipelineId: String?,
        latestBuildTimeStart: Long?,
        latestBuildTimeEnd: Long?,
        sortType: String?,
        collation: String?,
        tagValueIds: Set<Long>?
    ): List<TNodeRecord> {
        return with(TNode.T_NODE) {
            val dsl = dslContext.select(*TNode.T_NODE.fields()).from(this)
            if (!tagValueIds.isNullOrEmpty()) {
                dsl.leftJoin(TNodeTags.T_NODE_TAGS).on(NODE_ID.eq(TNodeTags.T_NODE_TAGS.NODE_ID))
            }
            val query = dsl.where(PROJECT_ID.eq(projectId))
            conditions(
                keywords = keywords,
                query = query,
                nodeIp = nodeIp,
                displayName = displayName,
                createdUser = createdUser,
                lastModifiedUser = lastModifiedUser,
                nodeType = nodeType,
                nodeStatus = nodeStatus,
                agentVersion = agentVersion,
                osName = osName,
                latestBuildPipelineId = latestBuildPipelineId,
                latestBuildTimeStart = latestBuildTimeStart,
                latestBuildTimeEnd = latestBuildTimeEnd,
                sortType = sortType,
                collation = collation,
                tagValueIds = tagValueIds
            )
            query.limit(limit).offset(offset)
                .fetchInto(this)
        }
    }

    private fun <T : Record> TNode.conditions(
        keywords: String?,
        query: SelectConditionStep<T>,
        nodeIp: String?,
        displayName: String?,
        createdUser: String?,
        lastModifiedUser: String?,
        nodeType: NodeType?,
        nodeStatus: NodeStatus?,
        agentVersion: String?,
        osName: String?,
        latestBuildPipelineId: String?,
        latestBuildTimeStart: Long?,
        latestBuildTimeEnd: Long?,
        sortType: String?,
        collation: String?,
        tagValueIds: Set<Long>?
    ) {
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
        if (nodeType != null) {
            query.and(NODE_TYPE.eq(nodeType.name))
        } else {
            /*除非特别指定，暂不显示内部NodeType类型*/
            query.and(NODE_TYPE.`in`(NodeType.coreTypesName()))
        }
        if (nodeStatus != null) {
            query.and(NODE_STATUS.eq(nodeStatus.name))
        }
        if (!agentVersion.isNullOrEmpty()) {
            query.and(AGENT_VERSION.like("%$agentVersion%"))
        }
        if (!osName.isNullOrEmpty()) {
            query.and(OS_NAME.like("%$osName%"))
        }
        if (!latestBuildPipelineId.isNullOrEmpty()) {
            query.and(LAST_BUILD_PIPELINE_ID.like("%$latestBuildPipelineId%"))
        }

        if (latestBuildTimeStart != null && latestBuildTimeStart > 0) {
            query.and(LAST_BUILD_TIME.ge(Timestamp(latestBuildTimeStart).toLocalDateTime()))
        }
        if (latestBuildTimeEnd != null && latestBuildTimeEnd > 0) {
            query.and(LAST_BUILD_TIME.le(Timestamp(latestBuildTimeEnd).toLocalDateTime()))
        }
        if (!tagValueIds.isNullOrEmpty()) {
            query.and(TNodeTags.T_NODE_TAGS.TAG_VALUE_ID.`in`(tagValueIds))
        }
        when (sortType) {
            /*别名*/"displayName" -> query.orderBy(DISPLAY_NAME.transferOrder(collation))
            /*IP*/"nodeIp" -> query.orderBy(NODE_IP.transferOrder(collation))
            /*操作系统*/"osName" -> query.orderBy(OS_NAME.transferOrder(collation))
            /*Agent 状态*/"nodeStatus" -> query.orderBy(NODE_STATUS.transferOrder(collation))
            /*导入人*/"createdUser" -> query.orderBy(CREATED_USER.transferOrder(collation))
            /*最近修改人*/"lastModifiedUser" -> query.orderBy(LAST_MODIFY_USER.transferOrder(collation))
            /*最近修改时间*/"lastModifiedTime" -> query.orderBy(LAST_MODIFY_TIME.transferOrder(collation))
            /*用途*/"nodeType" -> query.orderBy(NODE_TYPE.transferOrder(collation))
            /*最近执行流水线*/"latestBuildPipelineId" -> query.orderBy(
            LAST_BUILD_PIPELINE_ID.transferOrder(
                collation
            )
        )
            /*最近执行时间*/"latestBuildTime" -> query.orderBy(LAST_BUILD_TIME.transferOrder(collation))
            else -> query.orderBy(LAST_MODIFY_TIME.desc())
        }
    }

    private fun <T> Field<T>.transferOrder(collation: String?): OrderField<T> {
        return if (collation == "ASC") {
            this.asc()
        } else {
            this.desc()
        }
    }

    fun countForAuthWithSearchCondition(
        dslContext: DSLContext,
        projectId: String?,
        nodeIp: String?,
        displayName: String?,
        createdUser: String?,
        lastModifiedUser: String?,
        keywords: String?,
        nodeType: NodeType?,
        nodeStatus: NodeStatus?,
        agentVersion: String?,
        osName: String?,
        latestBuildPipelineId: String?,
        latestBuildTimeStart: Long?,
        latestBuildTimeEnd: Long?,
        sortType: String?,
        collation: String?,
        tagValueIds: Set<Long>?
    ): Int {
        with(TNode.T_NODE) {
            val dsl = dslContext.selectCount().from(TNode.T_NODE)
            if (!tagValueIds.isNullOrEmpty()) {
                dsl.leftJoin(TNodeTags.T_NODE_TAGS).on(NODE_ID.eq(TNodeTags.T_NODE_TAGS.NODE_ID))
            }
            val query = dsl.where(PROJECT_ID.eq(projectId))
            conditions(
                keywords = keywords,
                query = query,
                nodeIp = nodeIp,
                displayName = displayName,
                createdUser = createdUser,
                lastModifiedUser = lastModifiedUser,
                nodeType = nodeType,
                nodeStatus = nodeStatus,
                agentVersion = agentVersion,
                osName = osName,
                latestBuildPipelineId = latestBuildPipelineId,
                latestBuildTimeStart = latestBuildTimeStart,
                latestBuildTimeEnd = latestBuildTimeEnd,
                sortType = sortType,
                collation = collation,
                tagValueIds = tagValueIds

            )
            return query.fetchOne(0, Int::class.java)!!
        }
    }

    fun listNodes(dslContext: DSLContext, projectId: String, nodeType: NodeType? = null): List<TNodeRecord> {
        with(TNode.T_NODE) {
            val query = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))

            if (nodeType != null) {
                query.and(NODE_TYPE.eq(nodeType.name))
            } else {
                /*除非特别指定，暂不显示内部NodeType类型*/
                query.and(NODE_TYPE.`in`(NodeType.coreTypesName()))
            }
            return query.orderBy(NODE_ID.desc())
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

    fun countByNodeType(
        dslContext: DSLContext,
        projectId: String,
        nodeType: NodeType
    ): Long {
        with(TNode.T_NODE) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_TYPE.`in`(nodeType.name))
                .fetchOne(0, Long::class.java)!!
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

    fun listNodesByType(
        dslContext: DSLContext,
        projectId: String,
        nodeType: String,
        limit: Int? = null,
        offset: Int? = null
    ): List<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(NODE_TYPE.eq(nodeType))
                .and(PROJECT_ID.eq(projectId))
                .and(NODE_STATUS.ne(NodeStatus.CREATING.name))
                .and(NODE_STATUS.ne(NodeStatus.DELETING.name))
                .and(NODE_STATUS.ne(NodeStatus.DELETED.name))
                .let { if (limit != null && offset != null) it.limit(limit).offset(offset) else it }
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

    fun batchUpdateNodeCreatedUser(dslContext: DSLContext, nodeIdList: List<Long>, userId: String) {
        with(TNode.T_NODE) {
            dslContext.update(this)
                .set(CREATED_USER, userId)
                .where(NODE_ID.`in`(nodeIdList))
                .execute()
        }
    }

    fun updateDisplayName(
        dslContext: DSLContext,
        nodeId: Long,
        nodeName: String,
        userId: String,
        projectId: String
    ): Int {
        with(TNode.T_NODE) {
            return dslContext.update(this)
                .set(DISPLAY_NAME, nodeName)
                .set(LAST_MODIFY_USER, userId)
                .set(LAST_MODIFY_TIME, LocalDateTime.now())
                .where(NODE_ID.eq(nodeId))
                .and(PROJECT_ID.eq(projectId))
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

    fun updateLastBuildTime(dslContext: DSLContext, pipelineId: String, nodeId: Long, time: LocalDateTime) {
        with(TNode.T_NODE) {
            dslContext.update(this)
                .set(LAST_BUILD_TIME, time)
                .set(LAST_BUILD_PIPELINE_ID, pipelineId)
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

    fun fetchProjectNodeCount(dslContext: DSLContext, projectId: String): Map<NodeType, Int> {
        with(TNode.T_NODE) {
            return dslContext.select(NODE_TYPE, DSL.count().`as`("COUNT"))
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .groupBy(NODE_TYPE)
                .fetch().map { NodeType.get(it[NODE_TYPE] as String) to it["COUNT"] as Int }.toMap()
        }
    }

    fun fetchNodeProject(dslContext: DSLContext): Set<String> {
        with(TNode.T_NODE) {
            return dslContext.select(PROJECT_ID).from(this).where(NODE_TYPE.eq(NodeType.THIRDPARTY.name))
                .groupBy(PROJECT_ID).fetch().map { it[PROJECT_ID] }.toSet()
        }
    }
}
