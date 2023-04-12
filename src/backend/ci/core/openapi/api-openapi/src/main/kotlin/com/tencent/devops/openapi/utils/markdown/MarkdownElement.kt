package com.tencent.devops.openapi.utils.markdown

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes(
    JsonSubTypes.Type(value = Code::class, name = Code.classType),
    JsonSubTypes.Type(value = Link::class, name = Link.classType),
    JsonSubTypes.Type(value = Table::class, name = Table.classType),
    JsonSubTypes.Type(value = Text::class, name = Text.classType)
)
open class MarkdownElement(
    open val key: String
)
