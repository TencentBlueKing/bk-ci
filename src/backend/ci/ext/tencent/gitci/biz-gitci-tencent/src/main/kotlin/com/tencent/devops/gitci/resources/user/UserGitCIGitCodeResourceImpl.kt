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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.user.UserGitCIGitCodeResource
import com.tencent.devops.gitci.constant.GitCIConstant.GIT_CI_FILE_DIR
import com.tencent.devops.gitci.constant.GitCIConstant.GIT_CI_FILE_SUFFIX
import com.tencent.devops.gitci.permission.GitCIV2PermissionService
import com.tencent.devops.gitci.utils.GitCommonUtils
import com.tencent.devops.gitci.v2.service.GitCIBasicSettingService
import com.tencent.devops.gitci.v2.service.OauthService
import com.tencent.devops.gitci.v2.service.ScmService
import com.tencent.devops.repository.pojo.git.GitMember
import com.tencent.devops.scm.pojo.Commit
import com.tencent.devops.scm.pojo.GitCICreateFile
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.scm.pojo.GitCodeBranchesOrder
import com.tencent.devops.scm.pojo.GitCodeBranchesSort
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserGitCIGitCodeResourceImpl @Autowired constructor(
    private val scmService: ScmService,
    private val oauthService: OauthService,
    private val permissionService: GitCIV2PermissionService,
    private val gitCIBasicSettingService: GitCIBasicSettingService
) : UserGitCIGitCodeResource {
    override fun getGitCodeProjectInfo(userId: String, gitProjectId: String): Result<GitCIProjectInfo?> {
        if (gitProjectId.isBlank()) {
            return Result(data = null)
        }
        return Result(
            scmService.getProjectInfo(
                token = getToken(userId, isEnableUser = false, gitProjectId = 0),
                gitProjectId = gitProjectId,
                useAccessToken = true
            )
        )
    }

    override fun getGitCodeProjectMembers(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        search: String?
    ): Result<List<GitMember>?> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId).toString()
        return Result(
            scmService.getProjectMembers(
                token = getToken(userId, isEnableUser = true, gitProjectId = gitProjectId.toLong()),
                gitProjectId = gitProjectId,
                page = page,
                pageSize = pageSize,
                search = search
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
        page: Int?,
        pageSize: Int?
    ): Result<List<Commit>?> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        permissionService.checkGitCIPermission(userId, projectId)
        return Result(
            scmService.getCommits(
                token = getToken(userId = userId, isEnableUser = true, gitProjectId = gitProjectId),
                gitProjectId = gitProjectId,
                filePath = filePath,
                branch = branch,
                since = since,
                until = until,
                page = page,
                perPage = pageSize
            )
        )
    }

    override fun gitCodeCreateFile(
        userId: String,
        projectId: String,
        gitCICreateFile: GitCICreateFile
    ): Result<Boolean> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId).toString()
        permissionService.checkGitCIPermission(userId, projectId, AuthPermission.CREATE)
        permissionService.checkEnableGitCI(gitProjectId.toLong())
        val newFile = gitCICreateFile.copy(
            filePath = getFilePath(gitCICreateFile.filePath)
        )
        return Result(
            scmService.createNewFile(
                userId = userId,
                token = getToken(userId = userId, isEnableUser = false, gitProjectId = gitProjectId.toLong()),
                gitProjectId = gitProjectId,
                gitCICreateFile = newFile
            )
        )
    }

    // 默认在.ci目录下，.yml后缀
    private fun getFilePath(filePath: String): String {
        var newPath = filePath
        if (!filePath.startsWith("$GIT_CI_FILE_DIR/")) {
            newPath = "$GIT_CI_FILE_DIR/$newPath"
        }
        if (!filePath.endsWith(GIT_CI_FILE_SUFFIX)) {
            newPath = "${newPath}$GIT_CI_FILE_SUFFIX"
        }
        return newPath
    }

    override fun getGitCodeBranches(
        userId: String,
        projectId: String,
        search: String?,
        page: Int?,
        pageSize: Int?,
        orderBy: GitCodeBranchesOrder?,
        sort: GitCodeBranchesSort?
    ): Result<List<String>?> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId).toString()
        return Result(
            scmService.getProjectBranches(
                token = getToken(userId = userId, isEnableUser = true, gitProjectId = gitProjectId.toLong()),
                gitProjectId = gitProjectId,
                page = page,
                pageSize = pageSize,
                orderBy = orderBy,
                sort = sort,
                search = search
            )
        )
    }

    // 针对获取项目信息使用超级token
    private fun getToken(gitProjectId: String): String {
        return scmService.getToken(gitProjectId).accessToken
    }

    // 看是否使用工蜂开启人的OAuth
    private fun getToken(userId: String, isEnableUser: Boolean, gitProjectId: Long): String {
        return if (isEnableUser) {
            val setting = gitCIBasicSettingService.getGitCIBasicSettingAndCheck(gitProjectId)
            oauthService.getAndCheckOauthToken(setting.enableUserId).accessToken
        } else {
            return oauthService.getAndCheckOauthToken(userId).accessToken
        }
    }
}
