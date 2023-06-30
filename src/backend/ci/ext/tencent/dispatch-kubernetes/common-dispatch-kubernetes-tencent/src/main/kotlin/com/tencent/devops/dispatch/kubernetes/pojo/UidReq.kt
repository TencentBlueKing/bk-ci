package com.tencent.devops.dispatch.kubernetes.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class UidReq(
    val uid: String,
    val patch: String? = null,
    val env: String? = null,
    @JsonProperty("delete_cbs")
    val deleteCbs: Boolean? = null
)
