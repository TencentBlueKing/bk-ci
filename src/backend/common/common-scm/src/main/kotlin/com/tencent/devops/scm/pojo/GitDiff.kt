package com.tencent.devops.scm.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitDiff(
    val diff: String,
    val new_path: String,
    val old_path: String,
    val new_file: Boolean,
    val renamed_file: Boolean,
    val deleted_file: Boolean
)