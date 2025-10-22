/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import io.swagger.v3.oas.annotations.media.Schema

/*
*
* {
    "author": {
        "id": 16703,
        "username": "user23",
        "web_url": "http://git.xsxx.com/u/user23",
        "name": "user23",
        "state": "active",
        "avatar_url": "http://git.xsxx.com/assets/images/avatar/no_user_avatar.png"
    },
    "reviewers": [
        {
            "type": "necessary",
            "review_state": "approving",
            "created_at": "2019-09-02T08:58:46+0000",
            "updated_at": "2019-09-02T08:58:46+0000",
            "id": 78458,
            "username": "user2",
            "web_url": "http://git.xsxx.com/u/user2",
            "name": "user2",
            "state": "active",
            "avatar_url": "http://git.xsxx.com/assets/images/avatar/no_user_avatar.png"
        },
        {
            "type": "suggestion",
            "review_state": "approving",
            "created_at": "2019-09-02T08:58:46+0000",
            "updated_at": "2019-09-02T08:58:46+0000",
            "id": 66212,
            "username": "user1",
            "web_url": "http://git.xsxx.com/u/user1",
            "name": "user1",
            "state": "active",
            "avatar_url": "http://git.xsxx.com/assets/images/avatar/no_user_avatar.png"
        }
    ],
    "id": 707707,
    "project_id": 64762,
    "reviewable_id": 639940,
    "reviewable_type": "merge_request",
    "iid": 27,
    "state": "approving",
    "approver_rule": 1,
    "necessary_approver_rule": 0,
    "push_reset_enabled": false,
    "created_at": "2019-09-02T08:58:46+0000",
    "updated_at": "2019-09-02T08:58:46+0000"
}
* */

@Schema(title = "git mr reviewers信息")
data class GitMrReviewInfo(
    val author: GitMrInfoReviewer?,
    @JsonProperty("project_id")
    val projectId: Long?,
    @JsonProperty("reviewable_id")
    val reviewableId: Long?,
    @JsonProperty("reviewable_type")
    val reviewableType: String?,
    val state: String?,
    @JsonProperty("created_at")
    val createTime: String? = "",
    @JsonProperty("updated_at")
    val updateTime: String? = "",
    @JsonProperty("iid")
    val mrNumber: String = "",
    @JsonProperty("id")
    val mrId: String = "",
    val reviewers: List<GitMrInfoReviewer>
) {

    data class GitMrInfoReviewer(
        @JsonProperty("id")
        val id: Int = 0,
        @JsonProperty("username")
        val username: String = "",
        @JsonProperty("web_url")
        val webUrl: String = "",
        @JsonProperty("state")
        val title: String = "",
        @JsonProperty("avatar_url")
        val avatarUrl: String = ""
    )
}
