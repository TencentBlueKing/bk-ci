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

package com.tencent.devops.gitci.pojo.git

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.devops.gitci.OBJECT_KIND_TAG_PUSH

/**
 * {
 *   "object_kind":"push",
 *   "before":"9d1861bd3ae32cda2a92479962712065aae19cf2",
 *   "after":"47f4f77f5428eed4e75f4d84d1b9089b38c5a34e",
 *   "ref":"refs/heads/test",
 *   "checkout_sha":"47f4f77f5428eed4e75f4d84d1b9089b38c5a34e",
 *   "user_name":"xxxx",
 *   "user_id":11648,
 *   "user_email":"xxxx@xxxx.com",
 *   "project_id":46619,
 *   "repository":{
 *    　"name":"maven-hello-world",
 *      "description":"",
 *      "homepage":"http://git.xxxx.com/xxxx/maven-hello-world",
 *      "git_http_url":"http://git.xxxx.com/xxxx/maven-hello-world.git",
 *      "git_ssh_url":"git@git.xxxx.com:xxxx/maven-hello-world.git",
 *      "url":"git@git.xxxx.com:xxxx/maven-hello-world.git",
 *      "visibility_level":0
 *    },
 *    "commits":[
 *       {
 *         "id":"47f4f77f5428eed4e75f4d84d1b9089b38c5a34e",
 *         "message":"Test webhook",
 *         "timestamp":"2018-03-16T06:50:11+0000",
 *         "url":"http://git.xxxx.com/xxxx/maven-hello-world/commit/47f4f77f5428eed4e75f4d84d1b9089b38c5a34e",
 *         "author":{
 *           "name":"xxxx",
 *           "email":"xxxx@xxxx.com"
 *         },
 *         "added":[
 *
 *         ],
 *         "modified":[
 *           "test.txt"
 *         ],
 *         "removed":[
 *         ]
 *       }
 *     ],
 *   "total_commits_count":1
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitTagPushEvent(
    val operation_kind: String,
    val ref: String,
    val before: String,
    val after: String,
    val user_name: String,
    val checkout_sha: String?,
    val project_id: Long,
    val repository: GitCommitRepository,
    val commits: List<GitCommit>,
    val total_commits_count: Int
) : GitEvent() {
    companion object {
        const val classType = OBJECT_KIND_TAG_PUSH
    }
}