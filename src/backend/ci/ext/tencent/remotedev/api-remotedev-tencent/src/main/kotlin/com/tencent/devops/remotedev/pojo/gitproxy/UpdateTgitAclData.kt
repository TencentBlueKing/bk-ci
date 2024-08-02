package com.tencent.devops.remotedev.pojo.gitproxy

data class UpdateTgitAclIpData(
    val projectId: String,
    val ips: Set<String>,
    val remove: Boolean,
    val tgitId: Long?
)

data class UpdateTgitAclUserData(
    val projectId: String,
    val tgitId: Long?
)
