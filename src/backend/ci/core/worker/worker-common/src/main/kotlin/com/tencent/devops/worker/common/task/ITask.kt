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

import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.env.BuildEnv
import com.tencent.devops.worker.common.env.BuildType
import com.tencent.devops.worker.common.logger.LoggerService
import java.io.File
import java.util.stream.Collectors
import org.slf4j.LoggerFactory

@Suppress("NestedBlockDepth", "TooManyFunctions")
abstract class ITask {

    private val logger = LoggerFactory.getLogger(ITask::class.java)

    private val environment = HashMap<String, String>()

    private val monitorData = HashMap<String, Any>()

    private var platformCode: String? = null

    private var platformErrorCode: Int? = null

    private var finishKillFlag: Boolean? = null

    /* 存储常量的key */
    private lateinit var constVar: List<String>

    fun run(
        buildTask: BuildTask,
        buildVariables: BuildVariables,
        workspace: File
    ) {
        constVar = buildVariables.variablesWithType.stream()
            .filter { it.readOnly == true }
            .map { it.key }
            .collect(Collectors.toList())
        execute(buildTask, buildVariables, workspace)
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
        if (this::constVar.isInitialized) {
            var errFlag = false
            env.forEach { (key, _) ->
                if (key in constVar) {
                    LoggerService.addErrorLine("variable $key is readonly, can't add again.")
                    errFlag = true
                }
            }
            if (errFlag) {
                throw TaskExecuteException(
                    errorMsg = "[Finish task] status: false, errorType: ${ErrorType.USER.num}, " +
                        "errorCode: ${ErrorCode.USER_INPUT_INVAILD}, message: can't add readonly variable again.",
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_INPUT_INVAILD
                )
            }
        }
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
