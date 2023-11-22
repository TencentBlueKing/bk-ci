package com.tencent.devops.remotedev.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectAccessDevicePermissionsResp(
    @ApiModelProperty(name = "has_ip_permissions", value = "出口IP管控权限")
    @JsonProperty("has_ip_permissions")
    val hasIpPermissions: Boolean,
    @ApiModelProperty(name = "has_mac_permissions", value = "设备管控权限")
    @JsonProperty("has_mac_permissions")
    val hasMacPermissions: Boolean
)
