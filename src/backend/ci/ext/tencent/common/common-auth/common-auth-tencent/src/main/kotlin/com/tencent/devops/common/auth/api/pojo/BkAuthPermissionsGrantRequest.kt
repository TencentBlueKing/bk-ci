package com.tencent.devops.common.auth.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class BkAuthPermissionsGrantRequest(
    @JsonProperty("project_code")
    val projectCode: String,
    @JsonProperty("service_code")
    val serviceCode: String,
    @JsonProperty("policy_code")
    val policyCode: String,
    @JsonProperty("resource_type")
    val resourceType: String,
    @JsonProperty("resource_code")
    val resourceCode: String,
    @JsonProperty("user_id_list")
    val userIdList: List<String>
)