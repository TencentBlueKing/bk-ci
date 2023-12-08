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

package com.tencent.devops.repository.service.scm

import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.pojo.enums.GitCodeBranchesSort
import com.tencent.devops.repository.pojo.enums.GitCodeProjectsOrder
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.git.GitCodeProjectInfo
import com.tencent.devops.repository.pojo.git.GitUserInfo
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.repository.service.tgit.ITGitService
import com.tencent.devops.scm.api.ServiceTGitResource
import com.tencent.devops.scm.code.git.api.GitBranch
import com.tencent.devops.scm.code.git.api.GitTag
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.pojo.GitFileInfo
import javax.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service
class TencentTGitServiceImpl @Autowired constructor(val client: Client) : ITGitService {
    override fun getToken(userId: String, code: String): GitToken {
        return client.getScm(ServiceTGitResource::class).getToken(userId, code).data!!
    }

    override fun getUserInfoByToken(token: String, tokenType: TokenTypeEnum): GitUserInfo {
        return client.getScm(ServiceTGitResource::class).getUserInfoByToken(token, tokenType).data!!
    }

    override fun refreshToken(userId: String, accessToken: GitToken): GitToken {
        return client.getScm(ServiceTGitResource::class).refreshToken(userId, accessToken).data!!
    }

    override fun getBranch(
        accessToken: String,
        userId: String,
        repository: String,
        page: Int?,
        pageSize: Int?,
        search: String?
    ): List<GitBranch> {
        return client.getScm(ServiceTGitResource::class).getBranch(
            accessToken = accessToken,
            userId = userId,
            repository = repository,
            page = page,
            pageSize = pageSize,
            search = search
        ).data!!
    }

    override fun getTag(
        accessToken: String,
        userId: String,
        repository: String,
        page: Int?,
        pageSize: Int?
    ): List<GitTag> {
        return client.getScm(ServiceTGitResource::class).getTag(
            accessToken = accessToken,
            userId = userId,
            repository = repository,
            page = page,
            pageSize = pageSize
        ).data ?: emptyList()
    }

    override fun getGitFileContent(
        repoName: String,
        filePath: String,
        authType: RepoAuthType?,
        token: String,
        ref: String
    ): String {
        return client.getScm(ServiceTGitResource::class).getGitFileContent(
            repoName = repoName,
            filePath = filePath,
            authType = authType,
            token = token,
            ref = ref
        ).data!!
    }

    override fun downloadGitFile(
        repoName: String,
        filePath: String,
        authType: RepoAuthType?,
        token: String,
        ref: String,
        response: HttpServletResponse
    ) {
        val serviceUrlPrefix = client.getScmUrl(ServiceTGitResource::class)
        val serviceUrl = "$serviceUrlPrefix/service/tgit/downloadGitFile?repoName=$repoName" +
                "&filePath=$filePath&authType=$authType&token=$token&ref=$ref"
        OkhttpUtils.downloadFile(serviceUrl, response)
    }

    override fun getFileTree(
        gitProjectId: String,
        path: String,
        token: String,
        ref: String?,
        recursive: Boolean?,
        tokenType: TokenTypeEnum
    ): List<GitFileInfo> {
        return client.getScm(ServiceTGitResource::class).getFileTree(
            gitProjectId = gitProjectId,
            path = path,
            token = token,
            ref = ref,
            recursive = recursive,
            tokenType = tokenType
        ).data!!
    }

    override fun getProjectList(
        accessToken: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        orderBy: GitCodeProjectsOrder?,
        sort: GitCodeBranchesSort?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<GitCodeProjectInfo> {
        return client.getScm(ServiceTGitResource::class).getProjectList(
            accessToken = accessToken,
            page = page,
            pageSize = pageSize,
            search = search,
            orderBy = orderBy,
            sort = sort,
            owned = owned,
            minAccessLevel = minAccessLevel
        ).data!!
    }
}
