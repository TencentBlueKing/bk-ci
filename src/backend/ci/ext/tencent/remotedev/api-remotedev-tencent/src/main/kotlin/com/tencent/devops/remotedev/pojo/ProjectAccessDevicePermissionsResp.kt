package com.tencent.devops.remotedev.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectAccessDevicePermissionsResp(
    @JsonProperty("has_ip_permissions")
    val hasIpPermissions: Boolean,
    @JsonProperty("has_mac_permissions")
    val hasMacPermissions: Boolean
)
