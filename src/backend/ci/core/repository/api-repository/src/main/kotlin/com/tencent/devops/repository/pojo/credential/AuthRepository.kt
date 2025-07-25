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

package com.tencent.devops.repository.pojo.credential

import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.ScmGitRepository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "代码库鉴权信息")
data class AuthRepository(
    val scmCode: String,
    val url: String,
    val userName: String,
    val auth: IAuthCred
) {
    constructor(repository: Repository) : this(
        scmCode = repository.scmCode,
        url = repository.url,
        userName = repository.userName,
        auth = when (repository) {
            is CodeGitRepository -> {
                if (repository.authType == RepoAuthType.OAUTH) {
                    UserOauthTokenAuthCred(userId = repository.userName)
                } else {
                    CredentialIdAuthCred(credentialId = repository.credentialId, projectId = repository.projectId!!)
                }
            }

            is GithubRepository -> {
                UserOauthTokenAuthCred(userId = repository.userName)
            }

            is ScmGitRepository -> {
                if (repository.authType == RepoAuthType.OAUTH) {
                    UserOauthTokenAuthCred(userId = repository.userName)
                } else {
                    CredentialIdAuthCred(credentialId = repository.credentialId, projectId = repository.projectId!!)
                }
            }

            else -> {
                CredentialIdAuthCred(credentialId = repository.credentialId, projectId = repository.projectId!!)
            }
        }
    )
}
