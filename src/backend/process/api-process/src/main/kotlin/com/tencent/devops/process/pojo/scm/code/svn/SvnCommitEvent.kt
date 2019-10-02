package com.tencent.devops.process.pojo.scm.code.svn

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SvnCommitEvent(
    val userName: String,
    val eventType: Int,
    val log: String,
    val rep_name: String,
    val revision: Int,
    val paths: List<String>,
    val files: List<SvnCommitEventFile>,
    val commitTime: Long?
)