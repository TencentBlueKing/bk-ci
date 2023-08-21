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

package com.tencent.devops.project.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.MigrateProjectConditionDTO
import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.project.pojo.ProjectBaseInfo
import com.tencent.devops.project.pojo.ProjectCreateExtInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import com.tencent.devops.project.pojo.ProjectDiffVO
import com.tencent.devops.project.pojo.ProjectLogo
import com.tencent.devops.project.pojo.ProjectProperties
import com.tencent.devops.project.pojo.ProjectUpdateCreatorDTO
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.ProjectWithPermission
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import java.io.InputStream

@Suppress("ALL")
interface ProjectService {

    /**
     * 校验项目名称/英文名称是否合法
     */
    fun validate(validateType: ProjectValidateType, name: String, projectId: String? = null)

    /**
     * 创建项目信息
     */
    fun create(
        userId: String,
        projectCreateInfo: ProjectCreateInfo,
        accessToken: String?,
        createExtInfo: ProjectCreateExtInfo,
        defaultProjectId: String? = null,
        projectChannel: ProjectChannelCode
    ): String

    fun createExtProject(
        userId: String,
        projectCode: String,
        projectCreateInfo: ProjectCreateInfo,
        needAuth: Boolean,
        needValidate: Boolean,
        channel: ProjectChannelCode
    ): ProjectVO?

    /**
     * 根据项目ID/英文ID获取项目信息对象
     * @param englishName projectCode 英文ID
     * @param needTips 前端是否需要tips弹框,目前只有项目详情页需要
     * @return ProjectVO 如果没有则为null
     */
    fun getByEnglishName(
        userId: String,
        englishName: String,
        accessToken: String?
    ): ProjectVO?

    /**
     * 根据项目ID/英文ID获取项目信息对象
     * @param englishName projectCode 英文ID
     * @return ProjectVO 如果没有则为null
     */
    fun show(
        userId: String,
        englishName: String,
        accessToken: String?
    ): ProjectVO?

    /**
     * 根据项目ID/英文ID获取项目审批中对比信息
     * @param englishName projectCode 英文ID
     * @return ProjectDiffVO 如果没有则为null
     */
    fun diff(userId: String, englishName: String, accessToken: String?): ProjectDiffVO?

    /**
     * 根据项目ID/英文ID获取项目信息对象
     * @param englishName projectCode 英文ID
     * @return ProjectVO 如果没有则为null
     */
    fun getByEnglishName(englishName: String): ProjectVO?

    /**
     * 修改项目信息 [englishName]是项目英文名，目前平台在api接口上会把他命名成projectId，实际上与t_project表中的project_id字段不同
     * 后续会统一
     */
    fun update(
        userId: String,
        englishName: String,
        projectUpdateInfo: ProjectUpdateInfo,
        accessToken: String?,
        needApproval: Boolean? = false
    ): Boolean

    /**
     * 更新Logo
     */
    fun updateLogo(
        userId: String,
        englishName: String, /* englishName is projectId */
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        accessToken: String?
    ): Result<ProjectLogo>

    /**
     * 上传Logo
     */
    fun uploadLogo(
        userId: String,
        inputStream: InputStream,
        accessToken: String?
    ): Result<String>

    fun updateProjectName(userId: String, projectId: String/* projectId is englishName */, projectName: String): Boolean

    /**
     * 获取所有项目信息
     */
    fun list(
        userId: String,
        accessToken: String?,
        enabled: Boolean? = null,
        unApproved: Boolean
    ): List<ProjectVO>

    fun listProjectsForApply(
        userId: String,
        accessToken: String?,
        projectName: String?,
        projectId: String?,
        page: Int,
        pageSize: Int
    ): Pagination<ProjectWithPermission>

    fun list(userId: String): List<ProjectVO>

    fun list(projectCodes: Set<String>): List<ProjectVO>

    fun listOnlyByProjectCode(projectCodes: Set<String>): List<ProjectVO>

    fun list(projectCodes: List<String>): List<ProjectVO>

    fun list(limit: Int, offset: Int): Page<ProjectVO>

    fun listByChannel(limit: Int, offset: Int, projectChannelCode: ProjectChannelCode): Page<ProjectVO>

    fun getAllProject(): List<ProjectVO>

    fun listMigrateProjects(
        migrateProjectConditionDTO: MigrateProjectConditionDTO,
        limit: Int,
        offset: Int
    ): List<ProjectWithPermission>

    /**
     * 获取用户已的可访问项目列表=
     */
    fun getProjectByUser(userName: String): List<ProjectVO>

    fun getNameByCode(projectCodes: String): HashMap<String, String>

    fun updateUsableStatus(userId: String, englishName: String /* englishName is projectId */, enabled: Boolean)

    fun searchProjectByProjectName(projectName: String, limit: Int, offset: Int): Page<ProjectVO>

    fun hasCreatePermission(userId: String): Boolean

    fun getMinId(): Long

    fun getMaxId(): Long

    fun getProjectListById(minId: Long, maxId: Long): List<ProjectBaseInfo>

    fun verifyUserProjectPermission(
        userId: String,
        projectId: String,
        permission: AuthPermission,
        accessToken: String?
    ): Boolean

    fun listSecrecyProject(): Set<String>?

    fun createProjectUser(projectId: String, createInfo: ProjectCreateUserInfo): Boolean

    fun relationIamProject(projectCode: String, relationId: String): Boolean

    fun getProjectByName(projectName: String): ProjectVO?

    fun updateProjectProperties(userId: String, projectCode: String, properties: ProjectProperties): Boolean

    fun cancelCreateProject(userId: String, projectId: String): Boolean

    fun cancelUpdateProject(userId: String, projectId: String): Boolean

    fun isRbacPermission(projectId: String): Boolean

    fun updateProjectSubjectScopes(
        projectId: String,
        subjectScopes: List<SubjectScopeInfo>
    ): Boolean

    fun updateProjectCreator(projectUpdateCreatorDtoList: List<ProjectUpdateCreatorDTO>): Boolean
}
