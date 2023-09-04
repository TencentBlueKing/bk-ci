package com.tencent.devops.repository.pojo

import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum

data class OauthParams(
    val gitProjectId: Long?,
    val userId: String,
    val redirectUrlType: RedirectUrlTypeEnum?,
    val redirectUrl: String?
)
