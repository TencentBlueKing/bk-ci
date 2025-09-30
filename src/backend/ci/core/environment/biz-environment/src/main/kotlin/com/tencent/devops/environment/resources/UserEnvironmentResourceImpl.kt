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

package com.tencent.devops.environment.resources

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.api.UserEnvironmentResource
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_ENV_NO_EDIT_PERMISSSION
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.EnvUpdateInfo
import com.tencent.devops.environment.pojo.EnvWithNodeCount
import com.tencent.devops.environment.pojo.EnvWithPermission
import com.tencent.devops.environment.pojo.EnvironmentId
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.SharedProjectInfo
import com.tencent.devops.environment.pojo.SharedProjectInfoWrap
import com.tencent.devops.environment.pojo.enums.EnvType
import com.tencent.devops.environment.service.EnvService
import org.springframework.beans.factory.annotation.Autowired

@Suppress("ALL")
@RestResource
class UserEnvironmentResourceImpl @Autowired constructor(
    private val envService: EnvService,
    private val environmentPermissionService: EnvironmentPermissionService
) : UserEnvironmentResource {

    @BkTimed(extraTags = ["operate", "getEnv"])
    override fun listUsableServerEnvs(userId: String, projectId: String): Result<List<EnvWithPermission>> {
        return Result(envService.listUsableServerEnvs(userId, projectId))
    }

    override fun hasCreatePermission(userId: String, projectId: String): Result<Boolean> {
        return Result(environmentPermissionService.checkEnvPermission(userId, projectId, AuthPermission.CREATE))
    }

    @BkTimed(extraTags = ["operate", "createEnvironment"])
    @AuditEntry(actionId = ActionId.ENVIRONMENT_CREATE)
    override fun create(userId: String, projectId: String, environment: EnvCreateInfo): Result<EnvironmentId> {
        if (environment.name.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_NAME_NULL)
        }
        if (environment.name.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_NAME_TOO_LONG)
        }

        return Result(envService.createEnvironment(userId, projectId, environment))
    }

    @AuditEntry(actionId = ActionId.ENVIRONMENT_EDIT)
    override fun update(
        userId: String,
        projectId: String,
        envHashId: String,
        environment: EnvUpdateInfo
    ): Result<Boolean> {
        if (envHashId.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_ID_NULL)
        }

        if (environment.name.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_NAME_NULL)
        }

        envService.updateEnvironment(userId, projectId, envHashId, environment)
        return Result(true)
    }

    @BkTimed(extraTags = ["operate", "getEnv"])
    override fun list(
        userId: String,
        projectId: String,
        envName: String?,
        envType: EnvType?,
        nodeHashId: String?
    ): Result<List<EnvWithPermission>> {
        return Result(
            envService.listEnvironment(
                userId = userId,
                projectId = projectId,
                envName = envName,
                envType = envType,
                nodeHashId = nodeHashId
            )
        )
    }

    @BkTimed(extraTags = ["operate", "getEnv"])
    override fun listByType(userId: String, projectId: String, envType: EnvType): Result<List<EnvWithNodeCount>> {
        return Result(envService.listEnvironmentByType(userId, projectId, envType))
    }

    @BkTimed(extraTags = ["operate", "getEnv"])
    override fun listBuildEnvs(userId: String, projectId: String, os: OS): Result<List<EnvWithNodeCount>> {
        return Result(envService.listBuildEnvs(userId, projectId, os))
    }

    @BkTimed(extraTags = ["operate", "getEnv"])
    @AuditEntry(actionId = ActionId.ENVIRONMENT_VIEW)
    override fun get(userId: String, projectId: String, envHashId: String): Result<EnvWithPermission> {
        if (envHashId.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_ID_NULL)
        }

        return Result(envService.getEnvironment(userId, projectId, envHashId))
    }

    @AuditEntry(actionId = ActionId.ENVIRONMENT_DELETE)
    override fun delete(userId: String, projectId: String, envHashId: String): Result<Boolean> {
        if (envHashId.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_ID_NULL)
        }
        envService.deleteEnvironment(userId, projectId, envHashId)
        return Result(true)
    }

    @BkTimed(extraTags = ["operate", "getEnv"])
    override fun listNodes(userId: String, projectId: String, envHashId: String): Result<List<NodeBaseInfo>> {
        if (envHashId.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_ID_NULL)
        }

        return Result(envService.listAllEnvNodes(userId, projectId, listOf(envHashId)))
    }

    @BkTimed(extraTags = ["operate", "getEnv"])
    override fun listNodesNew(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        envHashId: String
    ): Result<Page<NodeBaseInfo>> {
        if (envHashId.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_ID_NULL)
        }
        return Result(envService.listAllEnvNodesNew(userId, projectId, page, pageSize, listOf(envHashId)))
    }

    @BkTimed(extraTags = ["operate", "createNode"])
    @AuditEntry(actionId = ActionId.ENVIRONMENT_EDIT)
    override fun addNodes(
        userId: String,
        projectId: String,
        envHashId: String,
        nodeHashIds: List<String>
    ): Result<Boolean> {
        if (envHashId.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_ID_NULL)
        }

        if (nodeHashIds.isEmpty()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_NODE_HASH_ID_ILLEGAL)
        }

        envService.addEnvNodes(userId, projectId, envHashId, nodeHashIds)
        return Result(true)
    }

    @AuditEntry(actionId = ActionId.ENVIRONMENT_EDIT)
    override fun deleteNodes(
        userId: String,
        projectId: String,
        envHashId: String,
        nodeHashIds: List<String>
    ): Result<Boolean> {
        if (envHashId.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_ID_NULL)
        }

        if (nodeHashIds.isEmpty()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_NODE_HASH_ID_ILLEGAL)
        }

        envService.deleteEnvNodes(userId, projectId, envHashId, nodeHashIds)
        return Result(true)
    }

    override fun listUserShareEnv(
        userId: String,
        projectId: String,
        envHashId: String,
        search: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<SharedProjectInfo>> {
        if (projectId.isEmpty()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_SHARE_PROJECT_EMPTY)
        }
        return Result(
            envService.listUserShareEnv(
                userId = userId,
                projectId = projectId,
                envHashId = envHashId,
                search = search,
                page = page ?: 1,
                pageSize = pageSize ?: 20
            )
        )
    }

    override fun listShareEnv(
        userId: String,
        projectId: String,
        envHashId: String,
        name: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<SharedProjectInfo>> {
        checkParam(userId, projectId, envHashId)
        return Result(
            envService.listShareEnv(
                userId,
                projectId,
                envHashId,
                name,
                page ?: 1,
                pageSize ?: 20
            )
        )
    }

    @AuditEntry(actionId = ActionId.ENVIRONMENT_EDIT)
    override fun setShareEnv(
        userId: String,
        projectId: String,
        envHashId: String,
        sharedProjects: SharedProjectInfoWrap
    ): Result<Boolean> {
        checkParam(userId, projectId, envHashId)
        envService.setShareEnv(userId, projectId, envHashId, sharedProjects.sharedProjects)
        return Result(true)
    }

    @AuditEntry(actionId = ActionId.ENVIRONMENT_DELETE)
    override fun deleteShareEnv(userId: String, projectId: String, envHashId: String): Result<Boolean> {
        checkParam(userId, projectId, envHashId)
        envService.deleteShareEnv(userId, projectId, envHashId)
        return Result(true)
    }

    @AuditEntry(actionId = ActionId.ENVIRONMENT_DELETE)
    override fun deleteShareEnvBySharedProj(
        userId: String,
        projectId: String,
        envHashId: String,
        sharedProjectId: String
    ): Result<Boolean> {
        checkParam(userId, projectId, envHashId)
        envService.deleteShareEnvBySharedProj(userId, projectId, envHashId, sharedProjectId)
        return Result(true)
    }

    @AuditEntry(actionId = ActionId.ENVIRONMENT_EDIT)
    override fun enableNodeEnv(
        userId: String,
        projectId: String,
        envHashId: String,
        nodeHashId: String,
        enableNode: Boolean
    ): Result<Boolean> {
        if (!environmentPermissionService.checkEnvPermission(
                userId = userId,
                projectId = projectId,
                envId = HashUtil.decodeIdToLong(envHashId),
                permission = AuthPermission.EDIT
            )
        ) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_EDIT_PERMISSSION)
            )
        }
        envService.enableNodeEnv(
            projectId = projectId,
            envHashId = envHashId,
            nodeHashId = nodeHashId,
            enableNode = enableNode
        )
        return Result(true)
    }

    private fun checkParam(
        userId: String,
        projectId: String,
        envHashId: String
    ) {
        if (userId.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_ID_NULL)
        }

        if (envHashId.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_ID_NULL)
        }

        if (projectId.isEmpty()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_SHARE_PROJECT_EMPTY)
        }
    }
}
