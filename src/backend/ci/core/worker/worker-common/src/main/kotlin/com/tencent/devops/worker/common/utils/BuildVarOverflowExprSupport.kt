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

package com.tencent.devops.worker.common.utils

import com.tencent.devops.process.utils.BuildVarOverflowUtils
import com.tencent.devops.process.utils.PIPELINE_ID
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.variable.BuildVariableSDKApi
import com.tencent.devops.worker.common.logger.LoggerService
import org.slf4j.LoggerFactory

/**
 * Worker 侧大变量表达式按需加载支持。
 *
 * claim 下发的 variables 中大变量仅为引用串 `__BK_OVF__:<len>`；
 * 当插件入参 / 脚本使用 `${{ key }}` 时，通过 loader 远程拉取真实值。
 */
object BuildVarOverflowExprSupport {

    private val logger = LoggerFactory.getLogger(BuildVarOverflowExprSupport::class.java)
    private val variableApi by lazy { ApiFactory.create(BuildVariableSDKApi::class) }

    private const val VARIABLES_PREFIX = "variables."

    /**
     * @return overflowKeys 与按需 loader；无大变量时 loader 为 null
     */
    fun resolveOverflowOptions(
        variables: Map<String, String>
    ): Pair<Set<String>, ((String) -> String?)?> {
        val overflowKeys = BuildVarOverflowUtils.collectOverflowKeys(variables)
        if (overflowKeys.isEmpty()) {
            return emptySet<String>() to null
        }
        // variables map 里不一定有 BK_CI_PIPELINE_ID（例如部分上下文裁剪场景），
        // 用 LoggerService.buildVariables 兜底，避免 loader 被跳过导致 ${{ }} 仍展开成引用串。
        val pipelineId = variables[PIPELINE_ID]?.takeIf { it.isNotBlank() }
            ?: LoggerService.buildVariables?.pipelineId.orEmpty()
        if (pipelineId.isBlank()) {
            logger.warn("OVERFLOW_LOADER_SKIP|pipelineId blank, overflowKeys=$overflowKeys")
            return overflowKeys to null
        }
        val loader: (String) -> String? = loader@{ key ->
            try {
                // variables.xxx 落库 key 是 xxx
                val loadKey = if (key.startsWith(VARIABLES_PREFIX)) {
                    key.removePrefix(VARIABLES_PREFIX)
                } else {
                    key
                }
                val data = fetchRealValue(pipelineId, loadKey)
                // 短路径 steps.x.outputs.y 是运行时派生、不落库；其落库 key 为 jobs.<jobId>.steps.x.outputs.y。
                // 直查 miss（null 或仍是引用串）时，用当前 jobId 补前缀重查一次。
                if ((data == null || BuildVarOverflowUtils.isOverflowReference(data)) && isShortStepOutput(loadKey)) {
                    val jobId = LoggerService.buildVariables?.jobId?.takeIf { it.isNotBlank() }
                    if (!jobId.isNullOrBlank()) {
                        val fullKey = "jobs.$jobId.$loadKey"
                        val full = fetchRealValue(pipelineId, fullKey)
                        if (full != null && !BuildVarOverflowUtils.isOverflowReference(full)) {
                            logger.info("OVERFLOW_LOADER_OK|key=$key|fullKey=$fullKey|len=${full.length}")
                            return@loader full
                        }
                    }
                }
                when {
                    data == null -> {
                        logger.warn("OVERFLOW_LOADER_NULL|key=$key|loadKey=$loadKey")
                        variables[key] ?: variables[loadKey]
                    }
                    BuildVarOverflowUtils.isOverflowReference(data) -> {
                        // process 仍返回引用串：溢出表 miss 或未加载成功
                        logger.warn("OVERFLOW_LOADER_STILL_REF|key=$key|loadKey=$loadKey|data=$data")
                        data
                    }
                    else -> {
                        logger.info("OVERFLOW_LOADER_OK|key=$key|loadKey=$loadKey|len=${data.length}")
                        data
                    }
                }
            } catch (ignore: Throwable) {
                logger.warn("OVERFLOW_LOADER_FAIL|key=$key|${ignore.message}", ignore)
                variables[key] ?: variables[key.removePrefix(VARIABLES_PREFIX)]
            }
        }
        return overflowKeys to loader
    }

    private const val STEPS_PREFIX = "steps."
    private const val OUTPUTS_INFIX = ".outputs."

    // steps.<stepId>.outputs.<name> 短路径：当前 job 运行时派生、不落库
    private fun isShortStepOutput(key: String): Boolean =
        key.startsWith(STEPS_PREFIX) && key.contains(OUTPUTS_INFIX)

    /**
     * 单键查询大变量真实值；接口异常/失败返回 null（由调用方决定回退策略）。
     */
    private fun fetchRealValue(pipelineId: String, varName: String): String? {
        val result = variableApi.getBuildVariableValue(pipelineId = pipelineId, varName = varName)
        if (result.isNotOk()) {
            logger.warn(
                "OVERFLOW_LOADER_FAIL|varName=$varName|status=${result.status}|message=${result.message}"
            )
            return null
        }
        return result.data
    }
}
