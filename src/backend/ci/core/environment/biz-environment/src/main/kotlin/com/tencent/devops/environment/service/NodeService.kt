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

package com.tencent.devops.environment.service

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.constant.ALIAS
import com.tencent.devops.common.api.constant.IMPORTER
import com.tencent.devops.common.api.constant.LATEST_EXECUTE_PIPELINE
import com.tencent.devops.common.api.constant.LATEST_EXECUTE_TIME
import com.tencent.devops.common.api.constant.LATEST_MODIFIER
import com.tencent.devops.common.api.constant.LATEST_UPDATE_TIME
import com.tencent.devops.common.api.constant.USAGE
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.CsvUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.api.ServiceAgentResource
import com.tencent.devops.environment.constant.EnvironmentMessageCode.AGENT_STATUS
import com.tencent.devops.environment.constant.EnvironmentMessageCode.AGENT_VERSION
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_ENV_NO_DEL_PERMISSSION
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_CHANGE_USER_NOT_SUPPORT
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NAME_DUPLICATE
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NAME_OR_ID_INVALID
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NO_EDIT_PERMISSSION
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NO_IMPORT_PERMISSION_NODES
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_TYPE_TO_CHANGE_CREATOR_ONLY_SUPPORT_CMDB
import com.tencent.devops.environment.constant.EnvironmentMessageCode.NODE_USAGE_BUILD
import com.tencent.devops.environment.constant.EnvironmentMessageCode.NODE_USAGE_DEPLOYMENT
import com.tencent.devops.environment.constant.EnvironmentMessageCode.OS_TYPE
import com.tencent.devops.environment.constant.T_NODE_NODE_ID
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.NodeTagDao
import com.tencent.devops.environment.dao.slave.SlaveGatewayDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.NodeFetchReq
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.pojo.enums.OsType
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentBuildDetail
import com.tencent.devops.environment.service.node.NodeActionFactory
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import com.tencent.devops.environment.utils.AgentStatusUtils.getAgentStatus
import com.tencent.devops.environment.utils.NodeStringIdUtils
import com.tencent.devops.environment.utils.NodeUtils
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import jakarta.servlet.http.HttpServletResponse
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class NodeService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao,
    private val envDao: EnvDao,
    private val envNodeDao: EnvNodeDao,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val slaveGatewayService: SlaveGatewayService,
    private val environmentPermissionService: EnvironmentPermissionService,
    private val slaveGatewayDao: SlaveGatewayDao,
    private val nodeTagDao: NodeTagDao,
    private val nodeTagService: NodeTagService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(NodeService::class.java)
    }

    @ActionAuditRecord(
        actionId = ActionId.ENV_NODE_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENV_NODE
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENV_NODE_DELETE_CONTENT
    )
    fun deleteNodes(userId: String, projectId: String, nodeLongIds: List<Long>) {
        val canDeleteNodeIds =
            environmentPermissionService.listNodeByPermission(userId, projectId, AuthPermission.DELETE) // 用户所有有权限的 节点id
        val existNodeList = nodeDao.listByIds(dslContext, projectId, nodeLongIds) // 所有要删的且有记录的 节点id记录
        if (existNodeList.isEmpty()) {
            return
        }
        val existNodeIdList = existNodeList.map { it.nodeId } // 所有要删的且有记录的 节点id

        val unauthorizedNodeIds = existNodeIdList.filterNot { canDeleteNodeIds.contains(it) }
        if (unauthorizedNodeIds.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ERROR_ENV_NO_DEL_PERMISSSION,
                params = arrayOf(unauthorizedNodeIds.joinToString(",") { HashUtil.encodeLongId(it) })
            )
        }
        existNodeList.forEach {
            ActionAuditContext.current()
                .addInstanceInfo(
                    it.nodeId.toString(),
                    it.nodeId.toString(),
                    null,
                    null
                )
        }
        NodeActionFactory.load(NodeActionFactory.Action.DELETE)?.action(existNodeList)

        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            nodeDao.batchDeleteNode(context, projectId, existNodeIdList)
            envNodeDao.deleteByNodeIds(context, projectId, existNodeIdList)
            existNodeIdList.forEach {
                environmentPermissionService.deleteNode(projectId, it)
            }
            // 删除节点相关标签
            nodeTagDao.deleteByNodes(dslContext, existNodeIdList)
        }
    }

    @ActionAuditRecord(
        actionId = ActionId.ENV_NODE_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENV_NODE
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENV_NODE_DELETE_CONTENT
    )
    fun deleteNodeByAgentId(userId: String, projectId: String, agentId: String) {
        val node = thirdPartyAgentDao.getAgent(dslContext, HashUtil.decodeIdToLong(agentId))?.nodeId ?: return
        deleteNodes(userId, projectId, listOf(node))
    }

    fun hasCreatePermission(userId: String, projectId: String): Boolean {
        return environmentPermissionService.checkNodePermission(userId, projectId, AuthPermission.CREATE)
    }

    fun list(userId: String, projectId: String): List<NodeWithPermission> {
        val nodeRecordList = nodeDao.listNodes(dslContext, projectId)
        if (nodeRecordList.isEmpty()) {
            return emptyList()
        }

        return formatNodeWithPermissions(userId, projectId, nodeRecordList)
    }

    fun listNew(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
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
        data: NodeFetchReq?
    ): Page<NodeWithPermission> {
        val tagValues = if (data?.tags.isNullOrEmpty()) {
            null
        } else {
            val t = mutableSetOf<Long>()
            data?.tags?.forEach { tag ->
                t.addAll(tag.tagValues ?: return@forEach)
            }
            t
        }
        val nodeRecordList =
            if (-1 != page) {
                val sqlLimit = PageUtil.convertPageSizeToSQLMAXLimit(page ?: 1, pageSize ?: 20, 200)
                nodeDao.listNodesWithPageLimitAndSearchCondition(
                    dslContext = dslContext,
                    projectId = projectId,
                    limit = sqlLimit.limit,
                    offset = sqlLimit.offset,
                    nodeIp = nodeIp,
                    displayName = displayName,
                    createdUser = createdUser,
                    lastModifiedUser = lastModifiedUser,
                    keywords = keywords,
                    nodeType = nodeType,
                    nodeStatus = nodeStatus,
                    agentVersion = agentVersion,
                    osName = osName,
                    latestBuildPipelineId = latestBuildPipelineId,
                    latestBuildTimeStart = latestBuildTimeStart,
                    latestBuildTimeEnd = latestBuildTimeEnd,
                    sortType = sortType,
                    collation = collation,
                    tagValueIds = tagValues
                )
            } else {
                nodeDao.listNodes(dslContext = dslContext, projectId = projectId, nodeType = nodeType)
            }
        if (nodeRecordList.isEmpty()) {
            return Page(1, 0, 0, emptyList())
        }
        val count = nodeDao.countForAuthWithSearchCondition(
            dslContext = dslContext,
            projectId = projectId,
            nodeIp = nodeIp,
            displayName = displayName,
            createdUser = createdUser,
            lastModifiedUser = lastModifiedUser,
            keywords = keywords,
            nodeType = nodeType,
            nodeStatus = nodeStatus,
            agentVersion = agentVersion,
            osName = osName,
            latestBuildPipelineId = latestBuildPipelineId,
            latestBuildTimeStart = latestBuildTimeStart,
            latestBuildTimeEnd = latestBuildTimeEnd,
            sortType = sortType,
            collation = collation,
            tagValueIds = tagValues
        ).toLong()
        val nodes = formatNodeWithPermissions(userId, projectId, nodeRecordList)
        if (-1 != page) {
            val nodesMap = nodes.associateBy { it.agentHashId }
            val agentIds = nodesMap.keys.mapNotNull { it }
            agentIds.chunked(100).forEach { agentHashIds ->
                val agentBuilds = client.get(ServiceAgentResource::class).listLatestBuildPipelines(
                    agentIds = agentHashIds
                )
                agentBuilds.forEach build@{ build ->
                    val node = nodesMap[build.agentId] ?: return@build
                    node.latestBuildDetail = AgentBuildDetail(
                        nodeId = node.nodeId,
                        agentId = build.agentId,
                        projectId = build.projectId,
                        pipelineId = build.pipelineId,
                        pipelineName = build.pipelineName,
                        buildId = build.buildId,
                        buildNumber = build.buildNum,
                        vmSetId = build.vmSeqId,
                        taskName = build.taskName,
                        status = build.status,
                        createdTime = build.createdTime,
                        updatedTime = build.updatedTime,
                        workspace = build.workspace,
                        agentTask = null
                    )
                }
            }
        }
        val records = if (sortType == null) NodeUtils.sortByUser(nodes = nodes, userId = userId) else nodes
        return Page(
            page = page ?: 1,
            pageSize = pageSize ?: 20,
            count = count,
            records = records
        )
    }

    fun fetchNodesCount(projectId: String): Map<NodeType, Int> {
        return nodeDao.fetchProjectNodeCount(dslContext, projectId)
    }

    fun listNewExport(
        userId: String,
        projectId: String,
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
        data: NodeFetchReq?,
        response: HttpServletResponse
    ) {
        var page = 1
        val pageSize = 100
        var count = Long.MAX_VALUE
        val dataList = mutableListOf<Array<String?>>()
        while (page * pageSize < count) {
            val res = listNew(
                userId = userId,
                projectId = projectId,
                page = page,
                pageSize = 100,
                nodeIp = nodeIp,
                displayName = displayName,
                createdUser = createdUser,
                lastModifiedUser = lastModifiedUser,
                keywords = keywords,
                nodeType = nodeType,
                nodeStatus = nodeStatus,
                agentVersion = agentVersion,
                osName = osName,
                latestBuildPipelineId = latestBuildPipelineId,
                latestBuildTimeStart = latestBuildTimeStart,
                latestBuildTimeEnd = latestBuildTimeEnd,
                sortType = sortType,
                collation = collation,
                data = data
            )
            count = res.count
            page++
            res.records.forEach { record ->
                val dataArray = arrayOfNulls<String>(11)
                dataArray[0] = record.displayName ?: ""
                dataArray[1] = record.ip
                dataArray[2] = record.osName ?: ""
                dataArray[3] = record.nodeStatus
                dataArray[4] = record.agentVersion ?: ""
                dataArray[5] = if (record.nodeType == NodeType.THIRDPARTY.name)
                    I18nUtil.getCodeLanMessage(NODE_USAGE_BUILD)
                else
                    I18nUtil.getCodeLanMessage(NODE_USAGE_DEPLOYMENT)
                dataArray[6] = record.createdUser
                dataArray[7] = record.lastModifyUser ?: ""
                dataArray[8] = record.lastModifyTime ?: ""
                dataArray[9] = record.latestBuildDetail?.pipelineName ?: ""
                dataArray[10] = record.lastBuildTime ?: ""
                dataList.add(dataArray)
            }
        }

        val headers = arrayOf(
            /*0:别名*/I18nUtil.getCodeLanMessage(ALIAS),
            /*1:IP*/ "IP",
            /*2:操作系统*/I18nUtil.getCodeLanMessage(OS_TYPE),
            /*3:Agent状态*/I18nUtil.getCodeLanMessage(AGENT_STATUS),
            /*4:Agent版本*/I18nUtil.getCodeLanMessage(AGENT_VERSION),
            /*5:用途*/I18nUtil.getCodeLanMessage(USAGE),
            /*6:导入人*/I18nUtil.getCodeLanMessage(IMPORTER),
            /*7:最近修改人*/I18nUtil.getCodeLanMessage(LATEST_MODIFIER),
            /*8:最近修改时间*/I18nUtil.getCodeLanMessage(LATEST_UPDATE_TIME),
            /*9:最近执行流水线*/I18nUtil.getCodeLanMessage(LATEST_EXECUTE_PIPELINE),
            /*10:最近执行时间*/I18nUtil.getCodeLanMessage(LATEST_EXECUTE_TIME)
        )
        val bytes = CsvUtil.writeCsv(headers, dataList)
        CsvUtil.setCsvResponse("$projectId-environment-nodes-data", bytes, response)
    }

    fun formatNodeWithPermissions(
        userId: String,
        projectId: String,
        nodeRecordList: List<TNodeRecord>
    ): List<NodeWithPermission> {
        val nodeListResult = environmentPermissionService.listNodeByRbacPermission(
            userId = userId,
            projectId = projectId,
            nodeRecordList = nodeRecordList,
            authPermission = AuthPermission.LIST
        )
        if (nodeListResult.isEmpty()) return emptyList()
        val permissionMap = environmentPermissionService.listNodeByPermissions(
            userId = userId,
            projectId = projectId,
            permissions = setOf(AuthPermission.USE, AuthPermission.EDIT, AuthPermission.DELETE)
        )
        val canViewNodeIds = environmentPermissionService.listNodeByRbacPermission(
            userId = userId,
            projectId = projectId,
            nodeRecordList = nodeListResult,
            authPermission = AuthPermission.VIEW
        ).map { it.nodeId }

        val canUseNodeIds = permissionMap.takeIf { it.containsKey(AuthPermission.USE) }.run {
            permissionMap[AuthPermission.USE]?.map { HashUtil.decodeIdToLong(it) } ?: emptyList()
        }

        val canEditNodeIds = permissionMap.takeIf { it.containsKey(AuthPermission.EDIT) }.run {
            permissionMap[AuthPermission.EDIT]?.map { HashUtil.decodeIdToLong(it) } ?: emptyList()
        }
        val canDeleteNodeIds = permissionMap.takeIf { it.containsKey(AuthPermission.DELETE) }.run {
            permissionMap[AuthPermission.DELETE]?.map { HashUtil.decodeIdToLong(it) } ?: emptyList()
        }
        val thirdPartyAgentNodeIds = nodeListResult.filter { it.nodeType == NodeType.THIRDPARTY.name }.map { it.nodeId }
        val thirdPartyAgentMap = if (thirdPartyAgentNodeIds.isNotEmpty()) {
            thirdPartyAgentDao.getAgentsByNodeIds(dslContext, thirdPartyAgentNodeIds, projectId)
                .associateBy { it.nodeId }
        } else {
            emptyMap()
        }
        val tagMaps = if (thirdPartyAgentNodeIds.isNotEmpty()) {
            nodeTagService.fetchNodeTags(projectId, thirdPartyAgentNodeIds.toSet())
        } else {
            emptyMap()
        }

        val nodeEnvs = envNodeDao.listNodeIds(dslContext, projectId, nodeListResult.map { it.nodeId })
        val envInfos = envDao.listServerEnvByIdsAllType(
            dslContext, nodeEnvs.map { it.envId }.toSet()
        ).associateBy { it.envId }
        val nodeEnvsGroups = nodeEnvs.groupBy({ it.nodeId }, { envInfos[it.envId]?.envName ?: "" })

        return nodeListResult.map {
            val thirdPartyAgent = thirdPartyAgentMap[it.nodeId]
            val gatewayShowName = if (thirdPartyAgent != null) {
                slaveGatewayService.getShowName(thirdPartyAgent.gateway)
            } else ""

            val nodeStringId = NodeStringIdUtils.getNodeStringId(it)
            NodeWithPermission(
                nodeHashId = HashUtil.encodeLongId(it.nodeId),
                nodeId = it.nodeId.toString(),
                name = it.nodeName,
                ip = it.nodeIp,
                nodeStatus = it.nodeStatus,
                taskId = it.taskId,
                nodeType = it.nodeType,
                osName = it.osName,
                createdUser = it.createdUser,
                operator = it.operator,
                bakOperator = it.bakOperator,
                canUse = canUseNodeIds.contains(it.nodeId),
                canEdit = canEditNodeIds.contains(it.nodeId),
                canDelete = canDeleteNodeIds.contains(it.nodeId),
                canView = canViewNodeIds.contains(it.nodeId),
                gateway = gatewayShowName,
                displayName = NodeStringIdUtils.getRefineDisplayName(nodeStringId, it.displayName),
                createTime = if (null == it.createdTime) {
                    ""
                } else {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.createdTime)
                },
                lastModifyTime = if (null == it.lastModifyTime) {
                    ""
                } else {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.lastModifyTime)
                },
                lastModifyUser = it.lastModifyUser ?: "",
                agentStatus = getAgentStatus(it),
                agentVersion = it.agentVersion,
                agentHashId = HashUtil.encodeLongId(thirdPartyAgent?.id ?: 0L),
                cloudAreaId = it.cloudAreaId,
                osType = if (OsType.UNIX.name == it.osType || OsType.FREEBSD.name == it.osType) {
                    OsType.LINUX.name // 节点管理中 UNIX，FREEBSD 这两种状态归属于LINUX
                } else {
                    it.osType
                },
                bkHostId = it.hostId,
                serverId = it.serverId,
                size = it.size,
                envNames = nodeEnvsGroups[it.nodeId],
                lastBuildTime = if (null == it.lastBuildTime) {
                    ""
                } else {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.lastBuildTime)
                },
                tags = tagMaps[it.nodeId]
            )
        }
    }

    fun getNodeStatus(
        userId: String,
        projectId: String,
        nodeHashId: String?,
        nodeName: String?,
        agentHashId: String?
    ): NodeWithPermission {
        val hashId = when {
            nodeHashId != null -> nodeHashId
            nodeName != null -> nodeDao.getByDisplayName(dslContext, projectId, nodeName, null)
                .firstOrNull()?.nodeHashId

            agentHashId != null -> thirdPartyAgentDao.getAgent(dslContext, HashUtil.decodeIdToLong(agentHashId))
                ?.nodeId?.let { HashUtil.encodeLongId(it) }

            else -> null
        } ?: throw ErrorCodeException(
            errorCode = ERROR_NODE_NAME_OR_ID_INVALID
        )
        return listByHashIds(userId, projectId, listOf(hashId)).firstOrNull() ?: throw ErrorCodeException(
            errorCode = ERROR_NODE_NOT_EXISTS,
            params = arrayOf(hashId)
        )
    }

    fun listByHashIds(userId: String, projectId: String, hashIds: List<String>): List<NodeWithPermission> {
        val nodeIds = hashIds.map { HashUtil.decodeIdToLong(it) }
        val nodeRecordList = nodeDao.listAllByIds(dslContext, projectId, nodeIds)
        if (nodeRecordList.isEmpty()) {
            return emptyList()
        }

        val permissionMap = environmentPermissionService.listNodeByPermissions(
            userId, projectId,
            permissions = setOf(AuthPermission.USE, AuthPermission.EDIT, AuthPermission.DELETE)
        )

        val canViewNodeIds = environmentPermissionService.listNodeByRbacPermission(
            userId = userId,
            projectId = projectId,
            nodeRecordList = nodeRecordList,
            authPermission = AuthPermission.VIEW
        ).map { it.nodeId }

        val canUseNodeIds = if (permissionMap.containsKey(AuthPermission.USE)) {
            permissionMap[AuthPermission.USE]?.map { HashUtil.decodeIdToLong(it) } ?: emptyList()
        } else {
            emptyList()
        }
        val canEditNodeIds = if (permissionMap.containsKey(AuthPermission.EDIT)) {
            permissionMap[AuthPermission.EDIT]?.map { HashUtil.decodeIdToLong(it) } ?: emptyList()
        } else {
            emptyList()
        }
        val canDeleteNodeIds = if (permissionMap.containsKey(AuthPermission.DELETE)) {
            permissionMap[AuthPermission.DELETE]?.map { HashUtil.decodeIdToLong(it) } ?: emptyList()
        } else {
            emptyList()
        }
        val nodeListResult = environmentPermissionService.listNodeByRbacPermission(
            userId = userId,
            projectId = projectId,
            nodeRecordList = nodeRecordList,
            authPermission = AuthPermission.LIST
        )
        if (nodeListResult.isEmpty()) return emptyList()
        val thirdPartyAgentNodeIds = nodeRecordList.filter { it.nodeType == NodeType.THIRDPARTY.name }.map { it.nodeId }
        if (thirdPartyAgentNodeIds.isEmpty()) return emptyList()
        val thirdPartyAgentMap =
            thirdPartyAgentDao.getAgentsByNodeIds(dslContext, thirdPartyAgentNodeIds, projectId)
                .associateBy { it.nodeId }
        return nodeListResult.map {
            val thirdPartyAgent = thirdPartyAgentMap[it.nodeId]
            val gatewayShowName = if (thirdPartyAgent != null) {
                slaveGatewayService.getShowName(thirdPartyAgent.gateway)
            } else {
                ""
            }
            val nodeStringId = NodeStringIdUtils.getNodeStringId(it)
            NodeWithPermission(
                nodeHashId = HashUtil.encodeLongId(it.nodeId),
                nodeId = it.nodeId.toString(),
                name = it.nodeName,
                ip = it.nodeIp,
                nodeStatus = NodeStatus.getStatusName(it.nodeStatus),
                nodeType = NodeType.getTypeName(it.nodeType),
                osName = it.osName,
                createdUser = it.createdUser,
                operator = it.operator,
                bakOperator = it.bakOperator,
                canUse = canUseNodeIds.contains(it.nodeId),
                canEdit = canEditNodeIds.contains(it.nodeId),
                canDelete = canDeleteNodeIds.contains(it.nodeId),
                canView = canViewNodeIds.contains(it.nodeId),
                gateway = gatewayShowName,
                displayName = NodeStringIdUtils.getRefineDisplayName(nodeStringId, it.displayName),
                createTime = if (null == it.createdTime) {
                    ""
                } else {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.createdTime)
                },
                lastModifyTime = if (null == it.lastModifyTime) {
                    ""
                } else {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.lastModifyTime)
                },
                lastModifyUser = it.lastModifyUser ?: "",
                agentStatus = getAgentStatus(it),
                agentVersion = it.agentVersion,
                agentHashId = HashUtil.encodeLongId(thirdPartyAgent?.id ?: 0L),
                cloudAreaId = it.cloudAreaId,
                taskId = null,
                osType = it.osType,
                serverId = it.serverId
            )
        }
    }

    fun listUsableServerNodes(userId: String, projectId: String): List<NodeWithPermission> {
        val nodeRecordList = nodeDao.listServerNodes(dslContext, projectId)
        if (nodeRecordList.isEmpty()) {
            return emptyList()
        }

        val canUseNodeIds = environmentPermissionService.listNodeByPermission(userId, projectId, AuthPermission.USE)

        val validRecordList = nodeRecordList.filter { canUseNodeIds.contains(it.nodeId) }

        val canViewNodeIds = environmentPermissionService.listNodeByRbacPermission(
            userId = userId,
            projectId = projectId,
            nodeRecordList = nodeRecordList,
            authPermission = AuthPermission.VIEW
        ).map { it.nodeId }

        return validRecordList.map {
            val nodeStringId = NodeStringIdUtils.getNodeStringId(it)
            NodeWithPermission(
                nodeHashId = HashUtil.encodeLongId(it.nodeId),
                nodeId = it.nodeId.toString(),
                name = it.nodeName,
                ip = it.nodeIp,
                nodeStatus = NodeStatus.getStatusName(it.nodeStatus),
                nodeType = NodeType.getTypeName(it.nodeType),
                osName = it.osName,
                createdUser = it.createdUser,
                operator = it.operator,
                bakOperator = it.bakOperator,
                canUse = canUseNodeIds.contains(it.nodeId),
                canEdit = null,
                canDelete = null,
                canView = canViewNodeIds.contains(it.nodeId),
                gateway = "",
                displayName = NodeStringIdUtils.getRefineDisplayName(nodeStringId, it.displayName),
                createTime = if (null == it.createdTime) {
                    ""
                } else {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.createdTime)
                },
                lastModifyTime = if (null == it.lastModifyTime) {
                    ""
                } else {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.lastModifyTime)
                },
                agentStatus = getAgentStatus(it),
                agentVersion = it.agentVersion,
                lastModifyUser = it.lastModifyUser ?: "",
                cloudAreaId = it.cloudAreaId,
                taskId = null,
                osType = it.osType,
                serverId = it.serverId
            )
        }
    }

    fun listRawServerNodeByIds(userId: String, projectId: String, nodeHashIds: List<String>): List<NodeBaseInfo> {
        val nodeRecords =
            nodeDao.listServerNodesByIds(dslContext, projectId, nodeHashIds.map { HashUtil.decodeIdToLong(it) })
        return nodeRecords.map { NodeStringIdUtils.getNodeBaseInfo(it) }
    }

    fun listRawServerNodeByIds(nodeHashIds: List<String>): List<NodeBaseInfo> {
        val nodeRecords =
            nodeDao.listServerNodesByIds(dslContext, nodeHashIds.map { HashUtil.decodeIdToLong(it) })
        return nodeRecords.map { NodeStringIdUtils.getNodeBaseInfo(it) }
    }

    fun listByType(userId: String, projectId: String, type: String): List<NodeBaseInfo> {
        val nodeRecords = nodeDao.listNodesByType(dslContext, projectId, type)
        return nodeRecords.map { NodeStringIdUtils.getNodeBaseInfo(it) }
    }

    fun listByNodeType(userId: String, projectId: String, nodeType: NodeType): List<NodeBaseInfo> {
        val nodeRecords = nodeDao.listNodesByType(dslContext, projectId, nodeType.name)
        return nodeRecords.map { NodeStringIdUtils.getNodeBaseInfo(it) }
    }

    fun changeCreatedUser(userId: String, projectId: String, nodeHashId: String): String {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        val node = nodeDao.get(dslContext, projectId, nodeId) ?: throw ErrorCodeException(
            errorCode = ERROR_NODE_NOT_EXISTS,
            params = arrayOf(nodeHashId)
        )
        when (node.nodeType) {
            NodeType.CMDB.name -> {
                val isOperator = userId == node.operator
                val isBakOperator = node.bakOperator.split(";").contains(userId)
                if (isOperator || isBakOperator) {
                    nodeDao.updateCreatedUser(dslContext, nodeId, userId)
                } else {
                    throw ErrorCodeException(
                        errorCode = ERROR_NODE_NO_EDIT_PERMISSSION,
                        defaultMessage = MessageUtil.getMessageByLocale(
                            messageCode = ERROR_NODE_NO_EDIT_PERMISSSION,
                            language = I18nUtil.getLanguage(userId)
                        )
                    )
                }
            }

            else -> {
                throw ErrorCodeException(
                    errorCode = ERROR_NODE_CHANGE_USER_NOT_SUPPORT,
                    params = arrayOf(NodeType.getTypeName(node.nodeType)),
                    defaultMessage = MessageUtil.getMessageByLocale(
                        messageCode = ERROR_NODE_CHANGE_USER_NOT_SUPPORT,
                        language = I18nUtil.getLanguage(userId),
                        params = arrayOf(NodeType.getTypeName(node.nodeType))
                    )
                )
            }
        }
        return node.displayName
    }

    fun batchChangeCreateUser(
        userId: String,
        projectId: String,
        nodeHashIds: List<String>
    ): List<Pair<String, String>> {
        val nodeList = nodeDao.listAllByIds(
            dslContext,
            projectId,
            nodeHashIds.map { HashUtil.decodeIdToLong(it) }
        )
        checkNodesExists(nodeList, nodeHashIds)
        checkNodesTypeCmdb(nodeList)
        checkNodesImportPermission(userId, nodeList)

        val toChangeNodeIds = nodeList.map { it.nodeId }
        // 分批更新，以免单次where in 大列表
        val nodeIdsInBatch = toChangeNodeIds.chunked(2000)
        nodeIdsInBatch.forEach { it ->
            nodeDao.batchUpdateNodeCreatedUser(dslContext, it, userId)
        }

        return nodeList.map { Pair(it.nodeHashId, it.displayName) }
    }

    private fun checkNodesExists(existedNodeList: List<TNodeRecord>, toChangeNodeHashIds: List<String>) {
        val existNodeHashIdSet = existedNodeList.map { it.nodeHashId }.toSet()
        val noExistNodeHashIdSet = toChangeNodeHashIds.toSet().minus(existNodeHashIdSet)
        if (noExistNodeHashIdSet.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ERROR_NODE_NOT_EXISTS,
                params = arrayOf(noExistNodeHashIdSet.joinToString(","))
            )
        }
    }

    private fun checkNodesTypeCmdb(nodeList: List<TNodeRecord>) {
        val invalidTypeNodeIps: MutableList<String> = mutableListOf()
        nodeList.forEach { it ->
            if (it.nodeType != NodeType.CMDB.name) {
                invalidTypeNodeIps.add(it.nodeIp)
            }
        }
        if (invalidTypeNodeIps.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ERROR_NODE_TYPE_TO_CHANGE_CREATOR_ONLY_SUPPORT_CMDB,
                params = arrayOf(invalidTypeNodeIps.joinToString(","))
            )
        }
    }

    private fun checkNodesImportPermission(userId: String, nodeList: List<TNodeRecord>) {
        val noPermissionNodeIps: MutableList<String> = mutableListOf()
        nodeList.forEach { it ->
            if (!(it.bakOperator.split(";").contains(userId) || it.operator == userId)) {
                noPermissionNodeIps.add(it.nodeIp)
            }
        }
        if (noPermissionNodeIps.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ERROR_NODE_NO_IMPORT_PERMISSION_NODES,
                params = arrayOf(noPermissionNodeIps.joinToString(",")),
                defaultMessage = MessageUtil.getMessageByLocale(
                    messageCode = ERROR_NODE_NO_IMPORT_PERMISSION_NODES,
                    params = arrayOf(noPermissionNodeIps.joinToString(",")),
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
    }

    fun checkCmdbOperator(
        userId: String,
        projectId: String,
        nodeHashId: String
    ): Boolean {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        val node = nodeDao.get(dslContext, projectId, nodeId) ?: throw ErrorCodeException(
            errorCode = ERROR_NODE_NOT_EXISTS,
            defaultMessage = "the node does not exist",
            params = arrayOf(nodeHashId)
        )
        return when (node.nodeType) {
            NodeType.CMDB.name -> {
                val isOperator = userId == node.operator
                val isBakOperator = node.bakOperator.split(";").contains(userId)
                if (isOperator || isBakOperator) {
                    true
                } else {
                    throw ErrorCodeException(
                        errorCode = ERROR_NODE_NO_EDIT_PERMISSSION,
                        defaultMessage = MessageUtil.getMessageByLocale(
                            messageCode = ERROR_NODE_NO_EDIT_PERMISSSION,
                            language = I18nUtil.getLanguage(userId)
                        )
                    )
                }
            }

            else -> {
                throw ErrorCodeException(
                    errorCode = ERROR_NODE_CHANGE_USER_NOT_SUPPORT,
                    params = arrayOf(NodeType.getTypeName(node.nodeType)),
                    defaultMessage = MessageUtil.getMessageByLocale(
                        messageCode = ERROR_NODE_CHANGE_USER_NOT_SUPPORT,
                        language = I18nUtil.getLanguage(userId),
                        params = arrayOf(NodeType.getTypeName(node.nodeType))
                    )
                )
            }
        }
    }

    private fun checkDisplayName(projectId: String, nodeId: Long?, displayName: String) {
        if (nodeDao.isDisplayNameExist(dslContext, projectId, nodeId, displayName)) {
            throw ErrorCodeException(errorCode = ERROR_NODE_NAME_DUPLICATE, params = arrayOf(displayName))
        }
    }

    @ActionAuditRecord(
        actionId = ActionId.ENV_NODE_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENV_NODE
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENV_NODE_EDIT_CONTENT
    )
    fun updateDisplayName(userId: String, projectId: String, nodeHashId: String, displayName: String) {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        val nodeInDb = nodeDao.get(dslContext, projectId, nodeId) ?: throw ErrorCodeException(
            errorCode = ERROR_NODE_NOT_EXISTS,
            params = arrayOf(nodeHashId)
        )
        if (!environmentPermissionService.checkNodePermission(userId, projectId, nodeId, AuthPermission.EDIT)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(
                    ERROR_NODE_NO_EDIT_PERMISSSION,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        checkDisplayName(projectId, nodeId, displayName)
        ActionAuditContext.current()
            .setInstanceId(nodeInDb.nodeId.toString())
            .setOriginInstance(nodeInDb.displayName)
            .setInstanceName(displayName)
            .setInstance(displayName)
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            nodeDao.updateDisplayName(
                dslContext = context,
                nodeId = nodeId,
                nodeName = displayName,
                userId = userId,
                projectId = projectId
            )
            if (nodeInDb.displayName != displayName) {
                environmentPermissionService.updateNode(userId, projectId, nodeId, displayName)
            }
        }
    }

    fun getByDisplayNameNotWithPermission(
        userId: String,
        projectId: String,
        displayName: String,
        nodeType: List<String>? = null
    ): List<NodeBaseInfo> {
        val nodes = nodeDao.getByDisplayName(dslContext, projectId, displayName, nodeType)
        if (nodes.isEmpty()) {
            return emptyList()
        }
        return nodes.map { NodeStringIdUtils.getNodeBaseInfo(it) }
    }

    fun getByDisplayName(
        userId: String,
        projectId: String,
        displayName: String,
        nodeType: List<String>? = null
    ): List<NodeBaseInfo> {
        val nodes = nodeDao.getByDisplayName(dslContext, projectId, displayName, nodeType)
        if (nodes.isEmpty()) {
            return emptyList()
        }

        val canUseNodeIds = environmentPermissionService.listNodeByPermission(userId, projectId, AuthPermission.USE)
        val validRecordList = nodes.filter { canUseNodeIds.contains(it.nodeId) }
        return validRecordList.map { NodeStringIdUtils.getNodeBaseInfo(it) }
    }

    fun listByPage(projectId: String, offset: Int?, limit: Int?): Page<NodeBaseInfo> {
        val nodeInfos = nodeDao.listPageForAuth(dslContext, offset!!, limit!!, projectId)
        val count = nodeDao.countForAuth(dslContext, projectId)
        return Page(
            count = count.toLong(),
            page = offset,
            pageSize = limit,
            records = nodeInfos.map { NodeStringIdUtils.getNodeBaseInfo(it) }
        )
    }

    fun searchByDisplayName(projectId: String, offset: Int?, limit: Int?, displayName: String): Page<NodeBaseInfo> {
        val nodeInfos = nodeDao.searchByDisplayName(
            dslContext = dslContext,
            offset = offset!!,
            limit = limit!!,
            projectId = projectId,
            displayName = displayName
        )
        val count = nodeDao.countByDisplayName(
            dslContext = dslContext,
            project = projectId,
            displayName = displayName
        )
        return Page(
            count = count.toLong(),
            page = offset,
            pageSize = limit,
            records = nodeInfos.map { NodeStringIdUtils.getNodeBaseInfo(it) }
        )
    }

    fun extListNodes(userId: String, projectId: String): List<NodeWithPermission> {
        val nodeRecordList = nodeDao.listThirdpartyNodes(dslContext, projectId)
        if (nodeRecordList.isEmpty()) {
            return emptyList()
        }
        return nodeRecordList.map {
            val nodeStringId = NodeStringIdUtils.getNodeStringId(it)
            NodeStringIdUtils.getRefineDisplayName(nodeStringId, it.displayName)
            NodeWithPermission(
                nodeHashId = HashUtil.encodeLongId(it.nodeId),
                nodeId = it.nodeId.toString(),
                name = it.nodeName,
                ip = it.nodeIp,
                nodeStatus = it.nodeStatus,
                agentStatus = getAgentStatus(it),
                agentVersion = it.agentVersion,
                nodeType = it.nodeType,
                osName = it.osName,
                createdUser = it.createdUser,
                operator = it.operator,
                bakOperator = it.bakOperator,
                canUse = false,
                canEdit = false,
                canDelete = false,
                canView = false,
                gateway = "",
                displayName = NodeStringIdUtils.getRefineDisplayName(nodeStringId, it.displayName),
                createTime = if (null == it.createdTime) {
                    ""
                } else {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.createdTime)
                },
                lastModifyTime = if (null == it.lastModifyTime) {
                    ""
                } else {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.lastModifyTime)
                },
                lastModifyUser = it.lastModifyUser ?: "",
                pipelineRefCount = it.pipelineRefCount ?: 0,
                lastBuildTime = if (null == it.lastBuildTime) {
                    ""
                } else {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.lastBuildTime)
                },
                cloudAreaId = it.cloudAreaId,
                taskId = null,
                osType = it.osType,
                serverId = it.serverId
            )
        }
    }

    fun refreshGateway(oldToNewMap: Map<String, String>): Boolean {
        if (oldToNewMap.isEmpty()) {
            return false
        }
        return try {
            slaveGatewayDao.refreshGateway(dslContext, oldToNewMap)
            thirdPartyAgentDao.refreshGateway(dslContext, oldToNewMap)
            true
        } catch (ignore: Throwable) {
            logger.error("AUTH|refreshGateway failed with error: ", ignore)
            false
        }
    }

    fun getNodeInfosAndCountByType(
        projectId: String,
        nodeType: NodeType,
        limit: Int,
        offset: Int
    ): Pair<List<NodeBaseInfo>, Long> {
        val nodeInfos = nodeDao.listNodesByType(
            dslContext = dslContext,
            projectId = projectId,
            nodeType = NodeType.CMDB.name,
            offset = offset,
            limit = limit
        ).map { NodeStringIdUtils.getNodeBaseInfo(it) }

        val count = nodeDao.countByNodeType(
            dslContext = dslContext,
            projectId = projectId,
            nodeType = NodeType.CMDB
        )
        return Pair(nodeInfos, count)
    }

    fun addHashId() {
        val startTime = System.currentTimeMillis()
        logger.debug("OPRepositoryService:begin addHashId-----------")
        val threadPoolExecutor = ThreadPoolExecutor(
            1,
            1,
            0,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(1),
            Executors.defaultThreadFactory(),
            ThreadPoolExecutor.AbortPolicy()
        )
        threadPoolExecutor.submit {
            logger.debug("NodeService:begin addHashId threadPoolExecutor-----------")
            var offset = 0
            val limit = 1000
            try {
                do {
                    val envRecords = envDao.getAllEnv(dslContext, limit, offset)
                    val envSize = envRecords?.size
                    logger.debug("envSize:$envSize")
                    envRecords?.map {
                        val id = it.value1()
                        val hashId = HashUtil.encodeLongId(it.value1())
                        envDao.updateHashId(dslContext, id, hashId)
                    }
                    offset += limit
                } while (envSize == 1000)
                offset = 0
                do {
                    val nodeRecords = nodeDao.getAllNode(dslContext, limit, offset)
                    val nodeSize = nodeRecords?.size
                    logger.debug("nodeSize:$nodeSize")
                    nodeRecords?.map {
                        val id = it[T_NODE_NODE_ID] as Long
                        val hashId = HashUtil.encodeLongId(it[T_NODE_NODE_ID] as Long)
                        nodeDao.updateHashId(dslContext, id, hashId)
                    }
                    offset += limit
                } while (nodeSize == 1000)
            } catch (e: Exception) {
                logger.warn("NodeService：addHashId failed | $e ")
            } finally {
                threadPoolExecutor.shutdown()
            }
        }
        logger.info("NodeService:finish addHashId-----------")
        logger.info("addhashid time cost: ${System.currentTimeMillis() - startTime}")
    }
}
