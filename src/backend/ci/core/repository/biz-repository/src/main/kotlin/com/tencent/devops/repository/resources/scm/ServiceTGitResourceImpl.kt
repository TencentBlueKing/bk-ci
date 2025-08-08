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

package com.tencent.devops.repository.resources.scm

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.scm.ServiceTGitResource
import com.tencent.devops.repository.pojo.enums.GitCodeBranchesSort
import com.tencent.devops.repository.pojo.enums.GitCodeProjectsOrder
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.git.GitCodeProjectInfo
import com.tencent.devops.repository.pojo.git.GitUserInfo
import com.tencent.devops.repository.service.RepositoryService
import com.tencent.devops.repository.service.tgit.ITGitService
import com.tencent.devops.scm.code.git.api.GitBranch
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.pojo.ChangeFileInfo
import com.tencent.devops.scm.pojo.GitFileInfo
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("ALL")
class ServiceTGitResourceImpl @Autowired constructor(
    private val gitService: ITGitService,
    private val repositoryService: RepositoryService,
    private val client: Client
) : ServiceTGitResource {
    override fun getBranch(
        accessToken: String,
        userId: String,
        repository: String,
        page: Int?,
        pageSize: Int?,
        search: String?
    ): Result<List<GitBranch>> {
        return Result(
            gitService.getBranch(
                userId = userId,
                accessToken = accessToken,
                repository = repository,
                page = page,
                pageSize = pageSize,
                search = search
            )
        )
    }

    override fun getGitFileContent(
        repoName: String,
        filePath: String,
        authType: RepoAuthType?,
        token: String,
        ref: String
    ): Result<String> {
        return Result(
            gitService.getGitFileContent(
                repoName = repoName,
                filePath = filePath,
                authType = authType,
                token = token,
                ref = ref
            )
        )
    }

    override fun downloadGitFile(
        repoId: String,
        filePath: String,
        authType: RepoAuthType?,
        ref: String,
        response: HttpServletResponse
    ) {
        val repo = repositoryService.serviceGet(
            "",
            RepositoryConfigUtils.buildConfig(repoId, RepositoryType.ID)
        )
        val token = client.get(ServiceOauthResource::class).gitGet(repo.userName).data?.accessToken ?: ""
        gitService.downloadGitFile(
            repoName = repo.projectName,
            filePath = filePath,
            authType = authType,
            token = token,
            ref = ref,
            response = response
        )
    }

    override fun getGitFileTree(
        gitProjectId: String,
        path: String,
        token: String,
        ref: String?,
        recursive: Boolean?,
        tokenType: TokenTypeEnum
    ): Result<List<GitFileInfo>> {
        return Result(
            gitService.getFileTree(
                gitProjectId = gitProjectId,
                path = path,
                token = token,
                ref = ref,
                recursive = recursive,
                tokenType = tokenType
            )
        )
    }

    override fun getUserInfoByToken(
        token: String,
        tokenType: TokenTypeEnum
    ): Result<GitUserInfo> {
        return Result(
            gitService.getUserInfoByToken(
                token,
                tokenType
            )
        )
    }

    override fun getGitCodeProjectList(
        accessToken: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        orderBy: GitCodeProjectsOrder?,
        sort: GitCodeBranchesSort?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): Result<List<GitCodeProjectInfo>> {
        return Result(
            gitService.getProjectList(
                accessToken = accessToken,
                page = page,
                pageSize = pageSize,
                search = search,
                orderBy = orderBy,
                sort = sort,
                owned = owned,
                minAccessLevel = minAccessLevel
            )
        )
    }

    override fun getChangeFileList(
        token: String,
        tokenType: TokenTypeEnum,
        gitProjectId: String,
        from: String,
        to: String,
        straight: Boolean?,
        page: Int,
        pageSize: Int,
        url: String
    ): Result<List<ChangeFileInfo>> {
        return Result(
            gitService.getChangeFileList(
                tokenType = tokenType,
                gitProjectId = gitProjectId,
                token = token,
                from = from,
                to = to,
                straight = straight,
                page = page,
                pageSize = pageSize,
                url = url
            )
        )
    }
}
