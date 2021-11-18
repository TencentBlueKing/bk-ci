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

package com.tencent.devops.stream.pojo.git

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.devops.common.ci.v2.enums.gitEventKind.TGitObjectKind

/**
{
    "object_kind": "push",
    "operation_kind": "create",
    "action_kind": "client push",
    "before": "b96850262fabfa9a8a9d28fff9040621958379f9",
    "after": "e99db09904cfa385a129305e7660395f161950a",
    "ref": "refs/heads/master",
    "user_id": 11323,
    "user_name": "git_user1",
    "user_email": "git_user1@tencent.com",
    "project_id": 165245,
    "repository": {
        "name": "z-test",
        "url": "git@tencent.com:test/z-test.git",
        "description": "",
        "homepage": "https://tencent.com/test/z-test",
        "git_http_url":"http://tencent.com/test/z-test.git",
        "git_ssh_url":"git@tencent.com:test/z-test.git",
        "visibility_level":0
    },
    "commits": [
        {
            "id": "b96850262fabfa9a8a9d28fff9040621958379f9",
            "message": "fix readme",
            "timestamp": "2015-03-13T09:52:25+0000",
            "url": "https://tencent.com/test/z-test/commit/b96850262fabfa9a8a9d28fff9040621958379f9",
            "author": {
            "name": "alex",
            "email": "alex@tencent.com"
        },
            "added":[],
            "modified":[
            "README.md"
            ],
            "removed":[]
        },
        {
            "id": "1480a4610ca01dd10cb5bc30d8a1b1ea568c09a1",
            "message": "update readme",
            "timestamp": "2015-03-13T09:51:06+0000",
            "url": "https://tencent.com/test/z-test/commit/b96850262fabfa9a8a9d28fff9040621958379f9",
            "author": {
            "name": "Bob",
            "email": "Bob@tencent.com"
        },
            "added":[],
            "modified":[
            "README.md"
            ],
            "removed":[]
        }
    ],
    "total_commits_count": 2
}
 */
@Suppress("ConstructorParameterNaming")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitPushEvent(
    val operation_kind: String,
    val action_kind: String,
    val ref: String,
    val before: String,
    val after: String,
    val user_name: String,
    val checkout_sha: String?,
    val project_id: Long,
    val repository: GitCommitRepository,
    val commits: List<GitCommit>?,
    val push_options: Map<String, String>?,
    val total_commits_count: Int
) : GitEvent() {
    companion object {
        const val classType = TGitObjectKind.OBJECT_KIND_PUSH
    }
}
