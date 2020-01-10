package com.tencent.devops.common.auth.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class BkAuthProjectInfoResources(
    @JsonProperty("cc_app_id")
    val ccAppId: String,
    @JsonProperty("project_code")
    val projectCode: String,
    @JsonProperty("project_id")
    val projectId: String
)