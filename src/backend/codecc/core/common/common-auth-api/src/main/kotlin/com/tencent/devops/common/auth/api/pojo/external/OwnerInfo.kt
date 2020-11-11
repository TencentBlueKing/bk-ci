package com.tencent.devops.common.auth.api.pojo.external

import com.fasterxml.jackson.annotation.JsonProperty

data class OwnerInfo(
        val id: Int,
        @JsonProperty("username")
        val userName: String,
        @JsonProperty("web_url")
        val webUrl: String,
        val name: String,
        val state: String,
        @JsonProperty("avatar_url")
        val avatarUrl: String,
        @JsonProperty("access_level")
        val accessLevel: Int?
)