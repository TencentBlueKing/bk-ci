package com.tencent.devops.common.pipeline.pojo.secret

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import okhttp3.Request

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes(
    JsonSubTypes.Type(value = HeaderSecretParam::class, name = HeaderSecretParam.classType)
)
interface ISecretParam {
    fun secret(builder: Request.Builder)
}