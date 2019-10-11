package com.tencent.devops.common.auth.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class BkAuthPermissionsResourcesRequest(
    @JsonProperty("project_code")
    val projectCode: String,
    @JsonProperty("service_code")
    val serviceCode: String,
    @JsonProperty("policy_resource_type_list")
    val policyResourceTypeList: List<BkAuthPermissionsPolicyCodeAndResourceType>,
    @JsonProperty("user_id")
    val userId: String,
    @JsonProperty("is_exact_resource")
    val exactResource: Int = 1
)