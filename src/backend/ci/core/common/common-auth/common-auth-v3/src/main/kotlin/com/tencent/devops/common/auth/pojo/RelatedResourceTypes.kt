package com.tencent.devops.common.auth.pojo

import com.tencent.bk.sdk.iam.dto.callback.response.AttributeDTO

data class RelatedResourceTypes(
    val system: String,
    val type: String,
    val instances: List<List<Instance>>? = emptyList(),
    val attributes: List<AttributeDTO>? = emptyList()
)