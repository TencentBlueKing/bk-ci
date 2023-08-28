package com.tencent.devops.remotedev.pojo.software

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SoftwareCreate(
    @JsonProperty("ip")
    val ip: String,
    @JsonProperty("software_information")
    val softwareInfo: List<SoftwareInfo>,
    @JsonProperty("username")
    val username: String,
    @JsonProperty("callback_url")
    val callbackUrl: String? = null
)
