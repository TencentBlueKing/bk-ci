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

package com.tencent.devops.process.engine.context

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.process.TestBase
import com.tencent.devops.process.engine.cfg.BuildIdGenerator
import com.tencent.devops.process.engine.cfg.PipelineIdGenerator
import com.tencent.devops.process.pojo.app.StartBuildContext
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.process.utils.PIPELINE_RETRY_START_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_SKIP_FAILED_TASK
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StartBuildContextTest : TestBase() {

    private val params = mutableMapOf<String, String>()
    private val projectId = "projectId"
    private val pipelineId = PipelineIdGenerator().getNextId()
    private val buildId = BuildIdGenerator().getNextId()
    private val version = 1

    @BeforeEach
    fun setUp2() {
        params.clear()
        params[PIPELINE_START_USER_NAME] = "your name"
        params[PIPELINE_START_USER_ID] = "id123"
        params[PIPELINE_START_TYPE] = StartType.MANUAL.name
        params[PIPELINE_RETRY_START_TASK_ID] = "e-12345678901234567890123456789012"
        params[PIPELINE_RETRY_COUNT] = "1"
        params[PIPELINE_START_PARENT_BUILD_ID] = "1"
        params[PIPELINE_START_PARENT_BUILD_TASK_ID] = "12"
        params[PIPELINE_START_CHANNEL] = ChannelCode.BS.name
    }

    @Test
    fun needSkipWhenStageFailRetry() {
        params.remove(PIPELINE_RETRY_START_TASK_ID)
        params.remove(PIPELINE_RETRY_COUNT)
        val context = initDefaultStartBuildContext()
        Assertions.assertEquals(params, context.variables)
        val stage = genStages(stageSize = 2, jobSize = 2, elementSize = 2, needFinally = false)[1]
        val needSkipWhenStageFailRetry = context.needSkipWhenStageFailRetry(stage)
        println("needSkipWhenStageFailRetry=$needSkipWhenStageFailRetry")
        Assertions.assertEquals(false, needSkipWhenStageFailRetry)
    }

    @Test
    fun needSkipContainerWhenFailRetry() {
        params[PIPELINE_RETRY_COUNT] = "abc"
        var context = initDefaultStartBuildContext()
        Assertions.assertEquals(params, context.variables)
        Assertions.assertEquals(context.executeCount, 1)

        params[PIPELINE_RETRY_COUNT] = "2"
        context = initDefaultStartBuildContext()
        Assertions.assertEquals(context.executeCount, params[PIPELINE_RETRY_COUNT].toString().toInt() + 1)

        params[PIPELINE_START_CHANNEL] = ChannelCode.AM.name
        context = initDefaultStartBuildContext()
        Assertions.assertEquals(ChannelCode.AM, context.channelCode)
        params.remove(PIPELINE_START_CHANNEL)

        context = initDefaultStartBuildContext()
        Assertions.assertEquals(ChannelCode.BS, context.channelCode)

        val stage = genStages(stageSize = 2, jobSize = 2, elementSize = 2, needFinally = false)[1]
        val container = stage.containers[0]
        val needSkipContainerWhenFailRetry = context.needSkipContainerWhenFailRetry(stage, container)
        println("needSkipContainerWhenFailRetry=$needSkipContainerWhenFailRetry")
        Assertions.assertEquals(false, needSkipContainerWhenFailRetry)
    }

    private fun initDefaultStartBuildContext(): StartBuildContext {
        val pipelineParamMap: MutableMap<String, BuildParameters> = params.toList().associate {
            it.first to BuildParameters(key = it.first, value = it.second)
        }.toMutableMap()
        return StartBuildContext.init(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            resourceVersion = version,
            versionName = null,
            realStartParamKeys = emptyList(),
            modelStr = "",
            pipelineParamMap = pipelineParamMap,
            currentBuildNo = null,
            triggerReviewers = null,
            debug = true,
            yamlVersion = "v3.0"
        )
    }

    @Test
    fun needSkipTaskWhenRetry() {
        val context = initDefaultStartBuildContext()
        Assertions.assertEquals(params, context.variables)
        val stage = genStages(stageSize = 2, jobSize = 2, elementSize = 2, needFinally = false)[1]
        val container = stage.containers[0]
        val needSkipTaskWhenRetry = context.needSkipTaskWhenRetry(stage, container, taskId = "1")
        println("needSkipTaskWhenRetry=$needSkipTaskWhenRetry")
        Assertions.assertEquals(true, needSkipTaskWhenRetry)
        // 确认id不是当前要跳过的插件
        val element211 = stage.containers[0].elements[0]
        Assertions.assertEquals(false, context.inSkipStage(stage, element211))
    }

    @Test
    fun needSkipTaskWhenRetrySkip() {
        // 跳过Stage-2下所有失败插件
        params[PIPELINE_RETRY_START_TASK_ID] = "stage-2"
        params[PIPELINE_SKIP_FAILED_TASK] = true.toString()

        val stage2Context = initDefaultStartBuildContext()
        Assertions.assertEquals(params, stage2Context.variables)
        val stages = genStages(stageSize = 2, jobSize = 2, elementSize = 2, needFinally = false)
        val stage1 = stages[1]
        val stage2 = stages[2]
        val container1 = stage1.containers[0]
        val container2 = stage2.containers[0]

        var needSkipTaskWhenRetrySkip = stage2Context.needSkipTaskWhenRetry(
            stage = stage2, container = container1, taskId = container1.elements[0].id
        )
        println("needSkipTaskWhenRetrySkip=$needSkipTaskWhenRetrySkip")
        Assertions.assertEquals(false, needSkipTaskWhenRetrySkip)

        needSkipTaskWhenRetrySkip = stage2Context.needSkipTaskWhenRetry(
            stage = stage1, container = container1, taskId = container1.elements[0].id
        )
        println("needSkipTaskWhenRetrySkip=$needSkipTaskWhenRetrySkip")
        Assertions.assertEquals(true, needSkipTaskWhenRetrySkip)

        // 指定跳过插件是 Stage-1 里的 插件
        params[PIPELINE_RETRY_START_TASK_ID] = stage1.containers[0].elements[0].id!!
        params[PIPELINE_SKIP_FAILED_TASK] = true.toString()
        val skipElement = initDefaultStartBuildContext()
        needSkipTaskWhenRetrySkip = skipElement.needSkipTaskWhenRetry(
            stage = stages[2], container = container2, taskId = container2.elements[0].id
        )
        println("needSkipTaskWhenRetrySkip=$needSkipTaskWhenRetrySkip")
        Assertions.assertEquals(true, needSkipTaskWhenRetrySkip)
    }

    @Test
    fun needSkipTaskWhenRetryRerunDownstream() {
        // 单插件失败重试（skipFailedTask=false）：被重试插件之后、同Job内的后续插件需要一并重排（不跳过）
        val stages = genStages(stageSize = 2, jobSize = 2, elementSize = 3, needFinally = false)
        val stage = stages[1]
        val container = stage.containers[0]
        val parallelContainer = stage.containers[1] // 同stage的并行Job
        // 指定要重试的插件为容器中间那个
        params[PIPELINE_RETRY_START_TASK_ID] = container.elements[1].id!!
        val context = initDefaultStartBuildContext()

        // 被重试插件本身：不跳过
        Assertions.assertEquals(
            false, context.needSkipTaskWhenRetry(stage, container, container.elements[1].id)
        )
        // 被重试插件之前的插件：保持原逻辑，跳过
        Assertions.assertEquals(
            true, context.needSkipTaskWhenRetry(stage, container, container.elements[0].id)
        )
        // 被重试插件之后的插件：新逻辑，需要重排（不跳过），以便重新评估运行条件
        Assertions.assertEquals(
            false, context.needSkipTaskWhenRetry(stage, container, container.elements[2].id)
        )
        // 其它并行Job的插件：不受影响，仍跳过
        Assertions.assertEquals(
            true, context.needSkipTaskWhenRetry(stage, parallelContainer, parallelContainer.elements[0].id)
        )
    }

    @Test
    fun needSkipTaskWhenSkipRerunDownstream() {
        // 单插件跳过（skipFailedTask=true）：与重试对称，被跳过插件之后、同Job内的后续插件需要一并重排（不跳过）
        params[PIPELINE_SKIP_FAILED_TASK] = true.toString()
        val stages = genStages(stageSize = 2, jobSize = 2, elementSize = 3, needFinally = false)
        val stage = stages[1]
        val container = stage.containers[0]
        val parallelContainer = stage.containers[1]
        // 指定要跳过的插件为容器中间那个
        params[PIPELINE_RETRY_START_TASK_ID] = container.elements[1].id!!
        val context = initDefaultStartBuildContext()

        // 被跳过插件本身：不属于"重试时跳过"（其SKIP由inSkipStage处理）
        Assertions.assertEquals(
            false, context.needSkipTaskWhenRetry(stage, container, container.elements[1].id)
        )
        // 被跳过插件之前的插件：保持原逻辑，跳过
        Assertions.assertEquals(
            true, context.needSkipTaskWhenRetry(stage, container, container.elements[0].id)
        )
        // 被跳过插件之后的插件：新逻辑，需要重排（不跳过），据此重新评估运行条件
        Assertions.assertEquals(
            false, context.needSkipTaskWhenRetry(stage, container, container.elements[2].id)
        )
        // 其它并行Job的插件：不受影响，仍跳过
        Assertions.assertEquals(
            true, context.needSkipTaskWhenRetry(stage, parallelContainer, parallelContainer.elements[0].id)
        )
    }

    @Test
    fun inSkipStage() {
        // 跳过Stage-2下所有失败插件
        params[PIPELINE_RETRY_START_TASK_ID] = "stage-2"
        params[PIPELINE_SKIP_FAILED_TASK] = true.toString()

        val stage2Context = initDefaultStartBuildContext()
        Assertions.assertEquals(params, stage2Context.variables)
        val stages = genStages(stageSize = 2, jobSize = 2, elementSize = 2, needFinally = false)
        val stage1 = stages[1]
        val stage2 = stages[2]
        // 在重试要跳过的Stage-2里面
        stage2.containers.forEach { c ->
            c.elements.forEach { e ->
                e.status = BuildStatus.FAILED.name
                Assertions.assertEquals(true, stage2Context.inSkipStage(stage2, e))
            }
        }

        // Stage-1的不受影响
        stage1.containers.forEach { c ->
            c.elements.forEach { e ->
                e.status = BuildStatus.FAILED.name
                Assertions.assertEquals(false, stage2Context.inSkipStage(stage1, e))
            }
        }

        // 指定跳过插件是 Stage-1 里 插件
        params[PIPELINE_RETRY_START_TASK_ID] = stage1.containers[0].elements[0].id!!
        params[PIPELINE_SKIP_FAILED_TASK] = true.toString()
        val skipElement = initDefaultStartBuildContext()
        // Stage-2 的插件不在重试时跳过的Stage内范围
        stage2.containers.forEach { c ->
            c.elements.forEach { e ->
                e.status = BuildStatus.FAILED.name
                Assertions.assertEquals(false, skipElement.inSkipStage(stage2, e))
            }
        }

        // Stage-1 里面只有指定的跳过插件
        stage1.containers.forEach { c ->
            c.elements.forEach { e ->
                e.status = BuildStatus.FAILED.name
                if (e.id == params[PIPELINE_RETRY_START_TASK_ID]) {
                    println("Stage-1 里面只有指定的跳过插件: ${stage1.id}, ${e.id}")
                    Assertions.assertEquals(true, skipElement.inSkipStage(stage1, e))
                } else {
                    Assertions.assertEquals(false, skipElement.inSkipStage(stage1, e))
                }
            }
        }
    }
}
