package com.tencent.devops.common.ci.yaml

import com.tencent.devops.common.ci.service.AbstractService
import com.tencent.devops.common.ci.task.AbstractTask

/**
 * model
 */
data class CIBuildYaml(
    val trigger: Trigger?,
    val mr: MergeRequest?,
    val variables: Map<String, String>?,
    val services: List<AbstractService>?,
    val stages: List<Stage>?,
    val steps: List<AbstractTask>?
)