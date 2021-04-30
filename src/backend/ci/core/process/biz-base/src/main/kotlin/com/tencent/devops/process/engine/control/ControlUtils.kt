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

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.StageRunCondition
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
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

    // 是否失败时继续
    fun continueWhenFailure(additionalOptions: ElementAdditionalOptions?): Boolean {
        if (additionalOptions == null) {
            return false
        }
        return additionalOptions.continueWhenFailed
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
        hasFailedTaskInSuccessContainer: Boolean
    ): Boolean {
        var skip = false
        if (!isEnable(additionalOptions)) {
            skip = true
        } else when {
            // [只有前面有任务失败时才运行]，容器状态必须是失败 && 之前存在失败的任务
            additionalOptions?.runCondition == RunCondition.PRE_TASK_FAILED_ONLY -> {
                skip = containerFinalStatus.isSuccess() && !hasFailedTaskInSuccessContainer
            }
            // [即使前面有插件运行失败也运行，除非被取消才不运行]，不会跳过
            additionalOptions?.runCondition == RunCondition.PRE_TASK_FAILED_BUT_CANCEL -> {
                skip = containerFinalStatus.isCancel()
            }
            //  即使前面有插件运行失败也运行，即使被取消也运行 [未实现] 永远不跳过
            additionalOptions?.runCondition == RunCondition.PRE_TASK_FAILED_EVEN_CANCEL -> skip = false
            // 如果容器是失败状态，[其他条件] 都要跳过不执行
            containerFinalStatus.isFailure() -> skip = true
        }

        return skip || checkCustomVariableSkip(buildId, additionalOptions, variables)
    }

    // Job是否跳过判断
    fun checkJobSkipCondition(
        conditions: List<NameAndValue>,
        variables: Map<String, String>,
        buildId: String,
        runCondition: JobRunCondition
    ): Boolean {
        var skip = when (runCondition) {
            JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> true // 条件匹配就跳过
            JobRunCondition.CUSTOM_VARIABLE_MATCH -> false // 条件全匹配就运行
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
        runCondition: StageRunCondition
    ): Boolean {
        var skip = when (runCondition) {
            StageRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> true // 条件匹配就跳过
            StageRunCondition.CUSTOM_VARIABLE_MATCH -> false // 条件全匹配就运行
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

    fun checkContainerFailure(c: PipelineBuildContainer) =
        c.status.isFailure() && c.controlOption?.jobControlOption?.continueWhenFailed != true
}
