package com.tencent.devops.common.auth.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class BkAuthPermissionsPolicyCodeAndResourceType(
    @JsonProperty("policy_code")
    val policyCode: String,
    @JsonProperty("resource_type")
    val resourceType: String
)