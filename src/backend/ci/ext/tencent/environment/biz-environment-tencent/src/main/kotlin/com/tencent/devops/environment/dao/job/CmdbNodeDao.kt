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
import com.tencent.devops.environment.constant.T_NODE_AGENT_STATUS
import com.tencent.devops.environment.constant.T_NODE_AGENT_VERSION
import com.tencent.devops.environment.constant.T_NODE_CLOUD_AREA_ID
import com.tencent.devops.environment.constant.T_NODE_CREATED_USER
import com.tencent.devops.environment.constant.T_NODE_HOST_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_IP
import com.tencent.devops.environment.constant.T_NODE_NODE_STATUS
import com.tencent.devops.environment.constant.T_NODE_NODE_TYPE
import com.tencent.devops.environment.constant.T_NODE_OS_TYPE
import com.tencent.devops.environment.constant.T_NODE_PROJECT_ID
import com.tencent.devops.environment.model.CreateNodeModel
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.pojo.job.AgentVersionInfo
import com.tencent.devops.environment.pojo.job.UpdateTNodeInfo
import com.tencent.devops.environment.pojo.job.jobreq.Host
import com.tencent.devops.environment.pojo.job.jobresp.CCUpdateInfo
import com.tencent.devops.model.environment.tables.TNode
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Record3
import org.jooq.Record4
import org.jooq.Record5
import org.jooq.Record6
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class CmdbNodeDao {
    fun getCmdbNodesByIpAndProjectId(
        dslContext: DSLContext,
        projectId: String,
        nodeIpList: List<String>
    ): Result<Record3<Long, String, String>> {
        with(TNode.T_NODE) {
            return dslContext.select(
                NODE_ID.`as`(T_NODE_NODE_ID),
                NODE_IP.`as`(T_NODE_NODE_IP),
                NODE_STATUS.`as`(T_NODE_NODE_STATUS)
            ).from(this)
                .where(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                .and(NODE_IP.`in`(nodeIpList))
                .and(PROJECT_ID.eq(projectId))
                .fetch()
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
            val batchUpdate = dslContext.batch(
                dslContext.update(this)
                    .set(NODE_STATUS, nodeStatus)
                    .set(AGENT_VERSION, nodeAgentVersion)
                    .set(TASK_ID, jobId)
                    .set(LAST_MODIFY_TIME, LocalDateTime.now())
                    .where(NODE_IP.`in`(nodeIpList))
                    .and(NODE_TYPE.eq(NodeType.CMDB.name))
            )
            batchUpdate.execute()
        }
    }

    fun updateBuildAgentVersionByNodeId(
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

    fun updateHostIdAndCloudAreaIdByNodeId(
        dslContext: DSLContext,
        nodeCCUpdateInfoList: List<CCUpdateInfo>
    ) {
        with(TNode.T_NODE) {
            val batchUpdate = dslContext.batch(
                nodeCCUpdateInfoList.map {
                    dslContext.update(this)
                        .set(HOST_ID, it.bkHostId)
                        .set(CLOUD_AREA_ID, it.bkCloudId)
                        .set(OS_TYPE, it.osType)
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

    fun getDeployNodesLimit(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int
    ): Result<Record5<Long, String, String, Long, Long>> {
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
                .limit(pageSize).offset((page - 1) * pageSize)
                .fetch()
        }
    }

    fun getDeployNodesInCmdbLimit(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int
    ): Result<Record6<Long, String, String, Long, Long, String>> {
        with(TNode.T_NODE) {
            return dslContext.select(
                NODE_ID.`as`(T_NODE_NODE_ID),
                NODE_TYPE.`as`(T_NODE_NODE_TYPE),
                NODE_IP.`as`(T_NODE_NODE_IP),
                HOST_ID.`as`(T_NODE_HOST_ID),
                CLOUD_AREA_ID.`as`(T_NODE_CLOUD_AREA_ID),
                OS_TYPE.`as`(T_NODE_OS_TYPE)
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
    ): Result<Record4<Long, String, Boolean, String>> {
        with(TNode.T_NODE) {
            return dslContext.select(
                NODE_ID.`as`(T_NODE_NODE_ID),
                NODE_IP.`as`(T_NODE_NODE_IP),
                AGENT_STATUS.`as`(T_NODE_AGENT_STATUS),
                AGENT_VERSION.`as`(T_NODE_AGENT_VERSION)
            ).from(this)
                .where(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
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
        page: Int,
        pageSize: Int
    ): Result<Record6<String, Long, String, String, String, Long>> {
        with(TNode.T_NODE) {
            return dslContext.select(
                NODE_IP.`as`(T_NODE_NODE_IP),
                HOST_ID.`as`(T_NODE_HOST_ID),
                NODE_STATUS.`as`(T_NODE_NODE_STATUS),
                AGENT_VERSION.`as`(T_NODE_AGENT_VERSION),
                PROJECT_ID.`as`(T_NODE_PROJECT_ID),
                NODE_ID.`as`(T_NODE_NODE_ID)
            ).from(this)
                .where(NODE_TYPE.eq(NodeType.CMDB.name))
                .limit(pageSize).offset((page - 1) * pageSize)
                .fetch()
        }
    }

    fun updateNodeNotInCCByIp(dslContext: DSLContext, notInCCIpList: List<String>) {
        val hostIdDefault: Long? = null
        val cloudAreaIdDefault: Long? = null
        val osTypeDefault: String? = null
        val agentVersionDefault: String? = null
        with(TNode.T_NODE) {
            dslContext.update(this)
                .set(NODE_STATUS, NodeStatus.NOT_IN_CC.name)
                .set(HOST_ID, hostIdDefault)
                .set(CLOUD_AREA_ID, cloudAreaIdDefault)
                .set(OS_TYPE, osTypeDefault)
                .set(AGENT_VERSION, agentVersionDefault)
                .where(NODE_IP.`in`(notInCCIpList))
                .and(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                .and(NODE_STATUS.notEqual(NodeStatus.NOT_IN_CC.name))
                .execute()
        }
    }

    fun updateNodeInCCByIp(dslContext: DSLContext, ipToNodeStatus: Map<String, String>) {
        val agentVersionDefault: String? = null
        with(TNode.T_NODE) {
            val batchUpdate = dslContext.batch(
                ipToNodeStatus.map { (ip, nodeStatus) ->
                    val updateInfo = dslContext.update(this)
                        .set(NODE_STATUS, nodeStatus)
                    if (NodeStatus.NOT_INSTALLED.name == nodeStatus) {
                        updateInfo.set(AGENT_VERSION, agentVersionDefault)
                    }
                    updateInfo.where(NODE_IP.eq(ip))
                        .and(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                }
            )
            batchUpdate.execute()
        }
    }

    fun updateNodeInCCByHostId(dslContext: DSLContext, hostIdToNodeStatus: Map<Long, String>) {
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

    fun updateNodeNotInCmdb(dslContext: DSLContext, ipList: List<String>) {
        val hostIdDefault: Long? = null
        val cloudAreaIdDefault: Long? = null
        with(TNode.T_NODE) {
            dslContext.update(this)
                .set(NODE_STATUS, NodeStatus.NOT_IN_CMDB.name)
                .set(HOST_ID, hostIdDefault)
                .set(CLOUD_AREA_ID, cloudAreaIdDefault)
                .where(NODE_IP.`in`(ipList))
                .and(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                .and(NODE_STATUS.notEqual(NodeStatus.NOT_IN_CMDB.name))
                .execute()
        }
    }

    fun getCmdbNodesHostIdNullLimit(dslContext: DSLContext, page: Int, pageSize: Int): Result<Record2<String, Long>> {
        with(TNode.T_NODE) {
            return dslContext.select(
                NODE_IP.`as`(T_NODE_NODE_IP),
                NODE_ID.`as`(T_NODE_NODE_ID)
            ).from(this)
                .where(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.UNKNOWN.name, NodeType.OTHER.name))
                .and(HOST_ID.isNull)
                .limit(pageSize).offset((page - 1) * pageSize)
                .fetch()
        }
    }

    fun getNodesFromHostListByBkHostId(
        dslContext: DSLContext,
        projectId: String,
        hostList: List<Host>
    ): List<Record5<Long, String, Long, Long, String>> {
        val hostIdList = hostList.map { it.bkHostId }
        return with(TNode.T_NODE) {
            val nodeRecord = dslContext.select(
                NODE_ID.`as`(T_NODE_NODE_ID),
                NODE_IP.`as`(T_NODE_NODE_IP),
                HOST_ID.`as`(T_NODE_HOST_ID),
                CLOUD_AREA_ID.`as`(T_NODE_CLOUD_AREA_ID),
                CREATED_USER.`as`(T_NODE_CREATED_USER)
            ).from(this)
                .where(HOST_ID.`in`(hostIdList))
                .and(PROJECT_ID.eq(projectId))
                .fetch()
            nodeRecord.mapNotNull { it }
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

    fun getNodesFromHostListByIpAndBkCloudId(
        dslContext: DSLContext,
        projectId: String,
        hostList: List<Host>
    ): List<Record5<Long, String, Long, Long, String>> {
        val ipList = hostList.map { it.ip }
        val ipToRecordMap = hostList.associateBy { it.ip }
        return with(TNode.T_NODE) {
            val nodeRecord = dslContext.select(
                NODE_ID.`as`(T_NODE_NODE_ID),
                NODE_IP.`as`(T_NODE_NODE_IP),
                HOST_ID.`as`(T_NODE_HOST_ID),
                CLOUD_AREA_ID.`as`(T_NODE_CLOUD_AREA_ID),
                CREATED_USER.`as`(T_NODE_CREATED_USER)
            ).from(this)
                .where(NODE_IP.`in`(ipList))
                .and(PROJECT_ID.eq(projectId))
                .fetch()
            nodeRecord.mapNotNull {
                if (ipToRecordMap[it[T_NODE_NODE_IP] as? String]?.bkCloudId == it[T_NODE_CLOUD_AREA_ID] as? Long) {
                    it
                } else null
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

    fun batchAddNode(dslContext: DSLContext, nodes: List<CreateNodeModel>) {
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
                        OS_TYPE
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
                        it.osType
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

    fun countImportNode(dslContext: DSLContext, projectId: String): Int {
        with(TNode.T_NODE) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_TYPE.`in`(NodeType.CMDB.name, NodeType.OTHER.name))
                .fetchOne(0, Int::class.java)!!
        }
    }
}