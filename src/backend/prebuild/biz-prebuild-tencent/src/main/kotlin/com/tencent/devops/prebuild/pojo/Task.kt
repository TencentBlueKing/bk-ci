package com.tencent.devops.prebuild.pojo

data class Task(
    val task: String,
    val displayName: String,
    val input: List<AbstractInput>
)