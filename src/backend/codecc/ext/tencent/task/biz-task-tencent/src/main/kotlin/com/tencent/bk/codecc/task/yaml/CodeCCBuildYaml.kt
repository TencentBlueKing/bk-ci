package com.tencent.bk.codecc.task.yaml

import com.tencent.devops.common.ci.yaml.Stage

data class CodeCCBuildYaml(
    val variables : Map<String, String>?,
    val stages : List<Stage>?
)