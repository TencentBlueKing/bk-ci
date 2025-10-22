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

package com.tencent.devops.process.yaml.actions.data

import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeTGitRepository
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.repository.pojo.Repository

/**
 * pac绑定的代码库配置信息
 * @param projectId 项目ID
 * @param repoHashId 代码库hashId
 * @param enableUser 开启pac的用户ID
 * @param gitProjectId git项目ID,只有tgit/gitlab/github才有值
 * @param projectName 代码平台项目名: namespace/name
 * @param credentialId 代码库绑定的凭证ID
 */
data class PacRepoSetting(
    val projectId: String,
    val repoHashId: String,
    val enableUser: String,
    val gitProjectId: Long?,
    val projectName: String,
    val credentialId: String?,
    val aliasName: String
) {
    constructor(repository: Repository) : this(
        projectId = repository.projectId!!,
        repoHashId = repository.repoHashId!!,
        enableUser = repository.userName,
        gitProjectId = when (repository) {
            is CodeGitRepository -> repository.gitProjectId
            is CodeTGitRepository -> repository.gitProjectId
            is GithubRepository -> repository.gitProjectId
            is CodeGitlabRepository -> repository.gitProjectId
            else -> null
        },
        projectName = repository.projectName,
        credentialId = repository.credentialId,
        aliasName = repository.aliasName
    )
}
