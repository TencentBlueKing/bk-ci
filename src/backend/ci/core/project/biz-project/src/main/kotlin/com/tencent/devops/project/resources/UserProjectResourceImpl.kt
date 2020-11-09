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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.project.resources

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.user.UserProjectResource
import com.tencent.devops.project.pojo.*
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import com.tencent.devops.project.service.ProjectService
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream

@RestResource
class UserProjectResourceImpl @Autowired constructor(
    private val projectService: ProjectService,
    private val authPermissionApi: AuthPermissionApi,
    private val projectAuthServiceCode: ProjectAuthServiceCode
) : UserProjectResource {

    override fun list(userId: String, accessToken: String?): Result<List<ProjectVO>> {
        return Result(projectService.list(userId, accessToken))
    }

    override fun get(projectId: String, accessToken: String?): Result<ProjectVO> {
        return Result(projectService.getByEnglishName(projectId, accessToken) ?: throw OperationException("项目不存在"))
    }

    override fun create(userId: String, projectCreateInfo: ProjectCreateInfo, accessToken: String?): Result<Boolean> {
        // 创建项目
        projectService.create(userId, projectCreateInfo, accessToken)

        return Result(true)
    }

    override fun update(
        userId: String,
        projectId: String,
        projectUpdateInfo: ProjectUpdateInfo,
        accessToken: String?
    ): Result<Boolean> {
        return Result(projectService.update(userId, projectId, projectUpdateInfo, accessToken))
    }

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
}
