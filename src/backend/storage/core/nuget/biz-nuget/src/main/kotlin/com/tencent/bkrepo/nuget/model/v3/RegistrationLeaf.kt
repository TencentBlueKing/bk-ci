package com.tencent.bkrepo.nuget.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI

data class RegistrationLeaf(
    @JsonProperty("@id")
    val id: URI,
    val catalogEntry: RegistrationCatalogEntry,
    val packageContent: URI,
    val registration: URI
)
