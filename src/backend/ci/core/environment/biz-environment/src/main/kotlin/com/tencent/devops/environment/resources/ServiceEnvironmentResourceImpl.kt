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
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.EnvWithNodeCount
import com.tencent.devops.environment.pojo.EnvWithPermission
import com.tencent.devops.environment.pojo.EnvironmentId
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.SharedProjectInfoWrap
import com.tencent.devops.environment.service.EnvService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceEnvironmentResourceImpl @Autowired constructor(
    private val envService: EnvService
) : ServiceEnvironmentResource {

    @BkTimed(extraTags = ["operate", "getEnv"])
    override fun listUsableServerEnvs(userId: String, projectId: String): Result<List<EnvWithPermission>> {
        return Result(envService.listUsableServerEnvs(userId, projectId))
    }

    @BkTimed(extraTags = ["operate", "getEnv"])
    override fun listRawByEnvHashIds(
        userId: String,
        projectId: String,
        envHashIds: List<String>
    ): Result<List<EnvWithPermission>> {
        return Result(envService.listRawEnvByHashIds(userId, projectId, envHashIds))
    }

    @BkTimed(extraTags = ["operate", "getEnv"])
    override fun list(userId: String, projectId: String): Result<List<EnvWithPermission>> {
        return Result(envService.listEnvironment(userId, projectId))
    }

    @BkTimed(extraTags = ["operate", "createEnvironment"])
    @AuditEntry(actionId = ActionId.ENVIRONMENT_CREATE)
    override fun create(userId: String, projectId: String, environment: EnvCreateInfo): Result<EnvironmentId> {
        if (environment.name.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_NAME_NULL)
        }

        return Result(envService.createEnvironment(userId, projectId, environment))
    }

    @AuditEntry(actionId = ActionId.ENVIRONMENT_VIEW)
    override fun get(
        userId: String,
        projectId: String,
        envHashId: String,
        checkPermission: Boolean?
    ): Result<EnvWithPermission> {
        if (envHashId.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_ID_NULL)
        }

        return Result(envService.getEnvironment(userId, projectId, envHashId, checkPermission ?: true))
    }

    @AuditEntry(actionId = ActionId.ENVIRONMENT_DELETE)
    override fun delete(userId: String, projectId: String, envHashId: String): Result<Boolean> {
        if (envHashId.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_ID_NULL)
        }
        envService.deleteEnvironment(userId, projectId, envHashId)
        return Result(true)
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

    @BkTimed(extraTags = ["operate", "getNode"])
    override fun listNodesByEnvIds(
        userId: String,
        projectId: String,
        envHashIds: List<String>
    ): Result<List<NodeBaseInfo>> {
        if (envHashIds.isEmpty()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_NEED_PARAM_, params = arrayOf("envHashIds"))
        }
        return Result(envService.listAllEnvNodes(userId, projectId, envHashIds))
    }

    @BkTimed(extraTags = ["operate", "getNode"])
    override fun listNodesByEnvIdsNew(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        envHashIds: List<String>
    ): Result<Page<NodeBaseInfo>> {
        if (envHashIds.isEmpty()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_NEED_PARAM_, params = arrayOf("envHashIds"))
        }
        return Result(envService.listAllEnvNodesNew(userId, projectId, page, pageSize, envHashIds))
    }

    @BkTimed(extraTags = ["operate", "getEnv"])
    override fun listRawByEnvNames(
        userId: String,
        projectId: String,
        envNames: List<String>
    ): Result<List<EnvWithPermission>> {
        if (envNames.isEmpty()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_NEED_PARAM_, params = arrayOf("envNames"))
        }
        return Result(envService.listRawEnvByEnvNames(userId, projectId, envNames))
    }

    @BkTimed(extraTags = ["operate", "getEnv"])
    override fun listBuildEnvs(userId: String, projectId: String, os: OS): Result<List<EnvWithNodeCount>> {
        return Result(envService.listBuildEnvs(userId, projectId, os))
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
