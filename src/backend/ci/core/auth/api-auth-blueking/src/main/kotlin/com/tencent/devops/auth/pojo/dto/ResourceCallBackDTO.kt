package com.tencent.devops.auth.pojo.dto

import com.tencent.devops.auth.pojo.Filter
import com.tencent.devops.auth.pojo.Page

data class ResourceCallBackDTO (
    val type: String,
    val method: String,
    val page: Page,
    val filter: Filter?
)