package com.tencent.bkrepo.nuget.model.v2.search

data class NuGetSearchRequest(
    val filter: String? = null,
    val orderBy: String? = null,
    val skip: String? = null,
    val top: String? = null,
    val searchTerm: String? = null,
    val targetFramework: String? = null,
    val targetFrameworks: String? = null,
    val versionConstraints: String? = null,
    val includePreRelease: Boolean = false,
    val skipToken: String? = null,
    val select: String? = null,
    val inlinecount: String? = null,
    val expand: String? = null,
    val id: String? = null,
    val packageIds: String? = null,
    val versions: String? = null,
    val includeAllVersions: Boolean = false,
    val semVerLevel: String? = null
)
