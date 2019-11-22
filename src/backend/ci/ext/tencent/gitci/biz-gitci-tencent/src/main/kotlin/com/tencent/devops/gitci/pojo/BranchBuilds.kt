package com.tencent.devops.gitci.pojo

data class BranchBuilds(
    val branch: String,
    val buildTotal: Long,
    val buildIds: String,
    val eventIds: String
)