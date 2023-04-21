package com.tencent.devops.common.sdk.github.pojo

data class GithubOutput(
    val title: String,
    val summary: String,
    val text: String?,
    val annotations: List<Annotation>?,
    val images: List<Image>?
)
