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

package com.tencent.devops.repository.resources.github

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.sdk.github.pojo.GithubRepo
import com.tencent.devops.common.sdk.github.pojo.GithubUser
import com.tencent.devops.common.sdk.github.pojo.RepositoryContent
import com.tencent.devops.common.sdk.github.pojo.RepositoryPermissions
import com.tencent.devops.common.sdk.github.request.CreateOrUpdateFileContentsRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryContentRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryPermissionsRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryRequest
import com.tencent.devops.common.sdk.github.request.ListRepositoriesRequest
import com.tencent.devops.common.sdk.github.request.ListRepositoryCollaboratorsRequest
import com.tencent.devops.common.sdk.github.request.SearchRepositoriesRequest
import com.tencent.devops.common.sdk.github.response.CreateOrUpdateFileContentsResponse
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.github.ServiceGithubRepositoryResource
import com.tencent.devops.repository.github.service.GithubRepositoryService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGithubRepositoryResourceImpl @Autowired constructor(
    val githubRepositoryService: GithubRepositoryService
) : ServiceGithubRepositoryResource {
    override fun createOrUpdateFile(
        token: String,
        request: CreateOrUpdateFileContentsRequest
    ): Result<CreateOrUpdateFileContentsResponse> {
        return Result(
            githubRepositoryService.createOrUpdateFile(
                request = request,
                token = token
            )
        )
    }

    override fun getRepositoryContent(
        token: String,
        request: GetRepositoryContentRequest
    ): Result<RepositoryContent?> {
        return Result(
            githubRepositoryService.getRepositoryContent(
                request = request,
                token = token
            )
        )
    }

    override fun getRepositoryPermissions(
        token: String,
        request: GetRepositoryPermissionsRequest
    ): Result<RepositoryPermissions?> {
        return Result(
            githubRepositoryService.getRepositoryPermissions(
                request = request,
                token = token
            )
        )
    }

    override fun getRepository(
        token: String,
        request: GetRepositoryRequest
    ): Result<GithubRepo?> {
        return Result(
            githubRepositoryService.getRepository(
                request = request,
                token = token
            )
        )
    }

    override fun listRepositories(
        token: String,
        request: ListRepositoriesRequest
    ): Result<List<GithubRepo>> {
        return Result(
            githubRepositoryService.listRepositories(
                request = request,
                token = token
            )
        )
    }

    override fun listRepositoryCollaborators(
        token: String,
        request: ListRepositoryCollaboratorsRequest
    ): Result<List<GithubUser>> {
        return Result(
            githubRepositoryService.listRepositoryCollaborators(
                request = request,
                token = token
            )
        )
    }

    override fun searchRepositories(
        token: String,
        request: SearchRepositoriesRequest
    ): Result<List<GithubRepo>> {
        return Result(
            githubRepositoryService.searchRepositories(
                token = token,
                request = request
            )
        )
    }
}
