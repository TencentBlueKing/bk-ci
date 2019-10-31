package com.tencent.devops.gitci.pojo.yaml

import com.tencent.devops.gitci.pojo.task.AbstractInput

data class Task(
    val task: String,
    val displayName: String,
    val input: List<AbstractInput>
)