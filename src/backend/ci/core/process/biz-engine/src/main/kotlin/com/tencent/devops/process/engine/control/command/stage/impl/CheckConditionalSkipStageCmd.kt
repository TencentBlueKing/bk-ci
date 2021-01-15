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

package com.tencent.devops.process.engine.control.command.stage.impl

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.stage.StageCmd
import com.tencent.devops.process.engine.control.command.stage.StageContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Stage的按条件跳过命令处理
 */
@Service
class CheckConditionalSkipStageCmd : StageCmd {

    companion object {
        private val logger = LoggerFactory.getLogger(CheckConditionalSkipStageCmd::class.java)
    }

    override fun canExecute(commandContext: StageContext): Boolean {
        return commandContext.cmdFlowState == CmdFlowState.CONTINUE
    }

    override fun execute(commandContext: StageContext) {
        val stage = commandContext.stage
        // 仅在初次进入Container时进行跳过和依赖判断
        if (stage.status.isReadyToRun() && checkIfSkip(commandContext)) {
            commandContext.buildStatus = BuildStatus.SKIP
            commandContext.latestSummary = "s(${stage.stageId}) skipped"
            commandContext.cmdFlowState = CmdFlowState.FINALLY // 结束其他指令，走最终逻辑返回Stage
        }
    }

    /**
     * 检查[commandContext.stage]是否被按条件跳过
     */
    fun checkIfSkip(commandContext: StageContext): Boolean {
        val stage = commandContext.stage
        val buildId = stage.buildId
        val stageId = stage.stageId
        val stageControlOption = stage.controlOption?.stageControlOption
        if (stageControlOption?.enable == false || commandContext.containers.isEmpty()) { // 无任务
            logger.info("[$buildId]|STAGE_SKIP|s($stageId)|enable=${stageControlOption?.enable}")
            return true
        }

        // condition check
        val variables = commandContext.variables
        var skip = false
        if (stageControlOption != null) {
            val conditions = stageControlOption.customVariables ?: emptyList()
            skip = ControlUtils.checkStageSkipCondition(conditions, variables, buildId, stageControlOption.runCondition)
        }
        if (skip) {
            logger.info("[$buildId]|STAGE_CONDITION_SKIP|s($stageId)|conditions=$stageControlOption")
        }

        return skip
    }
}
