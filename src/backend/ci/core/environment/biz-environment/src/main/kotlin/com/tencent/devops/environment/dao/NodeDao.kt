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
import com.tencent.devops.environment.constant.T_ENV_ENV_ID
import com.tencent.devops.environment.constant.T_NODE_AGENT_STATUS
import com.tencent.devops.environment.constant.T_NODE_AGENT_VERSION
import com.tencent.devops.environment.constant.T_NODE_CLOUD_AREA_ID
import com.tencent.devops.environment.constant.T_NODE_DISPLAY_NAME
import com.tencent.devops.environment.model.CreateNodeModel
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.pojo.job.HostIdAndCloudAreaIdInfo
import com.tencent.devops.environment.pojo.job.req.Host
import com.tencent.devops.model.environment.tables.TNode
import com.tencent.devops.model.environment.tables.TEnv
import com.tencent.devops.model.environment.tables.TEnvNode
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Record2
import org.jooq.Record3
import org.jooq.Record5
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import com.tencent.devops.environment.constant.T_NODE_NODE_IP
import com.tencent.devops.environment.constant.T_NODE_HOST_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_HASH_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_STATUS
import com.tencent.devops.environment.constant.T_NODE_NODE_TYPE
import com.tencent.devops.environment.constant.T_NODE_PROJECT_ID
import com.tencent.devops.environment.pojo.job.UpdateTNodeInfo
import org.jooq.Record4
import org.jooq.Record7

@Suppress("ALL")
@Repository
class NodeDao {
    fun updateHostIdAndCloudAreaIdByNodeId(
        dslContext: DSLContext,
        nodeHostIdAndCloudAreaIdInfoList: List<HostIdAndCloudAreaIdInfo>
    ) {
        with(TNode.T_NODE) {
            val batchUpdate = dslContext.batch(
                nodeHostIdAndCloudAreaIdInfoList.map {
                    dslContext.update(this)
                        .set(HOST_ID, it.bkHostId)
                        .set(CLOUD_AREA_ID, it.bkCloudId)
                        .set(LAST_MODIFY_TIME, LocalDateTime.now())
                        .where(NODE_ID.eq(it.nodeId))
                }
            )
            batchUpdate.execute()
        }
    }

    fun batchUpdateAgentInfo(dslContext: DSLContext, updateAgentInfo: List<UpdateTNodeInfo>) {
        with(TNode.T_NODE) {
            val batchUpdate = dslContext.batch(
                updateAgentInfo.map {
                    dslContext.update(this)
                        .set(NODE_STATUS, it.nodeStatus)
                        .set(AGENT_STATUS, it.agentStatus)
                        .set(AGENT_VERSION, it.agentVersion)
                        .set(LAST_MODIFY_TIME, LocalDateTime.now())
                        .where(NODE_ID.eq(it.nodeId))
                }
            )
            batchUpdate.execute()
        }
    }

    fun batchUpdateDisplayName(dslContext: DSLContext, updateAgentInfo: List<UpdateTNodeInfo>) {
        with(TNode.T_NODE) {
            val batchUpdate = dslContext.batch(
                updateAgentInfo.map {
                    dslContext.update(this)
                        .set(DISPLAY_NAME, it.displayName)
                        .set(LAST_MODIFY_TIME, LocalDateTime.now())
                        .where(NODE_ID.eq(it.nodeId))
                }
            )
            batchUpdate.execute()
        }
    }

    fun batchUpdateCCInfo(dslContext: DSLContext, updateAgentInfo: List<UpdateTNodeInfo>) {
        with(TNode.T_NODE) {
            val batchUpdate = dslContext.batch(
                updateAgentInfo.map {
                    dslContext.update(this)
                        .set(NODE_STATUS, it.nodeStatus)
                        .set(AGENT_STATUS, it.agentStatus)
                        .set(AGENT_VERSION, it.agentVersion)
                        .set(LAST_MODIFY_TIME, LocalDateTime.now())
                        .where(NODE_ID.eq(it.nodeId))
                }
            )
            batchUpdate.execute()
        }
    }

    fun getDeployNodesLimit(dslContext: DSLContext, offset: Int, limit: Int): Result<Record5<Long, String, String, Long, Long>> {
        with(TNode.T_NODE) {
            return dslContext.select(
                NODE_ID.`as`(T_NODE_NODE_ID),
                NODE_TYPE.`as`(T_NODE_NODE_TYPE),
                NODE_IP.`as`(T_NODE_NODE_IP),
                HOST_ID.`as`(T_NODE_HOST_ID),
                CLOUD_AREA_ID.`as`(T_NODE_CLOUD_AREA_ID)
            ).from(this)
                .where(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                .orderBy(NODE_ID.desc())
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun getDeployNodesInCmdbLimit(dslContext: DSLContext, offset: Int, limit: Int): Result<Record5<Long, String, String, Long, Long>> {
        with(TNode.T_NODE) {
            return dslContext.select(
                NODE_ID.`as`(T_NODE_NODE_ID),
                NODE_TYPE.`as`(T_NODE_NODE_TYPE),
                NODE_IP.`as`(T_NODE_NODE_IP),
                HOST_ID.`as`(T_NODE_HOST_ID),
                CLOUD_AREA_ID.`as`(T_NODE_CLOUD_AREA_ID)
            ).from(this)
                .where(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                .and(NODE_STATUS.notEqual(NodeStatus.NOT_IN_CMDB.name))
                .orderBy(NODE_ID.desc())
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun getCmdbNodesByNodeIdList(
        dslContext: DSLContext,
        nodeIdList: List<Long>
    ): Result<Record4<Long, String, Boolean, String>> {
        with(TNode.T_NODE) {
            return dslContext.select(
                NODE_ID.`as`(T_NODE_HOST_ID),
                NODE_IP.`as`(T_NODE_NODE_IP),
                AGENT_STATUS.`as`(T_NODE_AGENT_STATUS),
                AGENT_VERSION.`as`(T_NODE_AGENT_VERSION)
            ).from(this)
                .where(NODE_TYPE.eq(NodeType.CMDB.name))
                .and(NODE_ID.`in`(nodeIdList))
                .fetch()
        }
    }

    fun countCmdbNodes(dslContext: DSLContext): Int {
        with(TNode.T_NODE) {
            return dslContext.selectCount()
                .from(TNode.T_NODE)
                .where(NODE_TYPE.eq(NodeType.CMDB.name))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countDeployNodes(dslContext: DSLContext): Int {
        with(TNode.T_NODE) {
            return dslContext.selectCount()
                .from(TNode.T_NODE)
                .where(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countDeployNodesInCmdb(dslContext: DSLContext): Int {
        with(TNode.T_NODE) {
            return dslContext.selectCount()
                .from(TNode.T_NODE)
                .where(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                .and(NODE_STATUS.notEqual(NodeStatus.NOT_IN_CMDB.name))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun getCmdbNodes(
        dslContext: DSLContext,
        offset: Int,
        limit: Int
    ): Result<Record7<String, Long, String, String, Boolean, String, Long>> {
        with(TNode.T_NODE) {
            return dslContext.select(
                NODE_IP.`as`(T_NODE_NODE_IP),
                HOST_ID.`as`(T_NODE_HOST_ID),
                NODE_STATUS.`as`(T_NODE_NODE_STATUS),
                AGENT_VERSION.`as`(T_NODE_AGENT_VERSION),
                AGENT_STATUS.`as`(T_NODE_AGENT_STATUS),
                PROJECT_ID.`as`(T_NODE_PROJECT_ID),
                NODE_ID.`as`(T_NODE_NODE_ID)
            ).from(this)
                .where(NODE_TYPE.eq(NodeType.CMDB.name))
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun countDisplayNameEmptyNodes(dslContext: DSLContext): Int {
        with(TNode.T_NODE) {
            return dslContext.selectCount()
                .from(TNode.T_NODE)
                .where(DISPLAY_NAME.isNull)
                .or(DISPLAY_NAME.eq(""))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun getNodesWhoseDisplayNameIsEmpty(dslContext: DSLContext, offset: Int, limit: Int): Result<Record3<Long, String, String>> {
        with(TNode.T_NODE) {
            return dslContext.select(
                NODE_ID.`as`(T_NODE_NODE_ID),
                NODE_TYPE.`as`(T_NODE_NODE_TYPE),
                NODE_HASH_ID.`as`(T_NODE_NODE_HASH_ID)
            ).from(this)
                .where(DISPLAY_NAME.isNull)
                .or(DISPLAY_NAME.eq(""))
                .orderBy(NODE_ID.desc())
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun getNodesByNodeId(dslContext: DSLContext, nodeIdList: List<Long>): Result<Record2<Long, String>> {
        with(TNode.T_NODE) {
            return dslContext.select(
                NODE_ID.`as`(T_NODE_NODE_ID),
                DISPLAY_NAME.`as`(T_NODE_DISPLAY_NAME)
            ).from(this)
                .where(NODE_ID.`in`(nodeIdList))
                .fetch()
        }
    }

    fun updateNodeNotInCCByIp(dslContext: DSLContext, notInCCIpList: List<String>) {
        val hostIdDefault: Long? = null
        val cloudAreaIdDefault: Long? = null
        with(TNode.T_NODE) {
            dslContext.update(this)
                .set(NODE_STATUS, NodeStatus.NOT_IN_CC.name)
                .set(LAST_MODIFY_TIME, LocalDateTime.now())
                .set(HOST_ID, hostIdDefault)
                .set(CLOUD_AREA_ID, cloudAreaIdDefault)
                .where(NODE_IP.`in`(notInCCIpList))
                .and(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                .and(NODE_STATUS.notEqual(NodeStatus.NOT_IN_CC.name))
                .execute()
        }
    }

    fun updateNodeInCCByIp(dslContext: DSLContext, inCCIpList: List<String>) {
        with(TNode.T_NODE) {
            dslContext.update(this)
                .set(NODE_STATUS, NodeStatus.NORMAL.name)
                .set(LAST_MODIFY_TIME, LocalDateTime.now())
                .where(NODE_IP.`in`(inCCIpList))
                .and(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                .and(NODE_STATUS.notEqual(NodeStatus.NORMAL.name))
                .execute()
        }
    }

    fun updateNodeNotInCmdb(dslContext: DSLContext, ipList: List<String>) {
        val hostIdDefault: Long? = null
        val cloudAreaIdDefault: Long? = null
        with(TNode.T_NODE) {
            dslContext.update(this)
                .set(NODE_STATUS, NodeStatus.NOT_IN_CMDB.name)
                .set(LAST_MODIFY_TIME, LocalDateTime.now())
                .set(HOST_ID, hostIdDefault)
                .set(CLOUD_AREA_ID, cloudAreaIdDefault)
                .where(NODE_IP.`in`(ipList))
                .and(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                .and(NODE_STATUS.notEqual(NodeStatus.NOT_IN_CMDB.name))
                .execute()
        }
    }

    fun getCmdbNodesHostIdNullLimit(dslContext: DSLContext, offset: Int, limit: Int): Result<Record2<String, Long>> {
        with(TNode.T_NODE) {
            return dslContext.select(
                NODE_IP.`as`(T_NODE_NODE_IP),
                NODE_ID.`as`(T_NODE_NODE_ID)
            ).from(this)
                .where(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                .and(HOST_ID.isNull)
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun getNodesFromHostListByBkHostId(dslContext: DSLContext, projectId: String, hostList: List<Host>): MutableList<TNodeRecord> {
        val hostIdList = hostList.map { it.bkHostId }
        return with(TNode.T_NODE) {
            dslContext.selectFrom(this)
                .where(HOST_ID.`in`(hostIdList))
                .and(PROJECT_ID.eq(projectId))
                .fetch()
        }
    }

    fun getNodesFromHostListByBkHostId(dslContext: DSLContext, hostList: List<Host>): MutableList<TNodeRecord> {
        val hostIdList = hostList.map { it.bkHostId }
        return with(TNode.T_NODE) {
            dslContext.selectFrom(this)
                .where(HOST_ID.`in`(hostIdList))
                .fetch()
        }
    }

    fun getNodesFromHostListByIpAndBkCloudId(dslContext: DSLContext, projectId: String, hostList: List<Host>): List<TNodeRecord> {
        val ipList = hostList.map { it.ip }
        val ipToRecordMap = hostList.associateBy { it.ip }
        return with(TNode.T_NODE) {
            dslContext.selectFrom(this)
                .where(NODE_IP.`in`(ipList))
                .and(PROJECT_ID.eq(projectId))
                .fetch()
        }.filter {
            ipToRecordMap[it.nodeIp]?.bkCloudId == it.cloudAreaId
        }
    }

    fun getNodesByNodeHashIdList(
        dslContext: DSLContext,
        projectId: String,
        nodeHashIdList: List<String>
    ): Result<Record3<String, Long, Long>> {
        with(TNode.T_NODE) {
            return dslContext.select(
                NODE_IP.`as`(T_NODE_NODE_IP),
                HOST_ID.`as`(T_NODE_HOST_ID),
                CLOUD_AREA_ID.`as`(T_NODE_CLOUD_AREA_ID)
            ).from(this)
                .where(NODE_HASH_ID.`in`(nodeHashIdList))
                .and(PROJECT_ID.eq(projectId))
                .fetch()
        }
    }

    fun getEnvsByEnvHashIdList(dslContext: DSLContext, projectId: String, envHashIdList: List<String>): Result<Record1<Long>> {
        with(TEnv.T_ENV) {
            return dslContext.select(ENV_ID.`as`(T_ENV_ENV_ID)).from(this)
                .where(ENV_HASH_ID.`in`(envHashIdList))
                .and(PROJECT_ID.eq(projectId))
                .fetch()
        }
    }

    fun getNodeIdsByEnvIdList(dslContext: DSLContext, projectId: String, envIdList: List<Long>): Result<Record1<Long>> {
        with(TEnvNode.T_ENV_NODE) {
            return dslContext.select(NODE_ID.`as`(T_NODE_NODE_ID)).from(this)
                .where(ENV_ID.`in`(envIdList))
                .and(PROJECT_ID.eq(projectId))
                .fetch()
        }
    }

    fun getNodesByNodeIdList(dslContext: DSLContext, projectId: String, nodeIdList: List<Long>): Result<Record3<String, Long, Long>> {
        with(TNode.T_NODE) {
            return dslContext.select(
                NODE_IP.`as`(T_NODE_NODE_IP),
                HOST_ID.`as`(T_NODE_HOST_ID),
                CLOUD_AREA_ID.`as`(T_NODE_CLOUD_AREA_ID)
            ).from(this)
                .where(NODE_ID.`in`(nodeIdList))
                .and(PROJECT_ID.eq(projectId))
                .fetch()
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
        lastModifiedUser: String?
    ): List<TNodeRecord> {
        return with(TNode.T_NODE) {
            val query = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
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
                dslContext.insertInto(
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
                    AGENT_VERSION,
                    DISPLAY_NAME,
                    IMAGE,
                    TASK_ID,
                    LAST_MODIFY_TIME,
                    LAST_MODIFY_USER,
                    PIPELINE_REF_COUNT,
                    LAST_BUILD_TIME,
                    HOST_ID,
                    CLOUD_AREA_ID
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
                    it.agentVersion,
                    it.displayName,
                    it.image,
                    it.taskId,
                    now,
                    it.createdUser,
                    it.pipelineRefCount,
                    it.lastBuildTime,
                    it.hostId,
                    it.cloudAreaId
                ).returning(NODE_ID).fetchOne()!!.let { newRecord ->
                    val hashId = HashUtil.encodeLongId(newRecord.nodeId)
                    val displayName = it.nodeType + "-" + hashId + "-" + newRecord.nodeId
                    dslContext.update(this)
                        .set(NODE_HASH_ID, hashId)
                        .set(DISPLAY_NAME, displayName)
                        .where(NODE_ID.eq(newRecord.nodeId))
                        .execute()
                }
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
