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

package com.tencent.devops.stream.service

import com.tencent.devops.common.api.exception.OauthForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.sdk.github.request.CreateOrUpdateFileContentsRequest
import com.tencent.devops.common.sdk.github.request.GHListBranchesRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryContentRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryRequest
import com.tencent.devops.common.sdk.github.request.ListCommitRequest
import com.tencent.devops.common.sdk.github.request.ListOrganizationsRequest
import com.tencent.devops.common.sdk.github.request.ListRepositoriesRequest
import com.tencent.devops.common.sdk.github.request.ListRepositoryCollaboratorsRequest
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.github.ServiceGithubBranchResource
import com.tencent.devops.repository.api.github.ServiceGithubCommitsResource
import com.tencent.devops.repository.api.github.ServiceGithubOrganizationResource
import com.tencent.devops.repository.api.github.ServiceGithubRepositoryResource
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.StreamCommitInfo
import com.tencent.devops.stream.pojo.StreamCreateFileInfo
import com.tencent.devops.stream.pojo.StreamGitGroup
import com.tencent.devops.stream.pojo.StreamGitMember
import com.tencent.devops.stream.pojo.StreamGitProjectBaseInfoCache
import com.tencent.devops.stream.pojo.StreamGitProjectInfoWithProject
import com.tencent.devops.stream.pojo.StreamProjectGitInfo
import com.tencent.devops.stream.pojo.enums.StreamBranchesOrder
import com.tencent.devops.stream.pojo.enums.StreamProjectsOrder
import com.tencent.devops.stream.pojo.enums.StreamSortAscOrDesc
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(prefix = "stream", value = ["scmType"], havingValue = "GITHUB")
class StreamGithubTransferService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val streamBasicSettingDao: StreamBasicSettingDao
) : StreamGitTransferService {
    // gitProjectId在github中必须为项目名字
    override fun getGitProjectCache(
        gitProjectId: String,
        useAccessToken: Boolean,
        userId: String?,
        accessToken: String?
    ): StreamGitProjectBaseInfoCache {
        return userId?.let {
            client.get(ServiceGithubRepositoryResource::class).getRepository(
                request = GetRepositoryRequest(
                    owner = userId,
                    repo = gitProjectId
                ),
                userId = it
            ).let {
                StreamGitProjectBaseInfoCache(
                    gitProjectId = it.gitProjectId.toString(),
                    gitHttpUrl = it.gitHttpUrl,
                    homepage = it.homepage,
                    pathWithNamespace = it.nameWithNamespace,
                    defaultBranch = it.defaultBranch
                )
            }
        } ?: throw OauthForbiddenException(
            message = "get git project($gitProjectId) info error|useAccessToken=$useAccessToken"
        )
    }

    override fun getGitProjectInfo(gitProjectId: String, userId: String?): StreamGitProjectInfoWithProject? {
        val realUserId = userId ?: try {
            streamBasicSettingDao.getSetting(dslContext, gitProjectId.toLong())?.enableUserId ?: return null
        } catch (e: NumberFormatException) {
            streamBasicSettingDao.getSettingByPathWithNameSpace(dslContext, gitProjectId)?.enableUserId ?: return null
        }
        return client.get(ServiceGithubRepositoryResource::class).getRepository(
            request = GetRepositoryRequest(
                owner = realUserId,
                repo = gitProjectId
            ),
            userId = realUserId
        ).let {
            StreamGitProjectInfoWithProject(
                gitProjectId = it.gitProjectId,
                name = it.name,
                homepage = it.homepage,
                gitHttpUrl = it.gitHttpUrl.replace("https", "http"),
                gitHttpsUrl = it.gitHttpUrl,
                gitSshUrl = it.gitSshUrl,
                nameWithNamespace = it.nameWithNamespace,
                pathWithNamespace = it.nameWithNamespace,
                defaultBranch = it.defaultBranch,
                description = it.description,
                avatarUrl = it.avatarUrl,
                routerTag = null
            )
        }
    }

    override fun getYamlContent(
        gitProjectId: String,
        userId: String,
        fileName: String,
        ref: String
    ): String {
        return client.get(ServiceGithubRepositoryResource::class).getRepositoryContent(
            request = GetRepositoryContentRequest(
                owner = userId,
                repo = gitProjectId,
                path = fileName,
                ref = ref
            ),
            userId = userId
        ).content ?: ""
    }

    override fun getProjectList(
        userId: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        orderBy: StreamProjectsOrder?,
        sort: StreamSortAscOrDesc?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<StreamProjectGitInfo>? {
        // search  owned  minAccessLevel 参数暂时没使用
        return client.get(ServiceGithubRepositoryResource::class).listRepositories(
            request = genListRepositoriesRequest(
                page = page,
                pageSize = pageSize,
                orderBy = orderBy,
                sort = sort
            ),
            userId = userId
        ).map { StreamProjectGitInfo(it) }
    }

    private fun genListRepositoriesRequest(
        page: Int?,
        pageSize: Int?,
        orderBy: StreamProjectsOrder?,
        sort: StreamSortAscOrDesc?
    ): ListRepositoriesRequest {
        val request = ListRepositoriesRequest()
        request.page = page ?: request.page
        request.perPage = pageSize ?: request.perPage
        request.sort = orderBy?.value ?: request.sort
        request.direction = sort?.value ?: request.direction
        return request
    }

    override fun getProjectMember(
        gitProjectId: String,
        userId: String,
        page: Int?,
        pageSize: Int?,
        search: String?
    ): List<StreamGitMember> {
        return client.get(ServiceGithubRepositoryResource::class).listRepositoryCollaborators(
            request = genListRepositoryCollaboratorsRequest(
                gitProjectId,
                userId,
                page,
                pageSize
            ),
            userId = userId
        ).map {
            // state 属性无
            StreamGitMember(
                id = it.id,
                username = it.username,
                state = ""
            )
        }
    }

    private fun genListRepositoryCollaboratorsRequest(
        gitProjectId: String,
        userId: String,
        page: Int?,
        pageSize: Int?
    ): ListRepositoryCollaboratorsRequest {
        val request = ListRepositoryCollaboratorsRequest(owner = userId, repo = gitProjectId)
        request.page = page ?: request.page
        request.perPage = pageSize ?: request.perPage
        return request
    }

    override fun isOAuth(
        userId: String,
        redirectUrlType: RedirectUrlTypeEnum?,
        redirectUrl: String?,
        gitProjectId: Long?,
        refreshToken: Boolean?
    ): Result<AuthorizeResult> {
        // todo 未实现
        return client.get(ServiceOauthResource::class).isOAuth(
            userId = userId,
            redirectUrlType = redirectUrlType,
            redirectUrl = redirectUrl,
            gitProjectId = gitProjectId,
            refreshToken = refreshToken
        )
    }

    override fun getCommits(
        userId: String,
        gitProjectId: Long,
        filePath: String?,
        branch: String?,
        since: String?,
        until: String?,
        page: Int?,
        perPage: Int?
    ): List<StreamCommitInfo>? {
        return client.get(ServiceGithubCommitsResource::class).listCommits(
            request = ListCommitRequest(
                owner = userId,
                // todo gitProjectId是 Long ，需要做兼容
                repo = gitProjectId.toString(),
                page = page ?: 1,
                perPage = perPage ?: 30
            ),
            userId = userId
        // todo commit 信息严重不足
        )?.map { StreamCommitInfo(it) }
    }

    override fun createNewFile(
        userId: String,
        gitProjectId: String,
        streamCreateFile: StreamCreateFileInfo
    ): Boolean {
        val createOrUpdateFile = client.get(ServiceGithubRepositoryResource::class).createOrUpdateFile(
            request = with(streamCreateFile) {
                CreateOrUpdateFileContentsRequest(
                    owner = userId,
                    repo = gitProjectId,
                    message = commitMessage,
                    content = content,
                    path = filePath
                )
            },
            userId = userId
        )
        return true
    }

    override fun getProjectBranches(
        userId: String,
        gitProjectId: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        orderBy: StreamBranchesOrder?,
        sort: StreamSortAscOrDesc?
    ): List<String>? {
        return client.get(ServiceGithubBranchResource::class).listBranch(
            request = GHListBranchesRequest(
                owner = userId,
                repo = gitProjectId,
                page = page ?: 1,
                perPage = pageSize ?: 30
            ),
            userId = userId
        ).map { it.name }
    }

    override fun getProjectGroupsList(
        userId: String,
        page: Int,
        pageSize: Int
    ): List<StreamGitGroup>? {
        return client.get(ServiceGithubOrganizationResource::class).listOrganizations(
            request = ListOrganizationsRequest(
                page = page,
                perPage = pageSize
            ),
            userId = userId
        ).ifEmpty { null }?.map {
            StreamGitGroup(it)
        }
    }
}
