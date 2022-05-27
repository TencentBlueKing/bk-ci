package com.tencent.bkrepo.nuget.pojo.response.search

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchResponseData(
    @JsonProperty("@id")
    val id: URI,
    val version: String,
    val description: String? = null,
    // All of the versions of the package matching the prerelease parameter
    val versions: List<SearchResponseDataVersion>,
    // string or array of strings
    val authors: List<String>? = null,
    val iconUrl: URI? = null,
    val licenseUrl: URI? = null,
    // string or array of strings
    val owners: List<String>? = null,
    val projectUrl: URI? = null,
    val registration: URI? = null,
    val summary: String? = null,
    // string or array of strings
    val tags: List<String>? = null,
    val title: String? = null,
    val totalDownloads: Int? = null,
    val verified: Boolean? = null,
    val packageTypes: List<SearchResponseDataTypes> = emptyList()
)
