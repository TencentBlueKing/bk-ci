package com.tencent.devops.common.ci.v2.utils

import com.tencent.devops.common.ci.v2.ScriptBuildYaml
import com.tencent.devops.common.ci.v2.Stage

object YamlDependOnUtils {
    fun parseDependOn(scriptBuildYaml: ScriptBuildYaml) {
        val stageList = listOf<Stage>()
        scriptBuildYaml.stages.forEach {
            it.jobs
        }
    }
}
