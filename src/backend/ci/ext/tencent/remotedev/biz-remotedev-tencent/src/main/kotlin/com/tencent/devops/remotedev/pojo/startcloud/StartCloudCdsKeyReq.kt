package com.tencent.devops.remotedev.pojo.startcloud

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * CDS openapi: POST /openapi/capi/schedule/cds-key/apply
 * 按 cdsId(即 envId) + vmId 申请 cdsKey，同一 vmId + cdsId 重复申请返回已有 key
 */
data class StartCloudApplyCdsKeyReq(
    @get:JsonProperty("cds_id")
    val cdsId: String,
    @get:JsonProperty("cds_vm_id")
    val cdsVmId: String
)

/**
 * apply 接口返回为平铺结构：{"cds_id","cds_key","code","message"}，没有 data 包裹
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class StartCloudApplyCdsKeyResp(
    @JsonProperty("cds_id")
    val cdsId: String?,
    @JsonProperty("cds_key")
    val cdsKey: String?,
    val code: Int,
    val message: String?
)
