package com.tencent.bkrepo.nuget.pojo.v3.metadata.leaf

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RegistrationLeaf(
    @JsonProperty("@id")
    val id: URI,
    val catalogEntry: URI? = null,
    val listed: Boolean? = null,
    val packageContent: URI? = null,
    val published: String? = null,
    val registration: URI? = null
)
