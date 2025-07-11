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

package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.OPRepositoryResource
import com.tencent.devops.repository.service.OPRepositoryService
import com.tencent.devops.scm.config.GitConfig
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OPRepositoryResourceImpl @Autowired constructor(
    private val opRepositoryService: OPRepositoryService,
    private val gitConfig: GitConfig
) : OPRepositoryResource {
    override fun addHashId() {
        opRepositoryService.addHashId()
    }

    override fun updateGitDomain(
        oldGitDomain: String,
        newGitDomain: String,
        grayProject: String?,
        grayWeight: Int?,
        grayWhiteProject: String?
    ): Result<Boolean> {
        return Result(
            opRepositoryService.updateGitDomain(
                oldGitDomain = oldGitDomain,
                newGitDomain = newGitDomain,
                grayProject = grayProject,
                grayWeight = grayWeight,
                grayWhiteProject = grayWhiteProject
            )
        )
    }

    override fun updateGitProjectId() {
        opRepositoryService.updateAction(
            "updateGitProjectId",
            listOf(
                { opRepositoryService.updateCodeGitProjectId() },
                { opRepositoryService.updateGitLabProjectId() }
            )
        )
    }

    override fun updateGithubProjectId() {
        opRepositoryService.updateAction(
            "updateGitProjectId",
            listOf { opRepositoryService.updateCodeGithubProjectId() }
        )
    }

    override fun setGrayGitHookUrl(projectId: String, repositoryId: Long): Result<Boolean> {
        opRepositoryService.updateGitHookUrl(
            projectId = projectId,
            repositoryId = repositoryId,
            newHookUrl = gitConfig.gitGrayHookUrl,
            oldHookUrl = gitConfig.gitHookUrl
        )
        return Result(true)
    }

    override fun removeGrayGitHookUrl(projectId: String, repositoryId: Long): Result<Boolean> {
        opRepositoryService.updateGitHookUrl(
            projectId = projectId,
            repositoryId = repositoryId,
            newHookUrl = gitConfig.gitHookUrl,
            oldHookUrl = gitConfig.gitGrayHookUrl
        )
        return Result(true)
    }

    override fun removeRepositoryPipelineRef(projectId: String, repoHashId: String): Result<Boolean> {
        opRepositoryService.removeRepositoryPipelineRef(
            projectId = projectId,
            repoHashId = repoHashId
        )
        return Result(true)
    }

    override fun updateRepoCredentialType(projectId: String?, repoHashId: String?): Result<Boolean> {
        opRepositoryService.updateAction(
            "updateRepoCredentialType",
            listOf {
                opRepositoryService.updateRepoCredentialType(
                    projectId = projectId,
                    repoHashId = repoHashId
                )
            }
        )
        return Result(true)
    }

    override fun updateRepoScmCode(projectId: String?, repoHashId: String?): Result<Boolean> {
        opRepositoryService.updateAction(
            "updateRepoScmCode",
            listOf {
                opRepositoryService.updateRepoScmCode(
                    projectId = projectId,
                    repoHashId = repoHashId
                )
            }
        )
        return Result(true)
    }
}
