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
 */

package com.tencent.devops.repository.resources

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils.buildConfig
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.AtomRefRepositoryInfo
import com.tencent.devops.repository.pojo.RepoPipelineRefRequest
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryId
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.RepositoryInfoWithPermission
import com.tencent.devops.repository.pojo.enums.Permission
import com.tencent.devops.repository.service.RepoPipelineService
import com.tencent.devops.repository.service.RepositoryService
import java.net.URLDecoder
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("ALL")
class ServiceRepositoryResourceImpl @Autowired constructor(
    private val repositoryService: RepositoryService,
    private val repoPipelineService: RepoPipelineService
) : ServiceRepositoryResource {

    @BkTimed(extraTags = ["operate", "create"])
    @AuditEntry(actionId = ActionId.REPERTORY_CREATE)
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

    @BkTimed(extraTags = ["operate", "get"])
    override fun list(projectId: String, repositoryType: ScmType?): Result<List<RepositoryInfoWithPermission>> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        return Result(repositoryService.serviceList(projectId, repositoryType))
    }

    /**
     * @param repositoryId 代表的是hashId或者代码库名，依赖repositoryType
     */
    @BkTimed(extraTags = ["operate", "get"])
    override fun get(projectId: String, repositoryId: String, repositoryType: RepositoryType?): Result<Repository> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (repositoryId.isBlank()) {
            throw ParamBlankException("Invalid repositoryHashId")
        }
        return Result(
            repositoryService.serviceGet(
                projectId,
                buildConfig(URLDecoder.decode(repositoryId, "UTF-8"), repositoryType)
            )
        )
    }

    override fun count(
        projectId: Set<String>,
        repositoryHashId: String?,
        repositoryType: ScmType?,
        aliasName: String?
    ): Result<Long> {
        val data = repositoryService.serviceCount(projectId, repositoryHashId ?: "", repositoryType, aliasName ?: "")
        return Result(data)
    }

    @BkTimed(extraTags = ["operate", "get"])
    override fun hasPermissionList(
        userId: String,
        projectId: String,
        repositoryType: String?,
        permission: Permission,
        page: Int?,
        pageSize: Int?,
        aliasName: String?
    ): Result<Page<RepositoryInfo>> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val bkAuthPermission = when (permission) {
            Permission.DELETE -> AuthPermission.DELETE
            Permission.LIST -> AuthPermission.LIST
            Permission.VIEW -> AuthPermission.VIEW
            Permission.EDIT -> AuthPermission.EDIT
            Permission.USE -> AuthPermission.USE
        }
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 9999
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = repositoryService.hasPermissionList(
            userId = userId,
            projectId = projectId,
            repositoryType = repositoryType,
            authPermission = bkAuthPermission,
            offset = limit.offset,
            limit = limit.limit,
            aliasName = aliasName
        )
        return Result(Page(pageNotNull, pageSizeNotNull, result.count, result.records))
    }

    @BkTimed(extraTags = ["operate", "get"])
    override fun listByProjects(projectIds: Set<String>, page: Int?, pageSize: Int?): Result<Page<RepositoryInfo>> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val result = repositoryService.listByProject(projectIds, null, limit.offset, limit.limit)
        return Result(Page(pageNotNull, pageSizeNotNull, result.count, result.records))
    }

    @BkTimed(extraTags = ["operate", "get"])
    override fun listByProject(
        projectId: String,
        repositoryType: ScmType?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<RepositoryInfo>> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20

        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = repositoryService.listByProject(setOf(projectId), repositoryType, limit.offset, limit.limit)
        return Result(Page(pageNotNull, pageSizeNotNull, result.count, result.records))
    }

    @AuditEntry(actionId = ActionId.REPERTORY_DELETE)
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

    @AuditEntry(actionId = ActionId.REPERTORY_EDIT)
    override fun edit(
        userId: String,
        projectId: String,
        repositoryHashId: String,
        repository: Repository
    ): Result<Boolean> {
        repositoryService.userEdit(userId, projectId, repositoryHashId, repository)
        return Result(true)
    }

    @BkTimed(extraTags = ["operate", "get"])
    override fun listRepoByIds(
        repositoryIds: Set<String>
    ): Result<List<Repository>> {
        return (Result(repositoryService.getRepositoryByHashIds(repositoryIds.toList())))
    }

    override fun updatePipelineRef(
        userId: String,
        projectId: String,
        request: RepoPipelineRefRequest
    ): Result<Boolean> {
        repoPipelineService.updatePipelineRef(
            userId = userId,
            projectId = projectId,
            request = request
        )
        return Result(true)
    }

    override fun updateAtomRepoFlag(
        userId: String,
        atomRefRepositoryInfo: List<AtomRefRepositoryInfo>
    ): Result<Boolean> {
        repositoryService.updateAtomRepoFlag(
            userId = userId,
            atomRefRepositoryInfo = atomRefRepositoryInfo
        )
        return Result(true)
    }

    override fun getGitProjectIdByRepositoryHashId(
        userId: String,
        repositoryHashIdList: List<String>
    ): Result<List<String>> {
        return Result(repositoryService.getGitProjectIdByRepositoryHashId(userId, repositoryHashIdList))
    }

    override fun updateStoreRepoProject(userId: String, projectId: String, repositoryId: Long): Result<Boolean> {
        return repositoryService.updateStoreRepoProject(userId, projectId, repositoryId)
    }
}
