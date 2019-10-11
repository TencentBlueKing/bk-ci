package com.tencent.devops.repository.pojo.git

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel

/*
*
* {
    "author": {
        "id": 16703,
        "username": "ddlin",
        "web_url": "http://git.code.oa.com/u/ddlin",
        "name": "ddlin",
        "state": "active",
        "avatar_url": "http://git.code.oa.com/assets/images/avatar/no_user_avatar.png"
    },
    "reviewers": [
        {
            "type": "necessary",
            "review_state": "approving",
            "created_at": "2019-09-02T08:58:46+0000",
            "updated_at": "2019-09-02T08:58:46+0000",
            "id": 78458,
            "username": "v_menglu",
            "web_url": "http://git.code.oa.com/u/v_menglu",
            "name": "v_menglu",
            "state": "active",
            "avatar_url": "http://git.code.oa.com/assets/images/avatar/no_user_avatar.png"
        },
        {
            "type": "suggestion",
            "review_state": "approving",
            "created_at": "2019-09-02T08:58:46+0000",
            "updated_at": "2019-09-02T08:58:46+0000",
            "id": 66212,
            "username": "v_kaizewu",
            "web_url": "http://git.code.oa.com/u/v_kaizewu",
            "name": "v_kaizewu",
            "state": "active",
            "avatar_url": "http://git.code.oa.com/assets/images/avatar/no_user_avatar.png"
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

@ApiModel("git mr reviewers信息")
data class GitMrReviewInfo(
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
