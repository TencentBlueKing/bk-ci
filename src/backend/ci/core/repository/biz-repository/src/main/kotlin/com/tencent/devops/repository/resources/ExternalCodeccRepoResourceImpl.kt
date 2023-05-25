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

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils.buildConfig
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ExternalCodeccRepoResource
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.service.CommonRepoFileService
import com.tencent.devops.repository.service.RepoFileService
import com.tencent.devops.scm.pojo.GitMember
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ExternalCodeccRepoResourceImpl @Autowired constructor(
    private val repoFileService: RepoFileService,
    private val commonRepoFileService: CommonRepoFileService
) : ExternalCodeccRepoResource {
    override fun getFileContent(
        repoId: String,
        filePath: String,
        reversion: String?,
        branch: String?,
        subModule: String?,
        repositoryType:
            RepositoryType?
    ): Result<String> {
        return Result(
            repoFileService.getFileContent(
                repositoryConfig = buildConfig(repoId, repositoryType),
                filePath = filePath,
                reversion = reversion,
                branch = branch,
                subModule = subModule
            )
        )
    }

    override fun getFileContentV2(
        repoId: String,
        filePath: String,
        reversion: String?,
        branch: String?,
        subModule: String?,
        repositoryType: RepositoryType?
    ): Result<String> {
        return Result(
            repoFileService.getFileContent(
                repositoryConfig = buildConfig(repoId, repositoryType),
                filePath = filePath,
                reversion = reversion,
                branch = branch,
                subModule = subModule,
                svnFullPath = true
            )
        )
    }

    override fun getGitFileContentCommon(
        repoUrl: String,
        filePath: String,
        ref: String?,
        token: String,
        authType: RepoAuthType?,
        subModule: String?
    ): Result<String> {
        return Result(
            commonRepoFileService.getGitFileContent(
                repoUrl = repoUrl,
                filePath = filePath,
                ref = ref,
                token = token,
                authType = authType,
                subModule = subModule
            )
        )
    }

    override fun getGitFileContentOAuth(
        userId: String,
        repoName: String,
        filePath: String,
        ref: String?
    ): Result<String> {
        return commonRepoFileService.getGitFileContentOauth(
            userId = userId,
            repoName = repoName,
            filePath = filePath,
            ref = ref
        )
    }

    override fun getRepoMembers(repoUrl: String, userId: String): Result<List<GitMember>> {
        return commonRepoFileService.getGitProjectMembers(repoUrl, userId)
    }

    override fun getRepoAllMembers(repoUrl: String, userId: String): Result<List<GitMember>> {
        return commonRepoFileService.getGitProjectAllMembers(repoUrl, userId)
    }

    override fun isProjectMember(repoUrl: String, userId: String): Result<Boolean> {
        return commonRepoFileService.isProjectMember(repoUrl = repoUrl, userId = userId)
    }

    override fun getFileContentByUrl(
        projectId: String,
        repoUrl: String,
        scmType: ScmType,
        filePath: String,
        reversion: String?,
        branch: String?,
        subModule: String?,
        credentialId: String
    ): Result<String> {
        return Result(
            repoFileService.getFileContentByUrl(
                projectId = projectId,
                repoUrl = repoUrl,
                scmType = scmType,
                filePath = filePath,
                reversion = reversion,
                branch = branch,
                subModule = subModule,
                credentialId = credentialId
            )
        )
    }
}
