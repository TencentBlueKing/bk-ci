package com.tencent.devops.plugin.worker.task.codecc.util

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.plugin.codecc.pojo.coverity.CoverityConfig
import java.io.File

class Coverity constructor(private val coverityConfig: CoverityConfig) {

    fun coverity(scriptType: BuildScriptType, buildId: String, file: File, workspace: File): String {
        return when (scriptType) {
            BuildScriptType.SHELL ->
                CodeccUtils.executeCoverityCommand(buildId, workspace, coverityConfig)
            else ->
                throw InvalidParamException("unsupported scriptype $scriptType")
        }
    }
}