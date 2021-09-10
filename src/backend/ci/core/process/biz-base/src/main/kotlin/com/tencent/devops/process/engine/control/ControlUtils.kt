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

import com.tencent.devops.common.api.expression.EvalExpress
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.StageRunCondition
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.util.TaskUtils
import com.tencent.devops.process.utils.TASK_FAIL_RETRY_MAX_COUNT
import com.tencent.devops.process.utils.TASK_FAIL_RETRY_MIN_COUNT
import org.slf4j.LoggerFactory

@Suppress("ALL")
object ControlUtils {

    private val logger = LoggerFactory.getLogger(javaClass)

    // 是否使用
    fun isEnable(additionalOptions: ElementAdditionalOptions?): Boolean {
        if (additionalOptions == null) {
            return true
        }
        return additionalOptions.enable
    }

    // 插件是否失败时自动跳过/继续
    fun continueWhenFailure(additionalOptions: ElementAdditionalOptions?): Boolean {
        return (additionalOptions?.continueWhenFailed ?: false) && additionalOptions?.manualSkip != true // 手动跳过不算
    }

    // 是否失败时自动重试
    fun retryWhenFailure(additionalOptions: ElementAdditionalOptions?, retryCount: Int): Boolean {
        if (additionalOptions == null || !isEnable(additionalOptions)) {
            return false
        }
        val retryWhenFailed = additionalOptions.retryWhenFailed

        return if (retryWhenFailed) {
            var settingRetryCount = additionalOptions.retryCount
            if (settingRetryCount > TASK_FAIL_RETRY_MAX_COUNT) {
                settingRetryCount = TASK_FAIL_RETRY_MAX_COUNT
            }
            if (settingRetryCount < TASK_FAIL_RETRY_MIN_COUNT) {
                settingRetryCount = TASK_FAIL_RETRY_MIN_COUNT
            }
            retryCount < settingRetryCount
        } else {
            false
        }
    }

    // 需要暂停，且没有暂停过
    fun pauseBeforeExec(additionalOptions: ElementAdditionalOptions?, alreadyPauseFlag: String?): Boolean {
        return pauseFlag(additionalOptions) && alreadyPauseFlag.isNullOrEmpty()
    }

    // 暂停标识位
    fun pauseFlag(additionalOptions: ElementAdditionalOptions?): Boolean {
        return additionalOptions?.pauseBeforeExec == true
    }

    fun checkCustomVariableSkip(
        buildId: String,
        additionalOptions: ElementAdditionalOptions?,
        variables: Map<String, String>
    ): Boolean {

        var skip = true // 所有自定义条件都满足，则跳过
        // 自定义变量全部满足时不运行
        if (skipWhenCustomVarMatch(additionalOptions)) {
            for (names in additionalOptions!!.customVariables!!) {
                val key = names.key
                val value = EnvUtils.parseEnv(names.value, variables)
                val existValue = variables[key]
                if (value != existValue) {
                    logger.info("[$buildId]|NOT_MATCH|key=$key|exists=$existValue|exp=$value|o=${names.value}")
                    skip = false
                    break
                }
            }
            return skip
        }

        skip = false // 所有自定义条件都满足，则不能跳过
        // 自定义变量全部满足时运行
        if (notSkipWhenCustomVarMatch(additionalOptions)) {
            for (names in additionalOptions!!.customVariables!!) {
                val key = names.key
                val value = EnvUtils.parseEnv(names.value, variables)
                val existValue = variables[key]
                if (value != existValue) {
                    logger.info("[$buildId]|MATCH|key=$key|exists=$existValue|exp=$value|o=${names.value}")
                    skip = true
                    break
                }
            }
        }
        return skip
    }

    private fun notSkipWhenCustomVarMatch(additionalOptions: ElementAdditionalOptions?) =
        additionalOptions != null && additionalOptions.runCondition == RunCondition.CUSTOM_VARIABLE_MATCH &&
            additionalOptions.customVariables != null && additionalOptions.customVariables!!.isNotEmpty()

    private fun skipWhenCustomVarMatch(additionalOptions: ElementAdditionalOptions?) =
        additionalOptions != null && additionalOptions.runCondition == RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN &&
            additionalOptions.customVariables != null && additionalOptions.customVariables!!.isNotEmpty()

    /**
     * 对构建[buildId]的任务的流程控制条件[additionalOptions]进行排查，结合当前容器状态[containerFinalStatus]
     * 以及是否当前容器存在失败任务[hasFailedTaskInSuccessContainer]等条件，排查[RunCondition]下各个条件
     * 是否满足跳过条件，如果满足返回true,表示跳过。
     */
    fun checkTaskSkip(
        buildId: String,
        additionalOptions: ElementAdditionalOptions?,
        containerFinalStatus: BuildStatus,
        variables: Map<String, String>,
        hasFailedTaskInSuccessContainer: Boolean,
        buildLogPrinter: BuildLogPrinter? = null
    ): Boolean {
        var skip = false
        val runCondition = additionalOptions?.runCondition
        if (!isEnable(additionalOptions)) {
            skip = true
        } else when {
            // [只有前面有任务失败时才运行]，之前存在失败的任务
            runCondition == RunCondition.PRE_TASK_FAILED_ONLY -> {
                skip = !(containerFinalStatus.isFailure() || hasFailedTaskInSuccessContainer)
            }
            // [即使前面有插件运行失败也运行，除非被取消才不运行]，不会跳过
            runCondition == RunCondition.PRE_TASK_FAILED_BUT_CANCEL -> {
                skip = containerFinalStatus.isCancel()
            }
            //  即使前面有插件运行失败也运行，即使被取消也运行， 永远不跳过
            runCondition == RunCondition.PRE_TASK_FAILED_EVEN_CANCEL -> skip = false
            // 如果容器是失败或者取消状态，[其他条件] 都要跳过不执行
            containerFinalStatus.isFailure() || containerFinalStatus.isCancel() -> skip = true
        }
        return if (runCondition in TaskUtils.customConditionList) skip || checkCustomVariableSkip(
            buildId = buildId,
            additionalOptions = additionalOptions,
            variables = variables
        ) || checkCustomConditionSkip(
            buildId = buildId,
            additionalOptions = additionalOptions,
            variables = variables,
            buildLogPrinter = buildLogPrinter
        ) else skip
    }

    private fun checkCustomConditionSkip(
        buildId: String,
        additionalOptions: ElementAdditionalOptions?,
        variables: Map<String, String>,
        buildLogPrinter: BuildLogPrinter? = null
    ): Boolean {
        if (additionalOptions?.runCondition == RunCondition.CUSTOM_CONDITION_MATCH &&
            !additionalOptions.customCondition.isNullOrBlank()) {
            return !evalExpression(additionalOptions.customCondition, buildId, variables, buildLogPrinter)
        }

        return false
    }

    // Job是否跳过判断
    fun checkJobSkipCondition(
        conditions: List<NameAndValue>,
        variables: Map<String, String>,
        buildId: String,
        runCondition: JobRunCondition,
        customCondition: String? = null,
        buildLogPrinter: BuildLogPrinter? = null
    ): Boolean {
        var skip = when (runCondition) {
            JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> true // 条件匹配就跳过
            JobRunCondition.CUSTOM_VARIABLE_MATCH -> false // 条件全匹配就运行
            JobRunCondition.CUSTOM_CONDITION_MATCH -> { // 满足以下自定义条件时运行
                return !evalExpression(customCondition, buildId, variables, buildLogPrinter)
            }
            else -> return false // 其它类型直接返回不跳过
        }
        for (names in conditions) {
            val key = names.key
            val value = names.value
            val existValue = variables[key]
            val env = EnvUtils.parseEnv(value, variables)
            if (env != existValue) {
                skip = !skip // 不满足则取反
                logger.info("[$buildId]|JOB_CONDITION|$skip|$runCondition|key=$key|actual=$existValue|expect=$value")
                break
            }
        }
        return skip
    }

    // stage是否跳过判断
    fun checkStageSkipCondition(
        conditions: List<NameAndValue>,
        variables: Map<String, Any>,
        buildId: String,
        runCondition: StageRunCondition,
        customCondition: String? = null,
        buildLogPrinter: BuildLogPrinter? = null
    ): Boolean {
        var skip = when (runCondition) {
            StageRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> true // 条件匹配就跳过
            StageRunCondition.CUSTOM_VARIABLE_MATCH -> false // 条件全匹配就运行
            StageRunCondition.CUSTOM_CONDITION_MATCH -> { // 满足以下自定义条件时运行
                return !evalExpression(customCondition, buildId, variables, buildLogPrinter)
            }
            else -> return false // 其它类型直接返回不跳过
        }
        for (names in conditions) {
            val key = names.key
            val value = names.value
            val existValue = variables[key]
            if (value != existValue) {
                skip = !skip // 不满足则取反
                logger.info("[$buildId]|STAGE_CONDITION|$skip|$runCondition|key=$key|actual=$existValue|expect=$value")
                break
            }
        }
        return skip
    }

    private fun evalExpression(
        customCondition: String?,
        buildId: String,
        variables: Map<String, Any>,
        buildLogPrinter: BuildLogPrinter? = null
    ): Boolean {
        return if (!customCondition.isNullOrBlank()) {
            try {
                val expressionResult = EvalExpress.eval(buildId, customCondition, variables)
                logger.info("[$buildId]|STAGE_CONDITION|skip|CUSTOM_CONDITION_MATCH|expression=$customCondition" +
                    "|result=$expressionResult")
                val logMessage = "Custom condition($customCondition) result is $expressionResult. " +
                    if (!expressionResult) {
                        " will be skipped! "
                    } else {
                        ""
                    }
                buildLogPrinter?.addLine(buildId, logMessage, "", "", 1)
                expressionResult
            } catch (ignore: Exception) {
                // 异常，则任务表达式为false
                logger.info("[$buildId]|STAGE_CONDITION|skip|CUSTOM_CONDITION_MATCH|expression=$customCondition" +
                    "|result=exception: ${ignore.message}", ignore)
                val logMessage =
                    "Custom condition($customCondition) parse failed, will be skipped! Detail: ${ignore.message}"
                buildLogPrinter?.addRedLine(buildId, logMessage, "", "", 1)
                return false
            }
        } else {
            // 空表达式也认为是false
            logger.info("[$buildId]|STAGE_CONDITION|skip|CUSTOM_CONDITION_MATCH|expression is empty!")
            val logMessage = "Custom condition is empty, will be skipped!"
            buildLogPrinter?.addRedLine(buildId, logMessage, "", "", 1)
            false
        }
    }

    fun checkContainerFailure(c: PipelineBuildContainer) =
        c.status.isFailure() && c.controlOption?.jobControlOption?.continueWhenFailed != true
}
