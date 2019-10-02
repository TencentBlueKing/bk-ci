package com.tencent.devops.process.pojo.scm.code.svn

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Created by liangyuzhou on 2017/9/4.
 * Powered By Tencent
 */
data class SvnCommitEventFile(
    val type: String,
    val file: String,
    val size: Long,
    @JsonProperty("isFile")
    val isFile: Boolean
)