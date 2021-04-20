/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.repository.controller.user

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.pojo.project.ProjectCreateRequest
import com.tencent.bkrepo.repository.pojo.project.ProjectInfo
import com.tencent.bkrepo.repository.pojo.project.UserProjectCreateRequest
import com.tencent.bkrepo.repository.service.repo.ProjectService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Api("项目用户接口")
@RestController
@RequestMapping("/api/project")
class UserProjectController(
    private val permissionManager: PermissionManager,
    private val projectService: ProjectService
) {
    @ApiOperation("创建项目")
    @Principal(PrincipalType.PLATFORM)
    @PostMapping("/create")
    fun createProject(
        @RequestAttribute userId: String,
        @RequestBody userProjectRequest: UserProjectCreateRequest
    ): Response<Void> {
        val createRequest = with(userProjectRequest) {
            ProjectCreateRequest(
                name = name,
                displayName = displayName,
                description = description,
                operator = userId
            )
        }
        projectService.createProject(createRequest)
        return ResponseBuilder.success()
    }

    @ApiOperation("查询项目是否存在")
    @GetMapping("/exist/{projectId}")
    fun checkExist(
        @RequestAttribute userId: String,
        @ApiParam(value = "项目ID", required = true)
        @PathVariable projectId: String
    ): Response<Boolean> {
        permissionManager.checkProjectPermission(PermissionAction.READ, projectId)
        return ResponseBuilder.success(projectService.checkExist(projectId))
    }

    @ApiOperation("项目列表")
    @Principal(PrincipalType.PLATFORM)
    @GetMapping("/list")
    fun listProject(): Response<List<ProjectInfo>> {
        return ResponseBuilder.success(projectService.listProject())
    }

    @Deprecated("waiting kb-ci", replaceWith = ReplaceWith("createProject"))
    @ApiOperation("创建项目")
    @Principal(PrincipalType.PLATFORM)
    @PostMapping
    fun create(
        @RequestAttribute userId: String,
        @RequestBody userProjectRequest: UserProjectCreateRequest
    ): Response<Void> {
        return this.createProject(userId, userProjectRequest)
    }
}
