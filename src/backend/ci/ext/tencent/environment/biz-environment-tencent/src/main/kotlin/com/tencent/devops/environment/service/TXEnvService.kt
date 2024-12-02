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
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_ENV_NO_DEL_PERMISSSION
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_ENV_NO_EDIT_PERMISSSION
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_HAD_BEEN_ASSIGN
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NAME_DUPLICATE
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NAME_INVALID_CHARACTER
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.EnvShareProjectDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.EnvironmentId
import com.tencent.devops.environment.pojo.enums.TXEnvType
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
@Suppress("ALL")
class TXEnvService @Autowired constructor(
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
) : EnvService(
    dslContext = dslContext,
    envDao = envDao,
    nodeDao = nodeDao,
    envNodeDao = envNodeDao,
    thirdPartyAgentDao = thirdPartyAgentDao,
    slaveGatewayService = slaveGatewayService,
    environmentPermissionService = environmentPermissionService,
    envShareProjectDao = envShareProjectDao,
    nodeService = nodeService,
    client = client
) {

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
        if (envCreateInfo.envType == TXEnvType.DEVX.envType() && !envCreateInfo.nodeHashIds.isNullOrEmpty()) {
            // 暂时仅支持一个公共云桌面进入一个环境
            val check = envNodeDao.listNodeIds(
                dslContext = dslContext,
                projectId = projectId,
                nodeIds = envCreateInfo.nodeHashIds!!.map { HashUtil.decodeIdToLong(it) })
            if (check.isNotEmpty()) {
                throw ErrorCodeException(errorCode = ERROR_NODE_HAD_BEEN_ASSIGN)
            }
        }
        return super.createEnvironment(userId, projectId, envCreateInfo).also { envHashId ->
            if (envCreateInfo.envType == TXEnvType.DEVX.envType()) {
                client.get(ServiceRemoteDevResource::class).reloadEnvHook(
                    userId = userId,
                    projectId = projectId,
                    envHashId = envHashId.hashId,
                    nodeHashId = null
                )
            }
        }
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
        nodeHashIds: List<String>
    ) {
        val env = envDao.get(dslContext, projectId, HashUtil.decodeIdToLong(envHashId))
        if (env.envType == TXEnvType.DEVX.name) {
            // 暂时仅支持一个公共云桌面进入一个环境
            val check = envNodeDao.listNodeIds(
                dslContext = dslContext,
                projectId = projectId,
                nodeIds = nodeHashIds.map { HashUtil.decodeIdToLong(it) })
            if (check.isNotEmpty()) {
                throw ErrorCodeException(errorCode = ERROR_NODE_HAD_BEEN_ASSIGN)
            }
        }
        super.addEnvNodes(userId, projectId, envHashId, nodeHashIds)
        if (env.envType == TXEnvType.DEVX.name) {
            client.get(ServiceRemoteDevResource::class).reloadEnvHook(
                userId = userId,
                projectId = projectId,
                envHashId = envHashId,
                nodeHashId = nodeHashIds
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
        val env = envDao.getOrNull(dslContext, projectId, envId) ?: return
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, AuthPermission.DELETE)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_DEL_PERMISSSION)
            )
        }
        if (env.envType == TXEnvType.DEVX.name) {
            client.get(ServiceRemoteDevResource::class).deleteEnvHook(
                userId = userId,
                projectId = projectId,
                envHashId = envHashId,
                nodeHashId = null
            )
        }
        super.deleteEnvironment(userId, projectId, envHashId)
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

        val env = envDao.getOrNull(dslContext, projectId, envId) ?: return
        if (env.envType == TXEnvType.DEVX.name) {
            client.get(ServiceRemoteDevResource::class).deleteEnvHook(
                userId = userId,
                projectId = projectId,
                envHashId = envHashId,
                nodeHashId = nodeHashIds
            )
        }
        super.deleteEnvNodes(userId, projectId, envHashId, nodeHashIds)
    }
}
