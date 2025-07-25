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

package com.tencent.devops.process.engine.control.command.stage

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.command.CmdContext
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStageEvent

/**
 * Stage 上下文
 */
data class StageContext(
    val stage: PipelineBuildStage, // 当前Stage
    val containers: List<PipelineBuildContainer>, // 当前Stage下的容器列表
    var buildStatus: BuildStatus, // 每次流转最近一次的状态，用于传递到最终Stage执行状态
    val event: PipelineBuildStageEvent, // 当前Stage消息事件
    var latestSummary: String, // 最新备注信息，64字符以内
    var fastKill: Boolean = false, // 快速失败标志
    var cmdFlowState: CmdFlowState = CmdFlowState.CONTINUE, // 当前Stage引擎命令流转状态
    var cancelContainerNum: Int = 0,
    var failureContainerNum: Int = 0,
    var skipContainerNum: Int = 0,
    var previousStageStatus: BuildStatus? = null, // 上一个Stage的状态
    var concurrency: Int = 0,
    val maxConcurrency: Int = 20, // #5109 并发控制埋点准备
    val pipelineAsCodeEnabled: Boolean? = null, // YAML流水线功能开关
    // Agent复用互斥的最后一波job，如果是不为空且为0就可以解锁互斥锁
    var agentReuseMutexEndJob: MutableMap<String, Int>? = null,
    val debug: Boolean, // 是否为调试构建
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
