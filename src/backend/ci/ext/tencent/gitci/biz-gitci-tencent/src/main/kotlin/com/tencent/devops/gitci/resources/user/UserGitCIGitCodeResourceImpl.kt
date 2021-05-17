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

package com.tencent.devops.gitci.resources.user

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.user.UserGitCIGitCodeResource
import com.tencent.devops.gitci.utils.GitCommonUtils
import com.tencent.devops.gitci.v2.service.OauthService
import com.tencent.devops.gitci.v2.service.ScmService
import com.tencent.devops.repository.pojo.git.GitMember
import com.tencent.devops.scm.pojo.Commit
import com.tencent.devops.scm.pojo.GitCICreateFile
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class UserGitCIGitCodeResourceImpl @Autowired constructor(
    private val scmService: ScmService,
    private val oauthService: OauthService
) : UserGitCIGitCodeResource {
    override fun getGitCodeProjectInfo(userId: String, gitProjectId: String): Result<GitCIProjectInfo?> {
        return Result(
            scmService.getProjectInfo(
                token = getToken(userId),
                gitProjectId = gitProjectId,
                useAccessToken = true
            )
        )
    }

    override fun getGitCodeProjectMembers(
        userId: String,
        projectId: String,
        page: Int,
        pageSize: Int,
        search: String?
    ): Result<List<GitMember>?> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId).toString()
        return Result(
            scmService.getProjectMembers(
                token = getToken(userId),
                gitProjectId = gitProjectId, page = page, pageSize = pageSize, search = search
            )
        )
    }

    override fun getGitCodeCommits(
        userId: String,
        projectId: String,
        filePath: String?,
        branch: String?,
        since: String?,
        until: String?,
        page: Int,
        perPage: Int
    ): Result<List<Commit>?> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        return Result(
            scmService.getCommits(
                token = getToken(userId = userId),
                gitProjectId = gitProjectId,
                filePath = filePath,
                branch = branch,
                since = since,
                until = until,
                page = page,
                perPage = perPage
            )
        )
    }

    override fun gitCodeCreateFile(
        userId: String,
        projectId: String,
        gitCICreateFile: GitCICreateFile
    ): Result<Boolean> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId).toString()
        return Result(
            scmService.createNewFile(
                token = getToken(userId = userId),
                gitProjectId = gitProjectId,
                gitCICreateFile = gitCICreateFile
            )
        )
    }

    private fun getToken(userId: String): String {
        val token = oauthService.getOauthToken(userId) ?: throw CustomException(
            Response.Status.UNAUTHORIZED,
            "用户$userId 无OAuth权限"
        )
        return token.accessToken
    }
}
