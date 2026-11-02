/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.tencent.devops.common.api.pojo.AtomMonitorData
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildTaskResult
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.utils.BuildVarOverflowUtils
import com.tencent.devops.process.utils.PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX
import com.tencent.devops.process.utils.PIPELINE_VARIABLES_STRING_LENGTH_MAX
import com.tencent.devops.worker.common.heartbeat.Heartbeat
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.service.SensitiveValueService
import com.tencent.devops.worker.common.utils.BatScriptUtil
import com.tencent.devops.worker.common.utils.LargeVariableStore
import com.tencent.devops.worker.common.utils.TaskUtil
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class TaskDaemon(
    private val task: ITask,
    private val buildTask: BuildTask,
    private val buildVariables: BuildVariables,
    private val workspace: File
) : Callable<Map<String, String>> {
    override fun call(): Map<String, String> {
        // 绑定本插件的日志上下文到当前线程：
        // 1) 让插件执行线程内的所有日志归属到正确的elementId；
        // 2) 让插件派生的子线程（例如commons-exec的PumpStreamHandler读取流的线程）
        //    通过InheritableThreadLocal继承上下文，即使主循环已切到下一个插件，
        //    本插件残余的异步日志仍然以本elementId上报，不会污染后续插件。
        LoggerService.setTaskContext(
            LoggerService.TaskExecutionContext(
                elementId = buildTask.taskId ?: LoggerService.elementId,
                stepId = buildTask.stepId ?: LoggerService.stepId,
                elementName = buildTask.elementName
                    ?: buildTask.taskId
                    ?: LoggerService.elementName,
                containerHashId = buildVariables.containerHashId,
                jobId = buildVariables.jobId ?: LoggerService.jobId,
                executeCount = buildTask.executeCount ?: LoggerService.executeCount
            )
        )
        return try {
            task.run(buildTask, buildVariables, workspace)
            task.getAllEnv()
        } catch (e: InterruptedException) {
            task.getAllEnv()
        } finally {
            LoggerService.clearTaskContext()
        }
    }

    fun runWithTimeout() {
        val timeout = TaskUtil.getTimeOut(buildTask)
        val executor = Executors.newCachedThreadPool()
        val taskId = buildTask.taskId
        if (taskId != null) {
            TaskExecutorCache.put(taskId, executor)
        }
        val f1 = executor.submit(this)
        try {
            f1.get(timeout, TimeUnit.MINUTES)
                ?: throw TimeoutException("Task[${buildTask.elementName}] timeout: $timeout minutes")
        } catch (ignore: TimeoutException) {
            throw TaskExecuteException(
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_TASK_OUTTIME_LIMIT,
                errorMsg = ignore.message ?: "Task[${buildTask.elementName}] timeout: $timeout minutes"
            )
        } finally {
            executor.shutdownNow()
            if (taskId != null) {
                TaskExecutorCache.invalidate(taskId)
                Heartbeat.clearTaskProgress(taskId)
            }
        }
    }

    private fun getAllEnv(): Map<String, String> {
        return task.getAllEnv()
    }

    private fun addMonitorData(monitorDataMap: Map<String, Any>) {
        task.addMonitorData(monitorDataMap)
    }

    private fun getMonitorData(): Map<String, Any> {
        return task.getMonitorData()
    }

    private fun getSensitiveKeys(): Set<String> {
        return task.getSensitiveKeys()
    }

    fun getBuildResult(
        isSuccess: Boolean = true,
        errorMessage: String? = null,
        errorType: String? = null,
        errorCode: Int? = 0
    ): BuildTaskResult {

        val allEnv = getAllEnv()
        val buildResult = mutableMapOf<String, String>()
        LargeVariableStore.ensureLargeValueLimit()
        val sensitiveKeys = getSensitiveKeys().toMutableSet()
        if (allEnv.isNotEmpty()) {
            val largeValues = mutableMapOf<String, String>()
            allEnv.forEach { (key, value) ->
                if (SensitiveValueService.matchSensitiveValue(value)) {
                    LoggerService.addWarnLine("Warning, credentials cannot be assigned to variable[$key]")
                    return@forEach
                }
                if (value.length <= PARAM_MAX_LENGTH) {
                    buildResult[key] = value
                } else if (!BuildVarOverflowUtils.exceedsLargeValueLimit(value) &&
                    LargeVariableStore.isSupported()
                ) {
                    // 敏感值也要能传给后续插件，同样存入制品库；日志侧由 SensitiveValueService 覆盖
                    if (key in sensitiveKeys) {
                        SensitiveValueService.addSensitiveValue(value)
                    }
                    largeValues[key] = value
                } else {
                    LoggerService.addWarnLine(
                        "Warning, assignment to variable [$key] failed, " +
                            "more than ${valueLimitText()}(len=${value.length})"
                    )
                }
            }
            buildResult.putAll(storeLargeValues(largeValues))
        }

        // 对内置bat脚本执行失败进行监控
        if (BatScriptUtil.retryTimes() > 0) {
            val extData: MutableMap<String, Any> = getMonitorData()[AtomMonitorData::extData.name]?.let {
                JsonUtil.toMutableMap(it)
            } ?: mutableMapOf()
            extData["task-daemon-bat-auto-retry-times"] = BatScriptUtil.retryTimes()
            addMonitorData(mapOf(AtomMonitorData::extData.name to extData))
            BatScriptUtil.retryClean()
        }
        return BuildTaskResult(
            taskId = buildTask.taskId!!,
            elementId = buildTask.taskId!!,
            containerId = buildVariables.containerHashId,
            elementVersion = buildTask.elementVersion,
            success = isSuccess,
            executeCount = buildTask.executeCount,
            buildResult = buildResult,
            message = errorMessage?.let {
                CommonUtils.interceptStringInLength(
                    string = SensitiveValueService.fixSensitiveContent(it),
                    length = PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX
                )
            },
            type = buildTask.type,
            errorType = errorType,
            errorCode = errorCode,
            platformCode = task.getPlatformCode(),
            platformErrorCode = task.getPlatformErrorCode(),
            monitorData = getMonitorData(),
            sensitiveKeys = sensitiveKeys.takeIf { it.isNotEmpty() }
        )
    }

    /**
     * 在构建机上把超长变量并行上传到制品库，只向引擎上报引用串；上传失败的变量丢弃并告警。
     */
    private fun storeLargeValues(largeValues: Map<String, String>): Map<String, String> {
        if (largeValues.isEmpty()) return emptyMap()
        val references = LargeVariableStore.currentScope()
            ?.let { LargeVariableStore.store(it, buildTask.executeCount ?: 1, largeValues) }
            ?: emptyMap()
        largeValues.forEach { (key, value) ->
            if (references.containsKey(key)) {
                LoggerService.addNormalLine(
                    "Notice, variable [$key] is larger than $PARAM_MAX_LENGTH characters(len=${value.length}), " +
                        "it is stored in artifactory; use \${{ $key }} expression to reference the full value."
                )
            } else {
                LoggerService.addWarnLine(
                    "Warning, assignment to variable [$key] failed, upload large value(len=${value.length}) error"
                )
            }
        }
        return references
    }

    companion object {
        /**
         * 变量值直接上报的最大字符数，与 DB VALUE 列 varchar(4000) 对齐；
         * 超过的非敏感值（不超过 [BuildVarOverflowUtils.largeValueMaxBytes]）作为大变量存入制品库。
         */
        const val PARAM_MAX_LENGTH = PIPELINE_VARIABLES_STRING_LENGTH_MAX

        /** 变量值上限的展示文案：可存入制品库时为大变量上限，否则为直接上报上限。 */
        fun valueLimitText(): String =
            if (LargeVariableStore.isSupported()) {
                "${BuildVarOverflowUtils.largeValueMaxBytes() / 1024}KB"
            } else {
                "$PARAM_MAX_LENGTH characters"
            }
    }
}
