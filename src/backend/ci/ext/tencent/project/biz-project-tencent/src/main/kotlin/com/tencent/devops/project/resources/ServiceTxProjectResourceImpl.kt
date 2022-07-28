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

package com.tencent.devops.project.resources

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.pojo.PipelinePermissionInfo
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.project.pojo.AddManagerRequest
import com.tencent.devops.project.pojo.ProjectCreateExtInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectCreateUserDTO
import com.tencent.devops.project.pojo.ProjectDeptInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import com.tencent.devops.project.service.ProjectLocalService
import com.tencent.devops.project.service.ProjectMemberService
import com.tencent.devops.project.service.ProjectService
import com.tencent.devops.project.service.ProjectTagService
import com.tencent.devops.project.service.ProjectExtPermissionService
import com.tencent.devops.project.service.ProjectTxInfoService
import com.tencent.devops.project.util.ProjectUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@RestResource
class ServiceTxProjectResourceImpl @Autowired constructor(
    private val bsAuthPermissionApi: AuthPermissionApi,
    private val projectExtPermissionService: ProjectExtPermissionService,
    private val projectLocalService: ProjectLocalService,
    private val projectService: ProjectService,
    private val projectMemberService: ProjectMemberService,
    private val projectTagService: ProjectTagService,
    private val projectTxService: ProjectTxInfoService
) : ServiceTxProjectResource {

    @Value("\${tag.auto:#{null}}")
    val autoTag: String? = null

    override fun addManagerForProject(userId: String, addManagerRequest: AddManagerRequest): Result<Boolean> {
        return Result(
            bsAuthPermissionApi.addResourcePermissionForUsers(
                userId = userId,
                projectCode = addManagerRequest.projectCode,
                serviceCode = BSPipelineAuthServiceCode(),
                permission = AuthPermission.MANAGE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT,
                resourceCode = "*",
                userIdList = addManagerRequest.managerList,
                supplier = null
            )
        )
    }

    override fun getProjectEnNamesByCenterId(
        userId: String,
        centerId: Long?
    ): Result<List<String>> {
        return Result(
            projectLocalService.getProjectEnNamesByCenterId(
                userId = userId,
                centerId = centerId,
                interfaceName = "/service/projects/enNames/center"
            )
        )
    }

    override fun getProjectEnNamesByDeptIdAndCenterName(
        userId: String,
        deptId: Long?,
        centerName: String?
    ): Result<List<String>> {
        return Result(
            projectLocalService.getProjectEnNamesByOrganization(
                userId = userId,
                deptId = deptId,
                centerName = centerName,
                interfaceName = "/service/projects/enNames/dept"
            )
        )
    }

    override fun getProjectEnNamesByOrganization(
        userId: String,
        bgId: Long,
        deptName: String?,
        centerName: String?
    ): Result<List<String>> {
        return Result(
            projectLocalService.getProjectEnNamesByOrganization(
                userId = userId,
                bgId = bgId,
                deptName = deptName,
                centerName = centerName,
                interfaceName = "/service/projects/enNames/organization"
            )
        )
    }

    override fun getProjectByGroup(
        userId: String,
        bgName: String?,
        deptName: String?,
        centerName: String?
    ): Result<List<ProjectVO>> {
        return Result(
            projectLocalService.getProjectByGroup(
                userId = userId,
                bgName = bgName,
                deptName = deptName,
                centerName = centerName
            )
        )
    }

    override fun getProjectByName(
        userId: String,
        organizationType: String,
        organizationId: Long,
        deptName: String?,
        centerName: String?
    ): Result<List<ProjectVO>> {
        return Result(
            projectLocalService.getProjectByOrganizationId(
                userId = userId,
                organizationType = organizationType,
                organizationId = organizationId,
                deptName = deptName,
                centerName = centerName,
                interfaceName = "/service/project/tx/getProjectByOrganizationId"
            )
        )
    }

    override fun getProjectByName(
        userId: String,
        organizationType: String,
        organizationId: Long,
        name: String,
        nameType: ProjectValidateType,
        showSecrecy: Boolean?
    ): Result<ProjectVO?> {
        return Result(
            projectLocalService.getByName(
                name = name,
                nameType = nameType,
                organizationId = organizationId,
                organizationType = organizationType,
                showSecrecy = showSecrecy
            )
        )
    }

    override fun getProjectByGroupId(
        userId: String,
        bgId: Long?,
        deptId: Long?,
        centerId: Long?
    ): Result<List<ProjectVO>> {
        return Result(
            projectLocalService.getProjectByGroupId(
                userId = userId,
                bgId = bgId,
                deptId = deptId,
                centerId = centerId
            )
        )
    }

    override fun list(accessToken: String): Result<List<ProjectVO>> {
        return Result(projectService.list("", accessToken))
    }

    override fun getPreUserProject(userId: String, accessToken: String): Result<ProjectVO?> {
        return Result(projectLocalService.getOrCreatePreProject(userId = userId, accessToken = accessToken))
    }

    override fun getOrCreateRdsProject(userId: String, projectId: String, projectName: String): Result<ProjectVO?> {
        return Result(projectLocalService.getOrCreateRdsProject(userId, projectId, projectName))
    }

    override fun create(
        userId: String,
        accessToken: String,
        projectCreateInfo: ProjectCreateInfo,
        routerTag: String?
    ): Result<String> {
        var channelCode = ProjectChannelCode.BS
        val createExtInfo =
            if (!routerTag.isNullOrEmpty() && !autoTag.isNullOrEmpty() && routerTag == autoTag) {
                channelCode = ProjectChannelCode.AUTO
                logger.info("create $userId ${projectCreateInfo.englishName} $routerTag")
                ProjectCreateExtInfo(needAuth = false, needValidate = true)
            } else {
                ProjectCreateExtInfo(needAuth = true, needValidate = true)
            }

        val createResult = projectService.create(
            userId = userId,
            accessToken = accessToken,
            projectCreateInfo = projectCreateInfo,
            createExtInfo = createExtInfo,
            projectChannel = channelCode
        )
        if (channelCode == ProjectChannelCode.AUTO) {
            projectTagService.updateTagByProject(projectCode = projectCreateInfo.englishName, tag = autoTag)
        }
        return Result(createResult)
    }

    override fun getProjectManagers(
        projectCode: String
    ): Result<List<String>> {
        return Result(projectMemberService.getProjectManagers(projectCode))
    }

    override fun verifyUserProjectPermission(
        accessToken: String,
        projectCode: String,
        userId: String
    ): Result<Boolean> {
        return Result(
            projectExtPermissionService.verifyUserProjectPermission(
                accessToken = accessToken,
                projectCode = projectCode,
                userId = userId
            )
        )
    }

    override fun verifyProjectByOrganization(
        projectCode: String,
        organizationType: String,
        organizationId: Int
    ): Result<Boolean> {
        val projectInfo = projectLocalService.getByEnglishName(projectCode)
        val organizationIdString = organizationId.toString()
        return if (projectInfo != null) {
            val result = when (organizationType) {
                AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG -> projectInfo.bgId == organizationIdString
                AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT -> projectInfo.deptId == organizationIdString
                AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER -> projectInfo.centerId == organizationIdString
                else -> projectInfo.bgId == organizationIdString
            }
            Result(result)
        } else {
            Result(false)
        }
    }

    override fun createGitCIProject(gitProjectId: Long, userId: String, gitProjectName: String?): Result<ProjectVO> {
        return Result(projectLocalService.createGitCIProject(userId, gitProjectId, gitProjectName))
    }

    override fun createProjectUser(
        createUser: String?,
        checkManager: Boolean,
        createInfo: ProjectCreateUserDTO
    ): Result<Boolean> {
        val userIds = mutableSetOf<String>()
        if (!createInfo.userIds.isNullOrEmpty()) {
            userIds.addAll(createInfo.userIds!!)
        }
        if (!createInfo.userId.isNullOrEmpty()) {
            userIds.add(createInfo.userId!!)
        }
        return Result(
            projectExtPermissionService.createUser2Project(
                createUser = createUser ?: "",
                userIds = userIds.toList(),
                projectCode = createInfo.projectId,
                roleId = createInfo.roleId,
                roleName = createInfo.roleName,
                checkManager = checkManager
            )
        )
    }

    override fun createPipelinePermission(
        createUser: String?,
        checkManager: Boolean,
        createInfo: PipelinePermissionInfo
    ): Result<Boolean> {
        return Result(
            projectLocalService.grantInstancePermission(
                userId = createUser ?: "",
                projectId = createInfo.projectId,
                resourceType = createInfo.resourceType,
                resourceCode = createInfo.resourceTypeCode,
                permission = createInfo.permission,
                createUserList = arrayListOf(createInfo.userId),
                checkManager = checkManager
            )
        )
    }

    override fun getProjectRoles(
        projectCode: String
    ): Result<List<BKAuthProjectRolesResources>> {
        return Result(
            projectLocalService.getProjectRole(
                projectId = projectCode
            )
        )
    }

    override fun bindRelationSystem(projectCode: String, relationId: String): Result<Boolean> {
        projectLocalService.updateRelationId(projectCode, relationId)
        return Result(true)
    }

    override fun updateProjectName(userId: String, projectCode: String, projectName: String): Result<Boolean> {
        return Result(projectTxService.updateProjectName(userId, projectCode, projectName))
    }

    override fun getProjectInfoByProjectName(userId: String, projectName: String): Result<ProjectVO>? {

        val tProjectRecord = projectTxService.getProjectInfoByProjectName(
            userId = userId,
            projectName = projectName
        ) ?: return null

        return Result(ProjectUtils.packagingBean(tProjectRecord))
    }

    override fun bindProjectOrganization(
        userId: String,
        projectCode: String,
        projectDeptInfo: ProjectDeptInfo
    ): Result<Boolean> {
        return Result(
            projectTxService.bindProjectDept(
                userId = userId,
                projectCode = projectCode,
                projectDeptInfo = projectDeptInfo
            )
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(ServiceTxProjectResourceImpl::class.java)
    }
}
