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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.repository.pojo.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * {
"name": "master",
"commit": {
"sha": "bdd43327c549105f5e1296d65121afbeb0f3f1ef",
"node_id": "MDM6UmVmNjEyOTgzNDU6dGVzdF92MQ==",
"commit": {
"author": {
"name": "The xxx",
"date": "2015-04-06T15:06:50-08:00",
"email": "xx@cc.com"
},
"url": "https://api.github.com/repos/xx/Hello-World/git/commits/bdd43327c549105f5e1296d65121afbeb0f3f1ef",
"message": "Merge pull request #6 from Spaceghost/patch-1\n\nNew line at end of file.",
"tree": {
"sha": "b4eecafa9be2f2006ce1b709d6857b07069b4608",
"url": "https://api.github.com/repos/xx/Hello-World/git/trees/bdd43327c549105f5e1296d65121afbeb0f3f1ef"
},
"committer": {
"name": "The cc",
"date": "2015-04-06T15:06:50-08:00",
"email": "xx@cc.com"
},
"verification": {
"verified": false,
"reason": "unsigned",
"signature": null,
"payload": null
}
}
}
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubBranch(
    val name: String,
    val commit: GithubCommit?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubCommit(
    val sha: String,
    @JsonProperty("node_id")
    val nodeId: String,
    val commit: GithubCommitData?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubCommitData(
    val message: String,
    val author: GithubCommitAuthor
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubCommitAuthor(
    val name: String,
    val date: String,
    val email: String
)
