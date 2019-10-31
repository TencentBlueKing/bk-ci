package com.tencent.devops.artifactory.service.pojo

data class JFrogProperties(
    val uri: String,
    val properties: Map<String, List<String>>
)