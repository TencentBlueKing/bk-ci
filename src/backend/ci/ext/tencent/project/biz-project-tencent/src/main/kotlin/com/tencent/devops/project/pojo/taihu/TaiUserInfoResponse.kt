package com.tencent.devops.project.pojo.taihu

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TaiUserInfoResponse(
    val data: List<TaiUserInfo>
)
