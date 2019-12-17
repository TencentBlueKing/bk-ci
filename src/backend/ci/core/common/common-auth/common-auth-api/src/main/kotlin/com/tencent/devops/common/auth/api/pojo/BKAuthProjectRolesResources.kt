package com.tencent.devops.common.auth.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class BKAuthProjectRolesResources (
    @JsonProperty("display_name")
    val displayName: String,
    @JsonProperty("role_id")
    val roleId: Int,
    @JsonProperty("role_name")
    val roleName: String,
    val type: String
)
