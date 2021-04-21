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

package com.tencent.devops.project.service.iam

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.BSAuthProjectApi
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.service.ProjectLocalService
import com.tencent.devops.project.service.tof.TOFService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectIamV0Service @Autowired constructor(
    private val bkAuthProjectApi: BSAuthProjectApi,
    private val bkAuthPermissionApi: AuthPermissionApi,
    private val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
    private val projectLocalService: ProjectLocalService,
    private val tofService: TOFService,
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao
) {

    fun createUser2Project(
        createUser: String,
        userIds: List<String>,
        projectCode: String,
        roleId: Int?,
        roleName: String?
    ): Boolean {
        ProjectLocalService.logger.info("[createUser2Project] createUser[$createUser] userId[$userIds] projectCode[$projectCode]")

        if (!bkAuthProjectApi.isProjectUser(createUser, bsPipelineAuthServiceCode, projectCode, BkAuthGroup.MANAGER)) {
            ProjectLocalService.logger.error("$createUser is not manager for project[$projectCode]")
            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.NOT_MANAGER))
        }
        return createUser2ProjectImpl(
            userIds = userIds,
            projectId = projectCode,
            roleId = roleId,
            roleName = roleName
        )
    }

    fun createUser2ProjectByApp(
        organizationType: String,
        organizationId: Long,
        userId: String,
        projectCode: String,
        roleId: Int?,
        roleName: String?
    ): Boolean {
        ProjectLocalService.logger.info("[createUser2ProjectByApp] organizationType[$organizationType], organizationId[$organizationId] userId[$userId] projectCode[$projectCode]")
        var bgId: Long? = null
        var deptId: Long? = null
        var centerId: Long? = null
        when (organizationType) {
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG -> bgId = organizationId
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT -> deptId = organizationId
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER -> centerId = organizationId
            else -> {
                throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.ORG_TYPE_ERROR)))
            }
        }
        val projectList = projectLocalService.getProjectByGroupId(
            userId = userId,
            bgId = bgId,
            deptId = deptId,
            centerId = centerId
        )
        if (projectList.isEmpty()) {
            ProjectLocalService.logger.error("organizationType[$organizationType] :organizationId[$organizationId]  not project[$projectCode] permission ")
            throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.ORG_NOT_PROJECT)))
        }

        var isCreate = false
        projectList.forEach { project ->
            if (project.projectCode.equals(projectCode)) {
                isCreate = true
                return@forEach
            }
        }
        if (isCreate) {
            return createUser2ProjectImpl(
                userIds = arrayListOf(userId),
                projectId = projectCode,
                roleId = roleId,
                roleName = roleName
            )
        } else {
            ProjectLocalService.logger.error("organizationType[$organizationType] :organizationId[$organizationId]  not project[$projectCode] permission ")
            throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.ORG_NOT_PROJECT)))
        }
    }

    fun createPipelinePermission(
        createUser: String,
        projectId: String,
        userId: String,
        permission: String,
        resourceType: String,
        resourceTypeCode: String
    ): Boolean {
        ProjectLocalService.logger.info("createPipelinePermission createUser[$createUser] projectId[$projectId] userId[$userId] permissionList[$permission]")
        if (!bkAuthProjectApi.isProjectUser(createUser, bsPipelineAuthServiceCode, projectId, BkAuthGroup.MANAGER)) {
            ProjectLocalService.logger.info("createPipelinePermission createUser is not project manager,createUser[$createUser] projectId[$projectId]")
            throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.NOT_MANAGER)))
        }
        val createUserList = userId.split(",")

        createUserList?.forEach {
            if (!bkAuthProjectApi.isProjectUser(it, bsPipelineAuthServiceCode, projectId, null)) {
                ProjectLocalService.logger.info("createPipelinePermission userId is not project manager,userId[$userId] projectId[$projectId]")
                throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.USER_NOT_PROJECT_USER)))
            }
        }

        return createPermission(
            userId = userId,
            projectId = projectId,
            permission = permission,
            resourceType = resourceType,
            authServiceCode = bsPipelineAuthServiceCode,
            resourceTypeCode = resourceTypeCode,
            userList = createUserList
        )
    }

    fun createPipelinePermissionByApp(
        organizationType: String,
        organizationId: Long,
        userId: String,
        projectId: String,
        permission: String,
        resourceType: String,
        resourceTypeCode: String
    ): Boolean {
        ProjectLocalService.logger.info("[createPipelinePermissionByApp] organizationType[$organizationType], organizationId[$organizationId] userId[$userId] projectCode[$projectId], permission[$permission], resourceType[$resourceType],resourceTypeCode[$resourceTypeCode]")
        val projectList = getProjectListByOrg(userId, organizationType, organizationId)
        if (projectList.isEmpty()) {
            ProjectLocalService.logger.error("organizationType[$organizationType] :organizationId[$organizationId]  not project[$projectId] permission ")
            throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.ORG_NOT_PROJECT)))
        }
        var isCreate = false
        projectList.forEach { project ->
            if (project.projectCode == projectId) {
                isCreate = true
                return@forEach
            }
        }
        if (!isCreate) {
            throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.USER_NOT_PROJECT_USER)))
        }
        val createUserList = userId.split(",")

        createUserList?.forEach {
            if (!bkAuthProjectApi.isProjectUser(it, bsPipelineAuthServiceCode, projectId, null)) {
                ProjectLocalService.logger.error("createPipelinePermission userId is not project user,userId[$it] projectId[$projectId]")
                throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.USER_NOT_PROJECT_USER)))
            }
        }

        // TODO:此处bsPipelineAuthServiceCode 也需写成配置化
        return createPermission(
            userId = userId,
            projectId = projectId,
            permission = permission,
            resourceType = resourceType,
            authServiceCode = bsPipelineAuthServiceCode,
            resourceTypeCode = resourceTypeCode,
            userList = createUserList
        )
    }

    fun getProjectRole(
        organizationType: String,
        organizationId: Long,
        projectId: String
    ): List<BKAuthProjectRolesResources> {
        ProjectLocalService.logger.info("[getProjectRole] organizationType[$organizationType], organizationId[$organizationId] projectCode[$projectId]")
        val projectList = getProjectListByOrg("", organizationType, organizationId)
        if (projectList.isEmpty()) {
            ProjectLocalService.logger.error("organizationType[$organizationType] :organizationId[$organizationId]  not project[$projectId] permission ")
            throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.ORG_NOT_PROJECT)))
        }
        if (projectList.isEmpty()) {
            ProjectLocalService.logger.error("organizationType[$organizationType] :organizationId[$organizationId]  not project[$projectId] permission ")
            throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.ORG_NOT_PROJECT)))
        }
        var queryProject: ProjectVO? = null
        projectList.forEach { project ->
            if (project.projectCode == projectId) {
                queryProject = project
                return@forEach
            }
        }
        var roles = mutableListOf<BKAuthProjectRolesResources>()
        if (queryProject != null) {
            roles = bkAuthProjectApi.getProjectRoles(
                bsPipelineAuthServiceCode,
                queryProject!!.englishName,
                queryProject!!.projectId
            ).toMutableList()
        }
        return roles
    }

    private fun createPermission(
        userId: String,
        userList: List<String>?,
        projectId: String,
        permission: String,
        resourceType: String,
        authServiceCode: AuthServiceCode,
        resourceTypeCode: String
    ): Boolean {
        projectDao.getByEnglishName(dslContext, projectId)
            ?: throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NOT_EXIST))

        val authPermission = AuthPermission.get(permission)
        val authResourceType = AuthResourceType.get(resourceType)

        return bkAuthPermissionApi.addResourcePermissionForUsers(
            userId = userId,
            projectCode = projectId,
            permission = authPermission,
            serviceCode = authServiceCode,
            resourceType = authResourceType,
            resourceCode = resourceTypeCode,
            userIdList = userList ?: emptyList(),
            supplier = null
        )
    }

    private fun createUser2ProjectImpl(
        userIds: List<String>,
        projectId: String,
        roleId: Int?,
        roleName: String?
    ): Boolean {
        ProjectLocalService.logger.info("[createUser2Project]  userId[$userIds] projectCode[$projectId], roleId[$roleId], roleName[$roleName]")
        val projectInfo = projectDao.getByEnglishName(dslContext, projectId) ?: throw RuntimeException()
        val roleList = bkAuthProjectApi.getProjectRoles(bsPipelineAuthServiceCode, projectId, projectInfo.englishName)
        var authRoleId: String? = BkAuthGroup.DEVELOPER.value
        roleList.forEach {
            if (roleId == null && roleName.isNullOrEmpty()) {
                if (it.roleName == BkAuthGroup.DEVELOPER.value) {
                    authRoleId = it.roleId.toString()
                    return@forEach
                }
            }
            if (roleId != null) {
                if (it.roleId == roleId) {
                    authRoleId = it.roleId.toString()
                    return@forEach
                }
            }
            if (roleName != null) {
                if (it.roleName == roleName) {
                    authRoleId = it.roleId.toString()
                    return@forEach
                }
            }
        }
        userIds.forEach {
            try {
                tofService.getStaffInfo(it)
                bkAuthProjectApi.createProjectUser(
                    user = it,
                    serviceCode = bsPipelineAuthServiceCode,
                    projectCode = projectInfo.projectId,
                    role = authRoleId!!
                )
            } catch (ope: OperationException) {
                throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.QUERY_USER_INFO_FAIL))
            } catch (e: Exception) {
                ProjectLocalService.logger.warn("createUser2Project fail, userId[$it]", e)
                return false
            }
        }
        return true
    }

    private fun getProjectListByOrg(userId: String, organizationType: String, organizationId: Long): List<ProjectVO> {
        var bgId: Long? = null
        var deptId: Long? = null
        var centerId: Long? = null
        when (organizationType) {
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG -> bgId = organizationId
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT -> deptId = organizationId
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER -> centerId = organizationId
            else -> {
                throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.ORG_TYPE_ERROR)))
            }
        }
        return projectLocalService.getProjectByGroupId(userId, bgId, deptId, centerId)
    }
}
