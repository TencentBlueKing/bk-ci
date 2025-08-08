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

package com.tencent.devops.repository.service.tgit

import com.tencent.devops.repository.pojo.enums.GitCodeBranchesSort
import com.tencent.devops.repository.pojo.enums.GitCodeProjectsOrder
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.git.GitCodeProjectInfo
import com.tencent.devops.repository.pojo.git.GitUserInfo
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.code.git.api.GitBranch
import com.tencent.devops.scm.code.git.api.GitTag
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.pojo.ChangeFileInfo
import com.tencent.devops.scm.pojo.GitFileInfo
import jakarta.servlet.http.HttpServletResponse

interface ITGitService {

    fun getToken(userId: String, code: String): GitToken

    fun getUserInfoByToken(token: String, tokenType: TokenTypeEnum = TokenTypeEnum.OAUTH): GitUserInfo

    fun refreshToken(userId: String, accessToken: GitToken): GitToken

    fun getBranch(
        accessToken: String,
        userId: String,
        repository: String,
        page: Int?,
        pageSize: Int?,
        search: String?
    ): List<GitBranch>

    fun getTag(accessToken: String, userId: String, repository: String, page: Int?, pageSize: Int?): List<GitTag>

    fun getGitFileContent(
        repoName: String,
        filePath: String,
        authType: RepoAuthType?,
        token: String,
        ref: String
    ): String

    fun downloadGitFile(
        repoName: String,
        filePath: String,
        authType: RepoAuthType?,
        token: String,
        ref: String,
        response: HttpServletResponse
    )

    fun getFileTree(
        gitProjectId: String,
        path: String,
        token: String,
        ref: String?,
        recursive: Boolean? = false,
        tokenType: TokenTypeEnum
    ): List<GitFileInfo>

    fun getProjectList(
        accessToken: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        orderBy: GitCodeProjectsOrder?,
        sort: GitCodeBranchesSort?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<GitCodeProjectInfo>

    fun getChangeFileList(
        token: String,
        tokenType: TokenTypeEnum,
        gitProjectId: String,
        from: String,
        to: String,
        straight: Boolean? = false,
        page: Int,
        pageSize: Int,
        url: String
    ): List<ChangeFileInfo>
}
