package com.tencent.devops.common.ci

data class CiBuildConfig(
    val codeCCSofwarePath: String?,
    val registryHost: String?,
    val registryUserName: String?,
    val registryPassword: String?,
    val registryImage: String?,
    val cpu: Int,
    val memory: String,
    val disk: String,
    val volume: Int,
    val activeDeadlineSeconds: Int,
    val devCloudAppId: String,
    val devCloudToken: String,
    val devCloudUrl: String
)