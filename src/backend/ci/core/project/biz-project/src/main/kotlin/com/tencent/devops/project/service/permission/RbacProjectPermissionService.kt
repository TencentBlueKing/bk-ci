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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.project.constant.ProjectMessageCode.APPROVAL_PROJECT_CANT_UPDATE
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.pojo.AuthProjectCreateInfo
import com.tencent.devops.project.pojo.ResourceUpdateInfo
import com.tencent.devops.project.pojo.enums.ProjectApproveStatus
import com.tencent.devops.project.pojo.enums.ProjectTipsStatus
import com.tencent.devops.project.service.ProjectApprovalService
import com.tencent.devops.project.service.ProjectExtService
import com.tencent.devops.project.service.ProjectPermissionService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Value

@Suppress("LongParameterList")
class RbacProjectPermissionService(
    private val authProjectApi: AuthProjectApi,
    private val authResourceApi: AuthResourceApi,
    private val authPermissionApi: AuthPermissionApi,
    private val projectAuthServiceCode: ProjectAuthServiceCode,
    private val projectApprovalService: ProjectApprovalService,
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao,
    private val projectExtService: ProjectExtService
) : ProjectPermissionService {

    // 项目是否需要开启审批
    @Value("\${auth.project.approval:#{false}}")
    private val authProjectApproval: Boolean = false

    override fun verifyUserProjectPermission(accessToken: String?, projectCode: String, userId: String): Boolean {
        return authProjectApi.checkProjectUser(
            user = userId,
            serviceCode = projectAuthServiceCode,
            projectCode = projectCode
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
            resourceType = AuthResourceType.PROJECT,
            resourceCode = projectCode,
            projectCode = projectCode,
            permission = permission
        )
    }

    override fun createResources(
        resourceRegisterInfo: ResourceRegisterInfo,
        authProjectCreateInfo: AuthProjectCreateInfo
    ): String {
        var authProjectId = ""
        with(authProjectCreateInfo) {
            val tipsStatus = if (approvalStatus == ProjectApproveStatus.CREATE_PENDING.status) {
                ProjectTipsStatus.SHOW_CREATE_PENDING.status
            } else {
                ProjectTipsStatus.SHOW_SUCCESSFUL_CREATE.status
            }
            projectApprovalService.create(
                userId = userId,
                projectCreateInfo = projectCreateInfo,
                approvalStatus = approvalStatus,
                subjectScopes = subjectScopes,
                tipsStatus = tipsStatus
            )
            if (approvalStatus == ProjectApproveStatus.APPROVED.status) {
                // 创建兼容的权限中心项目
                authProjectId = projectExtService.createOldAuthProject(
                    userId = userId,
                    accessToken = accessToken,
                    projectCreateInfo = projectCreateInfo
                ) ?: ""
            }
        }

        authResourceApi.createResource(
            user = authProjectCreateInfo.userId,
            serviceCode = projectAuthServiceCode,
            resourceType = AuthResourceType.PROJECT,
            projectCode = resourceRegisterInfo.resourceCode,
            resourceCode = resourceRegisterInfo.resourceCode,
            resourceName = resourceRegisterInfo.resourceName
        )
        return authProjectId
    }

    override fun deleteResource(projectCode: String) {
        projectApprovalService.delete(projectId = projectCode)
        authResourceApi.deleteResource(
            serviceCode = projectAuthServiceCode,
            resourceType = AuthResourceType.PROJECT,
            projectCode = projectCode,
            resourceCode = projectCode
        )
    }

    override fun modifyResource(resourceUpdateInfo: ResourceUpdateInfo) {
        val englishName = resourceUpdateInfo.projectUpdateInfo.englishName
        val projectInfo = projectDao.getByEnglishName(dslContext = dslContext, englishName = englishName)!!

        val approvalStatus = projectInfo.approvalStatus
        if (approvalStatus == ProjectApproveStatus.CREATE_PENDING.status ||
            approvalStatus == ProjectApproveStatus.UPDATE_PENDING.status
        ) {
            throw ErrorCodeException(
                errorCode = APPROVAL_PROJECT_CANT_UPDATE,
                params = arrayOf(englishName),
                defaultMessage = "Projects($englishName) in approval cannot be modified"
            )
        }
        with(resourceUpdateInfo) {
            projectApprovalService.update(
                userId = userId,
                projectUpdateInfo = projectUpdateInfo,
                approvalStatus = resourceUpdateInfo.approvalStatus,
                subjectScopes = subjectScopes
            )
        }

        // 如果创建时被拒绝,修改后再创建,需要重新发起创建申请单
        if (approvalStatus == ProjectApproveStatus.CREATE_REJECT.status) {
            authResourceApi.createResource(
                user = resourceUpdateInfo.userId,
                serviceCode = projectAuthServiceCode,
                resourceType = AuthResourceType.PROJECT,
                projectCode = englishName,
                resourceCode = englishName,
                resourceName = resourceUpdateInfo.projectUpdateInfo.projectName
            )
        } else {
            authResourceApi.modifyResource(
                serviceCode = projectAuthServiceCode,
                resourceType = AuthResourceType.PROJECT,
                projectCode = englishName,
                resourceCode = englishName,
                resourceName = resourceUpdateInfo.projectUpdateInfo.projectName
            )
        }
    }

    override fun getUserProjects(userId: String): List<String> {
        return authProjectApi.getUserProjects(
            serviceCode = projectAuthServiceCode,
            userId = userId,
            supplier = null
        )
    }

    override fun getUserProjectsAvailable(userId: String): Map<String, String> {
        return authProjectApi.getUserProjectsAvailable(
            userId = userId,
            serviceCode = projectAuthServiceCode,
            supplier = null
        )
    }

    override fun cancelCreateAuthProject(userId: String, projectCode: String) {
        projectApprovalService.delete(
            projectId = projectCode
        )
        authResourceApi.cancelCreateResource(
            userId = userId,
            serviceCode = projectAuthServiceCode,
            resourceType = AuthResourceType.PROJECT,
            projectCode = projectCode,
            resourceCode = projectCode
        )
    }

    override fun cancelUpdateAuthProject(userId: String, projectCode: String) {
        projectApprovalService.cancelUpdate(
            userId = userId,
            projectId = projectCode,
            approvalStatus = ProjectApproveStatus.APPROVED.status
        )
        authResourceApi.cancelUpdateResource(
            userId = userId,
            serviceCode = projectAuthServiceCode,
            resourceType = AuthResourceType.PROJECT,
            projectCode = projectCode,
            resourceCode = projectCode
        )
    }

    override fun needApproval(needApproval: Boolean?) = needApproval == true && authProjectApproval

    override fun isShowUserManageIcon(): Boolean = true

    override fun filterProjects(userId: String, permission: AuthPermission): List<String>? {
        return authProjectApi.getUserProjectsByPermission(
            serviceCode = projectAuthServiceCode,
            userId = userId,
            permission = permission,
            supplier = null
        )
    }
}
