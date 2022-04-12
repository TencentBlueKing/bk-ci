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
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.stream.pojo.StreamCommitInfo
import com.tencent.devops.stream.pojo.StreamCreateFileInfo
import com.tencent.devops.stream.pojo.StreamGitGroup
import com.tencent.devops.stream.pojo.StreamGitMember
import com.tencent.devops.stream.pojo.StreamGitProjectBaseInfoCache
import com.tencent.devops.stream.pojo.StreamGitProjectInfoWithProject
import com.tencent.devops.stream.pojo.StreamProjectGitInfo
import com.tencent.devops.stream.pojo.enums.StreamBranchesOrder
import com.tencent.devops.stream.pojo.enums.StreamBranchesSort
import com.tencent.devops.stream.pojo.enums.StreamProjectsOrder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(prefix = "stream", value = ["scmType"], havingValue = "CODE_GIT")
class StreamTGitTransferService @Autowired constructor(
    private val client: Client
) : StreamGitTransferService {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamTGitTransferService::class.java)
    }

    fun getAndCheckOauthToken(
        userId: String
    ): GitToken {
        return client.get(ServiceOauthResource::class).gitGet(userId).data ?: throw OauthForbiddenException(
            message = "用户[$userId]尚未进行OAUTH授权，请先授权。"
        )
    }

    override fun getGitProjectCache(
        gitProjectId: String,
        useAccessToken: Boolean,
        userId: String?,
        accessToken: String?
    ): StreamGitProjectBaseInfoCache {
        return client.get(ServiceGitResource::class).getProjectInfo(
            token = accessToken ?: getAndCheckOauthToken(userId!!).accessToken,
            tokenType = if (useAccessToken) {
                TokenTypeEnum.OAUTH
            } else {
                TokenTypeEnum.PRIVATE_KEY
            },
            gitProjectId = gitProjectId
        ).data!!.let {
            StreamGitProjectBaseInfoCache(
                gitProjectId = it.id.toString(),
                gitHttpUrl = it.repositoryUrl,
                homepage = it.homepage,
                pathWithNamespace = it.pathWithNamespace,
                defaultBranch = it.defaultBranch
            )
        }
    }

    override fun getGitProjectInfo(
        gitProjectId: String,
        userId: String
    ): StreamGitProjectInfoWithProject {
        return client.get(ServiceGitResource::class).getProjectInfo(
            token = getAndCheckOauthToken(userId).accessToken,
            tokenType = TokenTypeEnum.OAUTH,
            gitProjectId = gitProjectId
        ).data!!.let {
            StreamGitProjectInfoWithProject(
                gitProjectId = it.id.toLong(),
                name = it.name,
                homepage = it.homepage,
                gitHttpUrl = it.repositoryUrl.replace("https", "http"),
                gitHttpsUrl = it.gitHttpsUrl,
                gitSshUrl = it.gitSshUrl,
                nameWithNamespace = it.namespaceName,
                pathWithNamespace = it.pathWithNamespace,
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
        return client.get(ServiceGitResource::class).getGitFileContent(
            repoName = gitProjectId,
            filePath = fileName,
            authType = RepoAuthType.OAUTH,
            token = getAndCheckOauthToken(userId).accessToken,
            ref = ref
        ).data!!
    }

    override fun getProjectList(
        userId: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        orderBy: StreamProjectsOrder?,
        sort: StreamBranchesSort?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<StreamProjectGitInfo>? {
        TODO("等待接口完成")
    }

    override fun getProjectMember(
        gitProjectId: String,
        userId: String,
        page: Int?,
        pageSize: Int?,
        search: String?
    ): List<StreamGitMember> {
        return client.get(ServiceGitResource::class).getMembers(
            token = getAndCheckOauthToken(userId).accessToken,
            gitProjectId = gitProjectId,
            page = page ?: 1,
            pageSize = pageSize ?: 20,
            search = search,
            tokenType = TokenTypeEnum.OAUTH
        ).data?.map {
            StreamGitMember(
                id = it.id,
                username = it.username,
                state = it.state
            )
        } ?: emptyList()
    }

    override fun isOAuth(
        userId: String,
        redirectUrlType: RedirectUrlTypeEnum?,
        redirectUrl: String?,
        gitProjectId: Long?,
        refreshToken: Boolean?
    ): Result<AuthorizeResult> {
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
        TODO("等待接口完成")
    }

    override fun createNewFile(userId: String, gitProjectId: String, streamCreateFile: StreamCreateFileInfo): Boolean {
        TODO("等待接口完成")
    }

    override fun getProjectBranches(
        userId: String,
        gitProjectId: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        orderBy: StreamBranchesOrder?,
        sort: StreamBranchesSort?
    ): List<String>? {
        return client.get(ServiceGitResource::class).getBranch(
            accessToken = getAndCheckOauthToken(userId).accessToken,
            userId,
            gitProjectId,
            page,
            pageSize
        ).data?.map { it.name }
    }

    override fun getProjectGroupsList(userId: String, page: Int, pageSize: Int): List<StreamGitGroup>? {
        return client.get(ServiceGitResource::class).getProjectGroupsList(
            accessToken = getAndCheckOauthToken(userId).accessToken,
            page = page,
            pageSize = pageSize,
            owned = false,
            minAccessLevel = com.tencent.devops.scm.enums.GitAccessLevelEnum.DEVELOPER,
            tokenType = TokenTypeEnum.OAUTH
        ).data?.ifEmpty { null }?.map {
            StreamGitGroup(it)
        }
    }
}
