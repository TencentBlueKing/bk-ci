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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.audit.ActionAuditContent.PROJECT_ENABLE_OR_DISABLE_TEMPLATE
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_ENV_BUILD_2_DEPLOY_DENY
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_ENV_BUILD_CAN_NOT_ADD_SVR
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_ENV_DEPLOY_2_BUILD_DENY
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_ENV_DEPLOY_CAN_NOT_ADD_AGENT
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_ENV_NOT_EXISTS
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
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_TAG_NO_EDIT_PERMISSSION
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_QUOTA_LIMIT
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.EnvShareProjectDao
import com.tencent.devops.environment.dao.EnvTagDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.NodeTagKeyDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.model.EnvNode
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.AddSharedProjectInfo
import com.tencent.devops.environment.pojo.EnvAddNodesData
import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.EnvUpdateInfo
import com.tencent.devops.environment.pojo.EnvVar
import com.tencent.devops.environment.pojo.EnvWithNode
import com.tencent.devops.environment.pojo.EnvWithNodeCount
import com.tencent.devops.environment.pojo.EnvWithPermission
import com.tencent.devops.environment.pojo.EnvironmentId
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.environment.pojo.SharedProjectInfo
import com.tencent.devops.environment.pojo.enums.AgentType
import com.tencent.devops.environment.pojo.enums.EnvNodeType
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
import jakarta.ws.rs.NotFoundException
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Suppress("ALL")
class EnvService @Autowired constructor(
    private val dslContext: DSLContext,
    private val envDao: EnvDao,
    private val nodeDao: NodeDao,
    private val envNodeDao: EnvNodeDao,
    private val envTagDao: EnvTagDao,
    private val nodeTagKeyDao: NodeTagKeyDao,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val slaveGatewayService: SlaveGatewayService,
    private val environmentPermissionService: EnvironmentPermissionService,
    private val envShareProjectDao: EnvShareProjectDao,
    private val nodeService: NodeService,
    private val client: Client,
    private val authProjectApi: AuthProjectApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode
) : IEnvService {

    override fun checkName(projectId: String, envId: Long?, envName: String) {
        if (envName.contains("@")) {
            throw ErrorCodeException(errorCode = ERROR_NODE_NAME_INVALID_CHARACTER, params = arrayOf(envName))
        }
        if (envDao.isNameExist(dslContext, projectId, envId, envName)) {
            throw ErrorCodeException(errorCode = ERROR_NODE_NAME_DUPLICATE, params = arrayOf(envName))
        }
    }

    @ActionAuditRecord(
        actionId = ActionId.ENVIRONMENT_CREATE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENVIRONMENT
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENVIRONMENT_CREATE_CONTENT
    )
    override fun createEnvironment(userId: String, projectId: String, envCreateInfo: EnvCreateInfo): EnvironmentId {
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, AuthPermission.CREATE)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_CREATE_PERMISSSION)
            )
        }

        checkName(projectId, null, envCreateInfo.name)

        val envCreator = EnvCreatorFactory.load(envCreateInfo.source.name)
            ?: throw IllegalArgumentException("unsupported nodeSourceType ${envCreateInfo.source}")

        val environmentId = envCreator.createEnv(
            projectId = projectId,
            userId = userId,
            envCreateInfo = envCreateInfo
        )
        // 审计
        ActionAuditContext.current()
            .setInstanceId(HashUtil.decodeIdToLong(environmentId.hashId).toString())
            .setInstanceName(envCreateInfo.name)
            .setInstance(envCreateInfo)
        return environmentId
    }

    @ActionAuditRecord(
        actionId = ActionId.ENVIRONMENT_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENVIRONMENT
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENVIRONMENT_EDIT_CONTENT
    )
    override fun updateEnvironment(
        userId: String,
        projectId: String,
        envHashId: String,
        envUpdateInfo: EnvUpdateInfo
    ) {
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

        ActionAuditContext.current()
            .addInstanceInfo(
                envId.toString(),
                envUpdateInfo.name,
                getEnvironment(userId, projectId, envHashId),
                envUpdateInfo
            )
        envUpdateInfo.envVars?.forEach {
            it.lastUpdateUser = userId
            it.lastUpdateTime = LocalDateTime.now()
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

    override fun listEnvironment(
        userId: String,
        projectId: String,
        envName: String?,
        envType: EnvType?,
        nodeHashId: String?
    ): List<EnvWithPermission> {
        val envIds = nodeHashId?.let {
            envNodeDao.listNodeIds(
                dslContext,
                projectId,
                listOf(HashUtil.decodeIdToLong(nodeHashId))
            ).map { it.envId }.toSet()
        }
        val envRecordList = envDao.list(
            dslContext = dslContext,
            projectId = projectId,
            envName = envName,
            envType = envType,
            envIds = envIds
        )
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
        val tagNodeCount = envTagDao.batchEnvTagNodeCount(
            dslContext = dslContext,
            envIds = envRecordList.filter { it.envNodeType == EnvNodeType.TAG.name }.map { it.envId }.toSet(),
            projectId = projectId
        )
        val nodeCountMap = envNodeDao.batchCount(
            dslContext = dslContext,
            projectId = projectId,
            envIds = envRecordList.filter { it.envNodeType == EnvNodeType.NODE.name }.map { it.envId }.toList()
        ).associateBy({ it.value1() }, { it.value2() })
        return envListResult.map {
            EnvWithPermission(
                envHashId = HashUtil.encodeLongId(it.envId),
                name = it.envName,
                desc = it.envDesc,
                envType = if (it.envType == EnvType.TEST.name) EnvType.DEV.name else it.envType, // 兼容性代码
                envNodeType = it.envNodeType,
                nodeCount = if (it.envNodeType == EnvNodeType.TAG.name) {
                    tagNodeCount[it.envId] ?: 0
                } else {
                    nodeCountMap[it.envId] ?: 0
                },
                tags = null,
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

        val tagNodeCount = envTagDao.batchEnvTagNodeCount(
            dslContext = dslContext,
            envIds = envRecordList.filter { it.envNodeType == EnvNodeType.TAG.name }.map { it.envId }.toSet(),
            projectId = projectId
        )
        val nodeCountMap = envNodeDao.batchCount(
            dslContext = dslContext,
            projectId = projectId,
            envIds = envRecordList.filter { it.envNodeType == EnvNodeType.NODE.name }.map { it.envId }.toList()
        ).associateBy({ it.value1() }, { it.value2() })
        return validRecordList.map {
            EnvWithPermission(
                envHashId = HashUtil.encodeLongId(it.envId),
                name = it.envName,
                desc = it.envDesc,
                envType = if (it.envType == EnvType.TEST.name) EnvType.DEV.name else it.envType, // 兼容性代码
                envNodeType = it.envNodeType,
                nodeCount = if (it.envNodeType == EnvNodeType.TAG.name) {
                    tagNodeCount[it.envId] ?: 0
                } else {
                    nodeCountMap[it.envId] ?: 0
                },
                tags = null,
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
            val nodeIds = fetchEnvNodes(projectId, listOf(it.envId)).map { node ->
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
                projectId = projectId,
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
            val nodeIds = fetchEnvNodes(nodeProjectId, listOf(it.envId)).map { node -> node.nodeId }.toSet()

            val normalNodeCount = if (nodeIds.isEmpty()) {
                0
            } else {
                thirdPartyAgentDao.countAgentByStatusAndOS(
                    dslContext = dslContext,
                    projectId = nodeProjectId,
                    nodeIds = nodeIds,
                    status = AgentStatus.IMPORT_OK,
                    os = os,
                    agentType = AgentType.BUILD
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
                    os = os,
                    agentType = AgentType.BUILD
                )
            }

            EnvWithNodeCount(
                projectId = projectId,
                envHashId = HashUtil.encodeLongId(it.envId),
                name = it.envName,
                normalNodeCount = normalNodeCount,
                abnormalNodeCount = abnormalNodeCount,
                sharedProjectId = it.sharedProjectId,
                sharedUserId = it.sharedUserId
            )
        }
    }

    @ActionAuditRecord(
        actionId = ActionId.ENVIRONMENT_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENVIRONMENT
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENVIRONMENT_VIEW_CONTENT
    )
    override fun getEnvironment(
        userId: String,
        projectId: String,
        envHashId: String,
        checkPermission: Boolean
    ): EnvWithPermission {
        val envId = HashUtil.decodeIdToLong(envHashId)
        if (checkPermission && !environmentPermissionService.checkEnvPermission(
                userId = userId,
                projectId = projectId,
                envId = envId,
                permission = AuthPermission.VIEW
            )
        ) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_VIEW_PERMISSSION)
            )
        }
        val env = envDao.get(dslContext, projectId, envId)
        ActionAuditContext.current()
            .setInstanceId(envId.toString())
            .setInstanceName(env.envName)
//        val nodeCount = if (env.envNodeType == EnvNodeType.TAG.name) {
//            envTagDao.batchEnvTagNodeCount(dslContext, setOf(envId), projectId)[envId]
//        } else {
//            envNodeDao.count(dslContext, projectId, envId)
//        }
        val tags = envTagDao.fetchEnvTag(dslContext, projectId, envId)
        return EnvWithPermission(
            envHashId = HashUtil.encodeLongId(env.envId),
            name = env.envName,
            desc = env.envDesc,
            envType = if (env.envType == EnvType.TEST.name) EnvType.DEV.name else env.envType, // 兼容性代码
            envNodeType = env.envNodeType,
            nodeCount = null,
            tags = tags,
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

    override fun getEnvEnvVar(
        userId: String,
        projectId: String,
        envHashId: String,
        envName: String?,
        envValue: String?,
        secure: Boolean?,
        lastUpdateUser: String?,
        checkPermission: Boolean
    ): List<EnvVar> {
        val envId = HashUtil.decodeIdToLong(envHashId)
        if (checkPermission && !environmentPermissionService.checkEnvPermission(
                userId = userId,
                projectId = projectId,
                envId = envId,
                permission = AuthPermission.VIEW
            )
        ) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_VIEW_PERMISSSION)
            )
        }
        val env = envDao.get(dslContext, projectId, envId)
        ActionAuditContext.current()
            .setInstanceId(envId.toString())
            .setInstanceName(env.envName)
        var envs: List<EnvVar> = if (env.envVars.isNullOrBlank()) {
            listOf()
        } else {
            ObjectMapper().readValue(env.envVars)
        }
        if (!envName.isNullOrBlank()) {
            envs = envs.filter { it.name == envName }
        }
        if (!envValue.isNullOrBlank()) {
            envs = envs.filter { it.value == envValue }
        }
        if (secure != null) {
            envs = envs.filter { it.secure == secure }
        }
        if (lastUpdateUser != null) {
            envs = envs.filter { it.lastUpdateUser == lastUpdateUser }
        }
        return envs
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
                envNodeType = it.envNodeType,
                nodeCount = null,
                tags = null,
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

    @ActionAuditRecord(
        actionId = ActionId.ENVIRONMENT_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENVIRONMENT
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENVIRONMENT_DELETE_CONTENT
    )
    override fun deleteEnvironment(userId: String, projectId: String, envHashId: String) {
        val envId = HashUtil.decodeIdToLong(envHashId)
        val envInfo = envDao.getOrNull(dslContext, projectId, envId) ?: return
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, AuthPermission.DELETE)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_DEL_PERMISSSION)
            )
        }
        ActionAuditContext.current()
            .setInstanceId(envId.toString())
            .setInstanceName(envInfo.envName)
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            envDao.deleteEnv(context, envId)
            environmentPermissionService.deleteEnv(projectId, envId)
            // 删除环境时需要同时删除EnvTag,EnvNode和EnvShareProject相关数据
            envTagDao.deleteByEnvId(dslContext, envId)
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
        val envNodes = fetchEnvNodes(projectId, envIds)
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
        val envNodes = fetchEnvNodes(projectId, listOf(envId))
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
        val nodeIdMaps = fetchEnvNodes(projectId, envIds).associate { it.nodeId to it.enableNode }
        val nodeList = nodeDao.listByIds(dslContext, projectId, nodeIdMaps.keys)
        if (nodeList.isEmpty()) {
            return emptyList()
        }
        val thirdPartyAgentMap =
            thirdPartyAgentDao.getAgentsByNodeIds(dslContext, nodeIdMaps.keys, projectId).associateBy { it.nodeId }
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
                displayName = NodeStringIdUtils.getRefineDisplayName(nodeStringId, it.displayName),
                envEnableNode = nodeIdMaps[it.nodeId] ?: true,
                agentHashId = if (thirdPartyAgent == null) {
                    null
                } else {
                    HashUtil.encodeLongId(thirdPartyAgent.id)
                },
                agentId = thirdPartyAgent?.id
            )
        }
    }

    override fun listAllEnvNodesNew(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        envHashIds: List<String>?,
        envName: String?,
        nodeIp: String?,
        displayName: String?,
        createdUser: String?,
        nodeStatus: NodeStatus?
    ): Page<NodeBaseInfo> {
        val envIds = envHashIds?.map { HashUtil.decodeIdToLong(it) }
            ?: if (envName != null) {
                val rEnvId = envDao.getByEnvName(dslContext, projectId, envName)?.envId
                if (rEnvId != null) {
                    listOf(rEnvId)
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        val canViewEnvIdList = environmentPermissionService.listEnvByViewPermission(userId, projectId)
        val invalidEnvIds = envIds.filterNot { canViewEnvIdList.contains(it) }
        if (invalidEnvIds.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ERROR_NODE_INSUFFICIENT_PERMISSIONS,
                params = arrayOf(invalidEnvIds.joinToString(","))
            )
        }
        val nodeIdMaps = fetchEnvNodes(projectId, envIds).associate { it.nodeId to it.enableNode }
        val nodeList = if (-1 != page) {
            val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page ?: 1, pageSize ?: 20)
            nodeDao.listNodesByIdListWithPageLimit(
                dslContext = dslContext,
                projectId = projectId,
                nodeIp = nodeIp,
                displayName = displayName,
                createdUser = createdUser,
                nodeStatus = nodeStatus,
                limit = sqlLimit.limit,
                offset = sqlLimit.offset,
                nodeIds = nodeIdMaps.keys
            )
        } else {
            nodeDao.listByIds(
                dslContext = dslContext,
                projectId = projectId,
                nodeIds = nodeIdMaps.keys,
                nodeIp = nodeIp,
                displayName = displayName,
                createdUser = createdUser,
                nodeStatus = nodeStatus
            )
        }
        if (nodeList.isEmpty()) {
            return Page(0, 0, 0, emptyList())
        }
        val thirdPartyAgentMap =
            thirdPartyAgentDao.getAgentsByNodeIds(dslContext, nodeIdMaps.keys, projectId).associateBy { it.nodeId }
        val nodeRecList = nodeList.map {
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
                nodeStatus = it.nodeStatus,
                agentStatus = getAgentStatus(it),
                nodeType = nodeType,
                osName = it.osName,
                createdUser = it.createdUser,
                operator = it.operator,
                bakOperator = it.bakOperator,
                gateway = gatewayShowName,
                displayName = NodeStringIdUtils.getRefineDisplayName(nodeStringId, it.displayName),
                envEnableNode = nodeIdMaps[it.nodeId] ?: true,
                size = it.size,
                agentHashId = if (thirdPartyAgent == null) {
                    null
                } else {
                    HashUtil.encodeLongId(thirdPartyAgent.id)
                },
                agentId = thirdPartyAgent?.id,
                lastModifyTime = it.lastModifyTime?.timestamp(),
            )
        }
        val count = nodeDao.countByNodeIdList(dslContext, projectId, nodeIdMaps.keys).toLong()
        return Page(
            page = page ?: 1,
            pageSize = pageSize ?: 20,
            count = count,
            records = nodeRecList
        )
    }

    @ActionAuditRecord(
        actionId = ActionId.ENVIRONMENT_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENVIRONMENT
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENVIRONMENT_EDIT_ADD_NODES_CONTENT
    )
    override fun addEnvNodes(
        userId: String,
        projectId: String,
        envHashId: String,
        data: EnvAddNodesData
    ) {
        val envId = HashUtil.decodeIdToLong(envHashId)
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, AuthPermission.EDIT)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_EDIT_PERMISSSION)
            )
        }

        val envRecord = envDao.get(dslContext, projectId, envId)

        // 添加标签
        val tags = data.tags
        if (!tags.isNullOrEmpty()) {
            if (!authProjectApi.checkProjectManager(userId, pipelineAuthServiceCode, projectId)) {
                throw PermissionForbiddenException(
                    message = I18nUtil.getCodeLanMessage(
                        ERROR_NODE_TAG_NO_EDIT_PERMISSSION,
                        language = I18nUtil.getLanguage(userId)
                    )
                )
            }
            val tagKeys = nodeTagKeyDao.fetchNodeKeyByIds(
                dslContext = dslContext,
                projectId = projectId,
                keyIds = tags.map { it.tagKeyId }.toSet()
            ).associate { it.id to Pair((it.allowMulValues ?: false), it.keyName) }
            val tagsMap = mutableMapOf<Long, MutableSet<Long>>()
            tags.forEach { tag ->
                tagsMap.putIfAbsent(tag.tagKeyId, mutableSetOf(tag.tagValueId))?.add(tag.tagValueId)
            }
            tags.forEach { tag ->
                if (tagKeys[tag.tagKeyId]?.first == false && (tagsMap[tag.tagKeyId]?.size ?: 0) > 1) {
                    throw ErrorCodeException(
                        errorCode = EnvironmentMessageCode.ERROR_NODE_TAG_NO_ALLOW_VALUES,
                        params = arrayOf(tagKeys[tag.tagKeyId]?.second ?: "")
                    )
                }
            }
            ActionAuditContext.current()
                .addInstanceInfo(envHashId, JsonUtil.toJson(tags), null, null)

            val envId = HashUtil.decodeIdToLong(envHashId)
            dslContext.transaction { config ->
                val ctx = DSL.using(config)
                // 类型转换需要清空之前类型的记录
                if (envRecord.envNodeType == EnvNodeType.NODE.name) {
                    envNodeDao.deleteByEnvId(dslContext, envId)
                    envDao.updateEnvNodeType(dslContext, envId, EnvNodeType.TAG)
                }
                envTagDao.deleteEnvTags(ctx, projectId, setOf(envId))
                envTagDao.batchAddEnvTags(
                    dslContext = ctx,
                    projectId = projectId,
                    envAndValueAndKeyIds = mapOf(envId to tags.associate { it.tagValueId to it.tagKeyId })
                )
            }
            return
        }

        val nodeHashIds = data.nodeHashIds ?: return
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
        ActionAuditContext.current()
            .setInstanceId(envId.toString())
            .setInstanceName(env.envName)
            .addExtendData("addNodeIds", nodeLongIds.toString())

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
        val toAddNodeIds = nodeLongIds.subtract(existEnvNodeIds.toSet())

        // 验证节点类型
        val existNodesMap = existNodes.associateBy { it.nodeId }
        val serverNodeTypes = listOf(NodeType.CMDB.name)
        val buildNodeType = listOf(NodeType.DEVCLOUD.name, NodeType.THIRDPARTY.name)

        toAddNodeIds.forEach {
            if (env.envType == EnvType.BUILD.name && existNodesMap[it]?.nodeType in serverNodeTypes) {
                throw ErrorCodeException(
                    errorCode = ERROR_ENV_BUILD_CAN_NOT_ADD_SVR,
                    params = arrayOf(HashUtil.encodeLongId(it))
                )
            }
            if (env.envType != EnvType.BUILD.name && existNodesMap[it]?.nodeType in buildNodeType) {
                throw ErrorCodeException(
                    errorCode = ERROR_ENV_DEPLOY_CAN_NOT_ADD_AGENT,
                    params = arrayOf(HashUtil.encodeLongId(it))
                )
            }
        }
        // 类型转换需要清空之前类型的记录
        if (envRecord.envNodeType == EnvNodeType.TAG.name) {
            envTagDao.deleteByEnvId(dslContext, envId)
            envDao.updateEnvNodeType(dslContext, envId, EnvNodeType.NODE)
        }
        envNodeDao.batchStoreEnvNode(dslContext, toAddNodeIds.toList(), envId, projectId)
    }

    @ActionAuditRecord(
        actionId = ActionId.ENVIRONMENT_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENVIRONMENT
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENVIRONMENT_EDIT_DELETE_NODES_CONTENT
    )
    override fun deleteEnvNodes(userId: String, projectId: String, envHashId: String, nodeHashIds: List<String>) {
        val envId = HashUtil.decodeIdToLong(envHashId)
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, AuthPermission.EDIT)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_EDIT_PERMISSSION)
            )
        }
        val envInfo = envDao.getOrNull(dslContext, projectId, envId) ?: return
        val nodeLongIds = nodeHashIds.map { HashUtil.decodeIdToLong(it) }
        ActionAuditContext.current()
            .setInstanceId(envId.toString())
            .setInstanceName(envInfo.envName)
            .addExtendData("deleteNodeIds", nodeLongIds.toString())
        envNodeDao.batchDeleteEnvNode(
            dslContext = dslContext,
            projectId = projectId,
            envId = envId,
            nodeIds = nodeLongIds
        )
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
                    envNodeType = it.envNodeType,
                    nodeCount = null,
                    tags = null,
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
                    envNodeType = it.envNodeType,
                    nodeCount = null,
                    tags = null,
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
                envNodeType = it.envNodeType,
                nodeCount = null,
                tags = null,
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
                envNodeType = it.envNodeType,
                nodeCount = null,
                tags = null,
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

    @ActionAuditRecord(
        actionId = ActionId.ENVIRONMENT_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENVIRONMENT
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENVIRONMENT_EDIT_SET_SHARE_ENV_CONTENT
    )
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
        ActionAuditContext.current()
            .setInstanceId(envId.toString())
            .setInstanceName(existEnv.envName)
        envShareProjectDao.batchSave(
            dslContext = dslContext,
            userId = userId,
            envId = envId,
            envName = existEnv.envName,
            mainProjectId = projectId,
            sharedProjects = sharedProjects
        )
    }

    @ActionAuditRecord(
        actionId = ActionId.ENVIRONMENT_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENVIRONMENT
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENVIRONMENT_OF_SHARE_DELETE_CONTENT
    )
    fun deleteShareEnv(userId: String, projectId: String, envHashId: String) {
        val envId = HashUtil.decodeIdToLong(envHashId)
        val envInfo = envDao.getOrNull(dslContext, projectId, envId) ?: return
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, AuthPermission.DELETE)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_DEL_PERMISSSION)
            )
        }
        ActionAuditContext.current()
            .setInstanceId(envId.toString())
            .setInstanceName(envInfo.envName)
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
            creator = null,
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
        creator: String?,
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
        val records = envShareProjectDao.listPage(
            dslContext = dslContext,
            projectId = projectId,
            envId = envId,
            name = name,
            creator = creator,
            offset = offset,
            limit = limitTmp + 1
        )
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

    @ActionAuditRecord(
        actionId = ActionId.ENVIRONMENT_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENVIRONMENT
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENVIRONMENT_OF_SHARE_DELETE_CONTENT
    )
    fun deleteShareEnvBySharedProj(userId: String, projectId: String, envHashId: String, sharedProjectId: String) {
        val envId = HashUtil.decodeIdToLong(envHashId)
        val envInfo = envDao.getOrNull(dslContext, projectId, envId) ?: return
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, AuthPermission.DELETE)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_DEL_PERMISSSION)
            )
        }
        ActionAuditContext.current()
            .setInstanceId(envId.toString())
            .setInstanceName(envInfo.envName)
        envShareProjectDao.deleteBySharedProj(dslContext, envId, projectId, sharedProjectId)
    }

    @ActionAuditRecord(
        actionId = ActionId.ENVIRONMENT_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENVIRONMENT
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENVIRONMENT_ENABLE_OR_DISABLE_NODE
    )
    fun enableNodeEnv(
        projectId: String,
        userId: String,
        envHashId: String?,
        nodeHashId: String?,
        envName: String?,
        nodeName: String?,
        enableNode: Boolean
    ): Result<Boolean> {
        val env = if (envHashId != null) {
            envDao.get(dslContext, projectId, envId = HashUtil.decodeIdToLong(envHashId))
        } else if (envName != null) {
            envDao.getByEnvName(dslContext, projectId, envName)
        } else {
            throw NotFoundException(
                I18nUtil.getCodeLanMessage(
                    messageCode = CommonMessageCode.ERROR_NEED_PARAM_, params = arrayOf("envHashId/envName")
                )
            )
        }
        if (env == null) {
            throw NotFoundException(
                I18nUtil.getCodeLanMessage(
                    messageCode = ERROR_ENV_NOT_EXISTS, params = arrayOf(envHashId ?: envName!!)
                )
            )
        }

        val envId = env.envId
        if (!environmentPermissionService.checkEnvPermission(
                userId = userId,
                projectId = projectId,
                envId = envId,
                permission = AuthPermission.EDIT
            )
        ) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_EDIT_PERMISSSION)
            )
        }

        val node = if (nodeHashId != null) {
            nodeDao.get(dslContext, projectId, nodeId = HashUtil.decodeIdToLong(nodeHashId))
        } else if (nodeName != null) {
            nodeDao.getByDisplayName(
                dslContext = dslContext,
                projectId = projectId,
                displayName = nodeName,
                nodeType = listOf(NodeType.THIRDPARTY.name)
            ).firstOrNull()
        } else {
            throw NotFoundException(
                I18nUtil.getCodeLanMessage(
                    messageCode = CommonMessageCode.ERROR_NEED_PARAM_, params = arrayOf("nodeHashId/nodeName")
                )
            )
        }
        if (node == null) {
            throw NotFoundException(
                I18nUtil.getCodeLanMessage(
                    messageCode = ERROR_NODE_NOT_EXISTS, params = arrayOf(nodeHashId ?: nodeName!!)
                )
            )
        }

        val nodeId = node.nodeId
        ActionAuditContext.current().setInstanceId(nodeId.toString()).setInstanceName(envId.toString())
        if (enableNode) {
            ActionAuditContext.current().addAttribute(PROJECT_ENABLE_OR_DISABLE_TEMPLATE, "enable")
        } else {
            ActionAuditContext.current().addAttribute(PROJECT_ENABLE_OR_DISABLE_TEMPLATE, "disable")
        }
        return if (envNodeDao.exists(dslContext, projectId, envId, nodeId)) {
            Result(
                data = envNodeDao.disableOrEnableNode(
                    dslContext = dslContext,
                    projectId = projectId,
                    envId = envId,
                    nodeId = nodeId,
                    enable = enableNode
                )
            )
        } else {
            Result(data = false,
                   status = 400,
                   message = I18nUtil.getCodeLanMessage(
                       messageCode = ERROR_NODE_NOT_EXISTS,
                       params = arrayOf(node.displayName)
                   )
            )
        }
    }

    /**
     * 统一获取环境所拥有的节点的接口，动态环境和静态环境都从这里拿
     * @return <nodeId, enableNode>
     */
    fun fetchEnvNodes(
        projectId: String,
        envIds: List<Long>
    ): List<EnvNode> {
        if (envIds.isEmpty()) {
            return emptyList()
        }
        val result = mutableListOf<EnvNode>()
        val envs = envDao.list(dslContext, projectId, envIds = envIds)
        envTagDao.batchEnvTagNode(
            dslContext = dslContext,
            projectId = projectId,
            envIds = envs.filter { it.envNodeType == EnvNodeType.TAG.name }.map { it.envId }.toSet()
        ).forEach {
            it.value.forEach { node ->
                result.add(EnvNode(it.key, node, true))
            }
        }
        val envNodeRecordList = envNodeDao.list(
            dslContext = dslContext,
            projectId = projectId,
            envIds = envs.filter { it.envNodeType == EnvNodeType.NODE.name }.map { it.envId }.toList()
        ).forEach {
            result.add(EnvNode(it.envId, it.nodeId, it.enableNode))
        }
        return result
    }
}
