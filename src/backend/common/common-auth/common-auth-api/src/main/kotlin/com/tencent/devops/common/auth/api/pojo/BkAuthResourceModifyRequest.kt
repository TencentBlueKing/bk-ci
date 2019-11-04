package com.tencent.devops.common.auth.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class BkAuthResourceModifyRequest(
    @JsonProperty("project_Code")
    val projectCode: String,
    @JsonProperty("service_code")
    val serviceCode: String,
    @JsonProperty("resource_code")
    val resourceCode: String,
    @JsonProperty("resource_name")
    val resourceName: String,
    @JsonProperty("resource_type")
    val resourceType: String
)