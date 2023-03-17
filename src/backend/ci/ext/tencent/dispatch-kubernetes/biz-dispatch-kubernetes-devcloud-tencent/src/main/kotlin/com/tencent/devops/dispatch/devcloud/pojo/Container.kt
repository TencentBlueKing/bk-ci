package com.tencent.devops.dispatch.devcloud.pojo

data class Container(
    val name: String = "",
    val image: String,
    val command: List<String> = emptyList(),
    val args: List<String> = emptyList(),
    val workingDir: String = "",
    val ports: List<ContainerPort> = emptyList(),
    val env: List<EnvVar> = emptyList(),
    val resource: ResourceRequirements,
    val volumeMounts: List<VolumeMount> = emptyList(),
    val livenessProbe: Probe? = null,
    val readinessProbe: Probe? = null,
    val startupProbe: Probe? = null,
    val lifecycle: Lifecycle? = null,
    val terminationMessagePath: String = "",
    val terminationMessagePolicy: String = "",
    val imagePullPolicy: String = "",
    val securityContext: SecurityContext? = null,
    val stdin: Boolean = true,
    val stdinOnce: Boolean = false,
    val tty: Boolean = false
)
