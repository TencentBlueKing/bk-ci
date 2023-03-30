package com.tencent.devops.dispatch.codecc.pojo.devcloud

data class DevCloudImage(
    val name: String,
    val version: String,
    val description: String?,
    val shareMode: String?,
    val shareRange: List<String>?,
    val params: ImageParams
)

data class ImageParams(
    val container: Container,
    val registry: Registry
)

data class Container(
    val name: String
)

data class DevCloudImageVersion(
    val version: String,
    val description: String?,
    val params: ImageParams
)
