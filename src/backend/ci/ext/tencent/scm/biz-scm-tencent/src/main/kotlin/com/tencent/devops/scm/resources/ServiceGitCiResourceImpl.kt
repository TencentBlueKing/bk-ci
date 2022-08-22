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

package com.tencent.devops.scm.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.api.ServiceGitCiResource
import com.tencent.devops.scm.pojo.ChangeFileInfo
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.scm.pojo.GitCodeBranchesOrder
import com.tencent.devops.scm.pojo.GitCodeBranchesSort
import com.tencent.devops.scm.pojo.GitCodeFileInfo
import com.tencent.devops.scm.pojo.GitCodeGroup
import com.tencent.devops.scm.pojo.GitCodeProjectInfo
import com.tencent.devops.scm.pojo.GitCodeProjectsOrder
import com.tencent.devops.scm.pojo.GitMember
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import com.tencent.devops.scm.pojo.MrCommentBody
import com.tencent.devops.scm.services.GitCiService
import com.tencent.devops.scm.services.GitService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGitCiResourceImpl @Autowired constructor(
    private val gitService: GitService,
    private val gitCiService: GitCiService
) : ServiceGitCiResource {

    override fun getToken(gitProjectId: String): Result<GitToken> {
        return Result(gitService.getToken(gitProjectId))
    }

    override fun checkUserGitAuth(
        userId: String,
        gitProjectId: String,
        accessLevel: Int,
        privateToken: String?,
        useAccessToken: Boolean
    ): Result<Boolean> {
        return Result(
            gitService.checkUserGitAuth(
                userId = userId,
                gitProjectId = gitProjectId,
                accessLevel = accessLevel,
                privateToken = privateToken,
                useAccessToken = useAccessToken
            )
        )
    }

    override fun clearToken(token: String): Result<Boolean> {
        return Result(gitService.clearToken(token))
    }

    override fun refreshToken(gitProjectId: String, refreshToken: String): Result<GitToken> {
        return Result(gitService.refreshProjectToken(gitProjectId, refreshToken))
    }

    override fun getMembers(
        token: String,
        gitProjectId: String,
        page: Int,
        pageSize:
        Int,
        search: String?
    ): Result<List<GitMember>> {
        return Result(gitCiService.getGitCIMembers(token, gitProjectId, page, pageSize, search))
    }

    override fun getBranches(
        token: String,
        gitProjectId: String,
        page: Int,
        pageSize: Int,
        search: String?,
        orderBy: GitCodeBranchesOrder?,
        sort: GitCodeBranchesSort?
    ): Result<List<String>> {
        return Result(gitCiService.getBranch(token, gitProjectId, page, pageSize, search, orderBy, sort))
    }

    override fun getGitUserId(rtxUserId: String, gitProjectId: String): Result<String?> {
        return Result(gitService.getGitCIUserId(rtxUserId, gitProjectId))
    }

    override fun getGitCIFileContent(
        gitProjectId: String,
        filePath: String,
        token: String,
        ref: String,
        useAccessToken: Boolean
    ): Result<String> {
        return Result(
            gitCiService.getGitCIFileContent(
                gitProjectId = gitProjectId,
                filePath = filePath,
                token = token,
                ref = ref,
                useAccessToken = useAccessToken
            )
        )
    }

    override fun getProjectInfo(
        accessToken: String,
        gitProjectId: String,
        useAccessToken: Boolean
    ): Result<GitCIProjectInfo?> {
        return gitCiService.getGitCIProjectInfo(
            gitProjectId = gitProjectId,
            token = accessToken,
            useAccessToken = useAccessToken
        )
    }

    override fun getGitCodeProjectInfo(
        gitProjectId: String
    ): Result<GitCodeProjectInfo?> {
        return gitCiService.getGitCodeProjectInfo(
            gitProjectId = gitProjectId,
            token = gitService.getToken(gitProjectId).accessToken,
            useAccessToken = true
        )
    }

    override fun getMergeRequestChangeInfo(
        gitProjectId: Long,
        token: String?,
        mrId: Long
    ): Result<GitMrChangeInfo?> {
        return gitCiService.getMergeRequestChangeInfo(
            gitProjectId, token, mrId
        )
    }

    override fun getProjectList(
        accessToken: String,
        userId: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        orderBy: GitCodeProjectsOrder?,
        sort: GitCodeBranchesSort?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): Result<List<GitCodeProjectInfo>> {
        return Result(
            gitCiService.getProjectList(
                accessToken = accessToken, userId = userId, page = page, pageSize = pageSize, search = search,
                orderBy = orderBy, sort = sort, owned = owned, minAccessLevel = minAccessLevel
            )
        )
    }

    override fun getGitFileInfo(
        gitProjectId: String,
        filePath: String?,
        token: String,
        ref: String?,
        useAccessToken: Boolean
    ): Result<GitCodeFileInfo> {
        return gitCiService.getFileInfo(
            gitProjectId = gitProjectId,
            filePath = filePath,
            token = token,
            ref = ref,
            useAccessToken = useAccessToken
        )
    }

    override fun getCommitChangeFileList(
        token: String,
        gitProjectId: String,
        from: String,
        to: String,
        straight: Boolean?,
        page: Int,
        pageSize: Int,
        useAccessToken: Boolean
    ): Result<List<ChangeFileInfo>> {
        return Result(
            gitCiService.getChangeFileList(
                token = token,
                gitProjectId = gitProjectId,
                from = from,
                to = to,
                straight = straight,
                page = page,
                pageSize = pageSize,
                useAccessToken = useAccessToken
            )
        )
    }

    override fun getProjectMembersAll(
        gitProjectId: String,
        page: Int,
        pageSize: Int,
        search: String?
    ): Result<List<GitMember>> {
        return Result(
            gitCiService.getGitCIAllMembers(
                token = gitService.getToken(gitProjectId).accessToken,
                gitProjectId = gitProjectId,
                page = page,
                pageSize = pageSize,
                query = search
            )
        )
    }

    override fun addMrComment(token: String, gitProjectId: String, mrId: Long, mrBody: MrCommentBody) {
        gitCiService.addMrComment(token = token, gitProjectId = gitProjectId, mrId = mrId, mrBody = mrBody)
    }

    override fun getProjectGroupsList(
        accessToken: String,
        page: Int?,
        pageSize: Int?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): Result<List<GitCodeGroup>> {
        return Result(
            gitCiService.getProjectGroupList(
                accessToken = accessToken,
                page = page,
                pageSize = pageSize,
                owned = owned,
                minAccessLevel = minAccessLevel
            )
        )
    }
}
