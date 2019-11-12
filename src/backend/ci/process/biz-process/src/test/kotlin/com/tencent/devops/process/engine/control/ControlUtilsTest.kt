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

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import org.junit.Assert
import org.junit.Test

/**
 * @version 1.0
 */
class ControlUtilsTest {

    @Test
    fun skipPreTaskNotFail() {
        val nullObject = null
        Assert.assertFalse(ControlUtils.skipPreTaskNotFail(nullObject, BuildStatus.SUCCEED))
        Assert.assertFalse(ControlUtils.skipPreTaskNotFail(nullObject, BuildStatus.FAILED))
        Assert.assertFalse(ControlUtils.skipPreTaskNotFail(nullObject, BuildStatus.CANCELED))
        Assert.assertFalse(ControlUtils.skipPreTaskNotFail(nullObject, BuildStatus.RUNNING))
        Assert.assertFalse(ControlUtils.skipPreTaskNotFail(nullObject, BuildStatus.TERMINATE))
        Assert.assertFalse(ControlUtils.skipPreTaskNotFail(nullObject, BuildStatus.REVIEWING))
        Assert.assertFalse(ControlUtils.skipPreTaskNotFail(nullObject, BuildStatus.REVIEW_ABORT))
        Assert.assertFalse(ControlUtils.skipPreTaskNotFail(nullObject, BuildStatus.REVIEW_PROCESSED))
        Assert.assertFalse(ControlUtils.skipPreTaskNotFail(nullObject, BuildStatus.HEARTBEAT_TIMEOUT))
        Assert.assertFalse(ControlUtils.skipPreTaskNotFail(nullObject, BuildStatus.PREPARE_ENV))
        Assert.assertFalse(ControlUtils.skipPreTaskNotFail(nullObject, BuildStatus.UNEXEC))
        Assert.assertFalse(ControlUtils.skipPreTaskNotFail(nullObject, BuildStatus.SKIP))
        Assert.assertFalse(ControlUtils.skipPreTaskNotFail(nullObject, BuildStatus.QUALITY_CHECK_FAIL))
        Assert.assertFalse(ControlUtils.skipPreTaskNotFail(nullObject, BuildStatus.QUEUE))
        Assert.assertFalse(ControlUtils.skipPreTaskNotFail(nullObject, BuildStatus.LOOP_WAITING))
        Assert.assertFalse(ControlUtils.skipPreTaskNotFail(nullObject, BuildStatus.CALL_WAITING))
        Assert.assertFalse(ControlUtils.skipPreTaskNotFail(nullObject, BuildStatus.TRY_FINALLY))
        Assert.assertFalse(ControlUtils.skipPreTaskNotFail(nullObject, BuildStatus.QUEUE_TIMEOUT))
        Assert.assertFalse(ControlUtils.skipPreTaskNotFail(nullObject, BuildStatus.EXEC_TIMEOUT))

        BuildStatus.values().forEach { status ->
            if (BuildStatus.isFailure(status)) {
                Assert.assertFalse(
                    ControlUtils.skipPreTaskNotFail(
                        ElementAdditionalOptions(
                            enable = false,
                            continueWhenFailed = false,
                            timeout = 0,
                            otherTask = nullObject,
                            customCondition = nullObject,
                            customVariables = nullObject,
                            runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                        ), status
                    )
                )
            } else {
                Assert.assertTrue(
                    ControlUtils.skipPreTaskNotFail(
                        ElementAdditionalOptions(
                            enable = false,
                            continueWhenFailed = false,
                            timeout = 0,
                            otherTask = nullObject,
                            customCondition = nullObject,
                            customVariables = nullObject,
                            runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                        ), status
                    )
                )
            }
        }
    }

    @Test
    fun isEnable() {
        Assert.assertTrue(ControlUtils.isEnable(null))
        Assert.assertFalse(
            ControlUtils.isEnable(
                ElementAdditionalOptions(
                    enable = false, continueWhenFailed = false, timeout = 0, runCondition = null,
                    otherTask = null, customCondition = null, customVariables = null
                )
            )
        )
        Assert.assertTrue(
            ControlUtils.continueWhenFailure(
                ElementAdditionalOptions(
                    enable = true, continueWhenFailed = true, timeout = 0, runCondition = null,
                    otherTask = null, customCondition = null, customVariables = null
                )
            )
        )
        Assert.assertTrue(ControlUtils.isEnable(null))
        Assert.assertFalse(ControlUtils.continueWhenFailure(null))
    }

    @Test
    fun continueWhenFailure() {
        Assert.assertFalse(ControlUtils.continueWhenFailure(null))
        Assert.assertFalse(
            ControlUtils.continueWhenFailure(
                ElementAdditionalOptions(
                    enable = true, continueWhenFailed = false, timeout = 0, runCondition = null,
                    otherTask = null, customCondition = null, customVariables = null
                )
            )
        )
        Assert.assertTrue(
            ControlUtils.continueWhenFailure(
                ElementAdditionalOptions(
                    enable = true, continueWhenFailed = true, timeout = 0, runCondition = null,
                    otherTask = null, customCondition = null, customVariables = null
                )
            )
        )
    }

    @Test
    fun checkSkipCondition() {
        val buildId = "b-x1x2x3x4x6x2"
        val conditions = mutableListOf(NameAndValue("key1", "a"), NameAndValue("key2", "b"))
        val variables = mutableMapOf("key1" to "a", "key2" to "b")
        Assert.assertTrue(
            ControlUtils.checkSkipCondition(
                conditions = conditions,
                variables = variables,
                buildId = buildId,
                runCondition = JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN
            )
        )
        Assert.assertFalse(
            ControlUtils.checkSkipCondition(
                conditions = conditions,
                variables = variables,
                buildId = buildId,
                runCondition = JobRunCondition.CUSTOM_VARIABLE_MATCH
            )
        )

        conditions.clear()
        conditions.add(NameAndValue("key2", "a"))
        conditions.add(NameAndValue("key3", "3"))
        variables.clear()
        variables["key3"] = "un"
        Assert.assertFalse(
            ControlUtils.checkSkipCondition(
                conditions = conditions,
                variables = variables,
                buildId = buildId,
                runCondition = JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN
            )
        )
        Assert.assertTrue(
            ControlUtils.checkSkipCondition(
                conditions = conditions,
                variables = variables,
                buildId = buildId,
                runCondition = JobRunCondition.CUSTOM_VARIABLE_MATCH
            )
        )
        Assert.assertFalse(
            ControlUtils.checkSkipCondition(
                conditions = conditions,
                variables = variables,
                buildId = buildId,
                runCondition = JobRunCondition.CUSTOM_CONDITION_MATCH
            )
        )
    }
}
