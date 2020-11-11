package com.tencent.devops.common.auth.api.pojo.external.response;

import io.swagger.annotations.ApiModel;

data class AuthMgrResourceResponse(
    val policy: List<AuthTaskPolicy>,
    val role: List<AuthTaskRole>
)