package com.tencent.devops.common.auth.pojo

import com.tencent.devops.common.auth.api.AuthResourceType

data class Instance(
    val id: String,
    val type: AuthResourceType
)