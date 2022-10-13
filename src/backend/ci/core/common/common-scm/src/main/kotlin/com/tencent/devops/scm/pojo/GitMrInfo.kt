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

package com.tencent.devops.scm.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel

/*
* {
    "labels": [
        "label1"
    ],
    "id": 645665,
    "title": "dddd",
    "target_project_id": 64762,
    "target_branch": "master",
    "source_project_id": 64762,
    "source_branch": "branch2019090405",
    "state": "reopened",
    "merge_status": "can_be_merged",
    "iid": 30,
    "description": "dddd",
    "created_at": "2019-09-04T07:27:55+0000",
    "updated_at": "2019-09-04T08:31:21+0000",
    "assignee": {
        "id": 66212,
        "username": "user1",
        "web_url": "http://git.xxx.com/u/user1",
        "name": "v_kaizewu",
        "state": "active",
        "avatar_url": "http://git.xxx.com/assets/images/avatar/no_user_avatar.png"
    },
    "author": {
        "id": 16703,
        "username": "user1",
        "web_url": "http://git.xxx.com/u/user1",
        "name": "user1",
        "state": "active",
        "avatar_url": "http://git.xxx.com/assets/images/avatar/no_user_avatar.png"
    },
    "milestone": {
        "id": 3669,
        "project_id": 64762,
        "title": "Milestone1",
        "state": "active",
        "iid": 1,
        "due_date": "2019-09-02",
        "created_at": "2019-09-02T08:58:20+0000",
        "updated_at": "2019-09-02T08:58:20+0000",
        "description": ""
    },
    "necessary_reviewers": null,
    "suggestion_reviewers": null,
    "project_id": 64762,
    "work_in_progress": false,
    "downvotes": 0,
    "upvotes": 0
}
* */

@ApiModel("git mr信息")
data class GitMrInfo(
    val title: String = "",
    @JsonProperty("target_project_id")
    val targetProjectId: String = "",
    @JsonProperty("target_branch")
    val targetBranch: String? = "",
    @JsonProperty("source_project_id")
    val sourceProjectId: String? = "",
    @JsonProperty("source_branch")
    val sourceBranch: String? = "",
    @JsonProperty("created_at")
    val createTime: String? = "",
    @JsonProperty("updated_at")
    val updateTime: String? = "",
    @JsonProperty("iid")
    val mrNumber: String = "",
    @JsonProperty("id")
    val mrId: String = "",
    val labels: List<String>,
    val description: String? = "",
    val assignee: GitMrInfoAssignee? = null,
    val milestone: GitMrInfoMilestone? = null,
    val author: GitMrInfoAuthor = GitMrInfoAuthor(),
    @JsonProperty("base_commit")
    val baseCommit: String? = null,
    @JsonProperty("merge_status")
    val mergeStatus: String? = null,
    @JsonProperty("target_commit")
    val targetCommit: String? = null,
    @JsonProperty("source_commit")
    val sourceCommit: String? = null
) {
    data class GitMrInfoAssignee(
        @JsonProperty("id")
        val id: Int = 0,
        val username: String = "",
        @JsonProperty("web_url")
        val webUrl: String = "",
        @JsonProperty("avatar_url")
        val avatarUrl: String? = ""
    )

    data class GitMrInfoMilestone(
        @JsonProperty("id")
        val id: Int = 0,
        @JsonProperty("title")
        val title: String = "",
        @JsonProperty("due_date")
        val dueDate: String = "",
        val description: String? = ""
    )

    data class GitMrInfoAuthor(
        @JsonProperty("id")
        val id: Int = 0,
        @JsonProperty("username")
        val username: String = "",
        @JsonProperty("web_url")
        val webUrl: String = "",
        @JsonProperty("title")
        val title: String? = "",
        @JsonProperty("avatar_url")
        val avatarUrl: String? = ""
    )
}
