package com.tencent.devops.dispatch.kubernetes.pojo

data class KubernetesWorkspaceUrlRsp(
    val webVscodeUrl: String,
    val sshUrl: String,
    val apiUrl: String
)
