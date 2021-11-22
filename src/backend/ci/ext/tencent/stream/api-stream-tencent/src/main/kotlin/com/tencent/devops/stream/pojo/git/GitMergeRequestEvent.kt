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
    "object_kind": "merge_request",
    "user": {
        "name": "git_user2",
        "username": "git_user2",
        "avatar_url": "https://blog.bobo.com.cn/s/blog_6e572cd60101qls0.html"
    },
    "object_attributes": {
        "id": 23,
        "target_branch": "master",
        "source_branch": "fenzhi01",
        "source_project_id": 2,
        "author_id": 3,
        "assignee_id": 4,
        "title": "agree",
        "created_at": "2018-03-13T09:51:06+0000",
        "updated_at": "2018-03-13T09:51:06+0000",
        "st_commits": null,
        "st_diffs": null,
        "milestone_id": null,
        "state": "opened",
        "merge_status": "unchecked",
        "target_project_id": 14,
        "iid": 1,
        "description": "",
        "source": {
            "name": "rename",
            "ssh_url": "ssh://git@tencent.com/z-413/rename.git",
            "http_url": "http://tencent.com/z-413/rename.git",
            "web_url": "https://tencent.com/z-413/rename",
            "visibility_level": 20,
            "namespace": "z-413"
        },
        "target": {
            "name": "rename",
            "ssh_url": "ssh://git@tencent.com/z-413/rename.git",
            "http_url": "http://tencent.com/z-413/rename.git",
            "web_url": "https://tencent.com/z-413/rename",
            "visibility_level": 20,
            "namespace": "z-413"
        },
        "last_commit": {
            "id": "1480a4610ca01dd10cb5bc30d83151eab98c09a1",
            "message": "fixed readme",
            "timestamp": "2012-01-03T23:36:29+02:00",
            "url": "https://tencent.com/z-413/rename/commits/da1480a4610ca01dd10cb5bc30d83151eab98c09a1",
            "author": {
                "name": "alex",
                "email": "alex@tencent.com"
            }
        },
        "url": "https://tencent.com/rename/merge_requests/1",
        "action": "open",
        "extension_action": "open"
    }
}
*/
@Suppress("ConstructorParameterNaming")
data class GitMergeRequestEvent(
    val user: GitUser,
    val object_attributes: GitMRAttributes
) : GitEvent() {
    companion object {
        const val classType = TGitObjectKind.OBJECT_KIND_MERGE_REQUEST
    }
}

@Suppress("ConstructorParameterNaming")
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
    val target_project_id: Long,
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
