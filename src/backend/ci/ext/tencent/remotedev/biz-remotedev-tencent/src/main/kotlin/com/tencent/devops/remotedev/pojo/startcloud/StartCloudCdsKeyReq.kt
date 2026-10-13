package com.tencent.devops.remotedev.pojo.startcloud

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * CDS openapi: POST /openapi/capi/schedule/cds-key/exchange
 * 通过 cdsId(即 envId) + vmId 换取 cdsKey
 */
data class StartCloudCdsKeyReq(
    @get:JsonProperty("cds_id")
    val cdsId: String,
    @get:JsonProperty("vm_id")
    val vmId: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class StartCloudCdsKeyRespData(
    @JsonProperty("cds_key")
    val cdsKey: String?
)
