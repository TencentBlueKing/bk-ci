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

package com.tencent.devops.auth.provider.v0.service

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.pojo.vo.ProjectPermissionInfoVO
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.code.BSProjectServiceCodec
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class V0AuthPermissionProjectServiceImpl @Autowired constructor(
    private val authProjectApi: AuthProjectApi,
    private val bsProjectAuthServiceCode: BSProjectServiceCodec,
    val client: Client
) : PermissionProjectService {

    override fun getProjectUsers(projectCode: String, group: BkAuthGroup?): List<String> {

        return authProjectApi.getProjectUsers(
            serviceCode = bsProjectAuthServiceCode,
            projectCode = projectCode,
            group = group
        )
    }

    override fun getProjectGroupAndUserList(projectCode: String): List<BkAuthGroupAndUserList> {
        return authProjectApi.getProjectGroupAndUserList(
            serviceCode = bsProjectAuthServiceCode,
            projectCode = projectCode
        )
    }

    override fun getUserProjects(userId: String): List<String> {
        return authProjectApi.getUserProjects(
            serviceCode = bsProjectAuthServiceCode,
            userId = userId,
            supplier = null
        )
    }

    override fun getUserProjectsByPermission(
        userId: String,
        action: String,
        resourceType: String?
    ): List<String> {
        return emptyList()
    }

    override fun isProjectUser(userId: String, projectCode: String, group: BkAuthGroup?): Boolean {
        return authProjectApi.isProjectUser(
            user = userId,
            projectCode = projectCode,
            group = group,
            serviceCode = bsProjectAuthServiceCode
        )
    }

    override fun checkProjectManager(userId: String, projectCode: String): Boolean {
        return authProjectApi.checkProjectManager(
            userId = userId,
            projectCode = projectCode,
            serviceCode = bsProjectAuthServiceCode
        )
    }

    override fun createProjectUser(userId: String, projectCode: String, role: String): Boolean {
        return true
    }

    override fun batchCreateProjectUser(
        userId: String,
        projectCode: String,
        roleCode: String,
        members: List<String>
    ): Boolean = true

    override fun getProjectRoles(projectCode: String, projectId: String): List<BKAuthProjectRolesResources> {
        return authProjectApi.getProjectRoles(
            serviceCode = bsProjectAuthServiceCode,
            projectCode = projectCode,
            projectId = projectId
        )
    }

    override fun getProjectPermissionInfo(projectCode: String): ProjectPermissionInfoVO {
        val projectInfo = client.get(ServiceProjectResource::class).get(englishName = projectCode).data
            ?: throw ErrorCodeException(
                errorCode = AuthMessageCode.RESOURCE_NOT_FOUND,
                params = arrayOf(projectCode),
                defaultMessage = "project $projectCode not exist"
            )
        return ProjectPermissionInfoVO(
            projectCode = projectCode,
            projectName = projectInfo.projectName,
            creator = projectInfo.creator!!,
            owners = getProjectUsers(
                projectCode, BkAuthGroup.MANAGER
            ),
            members = getProjectUsers(projectCode, null)
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(V0AuthPermissionProjectServiceImpl::class.java)
    }
}
