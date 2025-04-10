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

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ActionId.PROJECT_CREATE
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.pojo.OrgInfo
import com.tencent.devops.project.pojo.ProjectBaseInfo
import com.tencent.devops.project.pojo.ProjectByConditionDTO
import com.tencent.devops.project.pojo.ProjectCreateExtInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import com.tencent.devops.project.pojo.ProjectOrganizationInfo
import com.tencent.devops.project.pojo.ProjectProperties
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.PluginDetailsDisplayOrder
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import com.tencent.devops.project.service.ProjectOrganizationService
import com.tencent.devops.project.service.ProjectPermissionService
import com.tencent.devops.project.service.ProjectService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceProjectResourceImpl @Autowired constructor(
    private val projectService: ProjectService,
    private val projectOrganizationService: ProjectOrganizationService,
    private val projectPermissionService: ProjectPermissionService
) : ServiceProjectResource {

    override fun getProjectByUser(userName: String): Result<List<ProjectVO>> {
        return Result(projectService.getProjectByUser(userName))
    }

    override fun verifyUserProjectPermission(
        accessToken: String?,
        projectCode: String,
        userId: String
    ): Result<Boolean> {
        return Result(
            projectPermissionService.verifyUserProjectPermission(
                accessToken = accessToken,
                projectCode = projectCode,
                userId = userId
            )
        )
    }

    override fun list(userId: String, productIds: String?): Result<List<ProjectVO>> {
        return Result(projectService.list(userId, productIds))
    }

    override fun getAllProject(): Result<List<ProjectVO>> {
        return Result(projectService.getAllProject())
    }

    override fun listProjectsByCondition(
        projectConditionDTO: ProjectConditionDTO,
        limit: Int,
        offset: Int
    ): Result<List<ProjectByConditionDTO>> {
        return Result(
            projectService.listProjectsByCondition(
                projectConditionDTO = projectConditionDTO,
                limit = limit,
                offset = offset
            )
        )
    }

    override fun listByProjectCode(projectCodes: Set<String>): Result<List<ProjectVO>> {
        return Result(projectService.list(projectCodes = projectCodes, enabled = true))
    }

    override fun listOnlyByProjectCode(projectCodes: Set<String>): Result<List<ProjectVO>> {
        return Result(projectService.listOnlyByProjectCode(projectCodes))
    }

    override fun listByProjectCodeList(projectCodes: List<String>): Result<List<ProjectVO>> {
        return Result(projectService.list(projectCodes))
    }

    override fun getNameByCode(projectCodes: String): Result<HashMap<String, String>> {
        return Result(projectService.getNameByCode(projectCodes))
    }

    override fun get(englishName: String): Result<ProjectVO?> {
        return Result(projectService.getByEnglishName(englishName))
    }

    @AuditEntry(actionId = PROJECT_CREATE)
    override fun create(
        userId: String,
        projectCreateInfo: ProjectCreateInfo,
        accessToken: String?
    ): Result<Boolean> {
        // 创建项目
        projectService.create(
            userId = userId,
            projectCreateInfo = projectCreateInfo,
            accessToken = accessToken,
            createExtInfo = ProjectCreateExtInfo(needAuth = true, needValidate = true),
            projectChannel = ProjectChannelCode.BS
        )

        return Result(true)
    }

    @AuditEntry(actionId = PROJECT_CREATE)
    override fun createExtSystem(
        userId: String,
        projectInfo: ProjectCreateInfo,
        needAuth: Boolean,
        needValidate: Boolean,
        channel: ProjectChannelCode
    ): Result<ProjectVO?> {
        return Result(
            projectService.createExtProject(
                userId = userId,
                projectCreateInfo = projectInfo,
                channel = channel,
                projectCode = projectInfo.englishName,
                needAuth = needAuth,
                needValidate = needValidate
            )
        )
    }

    @AuditEntry(actionId = ActionId.PROJECT_EDIT)
    override fun update(
        userId: String,
        projectId: String,
        projectUpdateInfo: ProjectUpdateInfo,
        accessToken: String?
    ): Result<Boolean> {
        return Result(projectService.update(userId, englishName = projectId, projectUpdateInfo, accessToken))
    }

    override fun updateProjectName(userId: String, projectCode: String, projectName: String): Result<Boolean> {
        return Result(
            projectService.updateProjectName(userId = userId, projectId = projectCode, projectName = projectName)
        )
    }

    override fun updateProjectProperties(projectCode: String, properties: ProjectProperties): Result<Boolean> {
        return Result(
            projectService.updateProjectProperties(
                projectCode = projectCode,
                properties = properties
            )
        )
    }

    override fun getProjectByName(userId: String, projectName: String): Result<ProjectVO?> {
        return Result(projectService.getProjectByName(projectName))
    }

    override fun validate(validateType: ProjectValidateType, name: String, projectId: String?): Result<Boolean> {
        projectService.validate(validateType, name, projectId)
        return Result(true)
    }

    override fun isOrgProject(projectId: String, orgInfos: OrgInfo): Result<Boolean> {
        return Result(projectOrganizationService.isOrgProject(projectId, orgInfos))
    }

    override fun getMinId(): Result<Long> {
        return Result(projectService.getMinId())
    }

    override fun getMaxId(): Result<Long> {
        return Result(projectService.getMaxId())
    }

    override fun getProjectListById(minId: Long, maxId: Long): Result<List<ProjectBaseInfo>> {
        return Result(projectService.getProjectListById(minId, maxId))
    }

    override fun listSecrecyProject(): Result<Set<String>?> {
        return Result(projectService.listSecrecyProject())
    }

    override fun createProjectUser(projectId: String, createInfo: ProjectCreateUserInfo): Result<Boolean> {
        return Result(projectService.createProjectUser(projectId, createInfo))
    }

    override fun hasPermission(userId: String, projectId: String, permission: AuthPermission): Result<Boolean> {
        return Result(
            projectService.verifyUserProjectPermission(
                accessToken = null,
                userId = userId,
                projectId = projectId,
                permission = permission
            )
        )
    }

    override fun updateProjectSubjectScopes(
        projectId: String,
        subjectScopes: List<SubjectScopeInfo>
    ): Result<Boolean> {
        return Result(
            projectService.updateProjectSubjectScopes(
                projectId = projectId,
                subjectScopes = subjectScopes
            )
        )
    }

    override fun updateProjectProductId(
        projectCode: String,
        productName: String?,
        productId: Int?
    ): Result<Boolean> {
        projectService.updateProjectProductId(
            englishName = projectCode,
            productName = productName,
            productId = productId
        )
        return Result(true)
    }

    override fun updateOrganizationByEnglishName(
        projectCode: String,
        projectOrganizationInfo: ProjectOrganizationInfo
    ): Result<Boolean> {
        if (projectOrganizationInfo.bgId == null || projectOrganizationInfo.bgName.isNullOrBlank()) {
            throw ParamBlankException("bgId or bgName must be not null.")
        }
        projectService.updateOrganizationByEnglishName(
            englishName = projectCode,
            projectOrganizationInfo = projectOrganizationInfo
        )
        return Result(true)
    }

    override fun getProjectListByProductId(productId: Int): Result<List<ProjectBaseInfo>> {
        return Result(
            projectService.getProjectListByProductId(
                productId = productId
            )
        )
    }

    override fun getExistedEnglishName(englishName: List<String>): Result<List<String>?> {
        return Result(
            projectService.getExistedEnglishName(englishName)
        )
    }

    override fun updatePluginDetailsDisplay(
        projectId: String,
        pluginDetailsDisplayOrder: List<PluginDetailsDisplayOrder>
    ): Result<Boolean> {
        return Result(
            projectService.updatePluginDetailsDisplay(
                englishName = projectId,
                pluginDetailsDisplayOrder = pluginDetailsDisplayOrder
            )
        )
    }
}
