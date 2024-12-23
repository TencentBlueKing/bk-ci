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

import com.tencent.devops.common.api.exception.OauthForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.repository.pojo.AppInstallationResult
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.GitCodeFileEncoding
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.git.GitOperationFile
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.stream.constant.StreamMessageCode.NOT_AUTHORIZED_BY_OAUTH
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.StreamCommitInfo
import com.tencent.devops.stream.pojo.StreamCreateFileInfo
import com.tencent.devops.stream.pojo.StreamFileEncoding
import com.tencent.devops.stream.pojo.StreamGitGroup
import com.tencent.devops.stream.pojo.StreamGitMember
import com.tencent.devops.stream.pojo.StreamGitProjectBaseInfoCache
import com.tencent.devops.stream.pojo.StreamGitProjectInfoWithProject
import com.tencent.devops.stream.pojo.StreamProjectGitInfo
import com.tencent.devops.stream.pojo.enums.StreamBranchesOrder
import com.tencent.devops.stream.pojo.enums.StreamProjectsOrder
import com.tencent.devops.stream.pojo.enums.StreamSortAscOrDesc
import com.tencent.devops.stream.pojo.enums.toGitCodeAscOrDesc
import com.tencent.devops.stream.pojo.enums.toGitCodeOrderBy
import com.tencent.devops.stream.service.StreamGitTransferService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class StreamTGitTransferService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val streamBasicSettingDao: StreamBasicSettingDao
) : StreamGitTransferService {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamTGitTransferService::class.java)
        private const val DEFAULT_PAGE = 1
        private const val DEFAULT_PAGE_SIZE = 20
    }

    fun getAndCheckOauthToken(
        userId: String
    ): GitToken {
        return client.get(ServiceOauthResource::class).gitGet(userId).data ?: throw OauthForbiddenException(
            message = MessageUtil.getMessageByLocale(NOT_AUTHORIZED_BY_OAUTH, I18nUtil.getLanguage(userId))
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
        ).data?.let {
            StreamGitProjectBaseInfoCache(
                gitProjectId = it.id.toString(),
                gitHttpUrl = it.repositoryUrl,
                homepage = it.homepage,
                pathWithNamespace = it.pathWithNamespace,
                defaultBranch = it.defaultBranch
            )
        } ?: throw OauthForbiddenException(
            message = "get git project($gitProjectId) info error|useAccessToken=$useAccessToken"
        )
    }

    override fun getGitProjectInfo(
        gitProjectId: String,
        userId: String?
    ): StreamGitProjectInfoWithProject? {
        val realUserId = userId ?: try {
            streamBasicSettingDao.getSetting(dslContext, gitProjectId.toLong())?.enableUserId ?: return null
        } catch (e: NumberFormatException) {
            streamBasicSettingDao.getSettingByPathWithNameSpace(dslContext, gitProjectId)?.enableUserId ?: return null
        }
        return client.get(ServiceGitResource::class).getProjectInfo(
            token = getAndCheckOauthToken(realUserId).accessToken,
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
        page: Int,
        pageSize: Int,
        search: String?,
        orderBy: StreamProjectsOrder?,
        sort: StreamSortAscOrDesc?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<StreamProjectGitInfo>? {
        return client.get(ServiceGitResource::class).getGitCodeProjectList(
            accessToken = getAndCheckOauthToken(userId).accessToken,
            page = page,
            pageSize = pageSize,
            search = search,
            orderBy = orderBy.toGitCodeOrderBy(),
            sort = sort.toGitCodeAscOrDesc(),
            owned = owned,
            minAccessLevel = minAccessLevel
        ).data?.map { StreamProjectGitInfo(it) }
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
            page = page ?: DEFAULT_PAGE,
            pageSize = pageSize ?: DEFAULT_PAGE_SIZE,
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
        gitProjectId: Long,
        refreshToken: Boolean?
    ): Result<AuthorizeResult> {
        // 更改为每次都进行重定向授权
        return client.get(ServiceOauthResource::class).isOAuth(
            userId = userId,
            redirectUrlType = redirectUrlType,
            redirectUrl = redirectUrl,
            gitProjectId = gitProjectId,
            refreshToken = true
        )
    }

    override fun enableCi(userId: String, projectName: String, enable: Boolean?): Result<Boolean> {
        return client.get(ServiceGitResource::class).enableCi(
            projectName = projectName,
            token = getAndCheckOauthToken(userId).accessToken,
            tokenType = TokenTypeEnum.OAUTH,
            enable = enable
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
        return client.get(ServiceGitResource::class).getCommits(
            gitProjectId = gitProjectId,
            filePath = filePath,
            branch = branch,
            token = getAndCheckOauthToken(userId).accessToken,
            since = since,
            until = until,
            page = page ?: 1,
            perPage = perPage ?: 20,
            tokenType = TokenTypeEnum.OAUTH
        ).data?.map { StreamCommitInfo(it) }
    }

    override fun createNewFile(userId: String, gitProjectId: String, streamCreateFile: StreamCreateFileInfo): Boolean {
        return client.get(ServiceGitResource::class).gitCreateFile(
            gitProjectId = gitProjectId,
            token = getAndCheckOauthToken(userId).accessToken,
            gitOperationFile = GitOperationFile(
                filePath = streamCreateFile.filePath,
                branch = streamCreateFile.branch,
                encoding = when (streamCreateFile.encoding) {
                    StreamFileEncoding.TEXT -> GitCodeFileEncoding.TEXT
                    StreamFileEncoding.BASE64 -> GitCodeFileEncoding.BASE64
                },
                content = streamCreateFile.content,
                commitMessage = streamCreateFile.commitMessage
            ),
            tokenType = TokenTypeEnum.OAUTH
        ).data ?: false
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
        return client.get(ServiceGitResource::class).getBranch(
            accessToken = getAndCheckOauthToken(userId).accessToken,
            userId = userId,
            repository = gitProjectId,
            page = page,
            pageSize = pageSize,
            search = search
        ).data?.map { it.name }
    }

    override fun getProjectGroupsList(userId: String, page: Int, pageSize: Int): List<StreamGitGroup>? {
        return client.get(ServiceGitResource::class).getProjectGroupsList(
            accessToken = getAndCheckOauthToken(userId).accessToken,
            page = page,
            pageSize = pageSize,
            owned = false,
            minAccessLevel = GitAccessLevelEnum.DEVELOPER,
            tokenType = TokenTypeEnum.OAUTH
        ).data?.ifEmpty { null }?.map {
            StreamGitGroup(it)
        }
    }

    override fun isInstallApp(userId: String, gitProjectId: Long): AppInstallationResult {
        // 工蜂没有app，默认都已经安装
        return AppInstallationResult(true)
    }
}
