package com.tencent.devops.external.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * {
    "ref": "refs/tags/test_v1",
    "node_id": "MDM6UmVmNjEyOTgzNDU6dGVzdF92MQ==",
    "url": "https://api.github.com/repos/irwinsun/goroutine/git/refs/tags/test_v1",
    "object": {
        "sha": "bdd43327c549105f5e1296d65121afbeb0f3f1ef",
        "type": "commit",
        "url": "https://api.github.com/repos/irwinsun/goroutine/git/commits/bdd43327c549105f5e1296d65121afbeb0f3f1ef"
    }
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubTag(
    val ref: String,
    val node_id: String,
    val url: String,
    @JsonProperty("object")
    val tagObject: GithubObject?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubObject(
    val sha: String,
    val type: String,
    val url: String
)
