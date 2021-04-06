package com.tencent.bkrepo.nuget.model.v3

data class Deprecation(
    val reasons: List<String>?,
    val message: String,
    val alternatePackage: AlternatePackage?
)
