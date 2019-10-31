package com.tencent.devops.gitci.pojo.yaml

import com.tencent.devops.gitci.pojo.task.AbstractTask

/**
 * model
 */
data class CIBuildYaml(
    val trigger: Trigger?,
    val mr: MergeRequest?,
    val variables: Map<String, String>?,
    val stages: List<Stage>?,
    val steps: List<AbstractTask>?
)