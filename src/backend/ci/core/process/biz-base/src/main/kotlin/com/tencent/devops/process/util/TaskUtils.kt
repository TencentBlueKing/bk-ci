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

package com.tencent.devops.process.util

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildTask

object TaskUtils {

    private val realExecuteBuildStatusList = listOf(
        BuildStatus.SUCCEED,
        BuildStatus.REVIEW_PROCESSED,
        BuildStatus.FAILED,
        BuildStatus.TERMINATE,
        BuildStatus.CANCELED,
        BuildStatus.REVIEW_ABORT,
        BuildStatus.QUALITY_CHECK_FAIL,
        BuildStatus.EXEC_TIMEOUT
    )

    val customConditionList = listOf(
        RunCondition.CUSTOM_VARIABLE_MATCH,
        RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
        RunCondition.CUSTOM_CONDITION_MATCH
    )

    private val failToRunBuildStatusList = listOf(
        RunCondition.PRE_TASK_FAILED_EVEN_CANCEL, // 不管同一Job下前面任务成功/失败/取消都执行
        RunCondition.PRE_TASK_FAILED_BUT_CANCEL, // 不管同一Job下前面任务成功/失败都执行，除了取消不执行
        RunCondition.PRE_TASK_FAILED_ONLY // 同一Job下前面任务有失败才执行
    )

    /**
     * 从当前的容器任务列表[taskList]，容器是否出现运行失败标识[isContainerFailed]，
     * 以及容器上存在允许失败继续的任务[hasFailedTaskInInSuccessContainer],一并计算并返回当前post[task]关联的父任务[PipelineBuildTask]，
     * 如果没有关联的父任务则返回null（异常情况），并一同返回是否允许执行该post[task]标识postExecuteFlag，
     * 非post[task]的postExecuteFlag永远false
     */
    fun getPostTaskAndExecuteFlag(
        taskList: List<PipelineBuildTask>,
        task: PipelineBuildTask,
        isContainerFailed: Boolean,
        hasFailedTaskInInSuccessContainer: Boolean = false
    ): Pair<PipelineBuildTask?, Boolean> {
        var parentTask: PipelineBuildTask? = null
        val additionalOptions = task.additionalOptions
        val postInfo = additionalOptions?.elementPostInfo
        var postExecuteFlag = false
        if (postInfo == null) {
            return parentTask to postExecuteFlag
        }
        val runCondition = additionalOptions.runCondition
        parentTask = taskList.filter { it.taskId == postInfo.parentElementId }.getOrNull(0)
        val conditionFlag = if (runCondition == RunCondition.PARENT_TASK_CANCELED_OR_TIMEOUT) {
            // 判断父任务是否是取消或者超时状态
            parentTask?.status == BuildStatus.CANCELED || parentTask?.status == BuildStatus.EXEC_TIMEOUT
        } else if (runCondition == RunCondition.PARENT_TASK_FINISH) {
            parentTask?.status?.isFinish() ?: false
        } else if (runCondition == RunCondition.PRE_TASK_SUCCESS ||
            runCondition == RunCondition.PRE_TASK_FAILED_BUT_CANCEL
        ) {
            getPreTaskExecuteFlag(taskList, runCondition)
        } else {
            if (isContainerFailed) { // 当前容器有失败的任务
                runCondition in getContinueConditionListWhenFail() // 需要满足[前置任务失败时才运行]或[除了取消才不运行]条件
            } else {
                // 除了[前置任务失败时才运行]的其他条件，或者设置了[前置任务失败时才运行]并且失败并继续的任务
                runCondition != RunCondition.PRE_TASK_FAILED_ONLY || hasFailedTaskInInSuccessContainer
            }
        }
        if (conditionFlag) {
            // 父任务必须是执行过的, 并且在指定的状态下和控制条件
            if (parentTask != null && parentTask.status in realExecuteBuildStatusList) {
                postExecuteFlag = true
            }
        }
        return parentTask to postExecuteFlag
    }

    private fun getPreTaskExecuteFlag(taskList: List<PipelineBuildTask>, runCondition: RunCondition): Boolean {
        var flag = true
        val taskSize = taskList.size - 1
        for (i in 0..taskSize) {
            val tmpTask = taskList[i]
            // 只需判断post任务之前的任务状态
            if (tmpTask.additionalOptions?.elementPostInfo != null) {
                return flag
            }
            val breakFlag = when (runCondition) {
                RunCondition.PRE_TASK_SUCCESS -> {
                    // 当前插件前面的插件存在失败的情况则返回true
                    !tmpTask.status.isSuccess() && tmpTask.status != BuildStatus.UNEXEC &&
                        !ControlUtils.continueWhenFailure(tmpTask.additionalOptions)
                }

                RunCondition.PRE_TASK_FAILED_BUT_CANCEL -> {
                    // 当前插件前面的插件存在取消的情况则返回true
                    tmpTask.status == BuildStatus.CANCELED
                }

                else -> {
                    false
                }
            }
            if (breakFlag) {
                flag = false
                break
            }
        }
        return flag
    }

    /**
     * 从当前的容器任务列表[taskList]，容器是否出现运行失败标识[isContainerFailed]，
     * 以及容器上存在允许失败继续的任务[hasFailedTaskInInSuccessContainer],一并计算并返回是否允许执行该[task]标识
     */
    fun getPostExecuteFlag(
        taskList: List<PipelineBuildTask>,
        task: PipelineBuildTask,
        isContainerFailed: Boolean,
        hasFailedTaskInInSuccessContainer: Boolean = false
    ): Boolean {
        return getPostTaskAndExecuteFlag(taskList, task, isContainerFailed, hasFailedTaskInInSuccessContainer).second
    }

    fun getContinueConditionListWhenFail() = failToRunBuildStatusList

    /**
     * 判断[task]是否为启动构建环境的任务，是返回true
     */
    fun isStartVMTask(task: PipelineBuildTask) = VMUtils.genStartVMTaskId(task.containerId) == task.taskId

    /**
     * 获取当前构建取消任务ID集合的redis键
     */
    fun getCancelTaskIdRedisKey(
        buildId: String,
        containerId: String,
        killFlag: Boolean = true
    ): String {
        return if (killFlag) {
            "CANCEL_TASK_IDS_${buildId}_$containerId"
        } else {
            "CANCEL_TASK_IDS_${buildId}_${containerId}_set"
        }
    }

    /**
     * 获取当前构建插件失败自动重试的redis键
     */
    fun getFailRetryTaskRedisKey(buildId: String, taskId: String): String {
        return "process:task:failRetry:count:$buildId:$taskId"
    }

    /**
     * 是否刷新task的时间
     */
    fun isRefreshTaskTime(
        buildId: String,
        taskId: String,
        additionalOptions: ElementAdditionalOptions?,
        executeCount: Int? = null
    ): Boolean {
        return if (additionalOptions?.retryWhenFailed == true) {
            val redisOperation: RedisOperation = SpringContextUtil.getBean(RedisOperation::class.java)
            val retryCount = redisOperation.get(
                getFailRetryTaskRedisKey(buildId = buildId, taskId = taskId)
            )?.toInt() ?: 0
            retryCount < 1 || (executeCount != null && executeCount > 1)
        } else {
            true
        }
    }

    /**
     * 解析[task]中的[ElementAdditionalOptions]配置的超时设定，做变量替换，并返回日志信息
     */
    fun parseTimeout(task: PipelineBuildTask, contextMap: Map<String, String>): String {
        val timeoutStr = task.additionalOptions?.timeoutVar
        return if (!timeoutStr.isNullOrBlank()) {
            val obj = Timeout.decTimeout(timeoutStr, contextMap)
            task.additionalOptions!!.change = true
            task.additionalOptions!!.timeout = obj.minutes.toLong() // 替换成真正的超时分钟数
            val ele: Element = JsonUtil.mapTo((task.taskParams), Element::class.java)
            ele.additionalOptions = task.additionalOptions
            task.taskParams["additionalOptions"] = JsonUtil.toMap(task.additionalOptions!!) // opt有变需要一起变

            if (obj.change && obj.replaceByVar) {
                "[SystemLog]Task[${task.taskName}] " +
                    "reset illegal timeout var[$timeoutStr=${obj.beforeChangeStr}]: ${obj.minutes} minutes"
            } else if (obj.replaceByVar) {
                "[SystemLog]Task[${task.taskName}] set timeout var[$timeoutStr]: ${obj.minutes} minutes"
            } else if (obj.change) {
                "[SystemLog]Task[${task.taskName}] reset illegal timeout[$timeoutStr]: ${obj.minutes} minutes"
            } else {
                "[SystemLog]Task[${task.taskName}] set timeout: ${obj.minutes} minutes"
            }
        } else {
            "[SystemLog]Task[${task.taskName}] set timeout: ${task.additionalOptions?.timeout} minutes"
        }
    }
}
