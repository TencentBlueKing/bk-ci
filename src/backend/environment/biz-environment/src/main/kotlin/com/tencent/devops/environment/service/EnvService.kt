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

package com.tencent.devops.environment.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.common.misc.client.BcsClient
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.ProjectConfigDao
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.EnvUpdateInfo
import com.tencent.devops.environment.pojo.EnvWithNodeCount
import com.tencent.devops.environment.pojo.EnvWithPermission
import com.tencent.devops.environment.pojo.EnvironmentId
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.enums.EnvType
import com.tencent.devops.environment.pojo.enums.NodeSource
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import com.tencent.devops.environment.utils.AgentStatusUtils.getAgentStatus
import com.tencent.devops.environment.utils.BcsVmNodeStatusUtils
import com.tencent.devops.environment.utils.BcsVmParamCheckUtils.checkAndGetVmCreateParam
import com.tencent.devops.environment.utils.NodeStringIdUtils
import com.tencent.devops.model.environment.tables.records.TEnvNodeRecord
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class EnvService @Autowired constructor(
    private val dslContext: DSLContext,
    private val envDao: EnvDao,
    private val nodeDao: NodeDao,
    private val envNodeDao: EnvNodeDao,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val projectConfigDao: ProjectConfigDao,
    private val bcsClient: BcsClient,
    private val slaveGatewayService: SlaveGatewayService,
    private val environmentPermissionService: EnvironmentPermissionService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(EnvService::class.java)
    }

    private fun checkName(projectId: String, envId: Long?, envName: String) {
        if (envDao.isNameExist(dslContext, projectId, envId, envName)) {
            throw OperationException("环境名称【$envName】已存在")
        }
    }

    fun createEnvironment(userId: String, projectId: String, envCreateInfo: EnvCreateInfo): EnvironmentId {
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, BkAuthPermission.CREATE)) {
            throw OperationException("No Permission")
        }

        checkName(projectId, null, envCreateInfo.name)

        val now = LocalDateTime.now()
        when (envCreateInfo.source) {
            NodeSource.EXISTING -> {
                val nodeLongIds = envCreateInfo.nodeHashIds!!.map { HashUtil.decodeIdToLong(it) }

                // 检查 node 权限
                val canUseNodeIds =
                    environmentPermissionService.listNodeByPermission(userId, projectId, BkAuthPermission.USE)
                val unauthorizedNodeIds = nodeLongIds.filterNot { canUseNodeIds.contains(it) }
                if (unauthorizedNodeIds.isNotEmpty()) {
                    throw OperationException(
                        "节点权限不足 [${unauthorizedNodeIds.joinToString(",") { HashUtil.encodeLongId(it) }}]"
                    )
                }

                // 检查 node 是否存在
                val existNodes = nodeDao.listByIds(dslContext, projectId, nodeLongIds)
                val existNodeIds = existNodes.map { it.nodeId }.toSet()
                val notExistNodeIds = nodeLongIds.filterNot { existNodeIds.contains(it) }
                if (notExistNodeIds.isNotEmpty()) {
                    throw OperationException("节点 [${notExistNodeIds.joinToString(",") { HashUtil.encodeLongId(it) }}] 不存在")
                }

                var envId = 0L
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)
                    envId = envDao.create(
                        context, userId, projectId, envCreateInfo.name, envCreateInfo.desc,
                        envCreateInfo.envType.name, ObjectMapper().writeValueAsString(envCreateInfo.envVars)
                    )
                    val envNodeList = nodeLongIds.map { TEnvNodeRecord(envId, it, projectId) }
                    envNodeDao.batchStoreEnvNode(context, envNodeList)

                    environmentPermissionService.createEnv(userId, projectId, envId, envCreateInfo.name)
                }
                return EnvironmentId(HashUtil.encodeLongId(envId))
            }
            NodeSource.CREATE -> {
                // 创建 BCSVM 节点
                val vmCreateInfoP = checkAndGetVmCreateParam(
                    dslContext = dslContext,
                    projectConfigDao = projectConfigDao,
                    nodeDao = nodeDao,
                    projectId = projectId,
                    userId = userId,
                    vmParam = envCreateInfo.bcsVmParam!!
                )
                val bcsVmList = bcsClient.createVM(
                    clusterId = envCreateInfo.bcsVmParam!!.clusterId,
                    namespace = projectId,
                    instanceCount = envCreateInfo.bcsVmParam!!.instanceCount,
                    image = vmCreateInfoP.first,
                    resCpu = vmCreateInfoP.second,
                    resMemory = vmCreateInfoP.third
                )
                val nodeList = bcsVmList.map {
                    TNodeRecord(
                        null,
                        "",
                        projectId,
                        it.ip,
                        it.name,
                        it.status,
                        NodeType.BCSVM.name,
                        it.clusterId,
                        projectId,
                        userId,
                        now,
                        now.plusDays(envCreateInfo.bcsVmParam!!.validity.toLong()),
                        it.osName,
                        null,
                        null,
                        false,
                        "",
                        "",
                        null,
                        now,
                        userId
                    )
                }

                var envId = 0L
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)

                    nodeDao.batchAddNode(context, nodeList)
                    val insertedNodeList = nodeDao.listServerNodesByIps(context, projectId, bcsVmList.map { it.ip })
                    insertedNodeList.forEach {
                        environmentPermissionService.createNode(
                            userId = userId,
                            projectId = projectId,
                            nodeId = it.nodeId,
                            nodeName = "${NodeStringIdUtils.getNodeStringId(it)}(${it.nodeIp})"
                        )
                    }

                    envId = envDao.create(
                        context, userId, projectId, envCreateInfo.name, envCreateInfo.desc,
                        envCreateInfo.envType.name, ObjectMapper().writeValueAsString(envCreateInfo.envVars)
                    )
                    envNodeDao.batchStoreEnvNode(
                        context,
                        insertedNodeList.map { TEnvNodeRecord(envId, it.nodeId, projectId) })

                    environmentPermissionService.createEnv(
                        userId = userId,
                        projectId = projectId,
                        envId = envId,
                        envName = envCreateInfo.name
                    )
                }

                return EnvironmentId(HashUtil.encodeLongId(envId))
            }
            else -> {
                throw IllegalArgumentException("unsupported nodeSourceType")
            }
        }
    }

    fun updateEnvironment(userId: String, projectId: String, envHashId: String, envUpdateInfo: EnvUpdateInfo) {
        val envId = HashUtil.decodeIdToLong(envHashId)
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, BkAuthPermission.EDIT)) {
            throw OperationException("No Permission")
        }
        checkName(projectId, envId, envUpdateInfo.name)

        val existEnv = envDao.get(dslContext, projectId, envId)
        if (existEnv.envType == EnvType.BUILD.name && envUpdateInfo.envType != EnvType.BUILD) {
            throw OperationException("构建环境不能修改为部署环境")
        }
        if (existEnv.envType != EnvType.BUILD.name && envUpdateInfo.envType == EnvType.BUILD) {
            throw OperationException("构建环境不能修改为构建环境")
        }

        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)

            envDao.update(
                dslContext = context,
                envId = HashUtil.decodeIdToLong(envHashId),
                name = envUpdateInfo.name,
                desc = envUpdateInfo.desc,
                envType = envUpdateInfo.envType.name,
                envVars = ObjectMapper().writeValueAsString(envUpdateInfo.envVars)
            )

            if (existEnv.envName != envUpdateInfo.name) {
                environmentPermissionService.updateEnv(
                    userId = userId,
                    projectId = projectId,
                    envId = envId,
                    envName = envUpdateInfo.name
                )
            }
        }
    }

    fun listEnvironment(userId: String, projectId: String): List<EnvWithPermission> {
        val envRecordList = envDao.list(dslContext, projectId)
        if (envRecordList.isEmpty()) {
            return listOf()
        }

        val permissionMap = environmentPermissionService.listEnvByPermissions(
            userId,
            projectId,
            setOf(BkAuthPermission.LIST, BkAuthPermission.EDIT, BkAuthPermission.DELETE)
        )
        val canListEnvIds = if (permissionMap.containsKey(BkAuthPermission.LIST)) {
            permissionMap[BkAuthPermission.LIST]?.map { HashUtil.decodeIdToLong(it) } ?: emptyList()
        } else {
            emptyList()
        }

        val canEditEnvIds = if (permissionMap.containsKey(BkAuthPermission.EDIT)) {
            permissionMap[BkAuthPermission.EDIT]?.map { HashUtil.decodeIdToLong(it) } ?: emptyList()
        } else {
            emptyList()
        }

        val canDeleteEnvIds = if (permissionMap.containsKey(BkAuthPermission.DELETE)) {
            permissionMap[BkAuthPermission.DELETE]?.map { HashUtil.decodeIdToLong(it) } ?: emptyList()
        } else {
            emptyList()
        }

        val validRecordList = envRecordList.filter { canListEnvIds.contains(it.envId) }
        if (validRecordList.isEmpty()) {
            return listOf()
        }

        val nodeCountMap = envNodeDao.batchCount(dslContext, projectId, envRecordList.map { it.envId })
            .associateBy({ it.value1() }, { it.value2() })
        return envRecordList.map {
            EnvWithPermission(
                envHashId = HashUtil.encodeLongId(it.envId),
                name = it.envName,
                desc = it.envDesc,
                envType = if (it.envType == EnvType.TEST.name) EnvType.DEV.name else it.envType, // 兼容性代码
                nodeCount = nodeCountMap[it.envId] ?: 0,
                envVars = jacksonObjectMapper().readValue(it.envVars),
                createdUser = it.createdUser,
                createdTime = it.createdTime.timestamp(),
                updatedUser = it.updatedUser,
                updatedTime = it.updatedTime.timestamp(),
                canEdit = canEditEnvIds.contains(it.envId),
                canDelete = canDeleteEnvIds.contains(it.envId),
                canUse = null
            )
        }
    }

    fun listUsableServerEnvs(userId: String, projectId: String): List<EnvWithPermission> {
        val envRecordList = envDao.listServerEnv(dslContext, projectId)
        if (envRecordList.isEmpty()) {
            return listOf()
        }

        val canUseEnvIds = environmentPermissionService.listEnvByPermission(userId, projectId, BkAuthPermission.USE)

        val validRecordList = envRecordList.filter { canUseEnvIds.contains(it.envId) }
        if (validRecordList.isEmpty()) {
            return listOf()
        }

        val nodeCountMap = envNodeDao.batchCount(dslContext, projectId, validRecordList.map { it.envId })
            .associateBy({ it.value1() }, { it.value2() })
        return validRecordList.map {
            EnvWithPermission(
                envHashId = HashUtil.encodeLongId(it.envId),
                name = it.envName,
                desc = it.envDesc,
                envType = if (it.envType == EnvType.TEST.name) EnvType.DEV.name else it.envType, // 兼容性代码
                nodeCount = nodeCountMap[it.envId] ?: 0,
                envVars = jacksonObjectMapper().readValue(it.envVars),
                createdUser = it.createdUser,
                createdTime = it.createdTime.timestamp(),
                updatedUser = it.updatedUser,
                updatedTime = it.updatedTime.timestamp(),
                canEdit = null,
                canDelete = null,
                canUse = null
            )
        }
    }

    fun listEnvironmentByType(userId: String, projectId: String, envType: EnvType): List<EnvWithNodeCount> {
        val envRecordList = envDao.listByType(dslContext, projectId, envType)
        if (envRecordList.isEmpty()) {
            return emptyList()
        }

        val canListEnvIds = environmentPermissionService.listEnvByPermission(userId, projectId, BkAuthPermission.LIST)
        val validRecordList = envRecordList.filter { canListEnvIds.contains(it.envId) }
        if (validRecordList.isEmpty()) {
            return emptyList()
        }

        return validRecordList.map {
            val nodeIds = envNodeDao.list(dslContext, projectId, listOf(it.envId)).map { node ->
                node.nodeId
            }.toSet()

            val normalNodeCount = if (nodeIds.isEmpty()) {
                0
            } else {
                nodeDao.countNodeByStatus(dslContext, projectId, nodeIds, NodeStatus.NORMAL)
            }

            val abnormalNodeCount = if (nodeIds.isEmpty()) {
                0
            } else {
                nodeIds.size - normalNodeCount
            }

            EnvWithNodeCount(
                envHashId = HashUtil.encodeLongId(it.envId),
                name = it.envName,
                normalNodeCount = normalNodeCount,
                abnormalNodeCount = abnormalNodeCount
            )
        }
    }

    fun listBuildEnvs(userId: String, projectId: String, os: OS): List<EnvWithNodeCount> {
        val envRecordList = envDao.listByType(dslContext, projectId, EnvType.BUILD)
        if (envRecordList.isEmpty()) {
            return emptyList()
        }

        val canListEnvIds = environmentPermissionService.listEnvByPermission(userId, projectId, BkAuthPermission.LIST)
        val validRecordList = envRecordList.filter { canListEnvIds.contains(it.envId) }
        if (validRecordList.isEmpty()) {
            return emptyList()
        }

        return validRecordList.map {
            val nodeIds = envNodeDao.list(dslContext, projectId, listOf(it.envId)).map { node ->
                node.nodeId
            }.toSet()

            val normalNodeCount = if (nodeIds.isEmpty()) {
                0
            } else {
                thirdPartyAgentDao.countAgentByStatusAndOS(dslContext, projectId, nodeIds, AgentStatus.IMPORT_OK, os)
            }

            val abnormalNodeCount = if (nodeIds.isEmpty()) {
                0
            } else {
                thirdPartyAgentDao.countAgentByStatusAndOS(
                    dslContext = dslContext,
                    projectId = projectId,
                    nodeIds = nodeIds,
                    status = AgentStatus.IMPORT_EXCEPTION,
                    os = os
                )
            }

            EnvWithNodeCount(
                envHashId = HashUtil.encodeLongId(it.envId),
                name = it.envName,
                normalNodeCount = normalNodeCount,
                abnormalNodeCount = abnormalNodeCount
            )
        }
    }

    fun getEnvironment(userId: String, projectId: String, envHashId: String): EnvWithPermission {
        val envId = HashUtil.decodeIdToLong(envHashId)
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, BkAuthPermission.VIEW)) {
            throw OperationException("No Permission")
        }
        val env = envDao.get(dslContext, projectId, envId)
        val nodeCount = envNodeDao.count(dslContext, projectId, envId)
        return EnvWithPermission(
            envHashId = HashUtil.encodeLongId(env.envId),
            name = env.envName,
            desc = env.envDesc,
            envType = if (env.envType == EnvType.TEST.name) EnvType.DEV.name else env.envType, // 兼容性代码
            nodeCount = nodeCount,
            envVars = jacksonObjectMapper().readValue(env.envVars),
            createdUser = env.createdUser,
            createdTime = env.createdTime.timestamp(),
            updatedUser = env.updatedUser,
            updatedTime = env.updatedTime.timestamp(),
            canEdit = environmentPermissionService.checkEnvPermission(userId, projectId, envId, BkAuthPermission.EDIT),
            canDelete = environmentPermissionService.checkEnvPermission(
                userId,
                projectId,
                envId,
                BkAuthPermission.DELETE
            ),
            canUse = null
        )
    }

    fun listRawEnvByHashIds(userId: String, projectId: String, envHashIds: List<String>): List<EnvWithPermission> {
        val envRecords =
            envDao.listServerEnvByIds(dslContext, projectId, envHashIds.map { HashUtil.decodeIdToLong(it) })
        return envRecords.map {
            EnvWithPermission(
                envHashId = HashUtil.encodeLongId(it.envId),
                name = it.envName,
                desc = it.envDesc,
                envType = if (it.envType == EnvType.TEST.name) EnvType.DEV.name else it.envType, // 兼容性代码
                nodeCount = null,
                envVars = jacksonObjectMapper().readValue(it.envVars),
                createdUser = it.createdUser,
                createdTime = it.createdTime.timestamp(),
                updatedUser = it.updatedUser,
                updatedTime = it.updatedTime.timestamp(),
                canEdit = null,
                canDelete = null,
                canUse = null
            )
        }
    }

    fun listRawEnvByEnvNames(userId: String, projectId: String, envNames: List<String>): List<EnvWithPermission> {
        val envRecords = envDao.listServerEnvByEnvNames(dslContext, projectId, envNames)
        val canUseEnvIds = environmentPermissionService.listEnvByPermission(userId, projectId, BkAuthPermission.USE)

        return envRecords.map {
            EnvWithPermission(
                envHashId = HashUtil.encodeLongId(it.envId),
                name = it.envName,
                desc = it.envDesc,
                envType = if (it.envType == EnvType.TEST.name) EnvType.DEV.name else it.envType, // 兼容性代码
                nodeCount = null,
                envVars = jacksonObjectMapper().readValue(it.envVars),
                createdUser = it.createdUser,
                createdTime = it.createdTime.timestamp(),
                updatedUser = it.updatedUser,
                updatedTime = it.updatedTime.timestamp(),
                canEdit = null,
                canDelete = null,
                canUse = canUseEnvIds.contains(it.envId)
            )
        }
    }

    fun deleteEnvironment(userId: String, projectId: String, envHashId: String) {
        val envId = HashUtil.decodeIdToLong(envHashId)
        envDao.getOrNull(dslContext, projectId, envId) ?: return
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, BkAuthPermission.DELETE)) {
            throw OperationException("No Permission")
        }

        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            envDao.deleteEnv(context, envId)
            environmentPermissionService.deleteEnv(projectId, envId)
        }
    }

    fun listRawServerNodeByEnvHashIds(
        userId: String,
        projectId: String,
        envHashIds: List<String>
    ): Map<String, List<NodeBaseInfo>> {
        val envIds = envHashIds.map { HashUtil.decodeIdToLong(it) }
        val envNodes = envNodeDao.list(dslContext, projectId, envIds)
        val nodeRecords = nodeDao.listServerNodesByIds(dslContext, projectId, envNodes.map { it.nodeId })
        val nodeBaseInfos = nodeRecords.map {
            val nodeStringId = NodeStringIdUtils.getNodeStringId(it)
            NodeStringIdUtils.getRefineDisplayName(nodeStringId, it.displayName)
            NodeBaseInfo(
                nodeHashId = HashUtil.encodeLongId(it.nodeId),
                nodeId = nodeStringId,
                name = it.nodeName,
                ip = it.nodeIp,
                nodeStatus = it.nodeStatus,
                agentStatus = getAgentStatus(it),
                nodeType = it.nodeType,
                osName = it.osName,
                createdUser = it.createdUser,
                operator = it.operator,
                bakOperator = it.bakOperator,
                gateway = "",
                displayName = NodeStringIdUtils.getRefineDisplayName(nodeStringId, it.displayName)
            )
        }
        val hashIdToNodeBaseInfoMap = nodeBaseInfos.associateBy { it.nodeHashId }

        val resultMap = mutableMapOf<String, MutableList<NodeBaseInfo>>()
        envHashIds.forEach {
            resultMap[it] = mutableListOf()
        }
        envNodes.forEach {
            val envHashId = HashUtil.encodeLongId(it.envId)
            val nodeHashId = HashUtil.encodeLongId(it.nodeId)
            if (hashIdToNodeBaseInfoMap.containsKey(nodeHashId)) {
                resultMap[envHashId]!!.add(hashIdToNodeBaseInfoMap[nodeHashId]!!)
            }
        }
        return resultMap
    }

    fun listAllEnvNodes(userId: String, projectId: String, envHashIds: List<String>): List<NodeBaseInfo> {
        val envIds = envHashIds.map { HashUtil.decodeIdToLong(it) }
        val canUseEnvIdList = environmentPermissionService.listEnvByPermission(userId, projectId, BkAuthPermission.USE)
        val invalidEnvIds = envIds.filterNot { canUseEnvIdList.contains(it) }
        if (invalidEnvIds.isNotEmpty()) {
            throw OperationException("节点权限不足：节点ID[${invalidEnvIds.joinToString(",")}]")
        }

        val envNodeRecordList = envNodeDao.list(dslContext, projectId, envIds)
        val nodeIds = envNodeRecordList.map { it.nodeId }.toSet()
        val nodeList = nodeDao.listByIds(dslContext, projectId, nodeIds)

        val thirdPartyAgentMap =
            thirdPartyAgentDao.getAgentsByNodeIds(dslContext, nodeIds, projectId).associateBy { it.nodeId }
        logger.info("listAllEnvNodes ${LocalDateTime.now()}")
        BcsVmNodeStatusUtils.updateBcsVmNodeStatus(dslContext, nodeDao, bcsClient, nodeList)
        logger.info("listAllEnvNodes ${LocalDateTime.now()}")
        return nodeList.map {
            val thirdPartyAgent = thirdPartyAgentMap[it.nodeId]
            val gatewayShowName = if (thirdPartyAgent != null) {
                slaveGatewayService.getShowName(thirdPartyAgent.gateway)
            } else {
                ""
            }

            val nodeStringId = NodeStringIdUtils.getNodeStringId(it)
            NodeBaseInfo(
                nodeHashId = HashUtil.encodeLongId(it.nodeId),
                nodeId = nodeStringId,
                name = it.nodeName,
                ip = it.nodeIp,
                nodeStatus = NodeStatus.getStatusName(it.nodeStatus),
                agentStatus = getAgentStatus(it),
                nodeType = NodeType.getTypeName(it.nodeType),
                osName = it.osName,
                createdUser = it.createdUser,
                operator = it.operator,
                bakOperator = it.bakOperator,
                gateway = gatewayShowName,
                displayName = NodeStringIdUtils.getRefineDisplayName(nodeStringId, it.displayName)
            )
        }
    }

    fun addEnvNodes(userId: String, projectId: String, envHashId: String, nodeHashIds: List<String>) {
        val envId = HashUtil.decodeIdToLong(envHashId)
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, BkAuthPermission.EDIT)) {
            throw OperationException("No EDIT Permission")
        }

        val nodeLongIds = nodeHashIds.map { HashUtil.decodeIdToLong(it) }

        // 检查 node 权限
        val canUseNodeIds = environmentPermissionService.listNodeByPermission(userId, projectId, BkAuthPermission.USE)
        val unauthorizedNodeIds = nodeLongIds.filterNot { canUseNodeIds.contains(it) }
        if (unauthorizedNodeIds.isNotEmpty()) {
            throw OperationException("节点权限不足：[${unauthorizedNodeIds.joinToString(",") { HashUtil.encodeLongId(it) }}]")
        }

        val env = envDao.get(dslContext, projectId, envId)

        // 检查 node 是否存在
        val existNodes = nodeDao.listByIds(dslContext, projectId, nodeLongIds)
        val existNodeIds = existNodes.map { it.nodeId }.toSet()
        val notExistNodeIds = nodeLongIds.filterNot { existNodeIds.contains(it) }
        if (notExistNodeIds.isNotEmpty()) {
            throw OperationException("节点：[${notExistNodeIds.joinToString(",") { HashUtil.encodeLongId(it) }}]不存在")
        }

        // 过滤已在环境中的节点
        val existEnvNodeIds = envNodeDao.list(dslContext, projectId, listOf(envId)).map { it.nodeId }
        val toAddNodeIds = nodeLongIds.subtract(existEnvNodeIds)

        // 验证节点类型
        val existNodesMap = existNodes.associateBy { it.nodeId }
        val serverNodeTypes = listOf(NodeType.CMDB.name, NodeType.CC.name, NodeType.BCSVM.name)

        toAddNodeIds.forEach {
            if (env.envType == EnvType.BUILD.name && existNodesMap[it]?.nodeType in serverNodeTypes) {
                throw OperationException("服务器节点[${HashUtil.encodeLongId(it)}]不能添加到构建环境")
            }
            if (env.envType != EnvType.BUILD.name && existNodesMap[it]?.nodeType !in serverNodeTypes) {
                throw OperationException("构建节点[${HashUtil.encodeLongId(it)}]不能添加到非构建环境")
            }
        }

        val envNodeList = toAddNodeIds.map { TEnvNodeRecord(envId, it, projectId) }
        envNodeDao.batchStoreEnvNode(dslContext, envNodeList)
    }

    fun deleteEnvNodes(userId: String, projectId: String, envHashId: String, nodeHashIds: List<String>) {
        val envId = HashUtil.decodeIdToLong(envHashId)
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, BkAuthPermission.EDIT)) {
            throw OperationException("No Permission")
        }

        envNodeDao.batchDeleteEnvNode(
            dslContext = dslContext,
            projectId = projectId,
            envId = HashUtil.decodeIdToLong(envHashId),
            nodeIds = nodeHashIds.map { HashUtil.decodeIdToLong(it) })
    }
}