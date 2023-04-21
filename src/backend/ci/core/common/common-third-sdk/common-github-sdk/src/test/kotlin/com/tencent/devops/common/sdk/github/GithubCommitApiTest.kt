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
 *
 */

package com.tencent.devops.common.sdk.github

import com.tencent.devops.common.sdk.github.request.CompareTwoCommitsRequest
import com.tencent.devops.common.sdk.github.request.GetCommitRequest
import com.tencent.devops.common.sdk.github.request.ListCommitRequest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
class GithubCommitApiTest : GithubApiTest() {

    @Test
    fun listCommit() {
        val request = ListCommitRequest(
            repoName = repoId.toString()
        )
        val response = client.execute(
            oauthToken = token,
            request = request
        )
        println(response)
    }

    @Test
    fun getCommit() {
        val request = GetCommitRequest(
            repoName = repoId.toString(),
            ref = "master"
        )
        val response = client.execute(
            oauthToken = token,
            request = request
        )
        println(response)
    }

    @Test
    fun compareTwoCommits() {
        val request = CompareTwoCommitsRequest(
            repoName = repoId.toString(),
            base = "530e45d8163aeb04bb3af5d69ec1f1d24782f179",
            head = "1c166db7bcb0266e4b0f8e469890614ff0f2c33f"
        )
        val response = client.execute(oauthToken = token, request = request)
        println(response)
    }
}
