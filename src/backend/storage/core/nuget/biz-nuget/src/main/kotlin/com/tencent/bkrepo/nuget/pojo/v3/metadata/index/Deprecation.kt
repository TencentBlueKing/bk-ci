package com.tencent.bkrepo.nuget.pojo.v3.metadata.index

data class Deprecation(
    val reasons: List<String>,
    val message: String? = null,
    val alternatePackage: AlternatePackage? = null
)
