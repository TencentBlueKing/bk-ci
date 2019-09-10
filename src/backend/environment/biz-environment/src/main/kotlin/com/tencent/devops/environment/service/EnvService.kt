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
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentDao
import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.EnvUpdateInfo
import com.tencent.devops.environment.pojo.EnvVar
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
import com.tencent.devops.environment.utils.NodeStringIdUtils
import com.tencent.devops.model.environment.tables.records.TEnvNodeRecord
import com.tencent.devops.environment.permission.EnvironmentPermissionService
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
        when (envCreateInfo.source) {
            NodeSource.EXISTING -> {
                val nodeLongIds = envCreateInfo.nodeHashIds!!.map { HashUtil.decodeIdToLong(it) }

                // 检查 node 权限
                val canUseNodeIds = environmentPermissionService.listNodeByPermission(userId, projectId, BkAuthPermission.USE)
                val unauthorizedNodeIds = nodeLongIds.filterNot { canUseNodeIds.contains(it) }
                if (unauthorizedNodeIds.isNotEmpty()) {
                    throw OperationException("节点权限不足 [${unauthorizedNodeIds.map { HashUtil.encodeLongId(it) }.joinToString(",")}]")
                }

                // 检查 node 是否存在
                val existNodes = nodeDao.listByIds(dslContext, projectId, nodeLongIds)
                val existNodeIds = existNodes.map { it.nodeId }.toSet()
                val notExistNodeIds = nodeLongIds.filterNot { existNodeIds.contains(it) }
                if (notExistNodeIds.isNotEmpty()) {
                    throw OperationException("节点 [${notExistNodeIds.map { HashUtil.encodeLongId(it) }.joinToString(",")}] 不存在")
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
                context,
                HashUtil.decodeIdToLong(envHashId),
                envUpdateInfo.name,
                envUpdateInfo.desc,
                envUpdateInfo.envType.name,
                ObjectMapper().writeValueAsString(envUpdateInfo.envVars)
            )
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
            permissionMap[BkAuthPermission.LIST]!!.map { HashUtil.decodeIdToLong(it) }
        } else {
            emptyList()
        }

        val canEditEnvIds = if (permissionMap.containsKey(BkAuthPermission.EDIT)) {
            permissionMap[BkAuthPermission.EDIT]!!.map { HashUtil.decodeIdToLong(it) }
        } else {
            emptyList()
        }

        val canDeleteEnvIds = if (permissionMap.containsKey(BkAuthPermission.DELETE)) {
            permissionMap[BkAuthPermission.DELETE]!!.map { HashUtil.decodeIdToLong(it) }
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
                HashUtil.encodeLongId(it.envId),
                it.envName,
                it.envDesc,
                if (it.envType == EnvType.TEST.name) EnvType.DEV.name else it.envType, // 兼容性代码
                nodeCountMap[it.envId] ?: 0,
                jacksonObjectMapper().readValue(it.envVars),
                it.createdUser,
                it.createdTime.timestamp(),
                it.updatedUser,
                it.updatedTime.timestamp(),
                canEditEnvIds.contains(it.envId),
                canDeleteEnvIds.contains(it.envId),
                null
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

        return envRecordList.map {
            val nodeIds = envNodeDao.list(dslContext, projectId, listOf(it.envId)).map {
                it.nodeId
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
                HashUtil.encodeLongId(it.envId),
                it.envName,
                normalNodeCount,
                abnormalNodeCount
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
            val nodeIds = envNodeDao.list(dslContext, projectId, listOf(it.envId)).map {
                it.nodeId
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
                    dslContext,
                    projectId,
                    nodeIds,
                    AgentStatus.IMPORT_EXCEPTION,
                    os
                )
            }

            EnvWithNodeCount(
                HashUtil.encodeLongId(it.envId),
                it.envName,
                normalNodeCount,
                abnormalNodeCount
            )
        }
    }

    fun getEnvironment(userId: String, projectId: String, envHashId: String): EnvWithPermission {
        val envId = HashUtil.decodeIdToLong(envHashId)
        val env = envDao.get(dslContext, projectId, envId)
        val nodeCount = envNodeDao.count(dslContext, projectId, envId)
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, BkAuthPermission.VIEW)) {
            throw OperationException("No Permission")
        }
        return EnvWithPermission(
            HashUtil.encodeLongId(env.envId),
            env.envName,
            env.envDesc,
            if (env.envType == EnvType.TEST.name) EnvType.DEV.name else env.envType, // 兼容性代码
            nodeCount,
            jacksonObjectMapper().readValue(env.envVars),
            env.createdUser,
            env.createdTime.timestamp(),
            env.updatedUser,
            env.updatedTime.timestamp(),
            true,
            true,
            null
        )
    }

    fun listRawEnvByHashIds(userId: String, projectId: String, envHashIds: List<String>): List<EnvWithPermission> {
        val envRecords =
            envDao.listServerEnvByIds(dslContext, projectId, envHashIds.map { HashUtil.decodeIdToLong(it) })
        return envRecords.map {
            EnvWithPermission(
                HashUtil.encodeLongId(it.envId),
                it.envName,
                it.envDesc,
                if (it.envType == EnvType.TEST.name) EnvType.DEV.name else it.envType, // 兼容性代码
                null,
                jacksonObjectMapper().readValue(it.envVars),
                it.createdUser,
                it.createdTime.timestamp(),
                it.updatedUser,
                it.updatedTime.timestamp(),
                null,
                null,
                null
            )
        }
    }

    fun listRawEnvByEnvNames(userId: String, projectId: String, envNames: List<String>): List<EnvWithPermission> {
        val envRecords = envDao.listServerEnvByEnvNames(dslContext, projectId, envNames)

        return envRecords.map {
            EnvWithPermission(
                HashUtil.encodeLongId(it.envId),
                it.envName,
                it.envDesc,
                if (it.envType == EnvType.TEST.name) EnvType.DEV.name else it.envType, // 兼容性代码
                null,
                jacksonObjectMapper().readValue<List<EnvVar>>(it.envVars),
                it.createdUser,
                it.createdTime.timestamp(),
                it.updatedUser,
                it.updatedTime.timestamp(),
                null,
                null,
                true
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
        }
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
        return nodeList.map {
            val thirdPartyAgent = thirdPartyAgentMap[it.nodeId]
            val gatewayShowName = if (thirdPartyAgent != null) {
                slaveGatewayService.getShowName(thirdPartyAgent.gateway)
            } else {
                ""
            }

            val nodeStringId = NodeStringIdUtils.getNodeStringId(it)
            NodeBaseInfo(
                HashUtil.encodeLongId(it.nodeId),
                nodeStringId,
                it.nodeName,
                it.nodeIp,
                NodeStatus.getStatusName(it.nodeStatus),
                getAgentStatus(it),
                NodeType.getTypeName(it.nodeType),
                it.osName,
                it.createdUser,
                it.operator,
                it.bakOperator,
                gatewayShowName,
                NodeStringIdUtils.getRefineDisplayName(nodeStringId, it.displayName)
            )
        }
    }

    fun addEnvNodes(userId: String, projectId: String, envHashId: String, nodeHashIds: List<String>) {
        val envId = HashUtil.decodeIdToLong(envHashId)
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, BkAuthPermission.EDIT)) {
            throw OperationException("No Permission")
        }

        envDao.get(dslContext, projectId, envId)

        val nodeLongIds = nodeHashIds.map { HashUtil.decodeIdToLong(it) }

        // 检查 node 权限
        val canUseNodeIds = environmentPermissionService.listNodeByPermission(userId, projectId, BkAuthPermission.USE)
        val unauthorizedNodeIds = nodeLongIds.filterNot { canUseNodeIds.contains(it) }
        if (unauthorizedNodeIds.isNotEmpty()) {
            throw OperationException("节点权限不足：[${unauthorizedNodeIds.map { HashUtil.encodeLongId(it) }.joinToString(",")}]")
        }

        // 检查 node 是否存在
        val existNodes = nodeDao.listByIds(dslContext, projectId, nodeLongIds)
        val existNodeIds = existNodes.map { it.nodeId }.toSet()
        val notExistNodeIds = nodeLongIds.filterNot { existNodeIds.contains(it) }
        if (notExistNodeIds.isNotEmpty()) {
            throw OperationException("节点：[${notExistNodeIds.map { HashUtil.encodeLongId(it) }.joinToString(",")}]不存在")
        }

        // 过滤已在环境中的节点
        val existEnvNodeIds = envNodeDao.list(dslContext, projectId, listOf(envId)).map { it.nodeId }
        val toAddNodeIds = nodeLongIds.subtract(existEnvNodeIds)

        val envNodeList = toAddNodeIds.map { TEnvNodeRecord(envId, it, projectId) }
        envNodeDao.batchStoreEnvNode(dslContext, envNodeList)
    }

    fun deleteEnvNodes(userId: String, projectId: String, envHashId: String, nodeHashIds: List<String>) {
        val envId = HashUtil.decodeIdToLong(envHashId)
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, BkAuthPermission.EDIT)) {
            throw OperationException("No Permission")
        }

        envNodeDao.batchDeleteEnvNode(
            dslContext,
            projectId,
            HashUtil.decodeIdToLong(envHashId),
            nodeHashIds.map { HashUtil.decodeIdToLong(it) })
    }
}