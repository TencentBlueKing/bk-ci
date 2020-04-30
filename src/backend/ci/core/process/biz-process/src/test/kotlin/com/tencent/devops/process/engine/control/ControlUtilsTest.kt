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
import com.tencent.devops.common.pipeline.enums.StageRunCondition
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.process.utils.TASK_FAIL_RETRY_MAX_COUNT
import com.tencent.devops.process.utils.TASK_FAIL_RETRY_MIN_COUNT
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * @version 1.0
 */
class ControlUtilsTest {

    private val nullObject = null
    private val buildId = "b-12345678901234567890123456789012"

    private var variables: MutableMap<String, String> = mutableMapOf()

    @Before
    fun setUp() {
        variables = mutableMapOf()
    }

    @Test
    fun skipPreTaskNotFail() {
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
                            runCondition = RunCondition.PRE_TASK_FAILED_ONLY,
                            retryCount = 0,
                            retryWhenFailed = false,
                            pauseBeforeExec = false,
                            subscriptionPauseUser = null
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
                            runCondition = RunCondition.PRE_TASK_FAILED_ONLY,
                            retryCount = 0,
                            retryWhenFailed = false,
                            pauseBeforeExec = false,
                            subscriptionPauseUser = null
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
                    enable = false,
                    continueWhenFailed = false,
                    timeout = 0,
                    runCondition = null,
                    otherTask = null,
                    customCondition = null,
                    customVariables = null,
                    retryCount = 0,
                    retryWhenFailed = false,
                    pauseBeforeExec = false,
                    subscriptionPauseUser = null
                )
            )
        )
        Assert.assertTrue(
            ControlUtils.continueWhenFailure(
                ElementAdditionalOptions(
                    enable = true,
                    continueWhenFailed = true,
                    timeout = 0,
                    runCondition = null,
                    otherTask = null,
                    customCondition = null,
                    customVariables = null,
                    retryCount = 0,
                    retryWhenFailed = false,
                    pauseBeforeExec = false,
                    subscriptionPauseUser = null
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
                    enable = true,
                    continueWhenFailed = false,
                    timeout = 0,
                    runCondition = null,
                    otherTask = null,
                    customCondition = null,
                    customVariables = null,
                    retryCount = 0,
                    retryWhenFailed = false,
                    pauseBeforeExec = false,
                    subscriptionPauseUser = null
                )
            )
        )
        Assert.assertTrue(
            ControlUtils.continueWhenFailure(
                ElementAdditionalOptions(
                    enable = true,
                    continueWhenFailed = true,
                    timeout = 0,
                    runCondition = null,
                    otherTask = null,
                    customCondition = null,
                    customVariables = null,
                    retryCount = 0,
                    retryWhenFailed = false,
                    pauseBeforeExec = false,
                    subscriptionPauseUser = null
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
            ControlUtils.checkJobSkipCondition(
                conditions = conditions,
                variables = variables,
                buildId = buildId,
                runCondition = JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN
            )
        )
        Assert.assertFalse(
            ControlUtils.checkJobSkipCondition(
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
            ControlUtils.checkJobSkipCondition(
                conditions = conditions,
                variables = variables,
                buildId = buildId,
                runCondition = JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN
            )
        )
        Assert.assertTrue(
            ControlUtils.checkJobSkipCondition(
                conditions = conditions,
                variables = variables,
                buildId = buildId,
                runCondition = JobRunCondition.CUSTOM_VARIABLE_MATCH
            )
        )
        Assert.assertFalse(
            ControlUtils.checkJobSkipCondition(
                conditions = conditions,
                variables = variables,
                buildId = buildId,
                runCondition = JobRunCondition.CUSTOM_CONDITION_MATCH
            )
        )
    }

    @Test
    fun retryWhenFailure() {
        val nullObject = null
        var retryCount = 0
        Assert.assertFalse(ControlUtils.retryWhenFailure(nullObject, retryCount))
        Assert.assertFalse(ControlUtils.retryWhenFailure(ElementAdditionalOptions(
            enable = true,
            retryWhenFailed = false,
            continueWhenFailed = false,
            timeout = 0,
            otherTask = nullObject,
            customCondition = nullObject,
            customVariables = nullObject,
            runCondition = RunCondition.PRE_TASK_FAILED_ONLY,
            retryCount = 0
        ), retryCount))

        Assert.assertFalse(ControlUtils.retryWhenFailure(ElementAdditionalOptions(
            enable = false,
            retryWhenFailed = true,
            continueWhenFailed = false,
            timeout = 0,
            otherTask = nullObject,
            customCondition = nullObject,
            customVariables = nullObject,
            runCondition = RunCondition.PRE_TASK_FAILED_ONLY,
            retryCount = 0
        ), retryCount))

        var setRetryCount = TASK_FAIL_RETRY_MAX_COUNT + 100 // 故意设置超过最大值，验证会被强制改回
        retryCount = 0
        while (retryCount < TASK_FAIL_RETRY_MAX_COUNT) {
            Assert.assertTrue(ControlUtils.retryWhenFailure(ElementAdditionalOptions(
                enable = true,
                retryWhenFailed = true,
                retryCount = setRetryCount,
                continueWhenFailed = false,
                timeout = 0,
                otherTask = nullObject,
                customCondition = nullObject,
                customVariables = nullObject,
                runCondition = RunCondition.PRE_TASK_FAILED_ONLY
            ), retryCount))
            retryCount++
        }

        // exceed max retry count
        Assert.assertFalse(ControlUtils.retryWhenFailure(ElementAdditionalOptions(
            enable = true,
            retryCount = setRetryCount,
            retryWhenFailed = true,
            continueWhenFailed = false,
            timeout = 0,
            otherTask = nullObject,
            customCondition = nullObject,
            customVariables = nullObject,
            runCondition = RunCondition.PRE_TASK_FAILED_ONLY
        ), retryCount))

        setRetryCount = 0 // 故意设置小于最小值，验证会被强制改回
        retryCount = 0
        while (retryCount < TASK_FAIL_RETRY_MIN_COUNT) {
            Assert.assertTrue(ControlUtils.retryWhenFailure(ElementAdditionalOptions(
                enable = true,
                retryCount = setRetryCount,
                retryWhenFailed = true,
                continueWhenFailed = false,
                timeout = 0,
                otherTask = nullObject,
                customCondition = nullObject,
                customVariables = nullObject,
                runCondition = RunCondition.PRE_TASK_FAILED_ONLY
            ), retryCount))
            retryCount++
        }

        // exceed max retry count
        Assert.assertFalse(ControlUtils.retryWhenFailure(ElementAdditionalOptions(
            enable = true,
            retryCount = setRetryCount,
            retryWhenFailed = true,
            continueWhenFailed = false,
            timeout = 0,
            otherTask = nullObject,
            customCondition = nullObject,
            customVariables = nullObject,
            runCondition = RunCondition.PRE_TASK_FAILED_ONLY
        ), retryCount))
    }

    @Test
    fun checkAdditionalSkip() {
        val variables = mutableMapOf<String, String>()

        // null check
        Assert.assertFalse(
            ControlUtils.checkAdditionalSkip(buildId = buildId,
                additionalOptions = null,
                containerFinalStatus = BuildStatus.RUNNING,
                variables = variables,
                hasFailedTaskInSuccessContainer = true)
        )

        // 不适用的 RunCondition 条件
        run check@{
            Assert.assertFalse(
                ControlUtils.checkAdditionalSkip(buildId = buildId,
                    additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_SUCCESS),
                    containerFinalStatus = BuildStatus.RUNNING,
                    variables = variables,
                    hasFailedTaskInSuccessContainer = true)
            )
            Assert.assertFalse(
                ControlUtils.checkAdditionalSkip(buildId = buildId,
                    additionalOptions = elementAdditionalOptions(runCondition = RunCondition.CUSTOM_CONDITION_MATCH),
                    containerFinalStatus = BuildStatus.RUNNING,
                    variables = variables,
                    hasFailedTaskInSuccessContainer = true)
            )
            Assert.assertFalse(
                ControlUtils.checkAdditionalSkip(buildId = buildId,
                    additionalOptions = elementAdditionalOptions(runCondition = RunCondition.CUSTOM_VARIABLE_MATCH),
                    containerFinalStatus = BuildStatus.RUNNING,
                    variables = variables,
                    hasFailedTaskInSuccessContainer = true)
            )
            Assert.assertFalse(
                ControlUtils.checkAdditionalSkip(buildId = buildId,
                    additionalOptions = elementAdditionalOptions(runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN),
                    containerFinalStatus = BuildStatus.RUNNING,
                    variables = variables,
                    hasFailedTaskInSuccessContainer = true)
            )
            Assert.assertFalse(
                ControlUtils.checkAdditionalSkip(buildId = buildId,
                    additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_FAILED_BUT_CANCEL),
                    containerFinalStatus = BuildStatus.RUNNING,
                    variables = variables,
                    hasFailedTaskInSuccessContainer = true)
            )
            Assert.assertFalse(
                ControlUtils.checkAdditionalSkip(buildId = buildId,
                    additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_FAILED_EVEN_CANCEL),
                    containerFinalStatus = BuildStatus.RUNNING,
                    variables = variables,
                    hasFailedTaskInSuccessContainer = true)
            )
            Assert.assertFalse(
                ControlUtils.checkAdditionalSkip(buildId = buildId,
                    additionalOptions = elementAdditionalOptions(runCondition = RunCondition.OTHER_TASK_RUNNING),
                    containerFinalStatus = BuildStatus.RUNNING,
                    variables = variables,
                    hasFailedTaskInSuccessContainer = true)
            )
        }

        // RunCondition.PRE_TASK_FAILED_ONLY & RUNNING
        Assert.assertFalse(
            ControlUtils.checkAdditionalSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(enable = true, runCondition = RunCondition.PRE_TASK_FAILED_ONLY),
                containerFinalStatus = BuildStatus.RUNNING,
                variables = variables,
                hasFailedTaskInSuccessContainer = true)
        )
        // RunCondition.PRE_TASK_FAILED_ONLY & FAIL
        Assert.assertFalse(
            ControlUtils.checkAdditionalSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(enable = true, runCondition = RunCondition.PRE_TASK_FAILED_ONLY),
                containerFinalStatus = BuildStatus.FAILED,
                variables = variables,
                hasFailedTaskInSuccessContainer = true)
        )
        // RunCondition.PRE_TASK_FAILED_ONLY & FAIL
        Assert.assertFalse(
            ControlUtils.checkAdditionalSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(enable = true, runCondition = RunCondition.PRE_TASK_FAILED_ONLY),
                containerFinalStatus = BuildStatus.FAILED,
                variables = variables,
                hasFailedTaskInSuccessContainer = false)
        )

        // RunCondition.PRE_TASK_FAILED_ONLY & disable
        Assert.assertTrue(
            ControlUtils.checkAdditionalSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(enable = false, runCondition = RunCondition.PRE_TASK_FAILED_ONLY),
                containerFinalStatus = BuildStatus.FAILED,
                variables = variables,
                hasFailedTaskInSuccessContainer = false)
        )

        // RunCondition.PRE_TASK_FAILED_ONLY & SUCCEED
        Assert.assertTrue(
            ControlUtils.checkAdditionalSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(enable = true, runCondition = RunCondition.PRE_TASK_FAILED_ONLY),
                containerFinalStatus = BuildStatus.SUCCEED,
                variables = variables,
                hasFailedTaskInSuccessContainer = false)
        )
    }

    private fun elementAdditionalOptions(runCondition: RunCondition = RunCondition.PRE_TASK_FAILED_ONLY, enable: Boolean = true, customVarabiles: List<NameAndValue>? = null): ElementAdditionalOptions {
        return ElementAdditionalOptions(
            enable = enable,
            retryCount = 1,
            retryWhenFailed = true,
            continueWhenFailed = false,
            timeout = 0,
            otherTask = nullObject,
            customCondition = nullObject,
            customVariables = customVarabiles,
            runCondition = runCondition
        )
    }

    @Test
    fun checkCustomVariableSkip() {
        variables["a"] = "1"
        val customeVarabiles = mutableListOf(
            NameAndValue("a", "1")
        )
        // 自定义变量全部满足时不运行
        Assert.assertTrue(
            ControlUtils.checkCustomVariableSkip(buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
                    enable = true,
                    customVarabiles = customeVarabiles
                ))
        )

        // 自定义变量全部满足时不运行, 不满足，运行。。。
        variables["a"] = "2"
        Assert.assertFalse(
            ControlUtils.checkCustomVariableSkip(buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
                    enable = true,
                    customVarabiles = customeVarabiles
                ))
        )

        // 自定义变量全部满足时运行
        variables["a"] = "1"
        Assert.assertFalse(
            ControlUtils.checkCustomVariableSkip(buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
                    enable = true,
                    customVarabiles = customeVarabiles
                ))
        )

        // 自定义变量全部满足时运行, 不满足不运行。。。
        variables["a"] = "2"
        Assert.assertTrue(
            ControlUtils.checkCustomVariableSkip(buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
                    enable = true,
                    customVarabiles = customeVarabiles
                ))
        )

        // 支持变量

        variables["a"] = "1"
        variables["var_1"] = "1"
        customeVarabiles.add(NameAndValue("a", "\${var_1}"))
        Assert.assertFalse(
            ControlUtils.checkCustomVariableSkip(buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
                    enable = true,
                    customVarabiles = customeVarabiles
                ))
        )

        variables["a"] = "1"
        variables["var_1"] = "1"
        customeVarabiles.add(NameAndValue("a", "\${var_1}"))
        Assert.assertTrue(
            ControlUtils.checkCustomVariableSkip(buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
                    enable = true,
                    customVarabiles = customeVarabiles
                ))
        )

        variables["a"] = "2"
        variables["var_1"] = "1"
        customeVarabiles.add(NameAndValue("a", "\${var_1}"))
        Assert.assertFalse(
            ControlUtils.checkCustomVariableSkip(buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
                    enable = true,
                    customVarabiles = customeVarabiles
                ))
        )

        variables["a"] = "2"
        variables["var_1"] = "1"
        customeVarabiles.add(NameAndValue("a", "\${var_1}"))
        Assert.assertTrue(
            ControlUtils.checkCustomVariableSkip(buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
                    enable = true,
                    customVarabiles = customeVarabiles
                ))
        )
    }

    @Test
    fun checkStageSkipCondition() {

        val conditions = mutableListOf(
            NameAndValue("a", "1")
        )

        variables["a"] = "2"
        // 条件匹配就跳过
        Assert.assertFalse(
            ControlUtils.checkStageSkipCondition(conditions, variables, buildId, StageRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN)
        )

        variables["a"] = "1"
        // 条件匹配就跳过
        Assert.assertTrue(
            ControlUtils.checkStageSkipCondition(conditions, variables, buildId, StageRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN)
        )

        variables["a"] = "1"
        // 条件匹配就跳过
        Assert.assertFalse(
            ControlUtils.checkStageSkipCondition(conditions, variables, buildId, StageRunCondition.CUSTOM_VARIABLE_MATCH)
        )

        variables["a"] = "2"
        // 条件匹配就跳过
        Assert.assertTrue(
            ControlUtils.checkStageSkipCondition(conditions, variables, buildId, StageRunCondition.CUSTOM_VARIABLE_MATCH)
        )

        run other@{
            Assert.assertFalse(
                ControlUtils.checkStageSkipCondition(conditions, variables, buildId, StageRunCondition.CUSTOM_CONDITION_MATCH)
            )
            Assert.assertFalse(
                ControlUtils.checkStageSkipCondition(conditions, variables, buildId, StageRunCondition.AFTER_LAST_FINISHED)
            )
        }
    }
}
