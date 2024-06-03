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

package com.tencent.devops.process.engine.control.command.container

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.command.CmdContext
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildContainerControlOption
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent

data class ContainerContext(
    val container: PipelineBuildContainer, // 当前容器
    val containerTasks: List<PipelineBuildTask>, // 当前容器下的插件任务列表
    var buildStatus: BuildStatus, // 每次流转最近一次的状态，用于传递到最终容器执行状态
    val event: PipelineBuildContainerEvent, // 当前容器消息事件
    var latestSummary: String, // 最新备注信息，64字符以内
    var cmdFlowState: CmdFlowState = CmdFlowState.CONTINUE, // 当前容器引擎命令流转状态
    val stageMatrixCount: Int = 0,
    var firstQueueTaskId: String? = null, // 缓存找到的第一个待执行的任务（未必执行）
    val pipelineAsCodeEnabled: Boolean? = null,
    var needUpdateControlOption: PipelineBuildContainerControlOption? = null, // 是否需要更新Job设置（超时、互斥组等）
    override var cmdFlowSeq: Int = 0, // 命令序号
    override val variables: Map<String, String>, // 变量
    override val watcher: Watcher, // 监控对象
    override val executeCount: Int = 1 // 执行次数
) : CmdContext(
    executeCount = executeCount,
    variables = variables,
    cmdFlowSeq = cmdFlowSeq,
    watcher = watcher
)
