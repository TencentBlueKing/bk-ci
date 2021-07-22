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

import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.engine.common.VMUtils
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
    @Suppress("ComplexMethod")
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
        var parentTaskIndex: Int? = null
        var buildLogPrinter: BuildLogPrinter? = null
        for ((index, pipelineTask) in taskList.withIndex()) {
            if (pipelineTask.taskId == task.taskId) {
                break
            }
            if (pipelineTask.taskId == postInfo.parentElementId) {
                parentTask = pipelineTask
                parentTaskIndex = index
            }
            // 给post任务和其父任务之间的未执行任务加上日志提示
            val endTaskFlag = task.taskId == VMUtils.genStopVMTaskId(task.taskSeq) ||
                task.taskId == VMUtils.genEndPointTaskId(task.taskSeq)
            if (parentTaskIndex != null && !endTaskFlag && pipelineTask.status == BuildStatus.UNEXEC) {
                if (buildLogPrinter == null) {
                    buildLogPrinter = SpringContextUtil.getBean(BuildLogPrinter::class.java)
                }
                buildLogPrinter.addLine(
                    buildId = pipelineTask.buildId,
                    message = "Do not meet the run conditions, ignored.",
                    tag = pipelineTask.taskId,
                    jobId = pipelineTask.containerHashId,
                    executeCount = pipelineTask.executeCount ?: 1
                )
            }
        }
        val runCondition = additionalOptions.runCondition
        val conditionFlag = if (runCondition == RunCondition.PARENT_TASK_CANCELED_OR_TIMEOUT) {
            // 判断父任务是否是取消或者超时状态
            parentTask?.status == BuildStatus.CANCELED || parentTask?.status == BuildStatus.EXEC_TIMEOUT
        } else if (runCondition == RunCondition.PRE_TASK_SUCCESS && !hasFailedTaskInInSuccessContainer) {
            getPreTaskSuccessFlag(taskList, task)
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

    private fun getPreTaskSuccessFlag(
        taskList: List<PipelineBuildTask>,
        task: PipelineBuildTask
    ): Boolean {
        var flag = true
        val taskSize = taskList.size - 1
        for (i in 0..taskSize) {
            val tmpTask = taskList[i]
            if (tmpTask.taskId == task.taskId) {
                return flag
            }
            // 当前插件前面的插件存在失败的情况则返回false
            if (tmpTask.status != BuildStatus.SUCCEED) {
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
    fun getCancelTaskIdRedisKey(buildId: String, containerId: String) = "CANCEL_TASK_IDS_${buildId}_$containerId"
}
