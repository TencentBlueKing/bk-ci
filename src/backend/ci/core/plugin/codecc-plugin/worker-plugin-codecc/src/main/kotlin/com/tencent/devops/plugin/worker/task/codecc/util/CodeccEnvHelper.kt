package com.tencent.devops.plugin.worker.task.codecc.util

import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.codecc.CodeccSDKApi
import java.io.File

object CodeccEnvHelper {

    private val api = ApiFactory.create(CodeccSDKApi::class)

    private val ENV_FILES = arrayOf("result.log", "result.ini")


    fun getCodeccEnv(workspace: File): MutableMap<String, String> {
        val result = mutableMapOf<String, String>()
        ENV_FILES.map { result.putAll(readScriptEnv(workspace, it)) }
        return result
    }

    private fun readScriptEnv(workspace: File, file: String): Map<String, String> {
        val f = File(workspace, file)
        if (!f.exists()) {
            return mapOf()
        }
        if (f.isDirectory) {
            return mapOf()
        }

        val lines = f.readLines()
        if (lines.isEmpty()) {
            return mapOf()
        }
        // KEY-VALUE
        return lines.filter { it.contains("=") }.map {
            val split = it.split("=", ignoreCase = false, limit = 2)
            split[0].trim() to split[1].trim()
        }.toMap()
    }

    fun saveTask(buildVariables: BuildVariables) {
        api.saveTask(buildVariables.projectId, buildVariables.pipelineId, buildVariables.buildId)
    }
}