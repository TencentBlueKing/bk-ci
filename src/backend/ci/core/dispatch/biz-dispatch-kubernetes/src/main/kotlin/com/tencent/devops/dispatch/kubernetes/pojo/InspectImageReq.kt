package com.tencent.devops.dispatch.kubernetes.pojo

data class InspectImageReq(
    val name: String,
    val ref: String,
    val cred: InspectImageCredential
)

data class InspectImageCredential(
    val username: String,
    val password: String
)
