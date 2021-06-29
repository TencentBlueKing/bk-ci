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

package com.tencent.devops.process.engine.context

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.process.TestBase
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.process.utils.PIPELINE_RETRY_START_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_SKIP_FAILED_TASK
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class StartBuildContextTest : TestBase() {

    private val params = mutableMapOf<String, Any>()

    @Before
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
        val context = StartBuildContext.init(params)
        val stage = genStages(stageSize = 2, jobSize = 2, elementSize = 2, needFinally = false)[1]
        val needSkipWhenStageFailRetry = context.needSkipWhenStageFailRetry(stage)
        println("needSkipWhenStageFailRetry=$needSkipWhenStageFailRetry")
        Assert.assertEquals(false, needSkipWhenStageFailRetry)
    }

    @Test
    fun needSkipContainerWhenFailRetry() {
        params[PIPELINE_RETRY_COUNT] = "abc"
        var context = StartBuildContext.init(params)
        Assert.assertEquals(context.retryCount, 0)
        params[PIPELINE_RETRY_COUNT] = "3"
        context = StartBuildContext.init(params)
        Assert.assertEquals(context.retryCount, 3)

        params[PIPELINE_START_CHANNEL] = ChannelCode.AM.name
        context = StartBuildContext.init(params)
        Assert.assertEquals(ChannelCode.AM, context.channelCode)
        params.remove(PIPELINE_START_CHANNEL)

        context = StartBuildContext.init(params)
        Assert.assertEquals(ChannelCode.BS, context.channelCode)

        val stage = genStages(stageSize = 2, jobSize = 2, elementSize = 2, needFinally = false)[1]
        val container = stage.containers[0]
        val needSkipContainerWhenFailRetry = context.needSkipContainerWhenFailRetry(stage, container)
        println("needSkipContainerWhenFailRetry=$needSkipContainerWhenFailRetry")
        Assert.assertEquals(false, needSkipContainerWhenFailRetry)
    }

    @Test
    fun needSkipTaskWhenRetry() {
        val context = StartBuildContext.init(params)
        val stage = genStages(stageSize = 2, jobSize = 2, elementSize = 2, needFinally = false)[1]
        val needSkipTaskWhenRetry = context.needSkipTaskWhenRetry(stage, taskId = "1")
        println("needSkipTaskWhenRetry=$needSkipTaskWhenRetry")
        Assert.assertEquals(true, needSkipTaskWhenRetry)
        // 确认id不是当前要跳过的插件
        Assert.assertEquals(false, context.isSkipTask("elementId"))
    }

    @Test
    fun needSkipTaskWhenRetrySkip() {
        // 跳过Stage-2下所有失败插件
        params[PIPELINE_RETRY_START_TASK_ID] = "stage-2"
        params[PIPELINE_SKIP_FAILED_TASK] = true
        val skipStage2Context = StartBuildContext.init(params)
        val stages = genStages(stageSize = 2, jobSize = 2, elementSize = 2, needFinally = false)
        var elementId = stages[2].containers[0].elements[0].id
        var needSkipTaskWhenRetrySkip = skipStage2Context.needSkipTaskWhenRetry(stages[2], taskId = elementId)
        println("needSkipTaskWhenRetrySkip=$needSkipTaskWhenRetrySkip")
        Assert.assertEquals(false, needSkipTaskWhenRetrySkip)

        // Stage-1不受影响，会跳过
        elementId = stages[1].containers[0].elements[0].id
        Assert.assertEquals(false, skipStage2Context.isSkipTask(elementId))
        needSkipTaskWhenRetrySkip = skipStage2Context.needSkipTaskWhenRetry(stages[1], taskId = elementId)
        println("needSkipTaskWhenRetrySkip=$needSkipTaskWhenRetrySkip")
        Assert.assertEquals(true, needSkipTaskWhenRetrySkip)

        // 指定跳过插件
        elementId = stages[2].containers[0].elements[0].id
        params[PIPELINE_RETRY_START_TASK_ID] = elementId!!
        params[PIPELINE_SKIP_FAILED_TASK] = true
        val skipElementContext = StartBuildContext.init(params)
        Assert.assertEquals(true, skipElementContext.isSkipTask(elementId))
        needSkipTaskWhenRetrySkip = skipElementContext.needSkipTaskWhenRetry(stages[2], taskId = elementId)
        println("needSkipTaskWhenRetrySkip=$needSkipTaskWhenRetrySkip")
        Assert.assertEquals(false, needSkipTaskWhenRetrySkip)
    }
}
