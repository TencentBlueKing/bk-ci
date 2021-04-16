package com.tencent.bkrepo.nuget.model.v3

data class DependencyGroups(
    val dependencies: List<Dependency>?,
    val targetFramework: String?
)
