package com.tencent.devops.dispatch.devcloud.pojo

data class SecurityContext(
    val capabilities: Capabilities,
    val privileged: Boolean,
    val seLinuxOptions: SELinuxOptions,
    val runAsUser: Long,
    val runAsGroup: Long,
    val runAsNonRoot: Boolean,
    val readOnlyRootFilesystem: Boolean,
    val allowPrivilegeEscalation: Boolean,
    val procMount: String,
    val seccompProfile: SeccompProfile
)
