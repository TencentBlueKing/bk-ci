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

package com.tencent.devops.environment.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.environment.agent.client.EsbAgentClient
import com.tencent.devops.common.environment.agent.pojo.agent.RawCmdbNode
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.constant.T_NODE_AGENT_STATUS
import com.tencent.devops.environment.constant.T_NODE_AGENT_VERSION
import com.tencent.devops.environment.constant.T_NODE_NODE_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_IP
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.ProjectConfigDao
import com.tencent.devops.environment.model.CreateNodeModel
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.CmdbNode
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.pojo.job.AddCmdbNodesRes
import com.tencent.devops.environment.pojo.job.AgentVersion
import com.tencent.devops.environment.pojo.job.NodeAgent
import com.tencent.devops.environment.pojo.job.ReImportCmdbNodeInfo
import com.tencent.devops.environment.pojo.job.UpdateTNodeInfo
import com.tencent.devops.environment.pojo.job.ccres.CCInfo
import com.tencent.devops.environment.pojo.job.ccres.CCResp
import com.tencent.devops.environment.pojo.job.ccres.QueryCCListHostWithoutBizData
import com.tencent.devops.environment.pojo.job.req.OpOperateReq
import com.tencent.devops.environment.service.job.OpService
import com.tencent.devops.environment.service.job.QueryAgentStatusService
import com.tencent.devops.environment.service.job.QueryFromCCService
import com.tencent.devops.environment.service.job.QueryFromCCService.Companion.FIELD_BK_HOST_ID
import com.tencent.devops.environment.service.job.QueryFromCCService.Companion.FIELD_BK_HOST_INNERIP
import com.tencent.devops.environment.utils.ImportServerNodeUtils
import com.tencent.devops.environment.utils.NodeStringIdUtils
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CmdbNodeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao,
    private val projectConfigDao: ProjectConfigDao,
    private val redisOperation: RedisOperation,
    private val esbAgentClient: EsbAgentClient,
    private val environmentPermissionService: EnvironmentPermissionService,
    private val queryFromCCService: QueryFromCCService,
    private val queryAgentStatusService: QueryAgentStatusService,
    private val opService: OpService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CmdbNodeService::class.java)

        const val FIELD_BK_SVR_ID = "svr_id"

        const val AGENT_ABNORMAL_NODE_STATUS = 0
        const val AGENT_NORMAL_NODE_STATUS = 1
        const val AGENT_NOT_INSTALLED_TAG = false
    }

    fun getUserCmdbNodesNew(
        userId: String,
        bakOperator: Boolean,
        page: Int,
        pageSize: Int,
        ips: List<String>
    ): Page<CmdbNode> {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val offset = sqlLimit.offset
        val limit = sqlLimit.limit

        val cmdbNodePage =
            ImportServerNodeUtils.getUserCmdbNodeNew(
                esbAgentClient = esbAgentClient,
                redisOperation = redisOperation,
                userId = userId,
                bakOperator = bakOperator,
                ips = ips,
                offset = offset,
                limit = limit
            )
        return Page(
            page = page,
            pageSize = pageSize,
            count = cmdbNodePage.totalRows.toLong(),
            records = cmdbNodePage.nodes.map {
                CmdbNode(
                    name = it.name,
                    operator = it.operator,
                    bakOperator = it.bakOperator,
                    ip = it.ip,
                    displayIp = it.displayIp,
                    agentStatus = it.agentStatus,
                    osName = it.osName
                )
            }
        )
    }

    /**
     * 重新导入
     * 将不在CC中的机器导入CC，直接update原来的记录
     */
    fun reImportCmdbNodes(
        userId: String,
        projectId: String,
        reImportCmdbNodeInfoList: List<ReImportCmdbNodeInfo>
    ): AddCmdbNodesRes {
        // 验证User是节点的主备负责人
        val nodeIpList = reImportCmdbNodeInfoList.map { it.nodeIp }
        val cmdbIpToNodeMap = checkUserOperator(userId, nodeIpList)

        // 将该节点添加到CC中
        val queryCCIpToCCInfoMap = addNodeToCC(cmdbIpToNodeMap)

        // update对应db记录的 NODE_STATUS 为 NORMAL，并写入 HOST_ID 和 CLOUD_AREA_ID
        val nodeIdList = reImportCmdbNodeInfoList.map { it.nodeId }
        val nodeIdToCCInfoMap = mutableMapOf<Long, CCInfo?>()
        reImportCmdbNodeInfoList.forEach {
            nodeIdToCCInfoMap[it.nodeId] = queryCCIpToCCInfoMap[it.nodeIp]
        }
        val nodeRecords = nodeDao.getCmdbNodesByNodeIdList(dslContext, nodeIdList)
        val ipToAgentVersionInfoMap = queryAgentStatusService.getAgentVersions(
            nodeIdToCCInfoMap.values.mapNotNull {
                AgentVersion(ip = it?.bkHostInnerip, bkHostId = it?.bkHostId)
            }
        )?.associateBy { it.ip }
        val agentStatusMap = ipToAgentVersionInfoMap.takeIf { !it.isNullOrEmpty() }.run {
            esbAgentClient.getAgentStatus(userId, ipToAgentVersionInfoMap!!.keys.filterNotNull().toList())
        }
        val opInfo = opService.operateOpProject("", OpOperateReq(2, listOf(projectId))).projGrayStatus?.get(0)
        val grayTag = projectId == opInfo?.englishName && true == opInfo.projGrayStatus
        val updateNodeInfo = nodeRecords.map {
            val nodeId = it[T_NODE_NODE_ID] as Long
            UpdateTNodeInfo(
                nodeId = nodeId,
                nodeStatus = NodeStatus.NORMAL.name,
                hostId = nodeIdToCCInfoMap[nodeId]?.bkHostId,
                cloudAreaId = nodeIdToCCInfoMap[nodeId]?.bkCloudId?.toLong(),
                agentStatus = if (grayTag) {
                    1 == ipToAgentVersionInfoMap?.get(it[T_NODE_NODE_IP] as String)?.status
                } else {
                    agentStatusMap[it[T_NODE_NODE_IP] as String] ?: false
                },
                agentVersion = if (grayTag)
                    ipToAgentVersionInfoMap?.get(it[T_NODE_NODE_IP] as String)?.version
                else null,
                lastModifyTime = LocalDateTime.now()
            )
        }
        nodeDao.batchUpdateCCInfo(dslContext, updateNodeInfo)
        return AddCmdbNodesRes(
            nodeStatus = true,
            nodesAgentList = nodeRecords.map {
                NodeAgent(
                    nodeIp = it[T_NODE_NODE_IP] as String,
                    nodesAgentStatus = if (it[T_NODE_AGENT_STATUS] as Boolean) 1 else 0,
                    nodesAgentVersion = it[T_NODE_AGENT_VERSION] as? String
                )
            },
            agentAbnormalNodesCount = nodeRecords.filterNot { it[T_NODE_AGENT_STATUS] as Boolean }.size,
            agentNotInstallNodesCount = 0
        )
    }

    /**
     * IDC测试机导入
     * 如果机器不存在，add一条新纪录
     */
    fun addCmdbNodes(userId: String, projectId: String, nodeIps: List<String>): AddCmdbNodesRes {
        // 验证 CMDB 节点IP和责任人
        val cmdbIpToNodeMap = checkUserOperator(userId, nodeIps)
        // 只添加不存在的节点
        val existIpList = nodeDao.listServerAndDevCloudNodes(dslContext, projectId) // 已存在 节点db记录
            .map { it.nodeIp }.toSet() // 已存在 节点ip
        val toAddIpList = nodeIps.filterNot { existIpList.contains(it) }.filterNot { it.isEmpty() } // 要添加的 节点ip
            .toSet() // 去重
        val toAddIpToCmdbNodeMap = cmdbIpToNodeMap.filter { toAddIpList.contains(it.key) } // 要添加的 节点ip - cmdb记录映射
        ImportServerNodeUtils.checkImportCount(
            dslContext = dslContext,
            projectConfigDao = projectConfigDao,
            nodeDao = nodeDao,
            projectId = projectId,
            userId = userId,
            toAddNodeCount = toAddIpList.size
        )
        val queryCCIpToCCInfoMap = addNodeToCC(toAddIpToCmdbNodeMap)

        val agentStatusMap = esbAgentClient.getAgentStatus(userId, toAddIpList)
        val ipAndHostIdList = queryCCIpToCCInfoMap.values.map {
            AgentVersion(ip = it.bkHostInnerip, bkHostId = it.bkHostId)
        }
        val agentVersionList = queryAgentStatusService.getAgentVersions(ipAndHostIdList)
        val ipToAgentVersionMap = agentVersionList?.associateBy { it.ip }
        if (logger.isDebugEnabled) logger.debug("[addCmdbNodes]ipToAgentVersionMap:$ipToAgentVersionMap")
        val toAddNodeList = toAddIpList.map {
            val cmdbNode = cmdbIpToNodeMap[it]!!
            val opInfo = opService.operateOpProject("", OpOperateReq(2, listOf(projectId))).projGrayStatus?.get(0)
            val grayTag = projectId == opInfo?.englishName && true == opInfo.projGrayStatus
            CreateNodeModel(
                nodeStringId = "",
                projectId = projectId,
                nodeIp = cmdbNode.ip,
                nodeName = cmdbNode.name,
                nodeStatus = if (AGENT_NOT_INSTALLED_TAG == ipToAgentVersionMap?.get(cmdbNode.ip)?.installedTag)
                    NodeStatus.NOT_INSTALLED.name
                else if (AGENT_ABNORMAL_NODE_STATUS == ipToAgentVersionMap?.get(cmdbNode.ip)?.status)
                    NodeStatus.ABNORMAL.name
                else if (AGENT_NORMAL_NODE_STATUS == ipToAgentVersionMap?.get(cmdbNode.ip)?.status)
                    NodeStatus.NORMAL.name
                else
                    "",
                nodeType = NodeType.CMDB.name,
                createdUser = userId,
                osName = cmdbNode.osName,
                operator = cmdbNode.operator,
                bakOperator = cmdbNode.bakOperator,
                agentStatus = if (grayTag) {
                    1 == ipToAgentVersionMap?.get(cmdbNode.ip)?.status
                } else {
                    agentStatusMap[cmdbNode.ip] ?: false
                },
                agentVersion = if (grayTag) ipToAgentVersionMap?.get(cmdbNode.ip)?.version else null,
                hostId = queryCCIpToCCInfoMap[cmdbNode.ip]?.bkHostId,
                cloudAreaId = queryCCIpToCCInfoMap[cmdbNode.ip]?.bkCloudId?.toLong()
            )
        }
        if (logger.isDebugEnabled) logger.debug("[addCmdbNodes]toAddNodeList:$toAddNodeList")

        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            nodeDao.batchAddNode(context, toAddNodeList)
            val insertedNodeList = nodeDao.listServerNodesByIps(
                dslContext = context,
                projectId = projectId,
                ips = toAddNodeList.map { it.nodeIp }
            )
            batchRegisterNodePermission(insertedNodeList = insertedNodeList, userId = userId, projectId = projectId)
        }
        return AddCmdbNodesRes(
            nodeStatus = true,
            nodesAgentList = toAddNodeList.map {
                NodeAgent(
                    nodeIp = it.nodeIp,
                    nodesAgentStatus = if (it.agentStatus) 1 else 0,
                    nodesAgentVersion = it.agentVersion
                )
            },
            agentAbnormalNodesCount = toAddNodeList.filterNot { it.agentStatus }.size,
            agentNotInstallNodesCount = 0
        )
    }

    /**
     * 用CMDB节点IP，验证用户是该机器的责任人（主备负责人）
     */
    private fun checkUserOperator(userId: String, nodeIps: List<String>): Map<String, RawCmdbNode> {
        val cmdbIpToNodeMap = esbAgentClient.getCmdbNodeByIps(userId, nodeIps).nodes // 所有ip对应记录
            .associateBy { it.ip } // ip - 记录 映射
        val invalidIps = nodeIps.filter { // 权限校验
            if (!cmdbIpToNodeMap.containsKey(it)) true
            else {
                val isOperator = cmdbIpToNodeMap[it]!!.operator == userId
                val isBakOpertor = cmdbIpToNodeMap[it]!!.bakOperator.split(";").contains(userId)
                !isOperator && !isBakOpertor
            }
        }
        if (invalidIps.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_IP_ILLEGAL_USER,
                params = arrayOf(invalidIps.joinToString(","))
            )
        }
        return cmdbIpToNodeMap
    }

    /**
     * 将节点添加到CC中
     * 返回值：无论在不在CC中的节点信息 CCInfo
     */
    private fun addNodeToCC(toAddIpToCmdbNodeMap: Map<String, RawCmdbNode>): Map<String?, CCInfo> {
        val serverIdToCmdbNodeMap = toAddIpToCmdbNodeMap.values.associateBy { it.serverId.toLong() }
        // 通过svrId查询节点是否在CC中
        val svrIdList = toAddIpToCmdbNodeMap.map { it.value.serverId.toLong() }
        if (logger.isDebugEnabled) logger.debug("[addCmdbNodes]svrIdList:$svrIdList")
        val (svrIdQueryCCRes, inCCSvrIdList, notInCCSvrIdList) = checkNodeInCCBySvrId(svrIdList)

        var queryCCIpToCCInfoMap = mapOf<String?, CCInfo>() // 在cc中，节点 ip-CCInfo 映射
        if (inCCSvrIdList.isNotEmpty()) { // 在CC中，通过svrId查出host_id（和云区域id，默认0，可默认）
            val ccData = svrIdQueryCCRes.data?.info
            queryCCIpToCCInfoMap = ccData!!.associateBy { it.bkHostInnerip }
        }
        if (logger.isDebugEnabled) logger.debug("[addCmdbNodes]queryCCIpToCCInfoMap:$queryCCIpToCCInfoMap")

        var addToCCIpToCCInfoMap = mapOf<String?, CCInfo>()
        if (notInCCSvrIdList.isNotEmpty()) { // 不在CC中，add到CC中，查出host_id和云区域id
            val addToCCResp = queryFromCCService.addHostToCiBiz(notInCCSvrIdList)
            if (logger.isDebugEnabled) logger.debug("[addCmdbNodes]addToCCResp:$addToCCResp")
            val ccHostIdList = addToCCResp.data?.bkHostIds // [11111,22222,33333,...]
            val addToCCInfoList = ccHostIdList?.mapIndexed { index, value ->
                CCInfo(
                    svrId = notInCCSvrIdList[index],
                    bkHostId = value,
                    bkHostInnerip = serverIdToCmdbNodeMap[notInCCSvrIdList[index]]?.ip
                )
            }
            if (logger.isDebugEnabled) logger.debug("[addCmdbNodes]addToCCInfo:$addToCCInfoList")
            addToCCIpToCCInfoMap = addToCCInfoList?.associateBy { it.bkHostInnerip } ?: mapOf()
            if (logger.isDebugEnabled) logger.debug("[addCmdbNodes]addToCCIpToCCInfoMap:$addToCCIpToCCInfoMap")
        }
        if (logger.isDebugEnabled) logger.debug("[addCmdbNodes]addToCCIpToCCInfoMap:$addToCCIpToCCInfoMap")

        return queryCCIpToCCInfoMap + addToCCIpToCCInfoMap
    }

    fun checkNodeInCCBySvrId(svrIdList: List<Long>):
        Triple<CCResp<QueryCCListHostWithoutBizData>, List<Long>, List<Long>> {
        val svrIdQueryCCRes = queryFromCCService.queryCCListHostWithoutBizByInRules(
            listOf(FIELD_BK_HOST_ID, FIELD_BK_HOST_INNERIP, FIELD_BK_SVR_ID), svrIdList, FIELD_BK_SVR_ID
        )
        if (logger.isDebugEnabled) logger.debug("[checkNodeInCCBySvrId]svrIdQueryCCRes:$svrIdQueryCCRes")
        val svrIdQueryCCList = svrIdQueryCCRes.data?.info // 所有在cc中的节点记录
        val svrIdToCCResMap = svrIdQueryCCList!!.associateBy { it.svrId } // cc中 svrId-节点记录 映射

        val inCCSvrIdList = mutableListOf<Long>() // 在CC中的节点的SvrId
        val notInCCSvrIdList = mutableListOf<Long>() // 不在CC中的节点的SvrId
        svrIdList.map {
            if (svrIdToCCResMap.containsKey(it.toLong())) inCCSvrIdList.add(it.toLong())
            else notInCCSvrIdList.add(it.toLong())
        }
        if (logger.isDebugEnabled) logger.debug("[checkNodeInCCBySvrId]inCCSvrIdList:$inCCSvrIdList")
        if (logger.isDebugEnabled) logger.debug("[checkNodeInCCBySvrId]notInCCSvrIdList:$notInCCSvrIdList")
        return Triple(svrIdQueryCCRes, inCCSvrIdList, notInCCSvrIdList)
    }

    private fun batchRegisterNodePermission(
        insertedNodeList: List<TNodeRecord>,
        userId: String,
        projectId: String
    ) {
        insertedNodeList.forEach {
            environmentPermissionService.createNode(
                userId = userId,
                projectId = projectId,
                nodeId = it.nodeId,
                nodeName = "${NodeStringIdUtils.getNodeStringId(it)}(${it.nodeIp})"
            )
        }
    }
}
