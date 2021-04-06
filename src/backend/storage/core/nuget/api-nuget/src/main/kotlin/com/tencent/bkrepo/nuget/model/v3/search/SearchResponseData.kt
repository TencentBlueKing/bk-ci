package com.tencent.bkrepo.nuget.model.v3.search

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
    @JsonProperty("id")
    var packageId: String?,
    val description: String?,
    val versions: List<SearchResponseDataVersion>,
    val authors: List<String>?,
    val iconUrl: URI?,
    val licenseUrl: URI?,
    val owners: List<String>?,
    val projectUrl: URI?,
    val registration: URI?,
    val summary: String?,
    val tags: List<String>?,
    val title: String?,
    val totalDownloads: Int?,
    val verified: Boolean?,
    val packageTypes: List<String>
)
