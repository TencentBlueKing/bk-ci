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

package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils.buildConfig
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.UserRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryId
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.RepositoryInfoWithPermission
import com.tencent.devops.repository.pojo.RepositoryPage
import com.tencent.devops.repository.pojo.commit.CommitResponse
import com.tencent.devops.repository.pojo.enums.Permission
import com.tencent.devops.repository.service.CommitService
import com.tencent.devops.repository.service.RepositoryPermissionService
import com.tencent.devops.repository.service.RepositoryService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserRepositoryResourceImpl @Autowired constructor(
    private val repositoryService: RepositoryService,
    private val commitService: CommitService,
    private val repositoryPermissionService: RepositoryPermissionService
) : UserRepositoryResource {

    companion object {
        private const val PageSize = 20
    }

    override fun hasCreatePermission(userId: String, projectId: String): Result<Boolean> {
        return Result(repositoryPermissionService.hasPermission(userId, projectId, BkAuthPermission.CREATE))
    }

    override fun hasAliasName(
        userId: String,
        projectId: String,
        repositoryHashId: String?,
        aliasName: String
    ): Result<Boolean> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (aliasName.isBlank()) {
            throw ParamBlankException("Invalid repository aliasName")
        }
        return Result(repositoryService.hasAliasName(projectId, repositoryHashId, aliasName))
    }

    override fun create(userId: String, projectId: String, repository: Repository): Result<RepositoryId> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (repository.aliasName.isBlank()) {
            throw ParamBlankException("Invalid repository aliasName")
        }
        if (repository.url.isBlank()) {
            throw ParamBlankException("Invalid repository url")
        }
        return Result(RepositoryId(repositoryService.userCreate(userId, projectId, repository)))
    }

    override fun get(
        userId: String,
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?
    ): Result<Repository> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (repositoryId.isBlank()) {
            throw ParamBlankException("Invalid repositoryHashId")
        }
        return Result(repositoryService.userGet(userId, projectId, buildConfig(repositoryId, repositoryType)))
    }

    override fun edit(
        userId: String,
        projectId: String,
        repositoryHashId: String,
        repository: Repository
    ): Result<Boolean> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (repositoryHashId.isBlank()) {
            throw ParamBlankException("Invalid repositoryHashId")
        }
        if (repository.aliasName.isBlank()) {
            throw ParamBlankException("Invalid repository aliasName")
        }
        if (repository.url.isBlank()) {
            throw ParamBlankException("Invalid repository url")
        }
        repositoryService.userEdit(userId, projectId, repositoryHashId, repository)
        return Result(true)
    }

    override fun list(
        userId: String,
        projectId: String,
        repositoryType: ScmType?,
        page: Int?,
        pageSize: Int?
    ): Result<RepositoryPage<RepositoryInfoWithPermission>> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: PageSize
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = repositoryService.userList(userId, projectId, repositoryType, limit.offset, limit.limit)
        return Result(
            RepositoryPage(
                pageNotNull,
                pageSizeNotNull,
                result.first.count,
                result.first.records,
                result.second
            )
        )
    }

    override fun hasPermissionList(
        userId: String,
        projectId: String,
        repositoryType: ScmType?,
        permission: Permission,
        page: Int?,
        pageSize: Int?
    ): Result<Page<RepositoryInfo>> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val bkAuthPermission = when (permission) {
            Permission.DELETE -> BkAuthPermission.DELETE
            Permission.LIST -> BkAuthPermission.LIST
            Permission.VIEW -> BkAuthPermission.VIEW
            Permission.EDIT -> BkAuthPermission.EDIT
            Permission.USE -> BkAuthPermission.USE
        }
        val pageNotNull = page ?: 0

        val pageSizeNotNull = pageSize ?: PageSize
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = repositoryService.hasPermissionList(
            userId,
            projectId,
            repositoryType,
            bkAuthPermission,
            limit.offset,
            limit.limit
        )
        return Result(Page(pageNotNull, pageSizeNotNull, result.count, result.records))
    }

    override fun delete(userId: String, projectId: String, repositoryHashId: String): Result<Boolean> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (repositoryHashId.isBlank()) {
            throw ParamBlankException("Invalid repositoryHashId")
        }
        repositoryService.userDelete(userId, projectId, repositoryHashId)
        return Result(true)
    }

    override fun getCommit(buildId: String): Result<List<CommitResponse>> {
        return Result(commitService.getCommit(buildId))
    }
}