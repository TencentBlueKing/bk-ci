package com.tencent.devops.common.auth.api.pojo.external.response;

data class AuthMgrResourceResponse(
    val policy: List<AuthTaskPolicy>,
    val role: List<AuthTaskRole>
)