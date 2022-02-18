package com.tencent.devops.turbo.model.pojo

import org.springframework.data.mongodb.core.mapping.Field

data class DisplayFieldEntity(
    // 字段key值
    @Field("field_key")
    val fieldKey: String,
    // 字段名
    @Field("field_name")
    val fieldName: String,
    // 是否链接字段
    @Field("link")
    val link: Boolean? = true,
    // 链接模板
    @Field("link_template")
    val linkTemplate: String?,
    // 链接变量
    @Field("link_variable")
    val linkVariable: Set<String>?
)
