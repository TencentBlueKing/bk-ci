package com.tencent.bkrepo.nuget.pojo.v3.metadata.index

import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI

data class RegistrationPageItem(
    @JsonProperty("@id")
    val id: URI,
    val catalogEntry: RegistrationCatalogEntry,
    val packageContent: URI
)
