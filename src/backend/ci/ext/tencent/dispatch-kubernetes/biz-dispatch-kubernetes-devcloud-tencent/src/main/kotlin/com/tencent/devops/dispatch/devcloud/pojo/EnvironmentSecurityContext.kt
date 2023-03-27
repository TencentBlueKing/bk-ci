package com.tencent.devops.dispatch.devcloud.pojo

data class EnvironmentSecurityContext(
    val seLinuxOptions: SELinuxOptions,
    val runAsUser: Long,
    val runAsGroup: Long,
    val runAsNonRoot: Boolean,
    val supplementalGroups: Long,
    val fsGroup: Long,
    val sysctls: Sysctl,
    val fsGroupChangePolicy: String,
    val seccompProfile: SeccompProfile
)
