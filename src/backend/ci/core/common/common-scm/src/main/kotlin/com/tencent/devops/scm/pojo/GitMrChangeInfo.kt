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

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel

/*
* {
    "labels": [],
    "id": 683474,
    "title": "--story=855977085 【openapi】获取构建详情加上构建变量列表",
    "target_project_id": 88710,
    "target_branch": "integration",
    "source_project_id": 88710,
    "source_branch": "story_855977085",
    "state": "opened",
    "merge_status": "unchecked",
    "iid": 1438,
    "description": "--story=855977085 【openapi】获取构建详情加上构建变量列表",
    "created_at": "2019-09-19T13:53:20+0000",
    "updated_at": "2019-09-19T14:11:54+0000",
    "assignee": null,
    "author": {
        "id": 23906,
        "username": "user1",
        "web_url": "http://git.xxx.com/u/user1",
        "name": "user1",
        "state": "active",
        "avatar_url": "http://git.xxx.com/assets/images/avatar/no_user_avatar.png"
    },
    "milestone": null,
    "necessary_reviewers": null,
    "suggestion_reviewers": null,
    "files": [
        {
            "old_path": "a/b/c/d.kt",
            "new_path": "a/b/c/d.kt",
            "a_mode": 33188,
            "b_mode": 33188,
            "diff": "",
            "new_file": false,
            "renamed_file": false,
            "deleted_file": false,
            "is_too_large": false,
            "is_collapse": true,
            "additions": 217,
            "deletions": 36
        },
        {
            "old_path": "a/b/c/d2.kt",
            "new_path": "a/b/c/d2.kt",
            "a_mode": 33188,
            "b_mode": 33188,
            "diff": "@@ -52,6 +52,7 @@\n ipelineRuntimeService.    projectId: String,\n",
            "new_file": false,
            "renamed_file": false,
            "deleted_file": false,
            "is_too_large": false,
            "is_collapse": false,
            "additions": 57,
            "deletions": 5
        }
    ],
    "upvotes": 0,
    "downvotes": 0,
    "project_id": 88710,
    "work_in_progress": false
}
* */

@ApiModel("git mr文件变更信息")
data class GitMrChangeInfo(
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
    @JsonAlias("changes")
    val files: List<GitMrFile>? // may be nil
) {
    data class GitMrFile(
        @JsonProperty("old_path")
        val oldPath: String,
        @JsonProperty("new_path")
        val newPath: String,
        @JsonProperty("new_file")
        val newFile: Boolean,
        @JsonProperty("renamed_file")
        val renameFile: Boolean,
        @JsonProperty("deleted_file")
        val deletedFile: Boolean,
        @JsonProperty("is_too_large")
        val isTooLarge: Boolean? = false,
        @JsonProperty("additions")
        val additions: Int? = 0,
        @JsonProperty("deletions")
        val deletions: Int? = 0
    )
}
