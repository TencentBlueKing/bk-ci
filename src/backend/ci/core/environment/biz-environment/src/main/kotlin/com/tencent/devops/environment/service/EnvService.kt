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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_ENV_BUILD_2_DEPLOY_DENY
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_ENV_BUILD_CAN_NOT_ADD_SVR
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_ENV_DEPLOY_2_BUILD_DENY
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_ENV_DEPLOY_CAN_NOT_ADD_AGENT
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_ENV_NO_CREATE_PERMISSSION
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_ENV_NO_DEL_PERMISSSION
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_ENV_NO_EDIT_PERMISSSION
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_ENV_NO_VIEW_PERMISSSION
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_INSUFFICIENT_PERMISSIONS
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NAME_DUPLICATE
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NAME_INVALID_CHARACTER
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NO_USE_PERMISSSION
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_SHARE_PROJECT_TYPE_ERROR
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_QUOTA_LIMIT
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.EnvShareProjectDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.AddSharedProjectInfo
import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.EnvUpdateInfo
import com.tencent.devops.environment.pojo.EnvWithNode
import com.tencent.devops.environment.pojo.EnvWithNodeCount
import com.tencent.devops.environment.pojo.EnvWithPermission
import com.tencent.devops.environment.pojo.EnvironmentId
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.environment.pojo.SharedProjectInfo
import com.tencent.devops.environment.pojo.enums.EnvType
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.pojo.enums.SharedEnvType
import com.tencent.devops.environment.service.node.EnvCreatorFactory
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import com.tencent.devops.environment.utils.AgentStatusUtils.getAgentStatus
import com.tencent.devops.environment.utils.NodeStringIdUtils
import com.tencent.devops.model.environment.tables.records.TEnvRecord
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat

@Service
@Suppress("ALL")
class EnvService @Autowired constructor(
    private val dslContext: DSLContext,
    private val envDao: EnvDao,
    private val nodeDao: NodeDao,
    private val envNodeDao: EnvNodeDao,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val slaveGatewayService: SlaveGatewayService,
    private val environmentPermissionService: EnvironmentPermissionService,
    private val envShareProjectDao: EnvShareProjectDao,
    private val nodeService: NodeService,
    private val client: Client
) : IEnvService {

    override fun checkName(projectId: String, envId: Long?, envName: String) {
        if (envName.contains("@")) {
            throw ErrorCodeException(errorCode = ERROR_NODE_NAME_INVALID_CHARACTER, params = arrayOf(envName))
        }
        if (envDao.isNameExist(dslContext, projectId, envId, envName)) {
            throw ErrorCodeException(errorCode = ERROR_NODE_NAME_DUPLICATE, params = arrayOf(envName))
        }
    }

    override fun createEnvironment(userId: String, projectId: String, envCreateInfo: EnvCreateInfo): EnvironmentId {
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, AuthPermission.CREATE)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_CREATE_PERMISSSION)
            )
        }

        checkName(projectId, null, envCreateInfo.name)

        val envCreator = EnvCreatorFactory.load(envCreateInfo.source.name)
            ?: throw IllegalArgumentException("unsupported nodeSourceType ${envCreateInfo.source}")

        return envCreator.createEnv(projectId = projectId, userId = userId, envCreateInfo = envCreateInfo)
    }

    override fun updateEnvironment(userId: String, projectId: String, envHashId: String, envUpdateInfo: EnvUpdateInfo) {
        val envId = HashUtil.decodeIdToLong(envHashId)
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, AuthPermission.EDIT)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_EDIT_PERMISSSION)
            )
        }
        checkName(projectId, envId, envUpdateInfo.name)

        val existEnv = envDao.get(dslContext, projectId, envId)
        if (existEnv.envType == EnvType.BUILD.name && envUpdateInfo.envType != EnvType.BUILD) {
            throw ErrorCodeException(errorCode = ERROR_ENV_BUILD_2_DEPLOY_DENY)
        }
        if (existEnv.envType != EnvType.BUILD.name && envUpdateInfo.envType == EnvType.BUILD) {
            throw ErrorCodeException(errorCode = ERROR_ENV_DEPLOY_2_BUILD_DENY)
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

    override fun listEnvironment(userId: String, projectId: String): List<EnvWithPermission> {
        val envRecordList = envDao.list(dslContext, projectId)
        if (envRecordList.isEmpty()) {
            return listOf()
        }

        val permissionMap = environmentPermissionService.listEnvByPermissions(
            userId,
            projectId,
            setOf(AuthPermission.LIST, AuthPermission.EDIT, AuthPermission.DELETE, AuthPermission.USE)
        )
        val canListEnvIds = if (permissionMap.containsKey(AuthPermission.LIST)) {
            permissionMap[AuthPermission.LIST]?.map { HashUtil.decodeIdToLong(it) } ?: emptyList()
        } else {
            emptyList()
        }

        val canEditEnvIds = if (permissionMap.containsKey(AuthPermission.EDIT)) {
            permissionMap[AuthPermission.EDIT]?.map { HashUtil.decodeIdToLong(it) } ?: emptyList()
        } else {
            emptyList()
        }

        val canDeleteEnvIds = if (permissionMap.containsKey(AuthPermission.DELETE)) {
            permissionMap[AuthPermission.DELETE]?.map { HashUtil.decodeIdToLong(it) } ?: emptyList()
        } else {
            emptyList()
        }

        val canUseEnvIds = if (permissionMap.containsKey(AuthPermission.USE)) {
            permissionMap[AuthPermission.USE]?.map { HashUtil.decodeIdToLong(it) } ?: emptyList()
        } else {
            emptyList()
        }
        val canListEnv = envRecordList.filter { canListEnvIds.contains(it.envId) }
        // 用于兼容rbac和其他版本权限，rbac只会展示出用户有列表权限的环境。而其
        // 他权限版本，则只要用户具有某个环境的列表权限，就会把该项目下所有的环境都返回
        val envListResult = if (canListEnv.isEmpty()) {
            return listOf()
        } else {
            environmentPermissionService.getEnvListResult(
                canListEnv = canListEnv,
                envRecordList = envRecordList
            )
        }
        val nodeCountMap = envNodeDao.batchCount(dslContext, projectId, envRecordList.map { it.envId })
            .associateBy({ it.value1() }, { it.value2() })
        return envListResult.map {
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
                canUse = canUseEnvIds.contains(it.envId),
                projectName = null
            )
        }
    }

    override fun listUsableServerEnvs(userId: String, projectId: String): List<EnvWithPermission> {
        val envRecordList = envDao.listServerEnv(dslContext, projectId)
        if (envRecordList.isEmpty()) {
            return listOf()
        }

        val canUseEnvIds = environmentPermissionService.listEnvByPermission(userId, projectId, AuthPermission.USE)

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
                canUse = null,
                projectName = null
            )
        }
    }

    override fun listEnvironmentByType(userId: String, projectId: String, envType: EnvType): List<EnvWithNodeCount> {
        val envRecordList = envDao.listByType(dslContext, projectId, envType)
        if (envRecordList.isEmpty()) {
            return emptyList()
        }

        val canListEnvIds = environmentPermissionService.listEnvByPermission(userId, projectId, AuthPermission.LIST)
        val validRecordList = envRecordList.filter { canListEnvIds.contains(it.envId) }.map {
            EnvWithNode(
                envId = it.envId, envName = it.envName, sharedProjectId = null, sharedUserId = null
            )
        }.plus(
            envShareProjectDao.listByShare(
                dslContext = dslContext,
                envName = null,
                sharedProjectId = projectId
            ).map {
                EnvWithNode(
                    envId = it.envId,
                    envName = it.envName,
                    sharedProjectId = it.mainProjectId,
                    sharedUserId = it.creator
                )
            }
        )
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
                abnormalNodeCount = abnormalNodeCount,
                sharedProjectId = it.sharedProjectId,
                sharedUserId = it.sharedUserId
            )
        }
    }

    override fun listBuildEnvs(userId: String, projectId: String, os: OS): List<EnvWithNodeCount> {
        val envRecordList = envDao.listByType(dslContext, projectId, EnvType.BUILD)

        val canListEnvIds = environmentPermissionService.listEnvByPermission(userId, projectId, AuthPermission.LIST)
        val validRecordList = envRecordList.filter { canListEnvIds.contains(it.envId) }.map {
            EnvWithNode(
                envId = it.envId, envName = it.envName, sharedProjectId = null, sharedUserId = null
            )
        }.plus(
            envShareProjectDao.listByShare(
                dslContext = dslContext,
                envName = null,
                sharedProjectId = projectId
            ).map {
                EnvWithNode(
                    envId = it.envId,
                    envName = it.envName,
                    sharedProjectId = it.mainProjectId,
                    sharedUserId = it.creator
                )
            }
        )

        if (validRecordList.isEmpty()) {
            return emptyList()
        }

        return validRecordList.map {
            val nodeProjectId = it.sharedProjectId ?: projectId
            val nodeIds = envNodeDao.list(
                dslContext = dslContext, projectId = nodeProjectId, envIds = listOf(it.envId)
            ).map { node ->
                node.nodeId
            }.toSet()

            val normalNodeCount = if (nodeIds.isEmpty()) {
                0
            } else {
                thirdPartyAgentDao.countAgentByStatusAndOS(
                    dslContext,
                    nodeProjectId,
                    nodeIds,
                    AgentStatus.IMPORT_OK,
                    os
                )
            }

            val abnormalNodeCount = if (nodeIds.isEmpty()) {
                0
            } else {
                thirdPartyAgentDao.countAgentByStatusAndOS(
                    dslContext = dslContext,
                    projectId = nodeProjectId,
                    nodeIds = nodeIds,
                    status = AgentStatus.IMPORT_EXCEPTION,
                    os = os
                )
            }

            EnvWithNodeCount(
                envHashId = HashUtil.encodeLongId(it.envId),
                name = it.envName,
                normalNodeCount = normalNodeCount,
                abnormalNodeCount = abnormalNodeCount,
                sharedProjectId = it.sharedProjectId,
                sharedUserId = it.sharedUserId
            )
        }
    }

    override fun getEnvironment(userId: String, projectId: String, envHashId: String): EnvWithPermission {
        val envId = HashUtil.decodeIdToLong(envHashId)
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, AuthPermission.VIEW)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_VIEW_PERMISSSION)
            )
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
            canEdit = environmentPermissionService.checkEnvPermission(userId, projectId, envId, AuthPermission.EDIT),
            canDelete = environmentPermissionService.checkEnvPermission(
                userId,
                projectId,
                envId,
                AuthPermission.DELETE
            ),
            canUse = null,
            projectName = client.get(ServiceProjectResource::class).get(env.projectId).data?.projectName
        )
    }

    override fun listRawEnvByHashIds(
        userId: String,
        projectId: String,
        envHashIds: List<String>
    ): List<EnvWithPermission> {
        val envRecords =
            envDao.listServerEnvByIds(dslContext, projectId, envHashIds.map { HashUtil.decodeIdToLong(it) })
        return format(envRecords)
    }

    override fun listRawEnvByHashIdsAllType(envHashIds: List<String>): List<EnvWithPermission> {
        val envRecords =
            envDao.listServerEnvByIdsAllType(dslContext, envHashIds.map { HashUtil.decodeIdToLong(it) })
        return format(envRecords)
    }

    override fun listRawEnvByEnvNames(
        userId: String,
        projectId: String,
        envNames: List<String>
    ): List<EnvWithPermission> {
        val envRecords = envDao.listServerEnvByEnvNames(dslContext, projectId, envNames)
        val canUseEnvIds = environmentPermissionService.listEnvByPermission(userId, projectId, AuthPermission.USE)

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
                canUse = canUseEnvIds.contains(it.envId),
                projectName = null
            )
        }
    }

    override fun deleteEnvironment(userId: String, projectId: String, envHashId: String) {
        val envId = HashUtil.decodeIdToLong(envHashId)
        envDao.getOrNull(dslContext, projectId, envId) ?: return
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, AuthPermission.DELETE)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_DEL_PERMISSSION)
            )
        }

        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            envDao.deleteEnv(context, envId)
            environmentPermissionService.deleteEnv(projectId, envId)
            // 删除环境时需要同时删除 EnvNode和EnvShareProject相关数据
            envNodeDao.deleteByEnvId(context, envId)
            envShareProjectDao.deleteByEnvId(context, envId)
        }
    }

    override fun listRawServerNodeByEnvHashIds(
        userId: String,
        projectId: String,
        envHashIds: List<String>
    ): Map<String, List<NodeBaseInfo>> {
        val envIds = envHashIds.map { HashUtil.decodeIdToLong(it) }
        val envNodes = envNodeDao.list(dslContext, projectId, envIds)
        val nodeRecords = nodeDao.listServerNodesByIds(dslContext, projectId, envNodes.map { it.nodeId })
        val nodeBaseInfos = nodeRecords.map { NodeStringIdUtils.getNodeBaseInfo(it) }
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

    fun thirdPartyEnv2Nodes(
        userId: String,
        projectId: String,
        envHashId: String
    ): List<NodeWithPermission> {
        val envId = HashUtil.decodeIdToLong(envHashId)
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, AuthPermission.VIEW)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_VIEW_PERMISSSION)
            )
        }
        val envNodes = envNodeDao.list(dslContext, projectId, listOf(envId))
        val nodes = nodeDao.listThirdpartyNodes(dslContext, projectId, envNodes.map { it.nodeId })
        return nodeService.formatNodeWithPermissions(userId, projectId, nodes)
    }

    override fun listAllEnvNodes(userId: String, projectId: String, envHashIds: List<String>): List<NodeBaseInfo> {
        val envIds = envHashIds.map { HashUtil.decodeIdToLong(it) }
        val canViewEnvIdList = environmentPermissionService.listEnvByViewPermission(userId, projectId)
        val invalidEnvIds = envIds.filterNot { canViewEnvIdList.contains(it) }
        if (invalidEnvIds.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ERROR_NODE_INSUFFICIENT_PERMISSIONS,
                params = arrayOf(invalidEnvIds.joinToString(","))
            )
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
            val nodeType = I18nUtil.getCodeLanMessage("ENV_NODE_TYPE_${it.nodeType}")
            val nodeStatus = I18nUtil.getCodeLanMessage("envNodeStatus.${it.nodeStatus}")
            val nodeStringId = NodeStringIdUtils.getNodeStringId(it)
            NodeBaseInfo(
                nodeHashId = HashUtil.encodeLongId(it.nodeId),
                nodeId = nodeStringId,
                name = it.nodeName,
                ip = it.nodeIp,
                nodeStatus = nodeStatus,
                agentStatus = getAgentStatus(it),
                nodeType = nodeType,
                osName = it.osName,
                createdUser = it.createdUser,
                operator = it.operator,
                bakOperator = it.bakOperator,
                gateway = gatewayShowName,
                displayName = NodeStringIdUtils.getRefineDisplayName(nodeStringId, it.displayName)
            )
        }
    }

    override fun addEnvNodes(userId: String, projectId: String, envHashId: String, nodeHashIds: List<String>) {
        val envId = HashUtil.decodeIdToLong(envHashId)
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, AuthPermission.EDIT)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_EDIT_PERMISSSION)
            )
        }

        val nodeLongIds = nodeHashIds.map { HashUtil.decodeIdToLong(it) }

        // 检查 node 权限
        val canUseNodeIds = environmentPermissionService.listNodeByPermission(userId, projectId, AuthPermission.USE)
        val unauthorizedNodeIds = nodeLongIds.filterNot { canUseNodeIds.contains(it) }
        if (unauthorizedNodeIds.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ERROR_NODE_NO_USE_PERMISSSION,
                params = arrayOf(unauthorizedNodeIds.joinToString(",") { HashUtil.encodeLongId(it) })
            )
        }

        val env = envDao.get(dslContext, projectId, envId)

        // 检查 node 是否存在
        val existNodes = nodeDao.listByIds(dslContext, projectId, nodeLongIds)
        val existNodeIds = existNodes.map { it.nodeId }.toSet()
        val notExistNodeIds = nodeLongIds.filterNot { existNodeIds.contains(it) }
        if (notExistNodeIds.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ERROR_NODE_NOT_EXISTS,
                params = arrayOf(notExistNodeIds.joinToString(",") { HashUtil.encodeLongId(it) })
            )
        }

        // 过滤已在环境中的节点
        val existEnvNodeIds = envNodeDao.list(dslContext, projectId, listOf(envId)).map { it.nodeId }
        val toAddNodeIds = nodeLongIds.subtract(existEnvNodeIds)

        // 验证节点类型
        val existNodesMap = existNodes.associateBy { it.nodeId }
        val serverNodeTypes = listOf(NodeType.CMDB.name)

        toAddNodeIds.forEach {
            if (env.envType == EnvType.BUILD.name && existNodesMap[it]?.nodeType in serverNodeTypes) {
                throw ErrorCodeException(
                    errorCode = ERROR_ENV_BUILD_CAN_NOT_ADD_SVR,
                    params = arrayOf(HashUtil.encodeLongId(it))
                )
            }
            if (env.envType != EnvType.BUILD.name && existNodesMap[it]?.nodeType !in serverNodeTypes) {
                throw ErrorCodeException(
                    errorCode = ERROR_ENV_DEPLOY_CAN_NOT_ADD_AGENT,
                    params = arrayOf(HashUtil.encodeLongId(it))
                )
            }
        }

        envNodeDao.batchStoreEnvNode(dslContext, toAddNodeIds.toList(), envId, projectId)
    }

    override fun deleteEnvNodes(userId: String, projectId: String, envHashId: String, nodeHashIds: List<String>) {
        val envId = HashUtil.decodeIdToLong(envHashId)
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, AuthPermission.EDIT)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_EDIT_PERMISSSION)
            )
        }

        envNodeDao.batchDeleteEnvNode(
            dslContext = dslContext,
            projectId = projectId,
            envId = HashUtil.decodeIdToLong(envHashId),
            nodeIds = nodeHashIds.map { HashUtil.decodeIdToLong(it) })
    }

    override fun listEnvironmentByLimit(projectId: String, offset: Int?, limit: Int?): Page<EnvWithPermission> {
        val envList = mutableListOf<EnvWithPermission>()
        val envRecords = envDao.listPage(dslContext, offset!!, limit!!, projectId)
        envRecords.map {
            envList.add(
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
                    canUse = null,
                    projectName = null
                )
            )
        }
        val count = envDao.countByProject(dslContext, projectId)
        return Page(
            count = count.toLong(),
            records = envList,
            pageSize = offset,
            page = limit
        )
    }

    override fun searchByName(projectId: String, envName: String, limit: Int, offset: Int): Page<EnvWithPermission> {
        val envList = mutableListOf<EnvWithPermission>()
        val envRecords = envDao.searchByName(
            dslContext = dslContext,
            offset = offset,
            limit = limit,
            projectId = projectId,
            envName = envName
        )
        envRecords.map {
            envList.add(
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
                    canUse = null,
                    projectName = null
                )
            )
        }
        val count = envDao.countByName(dslContext, projectId, envName)
        return Page(
            count = count.toLong(),
            records = envList,
            pageSize = offset,
            page = limit
        )
    }

    fun getByName(projectId: String, envName: String): EnvWithPermission? {
        return envDao.getByEnvName(dslContext, projectId, envName)?.let {
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
                canUse = null,
                projectName = null
            )
        }
    }

    private fun format(records: List<TEnvRecord>): List<EnvWithPermission> {
        return records.map {
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
                canUse = null,
                projectName = null
            )
        }
    }

    fun setShareEnv(userId: String, projectId: String, envHashId: String, sharedProjects: List<AddSharedProjectInfo>) {
        val envId = HashUtil.decodeIdToLong(envHashId)
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, AuthPermission.EDIT)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_EDIT_PERMISSSION)
            )
        }
        envShareProjectDao.count(dslContext = dslContext, projectId = projectId, envId = envId, name = null).let {
            if (it + sharedProjects.size > 500) {
                throw ErrorCodeException(errorCode = ERROR_QUOTA_LIMIT, params = arrayOf("500", it.toString()))
            }
        }

        val existEnv = envDao.get(dslContext, projectId, envId)
        if (existEnv.envType != EnvType.BUILD.name) {
            throw ErrorCodeException(errorCode = ERROR_NODE_SHARE_PROJECT_TYPE_ERROR)
        }
        envShareProjectDao.batchSave(
            dslContext = dslContext,
            userId = userId,
            envId = envId,
            envName = existEnv.envName,
            mainProjectId = projectId,
            sharedProjects = sharedProjects
        )
    }

    fun deleteShareEnv(userId: String, projectId: String, envHashId: String) {
        val envId = HashUtil.decodeIdToLong(envHashId)
        envDao.getOrNull(dslContext, projectId, envId) ?: return
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, AuthPermission.DELETE)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_DEL_PERMISSSION)
            )
        }

        envShareProjectDao.deleteByEnvAndMainProj(dslContext, envId, projectId)
    }

    fun listUserShareEnv(
        userId: String,
        projectId: String,
        envHashId: String,
        search: String?,
        page: Int = 1,
        pageSize: Int = 20
    ): Page<SharedProjectInfo> {
        val limitTmp = if (pageSize >= 1000) {
            1000
        } else {
            pageSize
        }
        val projectList = client.get(ServiceProjectResource::class).list(
            userId = userId
        ).data
            ?.filter { it.englishName != projectId && (search.isNullOrBlank() || it.projectName.contains(search)) }
            ?.map {
                SharedProjectInfo(
                    projectId = it.englishName,
                    gitProjectId = null,
                    name = it.projectName,
                    creator = it.creator,
                    type = SharedEnvType.PROJECT,
                    createTime = SimpleDateFormat("YY-MM-DD").parse(it.createdAt).time,
                    updateTime = SimpleDateFormat("YY-MM-DD").parse(
                        if (it.updatedAt.isNullOrBlank()) it.createdAt else it.updatedAt
                    ).time
                )
            }?.sortedBy { it.name } ?: emptyList()
        val envId = HashUtil.decodeIdToLong(envHashId)
        val count = envShareProjectDao.count(
            dslContext = dslContext,
            projectId = projectId,
            envId = envId,
            name = null
        )
        val sharedProjects = envShareProjectDao.listPage(
            dslContext = dslContext,
            projectId = projectId,
            envId = envId,
            name = null,
            offset = 0,
            limit = count + 1
        ).map { it.sharedProjectId }
        val records = projectList.filterNot { it.projectId in sharedProjects }
        val fromIndex = if ((page - 1) * limitTmp > records.size) records.size else (page - 1) * limitTmp
        val toIndex = if (page * limitTmp > records.size) records.size else page * limitTmp
        return Page(
            count = records.size.toLong(),
            records = records.subList(fromIndex, toIndex),
            pageSize = limitTmp,
            page = page
        )
    }

    fun listShareEnv(
        userId: String,
        projectId: String,
        envHashId: String,
        name: String?,
        page: Int = 1,
        pageSize: Int = 20
    ): Page<SharedProjectInfo> {
        val envId = HashUtil.decodeIdToLong(envHashId)
        val limitTmp = if (pageSize >= 1000) {
            1000
        } else {
            pageSize
        }
        val offset = limitTmp * (page - 1)
        val sharedProjectInfos = mutableListOf<SharedProjectInfo>()
        val records = envShareProjectDao.listPage(dslContext, projectId, envId, name, offset, limitTmp + 1)
        records.map {
            sharedProjectInfos.add(
                SharedProjectInfo(
                    projectId = it.mainProjectId,
                    gitProjectId = it.sharedProjectId,
                    name = it.sharedProjectName,
                    type = if (it.type == SharedEnvType.PROJECT.name) {
                        SharedEnvType.PROJECT
                    } else {
                        SharedEnvType.GROUP
                    },
                    creator = it.creator,
                    createTime = it.createTime.timestamp(),
                    updateTime = it.updateTime.timestamp()
                )
            )
        }
        val count = envShareProjectDao.count(dslContext, projectId, envId, name)
        return Page(
            count = count.toLong(),
            records = sharedProjectInfos,
            pageSize = limitTmp,
            page = page
        )
    }

    fun deleteShareEnvBySharedProj(userId: String, projectId: String, envHashId: String, sharedProjectId: String) {
        val envId = HashUtil.decodeIdToLong(envHashId)
        envDao.getOrNull(dslContext, projectId, envId) ?: return
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, AuthPermission.DELETE)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_DEL_PERMISSSION)
            )
        }

        envShareProjectDao.deleteBySharedProj(dslContext, envId, projectId, sharedProjectId)
    }
}
