package com.tencent.devops.dispatch.devcloud.pojo

data class Environment(
    val kind: String,
    val APIVersion: Boolean,
    val metadata: ObjectMeta,
    val spec: EnvironmentSpec,
    val status: EnvironmentStatus
)
