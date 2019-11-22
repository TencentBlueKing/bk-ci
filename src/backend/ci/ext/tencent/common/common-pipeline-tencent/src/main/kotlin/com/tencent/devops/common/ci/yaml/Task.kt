package com.tencent.devops.common.ci.yaml

import com.tencent.devops.common.ci.task.AbstractInput

data class Task(
    val task: String,
    val displayName: String,
    val input: List<AbstractInput>
)