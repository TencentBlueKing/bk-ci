package com.tencent.devops.dispatch.bcs.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class UidReq(
    val uid: String,
    val env: Map<String, String>? = null,
    @JsonProperty("delete_cbs")
    val deleteCbs: Boolean? = null
)
