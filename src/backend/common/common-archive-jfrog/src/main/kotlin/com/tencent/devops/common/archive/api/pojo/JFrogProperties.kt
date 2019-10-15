package com.tencent.devops.common.archive.api.pojo

data class JFrogProperties(
    val uri: String,
    val properties: Map<String, List<String>>
)