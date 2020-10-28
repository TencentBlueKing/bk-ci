package com.tencent.devops.dispatch.docker.pojo

data class SpecialDockerHostVO(
    val projectId: String,
    val hostIp: String,
    val remark: String?
)