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

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.environment.agent.client.EsbAgentClient
import com.tencent.devops.common.environment.agent.pojo.agent.RawCmdbNode
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.constant.Constants.FIELD_BK_CLOUD_ID
import com.tencent.devops.environment.constant.Constants.FIELD_BK_HOST_ID
import com.tencent.devops.environment.constant.Constants.FIELD_BK_HOST_INNERIP
import com.tencent.devops.environment.constant.Constants.FIELD_BK_OS_TYPE
import com.tencent.devops.environment.constant.Constants.FIELD_BK_SVR_ID
import com.tencent.devops.environment.constant.Constants.OS_TYPE_CC_CODE_AIX
import com.tencent.devops.environment.constant.Constants.OS_TYPE_CC_CODE_FREEBSD
import com.tencent.devops.environment.constant.Constants.OS_TYPE_CC_CODE_LINUX
import com.tencent.devops.environment.constant.Constants.OS_TYPE_CC_CODE_SOLARIS
import com.tencent.devops.environment.constant.Constants.OS_TYPE_CC_CODE_UNIX
import com.tencent.devops.environment.constant.Constants.OS_TYPE_CC_CODE_WINDOWS
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_IP_ILLEGAL_USER
import com.tencent.devops.environment.constant.T_NODE_AGENT_VERSION
import com.tencent.devops.environment.constant.T_NODE_NODE_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_IP
import com.tencent.devops.environment.constant.T_NODE_NODE_STATUS
import com.tencent.devops.environment.dao.ProjectConfigDao
import com.tencent.devops.environment.dao.job.CmdbNodeDao
import com.tencent.devops.environment.model.CreateNodeModel
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.CmdbNode
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.pojo.enums.OsType
import com.tencent.devops.environment.pojo.job.AddCmdbNodesRes
import com.tencent.devops.environment.pojo.job.AgentVersion
import com.tencent.devops.environment.pojo.job.NodeAgent
import com.tencent.devops.environment.pojo.job.ReImportCmdbNodeInfo
import com.tencent.devops.environment.pojo.job.UpdateTNodeInfo
import com.tencent.devops.environment.pojo.job.ccres.CCInfo
import com.tencent.devops.environment.pojo.job.ccres.CCResp
import com.tencent.devops.environment.pojo.job.ccres.QueryCCListHostWithoutBizData
import com.tencent.devops.environment.pojo.job.jobreq.OpOperateReq
import com.tencent.devops.environment.service.job.OpService
import com.tencent.devops.environment.service.job.QueryAgentStatusService
import com.tencent.devops.environment.service.job.QueryFromCCService
import com.tencent.devops.environment.utils.ComputeTimeUtils
import com.tencent.devops.environment.utils.ImportServerNodeUtils
import com.tencent.devops.environment.utils.NodeStringIdUtils
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service("CmdbNodeService")
@Primary
class CmdbNodeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val cmdbNodeDao: CmdbNodeDao,
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

        const val AGENT_ABNORMAL_NODE_STATUS = 0
        const val AGENT_NORMAL_NODE_STATUS = 1
        const val AGENT_NOT_INSTALLED_TAG = false

        const val INNER_IP_FIRST_INDEX = 0
        const val IEG_DEPT_ID = 3

        const val IMPORT_STATUS_SUCCEED = 1
        const val IMPORT_STATUS_FAILED = 2

        const val NODE_AGENT_STATUS_ABNORMAL = 0
        const val NODE_AGENT_STATUS_NORMAL = 1
        const val NODE_AGENT_STATUS_NOT_INSTALLED = 2
    }

    fun getUserCmdbNodesNew(
        userId: String,
        bakOperator: Boolean,
        page: Int,
        pageSize: Int,
        projectId: String,
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
        val pageFromCmdb = Page(
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
                    osName = it.osName
                )
            }
        )
        // 判断cmdbNodePage中的nodes，是否在蓝盾db中
        val cmdbIpList = cmdbNodePage.nodes.map { it.ip }
        val cmdbNodeRecord = cmdbNodeDao.getCmdbNodesByIpAndProjectId(
            dslContext, projectId, cmdbIpList
        )
        val ipToCmdbNodeRecordMap = cmdbNodeRecord.associateBy { it[T_NODE_NODE_IP] as String }
        // 1. 在 - 读取节点状态，且importStatus置为true
        val mutableCmdbIpList = cmdbIpList.toMutableList()
        pageFromCmdb.records.filter { it.ip in ipToCmdbNodeRecordMap.keys }.map {
            it.nodeStatus = ipToCmdbNodeRecordMap[it.ip]?.get(T_NODE_NODE_STATUS) as String
            it.importStatus = true
            mutableCmdbIpList.remove(it.ip)
        }
        // 2. 不在 - 重新查询节点状态：在不在CC（得到host_id）-> nodeman中查是否已经安装 -> job中查agent状态+版本号
        val nodeCCInfoList = if (mutableCmdbIpList.isNotEmpty()) {
            queryFromCCService.queryCCListHostWithoutBizByInRules(
                listOf(FIELD_BK_HOST_ID, FIELD_BK_HOST_INNERIP),
                mutableCmdbIpList,
                FIELD_BK_HOST_INNERIP
            ).data?.info
        } else null
        // 2.1 在cc
        val ipToAgentVersionInfoMap = if (!nodeCCInfoList.isNullOrEmpty()) {
            nodeCCInfoList.map {
                mutableCmdbIpList.remove(it.bkHostInnerip)
            }
            queryAgentStatusService.getAgentVersions(
                nodeCCInfoList.map {
                    AgentVersion(ip = it.bkHostInnerip, bkHostId = it.bkHostId)
                }
            )?.associateBy { it.ip }
        } else null
        if (!ipToAgentVersionInfoMap.isNullOrEmpty()) {
            pageFromCmdb.records.filterNot { it.ip in ipToCmdbNodeRecordMap.keys }.map {
                it.nodeStatus = getNodeStatus(ipToAgentVersionInfoMap[it.ip])
            }
        }
        // 2.2 不在cc
        if (mutableCmdbIpList.isNotEmpty()) {
            pageFromCmdb.records.filter { it.ip in mutableCmdbIpList }.map {
                it.nodeStatus = NodeStatus.NOT_IN_CC.name
            }
        }

        return pageFromCmdb
    }

    private fun getNodeStatus(agentInfo: AgentVersion?): String {
        return if (AGENT_NOT_INSTALLED_TAG == agentInfo?.installedTag)
            NodeStatus.NOT_INSTALLED.name
        else if (AGENT_ABNORMAL_NODE_STATUS == agentInfo?.status)
            NodeStatus.ABNORMAL.name
        else if (AGENT_NORMAL_NODE_STATUS == agentInfo?.status)
            NodeStatus.NORMAL.name
        else
            NodeStatus.NOT_INSTALLED.name
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
        val nodeRecords = cmdbNodeDao.getCmdbNodesByNodeIdList(dslContext, nodeIdList)
        val ipToAgentVersionInfoMap = queryAgentStatusService.getAgentVersions(
            nodeIdToCCInfoMap.values.mapNotNull {
                AgentVersion(ip = it?.bkHostInnerip, bkHostId = it?.bkHostId)
            }
        )?.associateBy { it.ip }
        val agentStatusMap = if (!ipToAgentVersionInfoMap.isNullOrEmpty()) {
            esbAgentClient.getAgentStatus(userId, ipToAgentVersionInfoMap.keys.filterNotNull().toList())
        } else null
        val opInfo = opService.operateOpProject("", OpOperateReq(2, listOf(projectId))).projGrayStatus?.get(0)
        val grayTag = projectId == opInfo?.englishName && true == opInfo.projGrayStatus
        val unsuccessfullyImportedNodesIpList = mutableListOf<String>()
        val updateNodeInfo = nodeRecords.filter {
            val importNodesStatus = queryCCIpToCCInfoMap.containsKey(it[T_NODE_NODE_IP] as String)
            if (!importNodesStatus) {
                unsuccessfullyImportedNodesIpList.add(it[T_NODE_NODE_IP] as String)
            }
            importNodesStatus
        }.map {
            val nodeId = it[T_NODE_NODE_ID] as Long
            UpdateTNodeInfo(
                nodeId = nodeId,
                nodeStatus = if (nodeIdToCCInfoMap.containsKey(nodeId)) {
                    getNodeStatus(ipToAgentVersionInfoMap?.get(it[T_NODE_NODE_IP] as String))
                } else {
                    NodeStatus.NOT_IN_CC.name
                },
                hostId = nodeIdToCCInfoMap[nodeId]?.bkHostId,
                cloudAreaId = nodeIdToCCInfoMap[nodeId]?.bkCloudId?.toLong(),
                agentStatus = if (grayTag) {
                    1 == ipToAgentVersionInfoMap?.get(it[T_NODE_NODE_IP] as String)?.status
                } else {
                    agentStatusMap?.get(it[T_NODE_NODE_IP] as String) ?: false
                },
                agentVersion = if (grayTag)
                    ipToAgentVersionInfoMap?.get(it[T_NODE_NODE_IP] as String)?.version
                else null,
                lastModifyTime = LocalDateTime.now()
            )
        }
        cmdbNodeDao.batchUpdateCCInfo(dslContext, updateNodeInfo)
        val addToCCNodeList = nodeRecords.map {
            NodeAgent(
                nodeIp = it[T_NODE_NODE_IP] as String,
                nodesAgentStatus =
                if (NodeStatus.NORMAL.name == it[T_NODE_NODE_STATUS] as String) NODE_AGENT_STATUS_NORMAL
                else if (NodeStatus.ABNORMAL.name == it[T_NODE_NODE_STATUS] as String) NODE_AGENT_STATUS_ABNORMAL
                else NODE_AGENT_STATUS_NOT_INSTALLED,
                nodesAgentVersion = it[T_NODE_AGENT_VERSION] as? String
            )
        }
        val notAddedNodeList = unsuccessfullyImportedNodesIpList.map {
            NodeAgent(
                nodeIp = it,
                importStatus = IMPORT_STATUS_FAILED
            )
        }
        return AddCmdbNodesRes(
            nodeStatus = true,
            nodesAgentList = addToCCNodeList + notAddedNodeList,
            agentAbnormalNodesCount = nodeRecords.filter {
                NodeStatus.ABNORMAL.name == it[T_NODE_NODE_STATUS] as String
            }.size,
            agentNotInstallNodesCount = nodeRecords.filter {
                NodeStatus.NOT_INSTALLED.name == it[T_NODE_NODE_STATUS] as String
            }.size
        )
    }

    /**
     * 测试机导入
     * 如果机器不存在，add一条新纪录
     */
    @ActionAuditRecord(
        actionId = ActionId.ENV_NODE_CREATE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENV_NODE
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENV_NODE_CREATE_CONTENT
    )
    fun addCmdbNodes(userId: String, projectId: String, nodeIps: List<String>): AddCmdbNodesRes {
        val startTime = LocalDateTime.now()
        // 验证 CMDB 节点IP和责任人
        val cmdbIpToNodeMap = checkUserOperator(userId, nodeIps)
        // 只添加不存在的节点
        val existIpList = cmdbNodeDao.listServerAndDevCloudNodes(dslContext, projectId) // 已存在 节点db记录
            .map { it.nodeIp }.toSet() // 已存在 节点ip
        val toAddIpList = nodeIps.filterNot { existIpList.contains(it) }.filterNot { it.isEmpty() } // 要添加的 节点ip
            .toSet() // 去重
        val time1 = LocalDateTime.now()
        val toAddIpToCmdbNodeMap = cmdbIpToNodeMap.filter { toAddIpList.contains(it.key) } // 要添加的 节点ip - cmdb记录映射
        ImportServerNodeUtils.checkImportCount(
            dslContext = dslContext,
            projectConfigDao = projectConfigDao,
            cmdbNodeDao = cmdbNodeDao,
            projectId = projectId,
            userId = userId,
            toAddNodeCount = toAddIpList.size
        )
        val time2 = LocalDateTime.now()
        val queryCCIpToCCInfoMap = addNodeToCC(toAddIpToCmdbNodeMap)
        val time3 = LocalDateTime.now()
        val agentStatusMap = esbAgentClient.getAgentStatus(userId, toAddIpList)
        val time4 = LocalDateTime.now()
        val ipAndHostIdList = queryCCIpToCCInfoMap.values.map {
            AgentVersion(ip = it.bkHostInnerip, bkHostId = it.bkHostId)
        }
        val agentVersionList = queryAgentStatusService.getAgentVersions(ipAndHostIdList)
        val time5 = LocalDateTime.now()
        val ipToAgentVersionMap = agentVersionList?.associateBy { it.ip }
        val unsuccessfullyImportedNodesIpList = mutableListOf<String>()
        val toAddNodeList = toAddIpList.filter {
            val importNodesStatus = queryCCIpToCCInfoMap.containsKey(it)
            if (!importNodesStatus) {
                unsuccessfullyImportedNodesIpList.add(it)
            }
            importNodesStatus
        }.map {
            val cmdbNode = cmdbIpToNodeMap[it]!!
            val opInfo = opService.operateOpProject("", OpOperateReq(2, listOf(projectId))).projGrayStatus?.get(0)
            val grayTag = projectId == opInfo?.englishName && true == opInfo.projGrayStatus
            CreateNodeModel(
                nodeStringId = "",
                projectId = projectId,
                nodeIp = cmdbNode.ip,
                nodeName = cmdbNode.name,
                nodeStatus = if (queryCCIpToCCInfoMap.containsKey(cmdbNode.ip)) {
                    getNodeStatus(ipToAgentVersionMap?.get(cmdbNode.ip))
                } else {
                    NodeStatus.NOT_IN_CC.name
                },
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
                cloudAreaId = queryCCIpToCCInfoMap[cmdbNode.ip]?.bkCloudId?.toLong(),
                osType = queryCCIpToCCInfoMap[cmdbNode.ip]?.osType,
                serverId = cmdbNode.serverId
            )
        }
        val time6 = LocalDateTime.now()
        if (logger.isDebugEnabled)
            logger.debug(
                "[addCmdbNodes]toAddNodeList: " +
                    toAddNodeList.joinToString(separator = ", ", transform = { it.toString() })
            )
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            cmdbNodeDao.batchInsertNode(context, toAddNodeList)
            val insertedNodeList = cmdbNodeDao.listServerNodesByIps(
                dslContext = context,
                projectId = projectId,
                ips = toAddNodeList.map { it.nodeIp }
            )
            batchRegisterNodePermissionAndAudit(
                insertedNodeList = insertedNodeList,
                userId = userId,
                projectId = projectId
            )
        }
        val time7 = LocalDateTime.now()
        logger.info(
            "[addCmdbNodes]Total: ${ComputeTimeUtils.calculateDuration(startTime, time7)}s, " +
                "checkUserOperator(cmdb): ${ComputeTimeUtils.calculateDuration(startTime, time1)}s, " +
                "checkImportCount: ${ComputeTimeUtils.calculateDuration(time1, time2)}s, " +
                "addNodeToCC: ${ComputeTimeUtils.calculateDuration(time2, time3)}s, " +
                "agent status from esb: ${ComputeTimeUtils.calculateDuration(time3, time4)}s, " +
                "agent versions from nodeman/job: ${ComputeTimeUtils.calculateDuration(time4, time5)}s, " +
                "toAddNodeList: ${ComputeTimeUtils.calculateDuration(time5, time6)}s, " +
                "batchInsertNode: ${ComputeTimeUtils.calculateDuration(time6, time7)}s, "
        )
        val importedNodeList = toAddNodeList.map {
            NodeAgent(
                nodeIp = it.nodeIp,
                importStatus = IMPORT_STATUS_SUCCEED,
                nodesAgentStatus = if (NodeStatus.NORMAL.name == it.nodeStatus) NODE_AGENT_STATUS_NORMAL
                else if (NodeStatus.ABNORMAL.name == it.nodeStatus) NODE_AGENT_STATUS_ABNORMAL
                else NODE_AGENT_STATUS_NOT_INSTALLED,
                nodesAgentVersion = it.agentVersion
            )
        }
        val notImportedNodeList = unsuccessfullyImportedNodesIpList.map {
            NodeAgent(nodeIp = it, importStatus = IMPORT_STATUS_FAILED)
        }
        return AddCmdbNodesRes(
            nodeStatus = true,
            nodesAgentList = importedNodeList + notImportedNodeList,
            agentAbnormalNodesCount = toAddNodeList.filter { NodeStatus.ABNORMAL.name == it.nodeStatus }.size,
            agentNotInstallNodesCount = toAddNodeList.filter { NodeStatus.NOT_INSTALLED.name == it.nodeStatus }.size,
            successfullyImportedNodeCount = importedNodeList.size,
            unsuccessfullyImportedNodeCount = notImportedNodeList.size
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
                errorCode = ERROR_NODE_IP_ILLEGAL_USER,
                params = arrayOf(invalidIps.joinToString(","), userId)
            )
        }
        return cmdbIpToNodeMap
    }

    /**
     * 将节点添加到CC中
     * 四类节点情况：（2.1、2.2为两种不能导入CC的情况，2.*为所有不允许导入CC的情况，2.1U2.2 <= 2.*）
     * 1. 单ip/多ip，在CC - 查CCInfo并更新信息；
     * 2.1 单ip/多ip，不在cc，不能导入CC - ieg的机器，需要用户手动处理，调用侧导入蓝盾并更新节点状态为NOT_IN_CC
     * 2.2 多ip，不在cc，不能导入CC - 有ip已经在CC中，本次导入去掉这个ip不导入，调用侧导入蓝盾并更新节点状态为NOT_IN_CC
     * 3. 单ip/多ip，不在cc，可以导入CC - 导入，查CCInfo更新信息
     * 返回值：无论在不在CC中的节点信息 CCInfo
     */
    private fun addNodeToCC(toAddIpToCmdbNodeMap: Map<String, RawCmdbNode>): Map<String?, CCInfo> {
        // 通过节点svrId查询：节点是否在CC中
        val serverIdToCmdbNodeMap = toAddIpToCmdbNodeMap.values.associateBy { it.serverId }
        val svrIdList = toAddIpToCmdbNodeMap.map { it.value.serverId }
        val (svrIdQueryCCRes, inCCSvrIdList, notInCCSvrIdList) = checkNodeInCCBySvrId(svrIdList)
        // 1. 在CC中，通过svrId查出host_id、云区域id、操作系统类型
        var queryCCIpToCCInfoMap = mapOf<String?, CCInfo>()
        if (inCCSvrIdList.isNotEmpty()) {
            val ccData = svrIdQueryCCRes.data?.info
            val ccDataWithStringOsType = ccData?.map {
                it.osType = getOsTypeByCCCode(it.osType)
                it
            } ?: listOf()
            queryCCIpToCCInfoMap = ccDataWithStringOsType.associateBy {
                it.bkHostInnerip?.split(",")?.get(INNER_IP_FIRST_INDEX)
            }
        }
        // 2.1 通过svrId查询：是否为ieg的机器，即运维部门id == 3（若为ieg的机器，需要用户手动处理，改机器业务为CC对应的二级业务，同步到CC中，此接口无法导入，调用侧设置状态为NOT_IN_CC）
        val toAddIpToCmdbNodeMutableMap = toAddIpToCmdbNodeMap.toMutableMap()
        toAddIpToCmdbNodeMutableMap.entries.retainAll {
            it.value.serverId in notInCCSvrIdList && IEG_DEPT_ID != it.value.deptId
        }
        // 2.2 通过ip查询：多ip节点是否有ip已经在CC中 (如果多ip节点已经有ip在CC中，则本次导入去掉这个ip不导入，调用侧设置状态为NOT_IN_CC)
        val toAddIpList = toAddIpToCmdbNodeMutableMap.keys
        val ipQueryCCRes = queryFromCCService.queryCCListHostWithoutBizByInRules(
            listOf(FIELD_BK_HOST_ID, FIELD_BK_CLOUD_ID, FIELD_BK_HOST_INNERIP, FIELD_BK_SVR_ID, FIELD_BK_OS_TYPE),
            toAddIpList,
            FIELD_BK_HOST_INNERIP
        )
        val inCCIpInfo = ipQueryCCRes.data?.info
        if (!inCCIpInfo.isNullOrEmpty()) {
            inCCIpInfo.map { ipInfo ->
                toAddIpList.forEach {
                    val ipInCC = ipInfo.bkHostInnerip?.contains(it) ?: false
                    if (ipInCC) toAddIpToCmdbNodeMutableMap.remove(it)
                }
            }
        }
        // 3. 不在CC中，add到CC中，查出host_id、云区域id和操作系统类型
        var addToCCIpToCCInfoMap = mapOf<String?, CCInfo>()
        val notInCCAddSvrIdList = toAddIpToCmdbNodeMutableMap.values.map { it.serverId }
        if (notInCCSvrIdList.isNotEmpty()) {
            val addToCCResp = queryFromCCService.addHostToCiBiz(notInCCAddSvrIdList)
            val (addToCCSvrIdQueryCCRes, _, _) = checkNodeInCCBySvrId(notInCCAddSvrIdList)
            val addToCCData = addToCCSvrIdQueryCCRes.data?.info
            val hostIdToCCInfoMap = addToCCData?.associateBy { it.bkHostId }
            val ccHostIdList = addToCCResp.data?.bkHostIds
            val addToCCInfoList = ccHostIdList?.mapIndexed { index, value ->
                CCInfo(
                    svrId = notInCCSvrIdList[index],
                    bkHostId = value,
                    bkHostInnerip = serverIdToCmdbNodeMap[notInCCSvrIdList[index]]?.ip,
                    bkCloudId = hostIdToCCInfoMap?.get(value)?.bkCloudId,
                    osType = getOsTypeByCCCode(hostIdToCCInfoMap?.get(value)?.osType)
                )
            }
            if (logger.isDebugEnabled)
                logger.debug(
                    "[addCmdbNodes]addToCCInfo: " +
                        addToCCInfoList?.joinToString(separator = ", ", transform = { it.toString() })
                )
            addToCCIpToCCInfoMap = addToCCInfoList?.associateBy {
                it.bkHostInnerip?.split(",")?.get(INNER_IP_FIRST_INDEX)
            } ?: mapOf()
        }
        return queryCCIpToCCInfoMap + addToCCIpToCCInfoMap
    }

    fun checkNodeInCCBySvrId(svrIdList: List<Long>):
        Triple<CCResp<QueryCCListHostWithoutBizData>, List<Long>, List<Long>> {
        val svrIdQueryCCRes = queryFromCCService.queryCCListHostWithoutBizByInRules(
            listOf(FIELD_BK_HOST_ID, FIELD_BK_CLOUD_ID, FIELD_BK_HOST_INNERIP, FIELD_BK_SVR_ID, FIELD_BK_OS_TYPE),
            svrIdList,
            FIELD_BK_SVR_ID
        )
        val svrIdQueryCCList = svrIdQueryCCRes.data?.info ?: listOf() // 所有在cc中的节点记录
        val svrIdToCCResMap = svrIdQueryCCList.associateBy { it.svrId } // cc中 svrId-节点记录 映射

        val inCCSvrIdList = mutableListOf<Long>() // 在CC中的节点的SvrId
        val notInCCSvrIdList = mutableListOf<Long>() // 不在CC中的节点的SvrId
        svrIdList.map {
            if (svrIdToCCResMap.containsKey(it)) inCCSvrIdList.add(it)
            else notInCCSvrIdList.add(it)
        }
        logger.info(
            "[checkNodeInCCBySvrId]inCCSvrIdList: ${inCCSvrIdList.joinToString()}, " +
                "notInCCSvrIdList: ${notInCCSvrIdList.joinToString()}"
        )
        return Triple(svrIdQueryCCRes, inCCSvrIdList, notInCCSvrIdList)
    }

    fun getOsTypeByCCCode(ccCode: String?): String {
        return when (ccCode) {
            OS_TYPE_CC_CODE_LINUX -> OsType.LINUX.name
            OS_TYPE_CC_CODE_WINDOWS -> OsType.WINDOWS.name
            OS_TYPE_CC_CODE_AIX -> OsType.AIX.name
            OS_TYPE_CC_CODE_SOLARIS -> OsType.SOLARIS.name
            OS_TYPE_CC_CODE_UNIX -> OsType.UNIX.name
            OS_TYPE_CC_CODE_FREEBSD -> OsType.FREEBSD.name
            else -> OsType.OTHER.name
        }
    }

    private fun batchRegisterNodePermissionAndAudit(
        insertedNodeList: List<TNodeRecord>,
        userId: String,
        projectId: String
    ) {
        insertedNodeList.forEach {
            val nodeName = "${NodeStringIdUtils.getNodeStringId(it)}(${it.nodeIp})"
            environmentPermissionService.createNode(
                userId = userId,
                projectId = projectId,
                nodeId = it.nodeId,
                nodeName = nodeName
            )
            // audit
            ActionAuditContext.current()
                .addInstanceInfo(it.nodeId.toString(), nodeName, null, null)
        }
    }
}
