package com.tencent.bkrepo.nuget.model.v3

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RegistrationCatalogEntry(
    @JsonProperty("@id")
    val id: URI,
    val authors: String?,
    val dependencyGroups: List<DependencyGroups>?,
    val deprecation: Deprecation?,
    val description: String?,
    val iconUrl: URI?,
    @JsonProperty("id")
    val packageId: String,
    val licenseUrl: URI?,
    val licenseExpression: String?,
    val listed: Boolean?,
    val minClientVersion: String?,
    val projectUrl: URI?,
    val published: String?,
    val requireLicenseAcceptance: Boolean?,
    val summary: String?,
    val tags: List<String>?,
    val title: String?,
    val version: String
)
