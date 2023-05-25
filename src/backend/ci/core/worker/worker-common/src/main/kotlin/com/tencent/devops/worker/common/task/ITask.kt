/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.worker.common.task

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.EnvReplacementParser
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.env.BuildEnv
import com.tencent.devops.worker.common.env.BuildType
import com.tencent.devops.worker.common.expression.SpecialFunctions
import java.io.File

@Suppress("NestedBlockDepth", "TooManyFunctions")
abstract class ITask {

    private val environment = HashMap<String, String>()

    private val monitorData = HashMap<String, Any>()

    private var platformCode: String? = null

    private var platformErrorCode: Int? = null

    private var finishKillFlag: Boolean? = null

    fun run(
        buildTask: BuildTask,
        buildVariables: BuildVariables,
        workspace: File
    ) {
        val params = buildTask.params
        val newVariables = combineVariables(buildTask, buildVariables)
        if (params != null && null != params["additionalOptions"]) {
            val additionalOptionsStr = params["additionalOptions"]
            val additionalOptions = JsonUtil.toOrNull(additionalOptionsStr, ElementAdditionalOptions::class.java)
            if (additionalOptions?.enableCustomEnv == true && additionalOptions.customEnv?.isNotEmpty() == true) {
                val variables = buildTask.buildVariable?.toMutableMap()
                val variablesBuild = newVariables.variables.toMutableMap()
                if (variables != null) {
                    additionalOptions.customEnv!!.forEach {
                        if (!it.key.isNullOrBlank()) {
                            // 解决BUG:93319235,将Task的env变量key加env.前缀塞入variables，塞入之前需要对value做替换
                            val value = EnvReplacementParser.parse(
                                value = it.value ?: "",
                                contextMap = variablesBuild,
                                onlyExpression = buildVariables.pipelineAsCodeSettings?.enable,
                                functions = SpecialFunctions.functions,
                                output = SpecialFunctions.output
                            )
                            variablesBuild["envs.${it.key}"] = value
                            variables[it.key!!] = value
                        }
                    }
                    return execute(
                        buildTask.copy(buildVariable = variables),
                        newVariables.copy(variables = variablesBuild),
                        workspace
                    )
                }
            }
        }
        execute(buildTask, newVariables, workspace)
    }

    /**
     *  如果之前的插件不是在构建机执行, 会缺少环境变量
     */
    private fun combineVariables(
        buildTask: BuildTask,
        buildVariables: BuildVariables
    ): BuildVariables {
        val buildVariable = buildTask.buildVariable ?: return buildVariables
        val newVariables = buildVariables.variables.plus(buildVariable)
        val buildParameters = buildVariable.map { (key, value) ->
            BuildParameters(key, value)
        }
        // 以key去重, 并以buildTask中的为准
        val newBuildParameters = buildVariables.variablesWithType.associateBy { it.key }
            .plus(buildParameters.associateBy { it.key })
        return buildVariables.copy(variables = newVariables, variablesWithType = newBuildParameters.values.toList())
    }

    protected abstract fun execute(
        buildTask: BuildTask,
        buildVariables: BuildVariables,
        workspace: File
    )

    protected fun addEnv(env: Map<String, String>) {
        environment.putAll(env)
    }

    protected fun addEnv(key: String, value: String) {
        environment[key] = value
    }

    protected fun getEnv(key: String) =
        environment[key] ?: ""

    fun getAllEnv(): Map<String, String> {
        return environment
    }

    protected fun addMonitorData(monitorDataMap: Map<String, Any>) {
        monitorData.putAll(monitorDataMap)
    }

    fun getMonitorData(): Map<String, Any> {
        return monitorData
    }

    protected fun addPlatformCode(taskPlatformCode: String) {
        platformCode = taskPlatformCode
    }

    fun getPlatformCode(): String? {
        return platformCode
    }

    protected fun addPlatformErrorCode(taskPlatformErrorCode: Int) {
        platformErrorCode = taskPlatformErrorCode
    }

    fun getPlatformErrorCode(): Int? {
        return platformErrorCode
    }

    protected fun addFinishKillFlag(taskFinishKillFlag: Boolean) {
        finishKillFlag = taskFinishKillFlag
    }

    fun getFinishKillFlag(): Boolean? {
        return finishKillFlag
    }

    protected fun isThirdAgent() = BuildEnv.getBuildType() == BuildType.AGENT
}
