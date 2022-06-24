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

import com.tencent.devops.common.sdk.github.request.CreateCheckRunRequest
import com.tencent.devops.common.sdk.github.request.GHGetBranchRequest
import com.tencent.devops.common.sdk.github.request.GHListBranchesRequest
import com.tencent.devops.common.sdk.github.request.GetCommitRequest
import com.tencent.devops.common.sdk.github.request.GetPullRequestRequest
import com.tencent.devops.common.sdk.github.request.GetTreeRequest
import com.tencent.devops.common.sdk.github.request.ListCommitRequest
import com.tencent.devops.common.sdk.github.request.ListPullRequestFileRequest
import com.tencent.devops.common.sdk.github.request.UpdateCheckRunRequest
import org.junit.jupiter.api.Test

class GithubApiTest {

    private val client = DefaultGithubClient(
        serverUrl = "https://github.com/",
        apiUrl = "https://api.github.com/"
    )

    private val token = "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"
    private val repo = "Study_Github"
    private val owner = "liuandhisgithub"
    private val defaultBranch = "master"

    @Test
    fun listBranches() {
        val request = GHListBranchesRequest(
            owner = "Tencent",
            repo = "bk-ci"
        )
        val response = client.execute(
            oauthToken = "d501d306428d8d34656c726a0c8980c08f343534232",
            request = request
        )
        println(response)
    }

    @Test
    fun getBranch() {
        val request = GHGetBranchRequest(
            owner = "Tencent",
            repo = "bk-ci",
            branch = "master"
        )
        val response = client.execute(
            oauthToken = "d501d306428d8d34656c726a0c8980c08f343534232",
            request = request
        )
        println(response)
    }

    @Test
    fun listCommit(){
        val request = ListCommitRequest(
            owner = owner,
            repo = repo,
        )
        val response = client.execute(
            oauthToken = token,
            request = request
        )
        println(response)
    }

    @Test
    fun getCommit(){
        val request = GetCommitRequest(
            owner = owner,
            repo = repo,
            ref = "28dc82cd39da037bd836f41ae4b305eb0ef3c775"
        )
        val response = client.execute(
            oauthToken = token,
            request = request
        )
        println(response)
    }

    @Test
    fun getPullRequest(){
        val request = GetPullRequestRequest(
            owner = "Florence-y",
            repo = "bk-ci",
            pullNumber = "1"
        )
        val response = client.execute(
            oauthToken = "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",
            request = request
        )
        println(response)
    }

    @Test
    fun listPullRequestFile(){
        val request = ListPullRequestFileRequest(
            owner = "Florence-y",
            repo = "bk-ci",
            pullNumber = "1"
        )
        val response = client.execute(
            oauthToken = "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",
            request = request
        )
        println(response)
    }

    @Test
    fun getTree(){
        val request = GetTreeRequest(
            owner = owner,
            repo = repo,
            treeSha = "28dc82cd39da037bd836f41ae4b305eb0ef3c775"
        )
        val response = client.execute(
            oauthToken = token,
            request = request
        )
        println(response)
    }

    @Test
    fun createCheckRun(){
        val request = CreateCheckRunRequest(
            owner = owner,
            repo = repo,
            name = "code-coverage",
            headSha = "28dc82cd39da037bd836f41ae4b305eb0ef3c775"
        )
        val response = client.execute(
            oauthToken = token,
            request = request
        )
        println(response)
    }

    @Test
    fun updateCheckRun(){
        val request = UpdateCheckRunRequest(
            owner = owner,
            repo = repo,
            checkRunId = "xxx"
        )
        val response = client.execute(
            oauthToken = token,
            request = request
        )
        println(response)
    }

}
