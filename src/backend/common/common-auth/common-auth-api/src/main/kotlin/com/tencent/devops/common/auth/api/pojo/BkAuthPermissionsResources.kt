package com.tencent.devops.common.auth.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class BkAuthPermissionsResources(
    @JsonProperty("policy_code")
    val policyCode: String,
    @JsonProperty("resource_type")
    val resourceType: String,
    @JsonProperty("resource_code_list")
    val resourceCodeList: List<String>
)