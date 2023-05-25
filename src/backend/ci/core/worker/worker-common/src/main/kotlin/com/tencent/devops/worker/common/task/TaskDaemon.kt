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
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildTaskResult
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.utils.PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.service.SensitiveValueService
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
        return try {
            task.run(buildTask, buildVariables, workspace)
            task.getAllEnv()
        } catch (e: InterruptedException) {
            task.getAllEnv()
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
            }
        }
    }

    private fun getAllEnv(): Map<String, String> {
        return task.getAllEnv()
    }

    private fun getMonitorData(): Map<String, Any> {
        return task.getMonitorData()
    }

    fun getBuildResult(
        isSuccess: Boolean = true,
        errorMessage: String? = null,
        errorType: String? = null,
        errorCode: Int? = 0
    ): BuildTaskResult {

        val allEnv = getAllEnv()
        val buildResult = mutableMapOf<String, String>()
        if (allEnv.isNotEmpty()) {
            allEnv.forEach { (key, value) ->
                if (value.length > PARAM_MAX_LENGTH) {
                    LoggerService.addWarnLine(
                        "Warning, assignment to variable [$key] failed, " +
                            "more than $PARAM_MAX_LENGTH characters(len=${value.length})"
                    )
                    return@forEach
                }
                if (SensitiveValueService.matchSensitiveValue(value)) {
                    LoggerService.addWarnLine("Warning, credentials cannot be assigned to variable[$key]")
                    return@forEach
                }
                buildResult[key] = value
            }
        }

        return BuildTaskResult(
            taskId = buildTask.taskId!!,
            elementId = buildTask.taskId!!,
            containerId = buildVariables.containerHashId,
            elementVersion = buildTask.elementVersion,
            success = isSuccess,
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
            monitorData = getMonitorData()
        )
    }

    companion object {
        private const val PARAM_MAX_LENGTH = 4000 // 流水线参数最大长度
    }
}
