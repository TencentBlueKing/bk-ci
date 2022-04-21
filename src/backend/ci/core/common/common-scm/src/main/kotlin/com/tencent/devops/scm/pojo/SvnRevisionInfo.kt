package com.tencent.devops.scm.pojo

data class SvnRevisionInfo(
    val revision: String,
    val branchName: String,
    val authorName: String,
    val commitTime: Long,
    val paths: List<String>
)
