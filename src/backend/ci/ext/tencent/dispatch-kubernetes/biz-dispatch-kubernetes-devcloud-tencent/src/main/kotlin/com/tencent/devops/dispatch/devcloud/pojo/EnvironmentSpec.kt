package com.tencent.devops.dispatch.devcloud.pojo

data class EnvironmentSpec(
    val volumes: Volume? = null,
    val initContainers: Container? = null,
    val containers: List<Container>,
    val restartPolicy: Boolean = false,
    val terminationGracePeriodSeconds: Long = 1200,
    val activeDeadlineSeconds: Long = 3600,
    val dnsPolicy: String = "",
    val securityContext: EnvironmentSecurityContext? = null,
    val imagePullCertificate: List<ImagePullCertificate>? = emptyList(),
)
