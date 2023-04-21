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

package com.tencent.devops.process.permission

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

class StreamPipelinePermissionServiceImpl @Autowired constructor(
    val client: Client,
    val pipelineInfoDao: PipelineInfoDao,
    val dslContext: DSLContext,
    val checkTokenService: ClientTokenService
) : PipelinePermissionService {
    override fun checkPipelinePermission(
        userId: String,
        projectId: String,
        permission: AuthPermission
    ): Boolean {
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
            userId = userId,
            token = checkTokenService.getSystemToken(null) ?: "",
            action = permission.value,
            projectCode = projectId,
            resourceCode = AuthResourceType.PIPELINE_DEFAULT.value
        ).data ?: false
    }

    override fun checkPipelinePermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: AuthPermission
    ): Boolean {
        return checkPipelinePermission(
            userId = userId,
            projectId = projectId,
            permission = permission
        )
    }

    override fun validPipelinePermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: AuthPermission,
        message: String?
    ) {
        val validResult = checkPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = permission
        )
        if (!validResult) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun getResourceByPermission(
        userId: String,
        projectId: String,
        permission: AuthPermission
    ): List<String> {
        if (!checkPipelinePermission(userId, projectId, permission)) {
            return emptyList()
        }
        return getProjectAllInstance(projectId)
    }

    override fun filterPipelines(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>,
        pipelineIds: List<String>
    ): Map<AuthPermission, List<String>> {
        return authPermissions.associateWith {
            pipelineIds
        }
    }

    override fun createResource(
        userId: String,
        projectId: String,
        pipelineId: String,
        pipelineName: String
    ) {
        return
    }

    override fun modifyResource(
        projectId: String,
        pipelineId: String,
        pipelineName: String
    ) {
        return
    }

    override fun deleteResource(projectId: String, pipelineId: String) {
        return
    }

    override fun isProjectUser(
        userId: String,
        projectId: String,
        group: BkAuthGroup?
    ): Boolean {
        return client.get(ServiceProjectAuthResource::class).isProjectUser(
            token = checkTokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId
        ).data ?: false
    }

    override fun checkProjectManager(userId: String, projectId: String): Boolean {
        return client.get(ServiceProjectAuthResource::class).checkProjectManager(
            token = checkTokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId
        ).data ?: false
    }

    private fun getProjectAllInstance(projectId: String): List<String> {
        return pipelineInfoDao.searchByProject(dslContext, projectId)?.map { it.pipelineId } ?: emptyList()
    }
}
