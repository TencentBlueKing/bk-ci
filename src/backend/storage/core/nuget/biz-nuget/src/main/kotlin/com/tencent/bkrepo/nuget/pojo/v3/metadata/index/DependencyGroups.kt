package com.tencent.bkrepo.nuget.pojo.v3.metadata.index

data class DependencyGroups(
    val dependencies: List<Dependency>? = null,
    val targetFramework: String? = null
)
