package com.tencent.bkrepo.nuget.pojo.v3.metadata.index

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RegistrationCatalogEntry(
    @JsonProperty("@id")
    val id: URI,
    // string or array of strings
    val authors: String? = null,
    // The dependencies of the package, grouped by target framework
    val dependencyGroups: List<DependencyGroups>? = null,
    val deprecation: Deprecation? = null,
    val description: String? = null,
    val iconUrl: URI? = null,
    @JsonProperty("id")
    val packageId: String,
    val licenseUrl: URI? = null,
    val licenseExpression: String? = null,
    val listed: Boolean? = null,
    val minClientVersion: String? = null,
    val projectUrl: URI? = null,
    val published: String? = null,
    val requireLicenseAcceptance: Boolean? = null,
    val summary: String? = null,
    // string or array of string
    val tags: List<String>? = null,
    val title: String? = null,
    val version: String,
    val vulnerabilities: Vulnerability? = null
)
