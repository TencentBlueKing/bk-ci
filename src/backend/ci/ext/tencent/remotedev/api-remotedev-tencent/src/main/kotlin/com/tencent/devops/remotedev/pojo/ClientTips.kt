package com.tencent.devops.remotedev.pojo

data class ClientTips(
    val id: Long,
    val title: String,
    val content: String,
    val weight: Int
)

data class ClientTipsInfo(
    val id: Long,
    val title: String,
    val content: String,
    val weight: Int,
    val effectiveUsers: Set<String>?,
    val effectiveProjects: Set<String>?
)
