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

package com.tencent.devops.repository.service.hub

import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.repository.service.RepositoryScmConfigService
import com.tencent.devops.repository.service.RepositoryService
import com.tencent.devops.repository.service.ScmApiManager
import com.tencent.devops.scm.api.pojo.Change
import com.tencent.devops.scm.api.pojo.Comment
import com.tencent.devops.scm.api.pojo.CommentInput
import com.tencent.devops.scm.api.pojo.Commit
import com.tencent.devops.scm.api.pojo.ListOptions
import com.tencent.devops.scm.api.pojo.PullRequest
import com.tencent.devops.scm.api.pojo.PullRequestInput
import com.tencent.devops.scm.api.pojo.PullRequestListOptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ScmPullRequestApiService @Autowired constructor(
    private val repositoryService: RepositoryService,
    private val providerRepositoryFactory: ScmProviderRepositoryFactory,
    private val repositoryScmConfigService: RepositoryScmConfigService,
    private val scmApiManager: ScmApiManager
) : AbstractScmApiService(
    repositoryService = repositoryService,
    providerRepositoryFactory = providerRepositoryFactory,
    repositoryScmConfigService = repositoryScmConfigService
) {
    fun findPullRequest(
        projectId: String,
        number: Int,
        authRepository: AuthRepository
    ): PullRequest {
        return invokeApi(
            projectId = projectId,
            authRepository = authRepository
        ) { providerProperties, providerRepository ->
            scmApiManager.findPullRequest(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                number = number
            )
        }
    }

    fun createPullRequest(
        projectId: String,
        input: PullRequestInput,
        authRepository: AuthRepository
    ): PullRequest {
        return invokeApi(
            projectId = projectId,
            authRepository = authRepository
        ) { providerProperties, providerRepository ->
            scmApiManager.createPullRequest(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                input = input
            )
        }
    }

    fun listPullRequest(
        projectId: String,
        opts: PullRequestListOptions,
        authRepository: AuthRepository
    ): List<PullRequest> {
        return invokeApi(
            projectId = projectId,
            authRepository = authRepository
        ) { providerProperties, providerRepository ->
            scmApiManager.listPullRequest(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                opts = opts
            )
        }
    }

    fun listPullRequestChanges(
        projectId: String,
        number: Int,
        opts: ListOptions,
        authRepository: AuthRepository
    ): List<Change> {
        return invokeApi(
            projectId = projectId,
            authRepository = authRepository
        ) { providerProperties, providerRepository ->
            scmApiManager.listPullRequestChanges(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                number = number,
                opts = opts
            )
        }
    }

    fun listPullRequestCommits(
        projectId: String,
        number: Int,
        opts: ListOptions,
        authRepository: AuthRepository
    ): List<Commit> {
        return invokeApi(
            projectId = projectId,
            authRepository = authRepository
        ) { providerProperties, providerRepository ->
            scmApiManager.listPullRequestCommits(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                number = number,
                opts = opts
            )
        }
    }

    fun merge(
        projectId: String,
        number: Int,
        authRepository: AuthRepository
    ) {
        return invokeApi(
            projectId = projectId,
            authRepository = authRepository
        ) { providerProperties, providerRepository ->
            scmApiManager.merge(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                number = number
            )
        }
    }

    fun findPullRequestComment(
        projectId: String,
        number: Int,
        commentId: Long,
        authRepository: AuthRepository
    ): Comment {
        return invokeApi(
            projectId = projectId,
            authRepository = authRepository
        ) { providerProperties, providerRepository ->
            scmApiManager.findPullRequestComment(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                number = number,
                commentId = commentId
            )
        }
    }

    fun listPullRequestComment(
        projectId: String,
        number: Int,
        opts: ListOptions,
        authRepository: AuthRepository
    ): List<Comment> {
        return invokeApi(
            projectId = projectId,
            authRepository = authRepository
        ) { providerProperties, providerRepository ->
            scmApiManager.listPullRequestComment(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                number = number,
                opts = opts
            )
        }
    }

    fun createPullRequestComment(
        projectId: String,
        number: Int,
        input: CommentInput,
        authRepository: AuthRepository
    ): Comment {
        return invokeApi(
            projectId = projectId,
            authRepository = authRepository
        ) { providerProperties, providerRepository ->
            scmApiManager.createPullRequestComment(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                number = number,
                input = input
            )
        }
    }

    fun deletePullRequestComment(
        projectId: String,
        number: Int,
        commentId: Long,
        authRepository: AuthRepository
    ) {
        return invokeApi(
            projectId = projectId,
            authRepository = authRepository
        ) { providerProperties, providerRepository ->
            scmApiManager.deletePullRequestComment(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                number = number,
                commentId = commentId
            )
        }
    }
}
