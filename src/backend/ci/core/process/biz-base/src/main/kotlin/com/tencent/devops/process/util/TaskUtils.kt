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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.util

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.process.engine.pojo.PipelineBuildTask

object TaskUtils {

    fun getPostExecuteFlag(
        taskList: List<PipelineBuildTask>,
        task: PipelineBuildTask,
        isContainerFailed: Boolean,
        hasFailedTaskInInSuccessContainer: Boolean? = null
    ): Boolean {
        val additionalOptions = task.additionalOptions
        val elementPostInfo = additionalOptions?.elementPostInfo
        var postExecuteFlag = false
        if (elementPostInfo != null) {
            val runCondition = additionalOptions.runCondition
            val conditionFlag = if (isContainerFailed) {
                runCondition in getContinueConditionListWhenFail()
            } else {
                runCondition != RunCondition.PRE_TASK_FAILED_ONLY ||
                    (additionalOptions.runCondition == RunCondition.PRE_TASK_FAILED_ONLY &&
                        hasFailedTaskInInSuccessContainer == true)
            }
            val realExecuteBuildStatusList = listOf(
                BuildStatus.SUCCEED,
                BuildStatus.REVIEW_PROCESSED,
                BuildStatus.FAILED,
                BuildStatus.TERMINATE,
                BuildStatus.CANCELED,
                BuildStatus.REVIEW_ABORT,
                BuildStatus.QUALITY_CHECK_FAIL,
                BuildStatus.EXEC_TIMEOUT
            )
            run lit@{
                taskList.forEach { taskExecute ->
                    val taskExecuteBuildStatus = taskExecute.status
                    // post任务的母任务必须是执行过的
                    if (taskExecute.taskId == elementPostInfo.parentElementId &&
                        taskExecuteBuildStatus in realExecuteBuildStatusList &&
                        conditionFlag
                    ) {
                        postExecuteFlag = true
                        return@lit
                    }
                }
            }
        }
        return postExecuteFlag
    }

    fun getContinueConditionListWhenFail(): List<RunCondition> {
        return listOf(
            RunCondition.PRE_TASK_FAILED_BUT_CANCEL,
            RunCondition.PRE_TASK_FAILED_ONLY
        )
    }
}
