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

package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.GitOperationFile
import com.tencent.devops.repository.pojo.git.GitProjectInfo
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.repository.service.RepoFileService
import com.tencent.devops.repository.service.RepositoryService
import com.tencent.devops.repository.service.RepositoryUserService
import com.tencent.devops.repository.service.scm.GitService
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitRepositoryDirItem
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGitRepositoryResourceImpl @Autowired constructor(
    private val gitService: GitService,
    private val repoFileService: RepoFileService,
    private val repositoryService: RepositoryService,
    private val repositoryUserService: RepositoryUserService
) : ServiceGitRepositoryResource {

    override fun createGitCodeRepository(
        userId: String,
        projectCode: String?,
        repositoryName: String,
        sampleProjectPath: String?,
        namespaceId: Int?,
        visibilityLevel: VisibilityLevelEnum?,
        tokenType: TokenTypeEnum,
        frontendType: FrontendTypeEnum?
    ): Result<RepositoryInfo?> {
        return repositoryService.createGitCodeRepository(
            userId = userId,
            projectCode = projectCode,
            repositoryName = repositoryName,
            sampleProjectPath = sampleProjectPath,
            namespaceId = namespaceId,
            visibilityLevel = visibilityLevel,
            tokenType = tokenType,
            frontendType = frontendType
        )
    }

    override fun updateGitCodeRepositoryByProjectName(userId: String, projectName: String, updateGitProjectInfo: UpdateGitProjectInfo, tokenType: TokenTypeEnum): Result<Boolean> {
        return repositoryService.updateGitCodeRepository(userId, projectName, updateGitProjectInfo, tokenType)
    }

    override fun updateGitCodeRepository(userId: String, repoId: String, updateGitProjectInfo: UpdateGitProjectInfo, tokenType: TokenTypeEnum): Result<Boolean> {
        return repositoryService.updateGitCodeRepository(userId, RepositoryConfigUtils.buildConfig(repoId, null), updateGitProjectInfo, tokenType)
    }

    override fun addGitProjectMember(userId: String, userIdList: List<String>, repoId: String, gitAccessLevel: GitAccessLevelEnum, tokenType: TokenTypeEnum): Result<Boolean> {
        return repositoryService.addGitProjectMember(userId, userIdList, RepositoryConfigUtils.buildConfig(repoId, null), gitAccessLevel, tokenType)
    }

    override fun deleteGitProjectMember(userId: String, userIdList: List<String>, repoId: String, tokenType: TokenTypeEnum): Result<Boolean> {
        return repositoryService.deleteGitProjectMember(userId, userIdList, RepositoryConfigUtils.buildConfig(repoId, null), tokenType)
    }

    override fun updateRepositoryUserInfo(userId: String, projectCode: String, repositoryHashId: String): Result<Boolean> {
        return repositoryUserService.updateRepositoryUserInfo(userId, projectCode, repositoryHashId)
    }

    override fun moveGitProjectToGroup(userId: String, groupCode: String?, repoId: String, tokenType: TokenTypeEnum): Result<GitProjectInfo?> {
        return repositoryService.moveGitProjectToGroup(userId, groupCode, RepositoryConfigUtils.buildConfig(repoId, null), tokenType)
    }

    override fun getFileContent(repoId: String, filePath: String, reversion: String?, branch: String?, repositoryType: RepositoryType?): Result<String> {
        return Result(repoFileService.getFileContent(RepositoryConfigUtils.buildConfig(repoId, repositoryType), filePath, reversion, branch))
    }

    override fun updateTGitFileContent(
        userId: String,
        repoId: String,
        repositoryType: RepositoryType?,
        gitOperationFile: GitOperationFile
    ): Result<Boolean> {
        return repoFileService.updateTGitFileContent(
            RepositoryConfigUtils.buildConfig(repoId, repositoryType),
            userId,
            gitOperationFile
        )
    }

    override fun delete(
        userId: String,
        projectId: String,
        repositoryHashId: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        val deleteGitProjectResult = repositoryService.deleteGitProject(
            userId = userId,
            repositoryConfig = RepositoryConfigUtils.buildConfig(repositoryHashId, null),
            tokenType = tokenType
        )
        if (deleteGitProjectResult.isNotOk()) {
            return deleteGitProjectResult
        }
        repositoryService.userDelete(userId, projectId, repositoryHashId)
        return Result(true)
    }

    override fun getGitRepositoryTreeInfo(
        userId: String,
        repoId: String,
        refName: String?,
        path: String?,
        tokenType: TokenTypeEnum
    ): Result<List<GitRepositoryDirItem>?> {
        return repositoryService.getGitRepositoryTreeInfo(
            userId = userId,
            repositoryConfig = RepositoryConfigUtils.buildConfig(repoId, null),
            refName = refName,
            path = path,
            tokenType = tokenType
        )
    }

    override fun getAuthUrl(authParamJsonStr: String): Result<String> {
        return Result(gitService.getAuthUrl(authParamJsonStr))
    }

    override fun getRepoRecentCommitInfo(
        userId: String,
        repoId: String,
        sha: String,
        tokenType: TokenTypeEnum
    ): Result<GitCommit?> {
        return repositoryService.getRepoRecentCommitInfo(
            userId = userId,
            sha = sha,
            repositoryConfig = RepositoryConfigUtils.buildConfig(repoId, null),
            tokenType = tokenType
        )
    }

    override fun createGitTag(
        userId: String,
        repoId: String,
        tagName: String,
        ref: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return repositoryService.createGitTag(
            userId = userId,
            tagName = tagName,
            ref = ref,
            repositoryConfig = RepositoryConfigUtils.buildConfig(repoId, null),
            tokenType = tokenType
        )
    }
}
