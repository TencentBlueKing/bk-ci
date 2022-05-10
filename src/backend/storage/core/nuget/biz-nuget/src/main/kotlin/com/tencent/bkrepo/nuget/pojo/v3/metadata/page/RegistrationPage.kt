package com.tencent.bkrepo.nuget.pojo.v3.metadata.page

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.bkrepo.nuget.pojo.v3.metadata.index.RegistrationPageItem
import java.net.URI

data class RegistrationPage(
    @JsonProperty("@id")
    val id: URI,
    val count: Int,
    val items: List<RegistrationPageItem>,
    val lower: String,
    val parent: URI,
    val upper: String
)
