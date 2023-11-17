package com.tencent.devops.remotedev.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectAccessDevicePermissionsResp(
    @JsonProperty("access_control")
    val accessControl: Boolean,
    @JsonProperty("has_permissions")
    val hasPermissions: Boolean
)
