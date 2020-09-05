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
import com.tencent.devops.common.ci.OBJECT_KIND_MERGE_REQUEST

/**
{
    "object_kind":"merge_request",
    "user":{
        "name":"aaronxsheng",
        "username":"aaronxsheng",
        "avatar_url":null
    },
    "object_attributes":{
        "id":238207,
        "target_branch":"master",
        "source_branch":"v1.0.0",
        "source_project_id":48138,
        "author_id":14451,
        "assignee_id":14451,
        "title":"ddd2",
        "created_at":"2018-11-26T06:57:58+0000",
        "updated_at":"2018-11-26T06:57:58+0000",
        "st_commits":null,
        "st_diffs":null,
        "milestone_id":null,
        "state":"opened" / "closed",
        "merge_status":"unchecked",
        "target_project_id":48138,
        "iid":3,
        "description":"ddd",
        "source":{
            "name":"aaron-git-test",
            "ssh_url":"git@git.code.oa.com:aaronxsheng/aaron-git-test.git",
            "http_url":"http://git.code.oa.com/aaronxsheng/aaron-git-test.git",
            "web_url":"http://git.code.oa.com/aaronxsheng/aaron-git-test",
            "namespace":"aaronxsheng",
            "visibility_level":0
        },
        "target":{
            "name":"aaron-git-test",
            "ssh_url":"git@git.code.oa.com:aaronxsheng/aaron-git-test.git",
            "http_url":"http://git.code.oa.com/aaronxsheng/aaron-git-test.git",
            "web_url":"http://git.code.oa.com/aaronxsheng/aaron-git-test",
            "namespace":"aaronxsheng",
            "visibility_level":0
        },
        "last_commit":{
            "id":"1663cbea34521a3c8097046716306d2fbdd9cfea",
            "message":"ddd",
            "timestamp":"2018-11-19T08:41:32+0000",
            "url":"http://git.code.oa.com/aaronxsheng/aaron-git-test/commits/1663cbea34521a3c8097046716306d2fbdd9cfea",
            "author":{
                "name":"aaronxsheng",
                "email":"aaronxsheng@tencent.com"
            }
        },
        "url":"http://git.code.oa.com/aaronxsheng/aaron-git-test/merge_requests/3",
        "action":"open" / "close",
        "extension_action":"push-update"
    }
}
*/
data class GitMergeRequestEvent(
    val user: GitUser,
    val object_attributes: GitMRAttributes
) : GitEvent() {
    companion object {
        const val classType = OBJECT_KIND_MERGE_REQUEST
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitMRAttributes(
    val id: Long,
    val target_branch: String,
    val source_branch: String,
    val source_project_id: Long,
    val author_id: Long,
    val assignee_id: Long,
    val title: String,
    val created_at: String,
    val updated_at: String,
    val state: String,
    val merge_status: String,
    val target_project_id: String,
    val iid: Long,
    val description: String?,
    val source: GitProject,
    val target: GitProject,
    val last_commit: GitCommit,
    val url: String,
    val action: String,
    val extension_action: String
)

// @JsonIgnoreProperties(ignoreUnknown = true)
// data class LastCommit(
//     val id: String,
//     val message: String,
//     val timestamp: String,
//     val url: String,
//     val author: GitCommitAuthor,
//     val title: String,
//     val created_at: String,
//     val updated_at: String,
//     val state: String,
//     val merge_status: String,
//     val target_project_id: String,
//     val iid: Long,
//     val description: String,
//     val source: GitProject,
//     val target: GitProject,
//     val last_commit: GitCommit,
//     val action: String,
//     val extension_action: String
// )
