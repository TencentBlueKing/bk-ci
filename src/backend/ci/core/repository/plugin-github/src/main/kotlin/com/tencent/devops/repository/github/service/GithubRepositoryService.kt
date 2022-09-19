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

package com.tencent.devops.repository.github.service

import com.tencent.devops.common.sdk.exception.SdkNotFoundException
import com.tencent.devops.common.sdk.github.DefaultGithubClient
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GithubRepositoryService @Autowired constructor(
    private val defaultGithubClient: DefaultGithubClient
) {
    fun createOrUpdateFile(
        request: CreateOrUpdateFileContentsRequest,
        token: String
    ): CreateOrUpdateFileContentsResponse {
        return defaultGithubClient.execute(
            request = request,
            oauthToken = token
        )
    }

    fun getRepositoryContent(
        request: GetRepositoryContentRequest,
        token: String
    ): RepositoryContent? {
        return try {
            defaultGithubClient.execute(
                request = request,
                oauthToken = token
            )
        } catch (ignore: SdkNotFoundException) {
            null
        }
    }

    fun getRepositoryPermissions(
        request: GetRepositoryPermissionsRequest,
        token: String
    ): RepositoryPermissions {
        return defaultGithubClient.execute(
            request = request,
            oauthToken = token
        )
    }

    fun getRepository(
        request: GetRepositoryRequest,
        token: String
    ): GithubRepo {
        return defaultGithubClient.execute(
            request = request,
            oauthToken = token
        )
    }

    fun listRepositories(
        request: ListRepositoriesRequest,
        token: String
    ): List<GithubRepo> {
        return defaultGithubClient.execute(
            request = request,
            oauthToken = token
        )
    }

    fun listRepositoryCollaborators(
        request: ListRepositoryCollaboratorsRequest,
        token: String
    ): List<GithubUser> {
        return defaultGithubClient.execute(
            request = request,
            oauthToken = token
        )
    }

    fun searchRepositories(
        token: String,
        request: SearchRepositoriesRequest
    ): List<GithubRepo> {
        return defaultGithubClient.execute(
            oauthToken = token,
            request = request
        ).items
    }
}
