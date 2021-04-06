/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoUpdateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import com.tencent.bkrepo.repository.pojo.repo.UserRepoCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.UserRepoUpdateRequest
import com.tencent.bkrepo.repository.service.RepositoryService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Api("仓库用户接口")
@RestController
@RequestMapping("/api/repo")
class UserRepositoryController(
    private val permissionManager: PermissionManager,
    private val repositoryService: RepositoryService
) {

    @ApiOperation("根据名称类型查询仓库")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    @GetMapping("/info/{projectId}/{repoName}", "/info/{projectId}/{repoName}/{type}")
    fun getRepoInfo(
        @ApiParam(value = "所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "仓库名称", required = true)
        @PathVariable repoName: String,
        @ApiParam(value = "仓库类型", required = true)
        @PathVariable type: String? = null
    ): Response<RepositoryInfo?> {
        return ResponseBuilder.success(repositoryService.getRepoInfo(projectId, repoName))
    }

    @ApiOperation("根据名称查询仓库是否存在")
    @GetMapping("/exist/{projectId}/{repoName}")
    fun checkExist(
        @ApiParam(value = "所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "仓库名称", required = true)
        @PathVariable repoName: String
    ): Response<Boolean> {
        return ResponseBuilder.success(repositoryService.checkExist(projectId, repoName))
    }

    @ApiOperation("创建仓库")
    @PostMapping("/create")
    fun createRepo(
        @RequestAttribute userId: String,
        @RequestBody userRepoCreateRequest: UserRepoCreateRequest
    ): Response<Void> {
        val createRequest = with(userRepoCreateRequest) {
            permissionManager.checkProjectPermission(PermissionAction.MANAGE, projectId)
            RepoCreateRequest(
                projectId = projectId,
                name = name,
                type = type,
                category = category,
                public = public,
                description = description,
                configuration = configuration,
                storageCredentialsKey = storageCredentialsKey,
                operator = userId
            )
        }
        repositoryService.createRepo(createRequest)
        return ResponseBuilder.success()
    }

    @ApiOperation("列表查询项目所有仓库")
    @GetMapping("/list/{projectId}")
    fun listRepo(
        @RequestAttribute userId: String,
        @ApiParam(value = "项目id", required = true)
        @PathVariable projectId: String,
        @ApiParam("仓库名称", required = false)
        @RequestParam name: String? = null,
        @ApiParam("仓库类型", required = false)
        @RequestParam type: String? = null
    ): Response<List<RepositoryInfo>> {
        permissionManager.checkProjectPermission(PermissionAction.READ, projectId)
        return ResponseBuilder.success(repositoryService.listRepo(projectId, name, type))
    }

    @ApiOperation("分页查询仓库列表")
    @GetMapping("/page/{projectId}/{pageNumber}/{pageSize}")
    fun listRepoPage(
        @ApiParam(value = "项目id", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "当前页", required = true, example = "0")
        @PathVariable pageNumber: Int,
        @ApiParam(value = "分页大小", required = true, example = "20")
        @PathVariable pageSize: Int,
        @ApiParam("仓库名称", required = false)
        @RequestParam name: String? = null,
        @ApiParam("仓库类型", required = false)
        @RequestParam type: String? = null
    ): Response<Page<RepositoryInfo>> {
        permissionManager.checkProjectPermission(PermissionAction.READ, projectId)
        return ResponseBuilder.success(repositoryService.listRepoPage(projectId, pageNumber, pageSize, name, type))
    }

    @ApiOperation("删除仓库")
    @Permission(type = ResourceType.REPO, action = PermissionAction.DELETE)
    @DeleteMapping("/delete/{projectId}/{repoName}")
    fun deleteRepo(
        @RequestAttribute userId: String,
        @ApiParam(value = "所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "仓库名称", required = true)
        @PathVariable repoName: String,
        @ApiParam(value = "是否强制删除", required = false)
        @RequestParam forced: Boolean = false
    ): Response<Void> {
        repositoryService.deleteRepo(RepoDeleteRequest(projectId, repoName, forced, userId))
        return ResponseBuilder.success()
    }

    @ApiOperation("更新仓库")
    @Permission(type = ResourceType.REPO, action = PermissionAction.UPDATE)
    @PostMapping("/update/{projectId}/{repoName}")
    fun updateRepo(
        @RequestAttribute userId: String,
        @ApiParam(value = "所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "仓库名称", required = true)
        @PathVariable repoName: String,
        @RequestBody request: UserRepoUpdateRequest
    ): Response<Void> {
        val repoUpdateRequest = RepoUpdateRequest(
            projectId = projectId,
            name = repoName,
            public = request.public,
            description = request.description,
            configuration = request.configuration,
            operator = userId
        )
        repositoryService.updateRepo(repoUpdateRequest)
        return ResponseBuilder.success()
    }

    @Deprecated("waiting kb-ci and bk", replaceWith = ReplaceWith("createRepo"))
    @ApiOperation("创建仓库")
    @PostMapping
    fun create(
        @RequestAttribute userId: String,
        @RequestBody userRepoCreateRequest: UserRepoCreateRequest
    ): Response<Void> {
        return this.createRepo(userId, userRepoCreateRequest)
    }
}
