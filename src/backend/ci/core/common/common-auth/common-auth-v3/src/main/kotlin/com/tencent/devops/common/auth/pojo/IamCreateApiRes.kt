package com.tencent.devops.common.auth.pojo

data class IamCreateApiRes(
    val result: Boolean,
    val code: Int,
    val message: String,
    val data: List<ActionPolicyRes>,
    val request_id: String
)