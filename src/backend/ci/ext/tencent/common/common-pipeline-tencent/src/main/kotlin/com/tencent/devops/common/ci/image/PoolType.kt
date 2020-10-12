package com.tencent.devops.common.ci.image

enum class PoolType {
    DockerOnVm,

    DockerOnDevCloud,

    DockerOnPcg,

    Windows,

    Macos,

    SelfHosted

    ;
}