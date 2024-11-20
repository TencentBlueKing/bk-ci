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
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.auth.api.ActionId.PROJECT_CREATE
import com.tencent.devops.common.auth.api.ActionId.PROJECT_EDIT
import com.tencent.devops.common.auth.api.ActionId.PROJECT_ENABLE
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.api.user.UserProjectResource
import com.tencent.devops.project.constant.ProjectMessageCode.PROJECT_NOT_EXIST
import com.tencent.devops.project.pojo.OperationalProductVO
import com.tencent.devops.project.pojo.ProjectByConditionDTO
import com.tencent.devops.project.pojo.ProjectCollation
import com.tencent.devops.project.pojo.ProjectCreateExtInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectDiffVO
import com.tencent.devops.project.pojo.ProjectLogo
import com.tencent.devops.project.pojo.ProjectSortType
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import com.tencent.devops.project.service.ProjectPermissionService
import com.tencent.devops.project.service.ProjectService
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream

@RestResource
class UserProjectResourceImpl @Autowired constructor(
    private val projectService: ProjectService,
    private val projectPermissionService: ProjectPermissionService
) : UserProjectResource {

    override fun list(
        userId: String,
        accessToken: String?,
        enabled: Boolean?,
        unApproved: Boolean?,
        sortType: ProjectSortType?,
        collation: ProjectCollation?
    ): Result<List<ProjectVO>> {
        return Result(
            projectService.list(
                userId = userId,
                accessToken = accessToken,
                enabled = enabled,
                unApproved = unApproved ?: false,
                sortType = sortType ?: ProjectSortType.PROJECT_NAME,
                collation = collation ?: ProjectCollation.DEFAULT
            )
        )
    }

    override fun listProjectsForApply(
        userId: String,
        accessToken: String?,
        projectName: String?,
        projectId: String?,
        page: Int,
        pageSize: Int
    ): Result<Pagination<ProjectByConditionDTO>> {
        return Result(
            projectService.listProjectsForApply(
                userId = userId,
                accessToken = accessToken,
                projectName = projectName,
                projectId = projectId,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun get(userId: String, projectId: String, accessToken: String?): Result<ProjectVO> {
        return Result(
            projectService.getByEnglishName(
                userId = userId,
                englishName = projectId,
                accessToken = accessToken
            )
                ?: throw OperationException("project $projectId not found")
        )
    }

    override fun show(userId: String, projectId: String, accessToken: String?): Result<ProjectVO> {
        return Result(
            projectService.show(
                userId = userId,
                englishName = projectId,
                accessToken = accessToken
            ) ?: throw OperationException("project $projectId not found")
        )
    }

    override fun diff(userId: String, projectId: String, accessToken: String?): Result<ProjectDiffVO> {
        return Result(
            projectService.diff(userId, projectId, accessToken)
                ?: throw OperationException(I18nUtil.getCodeLanMessage(PROJECT_NOT_EXIST))
        )
    }

    override fun getContainEmpty(userId: String, projectId: String, accessToken: String?): Result<ProjectVO?> {
        return Result(projectService.getByEnglishName(userId, projectId, accessToken))
    }

    @AuditEntry(actionId = PROJECT_CREATE)
    override fun create(userId: String, projectCreateInfo: ProjectCreateInfo, accessToken: String?): Result<Boolean> {
        // 创建项目
        projectService.create(
            userId = userId,
            projectCreateInfo = projectCreateInfo,
            accessToken = accessToken,
            createExtInfo = ProjectCreateExtInfo(needValidate = true, needAuth = true, needApproval = true),
            projectChannel = ProjectChannelCode.BS
        )

        return Result(true)
    }

    @AuditEntry(actionId = PROJECT_EDIT)
    override fun update(
        userId: String,
        projectId: String,
        projectUpdateInfo: ProjectUpdateInfo,
        accessToken: String?
    ): Result<Boolean> {
        return Result(
            projectService.update(
                userId = userId,
                englishName = projectId,
                projectUpdateInfo = projectUpdateInfo,
                accessToken = accessToken,
                needApproval = true
            )
        )
    }

    @AuditEntry(actionId = PROJECT_ENABLE)
    override fun enable(
        userId: String,
        projectId: String,
        enabled: Boolean
    ): Result<Boolean> {
        projectService.updateUsableStatus(userId, projectId, enabled)
        return Result(true)
    }

    override fun updateLogo(
        userId: String,
        englishName: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        accessToken: String?
    ): Result<ProjectLogo> {
        return projectService.updateLogo(userId, englishName, inputStream, disposition, accessToken)
    }

    override fun uploadLogo(
        userId: String,
        inputStream: InputStream,
        accessToken: String?
    ): Result<String> {
        return projectService.uploadLogo(userId, inputStream, accessToken)
    }

    override fun validate(
        userId: String,
        validateType: ProjectValidateType,
        name: String,
        projectId: String?
    ): Result<Boolean> {
        projectService.validate(validateType, name, projectId)
        return Result(true)
    }

    override fun hasCreatePermission(userId: String): Result<Boolean> {
        return Result(projectService.hasCreatePermission(userId))
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

    override fun cancelCreateProject(userId: String, projectId: String): Result<Boolean> {
        return Result(
            projectService.cancelCreateProject(
                userId = userId,
                projectId = projectId
            )
        )
    }

    override fun cancelUpdateProject(userId: String, projectId: String): Result<Boolean> {
        return Result(
            projectService.cancelUpdateProject(
                userId = userId,
                projectId = projectId
            )
        )
    }

    override fun getOperationalProducts(userId: String): Result<List<OperationalProductVO>> {
        return Result(
            projectService.getOperationalProducts()
        )
    }

    override fun remindUserOfRelatedProduct(userId: String, englishName: String): Result<Boolean> {
        return Result(
            projectService.remindUserOfRelatedProduct(
                userId = userId,
                englishName = englishName
            )
        )
    }

    override fun getOperationalProductsByBgName(userId: String, bgName: String): Result<List<OperationalProductVO>> {
        return Result(
            projectService.getOperationalProductsByBgName(bgName)
        )
    }

    override fun getPipelineDialect(userId: String, projectId: String): Result<String> {
        return Result(projectService.getPipelineDialect(projectId = projectId))
    }
}
