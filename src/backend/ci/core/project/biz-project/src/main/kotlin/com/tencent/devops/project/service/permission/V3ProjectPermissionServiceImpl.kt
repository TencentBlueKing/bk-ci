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
 *
 */

package com.tencent.devops.project.service.permission

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.pojo.AuthProjectCreateInfo
import com.tencent.devops.project.pojo.ResourceUpdateInfo
import com.tencent.devops.project.service.ProjectPermissionService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

@Suppress("ALL")
class V3ProjectPermissionServiceImpl @Autowired constructor(
    private val authProjectApi: AuthProjectApi,
    private val authResourceApi: AuthResourceApi,
    private val authPermissionApi: AuthPermissionApi,
    private val projectAuthServiceCode: ProjectAuthServiceCode,
    private val projectDao: ProjectDao,
    private val dslContext: DSLContext
) : ProjectPermissionService {

    override fun verifyUserProjectPermission(accessToken: String?, projectCode: String, userId: String): Boolean {
        return authProjectApi.checkProjectUser(
            user = userId,
            serviceCode = projectAuthServiceCode,
            projectCode = projectCode
        )
    }

    // 创建项目
    override fun createResources(
        resourceRegisterInfo: ResourceRegisterInfo,
        authProjectCreateInfo: AuthProjectCreateInfo
    ): String {
        val validateCreatePermission = authPermissionApi.validateUserResourcePermission(
            user = authProjectCreateInfo.userId,
            serviceCode = projectAuthServiceCode,
            resourceType = AuthResourceType.PROJECT,
            projectCode = "",
            permission = AuthPermission.CREATE
        )
        if (!validateCreatePermission) {
            throw PermissionForbiddenException(
                I18nUtil.getCodeLanMessage(ProjectMessageCode.USER_NOT_CREATE_PERM)
            )
        }
        authResourceApi.createResource(
            user = authProjectCreateInfo.userId,
            serviceCode = projectAuthServiceCode,
            resourceType = AuthResourceType.PROJECT,
            projectCode = resourceRegisterInfo.resourceCode,
            resourceCode = resourceRegisterInfo.resourceCode,
            resourceName = resourceRegisterInfo.resourceName
        )
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
        val projects = authProjectApi.getUserProjects(
            serviceCode = projectAuthServiceCode,
            userId = userId,
            supplier = null
        )

        if (projects.isEmpty()) {
            return emptyList()
        }

        val projectList = mutableListOf<String>()
        return if (projects[0] == "*") {
            projectDao.getAllProject(dslContext).filter { projectList.add(it.englishName) }
            projectList
        } else {
            projects.map {
                projectList.add(it.trim())
            }
            projectList
        }
    }

    override fun getUserProjectsAvailable(userId: String): Map<String, String> {
        return authProjectApi.getUserProjectsAvailable(
            userId = userId,
            serviceCode = projectAuthServiceCode,
            supplier = null
        )
    }

    override fun verifyUserProjectPermission(
        accessToken: String?,
        projectCode: String,
        userId: String,
        permission: AuthPermission
    ): Boolean {
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = projectAuthServiceCode,
            resourceType = projectResourceType,
            resourceCode = projectCode,
            projectCode = projectCode,
            permission = permission
        )
    }

    override fun cancelCreateAuthProject(userId: String, projectCode: String) = Unit

    override fun cancelUpdateAuthProject(userId: String, projectCode: String) = Unit

    override fun needApproval(needApproval: Boolean?) = false

    override fun isShowUserManageIcon(): Boolean = false

    override fun filterProjects(userId: String, permission: AuthPermission): List<String>? = null

    companion object {
        private val projectResourceType = AuthResourceType.PROJECT
    }
}
