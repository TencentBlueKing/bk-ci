package com.tencent.devops.project.pojo.taihu

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TaiUserInfoRequest(
    val usernames: Set<String>
)
