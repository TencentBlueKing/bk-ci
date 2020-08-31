package com.tencent.devops.common.auth.pojo

import com.tencent.bk.sdk.iam.dto.callback.response.AttributeDTO
import com.tencent.devops.common.auth.api.AuthResourceType

data class RelatedResourceTypes(
    val system: String,
    val type: AuthResourceType,
    val instance: List<Instance>? = null,
    val attributes: List<AttributeDTO>? = null
)