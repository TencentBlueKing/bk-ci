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

package com.tencent.devops.environment.dao.job

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.environment.constant.T_NODE_AGENT_VERSION
import com.tencent.devops.environment.constant.T_NODE_CLOUD_AREA_ID
import com.tencent.devops.environment.constant.T_NODE_HOST_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_IP
import com.tencent.devops.environment.constant.T_NODE_NODE_STATUS
import com.tencent.devops.environment.constant.T_NODE_NODE_TYPE
import com.tencent.devops.environment.constant.T_NODE_OS_TYPE
import com.tencent.devops.environment.constant.T_NODE_PROJECT_ID
import com.tencent.devops.environment.constant.T_NODE_SERVER_ID
import com.tencent.devops.environment.model.CreateNodeModel
import com.tencent.devops.environment.pojo.dto.CmdbNodeDTO
import com.tencent.devops.environment.pojo.dto.CmdbNodeStatusDTO
import com.tencent.devops.environment.pojo.dto.NodeUpdateAttrDTO
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.pojo.job.AgentVersionInfo
import com.tencent.devops.environment.pojo.job.UpdateTNodeInfo
import com.tencent.devops.environment.pojo.job.jobreq.Host
import com.tencent.devops.environment.pojo.job.jobresp.NodeAttr
import com.tencent.devops.model.environment.tables.TNode
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Record3
import org.jooq.Record5
import org.jooq.Record7
import org.jooq.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class CmdbNodeDao @Autowired constructor(
    private val defaultDSLContext: DSLContext
) {

    private val table = TNode.T_NODE
    // -------------------------------batch update node record(s)-------------------------------

    fun batchUpdateNodeSeverIdByIp(
        dslContext: DSLContext,
        nodeIpToServerIdMap: Map<String, Long?>
    ) {
        with(TNode.T_NODE) {
            val batchUpdate = dslContext.batch(
                nodeIpToServerIdMap.map { (ip, serverId) ->
                    dslContext.update(this)
                        .set(SERVER_ID, serverId)
                        .set(SYSTEM_UPDATE_TIME, LocalDateTime.now())
                        .where(NODE_IP.eq(ip))
                        .and(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                }
            )
            batchUpdate.execute()
        }
    }

    fun batchUpdateBuildAgentVersionByNodeId(
        dslContext: DSLContext,
        buildNodeAgentVersionInfoList: List<AgentVersionInfo>
    ) {
        with(TNode.T_NODE) {
            val batchUpdate = dslContext.batch(
                buildNodeAgentVersionInfoList.map {
                    dslContext.update(this)
                        .set(AGENT_VERSION, it.agentVersion)
                        .where(NODE_ID.eq(it.nodeId))
                }
            )
            batchUpdate.execute()
        }
    }

    fun batchUpdateHostIdAndCloudAreaIdByNodeId(
        nodeNodeAttrList: List<NodeAttr>
    ): Int {
        with(TNode.T_NODE) {
            val batchUpdate = defaultDSLContext.batch(
                nodeNodeAttrList.map {
                    defaultDSLContext.update(this)
                        .set(HOST_ID, it.bkHostId)
                        .set(CLOUD_AREA_ID, it.bkCloudId)
                        .set(OS_TYPE, it.osType)
                        .set(SYSTEM_UPDATE_TIME, LocalDateTime.now())
                        .where(NODE_ID.eq(it.nodeId))
                }
            )
            val affectedNumArr = batchUpdate.execute()
            return affectedNumArr.sum()
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
                        .set(SYSTEM_UPDATE_TIME, LocalDateTime.now())
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
                        .set(HOST_ID, it.hostId)
                        .set(CLOUD_AREA_ID, it.cloudAreaId)
                        .set(OS_TYPE, it.osType)
                        .set(LAST_MODIFY_TIME, LocalDateTime.now())
                        .where(NODE_ID.eq(it.nodeId))
                }
            )
            batchUpdate.execute()
        }
    }

    fun batchUpdateCCInfoByServerId(dslContext: DSLContext, updateAgentInfo: List<UpdateTNodeInfo>) {
        with(TNode.T_NODE) {
            val batchUpdate = dslContext.batch(
                updateAgentInfo.map {
                    dslContext.update(this)
                        .set(NODE_STATUS, it.nodeStatus)
                        .set(AGENT_STATUS, it.agentStatus)
                        .set(AGENT_VERSION, it.agentVersion)
                        .set(HOST_ID, it.hostId)
                        .set(CLOUD_AREA_ID, it.cloudAreaId)
                        .set(OS_TYPE, it.osType)
                        .set(LAST_MODIFY_TIME, LocalDateTime.now())
                        .where(SERVER_ID.eq(it.serverId))
                }
            )
            batchUpdate.execute()
        }
    }

    fun batchUpdateNodeInCCByServerId(dslContext: DSLContext, serverIdToNodeStatus: Map<Long, String>) {
        val agentVersionDefault: String? = null
        with(TNode.T_NODE) {
            val batchUpdate = dslContext.batch(
                serverIdToNodeStatus.map { (serverId, nodeStatus) ->
                    val updateInfo = dslContext.update(this)
                        .set(NODE_STATUS, nodeStatus)
                        .set(SYSTEM_UPDATE_TIME, LocalDateTime.now())
                    if (NodeStatus.NOT_INSTALLED.name == nodeStatus) {
                        updateInfo.set(AGENT_VERSION, agentVersionDefault)
                    }
                    updateInfo.where(SERVER_ID.eq(serverId))
                        .and(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                }
            )
            batchUpdate.execute()
        }
    }

    fun batchUpdateNodeMaintainerAndOsNameByNodeIp(nodeAttrList: List<NodeUpdateAttrDTO>) {
        with(TNode.T_NODE) {
            val batchUpdate = defaultDSLContext.batch(
                nodeAttrList.map {
                    defaultDSLContext.update(this)
                        .set(SERVER_ID, it.serverId)
                        .set(OPERATOR, it.operator)
                        .set(BAK_OPERATOR, it.bakOperator)
                        .set(OS_NAME, it.osName)
                        .set(SYSTEM_UPDATE_TIME, LocalDateTime.now())
                        .where(NODE_IP.eq(it.nodeIp))
                        .and(buildCmdbNodeTypeCondition())
                }
            )
            batchUpdate.execute()
        }
    }

    fun batchUpdateNodeInCCByHostId(dslContext: DSLContext, hostIdToNodeStatus: Map<Long, String>) {
        val agentVersionDefault: String? = null
        with(TNode.T_NODE) {
            val batchUpdate = dslContext.batch(
                hostIdToNodeStatus.map { (hostId, nodeStatus) ->
                    val updateInfo = dslContext.update(this)
                        .set(NODE_STATUS, nodeStatus)
                    if (NodeStatus.NOT_INSTALLED.name == nodeStatus) {
                        updateInfo.set(AGENT_VERSION, agentVersionDefault)
                    }
                    updateInfo.where(HOST_ID.eq(hostId))
                        .and(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                }
            )
            batchUpdate.execute()
        }
    }

    // -------------------------------update node record(s)-------------------------------

    /**
     * 将在CMDB中但被误更新为NOT_IN_CMDB的节点对应状态改为NOT_IN_CC
     * @param nodeIps 在CMDB中但节点状态被误更新为NOT_IN_CMDB的节点IP集合
     */
    fun updateNotInCmdbToNotInCCByNodeIp(nodeIps: Collection<String>): Int {
        if (nodeIps.isEmpty()) {
            return 0
        }
        with(TNode.T_NODE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(NODE_IP.`in`(nodeIps))
            conditions.add(NODE_STATUS.eq(NodeStatus.NOT_IN_CMDB.name))
            conditions.add(buildCmdbNodeTypeCondition())
            return defaultDSLContext.update(this)
                .set(NODE_STATUS, NodeStatus.NOT_IN_CC.name)
                .set(SYSTEM_UPDATE_TIME, LocalDateTime.now())
                .where(conditions)
                .execute()
        }
    }

    fun updateNodeNotInCCByServerId(dslContext: DSLContext, notInCCServerIdList: List<Long>) {
        val hostIdDefault: Long? = null
        val cloudAreaIdDefault: Long? = null
        val agentVersionDefault: String? = null
        with(TNode.T_NODE) {
            dslContext.update(this)
                .set(NODE_STATUS, NodeStatus.NOT_IN_CC.name)
                .set(HOST_ID, hostIdDefault)
                .set(CLOUD_AREA_ID, cloudAreaIdDefault)
                .set(AGENT_VERSION, agentVersionDefault)
                .set(SYSTEM_UPDATE_TIME, LocalDateTime.now())
                .where(SERVER_ID.`in`(notInCCServerIdList))
                .and(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                .and(NODE_STATUS.notEqual(NodeStatus.NOT_IN_CC.name))
                .execute()
        }
    }

    fun updateNodeStatusByNodeIp(
        dslContext: DSLContext,
        nodeIpList: List<String>,
        nodeStatus: String,
        nodeAgentVersion: String?,
        jobId: Long?
    ) {
        with(TNode.T_NODE) {
            dslContext.update(this)
                .set(NODE_STATUS, nodeStatus)
                .set(AGENT_VERSION, nodeAgentVersion)
                .set(TASK_ID, jobId)
                .set(LAST_MODIFY_TIME, LocalDateTime.now())
                .where(NODE_IP.`in`(nodeIpList))
                .and(NODE_TYPE.eq(NodeType.CMDB.name))
                .execute()
        }
    }

    fun updateNodeStatusNotInCmdbByNodeIp(nodeIps: Collection<String>): Int {
        with(TNode.T_NODE) {
            return defaultDSLContext.update(this)
                .set(NODE_STATUS, NodeStatus.NOT_IN_CMDB.name)
                .set(SYSTEM_UPDATE_TIME, LocalDateTime.now())
                .where(NODE_IP.`in`(nodeIps))
                .and(buildCmdbNodeTypeCondition())
                .and(NODE_STATUS.notEqual(NodeStatus.NOT_IN_CMDB.name))
                .execute()
        }
    }

    // -------------------------------count node record(s)-------------------------------

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

    fun countDeployNodesServerIdNull(dslContext: DSLContext): Int {
        with(TNode.T_NODE) {
            return dslContext.selectCount()
                .from(TNode.T_NODE)
                .where(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                .and(SERVER_ID.isNull)
                .and(NODE_STATUS.notEqual(NodeStatus.NOT_IN_CMDB.name))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countNodeInCmdb(): Int {
        with(TNode.T_NODE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(buildCmdbNodeTypeCondition())
            conditions.add(NODE_STATUS.notEqual(NodeStatus.NOT_IN_CMDB.name))
            return defaultDSLContext.selectCount()
                .from(TNode.T_NODE)
                .where(conditions)
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countImportNode(dslContext: DSLContext, projectId: String): Int {
        with(TNode.T_NODE) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countAllNodesOrByName(dslContext: DSLContext, name: String?): Int {
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

    // -------------------------------batch insert node(s)-------------------------------

    fun batchInsertNode(dslContext: DSLContext, nodes: List<CreateNodeModel>) {
        if (nodes.isEmpty()) return
        val now = LocalDateTime.now()
        with(TNode.T_NODE) {
            val batchInsert = dslContext.batch(
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
                        CLOUD_AREA_ID,
                        OS_TYPE,
                        SERVER_ID
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
                        it.cloudAreaId,
                        it.osType,
                        it.serverId
                    ).returning(NODE_ID).fetchOne()!!.let { newRecord ->
                        val hashId = HashUtil.encodeLongId(newRecord.nodeId)
                        val displayName = it.nodeType + "-" + hashId + "-" + newRecord.nodeId
                        dslContext.update(this)
                            .set(NODE_HASH_ID, hashId)
                            .set(DISPLAY_NAME, displayName)
                            .where(NODE_ID.eq(newRecord.nodeId))
                    }
                }
            )
            batchInsert.execute()
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

    // -------------------------------get node record(s)-------------------------------

    fun listCmdbNodeStatusByProjectIdAndServerId(
        projectId: String,
        serverIds: Collection<Long>
    ): List<CmdbNodeStatusDTO> {
        with(TNode.T_NODE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(buildCmdbNodeTypeCondition())
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(SERVER_ID.`in`(serverIds))
            val records = defaultDSLContext.select(
                NODE_ID,
                NODE_IP,
                SERVER_ID,
                NODE_STATUS
            ).from(this)
                .where(conditions)
                .fetch()
            return records.map { record ->
                CmdbNodeStatusDTO(
                    record.get(NODE_ID),
                    record.get(NODE_IP),
                    record.get(SERVER_ID),
                    record.get(NODE_STATUS)
                )
            }
        }
    }

    /**
     * 从传入的IP列表中筛选出状态为不在CMDB中（NOT_IN_CMDB）的IP
     */
    fun listNotInCmdbIps(nodeIps: Collection<String>): List<String> {
        with(TNode.T_NODE) {
            val conditons = buildBasicNodeIpConditions(nodeIps)
            conditons.add(NODE_STATUS.eq(NodeStatus.NOT_IN_CMDB.name))
            val records = defaultDSLContext.select(
                NODE_IP
            ).from(this)
                .where(conditons)
                .orderBy(NODE_ID.desc())
                .fetch()
            return records.map { record -> record.get(NODE_IP) }
        }
    }

    /**
     * 从传入的IP列表中筛选出状态为在CMDB中（非NOT_IN_CMDB）的IP
     */
    fun listInCmdbIps(nodeIps: Collection<String>): List<String> {
        with(TNode.T_NODE) {
            val conditons = buildBasicNodeIpConditions(nodeIps)
            conditons.add(NODE_STATUS.notEqual(NodeStatus.NOT_IN_CMDB.name))
            val records = defaultDSLContext.select(
                NODE_IP
            ).from(this)
                .where(conditons)
                .orderBy(NODE_ID.desc())
                .fetch()
            return records.map { record -> record.get(NODE_IP) }
        }
    }

    private fun buildBasicNodeIpConditions(nodeIps: Collection<String>): MutableList<Condition> {
        val conditons = mutableListOf<Condition>()
        conditons.add(buildCmdbNodeTypeCondition())
        conditons.add(table.NODE_IP.`in`(nodeIps))
        return conditons
    }

    fun listCmdbNodes(
        page: Int,
        pageSize: Int
    ): List<CmdbNodeDTO> {
        with(TNode.T_NODE) {
            val records = defaultDSLContext.select(
                NODE_ID,
                NODE_IP,
                SERVER_ID,
                OPERATOR,
                BAK_OPERATOR,
                OS_NAME
            ).from(this)
                .where(buildCmdbNodeTypeCondition())
                .orderBy(NODE_ID.desc())
                .limit(pageSize).offset((page - 1) * pageSize)
                .fetch()
            return records.map { record ->
                CmdbNodeDTO(
                    nodeId = record.get(table.NODE_ID),
                    nodeIp = record.get(table.NODE_IP),
                    serverId = record.get(table.SERVER_ID),
                    operator = record.get(table.OPERATOR),
                    bakOperator = record.get(table.BAK_OPERATOR),
                    osName = record.get(table.OS_NAME)
                )
            }
        }
    }

    private fun buildCmdbNodeTypeCondition(): Condition {
        return table.NODE_TYPE.`in`(NodeType.CMDB.name)
    }

    fun getDeployNodesInCmdbLimit(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int
    ): Result<Record7<Long, String, String, Long, Long, String, Long>> {
        with(TNode.T_NODE) {
            return dslContext.select(
                NODE_ID.`as`(T_NODE_NODE_ID),
                NODE_TYPE.`as`(T_NODE_NODE_TYPE),
                NODE_IP.`as`(T_NODE_NODE_IP),
                HOST_ID.`as`(T_NODE_HOST_ID),
                CLOUD_AREA_ID.`as`(T_NODE_CLOUD_AREA_ID),
                OS_TYPE.`as`(T_NODE_OS_TYPE),
                SERVER_ID.`as`(T_NODE_SERVER_ID)
            ).from(this)
                .where(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                .and(NODE_STATUS.notEqual(NodeStatus.NOT_IN_CMDB.name))
                .orderBy(NODE_ID.desc())
                .limit(pageSize).offset((page - 1) * pageSize)
                .fetch()
        }
    }

    fun getCmdbNodesByNodeIdList(
        dslContext: DSLContext,
        nodeIdList: List<Long>
    ): Result<Record5<Long, String, String, String, Long>> {
        with(TNode.T_NODE) {
            return dslContext.select(
                NODE_ID.`as`(T_NODE_NODE_ID),
                NODE_IP.`as`(T_NODE_NODE_IP),
                NODE_STATUS.`as`(T_NODE_NODE_STATUS),
                AGENT_VERSION.`as`(T_NODE_AGENT_VERSION),
                SERVER_ID.`as`(T_NODE_SERVER_ID)
            ).from(this)
                .where(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                .and(NODE_ID.`in`(nodeIdList))
                .fetch()
        }
    }

    fun getDeployNodesServerIdNullLimit(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int
    ): Result<Record2<Long, String>> {
        with(TNode.T_NODE) {
            return dslContext.select(
                NODE_ID.`as`(T_NODE_NODE_ID),
                NODE_IP.`as`(T_NODE_NODE_IP)
            ).from(this)
                .where(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                .and(SERVER_ID.isNull)
                .and(NODE_STATUS.notEqual(NodeStatus.NOT_IN_CMDB.name))
                .orderBy(NODE_ID.desc())
                .limit(pageSize).offset((page - 1) * pageSize)
                .fetch()
        }
    }

    fun getCmdbNodesHostIdNull(
        page: Int,
        pageSize: Int
    ): List<CmdbNodeDTO> {
        with(TNode.T_NODE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(buildCmdbNodeTypeCondition())
            conditions.add(SERVER_ID.isNotNull)
            conditions.add(HOST_ID.isNull)
            val records = defaultDSLContext.select(
                NODE_ID,
                NODE_IP,
                SERVER_ID
            ).from(this)
                .where(conditions)
                .offset((page - 1) * pageSize)
                .limit(pageSize)
                .fetch()
            return records.map {
                CmdbNodeDTO(
                    nodeId = it.get(NODE_ID),
                    nodeIp = it.get(NODE_IP),
                    serverId = it.get(SERVER_ID)
                )
            }
        }
    }

    fun getCmdbNodes(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int
    ): Result<Record7<String, Long, String, String, String, Long, Long>> {
        with(TNode.T_NODE) {
            return dslContext.select(
                NODE_IP.`as`(T_NODE_NODE_IP),
                HOST_ID.`as`(T_NODE_HOST_ID),
                NODE_STATUS.`as`(T_NODE_NODE_STATUS),
                AGENT_VERSION.`as`(T_NODE_AGENT_VERSION),
                PROJECT_ID.`as`(T_NODE_PROJECT_ID),
                NODE_ID.`as`(T_NODE_NODE_ID),
                SERVER_ID.`as`(T_NODE_SERVER_ID)
            ).from(this)
                .where(NODE_TYPE.eq(NodeType.CMDB.name))
                .limit(pageSize).offset((page - 1) * pageSize)
                .fetch()
        }
    }

    fun listCmdbNodesByHostIds(
        projectId: String,
        hostIds: Collection<Long>
    ): List<CmdbNodeDTO> {
        return with(TNode.T_NODE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(buildCmdbNodeTypeCondition())
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(HOST_ID.`in`(hostIds))
            val records = defaultDSLContext.select(
                NODE_ID,
                NODE_IP,
                SERVER_ID,
                CLOUD_AREA_ID,
                HOST_ID,
                CREATED_USER
            ).from(this)
                .where(conditions)
                .fetch()
            records.mapNotNull { record ->
                CmdbNodeDTO(
                    nodeId = record.get(table.NODE_ID),
                    nodeIp = record.get(table.NODE_IP),
                    serverId = record.get(table.SERVER_ID),
                    cloudAreaId = record.get(table.CLOUD_AREA_ID),
                    hostId = record.get(table.HOST_ID),
                    createdUser = record.get(table.CREATED_USER)
                )
            }
        }
    }

    fun getNodesFromHostListByBkHostId(dslContext: DSLContext, hostList: List<Host>): Result<Record2<Long, Long>> {
        val hostIdList = hostList.map { it.bkHostId }
        return with(TNode.T_NODE) {
            dslContext.select(
                NODE_ID.`as`(T_NODE_NODE_ID),
                HOST_ID.`as`(T_NODE_HOST_ID)
            ).from(this)
                .where(HOST_ID.`in`(hostIdList))
                .fetch()
        }
    }

    fun listCmdbNodesByIps(
        projectId: String,
        ips: Collection<String>
    ): List<CmdbNodeDTO> {
        return with(TNode.T_NODE) {
            val nodeRecord = defaultDSLContext.select(
                NODE_ID,
                NODE_IP,
                SERVER_ID,
                CLOUD_AREA_ID,
                HOST_ID,
                CREATED_USER
            ).from(this)
                .where(NODE_IP.`in`(ips))
                .and(PROJECT_ID.eq(projectId))
                .fetch()
            nodeRecord.mapNotNull { record ->
                CmdbNodeDTO(
                    nodeId = record.get(table.NODE_ID),
                    nodeIp = record.get(table.NODE_IP),
                    serverId = record.get(table.SERVER_ID),
                    cloudAreaId = record.get(table.CLOUD_AREA_ID),
                    hostId = record.get(table.HOST_ID),
                    createdUser = record.get(table.CREATED_USER)
                )
            }
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

    fun getNodesByNodeIdList(
        dslContext: DSLContext,
        projectId: String,
        nodeIdList: List<Long>
    ): Result<Record3<String, Long, Long>> {
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

    fun getNodeByProjectIdAndServerIdList(
        dslContext: DSLContext,
        projectId: String,
        serverIdList: List<Long>
    ): Result<Record3<Long, Long, String>> {
        with(TNode.T_NODE) {
            return dslContext.select(
                NODE_ID.`as`(T_NODE_NODE_ID),
                SERVER_ID.`as`(T_NODE_SERVER_ID),
                NODE_IP.`as`(T_NODE_NODE_IP)
            ).from(this)
                .where(SERVER_ID.`in`(serverIdList))
                .and(PROJECT_ID.eq(projectId))
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

    fun listAllNodes(dslContext: DSLContext): Result<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .fetch()
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

    fun getNodeHostIdByCloudIp(projectId: String, cloudAreaId: Int, ip: String): List<Long> {
        with(TNode.T_NODE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(NODE_IP.eq(ip))
            conditions.add(CLOUD_AREA_ID.eq(cloudAreaId.toLong()))
            conditions.add(PROJECT_ID.eq(projectId))
            val records = defaultDSLContext.select(HOST_ID).from(this).where(conditions).fetch()
            return records.map { record ->
                record.get(HOST_ID)
            }
        }
    }
}
