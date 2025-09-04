package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

data class Environment(
    val kind: String,
    val APIVersion: String,
    val metadata: ObjectMeta? = null,
    val spec: EnvironmentSpec,
    val status: EnvironmentStatus? = null
)
