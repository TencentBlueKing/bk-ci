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
 *
 */

package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.pojo.vo.ProjectPermissionInfoVO
import com.tencent.devops.auth.service.iam.PermissionManageFacadeService
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

@Suppress("LongParameterList")
class RbacPermissionProjectService(
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val dslContext: DSLContext,
    private val permissionService: PermissionService,
    private val resourceGroupMemberService: RbacPermissionResourceMemberService,
    private val client: Client,
    private val resourceMemberService: PermissionResourceMemberService,
    private val permissionManageFacadeService: PermissionManageFacadeService
) : PermissionProjectService {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionProjectService::class.java)
        private const val expiredAt = 365L
    }

    override fun getProjectUsers(
        projectCode: String,
        group: BkAuthGroup?
    ): List<String> {
        return resourceGroupMemberService.getResourceGroupMembers(
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode,
            group = group
        )
    }

    override fun getProjectGroupAndUserList(projectCode: String): List<BkAuthGroupAndUserList> {
        return resourceGroupMemberService.getResourceGroupAndMembers(
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode
        )
    }

    override fun getUserProjects(userId: String): List<String> {
        logger.info("[rbac] get user projects|userId = $userId")
        return getUserProjectsByPermission(
            userId = userId,
            action = AuthPermission.VISIT.value
        )
    }

    override fun getUserProjectsByPermission(
        userId: String,
        action: String,
        resourceType: String?
    ): List<String> {
        return permissionService.getUserProjectsByPermission(
            userId = userId,
            action = action,
            resourceType = resourceType
        )
    }

    override fun isProjectUser(userId: String, projectCode: String, group: BkAuthGroup?): Boolean {
        logger.info("[rbac] check project user|userId = $userId")
        val startEpoch = System.currentTimeMillis()
        try {
            val managerPermission = checkProjectManager(userId, projectCode)
            val checkCiManager = group != null && (group == BkAuthGroup.MANAGER || group == BkAuthGroup.CI_MANAGER)
            // 有管理员权限或者若为校验管理员权限,直接返回是否时管理员成员
            if (managerPermission || checkCiManager) {
                return managerPermission
            }

            return permissionService.validateUserProjectPermission(
                userId = userId,
                projectCode = projectCode,
                permission = AuthPermission.VISIT
            )
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to check project user"
            )
        }
    }

    override fun isProjectMember(
        userId: String,
        projectCode: String
    ): Boolean {
        return permissionManageFacadeService.isProjectMember(
            projectCode = projectCode,
            userId = userId
        )
    }

    override fun checkUserInProjectLevelGroup(userId: String, projectCode: String): Boolean {
        // todo 下个迭代改回
//        return resourceGroupMemberService.getResourceGroupMembers(
//            projectCode = projectCode,
//            resourceType = AuthResourceType.PROJECT.value,
//            resourceCode = projectCode,
//            group = null
//        ).contains(userId)
        return permissionManageFacadeService.isProjectMember(
            projectCode = projectCode,
            userId = userId
        )
    }

    override fun checkProjectManager(userId: String, projectCode: String): Boolean {
        return permissionService.checkProjectManager(userId, projectCode)
    }

    override fun createProjectUser(userId: String, projectCode: String, roleCode: String): Boolean {
        return true
    }

    override fun batchCreateProjectUser(
        userId: String,
        projectCode: String,
        roleCode: String,
        members: List<String>
    ): Boolean {
        logger.info("batchCreateProjectUser:$userId|$projectCode|$roleCode|$members")
        // 根据roleCode获取到对应的iam组ID
        val iamGroupId = resourceGroupMemberService.roleCodeToIamGroupId(
            projectCode = projectCode,
            roleCode = roleCode,
            resourceCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value
        )
        logger.info("batch add project user:$userId|$projectCode|$roleCode|$members")
        val expiredTime = System.currentTimeMillis() / 1000 + TimeUnit.DAYS.toSeconds(expiredAt)
        resourceMemberService.batchAddResourceGroupMembers(
            projectCode = projectCode,
            iamGroupId = iamGroupId,
            expiredTime = expiredTime,
            members = members
        )
        return true
    }

    override fun getProjectRoles(projectCode: String, projectId: String): List<BKAuthProjectRolesResources> {
        return emptyList()
    }

    override fun getProjectPermissionInfo(
        projectCode: String
    ): ProjectPermissionInfoVO {
        val projectInfo = client.get(ServiceProjectResource::class).get(englishName = projectCode).data
            ?: throw ErrorCodeException(
                errorCode = AuthMessageCode.RESOURCE_NOT_FOUND,
                params = arrayOf(projectCode),
                defaultMessage = "project $projectCode not exist"
            )
        val projectGroupAndUserList = getProjectGroupAndUserList(projectCode)
        val managerGroupRelationId = authResourceGroupDao.get(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode,
            groupCode = BkAuthGroup.MANAGER.value
        )!!.relationId.toInt()

        val remotedevManager = projectInfo.properties?.remotedevManager?.split(";")
        val members = projectGroupAndUserList.flatMap { it.userIdList }.distinct()

        val owners = projectGroupAndUserList
            .find { it.roleId == managerGroupRelationId }?.userIdList ?: emptyList()
        return ProjectPermissionInfoVO(
            projectCode = projectCode,
            projectName = projectInfo.projectName,
            creator = projectInfo.creator!!,
            owners = remotedevManager?.plus(owners)?.distinct() ?: owners,
            members = remotedevManager?.plus(members)?.distinct() ?: members
        )
    }
}
