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

package com.tencent.devops.stream.service.transfer

import com.tencent.devops.common.api.constant.HTTP_200
import com.tencent.devops.common.api.exception.OauthForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.sdk.github.pojo.GithubRepo
import com.tencent.devops.common.sdk.github.request.CreateOrUpdateFileContentsRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryContentRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryRequest
import com.tencent.devops.common.sdk.github.request.ListBranchesRequest
import com.tencent.devops.common.sdk.github.request.ListCommitRequest
import com.tencent.devops.common.sdk.github.request.ListOrganizationsRequest
import com.tencent.devops.common.sdk.github.request.ListRepositoriesRequest
import com.tencent.devops.common.sdk.github.request.ListRepositoryCollaboratorsRequest
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.api.ServiceGithubResource
import com.tencent.devops.repository.api.github.ServiceGithubAppResource
import com.tencent.devops.repository.api.github.ServiceGithubBranchResource
import com.tencent.devops.repository.api.github.ServiceGithubCommitsResource
import com.tencent.devops.repository.api.github.ServiceGithubOrganizationResource
import com.tencent.devops.repository.api.github.ServiceGithubRepositoryResource
import com.tencent.devops.repository.pojo.AppInstallationResult
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.pojo.github.GithubToken
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.stream.constant.StreamMessageCode.NOT_AUTHORIZED_BY_OAUTH
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
import com.tencent.devops.stream.service.StreamGitTransferService
import java.util.Base64
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

class StreamGithubTransferService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val streamBasicSettingDao: StreamBasicSettingDao
) : StreamGitTransferService {

    // github 组织白名单列表
    @Value("\${github.orgWhite:}")
    private var githubOrgWhite: String = ""

    companion object {
        private val logger = LoggerFactory.getLogger(StreamGithubTransferService::class.java)
        private const val DEFAULT_GITHUB_PER_PAGE = 100
        private const val DEFAULT_PAGE = 1
        private const val DEFAULT_PAGE_SIZE = 30
    }

    // gitProjectId在github中必须为项目名字
    override fun getGitProjectCache(
        gitProjectId: String,
        useAccessToken: Boolean,
        userId: String?,
        accessToken: String?
    ): StreamGitProjectBaseInfoCache {
        return client.get(ServiceGithubRepositoryResource::class).getRepository(
            request = GetRepositoryRequest(
                repoName = gitProjectId
            ),
            token = accessToken ?: getAndCheckOauthToken(userId!!).accessToken
        ).data?.let {
            StreamGitProjectBaseInfoCache(
                gitProjectId = it.id.toString(),
                gitHttpUrl = it.cloneUrl,
                homepage = it.homepage,
                pathWithNamespace = it.fullName,
                defaultBranch = it.defaultBranch
            )
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
                repoName = gitProjectId
            ),
            token = getAndCheckOauthToken(realUserId).accessToken
        ).data?.let {
            StreamGitProjectInfoWithProject(
                gitProjectId = it.id,
                name = it.name,
                homepage = it.htmlUrl,
                gitHttpUrl = it.cloneUrl,
                gitHttpsUrl = it.cloneUrl,
                gitSshUrl = it.sshUrl,
                nameWithNamespace = it.fullName,
                pathWithNamespace = it.fullName,
                defaultBranch = it.defaultBranch,
                description = it.description,
                avatarUrl = it.owner.avatarUrl,
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
                repoName = gitProjectId,
                path = fileName,
                ref = ref
            ),
            token = getAndCheckOauthToken(userId).accessToken
        ).data?.getDecodedContentAsString() ?: ""
    }

    override fun getProjectList(
        userId: String,
        page: Int,
        pageSize: Int,
        search: String?,
        orderBy: StreamProjectsOrder?,
        sort: StreamSortAscOrDesc?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<StreamProjectGitInfo>? {
        // search  owned  minAccessLevel 参数暂时没使用
        var githubPage = DEFAULT_PAGE
        val repos = mutableListOf<StreamProjectGitInfo>()
        // github查询有权限列表不支持名称搜索，需要自实现搜索
        // TODO 目前先全部查出来然后再分页,后面需要改造
        run outside@{
            while (true) {
                val request = ListRepositoriesRequest(
                    page = githubPage,
                    perPage = DEFAULT_GITHUB_PER_PAGE
                )
                val githubRepos = client.get(ServiceGithubRepositoryResource::class).listRepositories(
                    request = request,
                    token = getAndCheckOauthToken(userId).accessToken
                ).data!!
                val filterGithubRepos = githubRepos.filter {
                    isGithubOrgWhite(it) && search(search, it)
                }.map { StreamProjectGitInfo(it) }
                repos.addAll(filterGithubRepos)
                if (githubRepos.size < DEFAULT_GITHUB_PER_PAGE) {
                    return@outside
                }
                githubPage++
            }
        }
        val start = (page - 1) * pageSize
        val end = (start + pageSize).coerceAtMost(repos.size)
        return if (start >= repos.size) {
            emptyList()
        } else {
            repos.subList(start, end)
        }
    }

    private fun isGithubOrgWhite(githubRepo: GithubRepo): Boolean {
        if (githubOrgWhite.isBlank()) {
            return true
        }
        val org = githubRepo.fullName.split("/").first()
        return githubOrgWhite.split(",").contains(org)
    }

    private fun search(search: String?, githubRepo: GithubRepo): Boolean {
        if (search.isNullOrBlank()) {
            return true
        }
        return githubRepo.fullName.contains(search)
    }

    override fun getProjectMember(
        gitProjectId: String,
        userId: String,
        page: Int?,
        pageSize: Int?,
        search: String?
    ): List<StreamGitMember> {
        val request = ListRepositoryCollaboratorsRequest(
            repoName = gitProjectId,
            page = page ?: DEFAULT_PAGE,
            perPage = pageSize ?: DEFAULT_PAGE_SIZE
        )
        return client.get(ServiceGithubRepositoryResource::class).listRepositoryCollaborators(
            request = request,
            token = getAndCheckOauthToken(userId).accessToken
        ).data!!.map {
            // state 属性无
            StreamGitMember(
                id = it.id,
                username = it.login,
                state = ""
            )
        }
    }

    override fun isOAuth(
        userId: String,
        redirectUrlType: RedirectUrlTypeEnum?,
        redirectUrl: String?,
        gitProjectId: Long,
        refreshToken: Boolean?
    ): Result<AuthorizeResult> {
        // 直接以当前用户更新授权人并开启ci
        streamBasicSettingDao.updateOauthSetting(
            dslContext, gitProjectId, userId, userId
        )
        // github未实现 重定向授权逻辑，直接返回200
        return Result(AuthorizeResult(HTTP_200))
    }

    override fun enableCi(userId: String, projectName: String, enable: Boolean?): Result<Boolean> {
        // github 不支持
        return Result(true)
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
                repoName = gitProjectId.toString(),
                page = page ?: DEFAULT_PAGE,
                perPage = perPage ?: DEFAULT_PAGE_SIZE
            ),
            token = getAndCheckOauthToken(userId).accessToken
            // todo commit 信息严重不足
        ).data?.map { StreamCommitInfo(it) }
    }

    override fun createNewFile(
        userId: String,
        gitProjectId: String,
        streamCreateFile: StreamCreateFileInfo
    ): Boolean {
        client.get(ServiceGithubRepositoryResource::class).createOrUpdateFile(
            request = with(streamCreateFile) {
                CreateOrUpdateFileContentsRequest(
                    repoName = gitProjectId,
                    message = commitMessage,
                    content = Base64.getEncoder().encodeToString(content.toByteArray()),
                    path = filePath,
                    branch = branch
                )
            },
            token = getAndCheckOauthToken(userId).accessToken
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
            request = ListBranchesRequest(
                repoName = gitProjectId,
                page = page ?: DEFAULT_PAGE,
                perPage = pageSize ?: DEFAULT_PAGE_SIZE
            ),
            token = getAndCheckOauthToken(userId).accessToken
        ).data?.map { it.name }
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
            token = getAndCheckOauthToken(userId).accessToken
        ).data?.ifEmpty { null }?.map {
            StreamGitGroup(it)
        }
    }

    override fun isInstallApp(userId: String, gitProjectId: Long): AppInstallationResult {
        return client.get(ServiceGithubAppResource::class).isInstallApp(
            token = getAndCheckOauthToken(userId).accessToken,
            repoName = gitProjectId.toString()
        ).data!!
    }

    fun getAndCheckOauthToken(
        userId: String
    ): GithubToken {
        return client.get(ServiceGithubResource::class).getAccessToken(userId).data ?: throw OauthForbiddenException(
            message = MessageUtil.getMessageByLocale(NOT_AUTHORIZED_BY_OAUTH, I18nUtil.getLanguage(userId))
        )
    }
}
