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

package com.tencent.devops.repository.utils

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeP4Repository
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.scm.utils.code.svn.SvnUtils

@Suppress("ALL")
object RepositoryUtils {
    // 自动生成的仓库别名前带上"_"
    private const val PREFIX_ALIAS_NAME = "_"

    fun buildRepository(
        projectId: String,
        userName: String,
        scmType: ScmType,
        repositoryUrl: String,
        credentialId: String?
    ): Repository {
        val realCredentialId = credentialId ?: ""
        return when (scmType) {
            ScmType.CODE_SVN -> {
                val projectName = SvnUtils.getSvnProjectName(repositoryUrl)
                CodeSvnRepository(
                    aliasName = "$PREFIX_ALIAS_NAME$projectName",
                    url = repositoryUrl,
                    credentialId = realCredentialId,
                    projectName = projectName,
                    userName = userName,
                    projectId = projectId,
                    repoHashId = null,
                    svnType = CodeSvnRepository.SVN_TYPE_HTTP
                )
            }
            ScmType.CODE_GIT -> {
                val projectName = GitUtils.getProjectName(repositoryUrl)
                CodeGitRepository(
                    aliasName = "$PREFIX_ALIAS_NAME$projectName",
                    url = repositoryUrl,
                    credentialId = realCredentialId,
                    projectName = projectName,
                    userName = userName,
                    authType = RepoAuthType.HTTP,
                    projectId = projectId,
                    repoHashId = null,
                    gitProjectId = 0L
                )
            }
            ScmType.CODE_TGIT -> {
                val projectName = GitUtils.getProjectName(repositoryUrl)
                CodeGitRepository(
                    aliasName = "$PREFIX_ALIAS_NAME$projectName",
                    url = repositoryUrl,
                    credentialId = realCredentialId,
                    projectName = projectName,
                    userName = userName,
                    authType = RepoAuthType.HTTP,
                    projectId = projectId,
                    repoHashId = null,
                    gitProjectId = 0L
                )
            }
            ScmType.CODE_GITLAB -> {
                val projectName = GitUtils.getProjectName(repositoryUrl)
                CodeGitlabRepository(
                    aliasName = "$PREFIX_ALIAS_NAME$projectName",
                    url = repositoryUrl,
                    credentialId = realCredentialId,
                    projectName = projectName,
                    userName = userName,
                    projectId = projectId,
                    repoHashId = null,
                    gitProjectId = 0L
                )
            }
            ScmType.GITHUB -> {
                val projectName = GitUtils.getProjectName(repositoryUrl)
                GithubRepository(
                    aliasName = "$PREFIX_ALIAS_NAME$projectName",
                    url = repositoryUrl,
                    userName = userName,
                    projectName = projectName,
                    projectId = projectId,
                    repoHashId = null
                )
            }
            ScmType.CODE_P4 -> {
                CodeP4Repository(
                    aliasName = repositoryUrl,
                    url = repositoryUrl,
                    credentialId = credentialId!!,
                    projectName = repositoryUrl,
                    userName = userName,
                    projectId = projectId,
                    repoHashId = null
                )
            }
            else -> throw IllegalArgumentException("Unknown repository type")
        }
    }
}
