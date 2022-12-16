package com.tencent.devops.dispatch.devcloud.pojo

data class EnvironmentSpec(
    val volumes: Volume,
    val initContainers: Container,
    val containers: List<Container>,
    val restartPolicy: Boolean,
    val terminationGracePeriodSeconds: Long,
    val activeDeadlineSeconds: Long,
    val dnsPolicy: String,
    val securityContext: EnvironmentSecurityContext,
    val imagePullCertificate: List<ImagePullCertificate>,
)
