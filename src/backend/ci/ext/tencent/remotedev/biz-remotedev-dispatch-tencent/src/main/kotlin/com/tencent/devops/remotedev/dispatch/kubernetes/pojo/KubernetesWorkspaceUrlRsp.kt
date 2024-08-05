package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

data class KubernetesWorkspaceUrlRsp(
    val webVscodeUrl: String,
    val sshUrl: String,
    val apiUrl: String
)
