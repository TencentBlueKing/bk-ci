package com.tencent.devops.remotedev.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectAccessDevicePermissionsResp(
    @ApiModelProperty("出口IP管控权限")
    @JsonProperty("has_ip_permissions")
    val hasIpPermissions: Boolean,
    @ApiModelProperty("用户id")
    @JsonProperty("设备管控权限")
    val hasMacPermissions: Boolean
)
