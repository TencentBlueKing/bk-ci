package com.tencent.devops.common.auth.api

import com.fasterxml.jackson.annotation.JsonProperty

data class AuthPermissionVerifyRequest(
    @JsonProperty("project_code")
    val projectCode: String,
    @JsonProperty("service_code")
    val serviceCode: String,
    @JsonProperty("resource_code")
    val resourceCode: String,
    @JsonProperty("policy_code")
    val policyCode: String,
    @JsonProperty("resource_type")
    val resourceType: String,
    @JsonProperty("user_id")
    val userId: String
)