package com.tencent.devops.turbo.model.pojo

import org.springframework.data.mongodb.core.mapping.Field

data class ParamEnumEntity(
    @Field("param_value")
    val paramValue: Any,
    @Field("param_name")
    val paramName: String,
    @Field("visual_range")
    val visualRange: List<String> = listOf()
)
