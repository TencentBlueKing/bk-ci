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

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.StageRunCondition
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.process.TestBase
import com.tencent.devops.process.utils.TASK_FAIL_RETRY_MAX_COUNT
import com.tencent.devops.process.utils.TASK_FAIL_RETRY_MIN_COUNT
import org.junit.Assert
import org.junit.Test

/**
 * @version 1.0
 */
@Suppress("ALL")
class ControlUtilsTest : TestBase() {

    @Test
    fun isEnable() {
        Assert.assertTrue(ControlUtils.isEnable(null))
        Assert.assertFalse(
            ControlUtils.isEnable(
                additionalOptions = elementAdditionalOptions(
                    enable = false, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                )
            )
        )
        Assert.assertTrue(
            ControlUtils.isEnable(
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_FAILED_ONLY)
            )
        )
        Assert.assertTrue(ControlUtils.isEnable(nullObject))
    }

    @Test
    fun continueWhenFailure() {
        Assert.assertFalse(ControlUtils.continueWhenFailure(nullObject))
        Assert.assertFalse(
            ControlUtils.continueWhenFailure(
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_FAILED_ONLY)
            )
        )
        Assert.assertTrue(
            ControlUtils.continueWhenFailure(
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_FAILED_ONLY)
                    .copy(continueWhenFailed = true)
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
                runCondition = JobRunCondition.CUSTOM_CONDITION_MATCH,
                customCondition = "a==a"
            )
        )
    }

    @Test
    fun pauseBeforeExec() {
        var pauseFlag: String? = null
        Assert.assertFalse(ControlUtils.pauseBeforeExec(null, pauseFlag))
        Assert.assertTrue(ControlUtils.pauseBeforeExec(
            additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_FAILED_ONLY)
                .copy(pauseBeforeExec = true), alreadyPauseFlag = pauseFlag
        ))
        pauseFlag = ""
        Assert.assertTrue(ControlUtils.pauseBeforeExec(
            additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_FAILED_ONLY)
                .copy(pauseBeforeExec = true), alreadyPauseFlag = pauseFlag
        ))
        pauseFlag = "true"
        Assert.assertFalse(ControlUtils.pauseBeforeExec(
            additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_FAILED_ONLY)
                .copy(pauseBeforeExec = true),
            alreadyPauseFlag = pauseFlag
        ))
        Assert.assertFalse(ControlUtils.pauseBeforeExec(
            additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_FAILED_ONLY)
                .copy(pauseBeforeExec = true),
            alreadyPauseFlag = pauseFlag
        ))
        Assert.assertFalse(ControlUtils.pauseBeforeExec(
            additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_FAILED_ONLY)
                .copy(pauseBeforeExec = null),
            alreadyPauseFlag = pauseFlag
        ))
    }

    @Test
    fun retryWhenFailure() {
        val nullObject = null
        var retryCount = 0
        Assert.assertFalse(ControlUtils.retryWhenFailure(nullObject, retryCount))
        Assert.assertFalse(
            ControlUtils.retryWhenFailure(
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_FAILED_ONLY)
                    .copy(retryWhenFailed = false), retryCount = retryCount
            )
        )

        Assert.assertFalse(
            ControlUtils.retryWhenFailure(
                additionalOptions = elementAdditionalOptions(
                    enable = false, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                ), retryCount = retryCount
            )
        )

        var setRetryCount = TASK_FAIL_RETRY_MAX_COUNT + 100 // 故意设置超过最大值，验证会被强制改回
        retryCount = 0
        while (retryCount < TASK_FAIL_RETRY_MAX_COUNT) {
            Assert.assertTrue(
                ControlUtils.retryWhenFailure(
                    additionalOptions = elementAdditionalOptions(
                        retryCount = setRetryCount, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                    ).copy(retryWhenFailed = true), retryCount = retryCount
                )
            )
            retryCount++
        }

        // exceed max retry count
        Assert.assertFalse(
            ControlUtils.retryWhenFailure(
                additionalOptions = elementAdditionalOptions(
                    retryCount = setRetryCount, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                ).copy(retryWhenFailed = true), retryCount = retryCount
            )
        )

        setRetryCount = 0 // 故意设置小于最小值，验证会被强制改回
        retryCount = 0
        while (retryCount < TASK_FAIL_RETRY_MIN_COUNT) {

            Assert.assertTrue(
                ControlUtils.retryWhenFailure(
                    additionalOptions = elementAdditionalOptions(
                        retryCount = setRetryCount, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                    ).copy(retryWhenFailed = true), retryCount = retryCount
                )
            )
            retryCount++
        }

        // exceed max retry count
        Assert.assertFalse(
            ControlUtils.retryWhenFailure(
                additionalOptions = elementAdditionalOptions(
                    retryCount = setRetryCount, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                ).copy(retryWhenFailed = true), retryCount = retryCount
            )
        )
    }

    @Test
    fun checkCustomVariableSkip() {
        variables["a"] = "1"
        val customeVarabiles = mutableListOf(NameAndValue("a", "1"))
        // 自定义变量全部满足时不运行
        Assert.assertTrue(
            ControlUtils.checkCustomVariableSkip(buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN, customVariables = customeVarabiles
                ))
        )
        // 自定义变量全部满足时不运行, 不满足，运行。。。
        variables["a"] = "2"
        Assert.assertFalse(
            ControlUtils.checkCustomVariableSkip(buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN, customVariables = customeVarabiles
                ))
        )
        // 自定义变量全部满足时运行
        variables["a"] = "1"
        Assert.assertFalse(
            ControlUtils.checkCustomVariableSkip(buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH, customVariables = customeVarabiles
                ))
        )
        // 自定义变量全部满足时运行, 不满足不运行。。。
        variables["a"] = "2"
        Assert.assertTrue(
            ControlUtils.checkCustomVariableSkip(buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH, customVariables = customeVarabiles
                ))
        )
        // 支持变量
        variables["a"] = "1"; variables["var_1"] = "1"
        customeVarabiles.add(NameAndValue("a", "\${var_1}"))
        Assert.assertFalse(
            ControlUtils.checkCustomVariableSkip(buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH, customVariables = customeVarabiles
                ))
        )
        variables["a"] = "1"; variables["var_1"] = "1"
        customeVarabiles.add(NameAndValue("a", "\${var_1}"))
        Assert.assertTrue(
            ControlUtils.checkCustomVariableSkip(buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN, customVariables = customeVarabiles
                ))
        )
        variables["a"] = "2"; variables["var_1"] = "1"
        customeVarabiles.add(NameAndValue("a", "\${var_1}"))
        Assert.assertFalse(
            ControlUtils.checkCustomVariableSkip(buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN, customVariables = customeVarabiles
                ))
        )
        variables["a"] = "2"; variables["var_1"] = "1"
        customeVarabiles.add(NameAndValue("a", "\${var_1}"))
        Assert.assertTrue(
            ControlUtils.checkCustomVariableSkip(buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH, customVariables = customeVarabiles
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
            ControlUtils.checkStageSkipCondition(
                conditions = conditions, variables = variables, buildId = buildId,
                runCondition = StageRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN
            )
        )

        variables["a"] = "1"
        // 条件匹配就跳过
        Assert.assertTrue(
            ControlUtils.checkStageSkipCondition(
                conditions = conditions, variables = variables, buildId = buildId,
                runCondition = StageRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN
            )
        )

        variables["a"] = "1"
        // 条件匹配就跳过
        Assert.assertFalse(
            ControlUtils.checkStageSkipCondition(
                conditions = conditions, variables = variables, buildId = buildId,
                runCondition = StageRunCondition.CUSTOM_VARIABLE_MATCH
            )
        )

        variables["a"] = "2"
        // 条件匹配就跳过
        Assert.assertTrue(
            ControlUtils.checkStageSkipCondition(
                conditions = conditions, variables = variables, buildId = buildId,
                runCondition = StageRunCondition.CUSTOM_VARIABLE_MATCH
            )
        )

        run other@{
            Assert.assertFalse(
                ControlUtils.checkStageSkipCondition(
                    conditions = conditions, variables = variables, buildId = buildId,
                    runCondition = StageRunCondition.CUSTOM_CONDITION_MATCH,
                    customCondition = "a==a"
                )
            )
            Assert.assertFalse(
                ControlUtils.checkStageSkipCondition(
                    conditions = conditions, variables = variables, buildId = buildId,
                    runCondition = StageRunCondition.AFTER_LAST_FINISHED
                )
            )
        }
    }

    @Test
    fun `when container fail`() {
        val fail = BuildStatus.FAILED
        val failed = true
        Assert.assertTrue(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_SUCCESS),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed
            )
        )
        Assert.assertTrue(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.CUSTOM_VARIABLE_MATCH),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed
            )
        )

        Assert.assertTrue(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed
            )
        )
        Assert.assertTrue(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.CUSTOM_CONDITION_MATCH),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed
            )
        )
        Assert.assertTrue(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.OTHER_TASK_RUNNING),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed
            )
        )
    }

    @Test
    fun `when container running`() {
        variables["a"] = "b"
        val fail = BuildStatus.RUNNING
        val failed = false
        // 成功不跳过
        Assert.assertFalse(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_SUCCESS),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed
            )
        )
        // 满足执行条件 而不跳过
        Assert.assertFalse(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
                    customVariables = mutableListOf(NameAndValue(key = "a", value = "b"))
                ),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed
            )
        )
        // 不满足执行条件 而跳过
        Assert.assertTrue(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
                    customVariables = mutableListOf(NameAndValue(key = "a", value = "a"))
                ),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed
            )
        )
        // 满足不执行的条件 而跳过
        Assert.assertTrue(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
                    customVariables = mutableListOf(NameAndValue(key = "a", value = "b"))
                ),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed
            )
        )
        // 不满足不执行的条件 而不跳过
        Assert.assertFalse(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
                    customVariables = mutableListOf(NameAndValue(key = "a", value = "a"))
                ),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed
            )
        )
    }

    @Test
    fun `when container success`() {
        variables["a"] = "b"
        val fail = BuildStatus.SUCCEED
        val failed = false
        // 成功不跳过
        Assert.assertFalse(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_SUCCESS),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed
            )
        )
        // 满足执行条件 而不跳过
        Assert.assertFalse(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
                    customVariables = mutableListOf(NameAndValue(key = "a", value = "b"))
                ),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed
            )
        )
        // 不满足执行条件 而跳过
        Assert.assertTrue(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
                    customVariables = mutableListOf(NameAndValue(key = "a", value = "a"))
                ),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed
            )
        )
        // 满足不执行的条件 而跳过
        Assert.assertTrue(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
                    customVariables = mutableListOf(NameAndValue(key = "a", value = "b"))
                ),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed
            )
        )
        // 不满足不执行的条件 而不跳过
        Assert.assertFalse(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
                    customVariables = mutableListOf(NameAndValue(key = "a", value = "a"))
                ),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed
            )
        )
    }

    @Test
    fun checkTaskConditionSkip() {
        val variables = mutableMapOf<String, String>()

        // null check
        Assert.assertFalse(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = null,
                containerFinalStatus = BuildStatus.RUNNING,
                variables = variables,
                hasFailedTaskInSuccessContainer = true)
        )

        // RunCondition.PRE_TASK_FAILED_ONLY & RUNNING
        Assert.assertFalse(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    enable = true, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                ),
                containerFinalStatus = BuildStatus.RUNNING,
                variables = variables,
                hasFailedTaskInSuccessContainer = true)
        )
        // RunCondition.PRE_TASK_FAILED_ONLY & FAIL
        Assert.assertFalse(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    enable = true, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                ),
                containerFinalStatus = BuildStatus.FAILED,
                variables = variables,
                hasFailedTaskInSuccessContainer = true)
        )
        // RunCondition.PRE_TASK_FAILED_ONLY & FAIL
        Assert.assertFalse(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    enable = true, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                ),
                containerFinalStatus = BuildStatus.FAILED,
                variables = variables,
                hasFailedTaskInSuccessContainer = false)
        )

        // RunCondition.PRE_TASK_FAILED_ONLY & disable
        Assert.assertTrue(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    enable = false, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                ),
                containerFinalStatus = BuildStatus.FAILED,
                variables = variables,
                hasFailedTaskInSuccessContainer = false)
        )

        // RunCondition.PRE_TASK_FAILED_ONLY & SUCCEED
        Assert.assertTrue(
            ControlUtils.checkTaskSkip(buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    enable = true, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                ),
                containerFinalStatus = BuildStatus.SUCCEED,
                variables = variables,
                hasFailedTaskInSuccessContainer = false)
        )
    }
}
