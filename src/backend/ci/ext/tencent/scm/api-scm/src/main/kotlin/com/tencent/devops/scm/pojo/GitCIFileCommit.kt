package com.tencent.devops.scm.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel

/**
 * {
        "commit": {
            "id": "f268ea920fca4451f5ee4f44dbe82a0189d42e6b",
            "message": "1",
            "parent_ids": [
                "2eafa1f4c3ba82fea05dc8a36ac582231531bcab"
            ],
            "authored_date": "2020-12-29T08:10:48+0000",
            "author_name": "ruotiantang",
            "author_email": "ruotiantang@tencent.com",
            "committed_date": "2020-12-29T08:10:48+0000",
            "committer_name": "ruotiantang",
            "committer_email": "ruotiantang@tencent.com",
            "title": "1",
            "scroll_object_id": null,
            "short_id": "f268ea92",
            "created_at": "2020-12-29T08:10:48+0000"
        },
        "lines": [
            "11112233",
            ""
        ]
    }
 */
@ApiModel("gitci 文件的提交信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitCIFileCommit(
    val commit: Commit,
    val lines: List<String>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Commit(
    @JsonProperty("author_email")
    val authorEmail: String?,
    @JsonProperty("author_name")
    val authorName: String?,
    @JsonProperty("authored_date")
    val authoredDate: String?,
    @JsonProperty("committed_date")
    val committedDate: String?,
    @JsonProperty("committer_email")
    val committerEmail: String?,
    @JsonProperty("committer_name")
    val committerName: String?,
    @JsonProperty("created_at")
    val createdAt: String,
    val id: String,
    val message: String?,
    @JsonProperty("parent_ids")
    val parentIds: List<String>?,
    @JsonProperty("short_id")
    val shortId: String?,
    val title: String?,
    @JsonProperty("scroll_object_id")
    val scrollObjectId: Any?
)
