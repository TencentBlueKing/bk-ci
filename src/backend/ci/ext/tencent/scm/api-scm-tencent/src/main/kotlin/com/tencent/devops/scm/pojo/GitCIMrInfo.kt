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
import io.swagger.annotations.ApiModelProperty

/*
* {
    "labels": [],
    "id": 1589745,
    "title": "ruotian-test",
    "target_project_id": 10707958,
    "target_branch": "master",
    "source_project_id": 10707958,
    "source_branch": "ruotian-test",
    "state": "opened",
    "merge_status": "cannot_be_merged",
    "iid": 16,
    "description": "",
    "created_at": "2020-12-26T07:15:26+0000",
    "updated_at": "2020-12-26T07:16:42+0000",
    "resolved_at": null,
    "merge_type": null,
    "assignee": null,
    "author": {
        "id": 100936,
        "username": "ruotiantang",
        "web_url": "http://xxx.com/u/ruotiantang",
        "name": "ruotiantang",
        "state": "active",
        "avatar_url": "http://xxx.com/assets/images/avatar/no_user_avatar.png"
    },
    "merge_commit_sha": null,
    "milestone": null,
    "necessary_reviewers": null,
    "suggestion_reviewers": null,
    "base_commit": "1b798b98f3ea3177cb6a695657eae88b39034f56",
    "target_commit": "c0e7554bdc6fb0b208f9fb7c4fb216bf7f170ab9",
    "source_commit": "8df1e399c7545ab1e63c57ccfc3f90dfe8c4cf29",
    "project_id": 10707958,
    "work_in_progress": false,
    "upvotes": 0,
    "downvotes": 0
}
* */

@ApiModel("git mr信息")
data class GitCIMrInfo(
    val title: String = "",
    @JsonProperty("target_project_id")
    @ApiModelProperty(name = "target_project_id")
    val targetProjectId: String = "",
    @JsonProperty("target_branch")
    @ApiModelProperty(name = "target_branch")
    val targetBranch: String? = "",
    @JsonProperty("source_project_id")
    @ApiModelProperty(name = "source_project_id")
    val sourceProjectId: String? = "",
    @JsonProperty("source_branch")
    @ApiModelProperty(name = "source_branch")
    val sourceBranch: String? = "",
    @JsonProperty("created_at")
    @ApiModelProperty(name = "created_at")
    val createTime: String? = "",
    @JsonProperty("updated_at")
    @ApiModelProperty(name = "updated_at")
    val updateTime: String? = "",
    @JsonProperty("iid")
    @ApiModelProperty(name = "iid")
    val mrNumber: String = "",
    @JsonProperty("id")
    @ApiModelProperty(name = "id")
    val mrId: String = "",
    @JsonProperty("merge_status")
    @ApiModelProperty(name = "merge_status")
    val mergeStatus: String = "",
    val labels: List<String>,
    val description: String? = "",
    @JsonProperty("base_commit")
    @ApiModelProperty(name = "base_commit")
    val baseCommit: String?
)
