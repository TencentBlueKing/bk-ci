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

package com.tencent.devops.process.pojo.record

/**
 * 按执行次数取某个大变量引用串。
 *
 * 同名变量在插件、Job、Stage 上各留各的，所以范围必须带上。都不带时是流水线级，
 * 见 [LargeVariableScope.Pipeline]。
 */
data class LargeVariableRecordQuery(
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val executeCount: Int,
    val varName: String,
    val scope: LargeVariableScope = LargeVariableScope.Pipeline
)

/**
 * 引用串所在的执行记录。越具体的范围越优先，查不到时再退到 [Pipeline]。
 */
sealed class LargeVariableScope {
    /** 插件级。插件产出的大变量写在自己那一行 `TASK_VAR` 上。 */
    data class Task(val taskId: String) : LargeVariableScope()

    /** Job 级。当前没有直接写 `CONTAINER_VAR` 的场景，命中的是 [Pipeline] 快照。 */
    data class Job(val containerId: String) : LargeVariableScope()

    /** Stage 级。当前没有直接写 `STAGE_VAR` 的场景，命中的是 [Pipeline] 快照。 */
    data class Stage(val stageId: String) : LargeVariableScope()

    /**
     * 流水线级。变量表每个 key 只有当前值；这一次执行结束时，仍属于这次执行的引用串
     * 快照进 `T_PIPELINE_BUILD_RECORD_MODEL.MODEL_VAR` 的 `bkLargeVariables`。
     * 不经插件产生的大变量（Stage 审核入参、Job 级互斥信息等）都只在这里。
     * 构建还在跑、快照还没写时，读不到就退回变量表。
     */
    object Pipeline : LargeVariableScope()

    companion object {
        fun of(taskId: String?, containerId: String?, stageId: String?): LargeVariableScope {
            taskId?.takeIf { it.isNotEmpty() }?.let { return Task(it) }
            containerId?.takeIf { it.isNotEmpty() }?.let { return Job(it) }
            stageId?.takeIf { it.isNotEmpty() }?.let { return Stage(it) }
            return Pipeline
        }
    }
}
