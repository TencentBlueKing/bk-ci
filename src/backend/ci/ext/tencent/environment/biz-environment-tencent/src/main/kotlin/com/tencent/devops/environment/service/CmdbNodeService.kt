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
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationDTO
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
import com.tencent.devops.environment.constant.T_NODE_SERVER_ID
import com.tencent.devops.environment.dao.ProjectConfigDao
import com.tencent.devops.environment.dao.job.CmdbNodeDao
import com.tencent.devops.environment.model.CreateNodeModel
import com.tencent.devops.environment.permission.EnvNodeAuthorizationService
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.CmdbNode
import com.tencent.devops.environment.pojo.cmdb.common.CmdbServerDTO
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.pojo.enums.OsType
import com.tencent.devops.environment.pojo.job.AddCmdbNodesRes
import com.tencent.devops.environment.pojo.job.AgentVersion
import com.tencent.devops.environment.pojo.job.NodeAgent
import com.tencent.devops.environment.pojo.job.ReImportCmdbNodeInfo
import com.tencent.devops.environment.pojo.job.UpdateTNodeInfo
import com.tencent.devops.environment.pojo.job.ccres.CCHost
import com.tencent.devops.environment.service.cc.TencentCCService
import com.tencent.devops.environment.service.cmdb.EsbCmdbClient
import com.tencent.devops.environment.service.cmdb.TencentCmdbService
import com.tencent.devops.environment.service.gseagent.utils.NodeStatusUtils
import com.tencent.devops.environment.service.job.QueryAgentStatusService
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
    private val esbCmdbClient: EsbCmdbClient,
    private val environmentPermissionService: EnvironmentPermissionService,
    private val tencentCmdbService: TencentCmdbService,
    private val tencentCCService: TencentCCService,
    private val queryAgentStatusService: QueryAgentStatusService,
    private val envNodeAuthorizationService: EnvNodeAuthorizationService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CmdbNodeService::class.java)

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
        // 准备要替换掉
        val cmdbNodePage =
            ImportServerNodeUtils.getUserCmdbNodeNew(
                esbCmdbClient = esbCmdbClient,
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
                    osName = it.osName,
                    serverId = it.serverId
                )
            }
        )
        // 判断cmdbNodePage中的nodes，是否在蓝盾db中
        val cmdbServerIdList = cmdbNodePage.nodes.map { it.serverId }
        val cmdbNodeStatusList = cmdbNodeDao.listCmdbNodeStatusByProjectIdAndServerId(
            projectId, cmdbServerIdList
        )
        val serverIdToCmdbNodeStatusMap = cmdbNodeStatusList.associateBy { it.serverId }
        // 1. 在 - 读取节点状态，且importStatus置为true
        val mutableCmdbServerIdList = cmdbServerIdList.toMutableList()
        pageFromCmdb.records.filter { it.serverId in serverIdToCmdbNodeStatusMap.keys }.map {
            it.nodeStatus = serverIdToCmdbNodeStatusMap[it.serverId]?.nodeStatus
            it.importStatus = true
            mutableCmdbServerIdList.remove(it.serverId)
        }
        // 2. 不在 - 重新查询节点状态：在不在CC（得到host_id）-> nodeman中查是否已经安装 -> job中查agent状态+版本号
        val nodeCCInfoList = if (mutableCmdbServerIdList.isNotEmpty()) {
            tencentCCService.listHostsWithoutBiz(
                listOf(FIELD_BK_HOST_ID, FIELD_BK_HOST_INNERIP, FIELD_BK_SVR_ID),
                mutableCmdbServerIdList,
                FIELD_BK_SVR_ID
            ).data?.info
        } else null
        // 2.1 在cc
        val serverIdToAgentVersionInfoMap = if (!nodeCCInfoList.isNullOrEmpty()) {
            nodeCCInfoList.map {
                mutableCmdbServerIdList.remove(it.svrId)
            }
            queryAgentStatusService.getAgentVersions(
                nodeCCInfoList.map {
                    AgentVersion(serverId = it.svrId, ip = it.bkHostInnerip, bkHostId = it.bkHostId)
                }
            )?.associateBy { it.serverId }
        } else null
        if (!serverIdToAgentVersionInfoMap.isNullOrEmpty()) {
            pageFromCmdb.records.filterNot { it.serverId in serverIdToCmdbNodeStatusMap.keys }.map {
                it.nodeStatus = NodeStatusUtils.getNodeStatus(serverIdToAgentVersionInfoMap[it.serverId])
            }
        }
        // 2.2 不在cc
        if (mutableCmdbServerIdList.isNotEmpty()) {
            pageFromCmdb.records.filter { it.serverId in mutableCmdbServerIdList }.map {
                it.nodeStatus = NodeStatus.NOT_IN_CC.name
            }
        }

        return pageFromCmdb
    }

    /**
     * 重新导入
     * 将不在CC中的机器导入CC，直接update原来的记录
     */
    fun reImportCmdbNodesByIp(
        userId: String,
        projectId: String,
        reImportCmdbNodeInfoList: List<ReImportCmdbNodeInfo>
    ): AddCmdbNodesRes {
        logger.info("[reImportCmdbNodesByIp]")
        // 验证User是节点的主备负责人
        val nodeIpList = reImportCmdbNodeInfoList.mapNotNull { it.nodeIp }
        val cmdbIpToNodeMap = checkUserOperatorByIp(userId, nodeIpList)

        // 将该节点添加到CC中
        val queryCCInfoList = addNodeToCCByIpMap(cmdbIpToNodeMap)
        val queryCCIpToCCInfoMap = queryCCInfoList.associateBy {
            it.bkHostInnerip?.split(",")?.get(INNER_IP_FIRST_INDEX)
        }

        // update对应db记录的NODE_STATUS，并写入 HOST_ID 和 CLOUD_AREA_ID
        val nodeIdList = reImportCmdbNodeInfoList.map { it.nodeId }
        val nodeIdToCCInfoMap = mutableMapOf<Long, CCHost?>()
        reImportCmdbNodeInfoList.forEach {
            nodeIdToCCInfoMap[it.nodeId] = queryCCIpToCCInfoMap[it.nodeIp]
        }
        val nodeRecords = cmdbNodeDao.getCmdbNodesByNodeIdList(dslContext, nodeIdList)
        val ipToAgentVersionInfoMap = queryAgentStatusService.getAgentVersions(
            nodeIdToCCInfoMap.values.mapNotNull {
                AgentVersion(ip = it?.bkHostInnerip, bkHostId = it?.bkHostId, serverId = it?.svrId)
            }
        )?.associateBy { it.ip }
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
                    NodeStatusUtils.getNodeStatus(ipToAgentVersionInfoMap?.get(it[T_NODE_NODE_IP] as String))
                } else {
                    NodeStatus.NOT_IN_CC.name
                },
                hostId = nodeIdToCCInfoMap[nodeId]?.bkHostId,
                cloudAreaId = nodeIdToCCInfoMap[nodeId]?.bkCloudId?.toLong(),
                serverId = nodeIdToCCInfoMap[nodeId]?.svrId,
                osType = nodeIdToCCInfoMap[nodeId]?.osType,
                agentVersion = ipToAgentVersionInfoMap?.get(it[T_NODE_NODE_IP] as String)?.version,
                lastModifyTime = LocalDateTime.now()
            )
        }
        cmdbNodeDao.batchUpdateCCInfo(dslContext, updateNodeInfo)
        val addToCCNodeList = nodeRecords.map {
            NodeAgent(
                nodeIp = it[T_NODE_NODE_IP] as String,
                nodesAgentStatus =
                if (NodeStatus.NORMAL.name == it[T_NODE_NODE_STATUS] as String)
                    NODE_AGENT_STATUS_NORMAL
                else if (NodeStatus.ABNORMAL.name == it[T_NODE_NODE_STATUS] as String)
                    NODE_AGENT_STATUS_ABNORMAL
                else
                    NODE_AGENT_STATUS_NOT_INSTALLED,
                nodesAgentVersion = it[T_NODE_AGENT_VERSION] as? String
            )
        }
        val notAddedNodeList = unsuccessfullyImportedNodesIpList.map {
            NodeAgent(
                nodeIp = it,
                importStatus = IMPORT_STATUS_FAILED
            )
        }
        val nodesAgentList = addToCCNodeList + notAddedNodeList
        return AddCmdbNodesRes(
            nodeStatus = true,
            nodesAgentList = nodesAgentList,
            agentAbnormalNodesCount = nodesAgentList.filter {
                NODE_AGENT_STATUS_ABNORMAL == it.nodesAgentStatus
            }.size,
            agentNotInstallNodesCount = nodesAgentList.filter {
                NODE_AGENT_STATUS_NOT_INSTALLED == it.nodesAgentStatus
            }.size
        )
    }

    fun reImportCmdbNodesByServerId(
        userId: String,
        projectId: String,
        reImportCmdbNodeInfoList: List<ReImportCmdbNodeInfo>
    ): AddCmdbNodesRes {
        logger.info("[reImportCmdbNodesByServerId]")
        // 验证User是节点的主备负责人
        val nodeServerIdList = reImportCmdbNodeInfoList.map { it.serverId!! }
        val serverIdToCmdbServerMap = checkUserOperatorByServerId(
            userId, nodeServerIdList
        )

        // 将该节点添加到CC中
        val queryCCInfoList = addNodeToCCByServerIdMap(serverIdToCmdbServerMap)
        val queryCCServerIdToCCInfoMap = queryCCInfoList.associateBy { it.svrId }.filterKeys { null != it }

        // update对应db记录的 NODE_STATUS，并写入 HOST_ID 和 CLOUD_AREA_ID
        val nodeIdList = reImportCmdbNodeInfoList.map { it.nodeId }
        val nodeIdToCCInfoMap = mutableMapOf<Long, CCHost?>()
        reImportCmdbNodeInfoList.forEach {
            nodeIdToCCInfoMap[it.nodeId] = queryCCServerIdToCCInfoMap[it.serverId]
        }
        val nodeRecords = cmdbNodeDao.getCmdbNodesByNodeIdList(dslContext, nodeIdList)
        val serverIdToAgentVersionInfoMap = queryAgentStatusService.getAgentVersions(
            nodeIdToCCInfoMap.values.mapNotNull {
                AgentVersion(ip = it?.bkHostInnerip, bkHostId = it?.bkHostId, serverId = it?.svrId)
            }
        )?.associateBy { it.serverId }
        val unsuccessfullyImportedNodesIpList = mutableListOf<String>()
        val updateNodeInfo = nodeRecords.filter {
            val importNodesStatus = queryCCServerIdToCCInfoMap.containsKey(it[T_NODE_SERVER_ID] as Long)
            if (!importNodesStatus) {
                unsuccessfullyImportedNodesIpList.add(it[T_NODE_NODE_IP] as String)
            }
            importNodesStatus
        }.map {
            val nodeId = it[T_NODE_NODE_ID] as Long
            val serverId = it[T_NODE_SERVER_ID] as Long
            UpdateTNodeInfo(
                nodeId = nodeId,
                nodeStatus = if (nodeIdToCCInfoMap.containsKey(nodeId)) {
                    NodeStatusUtils.getNodeStatus(serverIdToAgentVersionInfoMap?.get(serverId))
                } else {
                    NodeStatus.NOT_IN_CC.name
                },
                hostId = nodeIdToCCInfoMap[nodeId]?.bkHostId,
                cloudAreaId = nodeIdToCCInfoMap[nodeId]?.bkCloudId?.toLong(),
                serverId = serverId,
                osType = nodeIdToCCInfoMap[nodeId]?.osType,
                agentVersion = serverIdToAgentVersionInfoMap?.get(serverId)?.version,
                lastModifyTime = LocalDateTime.now()
            )
        }
        cmdbNodeDao.batchUpdateCCInfoByServerId(dslContext, updateNodeInfo)
        val addToCCNodeList = nodeRecords.map {
            NodeAgent(
                nodeIp = it[T_NODE_NODE_IP] as String,
                nodesAgentStatus =
                if (NodeStatus.NORMAL.name == it[T_NODE_NODE_STATUS] as String)
                    NODE_AGENT_STATUS_NORMAL
                else if (NodeStatus.ABNORMAL.name == it[T_NODE_NODE_STATUS] as String)
                    NODE_AGENT_STATUS_ABNORMAL
                else
                    NODE_AGENT_STATUS_NOT_INSTALLED,
                nodesAgentVersion = it[T_NODE_AGENT_VERSION] as? String
            )
        }
        val notAddedNodeList = unsuccessfullyImportedNodesIpList.map {
            NodeAgent(
                nodeIp = it,
                importStatus = IMPORT_STATUS_FAILED
            )
        }
        val nodesAgentList = addToCCNodeList + notAddedNodeList
        return AddCmdbNodesRes(
            nodeStatus = true,
            nodesAgentList = nodesAgentList,
            agentAbnormalNodesCount = nodesAgentList.filter {
                NODE_AGENT_STATUS_ABNORMAL == it.nodesAgentStatus
            }.size,
            agentNotInstallNodesCount = nodesAgentList.filter {
                NODE_AGENT_STATUS_NOT_INSTALLED == it.nodesAgentStatus
            }.size
        )
    }

    /**
     * 测试机导入
     * 如果机器不存在，add一条新纪录
     * @param nodeIpList 要导入蓝盾的机器ip列表
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
    fun addCmdbNodesByIp(userId: String, projectId: String, nodeIpList: List<String>): AddCmdbNodesRes {
        val startTime = LocalDateTime.now()
        // 验证 CMDB 节点IP和责任人
        val cmdbIpToNodeMap = checkUserOperatorByIp(userId, nodeIpList)
        // 只添加不存在的节点
        val existIpList = cmdbNodeDao.listServerAndDevCloudNodes(dslContext, projectId) // 已存在 节点db记录
            .map { it.nodeIp }.toSet() // 已存在 节点ip
        val toAddIpList = nodeIpList.filterNot { existIpList.contains(it) }.filterNot { it.isEmpty() } // 要添加的 节点ip
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
        val queryCCInfoList = addNodeToCCByIpMap(toAddIpToCmdbNodeMap)
        val queryCCIpToCCInfoMap = queryCCInfoList.associateBy {
            it.bkHostInnerip?.split(",")?.get(INNER_IP_FIRST_INDEX)
        }
        val time3 = LocalDateTime.now()
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
            val nodeIp = cmdbNode.getFirstIp()
            CreateNodeModel(
                nodeStringId = "",
                projectId = projectId,
                nodeIp = nodeIp,
                nodeName = cmdbNode.hostName,
                nodeStatus = if (queryCCIpToCCInfoMap.containsKey(nodeIp)) {
                    NodeStatusUtils.getNodeStatus(ipToAgentVersionMap?.get(nodeIp))
                } else {
                    NodeStatus.NOT_IN_CC.name
                },
                nodeType = NodeType.CMDB.name,
                createdUser = userId,
                osName = cmdbNode.getOsNameLessThanMaxLength(),
                operator = cmdbNode.operator,
                bakOperator = cmdbNode.getBakOperatorStrLessThanMaxLength(),
                agentVersion = ipToAgentVersionMap?.get(nodeIp)?.version,
                hostId = queryCCIpToCCInfoMap[nodeIp]?.bkHostId,
                cloudAreaId = queryCCIpToCCInfoMap[nodeIp]?.bkCloudId?.toLong(),
                osType = queryCCIpToCCInfoMap[nodeIp]?.osType,
                serverId = cmdbNode.serverId
            )
        }
        val time6 = LocalDateTime.now()
        if (logger.isDebugEnabled)
            logger.debug(
                "[addCmdbNodesByIp]toAddNodeList: " +
                    toAddNodeList.joinToString(separator = ", ", transform = { it.toString() })
            )
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            cmdbNodeDao.batchInsertNode(context, toAddNodeList)
            val insertedNodeList = cmdbNodeDao.listServerNodesByIps(
                dslContext = context, projectId = projectId, ips = toAddNodeList.map { it.nodeIp }
            )
            batchRegisterNodePermissionAndAudit(
                insertedNodeList = insertedNodeList, userId = userId, projectId = projectId
            )
        }
        val time7 = LocalDateTime.now()
        logger.info(
            "[addCmdbNodesByIp]Total: ${ComputeTimeUtils.calculateDuration(startTime, time7)}s, " +
                "checkUserOperator(cmdb): ${ComputeTimeUtils.calculateDuration(startTime, time1)}s, " +
                "checkImportCount: ${ComputeTimeUtils.calculateDuration(time1, time2)}s, " +
                "addNodeToCCByServerIdMap: ${ComputeTimeUtils.calculateDuration(time2, time3)}s, " +
                "agent versions from nodeman/job: ${ComputeTimeUtils.calculateDuration(time3, time5)}s, " +
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
     * 测试机导入
     * 如果机器不存在，add一条新纪录
     * @param nodeServerIdList 要导入蓝盾的机器serverId列表
     */
    fun addCmdbNodesByServerId(userId: String, projectId: String, nodeServerIdList: List<Long>): AddCmdbNodesRes {
        val startTime = LocalDateTime.now()
        // 验证用户是该机器的主备负责人
        val serverIdToCmdbServerMap = checkUserOperatorByServerId(
            userId, nodeServerIdList
        )
        // 根据项目和serverId去重
        val existServerIdNodeRecord = cmdbNodeDao.getNodeByProjectIdAndServerIdList(
            dslContext, projectId, nodeServerIdList
        )
        val existServerIdList = existServerIdNodeRecord.mapNotNull { it[T_NODE_SERVER_ID] as? Long }
        val toAddServerIdList = nodeServerIdList.filterNot { existServerIdList.contains(it) }
        val time1 = LocalDateTime.now()
        val toAddServerIdToCmdbNodeMap = serverIdToCmdbServerMap.filter { toAddServerIdList.contains(it.key) }
        ImportServerNodeUtils.checkImportCount(
            dslContext = dslContext,
            projectConfigDao = projectConfigDao,
            cmdbNodeDao = cmdbNodeDao,
            projectId = projectId,
            userId = userId,
            toAddNodeCount = toAddServerIdList.size
        )
        val time2 = LocalDateTime.now()
        val queryCCInfoList = addNodeToCCByServerIdMap(toAddServerIdToCmdbNodeMap)
        val queryCCServerIdToCCInfoMap = queryCCInfoList.associateBy { it.svrId }
        val time3 = LocalDateTime.now()
        val agentVersionReqList = queryCCServerIdToCCInfoMap.values.map {
            AgentVersion(ip = it.bkHostInnerip, bkHostId = it.bkHostId, serverId = it.svrId)
        }
        val agentVersionList = queryAgentStatusService.getAgentVersions(agentVersionReqList)
        val time4 = LocalDateTime.now()
        val serverIdToAgentVersionMap = agentVersionList?.associateBy { it.serverId }
        val unsuccessfullyImportedNodeServerIdList = mutableListOf<Long>()
        val toAddNodeList = toAddServerIdList.filter {
            val importNodesStatus = queryCCServerIdToCCInfoMap.containsKey(it)
            if (!importNodesStatus) unsuccessfullyImportedNodeServerIdList.add(it)
            importNodesStatus
        }.map {
            val cmdbNode = serverIdToCmdbServerMap[it]!!
            val ccInfo = queryCCServerIdToCCInfoMap[cmdbNode.serverId]
            CreateNodeModel(
                nodeStringId = "",
                projectId = projectId,
                nodeIp = cmdbNode.getFirstIp(),
                nodeName = cmdbNode.hostName,
                nodeStatus = if (queryCCServerIdToCCInfoMap.containsKey(cmdbNode.serverId)) {
                    NodeStatusUtils.getNodeStatus(serverIdToAgentVersionMap?.get(cmdbNode.serverId))
                } else {
                    NodeStatus.NOT_IN_CC.name
                },
                nodeType = NodeType.CMDB.name,
                createdUser = userId,
                osName = cmdbNode.getOsNameLessThanMaxLength(),
                operator = cmdbNode.operator,
                bakOperator = cmdbNode.getBakOperatorStrLessThanMaxLength(),
                agentVersion = serverIdToAgentVersionMap?.get(cmdbNode.serverId)?.version,
                hostId = ccInfo?.bkHostId,
                cloudAreaId = ccInfo?.bkCloudId?.toLong(),
                osType = ccInfo?.osType,
                serverId = cmdbNode.serverId
            )
        }
        val time5 = LocalDateTime.now()
        if (logger.isDebugEnabled)
            logger.debug(
                "[addCmdbNodesByServerId]toAddNodeList: " +
                    toAddNodeList.joinToString(separator = ", ", transform = { it.toString() })
            )
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            cmdbNodeDao.batchInsertNode(context, toAddNodeList)
            val insertedNodeList = cmdbNodeDao.listServerNodesByIps(
                dslContext = context, projectId = projectId, ips = toAddNodeList.map { it.nodeIp }
            )
            batchRegisterNodePermissionAndAudit(
                insertedNodeList = insertedNodeList, userId = userId, projectId = projectId
            )
        }
        val time6 = LocalDateTime.now()
        logger.info(
            "[addCmdbNodesByServerId]Total: ${ComputeTimeUtils.calculateDuration(startTime, time6)}s, " +
                "checkUserOperator(cmdb): ${ComputeTimeUtils.calculateDuration(startTime, time1)}s, " +
                "checkImportCount: ${ComputeTimeUtils.calculateDuration(time1, time2)}s, " +
                "addNodeToCCByServerIdMap: ${ComputeTimeUtils.calculateDuration(time2, time3)}s, " +
                "agent versions from nodeman/job: ${ComputeTimeUtils.calculateDuration(time3, time4)}s, " +
                "toAddNodeList: ${ComputeTimeUtils.calculateDuration(time4, time5)}s, " +
                "batchInsertNode: ${ComputeTimeUtils.calculateDuration(time5, time6)}s, "
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
        val notImportedNodeList = unsuccessfullyImportedNodeServerIdList.map {
            NodeAgent(
                nodeIp = serverIdToAgentVersionMap?.get(it)?.ip,
                nodeServerId = it,
                importStatus = IMPORT_STATUS_FAILED
            )
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
     * 验证用户是该机器的责任人（主备负责人）
     * @param nodeIpList CMDB节点IP
     */
    private fun checkUserOperatorByIp(
        userId: String,
        nodeIpList: List<String>
    ): Map<String, CmdbServerDTO> {
        val ipToCmdbServerMap = tencentCmdbService.queryServerByIp(nodeIpList.toSet())
        // 权限校验
        val invalidIps = nodeIpList.filter {
            if (!ipToCmdbServerMap.containsKey(it)) {
                true
            } else {
                val cmdbServer = ipToCmdbServerMap[it]
                (cmdbServer == null) || !cmdbServer.hasOperatorOrBak(userId)
            }
        }
        if (invalidIps.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ERROR_NODE_IP_ILLEGAL_USER,
                params = arrayOf(invalidIps.joinToString(","), userId)
            )
        }
        return ipToCmdbServerMap
    }

    /**
     * 验证用户是该机器的责任人（主备负责人）
     * @param serverIdList CMDB节点serverId
     */
    private fun checkUserOperatorByServerId(
        userId: String,
        serverIdList: List<Long>
    ): Map<Long, CmdbServerDTO> {
        val serverIdToCmdbServerMap = tencentCmdbService.queryServerByServerId(serverIdList.toSet())
        if (serverIdToCmdbServerMap.isNotEmpty()) {
            val invalidServerIdList = serverIdList.filter {
                if (!serverIdToCmdbServerMap.containsKey(it)) {
                    true
                } else {
                    val cmdbServer = serverIdToCmdbServerMap[it]
                    (cmdbServer == null) || !cmdbServer.hasOperatorOrBak(userId)
                }
            }
            val invalidIpList = invalidServerIdList.mapNotNull {
                serverIdToCmdbServerMap[it]?.getFirstIp()
            }
            if (invalidIpList.isNotEmpty()) {
                throw ErrorCodeException(
                    errorCode = ERROR_NODE_IP_ILLEGAL_USER,
                    params = arrayOf(invalidIpList.joinToString(","), userId)
                )
            }
        }
        return serverIdToCmdbServerMap
    }

    /**
     * 将节点添加到CC中
     * 四类节点情况：（2.1、2.2为两种不能导入CC的情况，2.*为所有不允许导入CC的情况，2.1U2.2 <= 2.*）
     * 1. 单ip/多ip，在CC - 查CCInfo并更新信息；
     * 2.1 单ip/多ip，不在cc，不能导入CC - ieg的机器，需要用户手动处理，调用侧导入蓝盾并更新节点状态为NOT_IN_CC
     * 2.2 多ip，不在cc，不能导入CC - 有ip已经在CC中，本次导入去掉这个ip不导入，调用侧导入蓝盾并更新节点状态为NOT_IN_CC
     * 3. 单ip/多ip，不在cc，可以导入CC - 导入，查CCInfo更新信息
     *
     * @return 无论在不在CC中的节点信息 CCInfo
     */
    private fun addNodeToCCByIpMap(toAddIpToCmdbNodeMap: Map<String, CmdbServerDTO>): List<CCHost> {
        // 通过节点svrId查询：节点是否在CC中
        val serverIdToCmdbNodeMap = toAddIpToCmdbNodeMap.values.associateBy { it.serverId }
        val svrIdList = toAddIpToCmdbNodeMap.map { it.value.serverId }
        val (ccHostList, inCCSvrIdList, notInCCSvrIdList) = checkNodeInCCBySvrId(svrIdList)
        // 1. 在CC中，通过svrId查出host_id、云区域id、操作系统类型
        var queryCCInfoList: List<CCHost> = listOf()
        if (inCCSvrIdList.isNotEmpty()) {
            queryCCInfoList = ccHostList.map {
                it.osType = getOsTypeByCCCode(it.osType)
                it
            } ?: listOf()
        }
        // 2.1 通过svrId查询：是否为ieg的机器，即运维部门id == 3（若为ieg的机器，需要用户手动处理，改机器业务为CC对应的二级业务，同步到CC中，此接口无法导入，调用侧设置状态为NOT_IN_CC）
        val toAddIpToCmdbNodeMutableMap = toAddIpToCmdbNodeMap.toMutableMap()
        toAddIpToCmdbNodeMutableMap.entries.retainAll {
            it.value.serverId in notInCCSvrIdList && IEG_DEPT_ID != it.value.deptId
        }
        // 2.2 通过ip查询：多ip节点是否有ip已经在CC中 (如果多ip节点已经有ip在CC中，则本次导入去掉这个ip不导入，调用侧设置状态为NOT_IN_CC)
        val toAddIpList = toAddIpToCmdbNodeMutableMap.keys
        val ipQueryCCRes = tencentCCService.listHostsWithoutBiz(
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
        var addToCCInfoList: List<CCHost> = listOf()
        val notInCCAddSvrIdList = toAddIpToCmdbNodeMutableMap.values.map { it.serverId }
        if (notInCCSvrIdList.isNotEmpty()) {
            val addToCCResp = tencentCCService.addHostToCiBiz(notInCCAddSvrIdList)
            val (ccHostList, _, _) = checkNodeInCCBySvrId(notInCCAddSvrIdList)
            val hostIdToCCInfoMap = ccHostList.associateBy { it.bkHostId }
            val ccHostIdList = addToCCResp.data?.bkHostIds
            addToCCInfoList = ccHostIdList?.mapIndexed { index, value ->
                CCHost(
                    svrId = notInCCSvrIdList[index],
                    bkHostId = value,
                    bkHostInnerip = serverIdToCmdbNodeMap[notInCCSvrIdList[index]]?.getFirstIp(),
                    bkCloudId = hostIdToCCInfoMap?.get(value)?.bkCloudId,
                    osType = getOsTypeByCCCode(hostIdToCCInfoMap?.get(value)?.osType)
                )
            } ?: listOf()
            logger.info(
                "[addNodeToCCByIpMap]addToCCInfo: " +
                    addToCCInfoList.joinToString(separator = ", ", transform = { it.toString() })
            )
        }
        return queryCCInfoList + addToCCInfoList
    }

    private fun addNodeToCCByServerIdMap(toAddServerIdToCmdbNodeMap: Map<Long, CmdbServerDTO>): List<CCHost> {
        // 通过节点svrId查询：节点是否在CC中
        val serverIdToCmdbNodeMap = toAddServerIdToCmdbNodeMap.values.associateBy { it.serverId }
        val svrIdList = toAddServerIdToCmdbNodeMap.mapNotNull { it.value.serverId }
        val (ccHostList, inCCSvrIdList, notInCCSvrIdList) = checkNodeInCCBySvrId(svrIdList)
        // 1. 在CC中，通过svrId查出host_id、云区域id、操作系统类型
        var queryCCInfoList: List<CCHost> = listOf()
        if (inCCSvrIdList.isNotEmpty()) {
            queryCCInfoList = ccHostList.map {
                it.osType = getOsTypeByCCCode(it.osType)
                it
            } ?: listOf()
        }
        // 2.1 通过svrId查询：是否为ieg的机器，即运维部门id == 3（若为ieg的机器，需要用户手动处理，改机器业务为CC对应的二级业务，同步到CC中，此接口无法导入，调用侧设置状态为NOT_IN_CC）
        val toAddServerIdToCmdbNodeMutableMap = toAddServerIdToCmdbNodeMap.toMutableMap()
        toAddServerIdToCmdbNodeMutableMap.entries.retainAll {
            it.value.serverId in notInCCSvrIdList && IEG_DEPT_ID != it.value.deptId
        }
        // 2.2 通过ip查询：多ip节点是否有ip已经在CC中 (如果多ip节点已经有ip在CC中，则本次导入去掉这个ip不导入，调用侧设置状态为NOT_IN_CC)
        val toAddIpList = toAddServerIdToCmdbNodeMutableMap.values.mapNotNull { it.getFirstIp() }
        val ipQueryCCRes = tencentCCService.listHostsWithoutBiz(
            listOf(FIELD_BK_HOST_ID, FIELD_BK_CLOUD_ID, FIELD_BK_HOST_INNERIP, FIELD_BK_SVR_ID, FIELD_BK_OS_TYPE),
            toAddIpList,
            FIELD_BK_HOST_INNERIP
        )
        val inCCIpInfo = ipQueryCCRes.data?.info
        if (!inCCIpInfo.isNullOrEmpty()) {
            inCCIpInfo.map { ipInfo ->
                toAddIpList.forEach {
                    val ipInCC = ipInfo.bkHostInnerip?.contains(it) ?: false
                    if (ipInCC) toAddServerIdToCmdbNodeMutableMap.remove(ipInfo.svrId)
                }
            }
        }
        // 3. 不在CC中，add到CC中，查出host_id、云区域id和操作系统类型
        var addToCCHostList: List<CCHost> = listOf()
        val notInCCAddSvrIdList = toAddServerIdToCmdbNodeMutableMap.values.map { it.serverId }
        if (notInCCSvrIdList.isNotEmpty()) {
            val addToCCResp = tencentCCService.addHostToCiBiz(notInCCAddSvrIdList)
            val (ccHostList, _, _) = checkNodeInCCBySvrId(notInCCAddSvrIdList)
            val hostIdToCCInfoMap = ccHostList.associateBy { it.bkHostId }
            val ccHostIdList = addToCCResp.data?.bkHostIds
            addToCCHostList = ccHostIdList?.mapIndexed { index, value ->
                CCHost(
                    svrId = notInCCSvrIdList[index],
                    bkHostId = value,
                    bkHostInnerip = serverIdToCmdbNodeMap[notInCCSvrIdList[index]]?.getFirstIp(),
                    bkCloudId = hostIdToCCInfoMap?.get(value)?.bkCloudId,
                    osType = getOsTypeByCCCode(hostIdToCCInfoMap?.get(value)?.osType)
                )
            } ?: emptyList()
            logger.info(
                "[addNodeToCCByServerIdMap]addToCCInfo: " +
                    addToCCHostList.joinToString(separator = ", ", transform = { it.toString() })
            )
        }
        return queryCCInfoList + addToCCHostList
    }

    fun checkNodeInCCBySvrId(svrIdList: List<Long>): Triple<List<CCHost>, List<Long>, List<Long>> {
        val ccHostList = tencentCCService.listHostByServerId(svrIdList.toSet())
        val svrIdToCCHostMap = ccHostList.associateBy { it.svrId }
        val inCCSvrIdList = mutableListOf<Long>()
        val notInCCSvrIdList = mutableListOf<Long>()
        svrIdList.map {
            if (svrIdToCCHostMap.containsKey(it)) {
                inCCSvrIdList.add(it)
            } else {
                notInCCSvrIdList.add(it)
            }
        }
        if (notInCCSvrIdList.isNotEmpty()) {
            logger.info("notInCCSvrIdList=${notInCCSvrIdList.joinToString()}")
        }
        return Triple(ccHostList, inCCSvrIdList, notInCCSvrIdList)
    }

    fun getOsTypeByCCCode(ccCode: String?): String? {
        return when (ccCode) {
            OS_TYPE_CC_CODE_LINUX -> OsType.LINUX.name
            OS_TYPE_CC_CODE_WINDOWS -> OsType.WINDOWS.name
            OS_TYPE_CC_CODE_AIX -> OsType.AIX.name
            OS_TYPE_CC_CODE_SOLARIS -> OsType.SOLARIS.name
            OS_TYPE_CC_CODE_UNIX -> OsType.UNIX.name
            OS_TYPE_CC_CODE_FREEBSD -> OsType.FREEBSD.name
            null -> null
            else -> OsType.OTHER.name
        }
    }

    private fun batchRegisterNodePermissionAndAudit(
        insertedNodeList: List<TNodeRecord>,
        userId: String,
        projectId: String
    ) {
        val resourceAuthorizationList = mutableListOf<ResourceAuthorizationDTO>()
        insertedNodeList.forEach {
            val nodeName = "${NodeStringIdUtils.getNodeStringId(it)}(${it.nodeIp})"
            environmentPermissionService.createNode(
                userId = userId,
                projectId = projectId,
                nodeId = it.nodeId,
                nodeName = nodeName
            )
            resourceAuthorizationList.add(
                ResourceAuthorizationDTO(
                    projectCode = projectId,
                    resourceType = ResourceTypeId.ENV_NODE,
                    resourceName = nodeName,
                    resourceCode = HashUtil.encodeLongId(it.nodeId),
                    handoverTime = LocalDateTime.now().timestampmilli(),
                    handoverFrom = userId
                )
            )
            // audit
            ActionAuditContext.current()
                .addInstanceInfo(it.nodeId.toString(), nodeName, null, null)
        }
        envNodeAuthorizationService.addResourceAuthorization(
            projectId = projectId,
            resourceAuthorizationList = resourceAuthorizationList
        )
    }
}
