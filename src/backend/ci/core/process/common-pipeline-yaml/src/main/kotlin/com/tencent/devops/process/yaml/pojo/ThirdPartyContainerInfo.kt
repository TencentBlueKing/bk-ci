package com.tencent.devops.process.yaml.pojo

data class ThirdPartyContainerInfo(
    val image: String,
    val userName: String?,
    val password: String?,
    val credId: String?,
    val acrossTemplateId: String?
)
