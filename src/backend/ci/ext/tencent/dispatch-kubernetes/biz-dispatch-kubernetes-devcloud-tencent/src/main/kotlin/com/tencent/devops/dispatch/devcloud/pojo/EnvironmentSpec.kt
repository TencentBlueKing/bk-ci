package com.tencent.devops.dispatch.devcloud.pojo

data class EnvironmentSpec(
    val volumes: List<Volume> = emptyList(),
    val initContainers: List<Container> = emptyList(),
    val containers: List<Container>,
    val restartPolicy: String = "",
    val terminationGracePeriodSeconds: Long = 1200,
    val activeDeadlineSeconds: Long = 3600,
    val dnsPolicy: String = "",
    val securityContext: EnvironmentSecurityContext? = null,
    val imagePullCertificate: List<ImagePullCertificate>? = emptyList(),
)
