package com.tencent.devops.common.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class GongfengStatProjVO(
    val id : Int,
    @JsonProperty("bg_id")
    var bgId: Int?,
    @JsonProperty("org_paths")
    var orgPaths: String?,
    val path: String?,
    val description: String?,
    val visibility: String?,
    @JsonProperty("visibility_level")
    val visibilityLevel: Int?,
    val belong: String?,
    val owners: String?,
    @JsonProperty("current_owners")
    val currentOwners : String?,
    @JsonProperty("current_owners_org_paths")
    val currentOwnersOrgPaths : String?,
    @JsonProperty("created_at")
    val createdAt: String?,
    val creator: String?,
    val url: String?,
    val archived: Boolean?,
    @JsonProperty("is_sensitive")
    val isSensitive: Boolean?,
    @JsonProperty("sensitive_reason")
    val sensitiveReason: String?,
    @JsonProperty("public_visibility")
    val publicVisibility: Int?
)