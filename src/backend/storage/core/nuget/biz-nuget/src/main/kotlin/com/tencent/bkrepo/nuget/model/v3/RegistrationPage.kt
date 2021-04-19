package com.tencent.bkrepo.nuget.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI

data class RegistrationPage(
    @JsonProperty("@id")
    val id: URI,
    val count: Int,
    val items: List<RegistrationLeaf>,
    val lower: String,
    val parent: URI,
    val upper: String
)
