package com.tencent.devops.dispatch.devcloud.pojo

data class Container(
    val name: String,
    val image: String,
    val command: List<String>,
    val args: List<String>,
    val workingDir: String,
    val ports: List<ContainerPort>,
    val env: List<EnvVar>,
    val resource: ResourceRequirements,
    val volumeMounts: List<VolumeMount>,
    val livenessProbe: Probe,
    val readinessProbe: Probe,
    val startupProbe: Probe,
    val lifecycle: Lifecycle,
    val terminationMessagePath: String,
    val terminationMessagePolicy: String,
    val imagePullPolicy: String,
    val securityContext: SecurityContext,
    val stdin: Boolean,
    val stdinOnce: Boolean,
    val tty: Boolean
)
