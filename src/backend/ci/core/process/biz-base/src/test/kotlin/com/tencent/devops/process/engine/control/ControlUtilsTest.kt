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

import com.tencent.devops.common.expression.ExpressionParseException
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.StageRunCondition
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.TestBase
import com.tencent.devops.process.utils.TASK_FAIL_RETRY_MAX_COUNT
import com.tencent.devops.process.utils.TASK_FAIL_RETRY_MIN_COUNT
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * @version 1.0
 */
@Suppress("ALL")
class ControlUtilsTest : TestBase() {

    @BeforeEach
    fun setup() {
        val commonConfig: CommonConfig = mockk()
        val redisOperation: RedisOperation = mockk()
        every {
            commonConfig.devopsDefaultLocaleLanguage
        } returns "zh_CN"
        every {
            redisOperation.get(any())
        } returns "zh_CN"
        mockkObject(SpringContextUtil)
        every {
            SpringContextUtil.getBean(CommonConfig::class.java)
        } returns commonConfig
        every {
            SpringContextUtil.getBean(RedisOperation::class.java)
        } returns redisOperation
    }

    @Test
    fun isEnable() {
        Assertions.assertTrue(ControlUtils.isEnable(null))
        Assertions.assertFalse(
            ControlUtils.isEnable(
                additionalOptions = elementAdditionalOptions(
                    enable = false, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                )
            )
        )
        Assertions.assertTrue(
            ControlUtils.isEnable(
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_FAILED_ONLY)
            )
        )
        Assertions.assertTrue(ControlUtils.isEnable(nullObject))
    }

    @Test
    fun continueWhenFailure() {
        Assertions.assertFalse(ControlUtils.continueWhenFailure(nullObject))
        Assertions.assertFalse(
            ControlUtils.continueWhenFailure(
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_FAILED_ONLY)
            )
        )
        Assertions.assertTrue(
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
        Assertions.assertTrue(
            ControlUtils.checkJobSkipCondition(
                conditions = conditions,
                variables = variables,
                buildId = buildId,
                runCondition = JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
                asCodeEnabled = true
            )
        )
        Assertions.assertFalse(
            ControlUtils.checkJobSkipCondition(
                conditions = conditions,
                variables = variables,
                buildId = buildId,
                runCondition = JobRunCondition.CUSTOM_VARIABLE_MATCH,
                asCodeEnabled = true
            )
        )

        conditions.clear()
        conditions.add(NameAndValue("key2", "a"))
        conditions.add(NameAndValue("key3", "3"))
        variables.clear()
        variables["key3"] = "un"
        Assertions.assertFalse(
            ControlUtils.checkJobSkipCondition(
                conditions = conditions,
                variables = variables,
                buildId = buildId,
                runCondition = JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
                asCodeEnabled = true
            )
        )
        Assertions.assertTrue(
            ControlUtils.checkJobSkipCondition(
                conditions = conditions,
                variables = variables,
                buildId = buildId,
                runCondition = JobRunCondition.CUSTOM_VARIABLE_MATCH,
                asCodeEnabled = true
            )
        )
        Assertions.assertFalse(
            ControlUtils.checkJobSkipCondition(
                conditions = conditions,
                variables = variables,
                buildId = buildId,
                runCondition = JobRunCondition.CUSTOM_CONDITION_MATCH,
                customCondition = "key3=='un'",
                asCodeEnabled = true
            )
        )
        Assertions.assertFalse(
            ControlUtils.checkJobSkipCondition(
                conditions = conditions,
                variables = variables,
                buildId = buildId,
                runCondition = JobRunCondition.CUSTOM_CONDITION_MATCH,
                customCondition = "key3==key3",
                asCodeEnabled = true
            )
        )
        Assertions.assertFalse(
            ControlUtils.checkJobSkipCondition(
                conditions = conditions,
                variables = variables,
                buildId = buildId,
                runCondition = JobRunCondition.CUSTOM_CONDITION_MATCH,
                customCondition = "true==true",
                asCodeEnabled = true
            )
        )
        assertThrows<ExpressionParseException> {
            ControlUtils.checkJobSkipCondition(
                conditions = conditions,
                variables = variables,
                buildId = buildId,
                runCondition = JobRunCondition.CUSTOM_CONDITION_MATCH,
                customCondition = "a==a",
                asCodeEnabled = true
            )
        }
    }

    @Test
    fun pauseBeforeExec() {
        var pauseFlag: String? = null
        Assertions.assertFalse(ControlUtils.pauseBeforeExec(null, pauseFlag))
        Assertions.assertTrue(
            ControlUtils.pauseBeforeExec(
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_FAILED_ONLY)
                    .copy(pauseBeforeExec = true),
                alreadyPauseFlag = pauseFlag
            )
        )
        pauseFlag = ""
        Assertions.assertTrue(
            ControlUtils.pauseBeforeExec(
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_FAILED_ONLY)
                    .copy(pauseBeforeExec = true),
                alreadyPauseFlag = pauseFlag
            )
        )
        pauseFlag = "true"
        Assertions.assertFalse(
            ControlUtils.pauseBeforeExec(
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_FAILED_ONLY)
                    .copy(pauseBeforeExec = true),
                alreadyPauseFlag = pauseFlag
            )
        )
        Assertions.assertFalse(
            ControlUtils.pauseBeforeExec(
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_FAILED_ONLY)
                    .copy(pauseBeforeExec = true),
                alreadyPauseFlag = pauseFlag
            )
        )
        Assertions.assertFalse(
            ControlUtils.pauseBeforeExec(
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_FAILED_ONLY)
                    .copy(pauseBeforeExec = null),
                alreadyPauseFlag = pauseFlag
            )
        )
    }

    @Test
    fun retryWhenFailure() {
        val nullObject = null
        var retryCount = 0
        Assertions.assertFalse(ControlUtils.retryWhenFailure(nullObject, retryCount))
        Assertions.assertFalse(
            ControlUtils.retryWhenFailure(
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_FAILED_ONLY)
                    .copy(retryWhenFailed = false),
                retryCount = retryCount
            )
        )

        Assertions.assertFalse(
            ControlUtils.retryWhenFailure(
                additionalOptions = elementAdditionalOptions(
                    enable = false, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                ),
                retryCount = retryCount
            )
        )

        var setRetryCount = TASK_FAIL_RETRY_MAX_COUNT + 100 // 故意设置超过最大值，验证会被强制改回
        retryCount = 0
        while (retryCount < TASK_FAIL_RETRY_MAX_COUNT) {
            Assertions.assertTrue(
                ControlUtils.retryWhenFailure(
                    additionalOptions = elementAdditionalOptions(
                        retryCount = setRetryCount, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                    ).copy(retryWhenFailed = true),
                    retryCount = retryCount
                )
            )
            retryCount++
        }

        // exceed max retry count
        Assertions.assertFalse(
            ControlUtils.retryWhenFailure(
                additionalOptions = elementAdditionalOptions(
                    retryCount = setRetryCount, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                ).copy(retryWhenFailed = true),
                retryCount = retryCount
            )
        )

        setRetryCount = 0 // 故意设置小于最小值，验证会被强制改回
        retryCount = 0
        while (retryCount < TASK_FAIL_RETRY_MIN_COUNT) {

            Assertions.assertTrue(
                ControlUtils.retryWhenFailure(
                    additionalOptions = elementAdditionalOptions(
                        retryCount = setRetryCount, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                    ).copy(retryWhenFailed = true),
                    retryCount = retryCount
                )
            )
            retryCount++
        }

        // exceed max retry count
        Assertions.assertFalse(
            ControlUtils.retryWhenFailure(
                additionalOptions = elementAdditionalOptions(
                    retryCount = setRetryCount, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                ).copy(retryWhenFailed = true),
                retryCount = retryCount
            )
        )
    }

    @Test
    fun checkCustomVariableSkip() {
        variables["a"] = "1"
        val customeVarabiles = mutableListOf(NameAndValue("a", "1"))
        // 自定义变量全部满足时不运行
        Assertions.assertTrue(
            ControlUtils.checkCustomVariableSkip(
                buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN, customVariables = customeVarabiles
                )
            )
        )
        // 自定义变量全部满足时不运行, 不满足，运行。。。
        variables["a"] = "2"
        Assertions.assertFalse(
            ControlUtils.checkCustomVariableSkip(
                buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN, customVariables = customeVarabiles
                )
            )
        )
        // 自定义变量全部满足时运行
        variables["a"] = "1"
        Assertions.assertFalse(
            ControlUtils.checkCustomVariableSkip(
                buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH, customVariables = customeVarabiles
                )
            )
        )
        // 自定义变量全部满足时运行, 不满足不运行。。。
        variables["a"] = "2"
        Assertions.assertTrue(
            ControlUtils.checkCustomVariableSkip(
                buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH, customVariables = customeVarabiles
                )
            )
        )
        // 支持变量
        variables["a"] = "1"; variables["var_1"] = "1"
        customeVarabiles.add(NameAndValue("a", "\${var_1}"))
        Assertions.assertFalse(
            ControlUtils.checkCustomVariableSkip(
                buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH, customVariables = customeVarabiles
                )
            )
        )
        variables["a"] = "1"; variables["var_1"] = "1"
        customeVarabiles.add(NameAndValue("a", "\${var_1}"))
        Assertions.assertTrue(
            ControlUtils.checkCustomVariableSkip(
                buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN, customVariables = customeVarabiles
                )
            )
        )
        variables["a"] = "2"; variables["var_1"] = "1"
        customeVarabiles.add(NameAndValue("a", "\${var_1}"))
        Assertions.assertFalse(
            ControlUtils.checkCustomVariableSkip(
                buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN, customVariables = customeVarabiles
                )
            )
        )
        variables["a"] = "2"; variables["var_1"] = "1"
        customeVarabiles.add(NameAndValue("a", "\${var_1}"))
        Assertions.assertTrue(
            ControlUtils.checkCustomVariableSkip(
                buildId = buildId, variables = variables,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH, customVariables = customeVarabiles
                )
            )
        )
    }

    @Test
    fun checkStageSkipCondition() {

        val conditions = mutableListOf(
            NameAndValue("a", "1")
        )

        variables["a"] = "2"
        // 条件匹配就跳过
        Assertions.assertFalse(
            ControlUtils.checkStageSkipCondition(
                conditions = conditions, variables = variables, buildId = buildId,
                runCondition = StageRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
                asCodeEnabled = true
            )
        )

        variables["a"] = "1"
        // 条件匹配就跳过
        Assertions.assertTrue(
            ControlUtils.checkStageSkipCondition(
                conditions = conditions, variables = variables, buildId = buildId,
                runCondition = StageRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
                asCodeEnabled = true
            )
        )

        variables["a"] = "1"
        // 条件匹配就跳过
        Assertions.assertFalse(
            ControlUtils.checkStageSkipCondition(
                conditions = conditions, variables = variables, buildId = buildId,
                runCondition = StageRunCondition.CUSTOM_VARIABLE_MATCH,
                asCodeEnabled = true
            )
        )

        variables["a"] = "2"
        // 条件匹配就跳过
        Assertions.assertTrue(
            ControlUtils.checkStageSkipCondition(
                conditions = conditions, variables = variables, buildId = buildId,
                runCondition = StageRunCondition.CUSTOM_VARIABLE_MATCH,
                asCodeEnabled = true
            )
        )

        run other@{
            Assertions.assertFalse(
                ControlUtils.checkStageSkipCondition(
                    conditions = conditions, variables = variables, buildId = buildId,
                    runCondition = StageRunCondition.CUSTOM_CONDITION_MATCH,
                    customCondition = "a==a",
                    asCodeEnabled = true
                )
            )
            Assertions.assertFalse(
                ControlUtils.checkStageSkipCondition(
                    conditions = conditions, variables = variables, buildId = buildId,
                    runCondition = StageRunCondition.AFTER_LAST_FINISHED,
                    asCodeEnabled = true
                )
            )
        }
    }

    @Test
    fun `when container fail`() {
        val fail = BuildStatus.FAILED
        val failed = true
        Assertions.assertTrue(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_SUCCESS),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed,
                asCodeEnabled = true
            )
        )
        Assertions.assertTrue(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.CUSTOM_VARIABLE_MATCH),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed,
                asCodeEnabled = true
            )
        )

        Assertions.assertTrue(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed,
                asCodeEnabled = true
            )
        )
        Assertions.assertTrue(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.CUSTOM_CONDITION_MATCH),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed,
                asCodeEnabled = true
            )
        )
        Assertions.assertTrue(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.OTHER_TASK_RUNNING),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed,
                asCodeEnabled = true
            )
        )
    }

    @Test
    fun `when container running`() {
        variables["a"] = "b"
        val fail = BuildStatus.RUNNING
        val failed = false
        // 成功不跳过
        Assertions.assertFalse(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_SUCCESS),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed,
                asCodeEnabled = true
            )
        )
        // 满足执行条件 而不跳过
        Assertions.assertFalse(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
                    customVariables = mutableListOf(NameAndValue(key = "a", value = "b"))
                ),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed,
                asCodeEnabled = true
            )
        )
        // 不满足执行条件 而跳过
        Assertions.assertTrue(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
                    customVariables = mutableListOf(NameAndValue(key = "a", value = "a"))
                ),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed,
                asCodeEnabled = true
            )
        )
        // 满足不执行的条件 而跳过
        Assertions.assertTrue(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
                    customVariables = mutableListOf(NameAndValue(key = "a", value = "b"))
                ),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed,
                asCodeEnabled = true
            )
        )
        // 不满足不执行的条件 而不跳过
        Assertions.assertFalse(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
                    customVariables = mutableListOf(NameAndValue(key = "a", value = "a"))
                ),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed,
                asCodeEnabled = true
            )
        )
    }

    @Test
    fun `when container success`() {
        variables["a"] = "b"
        val fail = BuildStatus.SUCCEED
        val failed = false
        // 成功不跳过
        Assertions.assertFalse(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = elementAdditionalOptions(runCondition = RunCondition.PRE_TASK_SUCCESS),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed,
                asCodeEnabled = true
            )
        )
        // 满足执行条件 而不跳过
        Assertions.assertFalse(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
                    customVariables = mutableListOf(NameAndValue(key = "a", value = "b"))
                ),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed,
                asCodeEnabled = true
            )
        )
        // 不满足执行条件 而跳过
        Assertions.assertTrue(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
                    customVariables = mutableListOf(NameAndValue(key = "a", value = "a"))
                ),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed,
                asCodeEnabled = true
            )
        )
        // 满足不执行的条件 而跳过
        Assertions.assertTrue(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
                    customVariables = mutableListOf(NameAndValue(key = "a", value = "b"))
                ),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed,
                asCodeEnabled = true
            )
        )
        // 不满足不执行的条件 而不跳过
        Assertions.assertFalse(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
                    customVariables = mutableListOf(NameAndValue(key = "a", value = "a"))
                ),
                containerFinalStatus = fail, variables = variables, hasFailedTaskInSuccessContainer = failed,
                asCodeEnabled = true
            )
        )
    }

    @Test
    fun checkTaskConditionSkip() {
        val variables = mutableMapOf<String, String>()

        // null check
        Assertions.assertFalse(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = null,
                containerFinalStatus = BuildStatus.RUNNING,
                variables = variables,
                hasFailedTaskInSuccessContainer = true,
                asCodeEnabled = true
            )
        )

        // RunCondition.PRE_TASK_FAILED_ONLY & RUNNING
        Assertions.assertFalse(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    enable = true, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                ),
                containerFinalStatus = BuildStatus.RUNNING,
                variables = variables,
                hasFailedTaskInSuccessContainer = true,
                asCodeEnabled = true
            )
        )
        // RunCondition.PRE_TASK_FAILED_ONLY & FAIL
        Assertions.assertFalse(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    enable = true, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                ),
                containerFinalStatus = BuildStatus.FAILED,
                variables = variables,
                hasFailedTaskInSuccessContainer = true,
                asCodeEnabled = true
            )
        )
        // RunCondition.PRE_TASK_FAILED_ONLY & FAIL
        Assertions.assertFalse(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    enable = true, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                ),
                containerFinalStatus = BuildStatus.FAILED,
                variables = variables,
                hasFailedTaskInSuccessContainer = false,
                asCodeEnabled = true
            )
        )

        // RunCondition.PRE_TASK_FAILED_ONLY & disable
        Assertions.assertTrue(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    enable = false, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                ),
                containerFinalStatus = BuildStatus.FAILED,
                variables = variables,
                hasFailedTaskInSuccessContainer = false,
                asCodeEnabled = true
            )
        )

        // RunCondition.PRE_TASK_FAILED_ONLY & SUCCEED
        Assertions.assertTrue(
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = elementAdditionalOptions(
                    enable = true, runCondition = RunCondition.PRE_TASK_FAILED_ONLY
                ),
                containerFinalStatus = BuildStatus.SUCCEED,
                variables = variables,
                hasFailedTaskInSuccessContainer = false,
                asCodeEnabled = true
            )
        )
    }
}
