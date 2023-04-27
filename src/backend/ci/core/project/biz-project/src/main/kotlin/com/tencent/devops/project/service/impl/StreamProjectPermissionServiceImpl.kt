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

package com.tencent.devops.project.service.impl

import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.project.pojo.AuthProjectCreateInfo
import com.tencent.devops.project.pojo.ResourceUpdateInfo
import com.tencent.devops.project.service.ProjectPermissionService
import org.springframework.beans.factory.annotation.Autowired

class StreamProjectPermissionServiceImpl @Autowired constructor(
    val client: Client,
    val tokenService: ClientTokenService
) : ProjectPermissionService {
    override fun verifyUserProjectPermission(
        accessToken: String?,
        projectCode: String,
        userId: String
    ): Boolean {
        return client.get(ServiceProjectAuthResource::class).isProjectUser(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectCode
        ).data ?: false
    }

    override fun createResources(
        resourceRegisterInfo: ResourceRegisterInfo,
        authProjectCreateInfo: AuthProjectCreateInfo
    ): String {
        return ""
    }

    override fun deleteResource(projectCode: String) {
        return
    }

    override fun modifyResource(
        resourceUpdateInfo: ResourceUpdateInfo
    ) {
        return
    }

    override fun getUserProjects(userId: String): List<String> {
        return listOf("demo")
    }

    override fun getUserProjectsAvailable(userId: String): Map<String, String> {
        return emptyMap()
    }

    override fun verifyUserProjectPermission(
        accessToken: String?,
        projectCode: String,
        userId: String,
        permission: AuthPermission
    ): Boolean {
        if (permission == AuthPermission.MANAGE) {
            return client.get(ServiceProjectAuthResource::class).checkProjectManager(
                userId = userId,
                projectCode = projectCode,
                token = tokenService.getSystemToken(null)!!
            ).data ?: false
        }
        return client.get(ServiceProjectAuthResource::class).isProjectUser(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectCode
        ).data ?: false
    }

    override fun cancelCreateAuthProject(userId: String, projectCode: String) = Unit

    override fun cancelUpdateAuthProject(userId: String, projectCode: String) = Unit

    override fun needApproval(needApproval: Boolean?) = false

    override fun isShowUserManageIcon(): Boolean = false

    override fun filterProjects(userId: String, permission: AuthPermission): List<String>? = null
}
