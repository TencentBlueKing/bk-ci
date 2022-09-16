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

package com.tencent.devops.common.sdk.github

import com.tencent.devops.common.sdk.github.request.CreateOrUpdateFileContentsRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryContentRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryPermissionsRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryRequest
import com.tencent.devops.common.sdk.github.request.ListOrganizationsRequest
import com.tencent.devops.common.sdk.github.request.ListRepositoriesRequest
import com.tencent.devops.common.sdk.github.request.ListRepositoryCollaboratorsRequest
import com.tencent.devops.common.sdk.github.request.SearchRepositoriesRequest
import com.tencent.devops.common.sdk.util.SdkJsonUtil
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
class GithubRepositoryApiTest : GithubApiTest() {

    @Test
    fun getRepositoryByName() {
        val request = GetRepositoryRequest(
            repoName = repoName
        )
        val response = client.execute(
            oauthToken = token,
            request = request
        )
        println(SdkJsonUtil.toJson(response))
    }

    @Test
    fun getRepositoryById() {
        val request = GetRepositoryRequest(
            repoName = repoId.toString()
        )
        val response = client.execute(
            oauthToken = token,
            request = request
        )
        println(SdkJsonUtil.toJson(response))
    }

    @Test
    fun getRepositoryContent() {
        val request = GetRepositoryContentRequest(
            repoName = repoId.toString(),
            ref = "master",
            path = "README.md"
        )
        val response = client.execute(
            oauthToken = token,
            request = request
        )

        println(response)
        println(response.getDecodedContentAsString())
    }

    @Test
    fun listRepositoriesRequest() {
        val request = ListRepositoriesRequest()
        val response = client.execute(
            oauthToken = token,
            request = request
        )
        println(response)
    }

//    @Test
    fun createOrUpdateFileContents() {
        // create
//        val createRequest = CreateOrUpdateFileContentsRequest(
//            owner = owner,
//            repo = "chatroom",
//            path = "README5555.md",
//            message = "test",
//            content = "bXkgbmV3IGZpbGUgY29udGVudHM=",
//            branch = defaultBranch
//        )
//        val createResponse = client.execute(
//            oauthToken = token,
//            request = createRequest
//        )
//        println(createResponse)
        // update
        val updateRequest = CreateOrUpdateFileContentsRequest(
            repoName = repoId.toString(),
            path = "README5555.md",
            message = "update",
            // (update in new file) encoding by base64
            content = "YWRhc2Rhc2Rhcw==",
            sha = "0d5a690c8fad5e605a6e8766295d9d459d65de42",
            branch = "master"
        )

        val updateResponse = client.execute(
            oauthToken = token,
            request = updateRequest
        )
        println(updateResponse)
    }

    @Test
    fun listRepositoryCollaborators() {
        val request = ListRepositoryCollaboratorsRequest(
            repoName = repoId.toString()
        )

        val response = client.execute(
            oauthToken = token,
            request = request
        )
        println(response)
    }

    @Test
    fun listOrganizationsForTheAuthenticatedUser() {
        val request = ListOrganizationsRequest()
        val response = client.execute(
            oauthToken = token,
            request = request
        )
        println(response)
    }

    @Test
    fun getRepositoryPermissionsForAUser() {
        val request = GetRepositoryPermissionsRequest(
            repoName = repoId.toString(),
            username = "mingshewhe"
        )
        val response = client.execute(
            oauthToken = token,
            request = request
        )
        println(response)
    }

    @Test
    fun searchRepositories() {
        val request = SearchRepositoriesRequest()
        request.name("bk-ci")
        request.org("Tencent")
        val response = client.execute(
            oauthToken = token,
            request = request
        )
        println(response)
    }
}
